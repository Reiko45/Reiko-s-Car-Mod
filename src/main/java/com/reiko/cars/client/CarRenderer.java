package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.client.model.CarModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Draws a car in three passes: the paint tinted with the car's colour, then the trim and wheels (whose glass can
 * be see-through), then the lights, which glow while the engine runs.
 */
public final class CarRenderer extends EntityRenderer<CarEntity> {
    private final CarModel model;
    private final ResourceLocation texture;

    public CarRenderer(EntityRendererProvider.Context context, CarType type, ModelLayerLocation layer) {
        super(context);
        this.model = new CarModel(context.bakeLayer(layer));
        this.texture = ReikoCars.id("textures/entity/car/" + type.id() + ".png");
        this.shadowRadius = type.width * 0.55F;
    }

    @Override
    public void render(CarEntity car, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        leanOver(poseStack, car, entityYaw, partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        float towPitch = car.towPitch(partialTick);
        if (towPitch != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(towPitch)); // nose up, on a wrecker's wheel-lift (the model faces -z here)
        }
        float hurt = car.getHurtTime() - partialTick;
        float damage = Math.max(0.0F, car.getDamage() - partialTick);
        if (hurt > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * damage / 40.0F * car.getHurtDir()));
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        model.pose(car.wheelAngle(partialTick), car.steerAngle(partialTick));
        int overlay = OverlayTexture.NO_OVERLAY;
        model.renderBody(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay, 0xFF000000 | car.color());
        model.renderTrim(poseStack, buffers.getBuffer(RenderType.entityTranslucent(texture)), light, overlay);
        boolean lit = car.isEngineRunning() || car.lightsFlashing();
        model.renderLights(poseStack, buffers.getBuffer(lit ? RenderType.entityTranslucentEmissive(texture) : RenderType.entityTranslucent(texture)),
                lit ? LightTexture.FULL_BRIGHT : light, overlay);
        poseStack.popPose();
        if (car.isForSale()) {
            // A FOR SALE sign with the price floats over it, above its name if it has one.
            poseStack.pushPose();
            if (car.hasCustomName()) {
                poseStack.translate(0.0F, 0.3F, 0.0F);
            }
            long price = car.salePrice();
            Component sign = price == 0 ? Component.translatable("sale.reikocars.sign_free")
                    : Component.translatable("sale.reikocars.sign", com.reiko.economy.api.EconomyApi.get().format(price));
            renderNameTag(car, sign.copy().withStyle(net.minecraft.ChatFormatting.GOLD), poseStack, buffers, light, partialTick);
            poseStack.popPose();
        }
        super.render(car, entityYaw, partialTick, poseStack, buffers, light);
    }

    /**
     * Tips the pose over by a motorcycle's lean, about the line where its wheels touch the ground (the pose's origin
     * is the bike's position, on the ground), so it leans into turns. Its riders are tipped over the same way.
     */
    static void leanOver(PoseStack poseStack, CarEntity car, float yaw, float partialTick) {
        float lean = car.lean(partialTick);
        if (lean != 0.0F) {
            float radians = yaw * Mth.DEG_TO_RAD;
            poseStack.mulPose(new Quaternionf().rotationAxis(lean * Mth.DEG_TO_RAD, -Mth.sin(radians), 0.0F, Mth.cos(radians)));
        }
    }

    @Override
    public ResourceLocation getTextureLocation(CarEntity car) {
        return texture;
    }
}
