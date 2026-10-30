package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.client.model.CarModel;
import com.reiko.cars.weather.RoadWeather;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A car's windscreen wipers, and the rain on its windscreen. The windscreen is found in the model once: the biggest
 * pane of glass facing forward (with any others in line with it). Two blades rest along its foot (one, in the middle, on
 * a narrow screen) and sweep up and back while it's raining or snowing on the car and its engine runs, faster in a
 * storm. Raindrops (or snowflakes) gather on the glass while it's coming down, whether or not anyone's in the car, and the
 * blades wipe off the ones they pass over; once it stops they dry up. It's all drawn with the car, so the driver sees it
 * through the glass, and so does anyone outside.
 */
final class Wipers {
    private static final ResourceLocation WHITE = ReikoCars.id("textures/misc/white.png");
    private static final ResourceLocation RAINDROP = ReikoCars.id("textures/misc/raindrop.png");
    private static final ResourceLocation FLAKE = ReikoCars.id("textures/misc/snowflake_drop.png");
    private static final int BLADE = 0xFF1C1D20;
    private static final int MAX_DROPS = 120;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    /** How far the blades and drops stand off the glass, so they're drawn on it, not in it. */
    private static final float OFF_GLASS = 0.012F;

    private final CarModel model;
    private final CarGlass glass;
    private final CarGlass drops;
    private final CarGlass flakes;
    private final Map<CarEntity, State> states = new WeakHashMap<>();
    @Nullable
    private Shield shield;
    private boolean searched;

    Wipers(CarModel model, CarGlass glass, ResourceLocation texture) {
        this.model = model;
        this.glass = glass;
        this.drops = new CarGlass(texture, RAINDROP);
        this.flakes = new CarGlass(texture, FLAKE);
    }

    /** One blade: its pivot (across and up the screen from its bottom corner), length, and the angles it rests at and sweeps through. */
    record Blade(float pivotU, float pivotV, float length, float rest, float sweep) {
    }

    /** The windscreen, in the model's own space: a corner, the unit vectors across it and up it, its outward face, its size, and its blades. */
    record Shield(Vector3f origin, Vector3f across, Vector3f up, Vector3f out, float width, float height, Blade[] blades) {
        Vector3f at(float u, float v, float off) {
            return new Vector3f(origin).add(new Vector3f(across).mul(u)).add(new Vector3f(up).mul(v)).add(new Vector3f(out).mul(off));
        }
    }

    /** A car's wipers and the drops on its screen. */
    private static final class State {
        float phase;
        float phaseO;
        int lastTick = Integer.MIN_VALUE;
        /** Each drop: across, up, radius, and 1 for a snowflake. */
        final List<float[]> drops = new ArrayList<>();
    }

    /** How far up its sweep a blade is at {@code phase} (0 at rest, 1 at the top of the sweep). */
    static float sweepAt(float phase) {
        return (1.0F - Mth.cos(phase)) * 0.5F;
    }

    /** Draws {@code car}'s wipers and the drops on its screen; {@code poseStack} is at the model's pose. */
    void render(CarEntity car, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Shield shield = shield();
        if (shield == null) {
            return;
        }
        State state = states.computeIfAbsent(car, c -> new State());
        int ticks = car.tickCount - state.lastTick;
        if (state.lastTick == Integer.MIN_VALUE || ticks < 0 || ticks > 40) {
            ticks = 1;
        }
        for (int i = 0; i < Math.min(ticks, 5); i++) {
            step(car, shield, state);
        }
        state.lastTick = car.tickCount;

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Vector3f out = new Vector3f(shield.out);
        // the blades
        float phase = state.phaseO + (state.phase - state.phaseO) * partialTick;
        float sweep = sweepAt(phase);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        for (Blade blade : shield.blades) {
            float angle = blade.rest + blade.sweep * sweep;
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            float half = 0.016F;
            float u0 = blade.pivotU - sin * half;
            float v0 = blade.pivotV + cos * half;
            float u1 = blade.pivotU + sin * half;
            float v1 = blade.pivotV - cos * half;
            float du = cos * blade.length;
            float dv = sin * blade.length;
            quad(buffer, pose, out, light, shield.at(u0, v0, OFF_GLASS), shield.at(u1, v1, OFF_GLASS), shield.at(u1 + du, v1 + dv, OFF_GLASS),
                    shield.at(u0 + du, v0 + dv, OFF_GLASS));
            float cap = 0.03F;
            quad(buffer, pose, out, light, shield.at(blade.pivotU - cap, blade.pivotV - cap, OFF_GLASS + 0.002F),
                    shield.at(blade.pivotU + cap, blade.pivotV - cap, OFF_GLASS + 0.002F), shield.at(blade.pivotU + cap, blade.pivotV + cap, OFF_GLASS + 0.002F),
                    shield.at(blade.pivotU - cap, blade.pivotV + cap, OFF_GLASS + 0.002F));
        }
        // the drops, held back with the glass so they're drawn on it
        Matrix3f normals = pose.normal();
        Vector3f normal = normals.transform(new Vector3f(out)).normalize();
        float[] floats = new float[32];
        int[] ints = new int[12];
        float[][] corners = {{-1, -1, 0, 1}, {1, -1, 1, 1}, {1, 1, 1, 0}, {-1, 1, 0, 0}};
        for (float[] drop : state.drops) {
            for (int i = 0; i < 4; i++) {
                Vector3f at = matrix.transformPosition(shield.at(drop[0] + corners[i][0] * drop[2], drop[1] + corners[i][1] * drop[2], OFF_GLASS * 0.6F));
                int f = i * 8;
                floats[f] = at.x;
                floats[f + 1] = at.y;
                floats[f + 2] = at.z;
                floats[f + 3] = corners[i][2];
                floats[f + 4] = corners[i][3];
                floats[f + 5] = normal.x;
                floats[f + 6] = normal.y;
                floats[f + 7] = normal.z;
                ints[i * 3] = drop[3] > 0.5F ? 0xF0FFFFFF : 0xD8FFFFFF;
                ints[i * 3 + 1] = OverlayTexture.NO_OVERLAY;
                ints[i * 3 + 2] = light;
            }
            (drop[3] > 0.5F ? flakes : drops).drawGlassQuad(buffers, floats, 0, ints, 0);
        }
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, Vector3f normal, int light, Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
        for (Vector3f p : new Vector3f[] {a, b, c, d}) {
            buffer.addVertex(pose, p.x, p.y, p.z).setColor(BLADE).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(pose, normal.x, normal.y, normal.z);
        }
    }

    /** One tick: the blades move while it's coming down and the engine runs; drops land, dry up, and are wiped away. */
    private static void step(CarEntity car, Shield shield, State state) {
        boolean falling = RoadWeather.precipitation(car);
        BlockPos top = BlockPos.containing(car.getX(), car.getY() + car.carType().height, car.getZ());
        boolean snow = falling && RoadWeather.snowingAt(car.level(), top);
        boolean storm = falling && !snow && car.level().isThundering();
        boolean wiping = falling && car.isEngineRunning();
        state.phaseO = state.phase;
        if (wiping || state.phase > 0.0F) {
            state.phase += TWO_PI / (storm ? 18.0F : snow ? 24.0F : 30.0F);
            if (state.phase >= TWO_PI) {
                if (wiping) {
                    state.phase -= TWO_PI;
                    state.phaseO -= TWO_PI;
                } else {
                    state.phase = 0.0F; // back at rest, and stays there
                    state.phaseO = Math.min(state.phaseO, 0.0F);
                }
            }
        }
        RandomSource random = car.getRandom();
        if (falling) {
            float rate = snow ? 0.8F : storm ? 2.6F : 1.5F;
            int count = (int) rate + (random.nextFloat() < rate % 1.0F ? 1 : 0);
            for (int i = 0; i < count; i++) {
                float r = snow ? 0.012F + random.nextFloat() * 0.016F : 0.01F + random.nextFloat() * 0.016F;
                state.drops.add(new float[] {random.nextFloat() * shield.width, random.nextFloat() * shield.height, r, snow ? 1.0F : 0.0F});
            }
            while (state.drops.size() > MAX_DROPS) {
                state.drops.remove(0);
            }
        } else if (!state.drops.isEmpty() && random.nextInt(6) == 0) {
            state.drops.remove(random.nextInt(state.drops.size())); // drying up
        }
        // Wipe off what the blades passed over this tick.
        float from = sweepAt(state.phaseO);
        float to = sweepAt(state.phase);
        if (Math.abs(to - from) > 1.0E-4F && !state.drops.isEmpty()) {
            for (Blade blade : shield.blades) {
                float a = blade.rest + blade.sweep * Math.min(from, to);
                float b = blade.rest + blade.sweep * Math.max(from, to);
                state.drops.removeIf(drop -> {
                    float du = drop[0] - blade.pivotU;
                    float dv = drop[1] - blade.pivotV;
                    float distance = Mth.sqrt(du * du + dv * dv);
                    if (distance > blade.length + drop[2]) {
                        return false;
                    }
                    float slack = distance < 1.0E-3F ? 1.0F : drop[2] / distance;
                    float angle = (float) Math.atan2(dv, du);
                    return angle >= a - slack && angle <= b + slack;
                });
            }
        }
    }

    // ---- finding the windscreen ----

    @Nullable
    private Shield shield() {
        if (!searched) {
            searched = true;
            try {
                shield = find();
            } catch (RuntimeException e) {
                ReikoCars.LOGGER.warn("Couldn't find a car's windscreen", e);
            }
        }
        return shield;
    }

    /** Draws the model's trim at rest and picks out the windscreen. */
    @Nullable
    private Shield find() {
        Collector collector = new Collector();
        model.renderTrimOnly(new PoseStack(), collector, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
        // The front is toward -z in the model, and its y runs down.
        float[] best = null;
        float bestArea = 0.0F;
        for (float[] quad : collector.quads) {
            if (!glass.isGlass(quad[12], quad[13], quad[14], quad[15]) || quad[18] > -0.4F) {
                continue;
            }
            float area = area(quad);
            if (area > bestArea) {
                bestArea = area;
                best = quad;
            }
        }
        if (best == null) {
            return null;
        }
        Vector3f out = new Vector3f(best[16], best[17], best[18]).normalize();
        Vector3f centre = centre(best);
        Vector3f across = new Vector3f(1.0F, 0.0F, 0.0F).sub(new Vector3f(out).mul(out.x)).normalize();
        Vector3f up = new Vector3f(out).cross(across).normalize();
        if (up.y > 0.0F) {
            up.negate(); // up the glass, against the model's downward y
        }
        float minU = Float.MAX_VALUE;
        float maxU = -Float.MAX_VALUE;
        float minV = Float.MAX_VALUE;
        float maxV = -Float.MAX_VALUE;
        for (float[] quad : collector.quads) {
            Vector3f normal = new Vector3f(quad[16], quad[17], quad[18]).normalize();
            if (normal.dot(out) < 0.95F || Math.abs(new Vector3f(centre(quad)).sub(centre).dot(out)) > 0.06F
                    || !glass.isGlass(quad[12], quad[13], quad[14], quad[15])) {
                continue;
            }
            for (int i = 0; i < 4; i++) {
                Vector3f p = new Vector3f(quad[i * 3], quad[i * 3 + 1], quad[i * 3 + 2]).sub(centre);
                float u = p.dot(across);
                float v = p.dot(up);
                minU = Math.min(minU, u);
                maxU = Math.max(maxU, u);
                minV = Math.min(minV, v);
                maxV = Math.max(maxV, v);
            }
        }
        float width = maxU - minU;
        float height = maxV - minV;
        if (width < 0.25F || height < 0.12F) {
            return null;
        }
        Vector3f origin = new Vector3f(centre).add(new Vector3f(across).mul(minU)).add(new Vector3f(up).mul(minV));
        Blade[] blades;
        if (width >= 0.85F) {
            float length = Math.min(width * 0.46F, height * 0.92F);
            blades = new Blade[] {new Blade(width * 0.1F, height * 0.05F, length, 0.06F, 1.62F),
                    new Blade(width * 0.54F, height * 0.05F, length, 0.06F, 1.62F)};
        } else {
            float length = Math.min(width * 0.47F, height * 0.92F);
            blades = new Blade[] {new Blade(width * 0.5F, height * 0.04F, length, 0.25F, 2.64F)};
        }
        return new Shield(origin, across, up, out, width, height, blades);
    }

    private static Vector3f centre(float[] quad) {
        return new Vector3f((quad[0] + quad[3] + quad[6] + quad[9]) / 4.0F, (quad[1] + quad[4] + quad[7] + quad[10]) / 4.0F,
                (quad[2] + quad[5] + quad[8] + quad[11]) / 4.0F);
    }

    private static float area(float[] quad) {
        Vector3f a = new Vector3f(quad[3] - quad[0], quad[4] - quad[1], quad[5] - quad[2]);
        Vector3f b = new Vector3f(quad[9] - quad[0], quad[10] - quad[1], quad[11] - quad[2]);
        return a.cross(b).length();
    }

    /** Collects the model's faces: each its four corners, its texture's box (u0, v0, u1, v1) and its normal. */
    private static final class Collector implements VertexConsumer {
        final List<float[]> quads = new ArrayList<>();
        private float[] current = new float[19];
        private int count;

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY,
                              float normalZ) {
            current[count * 3] = x;
            current[count * 3 + 1] = y;
            current[count * 3 + 2] = z;
            if (count == 0) {
                current[12] = u;
                current[13] = v;
                current[14] = u;
                current[15] = v;
                current[16] = normalX;
                current[17] = normalY;
                current[18] = normalZ;
            } else {
                current[12] = Math.min(current[12], u);
                current[13] = Math.min(current[13], v);
                current[14] = Math.max(current[14], u);
                current[15] = Math.max(current[15], v);
            }
            if (++count == 4) {
                quads.add(current);
                current = new float[19];
                count = 0;
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            return this;
        }
    }
}
