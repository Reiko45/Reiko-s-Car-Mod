package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.client.model.TrailerModel;
import com.reiko.cars.trailer.TrailerEntity;
import com.reiko.cars.trailer.TrailerType;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws a trailer as the car renderer draws a car: tinted paint, then trim and wheels, then the lights. */
public final class TrailerRenderer extends EntityRenderer<TrailerEntity> {
    private final TrailerModel model;
    private final ResourceLocation texture;

    public TrailerRenderer(EntityRendererProvider.Context context, TrailerType type, ModelLayerLocation layer) {
        super(context);
        this.model = new TrailerModel(context.bakeLayer(layer), rampOpen(type));
        this.texture = ReikoCars.id("textures/entity/trailer/" + type.id() + ".png");
        this.shadowRadius = 1.6F;
    }

    @Override
    public void render(TrailerEntity trailer, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (trailer.isInvisible()) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        float hurt = trailer.getHurtTime() - partialTick;
        float damage = Math.max(0.0F, trailer.getDamage() - partialTick);
        if (hurt > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * damage / 80.0F * trailer.getHurtDir()));
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        model.pose(trailer.wheelAngle(partialTick));
        float deployed = trailer.deployAmount(partialTick);
        model.poseCamp(deployed);
        com.reiko.cars.car.CarEntity puller = trailer.tractor();
        if (puller != null && !trailer.trailerType().isSemiTrailer()) {
            // The tow ball's hitch bar points into the pickup, which may be turned from the trailer.
            float pickupYaw = Mth.rotLerp(partialTick, puller.yRotO, puller.getYRot());
            model.poseBallMount(true, Mth.wrapDegrees(pickupYaw - entityYaw) * Mth.DEG_TO_RAD);
        } else {
            model.poseBallMount(false, 0.0F);
        }
        int overlay = OverlayTexture.NO_OVERLAY;
        model.renderBody(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay, 0xFF000000 | trailer.color());
        model.renderTrim(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        boolean lit = trailer.lightsOn();
        model.renderLights(poseStack, buffers.getBuffer(lit ? RenderType.entityTranslucentEmissive(texture) : RenderType.entityCutoutNoCull(texture)),
                lit ? LightTexture.FULL_BRIGHT : light, overlay);
        if (model.hasCampLights() && deployed > 0.03F) {
            // After dark, the string of lights under the awning and the windows glow.
            boolean night = trailer.level() instanceof net.minecraft.client.multiplayer.ClientLevel client && client.getSkyDarken(partialTick) < 0.6F;
            model.renderCampLights(poseStack, buffers.getBuffer(night ? RenderType.entityTranslucentEmissive(texture) : RenderType.entityCutoutNoCull(texture)),
                    night ? LightTexture.FULL_BRIGHT : light, overlay);
            if (night) {
                model.renderWindowGlow(poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)), LightTexture.FULL_BRIGHT, overlay);
            }
        }
        com.reiko.cars.car.CarEntity tractor = trailer.tractor();
        if (tractor != null) {
            // The tractor's turn signals and hazards blink on the trailer's tail and marker lights too.
            for (boolean left : new boolean[] {true, false}) {
                if (com.reiko.cars.car.CarSignals.lit(tractor, left)) {
                    model.renderSignal(left, poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)), LightTexture.FULL_BRIGHT, overlay);
                }
            }
        }
        poseStack.popPose();
        super.render(trailer, entityYaw, partialTick, poseStack, buffers, light);
    }

    /**
     * How far a livestock trailer's ramp swings, in radians: from standing up as the back gate, round past level until
     * its end rests on the ground. 0 for the rest.
     */
    public static float rampOpen(TrailerType type) {
        if (!type.carriesLivestock()) {
            return 0.0F;
        }
        double drop = Math.asin(Math.min(1.0, type.stockFloor() / (double) type.rampLength()));
        return (float) -(Math.PI / 2.0 + drop);
    }

    @Override
    public ResourceLocation getTextureLocation(TrailerEntity trailer) {
        return texture;
    }
}
