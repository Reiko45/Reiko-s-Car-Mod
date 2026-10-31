package com.reiko.cars.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reiko.cars.ReikoCars;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * A vehicle's windows. Minecraft draws entities before water, rain, clouds and particles, so see-through glass drawn
 * with the entity fills in the depth buffer and hides every bit of water (and the rest) behind it, from the driver's
 * seat or anywhere else. So the parts with glass in them are split by face: the solid faces are drawn with the
 * entity, with the paintwork's cutout type, and the glass faces (any face whose texture has a part-transparent
 * texel, which takes in the dark seal round each pane) are kept back and drawn later, over everything behind them,
 * like a real window: with Fast or Fancy graphics once the weather's drawn, and with Fabulous! into the water's layer
 * once the water's drawn (that layer has the depth of everything solid, and Fabulous! sorts the rain, clouds and
 * particles against it).
 */
public final class CarGlass {
    /** The glass held back this frame, a texture at a time. */
    private static final List<CarGlass> PENDING = new ArrayList<>();
    /** Whether the level's being drawn, so the glass can wait for the weather. */
    private static boolean deferring;

    private final ResourceLocation texture;
    /** The texture the held-back faces are drawn with: the car's own, or (for grime on the windows) another. */
    private final ResourceLocation drawTexture;
    private final Filter filter = new Filter();
    private int width;
    private int height;
    /** How many glass texels there are above and to the left of each texel corner (a summed-area table). */
    private int[] glassBefore;
    /** Held-back vertices: x, y, z, u, v and the normal's x, y, z; with their color, overlay and light. */
    private float[] held = new float[0];
    private int[] heldInts = new int[0];
    private int heldVertices;

    public CarGlass(ResourceLocation texture) {
        this(texture, texture);
    }

    /**
     * Glass faces of {@code texture}'s model drawn with {@code drawTexture} instead, after the glass itself: a layer over
     * the windows (see {@link CarGrime}). Its faces come from {@link #drawGlassQuad}.
     */
    public CarGlass(ResourceLocation texture, ResourceLocation drawTexture) {
        this.texture = texture;
        this.drawTexture = drawTexture;
    }

    /**
     * One glass face (four vertices: x, y, z, u, v and the normal for each, eight floats apiece, and their colour, overlay
     * and light, three ints apiece) drawn with {@link #drawTexture}: held back with the glass, or now when it isn't.
     */
    void drawGlassQuad(MultiBufferSource buffers, float[] floats, int floatOffset, int[] ints, int intOffset) {
        VertexConsumer target = deferring ? null : buffers.getBuffer(RenderType.entityTranslucent(drawTexture));
        for (int i = 0; i < 4; i++) {
            int f = floatOffset + i * 8;
            int n = intOffset + i * 3;
            if (target == null) {
                hold(floats[f], floats[f + 1], floats[f + 2], ints[n], floats[f + 3], floats[f + 4], ints[n + 1], ints[n + 2], floats[f + 5],
                        floats[f + 6], floats[f + 7]);
            } else {
                target.addVertex(floats[f], floats[f + 1], floats[f + 2], ints[n], floats[f + 3], floats[f + 4], ints[n + 1], ints[n + 2],
                        floats[f + 5], floats[f + 6], floats[f + 7]);
            }
        }
    }

    /**
     * Draws some of the model (whatever {@code draw} draws into the consumer it's given): its solid faces now with
     * {@code solid}, and its glass faces after the weather (or now, when it's drawn outside the level).
     */
    public void draw(MultiBufferSource buffers, RenderType solid, Consumer<VertexConsumer> draw) {
        load();
        draw.accept(filter.to(buffers.getBuffer(solid), false));
        if (glassBefore != null) {
            draw.accept(filter.to(deferring ? null : buffers.getBuffer(RenderType.entityTranslucent(drawTexture)), true));
        }
    }

    /** Called at every stage of drawing the level: starts holding glass back after the sky, and draws it later on. */
    static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            dropPending();
            deferring = true;
        } else if (event.getStage() == (Minecraft.useShaderTransparency()
                ? RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS : RenderLevelStageEvent.Stage.AFTER_WEATHER)) {
            deferring = false;
            if (PENDING.isEmpty()) {
                return;
            }
            // After the weather (Fast, Fancy) depth writes are already off, and in the water's layer (Fabulous!) the
            // glass's depth is what that layer is sorted by: the glass's vanilla render type leaves the depth mask as
            // it finds it, and draws to whichever target is bound.
            MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            for (CarGlass glass : PENDING) {
                glass.replay(buffers.getBuffer(RenderType.entityTranslucent(glass.drawTexture)));
            }
            buffers.endBatch();
            dropPending();
        }
    }

    private static void dropPending() {
        for (CarGlass glass : PENDING) {
            glass.heldVertices = 0;
        }
        PENDING.clear();
    }

    private void hold(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        if (heldVertices == 0) {
            PENDING.add(this);
        }
        if ((heldVertices + 1) * 8 > held.length) {
            held = Arrays.copyOf(held, Math.max(8 * 256, held.length * 2));
            heldInts = Arrays.copyOf(heldInts, held.length / 8 * 3);
        }
        int f = heldVertices * 8;
        held[f] = x;
        held[f + 1] = y;
        held[f + 2] = z;
        held[f + 3] = u;
        held[f + 4] = v;
        held[f + 5] = nx;
        held[f + 6] = ny;
        held[f + 7] = nz;
        int n = heldVertices * 3;
        heldInts[n] = color;
        heldInts[n + 1] = overlay;
        heldInts[n + 2] = light;
        heldVertices++;
    }

    private void replay(VertexConsumer consumer) {
        for (int i = 0; i < heldVertices; i++) {
            int f = i * 8;
            int n = i * 3;
            consumer.addVertex(held[f], held[f + 1], held[f + 2], heldInts[n], held[f + 3], held[f + 4], heldInts[n + 1], heldInts[n + 2],
                    held[f + 5], held[f + 6], held[f + 7]);
        }
    }

    /** Whether any texel under these texture coordinates (0 to 1) is glass. */
    public boolean isGlass(float u0, float v0, float u1, float v1) {
        load();
        if (glassBefore == null) {
            return false;
        }
        int x0 = clamp((int) Math.floor(Math.min(u0, u1) * width + 1.0E-3F), width - 1);
        int y0 = clamp((int) Math.floor(Math.min(v0, v1) * height + 1.0E-3F), height - 1);
        int x1 = clamp((int) Math.ceil(Math.max(u0, u1) * width - 1.0E-3F), width);
        int y1 = clamp((int) Math.ceil(Math.max(v0, v1) * height - 1.0E-3F), height);
        // A face drawn edge-on in the texture (a flat cube's side) still covers one texel.
        x1 = Math.max(x1, x0 + 1);
        y1 = Math.max(y1, y0 + 1);
        int stride = width + 1;
        int count = glassBefore[y1 * stride + x1] - glassBefore[y0 * stride + x1] - glassBefore[y1 * stride + x0]
                + glassBefore[y0 * stride + x0];
        return count > 0;
    }

    /** How much of the texture under these coordinates (0 to 1) is glass, 0 to 1. */
    public float glassShare(float u0, float v0, float u1, float v1) {
        load();
        if (glassBefore == null) {
            return 0.0F;
        }
        int x0 = clamp((int) Math.floor(Math.min(u0, u1) * width + 1.0E-3F), width - 1);
        int y0 = clamp((int) Math.floor(Math.min(v0, v1) * height + 1.0E-3F), height - 1);
        int x1 = Math.max(clamp((int) Math.ceil(Math.max(u0, u1) * width - 1.0E-3F), width), x0 + 1);
        int y1 = Math.max(clamp((int) Math.ceil(Math.max(v0, v1) * height - 1.0E-3F), height), y0 + 1);
        int stride = width + 1;
        int count = glassBefore[y1 * stride + x1] - glassBefore[y0 * stride + x1] - glassBefore[y1 * stride + x0]
                + glassBefore[y0 * stride + x0];
        return count / (float) ((x1 - x0) * (y1 - y0));
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private void load() {
        if (width != 0) {
            return;
        }
        width = 1;
        height = 1;
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(texture); NativeImage image = NativeImage.read(in)) {
            int w = image.getWidth();
            int h = image.getHeight();
            int[] sums = new int[(w + 1) * (h + 1)];
            boolean any = false;
            for (int y = 0; y < h; y++) {
                int row = 0;
                for (int x = 0; x < w; x++) {
                    int alpha = image.getPixelRGBA(x, y) >>> 24;
                    if (alpha > 0 && alpha < 255) {
                        row++;
                        any = true;
                    }
                    sums[(y + 1) * (w + 1) + x + 1] = sums[y * (w + 1) + x + 1] + row;
                }
            }
            width = w;
            height = h;
            glassBefore = any ? sums : null;
        } catch (IOException | RuntimeException e) {
            ReikoCars.LOGGER.warn("Couldn't read {} to find its glass", texture, e);
            glassBefore = null;
        }
    }

    /**
     * Passes on only the glass quads (to a consumer, or held back when it has none), or only the rest. Models send
     * their quads four vertices at a time.
     */
    private final class Filter implements VertexConsumer {
        private final float[] floats = new float[4 * 8];
        private final int[] ints = new int[4 * 3];
        private VertexConsumer target;
        private boolean glass;
        private int count;

        Filter to(VertexConsumer target, boolean glass) {
            this.target = target;
            this.glass = glass;
            this.count = 0;
            return this;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX,
                float normalY, float normalZ) {
            int f = count * 8;
            floats[f] = x;
            floats[f + 1] = y;
            floats[f + 2] = z;
            floats[f + 3] = u;
            floats[f + 4] = v;
            floats[f + 5] = normalX;
            floats[f + 6] = normalY;
            floats[f + 7] = normalZ;
            ints[count * 3] = color;
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
            for (int i = 1; i < 4; i++) {
                u0 = Math.min(u0, floats[i * 8 + 3]);
                u1 = Math.max(u1, floats[i * 8 + 3]);
                v0 = Math.min(v0, floats[i * 8 + 4]);
                v1 = Math.max(v1, floats[i * 8 + 4]);
            }
            if (isGlass(u0, v0, u1, v1) != glass) {
                return;
            }
            for (int i = 0; i < 4; i++) {
                int g = i * 8;
                if (target == null) {
                    hold(floats[g], floats[g + 1], floats[g + 2], ints[i * 3], floats[g + 3], floats[g + 4], ints[i * 3 + 1],
                            ints[i * 3 + 2], floats[g + 5], floats[g + 6], floats[g + 7]);
                } else {
                    target.addVertex(floats[g], floats[g + 1], floats[g + 2], ints[i * 3], floats[g + 3], floats[g + 4], ints[i * 3 + 1],
                            ints[i * 3 + 2], floats[g + 5], floats[g + 6], floats[g + 7]);
                }
            }
        }

        // Models only send whole vertices (above). Anything built a piece at a time can't be sorted, so it's dropped
        // from the held-back glass and goes straight through otherwise.
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return target == null ? this : target.addVertex(x, y, z);
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return target == null ? this : target.setColor(red, green, blue, alpha);
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return target == null ? this : target.setUv(u, v);
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return target == null ? this : target.setUv1(u, v);
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return target == null ? this : target.setUv2(u, v);
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            return target == null ? this : target.setNormal(normalX, normalY, normalZ);
        }
    }
}
