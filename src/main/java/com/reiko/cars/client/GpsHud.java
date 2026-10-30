package com.reiko.cars.client;

import com.mojang.math.Axis;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.gps.GpsMath;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * The GPS's panel at the top of the screen, while it's taking you somewhere: an arrow pointing the way (from the way
 * your car's facing, or you are, on foot), where to, how far, and how long at this speed. It sits under any boss bars.
 */
public final class GpsHud implements LayeredDraw.Layer {
    private static final ResourceLocation ARROW = ReikoCars.id("textures/gui/gps_arrow.png");
    private static final int HEIGHT = 26;
    private static final int MAX_WIDTH = 230;
    /** How far down the boss bars reach this frame. */
    private static int bossBottom;

    static void onFrame(RenderGuiEvent.Pre event) {
        bossBottom = 0;
    }

    static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        bossBottom = Math.max(bossBottom, event.getY() + 5);
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        GpsClient.Destination going = GpsClient.destination();
        if (minecraft.options.hideGui || player == null || minecraft.level == null || going == null) {
            return;
        }
        Font font = minecraft.font;
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        Entity traveller = GpsClient.traveller(player);
        Vec3 at = traveller.getPosition(partialTick);
        boolean here = going.dimension().equals(minecraft.level.dimension());
        boolean metric = CarsClientConfig.speedUnit() == CarsClientConfig.SpeedUnit.KMH;

        // The second line: how far and how long, or which dimension it's in.
        Component line;
        int lineColor;
        double dx = going.pos().x - at.x;
        double dz = going.pos().z - at.z;
        int climb = going.hasY() ? (int) Math.round(going.pos().y - at.y) : 0;
        if (here) {
            double distance = Math.sqrt(dx * dx + dz * dz);
            String text = GpsMath.distance(distance, metric);
            double speed = GpsClient.speedPerSecond();
            if (speed > 0.8) {
                text += "   ~" + GpsMath.duration(distance / speed);
            }
            line = Component.literal(text);
            lineColor = 0xFF80DEEA;
        } else {
            line = Component.translatable("gps.reikocars.in_dimension", GpsScreen.dimensionName(going.dimension()));
            lineColor = 0xFFB0BEC5;
        }
        boolean showClimb = here && Math.abs(climb) > 6;
        String climbText = showClimb ? Integer.toString(Math.abs(climb)) : "";
        int lineWidth = font.width(line) + (showClimb ? 12 + font.width(climbText) : 0);
        int nameRoom = MAX_WIDTH - 36;
        String name = GpsScreen.fit(font, going.name().getString(), nameRoom);
        int width = Math.min(MAX_WIDTH, 36 + Math.max(font.width(name), lineWidth));
        int x = graphics.guiWidth() / 2 - width / 2;
        int y = bossBottom > 0 ? bossBottom + 6 : 4;

        graphics.fill(x, y, x + width, y + HEIGHT, 0xB8101418);
        graphics.renderOutline(x, y, width, HEIGHT, 0x704FC3F7);
        graphics.drawString(font, name, x + 30, y + 4, 0xFFFFFFFF, true);
        graphics.drawString(font, line, x + 30, y + 15, lineColor, true);
        if (showClimb) {
            int tx = x + 30 + font.width(line) + 6;
            triangle(graphics, tx, y + 15, climb > 0, 0xFFFFD54F);
            graphics.drawString(font, climbText, tx + 7, y + 15, 0xFFFFD54F, true);
        }

        // The arrow, turned from the way you're facing.
        int cx = x + 14;
        int cy = y + 13;
        if (here) {
            float yaw = traveller instanceof CarEntity car ? car.getViewYRot(partialTick) : player.getViewYRot(partialTick);
            float angle = (float) GpsMath.relativeAngle(GpsMath.forward(yaw), dx, dz);
            graphics.pose().pushPose();
            graphics.pose().translate(cx, cy, 0.0F);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
            graphics.blit(ARROW, -9, -9, 18, 18, 0.0F, 0.0F, 32, 32, 32, 32);
            graphics.pose().popPose();
        } else {
            // Somewhere else entirely: a dim ring for the arrow.
            graphics.renderOutline(cx - 7, cy - 7, 14, 14, 0x80B0BEC5);
            graphics.drawString(font, "?", cx - font.width("?") / 2, cy - 4, 0xFFB0BEC5, false);
        }
    }

    /** A little triangle, pointing up (the place is above you) or down, 5 pixels wide, top left at x, y. */
    private static void triangle(GuiGraphics graphics, int x, int y, boolean up, int color) {
        for (int row = 0; row < 3; row++) {
            int inset = up ? 2 - row : row;
            graphics.fill(x + inset, y + 2 + row, x + 5 - inset, y + 3 + row, color);
        }
    }
}
