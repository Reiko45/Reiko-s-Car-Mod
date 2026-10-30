package com.reiko.cars.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;

/**
 * A trailer model baked from one of the trailer layer definitions ({@link BoxTrailerModel} and the rest). They all
 * have the same bones, as the cars do:
 *
 * <ul>
 *   <li>{@code body}: the paintwork, tinted the trailer's colour;</li>
 *   <li>{@code trim}: frame, landing legs, deck, ladder and the like, drawn as they are;</li>
 *   <li>{@code lights}: tail and marker lights, lit while the tractor's engine runs;</li>
 *   <li>{@code wheel_l1}, {@code wheel_r1}, {@code wheel_l2}, {@code wheel_r2}: the two axles' wheels, which roll;</li>
 *   <li>{@code signal_l}, {@code signal_r}: amber copies of each side's tail and marker lights, drawn glowing while the
 *       tractor signals that way.</li>
 * </ul>
 */
public final class TrailerModel {
    private final ModelPart body;
    private final ModelPart trim;
    private final ModelPart lights;
    private final ModelPart[] wheels;
    private final ModelPart signalLeft;
    private final ModelPart signalRight;

    public TrailerModel(ModelPart root) {
        this.body = root.getChild("body");
        this.trim = root.getChild("trim");
        this.lights = root.getChild("lights");
        this.wheels = new ModelPart[] {root.getChild("wheel_l1"), root.getChild("wheel_r1"), root.getChild("wheel_l2"), root.getChild("wheel_r2")};
        this.signalLeft = root.getChild("signal_l");
        this.signalRight = root.getChild("signal_r");
    }

    /** One side's turn signal lamps, lit. */
    public void renderSignal(boolean left, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        (left ? signalLeft : signalRight).render(poseStack, consumer, light, overlay);
    }

    /** @param roll how far the wheels have turned, in radians */
    public void pose(float roll) {
        for (ModelPart wheel : wheels) {
            wheel.xRot = roll;
        }
    }

    public void renderBody(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        body.render(poseStack, consumer, light, overlay, color);
    }

    public void renderTrim(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        trim.render(poseStack, consumer, light, overlay);
        for (ModelPart wheel : wheels) {
            wheel.render(poseStack, consumer, light, overlay);
        }
    }

    public void renderLights(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        lights.render(poseStack, consumer, light, overlay);
    }

    // For the workbench preview, which draws each part on its own, some as see-through ghosts.

    public void renderTrimOnly(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        trim.render(poseStack, consumer, light, overlay, color);
    }

    /** Wheel {@code index}: front left, front right, back left, back right (the first axle, then the second). */
    public void renderWheel(int index, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        wheels[index].render(poseStack, consumer, light, overlay, color);
    }

    public void renderLights(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        lights.render(poseStack, consumer, light, overlay, color);
    }
}
