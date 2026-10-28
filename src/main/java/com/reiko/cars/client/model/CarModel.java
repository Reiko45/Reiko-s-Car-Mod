package com.reiko.cars.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

/**
 * A car model baked from one of the Blockbench layer definitions ({@link SedanModel} and the rest). Every car model
 * has the same bones, so one class draws them all:
 *
 * <ul>
 *   <li>{@code body}: the paintwork, drawn tinted with the car's colour (paint it light grey in the texture);</li>
 *   <li>{@code trim}: glass, seats, bumpers, grille and the like, drawn as they are (glass may be see-through);</li>
 *   <li>{@code lights}: head- and taillights, drawn at full brightness while the engine runs;</li>
 *   <li>{@code wheel_fl}, {@code wheel_fr}, {@code wheel_rl}, {@code wheel_rr}: each pivoting on its axle, so
 *       they can roll, and the front pair can steer.</li>
 *   <li>{@code wheel_rl2}, {@code wheel_rr2}, if the model has them: a second rear axle (the semi's), rolling with
 *       the first.</li>
 * </ul>
 */
public final class CarModel {
    private final ModelPart body;
    private final ModelPart trim;
    @Nullable
    private final ModelPart lights;
    private final ModelPart[] wheels;
    /** The second rear axle's wheels, left and right, if there's one: they count as the rear wheels' twins. */
    private final ModelPart[] twins;
    private final ModelPart frontLeft;
    private final ModelPart frontRight;

    public CarModel(ModelPart root) {
        this.body = root.getChild("body");
        this.trim = root.getChild("trim");
        this.lights = root.hasChild("lights") ? root.getChild("lights") : null;
        this.frontLeft = root.getChild("wheel_fl");
        this.frontRight = root.getChild("wheel_fr");
        this.wheels = new ModelPart[] {frontLeft, frontRight, root.getChild("wheel_rl"), root.getChild("wheel_rr")};
        this.twins = root.hasChild("wheel_rl2")
                ? new ModelPart[] {root.getChild("wheel_rl2"), root.getChild("wheel_rr2")}
                : new ModelPart[0];
    }

    /**
     * @param roll how far the wheels have turned, in radians (forward is positive)
     * @param steer the front wheels' angle in radians, positive to the car's left
     */
    public void pose(float roll, float steer) {
        for (ModelPart wheel : wheels) {
            wheel.xRot = roll;
        }
        for (ModelPart wheel : twins) {
            wheel.xRot = roll;
        }
        frontLeft.yRot = -steer;
        frontRight.yRot = -steer;
    }

    /** The paintwork, tinted {@code color} (ARGB). */
    public void renderBody(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        body.render(poseStack, consumer, light, overlay, color);
    }

    /** Everything that isn't paint or a light. */
    public void renderTrim(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        trim.render(poseStack, consumer, light, overlay);
        for (ModelPart wheel : wheels) {
            wheel.render(poseStack, consumer, light, overlay);
        }
        for (ModelPart wheel : twins) {
            wheel.render(poseStack, consumer, light, overlay);
        }
    }

    public void renderLights(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        renderLights(poseStack, consumer, light, overlay, -1);
    }

    public void renderLights(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        if (lights != null) {
            lights.render(poseStack, consumer, light, overlay, color);
        }
    }

    // ---- one bone at a time, for the workbench's preview, where parts not fitted yet are drawn as ghosts ----

    /** The trim alone, without the wheels, tinted {@code color} (ARGB). */
    public void renderTrimOnly(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        trim.render(poseStack, consumer, light, overlay, color);
    }

    /** Wheel {@code index}: 0 front left, 1 front right, 2 rear left, 3 rear right (with its twin, if any). */
    public void renderWheel(int index, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        wheels[index].render(poseStack, consumer, light, overlay, color);
        if (index >= 2 && twins.length > 0) {
            twins[index - 2].render(poseStack, consumer, light, overlay, color);
        }
    }
}
