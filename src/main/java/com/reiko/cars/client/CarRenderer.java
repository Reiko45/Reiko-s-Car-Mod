package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarSignals;
import com.reiko.cars.car.CarType;
import com.reiko.cars.car.WorkVehicles;
import com.reiko.cars.client.model.CarModel;
import net.minecraft.client.gui.Font;
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
 * be see-through: see {@link CarGlass}), with its dirt over them (see {@link CarGrime}), then the lights, which glow
 * while the engine runs.
 */
public final class CarRenderer extends EntityRenderer<CarEntity> {
    private final CarModel model;
    private final ResourceLocation texture;
    private final CarGlass glass;
    private final CarGrime grime;
    private final CarGrime snowCover;
    private final CarWreck wreck;
    @org.jetbrains.annotations.Nullable
    private final Wipers wipers;
    private final Font font;
    @org.jetbrains.annotations.Nullable
    private final PlateMounts.Mount[] plates;
    @org.jetbrains.annotations.Nullable
    private final LiveryMounts.Mounts livery;
    private static final ResourceLocation WHITE = ReikoCars.id("textures/misc/white.png");

    public CarRenderer(EntityRendererProvider.Context context, CarType type, ModelLayerLocation layer) {
        super(context);
        this.model = new CarModel(context.bakeLayer(layer));
        this.texture = ReikoCars.id("textures/entity/car/" + type.id() + ".png");
        this.glass = new CarGlass(texture);
        this.grime = new CarGrime(texture, glass);
        this.snowCover = new CarGrime(texture, glass, true);
        this.wreck = new CarWreck(texture, glass, model, type);
        this.wipers = type.isBike() ? null : new Wipers(model, glass, texture);
        this.font = context.getFont();
        this.plates = PlateMounts.of(type.id());
        this.livery = LiveryMounts.of(type.id());
        this.shadowRadius = type.width * 0.55F;
    }

    @Override
    public void render(CarEntity car, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        unmirrorInGui(poseStack);
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
        boolean wrecked = wreck.setup(car);
        drawPlates(car, poseStack, buffers, light, wrecked);
        drawLivery(car, poseStack, buffers, light);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        if (wrecked) {
            wreck.pose(new org.joml.Matrix4f(poseStack.last().pose()));
        }

        model.pose(car.wheelAngle(partialTick), car.steerAngle(partialTick));
        model.poseWork(car.bedTilt(partialTick) * WorkVehicles.TIP_DEGREES * Mth.DEG_TO_RAD, car.drumSpin(partialTick));
        if (car.carType().isFireTruck()) {
            float out = car.ladderOut(partialTick);
            model.poseLadder(com.reiko.cars.fire.FireLadder.swing(out), com.reiko.cars.fire.FireLadder.extension(out, car.ladderHeight()));
        }
        int overlay = OverlayTexture.NO_OVERLAY;
        RenderType solid = RenderType.entityCutoutNoCull(texture);
        // Crash damage (see CarWreck): every pass bends the model the same way, all but the wheels.
        model.renderBody(poseStack, bent(wrecked, buffers.getBuffer(solid), CarWreck.SOLID), light, overlay, 0xFF000000 | car.color());
        glass.draw(buffers, solid, consumer -> model.renderTrim(poseStack, bent(wrecked, consumer, CarWreck.SOLID), consumer, light, overlay));
        if (wrecked) {
            // Its scrapes, and its cracked and shattered windows.
            wreck.drawOverlays(buffers, consumer -> {
                model.renderBody(poseStack, consumer, light, overlay, 0xFFFFFFFF);
                model.renderTrim(poseStack, consumer, CarWreck.SINK, light, overlay);
            });
        }
        float dirt = car.dirt();
        if (dirt > 0.0F) {
            // Its dirt, over the paint and trim.
            grime.draw(dirt, car.carType().height, new org.joml.Matrix4f(poseStack.last().pose()), buffers, consumer -> {
                model.renderBody(poseStack, bent(wrecked, consumer, CarWreck.SOLID), light, overlay, 0xFFFFFFFF);
                model.renderTrim(poseStack, bent(wrecked, consumer, CarWreck.SOLID), consumer, light, overlay);
            });
        }
        float snow = car.snow();
        if (snow > 0.0F) {
            // Snow on its roof, hood and trunk (off the glass while the engine runs: the wipers and heater see to that).
            snowCover.draw(snow, car.carType().height, new org.joml.Matrix4f(poseStack.last().pose()), buffers, car.isEngineRunning(), consumer -> {
                model.renderBody(poseStack, bent(wrecked, consumer, CarWreck.SOLID), light, overlay, 0xFFFFFFFF);
                model.renderTrim(poseStack, bent(wrecked, consumer, CarWreck.SOLID), consumer, light, overlay);
            });
        }
        if (wipers != null) {
            wipers.render(car, partialTick, poseStack, buffers, light);
        }
        boolean lit = car.isEngineRunning() || car.lightsFlashing() || (car.alarmOn() && CarSignals.blinkOn(car));
        if (lit) {
            model.renderLights(poseStack, bent(wrecked, buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)), CarWreck.LAMPS_LIT),
                    LightTexture.FULL_BRIGHT, overlay);
            if (wrecked && wreck.heldLamps()) {
                // The smashed ones, dark.
                glass.draw(buffers, solid, consumer -> wreck.replayLamps(consumer, light));
            }
        } else {
            glass.draw(buffers, solid, consumer -> model.renderLights(poseStack, bent(wrecked, consumer, CarWreck.LAMPS_OFF), light, overlay));
        }
        for (boolean left : new boolean[] {true, false}) {
            if (CarSignals.lit(car, left)) {
                model.renderSignal(left, poseStack, bent(wrecked, buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)), CarWreck.SIGNAL),
                        LightTexture.FULL_BRIGHT, overlay);
            }
        }
        if (model.hasBeacons()) {
            // With the siren on, the two sets of beacons flash in turn, a few times a second.
            boolean firstOn = (car.level().getGameTime() + car.getId()) / 4 % 2 == 0;
            for (boolean first : new boolean[] {true, false}) {
                boolean flashing = car.sirenOn() && first == firstOn;
                if (flashing) {
                    model.renderBeacons(first, poseStack, bent(wrecked, buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)), CarWreck.PLAIN),
                            LightTexture.FULL_BRIGHT, overlay);
                } else {
                    glass.draw(buffers, solid, consumer -> model.renderBeacons(first, poseStack, bent(wrecked, consumer, CarWreck.PLAIN), light, overlay));
                }
            }
        }
        poseStack.popPose();
        if (car.carType().isFireTruck()) {
            FireHoseLines.draw(car, partialTick, poseStack, buffers);
        }
        if (car.isImpounded()) {
            // An IMPOUNDED sign over a car held at an impound lot.
            poseStack.pushPose();
            if (car.hasCustomName()) {
                poseStack.translate(0.0F, 0.3F, 0.0F);
            }
            renderNameTag(car, Component.translatable("impound.reikocars.sign").withStyle(net.minecraft.ChatFormatting.YELLOW), poseStack, buffers, light,
                    partialTick);
            poseStack.popPose();
        } else if (car.isForSale()) {
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

    /** {@code consumer}, bent by the crash damage (and whatever else {@code mode} does: see {@link CarWreck}) if the car has any. */
    private com.mojang.blaze3d.vertex.VertexConsumer bent(boolean wrecked, com.mojang.blaze3d.vertex.VertexConsumer consumer, int mode) {
        return wrecked ? wreck.bend(consumer, mode) : consumer;
    }

    /**
     * Patchouli's entity pages (the Car Manual's 3D views) draw an entity mirrored: a screen's axes are left-handed,
     * and unlike the inventory's view of the player it doesn't flip z to make up for it, so the lettering on a
     * vehicle's sides would read backwards there. Drawn that way (flat, unflipped), turns the vehicle the right way
     * round. In the world, and wherever an entity is drawn as the inventory draws it, this changes nothing.
     */
    static void unmirrorInGui(PoseStack poseStack) {
        org.joml.Matrix4f projection = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix();
        boolean flat = projection.m33() == 1.0F && projection.m23() == 0.0F;
        if (flat && poseStack.last().pose().determinant() > 0.0F) {
            poseStack.scale(-1.0F, 1.0F, 1.0F);
        }
    }

    /**
     * Writes the car's plate number on its plate panels. The pose here has the model's pixels at (x, y, z) / 16, the
     * front toward -z; the back plate is read from behind (+z), the front one from in front.
     */
    private void drawPlates(CarEntity car, PoseStack poseStack, MultiBufferSource buffers, int light, boolean wrecked) {
        String number = car.plate();
        if (plates == null || number.isEmpty()) {
            return;
        }
        for (int i = 0; i < plates.length; i++) {
            PlateMounts.Mount mount = plates[i];
            if (mount == null) {
                continue;
            }
            boolean back = i == 1;
            poseStack.pushPose();
            poseStack.translate(0.0F, mount.y() / 16.0F, (mount.z() + (back ? 0.01F : -0.01F)) / 16.0F);
            if (wrecked) {
                // Moved with the bumper it's on (this pose has the model's x and y turned over).
                org.joml.Vector3f shift = wreck.displacement(0.0F, 1.501F - mount.y() / 16.0F, mount.z() / 16.0F, new org.joml.Vector3f());
                poseStack.translate(-shift.x, -shift.y, shift.z);
            }
            if (!back) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            writePlate(poseStack, buffers, number, mount.width(), mount.height(), light);
            poseStack.popPose();
        }
    }

    /**
     * The car's livery, in the same pose as the plates: stripes along the top of the bodywork, and its race number on a
     * white roundel on each door.
     */
    private void drawLivery(CarEntity car, PoseStack poseStack, MultiBufferSource buffers, int light) {
        com.reiko.cars.tuning.Livery paint = car.livery();
        if (livery == null || paint.isPlain()) {
            return;
        }
        net.minecraft.world.item.DyeColor stripes = paint.stripeColor();
        if (stripes != null && livery.stripes().length > 0) {
            com.mojang.blaze3d.vertex.VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
            PoseStack.Pose pose = poseStack.last();
            int color = 0xFF000000 | stripes.getTextureDiffuseColor();
            float[] runs = livery.stripes();
            for (int i = 0; i + 2 < runs.length; i += 3) {
                float z0 = runs[i] / 16.0F;
                float z1 = runs[i + 1] / 16.0F;
                float y = runs[i + 2] / 16.0F + 0.004F;
                for (float side : new float[] {1.0F, -1.0F}) {
                    float x0 = side * 1.5F / 16.0F;
                    float x1 = side * 3.5F / 16.0F;
                    quad(consumer, pose, x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0, color, light, 0.0F, 1.0F, 0.0F);
                }
            }
        }
        float[] door = livery.door();
        if (paint.number() > 0 && door != null) {
            String number = Integer.toString(paint.number());
            float size = door[3] / 16.0F;
            for (int side : new int[] {1, -1}) {
                poseStack.pushPose();
                poseStack.translate(side * (door[0] / 16.0F + 0.006F), door[1] / 16.0F, door[2] / 16.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F * side));
                // Now the door faces +z here: a black-edged white roundel, and the number on it.
                com.mojang.blaze3d.vertex.VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
                PoseStack.Pose pose = poseStack.last();
                float outer = size / 2.0F + 0.3F / 16.0F;
                float inner = size / 2.0F;
                quad(consumer, pose, -outer, -outer, 0.0F, outer, -outer, 0.0F, outer, outer, 0.0F, -outer, outer, 0.0F, 0xFF1A1A1A, light, 0, 0, 1);
                quad(consumer, pose, -inner, -inner, 0.001F, inner, -inner, 0.001F, inner, inner, 0.001F, -inner, inner, 0.001F, 0xFFF4F4F0, light,
                        0, 0, 1);
                float textWidth = Math.max(1, font.width(number) - 1);
                float sy = door[3] * 0.6F / 16.0F / 7.0F;
                float sx = Math.min(sy, door[3] * 0.8F / 16.0F / textWidth);
                poseStack.translate(0.0F, 0.0F, 0.002F);
                poseStack.scale(sx, -sy, sy);
                font.drawInBatch(number, -textWidth / 2.0F, -3.5F, 0xFF111111, false, poseStack.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET,
                        0, light);
                poseStack.popPose();
            }
        }
    }

    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1,
                             float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int color, int light, float nx,
                             float ny, float nz) {
        float[][] corners = {{x0, y0, z0, 0, 0}, {x1, y1, z1, 0, 1}, {x2, y2, z2, 1, 1}, {x3, y3, z3, 1, 0}};
        for (float[] c : corners) {
            consumer.addVertex(pose, c[0], c[1], c[2]).setColor(color).setUv(c[3], c[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }

    /** Plate letters: tall and narrow, like the real thing. A motorcycle's small plate takes them on two lines. */
    private void writePlate(PoseStack poseStack, MultiBufferSource buffers, String number, float width, float height, int light) {
        int space = number.indexOf(' ');
        if (width < 5.0F && space > 0) {
            float lineHeight = height * 0.36F;
            line(poseStack, buffers, number.substring(0, space), width, lineHeight, height * 0.22F, light);
            line(poseStack, buffers, number.substring(space + 1), width, lineHeight, -height * 0.22F, light);
        } else {
            line(poseStack, buffers, number, width, height * 0.6F, 0.0F, light);
        }
    }

    private void line(PoseStack poseStack, MultiBufferSource buffers, String text, float width, float capHeight, float up, int light) {
        float textWidth = Math.max(1, font.width(text) - 1);
        float sy = capHeight / 16.0F / 7.0F;
        float sx = Math.min(sy * 0.9F, (width - 0.8F) / 16.0F / textWidth);
        poseStack.pushPose();
        poseStack.translate(0.0F, up / 16.0F, 0.0F);
        poseStack.scale(sx, -sy, sy);
        font.drawInBatch(text, -textWidth / 2.0F, -3.5F, 0xFF1E2A6E, false, poseStack.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, light);
        poseStack.popPose();
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
