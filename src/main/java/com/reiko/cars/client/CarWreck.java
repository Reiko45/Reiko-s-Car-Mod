package com.reiko.cars.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarDamage;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.client.model.CarModel;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A crashed car's damage, as it shows (see {@link CarDamage}).
 *
 * <ul>
 *   <li><b>Dents:</b> the model is bent where it was hit, every vertex moved by a smooth, lumpy field (the same for
 *       every face, so the boxes stay joined): a crumpled front or back is pushed in and buckled, a side caved in along
 *       its length. Wheels are left round. Scrapes, chipped paint and creases are drawn over the dented faces, more the
 *       worse the dent.</li>
 *   <li><b>Glass:</b> a cracked window has cracks drawn over it; a shattered one isn't drawn, only the shards left round
 *       its frame.</li>
 *   <li><b>Lamps:</b> a smashed lamp cluster (a corner's headlight or taillight, and its turn signal) is dark: it doesn't
 *       light up with the engine, or blink.</li>
 * </ul>
 *
 * The model's faces come in four vertices at a time, already placed by the pose; this works in the model's own frame,
 * the pose undone, where y runs down from the ground at 1.501 blocks.
 */
final class CarWreck {
    static final int STAGES = 4;
    // What a pass through {@link #bend} does besides bending.
    /** Nothing else. */
    static final int PLAIN = 0;
    /** Leaves out shattered windows' glass. */
    static final int SOLID = 1;
    /** Lamps lit: smashed ones are kept back, to be drawn dark (see {@link #replayLamps}). */
    static final int LAMPS_LIT = 2;
    /** Lamps off: smashed ones drawn darker. */
    static final int LAMPS_OFF = 3;
    /** A turn signal blinking: smashed corners left out. */
    static final int SIGNAL = 4;

    /** Takes whatever it's given and draws nothing: for the wheels, in the passes that draw over the bodywork. */
    static final VertexConsumer SINK = new Sink();

    private final ResourceLocation source;
    private final CarGlass glass;
    private final CarModel model;
    private final CarType type;
    private ResourceLocation[] scrapes;
    /** The scrapes, for faces drawn with the glass. */
    private CarGlass[] lateScrapes;
    /** Cracks and shards are drawn across each pane's face, whatever its texture's size, so they're fine-lined even up close. */
    private static final int PANE_SIZE = 128;
    private CarGlass[] cracks;
    private CarGlass shards;
    /** The car's texture with its glass taken out, for a shattered window's frame. */
    private CarGlass paneless;
    private boolean failed;

    // The car being drawn.
    private CarDamage damage = CarDamage.NONE;
    private final Matrix4f toModel = new Matrix4f();
    private final Matrix4f modelPose = new Matrix4f();
    private float frontSign = -1.0F;
    private float leftSign = 1.0F;
    private float halfLength;
    private float halfWidth;
    /** How far the model reaches from its middle: to its front, back, left and right (blocks); measured once. */
    private float[] ends;
    private long seed;

    private final Bend bend = new Bend();
    private final Sorter sorter = new Sorter();

    CarWreck(ResourceLocation source, CarGlass glass, CarModel model, CarType type) {
        this.source = source;
        this.glass = glass;
        this.model = model;
        this.type = type;
        this.halfLength = type.length / 2.0F;
        this.halfWidth = type.width / 2.0F;
    }

    /** Sets up for {@code car}; false if it has no damage to show. */
    boolean setup(CarEntity car) {
        damage = car.condition().damage();
        if (damage.isNone()) {
            return false;
        }
        frontSign = model.frontSign();
        leftSign = model.leftSign();
        if (ends == null) {
            measure();
        }
        seed = car.getUUID().getLeastSignificantBits() ^ car.getUUID().getMostSignificantBits();
        return true;
    }

    /** Measures how far the model's paintwork reaches each way, in its own frame. */
    private void measure() {
        float[] box = {Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE}; // x min, max; z min, max
        VertexConsumer bounds = new Sink() {
            @Override
            public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX,
                                  float normalY, float normalZ) {
                box[0] = Math.min(box[0], x);
                box[1] = Math.max(box[1], x);
                box[2] = Math.min(box[2], z);
                box[3] = Math.max(box[3], z);
            }
        };
        com.mojang.blaze3d.vertex.PoseStack pose = new com.mojang.blaze3d.vertex.PoseStack();
        model.renderBody(pose, bounds, 0, 0, -1); // the paintwork: not the mirrors, which stick out

        if (box[0] > box[1]) {
            ends = new float[] {halfLength, halfLength, halfWidth, halfWidth};
            return;
        }
        float front = frontSign > 0 ? box[3] : -box[2];
        float rear = frontSign > 0 ? -box[2] : box[3];
        float left = leftSign > 0 ? box[1] : -box[0];
        float right = leftSign > 0 ? -box[0] : box[1];
        ends = new float[] {front, rear, left, right};
        halfLength = (front + rear) / 2.0F;
        halfWidth = (left + right) / 2.0F;
    }

    /** The model's pose, once the renderer has set it up. */
    void pose(Matrix4f pose) {
        modelPose.set(pose);
        toModel.set(pose).invert();
    }

    CarDamage damage() {
        return damage;
    }

    // ---- the dents ----

    /**
     * How far the dents move the point {@code m} (in the model's frame, blocks): added to it. The front and back crumple
     * in toward the middle and buckle; the sides cave in, unevenly along their length.
     */
    Vector3f displacement(float mx, float my, float mz, Vector3f out) {
        float a = mz * frontSign; // along the car, + toward the front
        float b = mx * leftSign; // across it, + to its left
        float h = 1.501F - my; // up from the ground
        float da = 0.0F;
        float db = 0.0F;
        float dh = 0.0F;
        float length = halfLength * 2.0F;
        float reach = Mth.clamp(length * 0.22F, 0.35F, 1.1F);
        float depth = Math.min(0.42F, length * 0.11F);
        for (int end = 0; end < 2; end++) {
            float dent = end == 0 ? damage.front() : damage.rear();
            float along = end == 0 ? a : -a;
            float edge = ends == null ? halfLength : ends[end];
            if (dent <= 0.0F || along <= edge - reach) {
                continue;
            }
            float t = Math.min(1.0F, (along - (edge - reach)) / reach);
            float w = (float) Math.pow(t, 1.3) * dent;
            long s = seed + end * 101;
            float pushed = w * depth * (0.7F + 0.6F * noise(b * 2.5F, h * 2.5F, s));
            da += end == 0 ? -pushed : pushed;
            dh += w * 0.12F * (noise(b * 1.7F, 5.3F, s + 11) - 0.35F);
            db += w * 0.08F * (noise(h * 2.0F, 9.1F, s + 5) - 0.5F);
        }
        float sideReach = 0.45F;
        float sideDepth = Math.min(0.28F, halfWidth * 0.25F);
        for (int side = 0; side < 2; side++) {
            float dent = side == 0 ? damage.left() : damage.right();
            float across = side == 0 ? b : -b;
            float edge = ends == null ? halfWidth : ends[2 + side];
            if (dent <= 0.0F || across <= edge - sideReach) {
                continue;
            }
            float t = Math.min(1.0F, (across - (edge - sideReach)) / sideReach);
            float w = t * dent;
            long s = seed + 307 + side * 53;
            float caved = w * sideDepth * (0.25F + 1.1F * noise(a * 1.6F, h * 1.5F, s));
            db += side == 0 ? -caved : caved;
            dh += w * 0.03F * (noise(a * 2.1F, 3.7F, s + 3) - 0.5F);
        }
        return out.set(db * leftSign, -dh, da * frontSign);
    }

    /** Smooth value noise, 0 to 1, in two dimensions. */
    private static float noise(float x, float y, long seed) {
        int x0 = Mth.floor(x);
        int y0 = Mth.floor(y);
        float tx = x - x0;
        float ty = y - y0;
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

    /** {@code target}, with the model bent on the way through, and whatever else {@code mode} does. */
    VertexConsumer bend(VertexConsumer target, int mode) {
        return bend.to(target, mode);
    }

    /** Draws the lamps that {@link #LAMPS_LIT} kept back, dark, into {@code consumer}, lit by the world's {@code light}. */
    void replayLamps(VertexConsumer consumer, int light) {
        Quads held = bend.held;
        for (int q = 0; q < held.count; q++) {
            for (int i = 0; i < 4; i++) {
                int f = (q * 4 + i) * 8;
                int n = (q * 4 + i) * 3;
                consumer.addVertex(held.floats[f], held.floats[f + 1], held.floats[f + 2], darker(held.ints[n]), held.floats[f + 3],
                        held.floats[f + 4], held.ints[n + 1], light, held.floats[f + 5], held.floats[f + 6], held.floats[f + 7]);
            }
        }
    }

    boolean heldLamps() {
        return bend.held.count > 0;
    }

    /** A smashed lamp's colour: a dark, empty housing. */
    private static int darker(int argb) {
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        return (argb & 0xFF000000) | (r * 30 / 100) << 16 | (g * 30 / 100) << 8 | (b * 30 / 100);
    }

    /** Which lamp cluster a point (in the model's frame) belongs to. */
    private int lampAt(float mx, float mz) {
        boolean front = mz * frontSign > 0.0F;
        boolean left = mx * leftSign > 0.0F;
        return front ? (left ? CarDamage.FRONT_LEFT : CarDamage.FRONT_RIGHT) : (left ? CarDamage.REAR_LEFT : CarDamage.REAR_RIGHT);
    }

    /** Which window a pane facing {@code n} (in the model's frame) is, or -1 for one facing up (a sunroof). */
    private int windowFacing(Vector3f n) {
        float a = n.z * frontSign;
        float b = n.x * leftSign;
        if (Math.abs(n.y) > 0.85F) {
            return -1;
        }
        if (Math.abs(a) >= Math.abs(b)) {
            return a > 0.0F ? CarDamage.WINDSHIELD : CarDamage.BACK_WINDOW;
        }
        return b > 0.0F ? CarDamage.LEFT_WINDOW : CarDamage.RIGHT_WINDOW;
    }

    // ---- drawing over it ----

    /**
     * Draws the scrapes over the dented faces and the cracks and shards on the broken windows: over whatever {@code draw}
     * draws (the body and trim, the wheels into {@link #SINK}) into the consumer it's given.
     */
    void drawOverlays(MultiBufferSource buffers, Consumer<VertexConsumer> draw) {
        if (!load()) {
            return;
        }
        sorter.start();
        draw.accept(bend.to(sorter, PLAIN));
        for (int stage = 1; stage <= STAGES; stage++) {
            Quads quads = sorter.scraped[stage];
            if (quads.count == 0) {
                continue;
            }
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(scrapes[stage - 1]));
            for (int q = 0; q < quads.count; q++) {
                for (int i = 0; i < 4; i++) {
                    int f = (q * 4 + i) * 8;
                    int n = (q * 4 + i) * 3;
                    consumer.addVertex(quads.floats[f], quads.floats[f + 1], quads.floats[f + 2], 0xFFFFFFFF, quads.floats[f + 3],
                            quads.floats[f + 4], quads.ints[n + 1], quads.ints[n + 2], quads.floats[f + 5], quads.floats[f + 6],
                            quads.floats[f + 7]);
                }
            }
        }
        for (int stage = 1; stage <= STAGES; stage++) {
            Quads late = sorter.scrapedLate[stage];
            for (int q = 0; q < late.count; q++) {
                lateScrapes[stage - 1].drawGlassQuad(buffers, late.floats, q * 32, late.ints, q * 12);
            }
        }
        for (int q = 0; q < sorter.cracked.count; q++) {
            if (acrossFace(sorter.cracked.floats, q * 32, sorter.paneUv)) {
                cracks[sorter.crackVariant[q]].drawGlassQuad(buffers, sorter.paneUv, 0, sorter.cracked.ints, q * 12);
            }
        }
        for (int q = 0; q < sorter.shattered.count; q++) {
            // Its frame, without the glass; then the shards left in it.
            paneless.drawGlassQuad(buffers, sorter.shattered.floats, q * 32, sorter.shattered.ints, q * 12);
            if (acrossFace(sorter.shattered.floats, q * 32, sorter.paneUv)) {
                shards.drawGlassQuad(buffers, sorter.paneUv, 0, sorter.shattered.ints, q * 12);
            }
        }
    }

    /**
     * A copy of the face at {@code offset} in {@code floats}, into {@code out}, with its texture stretched across it: its
     * corners at the texture's corners, the way round its own texture runs. False for a face seen edge-on in its texture.
     */
    private static boolean acrossFace(float[] floats, int offset, float[] out) {
        float u0 = Float.MAX_VALUE;
        float u1 = -Float.MAX_VALUE;
        float v0 = Float.MAX_VALUE;
        float v1 = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            u0 = Math.min(u0, floats[offset + i * 8 + 3]);
            u1 = Math.max(u1, floats[offset + i * 8 + 3]);
            v0 = Math.min(v0, floats[offset + i * 8 + 4]);
            v1 = Math.max(v1, floats[offset + i * 8 + 4]);
        }
        if (u1 - u0 < 1.0E-5F || v1 - v0 < 1.0E-5F) {
            return false;
        }
        System.arraycopy(floats, offset, out, 0, 32);
        for (int i = 0; i < 4; i++) {
            out[i * 8 + 3] = (out[i * 8 + 3] - u0) / (u1 - u0);
            out[i * 8 + 4] = (out[i * 8 + 4] - v0) / (v1 - v0);
        }
        return true;
    }

    /**
     * How scraped a face is, 0 to 1: its side's dent, by how near it is to that end or side (in the model's frame:
     * {@code a} along, {@code b} across) and how much it faces that way.
     */
    private float scraped(float a, float b, Vector3f n) {
        float most = 0.0F;
        float length = halfLength * 2.0F;
        float reach = Mth.clamp(length * 0.3F, 0.45F, 1.4F);
        float na = n.z * frontSign;
        float nb = n.x * leftSign;
        most = Math.max(most, damage.front() * near(a, ends[0], reach) * facing(na));
        most = Math.max(most, damage.rear() * near(-a, ends[1], reach) * facing(-na));
        most = Math.max(most, damage.left() * near(b, ends[2], 0.5F) * facing(nb));
        most = Math.max(most, damage.right() * near(-b, ends[3], 0.5F) * facing(-nb));
        return most;
    }

    /** 1 at the {@code edge} (or past it), falling to 0 {@code reach} in from it. */
    private static float near(float at, float edge, float reach) {
        return Mth.clamp((at - (edge - reach)) / reach, 0.0F, 1.0F);
    }

    /** Faces turned toward a side get its scrapes; tops and edges a little, faces turned away none. */
    private static float facing(float toward) {
        return Mth.clamp(0.45F + toward, 0.0F, 1.0F);
    }

    // ---- the textures ----

    private boolean load() {
        if (scrapes != null) {
            return true;
        }
        if (failed) {
            return false;
        }
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(source); NativeImage image = NativeImage.read(in)) {
            int w = image.getWidth();
            int h = image.getHeight();
            long noiseSeed = source.hashCode() * 31L + 7;
            boolean[] glassTexel = new boolean[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int alpha = image.getPixelRGBA(x, y) >>> 24;
                    glassTexel[y * w + x] = alpha > 0 && alpha < 255;
                }
            }
            String name = source.getPath().replace('/', '_').replace(".png", "");
            scrapes = new ResourceLocation[STAGES];
            lateScrapes = new CarGlass[STAGES];
            for (int stage = 1; stage <= STAGES; stage++) {
                float f = stage / (float) STAGES;
                NativeImage over = new NativeImage(w, h, true);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int alpha = image.getPixelRGBA(x, y) >>> 24;
                        if (alpha < 255) {
                            over.setPixelRGBA(x, y, 0);
                            continue;
                        }
                        // Chipped paint showing bare metal, scratches along the panels, and dark creases.
                        float chip = 0.6F * smooth(x, y, 3, noiseSeed) + 0.4F * hash(x, y, noiseSeed + 1);
                        boolean scratchRow = hash(0, y, noiseSeed + 2) > 0.62F;
                        float scratch = smooth(x, y * 7, 4, noiseSeed + 3);
                        float crease = 0.7F * smooth(x, y, 5, noiseSeed + 4) + 0.3F * hash(x, y, noiseSeed + 5);
                        int color;
                        if (chip < f * 0.2F) {
                            // bare steel where the paint's chipped off
                            int grey = 112 + (int) (36 * hash(x, y, noiseSeed + 6));
                            color = 240 << 24 | (grey + 4) << 16 | grey << 8 | (grey - 2);
                        } else if (scratchRow && scratch > 1.0F - f * 0.45F) {
                            // a scratch along the panel, showing bright metal
                            int grey = 168 + (int) (34 * hash(x, y, noiseSeed + 7));
                            color = 200 << 24 | grey << 16 | grey << 8 | grey;
                        } else if (crease < f * 0.32F) {
                            // a crease's shadow
                            int a = (int) (30 + 50 * f);
                            color = a << 24 | 18 << 16 | 18 << 8 | 20;
                        } else {
                            color = 0;
                        }
                        over.setPixelRGBA(x, y, color); // ABGR: greys read the same either way
                    }
                }
                ResourceLocation id = ReikoCars.id("dynamic/scrapes/" + name + "_" + stage);
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(over));
                scrapes[stage - 1] = id;
                lateScrapes[stage - 1] = new CarGlass(source, id);
            }
            // The frame of a shattered window: the car's texture with the glass taken out.
            NativeImage frame = new NativeImage(w, h, true);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    frame.setPixelRGBA(x, y, glassTexel[y * w + x] ? 0 : image.getPixelRGBA(x, y));
                }
            }
            ResourceLocation frameId = ReikoCars.id("dynamic/paneless/" + name);
            Minecraft.getInstance().getTextureManager().register(frameId, new DynamicTexture(frame));
            paneless = new CarGlass(source, frameId);
            loadPanes();
            return true;
        } catch (IOException | RuntimeException e) {
            ReikoCars.LOGGER.warn("Couldn't make the crash damage for {}", source, e);
            failed = true;
            scrapes = null;
            return false;
        }
    }

    private static final int CRACK_PATTERNS = 2;
    /** The crack patterns and the shards, shared by every car: drawn across a pane's face. */
    private static CarGlass[] sharedCracks;
    private static CarGlass sharedShards;

    private void loadPanes() {
        if (sharedCracks == null) {
            sharedCracks = new CarGlass[CRACK_PATTERNS];
            for (int pattern = 0; pattern < CRACK_PATTERNS; pattern++) {
                ResourceLocation id = ReikoCars.id("dynamic/cracks/pattern_" + pattern);
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(crackPattern(pattern)));
                sharedCracks[pattern] = new CarGlass(source, id);
            }
            ResourceLocation id = ReikoCars.id("dynamic/cracks/shards");
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(shardPattern()));
            sharedShards = new CarGlass(source, id);
        }
        cracks = sharedCracks;
        shards = sharedShards;
    }

    /**
     * Cracked glass: a crazed star round where it was hit, cracks running out from it, rings of shorter ones round it,
     * and a few stray cracks off to the edges.
     */
    private static NativeImage crackPattern(int pattern) {
        int size = PANE_SIZE;
        NativeImage image = new NativeImage(size, size, true);
        image.fillRect(0, 0, size, size, 0);
        java.util.Random random = new java.util.Random(4711L + pattern * 977L);
        float cx = size * (pattern == 0 ? 0.36F : 0.62F);
        float cy = size * (pattern == 0 ? 0.42F : 0.55F);
        int rays = 13 + random.nextInt(5);
        float[] angles = new float[rays];
        for (int r = 0; r < rays; r++) {
            angles[r] = (r + random.nextFloat() * 0.6F) * Mth.TWO_PI / rays;
            float angle = angles[r];
            float x = cx;
            float y = cy;
            float length = size * (0.35F + random.nextFloat() * 0.6F);
            for (float run = 0.0F; run < length; run += 1.0F) {
                angle += (random.nextFloat() - 0.5F) * 0.12F;
                x += Mth.cos(angle);
                y += Mth.sin(angle);
                crackDot(image, x, y, run < 10 ? 230 : 200 - (int) (run / length * 90));
            }
        }
        // Rings: short cracks between neighbouring rays.
        for (float radius = 7.0F; radius < size * 0.32F; radius *= 1.55F) {
            for (int r = 0; r < rays; r++) {
                if (random.nextFloat() < 0.35F) {
                    continue;
                }
                float a0 = angles[r];
                float a1 = r + 1 < rays ? angles[r + 1] : angles[0] + Mth.TWO_PI;
                float wobble = radius * (0.9F + random.nextFloat() * 0.25F);
                for (float t = 0.0F; t <= 1.0F; t += 1.0F / (wobble * (a1 - a0) + 1.0F)) {
                    float angle = a0 + (a1 - a0) * t;
                    crackDot(image, cx + Mth.cos(angle) * wobble, cy + Mth.sin(angle) * wobble, 170);
                }
            }
        }
        // The crazed spot where it was hit.
        for (int y = -5; y <= 5; y++) {
            for (int x = -5; x <= 5; x++) {
                if (x * x + y * y <= 22 && random.nextFloat() < 0.55F) {
                    crackDot(image, cx + x, cy + y, 215);
                }
            }
        }
        return image;
    }

    private static void crackDot(NativeImage image, float x, float y, int alpha) {
        int ix = Mth.floor(x);
        int iy = Mth.floor(y);
        if (ix < 0 || iy < 0 || ix >= image.getWidth() || iy >= image.getHeight()) {
            return;
        }
        int was = image.getPixelRGBA(ix, iy) >>> 24;
        if (alpha > was) {
            image.setPixelRGBA(ix, iy, alpha << 24 | 250 << 16 | 247 << 8 | 244); // ABGR: a pale, bluish white
        }
    }

    /** What's left of a shattered window: jagged shards round the frame, cracked across, the middle gone. */
    private static NativeImage shardPattern() {
        int size = PANE_SIZE;
        NativeImage image = new NativeImage(size, size, true);
        long seed = 90210L;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float[] cell = cells(x + 0.5F, y + 0.5F, 17.0F, seed);
                int edgeDistance = Math.min(Math.min(x, y), Math.min(size - 1 - x, size - 1 - y));
                // A shard is kept if its cell reaches near the frame (how near, varying by cell), and a few odd ones.
                float reach = 6.0F + cell[2] * 22.0F;
                boolean kept = edgeDistance < reach - cell[0] * 0.9F || cell[2] > 0.96F;
                if (!kept) {
                    image.setPixelRGBA(x, y, 0);
                } else if (cell[1] - cell[0] < 1.1F) {
                    image.setPixelRGBA(x, y, 225 << 24 | 246 << 16 | 242 << 8 | 238); // its broken edge
                } else {
                    int tint = 175 + (int) (25 * cell[2]);
                    image.setPixelRGBA(x, y, 110 << 24 | (tint + 30) << 16 | (tint + 20) << 8 | tint);
                }
            }
        }
        return image;
    }

    /** Value noise: smooth blotches about {@code size} texels across, 0 to 1. */
    private static float smooth(int x, int y, int size, long seed) {
        return noise(x / (float) size, y / (float) size, seed);
    }

    /**
     * Cells round scattered points {@code size} texels apart: the distances to the nearest point and the next, and a
     * number for the nearest point's cell, 0 to 1.
     */
    private static float[] cells(float x, float y, float size, long seed) {
        int cx = Mth.floor(x / size);
        int cy = Mth.floor(y / size);
        float first = Float.MAX_VALUE;
        float second = Float.MAX_VALUE;
        float id = 0.0F;
        for (int j = cy - 1; j <= cy + 1; j++) {
            for (int i = cx - 1; i <= cx + 1; i++) {
                float px = (i + hash(i, j, seed)) * size;
                float py = (j + hash(i, j, seed + 1)) * size;
                float d = Mth.sqrt((px - x) * (px - x) + (py - y) * (py - y));
                if (d < first) {
                    second = first;
                    first = d;
                    id = hash(i, j, seed + 2);
                } else if (d < second) {
                    second = d;
                }
            }
        }
        return new float[] {first, second, id};
    }

    // ---- passing faces through ----

    /** Faces: x, y, z, u, v and the normal (eight floats a vertex), colour, overlay and light (three ints). */
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

    /** Collects whole faces, four vertices at a time, from a model drawing itself. */
    private abstract static class QuadCollector implements VertexConsumer {
        final float[] floats = new float[32];
        final int[] ints = new int[12];
        private int count;

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
            ints[count * 3] = color;
            ints[count * 3 + 1] = overlay;
            ints[count * 3 + 2] = light;
            if (++count < 4) {
                return;
            }
            count = 0;
            quad();
        }

        /** A whole face is in {@link #floats} and {@link #ints}. */
        abstract void quad();

        void reset() {
            count = 0;
        }

        /** Whether any of the face is glass (so it's drawn with the glass, after the weather). */
        boolean isGlassFace(CarGlass glass) {
            return glassShare(glass) > 0.0F;
        }

        /** Whether the face is a pane: mostly glass, not a panel with a pane's seal on it. */
        boolean isPane(CarGlass glass) {
            return glassShare(glass) > 0.5F;
        }

        float glassShare(CarGlass glass) {
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
            return glass.glassShare(u0, v0, u1, v1);
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

    /** Bends each face by the dents on its way to the target, and leaves out (or holds back, or darkens) what its mode says. */
    private final class Bend extends QuadCollector {
        private VertexConsumer target;
        private int mode;
        final Quads held = new Quads();
        private final Vector3f scratch = new Vector3f();
        private final Vector3f shift = new Vector3f();
        /** The face's middle in the model's frame, before it's bent. */
        final Vector3f centre = new Vector3f();
        private final Vector3f normal = new Vector3f();

        Bend to(VertexConsumer target, int mode) {
            this.target = target;
            this.mode = mode;
            if (mode == LAMPS_LIT) {
                held.count = 0;
            }
            reset();
            return this;
        }

        @Override
        void quad() {
            centre.set(0.0F);
            for (int i = 0; i < 4; i++) {
                int f = i * 8;
                toModel.transformPosition(floats[f], floats[f + 1], floats[f + 2], scratch);
                centre.add(scratch);
                displacement(scratch.x, scratch.y, scratch.z, shift);
                scratch.add(shift);
                modelPose.transformPosition(scratch);
                floats[f] = scratch.x;
                floats[f + 1] = scratch.y;
                floats[f + 2] = scratch.z;
            }
            centre.div(4.0F);
            switch (mode) {
                case SOLID -> {
                    if (damage.windows() != 0 && isPane(glass)) {
                        toModel.transformDirection(floats[5], floats[6], floats[7], normal);
                        int window = windowFacing(normal.normalize());
                        if (window >= 0 && damage.window(window) == CarDamage.SHATTERED) {
                            return; // gone: only its shards are drawn (see drawOverlays)
                        }
                    }
                }
                case LAMPS_LIT -> {
                    if (damage.lightBroken(lampAt(centre.x, centre.z))) {
                        held.add(floats, ints);
                        return;
                    }
                }
                case LAMPS_OFF -> {
                    if (damage.lightBroken(lampAt(centre.x, centre.z))) {
                        for (int i = 0; i < 4; i++) {
                            ints[i * 3] = darker(ints[i * 3]);
                        }
                    }
                }
                case SIGNAL -> {
                    if (damage.lightBroken(lampAt(centre.x, centre.z))) {
                        return;
                    }
                }
                default -> {
                }
            }
            for (int i = 0; i < 4; i++) {
                int f = i * 8;
                int n = i * 3;
                target.addVertex(floats[f], floats[f + 1], floats[f + 2], ints[n], floats[f + 3], floats[f + 4], ints[n + 1], ints[n + 2],
                        floats[f + 5], floats[f + 6], floats[f + 7]);
            }
        }
    }

    /** Files the (bent) faces of the body under their scrape stage, and the broken windows' glass under cracked or shattered. */
    private final class Sorter extends QuadCollector {
        final Quads[] scraped = new Quads[STAGES + 1];
        final Quads[] scrapedLate = new Quads[STAGES + 1];
        final Quads cracked = new Quads();
        final Quads shattered = new Quads();
        /** Which crack pattern each cracked face gets (by its window, so they aren't all alike). */
        int[] crackVariant = new int[16];
        /** A face being drawn with a pane texture across it. */
        final float[] paneUv = new float[32];
        private final Vector3f centre = new Vector3f();
        private final Vector3f normal = new Vector3f();

        Sorter() {
            for (int i = 0; i <= STAGES; i++) {
                scraped[i] = new Quads();
                scrapedLate[i] = new Quads();
            }
        }

        void start() {
            reset();
            for (int i = 0; i <= STAGES; i++) {
                scraped[i].count = 0;
                scrapedLate[i].count = 0;
            }
            cracked.count = 0;
            shattered.count = 0;
        }

        @Override
        void quad() {
            // Where the face was before it was bent (the bend hands each face on as it works it out).
            centre.set(bend.centre);
            toModel.transformDirection(floats[5], floats[6], floats[7], normal);
            normal.normalize();
            for (int i = 0; i < 4; i++) {
                ints[i * 3] = 0xFFFFFFFF;
            }
            if (isPane(glass)) {
                int window = windowFacing(normal);
                int state = window < 0 ? CarDamage.WHOLE : damage.window(window);
                if (state == CarDamage.CRACKED) {
                    if (cracked.count >= crackVariant.length) {
                        crackVariant = Arrays.copyOf(crackVariant, crackVariant.length * 2);
                    }
                    crackVariant[cracked.count] = Math.floorMod(window + (int) seed, CRACK_PATTERNS);
                    cracked.add(floats, ints);
                } else if (state == CarDamage.SHATTERED) {
                    shattered.add(floats, ints);
                }
                return;
            }
            float how = scraped(centre.z * frontSign, centre.x * leftSign, normal);
            int stage = Math.min(STAGES, (int) Math.ceil(how * STAGES - 0.25F));
            if (stage > 0) {
                // A panel with a pane's seal on it is drawn with the glass, so its scrapes wait with it.
                (isGlassFace(glass) ? scrapedLate : scraped)[stage].add(floats, ints);
            }
        }
    }

    private static class Sink implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return this;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY,
                              float normalZ) {
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
