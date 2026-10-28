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
 *   <li>{@code wheel_l1}, {@code wheel_r1}, {@code wheel_l2}, {@code wheel_r2}: the two axles' wheels, which roll.</li>
 * </ul>
 */
public final class TrailerModel {
    private final ModelPart body;
    private final ModelPart trim;
    private final ModelPart lights;
    private final ModelPart[] wheels;

    public TrailerModel(ModelPart root) {
        this.body = root.getChild("body");
        this.trim = root.getChild("trim");
        this.lights = root.getChild("lights");
        this.wheels = new ModelPart[] {root.getChild("wheel_l1"), root.getChild("wheel_r1"), root.getChild("wheel_l2"), root.getChild("wheel_r2")};
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
}
