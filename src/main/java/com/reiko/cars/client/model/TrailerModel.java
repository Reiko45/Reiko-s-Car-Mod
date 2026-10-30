package com.reiko.cars.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

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
 *
 * <p>Some have more, which move:
 *
 * <ul>
 *   <li>a camper's camp, which unfolds while it's parked: {@code awning} (rolling out from the wall it's hinged to),
 *       {@code camp_lights} (the string of lights along its edge, glowing at night), {@code steps} (folding out from
 *       under the door), {@code chairs} (the camp chairs and table, opening up from the ground), {@code jacks} (the
 *       stabilizer jacks, winding down) and, on the fifth-wheel camper, {@code slide} (its slide-out room, painted);
 *       and {@code window_glow}, the windows lit warm from inside at night while the camp's set up;</li>
 *   <li>a livestock trailer's {@code ramp}: the back gate, which swings down to the ground;</li>
 *   <li>a bumper-pull's {@code ball_mount}: the tow ball and its hitch bar, which belong to the pickup pulling it, so
 *       they're only drawn while it's hitched, turned to line up with the pickup.</li>
 * </ul>
 */
public final class TrailerModel {
    /** How far a livestock trailer's ramp swings down, in radians: from upright to resting on the ground. */
    private final float rampOpen;

    private final ModelPart body;
    private final ModelPart trim;
    private final ModelPart lights;
    private final ModelPart[] wheels;
    private final ModelPart signalLeft;
    private final ModelPart signalRight;
    @Nullable
    private final ModelPart awning;
    @Nullable
    private final ModelPart campLights;
    @Nullable
    private final ModelPart steps;
    @Nullable
    private final ModelPart chairs;
    @Nullable
    private final ModelPart jacks;
    @Nullable
    private final ModelPart slide;
    @Nullable
    private final ModelPart windowGlow;
    @Nullable
    private final ModelPart ramp;
    @Nullable
    private final ModelPart ballMount;

    public TrailerModel(ModelPart root) {
        this(root, 0.0F);
    }

    /** @param rampOpen how far the ramp (if it has one) swings down, in radians */
    public TrailerModel(ModelPart root, float rampOpen) {
        this.rampOpen = rampOpen;
        this.body = root.getChild("body");
        this.trim = root.getChild("trim");
        this.lights = root.getChild("lights");
        this.wheels = new ModelPart[] {root.getChild("wheel_l1"), root.getChild("wheel_r1"), root.getChild("wheel_l2"), root.getChild("wheel_r2")};
        this.signalLeft = root.getChild("signal_l");
        this.signalRight = root.getChild("signal_r");
        this.awning = optional(root, "awning");
        this.campLights = optional(root, "camp_lights");
        this.steps = optional(root, "steps");
        this.chairs = optional(root, "chairs");
        this.jacks = optional(root, "jacks");
        this.slide = optional(root, "slide");
        this.windowGlow = optional(root, "window_glow");
        this.ramp = optional(root, "ramp");
        this.ballMount = optional(root, "ball_mount");
        poseCamp(0.0F);
        poseBallMount(false, 0.0F);
    }

    @Nullable
    private static ModelPart optional(ModelPart root, String name) {
        return root.hasChild(name) ? root.getChild(name) : null;
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

    /** Unfolds a camper's camp (or lowers a livestock trailer's ramp) {@code amount} of the way, 0 to 1. */
    public void poseCamp(float amount) {
        boolean out = amount > 0.03F;
        for (ModelPart part : new ModelPart[] {awning, campLights, steps}) {
            if (part != null) {
                part.xScale = Math.max(0.01F, amount);
                part.visible = out;
            }
        }
        if (chairs != null) {
            // the chairs open up a moment after the awning's out
            float open = Math.max(0.0F, (amount - 0.4F) / 0.6F);
            chairs.yScale = Math.max(0.01F, open);
            chairs.visible = open > 0.03F;
        }
        if (jacks != null) {
            jacks.yScale = Math.max(0.01F, Math.min(1.0F, amount * 2.0F));
            jacks.visible = out;
        }
        if (slide != null) {
            slide.xScale = 0.25F + 0.75F * amount;
        }
        if (windowGlow != null) {
            windowGlow.visible = amount > 0.5F;
        }
        if (ramp != null) {
            ramp.xRot = rampOpen * amount;
        }
    }

    /** Shows the tow ball and hitch bar (for a hitched bumper-pull), turned {@code yaw} radians to line up with the pickup. */
    public void poseBallMount(boolean shown, float yaw) {
        if (ballMount != null) {
            ballMount.visible = shown;
            ballMount.yRot = yaw;
        }
    }

    public void renderBody(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        body.render(poseStack, consumer, light, overlay, color);
        if (slide != null) {
            slide.render(poseStack, consumer, light, overlay, color);
        }
    }

    public void renderTrim(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        trim.render(poseStack, consumer, light, overlay);
        for (ModelPart wheel : wheels) {
            wheel.render(poseStack, consumer, light, overlay);
        }
        for (ModelPart part : new ModelPart[] {awning, steps, chairs, jacks, ramp, ballMount}) {
            if (part != null) {
                part.render(poseStack, consumer, light, overlay);
            }
        }
    }

    public void renderLights(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        lights.render(poseStack, consumer, light, overlay);
    }

    /** Whether it has a camp's string of lights, or windows that glow. */
    public boolean hasCampLights() {
        return campLights != null;
    }

    /** The awning's string of lights (drawn glowing at night). */
    public void renderCampLights(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        if (campLights != null) {
            campLights.render(poseStack, consumer, light, overlay);
        }
    }

    /** The windows, lit from inside (drawn only at night, glowing). */
    public void renderWindowGlow(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        if (windowGlow != null) {
            windowGlow.render(poseStack, consumer, light, overlay);
        }
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
