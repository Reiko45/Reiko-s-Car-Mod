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
    private static final int LIGHT_SPACING = 19;
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
        int y = graphics.guiHeight() - height - MARGIN;
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
                : car.fuelFraction() <= 0 ? Component.translatable("hud.reikocars.out_of_fuel") : null;
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
        if (car.isLocked()) {
            Component locked = Component.translatable("hud.reikocars.locked");
            graphics.drawString(font, locked, x + WIDTH - 6 - font.width(locked), y + 16, 0x9E9E9E, false);
        }

        // Fuel gauge
        float fuel = car.fuelFraction();
        int barX = x + 14;
        int barY = y + 28;
        int barWidth = WIDTH - 28;
        graphics.drawString(font, "E", x + 5, barY - 1, 0xE57373, false);
        graphics.drawString(font, "F", x + WIDTH - 10, barY - 1, 0xA5D6A7, false);
        graphics.fill(barX, barY, barX + barWidth, barY + 6, 0xFF2A2A2A);
        int color = fuel < 0.15F ? 0xFFE53935 : fuel < 0.35F ? 0xFFFDD835 : 0xFF43A047;
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
            drawLight(graphics, LIGHTS[part.ordinal()], lightsX + part.ordinal() * LIGHT_SPACING, lightsY, lightColor(condition.level(part), flash));
        }
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
            graphics.drawString(font, hint, x + WIDTH - font.width(hint), y - 11, 0xFFFFD54F, true);
        }
    }
}
