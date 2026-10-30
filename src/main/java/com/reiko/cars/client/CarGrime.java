package com.reiko.cars.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reiko.cars.ReikoCars;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Dirt (or snow) on a car: the model drawn again over itself with a texture made from the car's own, in {@link
 * #STAGES} stages from a few specks to caked on. For dirt, each face takes the stage for how dirty the car is and how
 * high up the face is (the sills and wheels get dirtiest, the roof least), and on the glass it's a lighter smear. Snow
 * lies only on the faces that look up (the roof, hood and trunk, and a raked windscreen), most on the flattest. Either
 * is drawn on the glass after the glass itself (see {@link CarGlass}).
 */
final class CarGrime {
    static final int STAGES = 6;
    /** Below this much dirt nothing shows. */
    private static final float SHOWS_FROM = 0.06F;

    private final ResourceLocation source;
    private final CarGlass glass;
    /** Snow, not dirt. */
    private final boolean snow;
    /** Leave the glass clear this time (the wipers and heater keep it clear of snow while the engine runs). */
    private boolean clearGlass;
    private final Sorter sorter = new Sorter();
    private ResourceLocation[] stages;
    private CarGlass[] glassStages;
    private boolean failed;

    CarGrime(ResourceLocation source, CarGlass glass) {
        this(source, glass, false);
    }

    CarGrime(ResourceLocation source, CarGlass glass, boolean snow) {
        this.source = source;
        this.glass = glass;
        this.snow = snow;
    }

    /** As {@link #draw}, with the glass left clear when {@code clearGlass}. */
    void draw(float amount, float height, Matrix4f modelPose, MultiBufferSource buffers, boolean clearGlass, Consumer<VertexConsumer> draw) {
        this.clearGlass = clearGlass;
        draw(amount, height, modelPose, buffers, draw);
        this.clearGlass = false;
    }

    /**
     * Draws the grime over whatever {@code draw} draws (the car's body and trim, into the consumer it's given), for a
     * car {@code height} blocks tall whose model's pose is {@code modelPose} (the model's y runs down from the ground
     * at 1.501 blocks).
     */
    void draw(float dirt, float height, Matrix4f modelPose, MultiBufferSource buffers, Consumer<VertexConsumer> draw) {
        if (dirt < SHOWS_FROM || !load()) {
            return;
        }
        sorter.start(dirt, height, modelPose);
        draw.accept(sorter);
        for (int stage = 1; stage <= STAGES; stage++) {
            Quads quads = sorter.solid[stage];
            if (quads.count > 0) {
                VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(stages[stage - 1]));
                for (int q = 0; q < quads.count; q++) {
                    for (int i = 0; i < 4; i++) {
                        int f = (q * 4 + i) * 8;
                        int n = (q * 4 + i) * 3;
                        consumer.addVertex(quads.floats[f], quads.floats[f + 1], quads.floats[f + 2], quads.ints[n], quads.floats[f + 3],
                                quads.floats[f + 4], quads.ints[n + 1], quads.ints[n + 2], quads.floats[f + 5], quads.floats[f + 6],
                                quads.floats[f + 7]);
                    }
                }
            }
            Quads panes = sorter.glass[stage];
            for (int q = 0; q < panes.count; q++) {
                glassStages[stage - 1].drawGlassQuad(buffers, panes.floats, q * 32, panes.ints, q * 12);
            }
        }
    }

    /** Which stage a face takes: none (0) up to {@link #STAGES}, dirtier the lower it is. */
    static int stage(float dirt, float heightUp) {
        float low = Math.min(1.0F, Math.max(0.35F, 1.25F - 0.9F * heightUp));
        return Math.max(0, Math.min(STAGES, (int) Math.ceil(dirt * low * STAGES - 0.5F)));
    }

    /** Which stage of snow a face takes, for how much it looks up ({@code up}, the up part of its normal): none on the sides. */
    static int snowStage(float snow, float up) {
        if (up < 0.35F) {
            return 0;
        }
        float flat = Math.min(1.0F, up * 1.25F);
        return Math.max(0, Math.min(STAGES, (int) Math.ceil(snow * flat * STAGES - 0.3F)));
    }

    // ---- the grime textures ----

    private boolean load() {
        if (stages != null) {
            return true;
        }
        if (failed) {
            return false;
        }
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(source); NativeImage image = NativeImage.read(in)) {
            int w = image.getWidth();
            int h = image.getHeight();
            float[] coverage = new float[w * h];
            float[] tone = new float[w * h];
            long seed = source.hashCode();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    // Blotches over a few sizes, then specks: where it builds up first is where it's lowest.
                    float blotch = 0.45F * smooth(x, y, 9, seed) + 0.3F * smooth(x, y, 4, seed + 1) + 0.15F * smooth(x, y, 2, seed + 2)
                            + 0.1F * hash(x, y, seed + 3);
                    coverage[y * w + x] = blotch;
                    tone[y * w + x] = 0.6F * smooth(x, y, 6, seed + 7) + 0.4F * hash(x, y, seed + 8);
                }
            }
            float[] sorted = coverage.clone();
            Arrays.sort(sorted);
            stages = new ResourceLocation[STAGES];
            glassStages = new CarGlass[STAGES];
            String name = source.getPath().replace('/', '_').replace(".png", "");
            for (int stage = 1; stage <= STAGES; stage++) {
                float threshold = sorted[Math.min(sorted.length - 1, (int) (sorted.length * stage / (float) STAGES) - 1)];
                // The glass gets less of it, and lighter, so the driver can still see out.
                float paneThreshold = snow ? threshold : sorted[Math.max(0, (int) (sorted.length * 0.55F * stage / STAGES) - 1)];
                float strength = 0.7F + 0.3F * stage / STAGES;
                NativeImage grime = new NativeImage(w, h, true);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int alpha = image.getPixelRGBA(x, y) >>> 24;
                        int i = y * w + x;
                        boolean pane = alpha < 255;
                        if (alpha == 0 || coverage[i] > (pane ? paneThreshold : threshold)) {
                            grime.setPixelRGBA(x, y, 0);
                            continue;
                        }
                        float t = tone[i];
                        int r;
                        int g;
                        int b;
                        float a;
                        if (snow) {
                            // fresh snow: white, a little blue in the shade of its lumps; thinner on the glass
                            r = (int) (226 + 26 * t);
                            g = (int) (232 + 21 * t);
                            b = (int) (242 + 13 * t);
                            a = (pane ? 0.62F + 0.25F * t : 0.82F + 0.18F * t) * strength;
                        } else if (pane) {
                            // a dusty smear on the glass
                            r = (int) (150 + 30 * t);
                            g = (int) (138 + 26 * t);
                            b = (int) (112 + 20 * t);
                            a = (0.12F + 0.14F * t) * strength;
                        } else {
                            // road dirt and mud: dark brown to dusty tan
                            r = (int) (78 + 70 * t);
                            g = (int) (62 + 58 * t);
                            b = (int) (42 + 44 * t);
                            a = (0.5F + 0.4F * (1.0F - t)) * strength;
                        }
                        int ai = Math.min(255, Math.max(26, Math.round(a * 255)));
                        grime.setPixelRGBA(x, y, ai << 24 | b << 16 | g << 8 | r); // NativeImage is ABGR
                    }
                }
                ResourceLocation id = ReikoCars.id("dynamic/" + (snow ? "snow/" : "grime/") + name + "_" + stage);
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(grime));
                stages[stage - 1] = id;
                glassStages[stage - 1] = new CarGlass(source, id);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            ReikoCars.LOGGER.warn("Couldn't make the {} for {}", snow ? "snow" : "grime", source, e);
            failed = true;
            stages = null;
            return false;
        }
    }

    /** Value noise: smooth blotches about {@code size} texels across, 0 to 1. */
    private static float smooth(int x, int y, int size, long seed) {
        float fx = x / (float) size;
        float fy = y / (float) size;
        int x0 = (int) Math.floor(fx);
        int y0 = (int) Math.floor(fy);
        float tx = fx - x0;
        float ty = fy - y0;
        tx = tx * tx * (3 - 2 * tx);
        ty = ty * ty * (3 - 2 * ty);
        float a = hash(x0, y0, seed);
        float b = hash(x0 + 1, y0, seed);
        float c = hash(x0, y0 + 1, seed);
        float d = hash(x0 + 1, y0 + 1, seed);
        return a + (b - a) * tx + (c - a) * ty + (a - b - c + d) * tx * ty;
    }

    private static float hash(int x, int y, long seed) {
        long h = seed * 0x9E3779B97F4A7C15L + x * 0xC2B2AE3D27D4EB4FL + y * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    // ---- sorting the faces into stages ----

    /** Faces for one stage: x, y, z, u, v and the normal (eight floats a vertex), colour, overlay and light (three ints). */
    private static final class Quads {
        float[] floats = new float[32 * 16];
        int[] ints = new int[12 * 16];
        int count;

        void add(float[] f, int[] n) {
            if ((count + 1) * 32 > floats.length) {
                floats = Arrays.copyOf(floats, floats.length * 2);
                ints = Arrays.copyOf(ints, ints.length * 2);
            }
            System.arraycopy(f, 0, floats, count * 32, 32);
            System.arraycopy(n, 0, ints, count * 12, 12);
            count++;
        }
    }

    /** Takes the model's faces four vertices at a time and files each under its stage, glass apart. */
    private final class Sorter implements VertexConsumer {
        final Quads[] solid = new Quads[STAGES + 1];
        final Quads[] glass = new Quads[STAGES + 1];
        private final float[] floats = new float[32];
        private final int[] ints = new int[12];
        private final Matrix4f toModel = new Matrix4f();
        private final Vector3f scratch = new Vector3f();
        private float dirt;
        private float height;
        private int count;

        Sorter() {
            for (int i = 0; i <= STAGES; i++) {
                solid[i] = new Quads();
                glass[i] = new Quads();
            }
        }

        void start(float dirt, float height, Matrix4f modelPose) {
            this.dirt = dirt;
            this.height = Math.max(0.5F, height);
            toModel.set(modelPose).invert();
            count = 0;
            for (int i = 0; i <= STAGES; i++) {
                solid[i].count = 0;
                glass[i].count = 0;
            }
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY,
                              float normalZ) {
            int f = count * 8;
            floats[f] = x;
            floats[f + 1] = y;
            floats[f + 2] = z;
            floats[f + 3] = u;
            floats[f + 4] = v;
            floats[f + 5] = normalX;
            floats[f + 6] = normalY;
            floats[f + 7] = normalZ;
            ints[count * 3] = 0xFFFFFFFF; // the grime keeps its own colour, not the paint's
            ints[count * 3 + 1] = overlay;
            ints[count * 3 + 2] = light;
            if (++count < 4) {
                return;
            }
            count = 0;
            float u0 = floats[3];
            float u1 = floats[3];
            float v0 = floats[4];
            float v1 = floats[4];
            float up = 0.0F;
            for (int i = 0; i < 4; i++) {
                u0 = Math.min(u0, floats[i * 8 + 3]);
                u1 = Math.max(u1, floats[i * 8 + 3]);
                v0 = Math.min(v0, floats[i * 8 + 4]);
                v1 = Math.max(v1, floats[i * 8 + 4]);
                toModel.transformPosition(floats[i * 8], floats[i * 8 + 1], floats[i * 8 + 2], scratch);
                up += 1.501F - scratch.y;
            }
            int stage = snow ? snowStage(dirt, floats[6]) : stage(dirt, up / 4.0F / height);
            if (stage == 0) {
                return;
            }
            boolean pane = CarGrime.this.glass.isGlass(u0, v0, u1, v1);
            if (pane && clearGlass) {
                return;
            }
            (pane ? glass : solid)[stage].add(floats, ints);
        }

        // Models only send whole vertices; anything else is ignored.
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
