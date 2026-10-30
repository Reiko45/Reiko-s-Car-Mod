package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarSignals;
import com.reiko.cars.car.CarType;
import com.reiko.cars.car.WorkVehicles;
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
 * be see-through: see {@link CarGlass}), then the lights, which glow while the engine runs.
 */
public final class CarRenderer extends EntityRenderer<CarEntity> {
    private final CarModel model;
    private final ResourceLocation texture;
    private final CarGlass glass;

    public CarRenderer(EntityRendererProvider.Context context, CarType type, ModelLayerLocation layer) {
        super(context);
        this.model = new CarModel(context.bakeLayer(layer));
        this.texture = ReikoCars.id("textures/entity/car/" + type.id() + ".png");
        this.glass = new CarGlass(texture);
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
        model.poseWork(car.bedTilt(partialTick) * WorkVehicles.TIP_DEGREES * Mth.DEG_TO_RAD, car.drumSpin(partialTick));
        if (car.carType().isFireTruck()) {
            float out = car.ladderOut(partialTick);
            model.poseLadder(com.reiko.cars.fire.FireLadder.swing(out), com.reiko.cars.fire.FireLadder.extension(out, car.ladderHeight()));
        }
        int overlay = OverlayTexture.NO_OVERLAY;
        RenderType solid = RenderType.entityCutoutNoCull(texture);
        model.renderBody(poseStack, buffers.getBuffer(solid), light, overlay, 0xFF000000 | car.color());
        glass.draw(buffers, solid, consumer -> model.renderTrim(poseStack, consumer, light, overlay));
        boolean lit = car.isEngineRunning() || car.lightsFlashing() || (car.alarmOn() && CarSignals.blinkOn(car));
        if (lit) {
            model.renderLights(poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)), LightTexture.FULL_BRIGHT, overlay);
        } else {
            glass.draw(buffers, solid, consumer -> model.renderLights(poseStack, consumer, light, overlay));
        }
        for (boolean left : new boolean[] {true, false}) {
            if (CarSignals.lit(car, left)) {
                model.renderSignal(left, poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)), LightTexture.FULL_BRIGHT, overlay);
            }
        }
        if (model.hasBeacons()) {
            // With the siren on, the two sets of beacons flash in turn, a few times a second.
            boolean firstOn = (car.level().getGameTime() + car.getId()) / 4 % 2 == 0;
            for (boolean first : new boolean[] {true, false}) {
                boolean flashing = car.sirenOn() && first == firstOn;
                if (flashing) {
                    model.renderBeacons(first, poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)), LightTexture.FULL_BRIGHT, overlay);
                } else {
                    glass.draw(buffers, solid, consumer -> model.renderBeacons(first, poseStack, consumer, light, overlay));
                }
            }
        }
        poseStack.popPose();
        if (car.carType().isFireTruck()) {
            FireHoseLines.draw(car, partialTick, poseStack, buffers);
        }
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

    /** A fire truck stays drawn while someone near it has a hose out, for their hose and its jet. */
    @Override
    public boolean shouldRender(CarEntity car, net.minecraft.client.renderer.culling.Frustum frustum, double camX, double camY, double camZ) {
        return super.shouldRender(car, frustum, camX, camY, camZ) || car.carType().isFireTruck() && FireHoseLines.anyHoseNear(car);
    }

    @Override
    public ResourceLocation getTextureLocation(CarEntity car) {
        return texture;
    }
}
