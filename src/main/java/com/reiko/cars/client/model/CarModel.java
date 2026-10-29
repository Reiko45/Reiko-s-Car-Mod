package com.reiko.cars.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

/**
 * A car or motorcycle model baked from one of the Blockbench layer definitions ({@link SedanModel} and the rest).
 * Every model has the same bones, so one class draws them all:
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
 *
 * <p>The work vehicles have more, each optional: {@code beacon_a} and {@code beacon_b}, emergency lights that flash
 * in turn while the siren's on; {@code bed}, the dump truck's bed, pivoting at its hinge to tip; and
 * {@code drum_mount}, holding the mixer's {@code drum}, which turns about its own z axis.
 *
 * <p>A motorcycle has no right-hand wheels: {@code wheel_fl} is its front wheel and {@code wheel_rl} its back one.
 * Its front wheel sits inside a {@code fork} bone, with the forks and handlebars, and the whole fork steers.
 */
public final class CarModel {
    private final ModelPart body;
    private final ModelPart trim;
    @Nullable
    private final ModelPart lights;
    /** Front left, front right, rear left, rear right; a motorcycle's right-hand ones are null. */
    private final ModelPart[] wheels;
    /** The second rear axle's wheels, left and right, if there's one: they count as the rear wheels' twins. */
    private final ModelPart[] twins;
    /** A motorcycle's steering fork, which holds its front wheel; null for a car. */
    @Nullable
    private final ModelPart fork;
    /** What steers: the fork, or a car's two front wheels. */
    private final List<ModelPart> steered = new ArrayList<>();
    @Nullable
    private final ModelPart beaconA;
    @Nullable
    private final ModelPart beaconB;
    @Nullable
    private final ModelPart bed;
    @Nullable
    private final ModelPart drumMount;
    @Nullable
    private final ModelPart drum;

    public CarModel(ModelPart root) {
        this.body = root.getChild("body");
        this.trim = root.getChild("trim");
        this.lights = root.hasChild("lights") ? root.getChild("lights") : null;
        this.fork = root.hasChild("fork") ? root.getChild("fork") : null;
        ModelPart front = fork != null ? fork.getChild("wheel_fl") : root.getChild("wheel_fl");
        this.wheels = new ModelPart[] {front, child(root, "wheel_fr"), root.getChild("wheel_rl"), child(root, "wheel_rr")};
        this.twins = root.hasChild("wheel_rl2")
                ? new ModelPart[] {root.getChild("wheel_rl2"), root.getChild("wheel_rr2")}
                : new ModelPart[0];
        this.beaconA = child(root, "beacon_a");
        this.beaconB = child(root, "beacon_b");
        this.bed = child(root, "bed");
        this.drumMount = child(root, "drum_mount");
        this.drum = drumMount != null && drumMount.hasChild("drum") ? drumMount.getChild("drum") : null;
        if (fork != null) {
            steered.add(fork);
        } else {
            steered.add(wheels[0]);
            steered.add(wheels[1]);
        }
    }

    @Nullable
    private static ModelPart child(ModelPart root, String name) {
        return root.hasChild(name) ? root.getChild(name) : null;
    }

    /**
     * @param roll how far the wheels have turned, in radians (forward is positive)
     * @param steer the front wheels' angle in radians, positive to the car's left
     */
    public void pose(float roll, float steer) {
        for (ModelPart wheel : wheels) {
            if (wheel != null) {
                wheel.xRot = roll;
            }
        }
        for (ModelPart wheel : twins) {
            wheel.xRot = roll;
        }
        for (ModelPart part : steered) {
            part.yRot = -steer;
        }
    }

    /**
     * @param bedTilt how far the dump truck's bed is tipped up, in radians
     * @param drumSpin how far the mixer's drum has turned, in radians
     */
    public void poseWork(float bedTilt, float drumSpin) {
        if (bed != null) {
            bed.xRot = -bedTilt; // lifts its front end, about the hinge at its back
        }
        if (drum != null) {
            drum.zRot = drumSpin;
        }
    }

    public boolean hasBeacons() {
        return beaconA != null || beaconB != null;
    }

    /** One set of beacons: {@code first} for {@code beacon_a}, otherwise {@code beacon_b}. */
    public void renderBeacons(boolean first, PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        ModelPart beacon = first ? beaconA : beaconB;
        if (beacon != null) {
            beacon.render(poseStack, consumer, light, overlay);
        }
    }

    /** The paintwork, tinted {@code color} (ARGB). */
    public void renderBody(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        body.render(poseStack, consumer, light, overlay, color);
    }

    /** Everything that isn't paint or a light. */
    public void renderTrim(PoseStack poseStack, VertexConsumer consumer, int light, int overlay) {
        trim.render(poseStack, consumer, light, overlay);
        renderWorkParts(poseStack, consumer, light, overlay, -1);
        if (fork != null) {
            fork.render(poseStack, consumer, light, overlay); // the front wheel with it
        }
        for (int i = fork != null ? 1 : 0; i < wheels.length; i++) {
            if (wheels[i] != null) {
                wheels[i].render(poseStack, consumer, light, overlay);
            }
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

    private void renderWorkParts(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        if (bed != null) {
            bed.render(poseStack, consumer, light, overlay, color);
        }
        if (drumMount != null) {
            drumMount.render(poseStack, consumer, light, overlay, color);
        }
    }

    // ---- one bone at a time, for the workbench's preview, where parts not fitted yet are drawn as ghosts ----

    /** The trim alone, without the wheels (but with a motorcycle's forks and bars), tinted {@code color} (ARGB). */
    public void renderTrimOnly(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        trim.render(poseStack, consumer, light, overlay, color);
        renderWorkParts(poseStack, consumer, light, overlay, color);
        for (ModelPart beacon : new ModelPart[] {beaconA, beaconB}) {
            if (beacon != null) {
                beacon.render(poseStack, consumer, light, overlay, color);
            }
        }
        if (fork != null) {
            ModelPart front = wheels[0];
            front.visible = false;
            fork.render(poseStack, consumer, light, overlay, color);
            front.visible = true;
        }
    }

    /**
     * Wheel {@code index}: 0 front left, 1 front right, 2 rear left, 3 rear right (with its twin, if any). A
     * motorcycle's front wheel is 0 and its back wheel 2; it has no 1 or 3.
     */
    public void renderWheel(int index, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color) {
        ModelPart wheel = wheels[index];
        if (wheel == null) {
            return;
        }
        if (index == 0 && fork != null) {
            poseStack.pushPose();
            fork.translateAndRotate(poseStack);
            wheel.render(poseStack, consumer, light, overlay, color);
            poseStack.popPose();
        } else {
            wheel.render(poseStack, consumer, light, overlay, color);
        }
        if (index >= 2 && twins.length > 0) {
            twins[index - 2].render(poseStack, consumer, light, overlay, color);
        }
    }
}
