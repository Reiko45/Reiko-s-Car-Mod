package com.reiko.cars.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarType;
import com.reiko.cars.client.model.CarModel;
import com.reiko.cars.client.model.TrailerModel;
import com.reiko.cars.trailer.TrailerType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a car's or trailer's 3D model on a screen: the Car Workbench's preview and the JEI recipe page. Parts that
 * aren't fitted yet are drawn as see-through blue ghosts.
 */
public final class CarPreview {
    /** Parts not fitted yet: a see-through blueprint blue. */
    private static final int GHOST = 0x608CC8FF;
    private static final Map<CarType, CarModel> MODELS = new EnumMap<>(CarType.class);
    private static final Map<TrailerType, TrailerModel> TRAILER_MODELS = new EnumMap<>(TrailerType.class);

    private CarPreview() {
    }

    /**
     * Which parts to draw as fitted: the body (the paint and lights), the interior (the trim: seats, glass, bumpers;
     * for a trailer, its chassis: frame, legs and deck) and each wheel (front left, front right, rear left, rear right).
     */
    public record Fitted(boolean body, boolean interior, boolean[] wheels) {
        public static Fitted all() {
            return new Fitted(true, true, new boolean[] {true, true, true, true});
        }
    }

    private static CarModel model(CarType type) {
        return MODELS.computeIfAbsent(type, key -> new CarModel(Minecraft.getInstance().getEntityModels().bakeLayer(ReikoCarsClient.layer(key))));
    }

    /**
     * Draws the car centred on (cx, cy), seen a little from above and turned to {@code yaw} degrees. {@code scale} is
     * pixels to a block for a car up to 1.8 blocks tall; taller ones are drawn smaller to fit the same space.
     */
    public static void draw(GuiGraphics graphics, CarType type, float cx, float cy, float scale, float yaw, Fitted fitted) {
        CarModel model = model(type);
        scale *= Math.min(1.0F, 1.8F / type.height); // shrink the tall ones (pickup, off-roader) to fit the same space
        ResourceLocation texture = ReikoCars.id("textures/entity/car/" + type.id() + ".png");
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, cy, 150.0F);
        pose.scale(scale, scale, -scale);
        pose.mulPose(Axis.ZP.rotation((float) Math.PI));
        pose.mulPose(Axis.XP.rotationDegrees(-22.0F));
        pose.translate(0.0F, -type.height / 2.0F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        // the same flip the car renderer uses
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        model.pose(0.0F, 0.0F);

        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;
        if (fitted.body()) {
            model.renderBody(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay, 0xFF000000 | type.defaultColor);
        }
        graphics.flush();
        RenderType translucent = RenderType.entityTranslucent(texture);
        if (fitted.interior()) {
            model.renderTrimOnly(pose, buffers.getBuffer(translucent), light, overlay, -1);
        }
        for (int wheel = 0; wheel < 4; wheel++) {
            if (fitted.wheels()[wheel]) {
                model.renderWheel(wheel, pose, buffers.getBuffer(translucent), light, overlay, -1);
            }
        }
        if (fitted.body()) {
            model.renderLights(pose, buffers.getBuffer(translucent), light, overlay, -1);
        }
        graphics.flush();
        // The ghosts last, so the fitted parts show through them.
        if (!fitted.body()) {
            model.renderBody(pose, buffers.getBuffer(translucent), light, overlay, GHOST);
            model.renderLights(pose, buffers.getBuffer(translucent), light, overlay, GHOST);
        }
        if (!fitted.interior()) {
            model.renderTrimOnly(pose, buffers.getBuffer(translucent), light, overlay, GHOST);
        }
        for (int wheel = 0; wheel < 4; wheel++) {
            if (!fitted.wheels()[wheel]) {
                model.renderWheel(wheel, pose, buffers.getBuffer(translucent), light, overlay, GHOST);
            }
        }
        graphics.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }

    private static TrailerModel model(TrailerType type) {
        return TRAILER_MODELS.computeIfAbsent(type,
                key -> new TrailerModel(Minecraft.getInstance().getEntityModels().bakeLayer(ReikoCarsClient.layer(key)), TrailerRenderer.rampOpen(key)));
    }

    /**
     * Draws a trailer like {@link #draw(GuiGraphics, CarType, float, float, float, float, Fitted)}: {@code scale} is for
     * a car, and the trailer is drawn smaller to fit its 9 blocks into the same space. {@code fitted.interior()} is
     * its chassis.
     */
    public static void draw(GuiGraphics graphics, TrailerType type, float cx, float cy, float scale, float yaw, Fitted fitted) {
        TrailerModel model = model(type);
        scale *= Math.min(0.85F, 5.0F / type.length);
        ResourceLocation texture = ReikoCars.id("textures/entity/trailer/" + type.id() + ".png");
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx, cy, 150.0F);
        pose.scale(scale, scale, -scale);
        pose.mulPose(Axis.ZP.rotation((float) Math.PI));
        pose.mulPose(Axis.XP.rotationDegrees(-22.0F));
        pose.translate(0.0F, -type.height / 2.0F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0F, -1.501F, 0.0F);
        model.pose(0.0F);

        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;
        RenderType solid = RenderType.entityCutoutNoCull(texture);
        RenderType translucent = RenderType.entityTranslucent(texture);
        if (fitted.body()) {
            model.renderBody(pose, buffers.getBuffer(solid), light, overlay, 0xFF000000 | type.defaultColor);
            model.renderLights(pose, buffers.getBuffer(solid), light, overlay, -1);
        }
        if (fitted.interior()) {
            model.renderTrimOnly(pose, buffers.getBuffer(solid), light, overlay, -1);
        }
        for (int wheel = 0; wheel < 4; wheel++) {
            if (fitted.wheels()[wheel]) {
                model.renderWheel(wheel, pose, buffers.getBuffer(solid), light, overlay, -1);
            }
        }
        graphics.flush();
        if (!fitted.body()) {
            model.renderBody(pose, buffers.getBuffer(translucent), light, overlay, GHOST);
            model.renderLights(pose, buffers.getBuffer(translucent), light, overlay, GHOST);
        }
        if (!fitted.interior()) {
            model.renderTrimOnly(pose, buffers.getBuffer(translucent), light, overlay, GHOST);
        }
        for (int wheel = 0; wheel < 4; wheel++) {
            if (!fitted.wheels()[wheel]) {
                model.renderWheel(wheel, pose, buffers.getBuffer(translucent), light, overlay, GHOST);
            }
        }
        graphics.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }
}
