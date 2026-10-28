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
 * </ul>
 */
public final class CarModel {
    private final ModelPart body;
    private final ModelPart trim;
    @Nullable
    private final ModelPart lights;
    private final ModelPart[] wheels;
    private final ModelPart frontLeft;
    private final ModelPart frontRight;

    public CarModel(ModelPart root) {
        this.body = root.getChild("body");
        this.trim = root.getChild("trim");
        this.lights = root.hasChild("lights") ? root.getChild("lights") : null;
        this.frontLeft = root.getChild("wheel_fl");
        this.frontRight = root.getChild("wheel_fr");
        this.wheels = new ModelPart[] {frontLeft, frontRight, root.getChild("wheel_rl"), root.getChild("wheel_rr")};
    }

    /**
     * @param roll how far the wheels have turned, in radians (forward is positive)
     * @param steer the front wheels' angle in radians, positive to the car's left
     */
    public void pose(float roll, float steer) {
        for (ModelPart wheel : wheels) {
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
    }

    public void renderLights(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        if (lights != null) {
            lights.render(poseStack, consumer, light, overlay);
        }
    }
}
