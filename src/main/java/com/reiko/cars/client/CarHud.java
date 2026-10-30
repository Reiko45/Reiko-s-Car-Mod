package com.reiko.cars.client;

import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.trailer.Hitching;
import com.reiko.cars.trailer.TrailerEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The dashboard, bottom right while you're in a car: speed, a fuel gauge running from E to F, whether the car is
 * locked, and a row of warning lights for its oil, engine, tires, brakes and body, which come on amber when a part
 * is worn, orange when it's badly worn, and flash red when it has given out. Towing, a second row shows the
 * trailer's tires, brakes and body. Drawn in code, like the economy mod's screens, so it needs no textures.
 */
public final class CarHud implements LayeredDraw.Layer {
    private static final int WIDTH = 104;
    private static final int HEIGHT = 50;
    /** The warning lights, 9 by 7, in {@link Part} order: an oil can, an engine, a tire, a brake and a car. */
    private static final String[][] LIGHTS = {
            {"..##.....", "...#.....", "######..#", "########.", "######...", "######..#", "........."},
            {"..####...", "...##....", "#######.#", "##....###", "##....#.#", ".######..", "........."},
            {"..#####..", ".##...##.", "##.###.##", "#.##.##.#", "##.###.##", ".##...##.", "..#####.."},
            {".#.###.#.", "#.#...#.#", "#.#.#.#.#", "#.#.#.#.#", "#.#...#.#", "#.#.#.#.#", ".#.###.#."},
            {".........", "..####...", ".#..#.#..", "#########", "#########", ".##...##.", "........."},
    };
    private static final int LIGHT_SPACING = 17;
    /** The turn signal arrows, 7 by 7, either side of the warning lights: left, then right. */
    private static final String[] LEFT_ARROW = {"...#...", "..##...", ".######", "#######", ".######", "..##...", "...#..."};
    private static final String[] RIGHT_ARROW = {"...#...", "...##..", "######.", "#######", "######.", "...##..", "...#..."};
    /** A trailer, for the start of the trailer's row of lights. */
    private static final String[] TRAILER = {".........", "#######..", "#######..", "#########", "#######..", ".##.##...", "........."};
    /** The trailer's row of lights adds this much to the dashboard. */
    private static final int TRAILER_ROW = 10;
    private static final int MARGIN = 6;

    /** Off (dim) while good, amber when worn, orange when badly worn, flashing red when gone. */
    private static int lightColor(CarCondition.Level level, boolean flash) {
        return switch (level) {
            case GOOD -> 0x38FFFFFF;
            case LOW -> 0xFFF2C014;
            case CRITICAL -> 0xFFFF7A1A;
            case BROKEN -> flash ? 0xFFFF3B30 : 0x60FF3B30;
        };
    }

    private static void drawLight(GuiGraphics graphics, String[] glyph, int x, int y, int color) {
        for (int row = 0; row < glyph.length; row++) {
            String line = glyph[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '#') {
                    graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || !(minecraft.player.getVehicle() instanceof CarEntity car)
                || !CarsClientConfig.showHud()) {
            return;
        }
        Font font = minecraft.font;
        TrailerEntity towed = car.trailer();
        int height = HEIGHT + (towed != null ? TRAILER_ROW : 0);
        int x = graphics.guiWidth() - WIDTH - MARGIN;
        int y;
        boolean atBottom = fitsBesideHotbar(minecraft, graphics.guiWidth());
        if (atBottom) {
            y = graphics.guiHeight() - height - MARGIN;
        } else {
            // Too narrow a screen to sit beside the hotbar: up in the top right corner instead, under any effect icons.
            y = MARGIN + effectRows(minecraft) * 25;
        }
        // Key hints go just above the dashboard, or below it at the top of the screen.
        int hintY = atBottom ? y - 11 : y + height + 3;
        int hintStep = atBottom ? -11 : 11;
        graphics.fill(x, y, x + WIDTH, y + height, 0xA0101010);
        graphics.renderOutline(x, y, WIDTH, height, 0x60FFFFFF);

        // Speed
        CarsClientConfig.SpeedUnit unit = CarsClientConfig.speedUnit();
        int speed = (int) Math.round(Math.abs(car.displaySpeed()) * unit.perBlockPerTick);
        String number = Integer.toString(speed);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 6, y + 5, 0);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);
        graphics.drawString(font, number, 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
        Component unitLabel = Component.translatable(unit.translationKey);
        // Broken down or out of fuel: in red, at the top right, where the unit gives way to it if need be.
        Component warning = car.isBrokenDown() ? Component.translatable("hud.reikocars.broken_down")
                : car.getControllingPassenger() != null && !car.keyedIn() ? Component.translatable("hud.reikocars.no_key")
                : car.fuelFraction() <= 0 ? Component.translatable(car.carType().isElectric() ? "hud.reikocars.battery_flat" : "hud.reikocars.out_of_fuel")
                : null;
        int unitX = x + 8 + font.width(number) * 2;
        int warningX = warning == null ? x + WIDTH : x + WIDTH - 6 - font.width(warning);
        if (warning != null) {
            graphics.drawString(font, warning, warningX, y + 6, 0xFF5252, true);
        }
        if (unitX + font.width(unitLabel) < warningX - 2 || warning == null) {
            graphics.drawString(font, unitLabel, unitX, y + 12, 0xB0B0B0, false);
        }
        if (car.displaySpeed() < -0.01) {
            graphics.drawString(font, "R", x + WIDTH - 12, y + 6, 0xFFD54F, false);
        }
        long meter = car.meterCents();
        if (meter >= 0) {
            // A taxi's meter, where the lock shows otherwise.
            Component fare = Component.translatable("hud.reikocars.fare", com.reiko.economy.api.EconomyApi.get().format(meter));
            graphics.drawString(font, fare, x + WIDTH - 6 - font.width(fare), y + 16, 0xFFD54F, false);
        } else if (car.isLocked()) {
            Component locked = Component.translatable("hud.reikocars.locked");
            graphics.drawString(font, locked, x + WIDTH - 6 - font.width(locked), y + 16, 0x9E9E9E, false);
        }

        // Fuel gauge, or an electric car's battery: a battery outline and its charge in percent
        float fuel = car.fuelFraction();
        int barX = x + 14;
        int barY = y + 28;
        int barWidth = WIDTH - 28;
        boolean electric = car.carType().isElectric();
        int color = fuel < 0.15F ? 0xFFE53935 : fuel < 0.35F ? 0xFFFDD835 : electric ? 0xFF29B6F6 : 0xFF43A047;
        if (electric) {
            graphics.fill(x + 4, barY, x + 11, barY + 6, 0xFF9E9E9E);
            graphics.fill(x + 5, barY + 1, x + 10, barY + 5, 0xFF2A2A2A);
            graphics.fill(x + 11, barY + 2, x + 12, barY + 4, 0xFF9E9E9E);
            graphics.fill(x + 6, barY + 2, x + 6 + Math.max(fuel > 0 ? 1 : 0, Math.round(3 * fuel)), barY + 4, color);
            String percent = Math.round(fuel * 100) + "%";
            barWidth = WIDTH - 22 - font.width(percent);
            graphics.drawString(font, percent, x + WIDTH - 5 - font.width(percent), barY - 1, 0xB3E5FC, false);
        } else {
            graphics.drawString(font, "E", x + 5, barY - 1, 0xE57373, false);
            graphics.drawString(font, "F", x + WIDTH - 10, barY - 1, 0xA5D6A7, false);
        }
        graphics.fill(barX, barY, barX + barWidth, barY + 6, 0xFF2A2A2A);
        boolean blink = fuel < 0.15F && (minecraft.player.tickCount / 10) % 2 == 0;
        if (fuel > 0 && !blink) {
            graphics.fill(barX, barY, barX + Math.max(1, Mth.floor(barWidth * fuel)), barY + 6, color);
        }
        for (int quarter = 1; quarter < 4; quarter++) {
            int tick = barX + barWidth * quarter / 4;
            graphics.fill(tick, barY, tick + 1, barY + 6, 0x80000000);
        }
        CarCondition condition = car.condition();

        // Warning lights
        int lightsX = x + (WIDTH - (LIGHT_SPACING * (LIGHTS.length - 1) + 9)) / 2;
        int lightsY = y + 38;
        boolean flash = (minecraft.player.tickCount / 5) % 2 == 0;
        for (Part part : Part.values()) {
            if (car.hasPart(part)) { // (an electric car has no oil light)
                drawLight(graphics, LIGHTS[part.ordinal()], lightsX + part.ordinal() * LIGHT_SPACING, lightsY, lightColor(condition.level(part), flash));
            }
        }
        // The turn signal arrows, green while they blink.
        drawLight(graphics, LEFT_ARROW, x + 3, lightsY, com.reiko.cars.car.CarSignals.lit(car, true) ? 0xFF5BE35B : 0x38FFFFFF);
        drawLight(graphics, RIGHT_ARROW, x + WIDTH - 10, lightsY, com.reiko.cars.car.CarSignals.lit(car, false) ? 0xFF5BE35B : 0x38FFFFFF);
        if (towed != null) {
            // The trailer's own row: a trailer, then its tires, brakes and body under the car's.
            CarCondition trailerWear = towed.condition();
            drawLight(graphics, TRAILER, lightsX, lightsY + TRAILER_ROW, 0x90FFFFFF);
            for (Part part : Part.values()) {
                if (towed.hasPart(part)) {
                    drawLight(graphics, LIGHTS[part.ordinal()], lightsX + part.ordinal() * LIGHT_SPACING, lightsY + TRAILER_ROW,
                            lightColor(trailerWear.level(part), flash));
                }
            }
        }

        // What the hitch key would do here, just above the dashboard.
        Hitching.Action action = car.getControllingPassenger() == minecraft.player ? Hitching.available(car, minecraft.player) : null;
        if (action != null) {
            Component hint = Component.translatable("hud.reikocars.hitch." + action.name().toLowerCase(java.util.Locale.ROOT),
                    ReikoCarsClient.HITCH.getTranslatedKeyMessage());
            graphics.drawString(font, hint, x + WIDTH - font.width(hint), hintY, 0xFFFFD54F, true);
        }
        // And the siren key, in a police car or ambulance: lit up while they're on.
        if (car.carType().hasSiren() && car.getControllingPassenger() == minecraft.player) {
            Component hint = Component.translatable("hud.reikocars.siren", ReikoCarsClient.SIREN.getTranslatedKeyMessage());
            int sirenColor = car.sirenOn() ? ((minecraft.level.getGameTime() / 4) % 2 == 0 ? 0xFFFF5252 : 0xFF448AFF) : 0xC0FFFFFF;
            graphics.drawString(font, hint, x + WIDTH - font.width(hint), hintY + (action != null ? hintStep : 0), sirenColor, true);
        }
    }

    /** How far up the action bar goes to clear the key hints over the dashboard, when the dashboard sits beside the hotbar. */
    public static int actionBarRaise(Minecraft minecraft, CarEntity car) {
        boolean driving = car.getControllingPassenger() == minecraft.player;
        boolean hitch = driving && Hitching.available(car, minecraft.player) != null;
        boolean siren = driving && car.carType().hasSiren();
        return hitch && siren ? 25 : 14;
    }

    /**
     * Whether the dashboard clears the hotbar, the health and food bars over it, and the hotbar's attack indicator, beside
     * them. (Its key hints sit higher up, above all of those.)
     */
    public static boolean fitsBesideHotbar(Minecraft minecraft, int guiWidth) {
        int hotbarRight = guiWidth / 2 + 91;
        if (minecraft.options.attackIndicator().get() == net.minecraft.client.AttackIndicatorStatus.HOTBAR) {
            hotbarRight += 24;
        }
        return guiWidth - MARGIN - WIDTH > hotbarRight + 2;
    }

    /** How far down the top right corner the game's status effect icons reach, in rows of 25: 0, 1 or 2. */
    private static int effectRows(Minecraft minecraft) {
        boolean good = false;
        boolean bad = false;
        for (net.minecraft.world.effect.MobEffectInstance effect : minecraft.player.getActiveEffects()) {
            if (effect.showIcon()) {
                if (effect.getEffect().value().isBeneficial()) {
                    good = true;
                } else {
                    bad = true;
                }
            }
        }
        return bad ? 2 : good ? 1 : 0; // the game draws harmful ones on the second row even with no beneficial ones
    }
}
