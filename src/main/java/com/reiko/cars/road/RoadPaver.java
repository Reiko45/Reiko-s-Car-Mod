package com.reiko.cars.road;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlocks;
import com.reiko.cars.registry.ModItems;
import com.reiko.economy.core.EconomyService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The road paver at work (on the server). With its screed down (G), it lays a road behind it as it's driven: asphalt
 * the width the driver set, painted with a center line, lane lines and edge lines, with raised sidewalks and street
 * lamps along the sides. Shaping the land, it also works the ground just ahead of it to an even grade, cutting through
 * hills, filling dips, and running level over water and gaps as a bridge on pillars; otherwise the road lies on the
 * ground as it is.
 *
 * <p>It works in cross-sections every {@link #STEP} blocks along its path, one pass ahead of its front bumper getting
 * the ground ready and one at its back laying the road. Driven straight (and the paver steers itself straight, to the
 * nearest 45 degrees, while the driver doesn't), the road follows a line through block centers, so a road at 45
 * degrees gets its lines from corner to corner of each block, one straight line; round a bend it follows the paver.
 *
 * <p>It takes what it lays from its hopper, then from the driver's pockets, and if it's set to, pays for the rest from
 * the driver's bank account (in creative it's free). What it digs out goes into its hopper while there's room: the
 * ground (to fill with later), ores and logs. Ores and logs that don't fit are dropped beside the road; the rest is
 * cleared away.
 */
public final class RoadPaver {
    /** Cross-sections, in blocks of the path apart. */
    public static final double STEP = 0.4;
    /** Clear room above the road, and above sidewalks and the verges. */
    public static final int HEADROOM = 4;
    public static final int SIDE_HEADROOM = 3;
    /** Trees and sand over the road are cleared this high up, so they don't hang over it or fall onto it. */
    public static final int TREE_HEADROOM = 14;
    /** How deep a dip it fills; anything deeper (or water) it bridges, with the road deck on pillars. */
    public static final int FILL_DEPTH = 4;
    /** The steepest the road climbs or falls when it shapes the land: a block in four. */
    public static final double MAX_SLOPE = 0.25;
    /** How far ahead of the ground it's working on it looks to see where the road should go. */
    public static final double LOOKAHEAD = 3.0;
    /** Bridge pillars, this many blocks apart, at most this deep. */
    public static final int PILLAR_EVERY = 8;
    public static final int PILLAR_DEPTH = 48;
    /** Street poles under each lamp. */
    public static final int POLE_HEIGHT = 4;
    /** How far ahead of the front bumper the ground is got ready. */
    public static final double FRONT_REACH = 1.5;
    /** How near (in degrees) to a multiple of 45 the paver must be heading to follow a straight line, and for how many ticks. */
    public static final float STRAIGHT = 0.5F;
    public static final int STRAIGHT_TICKS = 3;
    /** Beside a cut or a bank, the ground's sloped off a block out for each block up or down, this far out from the road. */
    public static final int BATTER = 8;
    /** How high over the road a cut's slopes are cleared. */
    public static final int CUT_HEIGHT = 16;
    /** Blocks the paver can dig into its hopper and fill with: earth and stone. */
    public static final TagKey<Block> FILL_BLOCKS = BlockTags.create(ReikoCars.id("paver_fill"));
    public static final TagKey<Item> FILL_ITEMS = ItemTags.create(ReikoCars.id("paver_fill"));

    private RoadPaver() {
    }

    /** What a paver's doing with a column of blocks across the road. */
    public enum Role {
        ROAD,
        CURB,
        SIDEWALK,
        /** Where a lamp stands beside a road with no sidewalk. */
        VERGE,
        /** Beyond the road and its sidewalks: a bridge's railing, or a cut's or a bank's slope. */
        BATTER
    }

    /** What it lays, and where it finds it. */
    public enum Material {
        ASPHALT,
        ASPHALT_SLAB,
        PAINT,
        CURB,
        SIDEWALK,
        FILL,
        POLE,
        LAMP,
        RAILING;

        public Component displayName() {
            return Component.translatable("gui.reikocars.paver.material." + name().toLowerCase(java.util.Locale.ROOT));
        }

        /** Its price, in cents, when the paver pays for it. */
        public long price() {
            double dollars = switch (this) {
                case ASPHALT -> CarsConfig.get(CarsConfig.PAVER_ASPHALT_PRICE);
                case ASPHALT_SLAB -> CarsConfig.get(CarsConfig.PAVER_ASPHALT_PRICE) / 2.0;
                case PAINT -> CarsConfig.get(CarsConfig.PAVER_PAINT_PRICE);
                case CURB, SIDEWALK -> CarsConfig.get(CarsConfig.PAVER_SLAB_PRICE);
                case FILL -> CarsConfig.get(CarsConfig.PAVER_FILL_PRICE);
                case POLE -> CarsConfig.get(CarsConfig.PAVER_POLE_PRICE);
                case LAMP -> CarsConfig.get(CarsConfig.PAVER_LAMP_PRICE);
                case RAILING -> CarsConfig.get(CarsConfig.PAVER_RAILING_PRICE);
            };
            return Math.max(0L, Math.round(dollars * 100.0));
        }
    }

    /** A paver's road in progress. Kept on the paver, but not saved: a paver starts afresh when the world loads. */
    public static final class State {
        final Pass front = new Pass();
        final Pass rear = new Pass();
        /** The road's height (where its surface is) at the front pass, in blocks, before it's rounded to half blocks. */
        double grade = Double.NaN;
        @Nullable
        Line line;
        /** Each column's road surface height, in half blocks, as the front pass set it, for the back pass. */
        final Map<Long, Integer> levels = new LinkedHashMap<>(4096, 0.75F, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Integer> eldest) {
                return size() > 16384;
            }
        };
        /** The columns it's bridged (a deck over a gap), so a cross-section that sees one again still knows. */
        final Map<Long, Boolean> bridges = new LinkedHashMap<>(256, 0.75F, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                return size() > 4096;
            }
        };
        /** The road blocks laid this run, by column, and the rank of the line painted on each (edges first). */
        final Map<Long, Long> roads = new LinkedHashMap<>(4096, 0.75F, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Long> eldest) {
                return size() > 16384;
            }
        };
        final Map<Long, Integer> painted = new LinkedHashMap<>(4096, 0.75F, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Integer> eldest) {
                return size() > 16384;
            }
        };
        /** Each line down the road, followed block by block; and the settings they were set up for. */
        final List<Track> tracks = new ArrayList<>();
        @Nullable
        PaverSettings tracksFor;

        /** The path the paver's turned about (its drive wheels), for the back pass to follow. */
        final java.util.ArrayDeque<Trail> trail = new java.util.ArrayDeque<>();

        /** The point {@code distance} along the trail (dropping what's behind it, which is done with). */
        Vec3 trailAt(double distance) {
            while (trail.size() > 2) {
                java.util.Iterator<Trail> it = trail.iterator();
                it.next();
                if (it.next().distance() <= distance) {
                    trail.pollFirst();
                } else {
                    break;
                }
            }
            Trail a = null;
            for (Trail b : trail) {
                if (a != null && b.distance() >= distance) {
                    double t = b.distance() - a.distance() < 1.0E-9 ? 1.0 : (distance - a.distance()) / (b.distance() - a.distance());
                    t = Mth.clamp(t, 0.0, 1.0);
                    return new Vec3(Mth.lerp(t, a.at().x, b.at().x), b.at().y, Mth.lerp(t, a.at().z, b.at().z));
                }
                a = b;
            }
            return trail.peekLast().at();
        }

        /** Ticks it's been heading straight along a multiple of 45 degrees, and which way it was heading last tick. */
        int straightTicks;
        float lastYaw = Float.NaN;
        double nextLamp;
        int lampIndex;
        double nextPillar;
        /** Money spent this run (shown on the screen), and spent this tick but not yet taken. In cents. */
        long spent;
        long owed;
        /** Blocks of road laid this run. */
        long laid;

        void reset() {
            front.at = null;
            rear.at = null;
            front.distance = 0.0;
            rear.distance = 0.0;
            grade = Double.NaN;
            line = null;
            levels.clear();
            bridges.clear();
            roads.clear();
            painted.clear();
            tracks.clear();
            tracksFor = null;
            trail.clear();
            straightTicks = 0;
            lastYaw = Float.NaN;
            lampIndex = 0;
            owed = 0;
        }

        /** Money spent since the screed last went down, in cents. */
        public long spent() {
            return spent;
        }

        public long laid() {
            return laid;
        }
    }

    /**
     * One line down the road, followed from block to block as the paver goes: the block it's in, the anchor (in half
     * blocks, world-wide) where it came into it, and the last point it was at. Each block's piece is painted as the line
     * leaves it, from where it came in to where it goes out, so the next block's piece starts where it ended.
     */
    static final class Track {
        final Job.Stripe stripe;
        final int rank;
        @Nullable
        Vec3 last;
        int x;
        int z;
        int[] entry;
        /**
         * The piece painted ahead of time in the block it's in, as if the line went straight on (so the lines show as
         * the road's laid, and the last block has them when it stops); null if none. Painted again as it leaves.
         */
        @Nullable
        RoadMarking.Piece ahead;

        Track(Job.Stripe stripe, int rank) {
            this.stripe = stripe;
            this.rank = rank;
        }
    }

    /** A point on the path the paver's turned about, and how far along the path it is. */
    record Trail(Vec3 at, double distance) {
    }

    /** One pass's progress along the path: where it's got to, which way it was going, and how far it's come. */
    static final class Pass {
        @Nullable
        Vec3 at;
        float yaw;
        double distance;
    }

    /** The straight line a paver heading at a multiple of 45 degrees follows, through a block's center. */
    record Line(Vec3 origin, Vec3 dir, float yaw) {
        Vec3 project(Vec3 point) {
            double t = (point.x - origin.x) * dir.x + (point.z - origin.z) * dir.z;
            return new Vec3(origin.x + dir.x * t, point.y, origin.z + dir.z * t);
        }
    }

    /** Ran out of something it can't do without. */
    static final class OutOf extends RuntimeException {
        final Component message;

        OutOf(Component message) {
            super(null, null, false, false);
            this.message = message;
        }
    }

    // ---- turning it on and off ----

    /** The driver's G: the screed down (paving) or up. */
    public static void toggle(ServerPlayer driver, CarEntity paver) {
        if (paver.paving()) {
            stop(paver, driver, Component.translatable("paver.reikocars.stopped", money(paver.paverState().spent)));
            return;
        }
        if (paver.isInWater()) {
            driver.displayClientMessage(Component.translatable("paver.reikocars.not_in_water"), true);
            return;
        }
        paver.paverState().reset();
        paver.paverState().spent = 0;
        paver.paverState().laid = 0;
        paver.setPaving(true);
        PaverSettings settings = paver.paverSettings();
        driver.displayClientMessage(Component.translatable(settings.shape() ? "paver.reikocars.started_shaping" : "paver.reikocars.started",
                settings.width()), true);
        paver.level().playSound(null, paver.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.NEUTRAL, 0.6F, 0.7F);
    }

    /** Raises the screed, telling the driver {@code why} if there's a driver to tell. */
    public static void stop(CarEntity paver, @Nullable ServerPlayer driver, @Nullable Component why) {
        paver.setPaving(false);
        paver.paverState().reset();
        if (driver != null && why != null) {
            driver.displayClientMessage(why, true);
        }
        paver.level().playSound(null, paver.blockPosition(), SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 0.6F, 0.7F);
    }

    static Component money(long cents) {
        return Component.literal(EconomyService.INSTANCE.format(cents));
    }

    // ---- every tick ----

    /** A paver's tick on the server: paves if its screed's down and it's being driven forward. */
    public static void tick(CarEntity paver) {
        State state = paver.paverState();
        if (!paver.paving()) {
            return;
        }
        if (!(paver.getControllingPassenger() instanceof ServerPlayer driver)) {
            stop(paver, null, null); // the driver got out: the screed goes up
            return;
        }
        ServerLevel level = (ServerLevel) paver.level();
        float yaw = paver.getYRot();
        Line line = line(state, paver);
        Vec3 forward = forward(yaw);
        double half = paver.carType().length / 2.0;
        Vec3 frontPoint = paver.position().add(forward.scale(half + FRONT_REACH));
        // The road's laid along the path the paver turns about (its drive wheels), which bends smoothly, as far back as
        // the screed: its back end swings out round a bend, so its own path wouldn't.
        Vec3 pivot = paver.position().subtract(forward.scale(paver.carType().pivotBack()));
        double lag = half - paver.carType().pivotBack() - 0.25;
        Job job = new Job(level, paver, driver, paver.paverSettings(), state);
        if (state.front.at == null) {
            // Just started: the road starts at the paver's own level, under its back end.
            Vec3 rearPoint = pivot.subtract(forward.scale(lag));
            state.grade = Math.round(paver.getY() * 2.0) / 2.0;
            state.front.at = rearPoint.subtract(forward.scale(STEP));
            state.front.yaw = yaw;
            state.rear.at = rearPoint.subtract(forward.scale(STEP));
            state.rear.yaw = yaw;
            state.trail.clear();
            state.trail.add(new Trail(rearPoint.subtract(forward.scale(STEP)), -lag - STEP));
            state.trail.add(new Trail(pivot, 0.0));
            state.nextLamp = job.settings.lampSpacing() / 2.0;
            state.lampIndex = 0;
            state.nextPillar = PILLAR_EVERY / 2.0;
        }
        Trail last = state.trail.peekLast();
        double moved = Math.sqrt(Mth.square(pivot.x - last.at().x) + Mth.square(pivot.z - last.at().z));
        if (moved > 0.01 && (pivot.x - last.at().x) * forward.x + (pivot.z - last.at().z) * forward.z > 0.0) {
            state.trail.add(new Trail(pivot, last.distance() + moved));
        }
        Vec3 rearPoint = state.trailAt(state.trail.peekLast().distance() - lag);
        try {
            if (job.settings.shape()) {
                advance(state.front, frontPoint, yaw, line, job.extent(true), job::prepare);
            }
            advance(state.rear, rearPoint, yaw, line, job.extent(false), job::lay);
        } catch (OutOf out) {
            job.settle();
            stop(paver, driver, out.message);
            return;
        }
        job.settle();
    }

    /** Where a pass is now: each cross-section from where it was to {@code to}, if it's gone forward. */
    private static void advance(Pass pass, Vec3 to, float yaw, @Nullable Line line, double extent, Section section) {
        Vec3 from = pass.at;
        float fromYaw = pass.yaw;
        if (line != null) {
            to = line.project(to);
            from = line.project(from);
            yaw = line.yaw;
            fromYaw = line.yaw;
        }
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        Vec3 forward = forward(yaw);
        if (dx * forward.x + dz * forward.z <= 0.01) {
            return; // standing still or backing up: nothing to do until it's going forward past where it was
        }
        if (distance > 12.0) {
            // It jumped (teleported, or turned right round): start again here.
            from = to.subtract(forward.scale(STEP));
            fromYaw = yaw;
            dx = to.x - from.x;
            dz = to.z - from.z;
            distance = STEP;
        }
        if (line == null && distance > 1.0E-3) {
            // Round a bend, the back (or front) of the paver doesn't go quite the way the paver faces: it swings out. The
            // road there runs the way that end's actually going.
            yaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        }
        float turn = Mth.wrapDegrees(yaw - fromYaw);
        // Enough cross-sections that they're STEP apart along the middle, and round the outside of a bend too.
        int count = Mth.clamp((int) Math.ceil(Math.max(distance / STEP, Math.abs(turn) * Mth.DEG_TO_RAD * extent / STEP)), 1, 128);
        for (int i = 1; i <= count; i++) {
            double t = i / (double) count;
            Vec3 at = new Vec3(from.x + dx * t, to.y, from.z + dz * t);
            float sectionYaw = fromYaw + turn * (float) t;
            pass.distance += distance / count;
            section.at(at, forward(sectionYaw), left(sectionYaw), sectionYaw, distance / count, line != null);
        }
        pass.at = to;
        pass.yaw = yaw;
    }

    @FunctionalInterface
    interface Section {
        /** One cross-section; {@code straight} if it's on the straight line through block centers. */
        void at(Vec3 point, Vec3 forward, Vec3 left, float yaw, double step, boolean straight);
    }

    /** The line the paver's following, if it's heading straight along a multiple of 45 degrees. */
    @Nullable
    private static Line line(State state, CarEntity paver) {
        float yaw = paver.getYRot();
        float snapped = Math.round(yaw / 45.0F) * 45.0F;
        boolean steady = Float.isNaN(state.lastYaw) || Math.abs(Mth.wrapDegrees(yaw - state.lastYaw)) < 0.05F;
        boolean first = Float.isNaN(state.lastYaw);
        state.lastYaw = yaw;
        if (Math.abs(Mth.wrapDegrees(yaw - snapped)) > STRAIGHT || !steady) {
            state.straightTicks = 0;
            state.line = null;
            return null;
        }
        // Coming out of a bend, it's held straight a moment before the road snaps to the line; starting off straight, at once.
        state.straightTicks = first ? STRAIGHT_TICKS : state.straightTicks + 1;
        if (state.straightTicks < STRAIGHT_TICKS) {
            return null;
        }
        if (state.line == null || Math.abs(Mth.wrapDegrees(state.line.yaw - snapped)) > 0.01F) {
            Vec3 c = paver.position();
            state.line = new Line(new Vec3(Math.floor(c.x) + 0.5, c.y, Math.floor(c.z) + 0.5), forward(snapped), snapped);
        }
        return state.line;
    }

    /** The way a paver facing {@code yaw} degrees goes, flat (as {@link CarEntity#forward()}). */
    public static Vec3 forward(float yaw) {
        double r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Math.sin(r), 0.0, Math.cos(r));
    }

    /** To the left of a paver facing {@code yaw} (as {@link CarEntity#left()}). */
    public static Vec3 left(float yaw) {
        double r = yaw * Mth.DEG_TO_RAD;
        return new Vec3(Math.cos(r), 0.0, Math.sin(r));
    }

    /** How far the road reaches left of the line down its middle, and right of it (an even width's half a block more on the left). */
    public static double roadLeft(PaverSettings settings) {
        return settings.width() / 2.0 + (settings.width() % 2 == 0 ? 0.5 : 0.0);
    }

    public static double roadRight(PaverSettings settings) {
        return settings.width() / 2.0 - (settings.width() % 2 == 0 ? 0.5 : 0.0);
    }

    // ---- one tick's work ----

    /** One tick of paving: the settings, the ground, the materials and the bill. */
    static final class Job {
        final ServerLevel level;
        final CarEntity paver;
        final ServerPlayer driver;
        final PaverSettings settings;
        final State state;
        final boolean free;
        final double roadLeft;
        final double roadRight;
        final int sidewalkLeft;
        final int sidewalkRight;
        final int vergeLeft;
        final int vergeRight;
        /** The lines it paints, edges first (they win where two would share a block). */
        final List<Stripe> stripes = new ArrayList<>();

        Job(ServerLevel level, CarEntity paver, ServerPlayer driver, PaverSettings settings, State state) {
            this.level = level;
            this.paver = paver;
            this.driver = driver;
            this.settings = settings;
            this.state = state;
            this.free = driver.getAbilities().instabuild;
            this.roadLeft = RoadPaver.roadLeft(settings);
            this.roadRight = RoadPaver.roadRight(settings);
            this.sidewalkLeft = settings.sidewalks().left() ? settings.sidewalkWidth() : 0;
            this.sidewalkRight = settings.sidewalks().right() ? settings.sidewalkWidth() : 0;
            this.vergeLeft = settings.lamps() && settings.lampSides().any(true) && sidewalkLeft == 0 ? 1 : 0;
            this.vergeRight = settings.lamps() && settings.lampSides().any(false) && sidewalkRight == 0 ? 1 : 0;
            if (settings.edges()) {
                stripes.add(new Stripe(roadLeft - 0.5, RoadMarking.Line.EDGE));
                stripes.add(new Stripe(-(roadRight - 0.5), RoadMarking.Line.EDGE));
            }
            if (settings.center() != PaverSettings.Center.NONE) {
                stripes.add(new Stripe(0.0, settings.center() == PaverSettings.Center.DOUBLE ? RoadMarking.Line.CENTER_SOLID : RoadMarking.Line.CENTER_DASHED));
            }
            int lanes = settings.lanesThatFit();
            for (int k = 1; k < lanes; k++) {
                stripes.add(new Stripe(k * roadLeft / lanes, RoadMarking.Line.LANE));
                stripes.add(new Stripe(-k * roadRight / lanes, RoadMarking.Line.LANE));
            }
        }

        /** A line down the road, {@code offset} blocks left of its middle. */
        record Stripe(double offset, RoadMarking.Line line) {
        }

        double extentLeft() {
            return roadLeft + sidewalkLeft + vergeLeft;
        }

        double extentRight() {
            return roadRight + sidewalkRight + vergeRight;
        }

        /** How far out either side a pass works; the front one shapes the slopes beside the road too. */
        double extent(boolean front) {
            double reach = Math.max(extentLeft(), extentRight());
            return front ? reach + BATTER : reach;
        }

        Role role(double o) {
            if (o < roadLeft && o > -roadRight) {
                return Role.ROAD;
            }
            boolean onLeft = o > 0;
            double out = onLeft ? o - roadLeft : -o - roadRight;
            int sidewalk = onLeft ? sidewalkLeft : sidewalkRight;
            if (out < sidewalk) {
                return out < 1.0 ? Role.CURB : Role.SIDEWALK;
            }
            if (out < sidewalk + (onLeft ? vergeLeft : vergeRight)) {
                return Role.VERGE;
            }
            return Role.BATTER;
        }

        /** How far beyond the road and its sidewalks (and verge) a column is, in blocks, or 0 inside it. */
        double beyond(double o) {
            return o > 0 ? Math.max(0.0, o - extentLeft()) : Math.max(0.0, -o - extentRight());
        }

        /** Each block column across the road at {@code point}, between {@code left} blocks to its left and {@code right} to its right. */
        void columns(Vec3 point, Vec3 forward, Vec3 side, double left, double right, ColumnAction action) {
            double x0 = Math.min(point.x + side.x * left, point.x - side.x * right) - 1.0;
            double x1 = Math.max(point.x + side.x * left, point.x - side.x * right) + 1.0;
            double z0 = Math.min(point.z + side.z * left, point.z - side.z * right) - 1.0;
            double z1 = Math.max(point.z + side.z * left, point.z - side.z * right) + 1.0;
            for (int x = Mth.floor(x0); x <= Mth.floor(x1); x++) {
                for (int z = Mth.floor(z0); z <= Mth.floor(z1); z++) {
                    double cx = x + 0.5 - point.x;
                    double cz = z + 0.5 - point.z;
                    double along = cx * forward.x + cz * forward.z;
                    double o = cx * side.x + cz * side.z;
                    if (Math.abs(along) <= 0.5 && o < left - 1.0E-6 && o > -right + 1.0E-6) {
                        action.at(x, z, o, along);
                    }
                }
            }
        }

        // ---- the front: getting the ground ready ----

        /** The ground ahead of the paver at {@code point}: the grade worked out, then each column cut and filled to it. */
        void prepare(Vec3 point, Vec3 forward, Vec3 side, float yaw, double step, boolean straight) {
            double target = terrainAhead(point.add(forward.scale(LOOKAHEAD)), side);
            if (!Double.isNaN(target)) {
                if (target < state.grade - FILL_DEPTH) {
                    target = state.grade; // a gap or deep water ahead: carry on level, as a bridge
                }
                state.grade += Mth.clamp(target - state.grade, -MAX_SLOPE * step, MAX_SLOPE * step);
            }
            int here = (int) Math.round(state.grade * 2.0);
            boolean[] bridge = {false};
            // The railing's row is one block wide across the road's line: one block along the grid, two at 45 degrees
            // (a staircase, so the wall joins up).
            double railing = Math.abs(side.x) + Math.abs(side.z);
            List<long[]> beside = new ArrayList<>();
            columns(point, forward, side, extentLeft() + BATTER, extentRight() + BATTER, (x, z, o, along) -> {
                double beyond = beyond(o);
                if (beyond > 0.0) {
                    beside.add(new long[] {x, z, Double.doubleToLongBits(o), Double.doubleToLongBits(along)});
                    return;
                }
                long key = key(x, z);
                Integer known = state.levels.get(key);
                int level2 = known != null ? known : here;
                if (known == null) {
                    state.levels.put(key, level2);
                }
                boolean bridged = prepareColumn(x, z, level2, role(o));
                if (bridged) {
                    state.bridges.put(key, Boolean.TRUE);
                } else {
                    bridged = state.bridges.containsKey(key);
                }
                if (bridged && Math.abs(o) < 1.0) {
                    bridge[0] = true;
                }
            });
            // Beside the road, once the road's ready: a bridge's railing, or the slopes of a cut or a bank. No bank beside
            // a bridge, though: the road's bridging that gap, so the land beside it stays as it is.
            for (long[] c : beside) {
                int x = (int) c[0];
                int z = (int) c[1];
                double o = Double.longBitsToDouble(c[2]);
                double along = Double.longBitsToDouble(c[3]);
                double beyond = beyond(o);
                if (beyond < railing && (state.bridges.containsKey(key(x, z)) || overGap(x, z, Math.floorDiv(here, 2)))) {
                    railing(x, z, Math.floorDiv(here, 2));
                } else {
                    double edge = o > 0 ? extentLeft() - 0.5 : -(extentRight() - 0.5);
                    Vec3 at = point.add(forward.scale(along)).add(side.scale(edge));
                    boolean bridged = state.bridges.containsKey(key(Mth.floor(at.x), Mth.floor(at.z)));
                    batter(x, z, Math.floorDiv(here, 2), (int) Math.ceil(beyond), !bridged);
                }
            }
            // Pillars under a bridge, at its edges, every so often.
            if (bridge[0]) {
                if (state.front.distance >= state.nextPillar) {
                    state.nextPillar = state.front.distance + PILLAR_EVERY;
                    for (double o : new double[] {roadLeft - 0.5, -roadRight + 0.5}) {
                        Vec3 at = point.add(side.scale(o));
                        pillar(Mth.floor(at.x), Mth.floor(at.z));
                    }
                }
            } else {
                state.nextPillar = state.front.distance + PILLAR_EVERY / 2.0;
            }
        }

        /** The median height of the ground across the road at {@code point}, as the road's surface would be; NaN if it can't tell. */
        double terrainAhead(Vec3 point, Vec3 side) {
            List<Double> heights = new ArrayList<>();
            int gaps = 0;
            int top = Mth.floor(state.grade) + 8;
            int bottom = Mth.floor(state.grade) - FILL_DEPTH - 3;
            for (double o = -roadRight + 0.5; o < roadLeft; o += 1.0) {
                Vec3 at = point.add(side.scale(o));
                int x = Mth.floor(at.x);
                int z = Mth.floor(at.z);
                if (!level.isLoaded(new BlockPos(x, top, z))) {
                    continue;
                }
                Double height = null;
                for (int y = top; y >= bottom; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.getFluidState().isEmpty()) {
                        height = y + 2.0; // water: the road goes just over it
                        break;
                    }
                    if (ground(state, pos)) {
                        height = y + 1.0;
                        break;
                    }
                }
                if (height == null) {
                    gaps++;
                } else {
                    heights.add(height);
                }
            }
            if (heights.isEmpty() || gaps > heights.size()) {
                return heights.isEmpty() && gaps == 0 ? Double.NaN : state.grade - FILL_DEPTH - 1; // a gap
            }
            heights.sort(Double::compare);
            return heights.get(heights.size() / 2);
        }

        /**
         * Gets one column ready for a road whose surface is {@code level2} half blocks up: clears it above (more
         * where a lamp will stand) and fills it below, or, over water or a deep gap, puts in just a deck to drive on.
         * Returns whether it's a bridge.
         */
        boolean prepareColumn(int x, int z, int level2, Role role) {
            int base = Math.floorDiv(level2, 2); // the top of the solid ground under the road (a slab goes on it if it's at a half)
            int head = role == Role.ROAD ? HEADROOM : role == Role.VERGE ? POLE_HEIGHT + 1 : SIDE_HEADROOM;
            for (int y = base; y < base + head; y++) {
                clear(new BlockPos(x, y, z));
            }
            // Trees, and sand or gravel that would fall into the cut.
            for (int y = base + head; y < base + TREE_HEADROOM; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.getBlock() instanceof FallingBlock) {
                    clear(pos);
                } else if (!state.isAir() && !state.canBeReplaced()) {
                    break;
                }
            }
            int ground = base - 1;
            int depth = 0;
            while (depth <= FILL_DEPTH && !ground(level.getBlockState(new BlockPos(x, ground, z)), new BlockPos(x, ground, z))) {
                ground--;
                depth++;
            }
            if (depth <= FILL_DEPTH) {
                for (int y = ground + 1; y < base; y++) {
                    fill(new BlockPos(x, y, z));
                }
                return false;
            }
            fill(new BlockPos(x, base - 1, z)); // the deck
            return true;
        }

        /** Whether there's no ground within {@link #FILL_DEPTH} under a road whose ground would be up to {@code base}: a gap to bridge. */
        boolean overGap(int x, int z, int base) {
            for (int y = base - 1; y >= base - 1 - FILL_DEPTH; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (ground(level.getBlockState(pos), pos)) {
                    return false;
                }
            }
            return true;
        }

        /** A bridge's railing: a wall on the deck along its edge, over the gap, with room cleared above it. */
        void railing(int x, int z, int base) {
            BlockPos at = new BlockPos(x, base, z);
            for (int y = base + 1; y < base + SIDE_HEADROOM; y++) {
                clear(new BlockPos(x, y, z));
            }
            BlockState there = level.getBlockState(at);
            if (there.is(BlockTags.WALLS) || keep(there, at) || !allowed(at, there)) {
                return;
            }
            BlockPos deck = at.below();
            if (!ground(level.getBlockState(deck), deck)) {
                fill(deck);
            }
            BlockState wall = use(Material.RAILING);
            if (wall.hasProperty(BlockStateProperties.WATERLOGGED)) {
                wall = wall.setValue(BlockStateProperties.WATERLOGGED, false);
            }
            set(at, there, wall);
            // Joined up to the walls either side of it (and they to it).
            level.setBlock(at, Block.updateFromNeighbourShapes(level.getBlockState(at), level, at), Block.UPDATE_ALL);
        }

        /**
         * One column of a cut's or a bank's slope, {@code k} blocks out from the road: the land's cut back so it rises
         * no more than {@code k} blocks above the road there (only earth, stone, sand, snow and plants: never what
         * someone built, nor a tree standing on ground that's left), and (if {@code bank}) built up with fill so it falls
         * no more than {@code k} below it: only on the land, never into water. Freshly cut dirt gets its grass back.
         */
        void batter(int x, int z, int base, int k, boolean bank) {
            int top = base + k;
            boolean cut = false;
            for (int y = top; y < base + CUT_HEIGHT; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState there = level.getBlockState(pos);
                if (!there.isAir() && natural(there)) {
                    clear(pos);
                    cut |= level.getBlockState(pos).isAir();
                }
            }
            if (cut) {
                // A tree whose ground's been cut from under it goes too (its leaves elsewhere wither on their own).
                for (int y = top; y < base + CUT_HEIGHT; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState there = level.getBlockState(pos);
                    if (there.is(BlockTags.LOGS) || there.is(BlockTags.LEAVES)) {
                        clear(pos);
                    }
                }
            }
            BlockPos surface = new BlockPos(x, top - 1, z);
            if (cut && level.getBlockState(surface).is(Blocks.DIRT) && level.getBlockState(surface.above()).isAir() && allowed(surface, Blocks.DIRT.defaultBlockState())) {
                level.setBlock(surface, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            }
            // A bank: the ground built up to base - k, if it's lower, and there's land within reach (no further: a gap).
            if (!bank) {
                return;
            }
            int low = base - k;
            for (int y = top - 1; y >= low - 1 - FILL_DEPTH; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState there = level.getBlockState(pos);
                if (!there.getFluidState().isEmpty()) {
                    return; // water: no bank into it
                }
                if (ground(there, pos)) {
                    if (natural(there)) { // not on a road, nor anything else someone built
                        for (int fy = y + 1; fy < low; fy++) {
                            fill(new BlockPos(x, fy, z));
                        }
                    }
                    return;
                }
            }
        }

        /** The land, as opposed to what anyone built on it (or a tree): earth and stone, sand and gravel, snow and ice, plants. */
        boolean natural(BlockState state) {
            return state.is(FILL_BLOCKS) || state.getBlock() instanceof FallingBlock || state.is(BlockTags.SNOW) || state.is(BlockTags.ICE)
                    || state.canBeReplaced() && state.getFluidState().isEmpty();
        }

        /** A pillar of fill from under a bridge's deck down to the ground (or as far as it goes). */
        void pillar(int x, int z) {
            Integer level2 = state.levels.get(key(x, z));
            if (level2 == null) {
                return;
            }
            int top = Math.floorDiv(level2, 2) - 2;
            for (int y = top; y > top - PILLAR_DEPTH && y > level.getMinBuildHeight(); y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (ground(level.getBlockState(pos), pos)) {
                    break;
                }
                fill(pos);
            }
        }

        // ---- the back: laying the road ----

        /** The road behind the paver at {@code point}: asphalt and its markings, sidewalks, and a lamp when one's due. */
        void lay(Vec3 point, Vec3 forward, Vec3 side, float yaw, double step, boolean straight) {
            int here = settings.shape() ? (int) Math.round(state.grade * 2.0) : Integer.MIN_VALUE;
            columns(point, forward, side, extentLeft(), extentRight(), (x, z, o, along) -> {
                Role role = role(o);
                int level2 = levelOf(x, z, here, role);
                if (level2 == Integer.MIN_VALUE) {
                    return;
                }
                switch (role) {
                    case ROAD -> layRoad(x, z, level2);
                    case CURB, SIDEWALK -> laySidewalk(x, z, level2, role);
                    default -> {
                    }
                }
            });
            followLines(point, forward, side, straight);
            if (settings.lamps() && state.rear.distance >= state.nextLamp) {
                for (boolean onLeft : new boolean[] {true, false}) {
                    if (settings.lampSides().has(state.lampIndex, onLeft)) {
                        int sidewalk = onLeft ? sidewalkLeft : sidewalkRight;
                        double o = onLeft ? roadLeft + (sidewalk > 0 ? sidewalk - 0.5 : 0.5) : -(roadRight + (sidewalk > 0 ? sidewalk - 0.5 : 0.5));
                        Vec3 at = point.add(side.scale(o));
                        lamp(Mth.floor(at.x), Mth.floor(at.z), sidewalk > 0, onLeft ? side.reverse() : side, here);
                    }
                }
                state.lampIndex++;
                state.nextLamp += settings.lampSpacing();
            }
        }

        /**
         * A column's road height in half blocks: as the front pass got it ready, or (somewhere it didn't reach, round
         * the inside of a bend, say) got ready now; or, not shaping the land, the ground's own height there.
         */
        int levelOf(int x, int z, int here, Role role) {
            if (!settings.shape()) {
                int top = groundTop(x, z);
                return top == Integer.MIN_VALUE ? Integer.MIN_VALUE : top * 2;
            }
            long key = key(x, z);
            Integer known = state.levels.get(key);
            if (known != null) {
                return known;
            }
            state.levels.put(key, here);
            prepareColumn(x, z, here, role);
            return here;
        }

        /** Not shaping the land: the top of the ground in this column, near the paver's height. */
        int groundTop(int x, int z) {
            int around = Mth.floor(paver.getY());
            for (int y = around + 2; y >= around - 4; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (ground(level.getBlockState(pos), pos)) {
                    return y + 1;
                }
            }
            return Integer.MIN_VALUE;
        }

        /**
         * Each line down the road, on to where it is at this cross-section: on the straight line through block
         * centers the lines sit on rows of blocks (a line between two rows would run along their seam).
         */
        void followLines(Vec3 point, Vec3 forward, Vec3 side, boolean straight) {
            if (!settings.equals(state.tracksFor)) {
                state.tracks.clear();
                for (int i = 0; i < stripes.size(); i++) {
                    Stripe stripe = stripes.get(i);
                    state.tracks.add(new Track(stripe, stripe.line() == RoadMarking.Line.EDGE ? 0 : stripe.line() == RoadMarking.Line.LANE ? 2 : 1));
                }
                state.tracksFor = settings;
            }
            double spacing = 1.0 / (Math.abs(side.x) + Math.abs(side.z));
            for (Track track : state.tracks) {
                double o = straight ? Math.round(track.stripe.offset() / spacing) * spacing : track.stripe.offset();
                Vec3 at = new Vec3(point.x + side.x * o, point.y, point.z + side.z * o);
                if (track.last == null) {
                    // The line starts here: in this block, coming in from behind.
                    track.x = Mth.floor(at.x);
                    track.z = Mth.floor(at.z);
                    track.entry = boundary(track.x, track.z, at.x, at.z, -forward.x, -forward.z, null);
                    track.last = at;
                    track.ahead = null;
                    paintAhead(track, forward.x, forward.z);
                    continue;
                }
                follow(track, at);
            }
        }

        /** Takes a line from where it was to {@code to}, block by block, painting each block's piece as it leaves it. */
        void follow(Track track, Vec3 to) {
            double ax = track.last.x;
            double az = track.last.z;
            double dx = to.x - ax;
            double dz = to.z - az;
            for (int guard = 0; guard < 64; guard++) {
                double tx = dx > 1.0E-12 ? (track.x + 1 - ax) / dx : dx < -1.0E-12 ? (track.x - ax) / dx : Double.MAX_VALUE;
                double tz = dz > 1.0E-12 ? (track.z + 1 - az) / dz : dz < -1.0E-12 ? (track.z - az) / dz : Double.MAX_VALUE;
                double t = Math.min(tx, tz);
                if (t > 1.0) {
                    break; // still in this block
                }
                boolean acrossX = tx <= tz + 1.0E-9;
                boolean acrossZ = tz <= tx + 1.0E-9;
                int[] exit = exitAnchor(track, ax + dx * t, az + dz * t, acrossX ? (dx > 0 ? 1 : -1) : 0, acrossZ ? (dz > 0 ? 1 : -1) : 0);
                if (exit != null) {
                    paint(track, exit);
                } else if (track.ahead != null) {
                    unpaint(track); // it only clipped this block: what was painted ahead comes off
                }
                track.ahead = null;
                int nx = track.x + (acrossX ? (dx > 0 ? 1 : -1) : 0);
                int nz = track.z + (acrossZ ? (dz > 0 ? 1 : -1) : 0);
                int[] carried = exit != null ? exit : track.entry;
                track.x = nx;
                track.z = nz;
                // A block it only clipped gets nothing; the next starts where that one's line came in, if it can.
                track.entry = onBoundary(nx, nz, carried) ? carried : nearest(nx, nz, ax + dx * t, az + dz * t);
            }
            track.last = to;
            paintAhead(track, dx, dz);
        }

        /** Paints the piece of the block a line's in as if it went straight on the way it's going ({@code dx}, {@code dz}). */
        void paintAhead(Track track, double dx, double dz) {
            if (dx * dx + dz * dz < 1.0E-12) {
                return;
            }
            double ax = track.last.x;
            double az = track.last.z;
            double tx = dx > 1.0E-12 ? (track.x + 1 - ax) / dx : dx < -1.0E-12 ? (track.x - ax) / dx : Double.MAX_VALUE;
            double tz = dz > 1.0E-12 ? (track.z + 1 - az) / dz : dz < -1.0E-12 ? (track.z - az) / dz : Double.MAX_VALUE;
            double t = Math.min(tx, tz);
            boolean acrossX = tx <= tz + 1.0E-9;
            boolean acrossZ = tz <= tx + 1.0E-9;
            int[] exit = exitAnchor(track, ax + dx * t, az + dz * t, acrossX ? (dx > 0 ? 1 : -1) : 0, acrossZ ? (dz > 0 ? 1 : -1) : 0);
            if (exit != null) {
                RoadMarking.Piece piece = paint(track, exit);
                if (piece != null) {
                    track.ahead = piece;
                }
            }
        }

        /** Takes off the piece painted ahead in the block a line's in, if it's still there. */
        void unpaint(Track track) {
            long key = key(track.x, track.z);
            Long road = state.roads.get(key);
            if (road == null) {
                return;
            }
            BlockPos at = BlockPos.of(road);
            BlockState there = level.getBlockState(at);
            if (there.is(Asphalt.ROAD) && there.hasProperty(Asphalt.MARKING) && there.getValue(Asphalt.MARKING) == track.ahead.marking()
                    && there.getValue(Asphalt.FACING) == track.ahead.facing()) {
                level.setBlock(at, there.setValue(Asphalt.MARKING, RoadMarking.NONE), Block.UPDATE_ALL);
                state.painted.remove(key, track.rank);
            }
        }

        /**
         * The anchor a line leaves its block by, through the side (or the corner) {@code sx}, {@code sz} points to,
         * crossing at ({@code cx}, {@code cz}): the one on that side nearest the crossing that makes a piece with the
         * anchor it came in by; null if none does (it only clipped the block).
         */
        @Nullable
        int[] exitAnchor(Track track, double cx, double cz, int sx, int sz) {
            List<int[]> candidates = new ArrayList<>();
            int gx = 2 * track.x + (sx > 0 ? 2 : 0);
            int gz = 2 * track.z + (sz > 0 ? 2 : 0);
            if (sx != 0 && sz != 0) {
                candidates.add(new int[] {gx, gz});
            } else if (sx != 0) {
                for (int k = 0; k <= 2; k++) {
                    candidates.add(new int[] {gx, 2 * track.z + k});
                }
                candidates.sort(java.util.Comparator.comparingDouble(a -> Math.abs(a[1] - cz * 2.0)));
            } else {
                for (int k = 0; k <= 2; k++) {
                    candidates.add(new int[] {2 * track.x + k, gz});
                }
                candidates.sort(java.util.Comparator.comparingDouble(a -> Math.abs(a[0] - cx * 2.0)));
            }
            for (int[] candidate : candidates) {
                if (!degenerate(track.x, track.z, track.entry, candidate)) {
                    return candidate;
                }
            }
            return null;
        }

        /** Whether anchors {@code a} and {@code b} make no piece in block (x, z): the same anchor, or both on one side. */
        static boolean degenerate(int x, int z, int[] a, int[] b) {
            int ax = a[0] - 2 * x;
            int az = a[1] - 2 * z;
            int bx = b[0] - 2 * x;
            int bz = b[1] - 2 * z;
            return ax == bx && az == bz || ax == bx && ax != 1 || az == bz && az != 1;
        }

        static boolean onBoundary(int x, int z, int[] a) {
            int ax = a[0] - 2 * x;
            int az = a[1] - 2 * z;
            return ax >= 0 && ax <= 2 && az >= 0 && az <= 2 && !(ax == 1 && az == 1);
        }

        /** The anchor on block (x, z)'s edge nearest the point (cx, cz) on it. */
        static int[] nearest(int x, int z, double cx, double cz) {
            int gx = Mth.clamp((int) Math.round(cx * 2.0), 2 * x, 2 * x + 2);
            int gz = Mth.clamp((int) Math.round(cz * 2.0), 2 * z, 2 * z + 2);
            if (gx == 2 * x + 1 && gz == 2 * z + 1) {
                gx = 2 * x; // the middle isn't on the edge: the nearest side's (only for a line that starts mid-block)
            }
            return new int[] {gx, gz};
        }

        /** Where a line from (px, pz) going (fx, fz) leaves block (x, z), as the nearest anchor. */
        static int[] boundary(int x, int z, double px, double pz, double fx, double fz, @Nullable int[] unused) {
            double tx = fx > 1.0E-12 ? (x + 1 - px) / fx : fx < -1.0E-12 ? (x - px) / fx : Double.MAX_VALUE;
            double tz = fz > 1.0E-12 ? (z + 1 - pz) / fz : fz < -1.0E-12 ? (z - pz) / fz : Double.MAX_VALUE;
            double t = Math.min(tx, tz);
            return nearest(x, z, px + fx * t, pz + fz * t);
        }

        /**
         * Paints the piece of a line in the block it's leaving, on the road laid there this run, unless a line that ranks
         * higher's there. Returns the piece painted, or null.
         */
        @Nullable
        RoadMarking.Piece paint(Track track, int[] exit) {
            long key = key(track.x, track.z);
            Long road = state.roads.get(key);
            if (road == null) {
                return null; // not this run's road: one it crossed, say
            }
            Integer rank = state.painted.get(key);
            if (rank != null && rank < track.rank) {
                return null;
            }
            int[] from = {track.entry[0] - 2 * track.x, track.entry[1] - 2 * track.z};
            int[] to = {exit[0] - 2 * track.x, exit[1] - 2 * track.z};
            RoadMarking.Piece piece = RoadMarking.between(track.stripe.line(), from, to);
            BlockPos at = BlockPos.of(road);
            BlockState there = level.getBlockState(at);
            if (piece == null || !there.is(Asphalt.ROAD) || !there.hasProperty(Asphalt.MARKING)) {
                return null;
            }
            if (there.getValue(Asphalt.MARKING) == RoadMarking.NONE) {
                use(Material.PAINT);
            }
            state.painted.put(key, track.rank);
            level.setBlock(at, there.setValue(Asphalt.MARKING, piece.marking()).setValue(Asphalt.FACING, piece.facing()), Block.UPDATE_ALL);
            return piece;
        }

        /** A block of plain asphalt (its lines are painted on as they leave it: see {@link #followLines}). */
        void layRoad(int x, int z, int level2) {
            boolean half = (level2 & 1) != 0;
            int base = Math.floorDiv(level2, 2);
            BlockPos at = half ? new BlockPos(x, base, z) : new BlockPos(x, base - 1, z);
            BlockState there = level.getBlockState(at);
            if (there.is(Asphalt.ROAD) || keep(there, at)) {
                return; // a road already (this one, or one it's crossing), or something it leaves alone
            }
            if (!allowed(at, there)) {
                return;
            }
            BlockState road;
            if (half) {
                use(Material.ASPHALT_SLAB);
                road = ModBlocks.ASPHALT_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
                BlockPos under = at.below();
                if (!ground(level.getBlockState(under), under)) {
                    fill(under);
                }
            } else {
                use(Material.ASPHALT);
                road = ModBlocks.ASPHALT.get().defaultBlockState();
            }
            set(at, there, road);
            state.roads.put(key(x, z), at.asLong());
            state.laid++;
            if (state.laid % 10 == 0) {
                com.reiko.cars.advancement.CarAdvancements.reach(driver, "pave", state.laid);
            }
            clearPlants(at.above(), 2);
        }

        void laySidewalk(int x, int z, int level2, Role role) {
            boolean half = (level2 & 1) != 0;
            int base = Math.floorDiv(level2, 2);
            BlockPos at = new BlockPos(x, base, z);
            BlockState there = level.getBlockState(at);
            if (there.getBlock() instanceof SlabBlock || keep(there, at) || there.is(ModBlocks.STREET_POLE.get())) {
                return;
            }
            if (!allowed(at, there)) {
                return;
            }
            Material material = role == Role.CURB ? Material.CURB : Material.SIDEWALK;
            BlockState slab = use(material);
            if (half) {
                use(material); // a whole block of it, to come up level with the road's half block
            }
            slab = slab.hasProperty(SlabBlock.TYPE) ? slab.setValue(SlabBlock.TYPE, half ? SlabType.DOUBLE : SlabType.BOTTOM) : slab;
            if (slab.hasProperty(BlockStateProperties.WATERLOGGED)) {
                slab = slab.setValue(BlockStateProperties.WATERLOGGED, false);
            }
            BlockPos under = at.below();
            if (!ground(level.getBlockState(under), under)) {
                fill(under);
            }
            set(at, there, slab);
            clearPlants(at.above(), 2);
        }

        /** A street lamp: poles up from the sidewalk (or the verge), the lamp on top reaching out toward the road. */
        void lamp(int x, int z, boolean onSidewalk, Vec3 towardRoad, int here) {
            int level2 = levelOf(x, z, here, onSidewalk ? Role.SIDEWALK : Role.VERGE);
            if (level2 == Integer.MIN_VALUE) {
                return;
            }
            boolean half = (level2 & 1) != 0;
            int base = Math.floorDiv(level2, 2);
            int y0 = onSidewalk && half ? base + 1 : base; // on the whole block a half-height sidewalk makes, or in place of its slab
            for (int y = y0; y <= y0 + POLE_HEIGHT; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState there = level.getBlockState(pos);
                if (there.is(ModBlocks.STREET_POLE.get()) || there.is(ModBlocks.STREET_LAMP.get()) || keep(there, pos) || !there.getFluidState().isEmpty()
                        || !allowed(pos, there)) {
                    return; // a lamp here already, or something in the way
                }
            }
            BlockPos under = new BlockPos(x, y0 - 1, z);
            if (!ground(level.getBlockState(under), under)) {
                fill(under);
            }
            use(Material.POLE, POLE_HEIGHT);
            use(Material.LAMP);
            for (int y = y0; y < y0 + POLE_HEIGHT; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                set(pos, level.getBlockState(pos), ModBlocks.STREET_POLE.get().defaultBlockState());
            }
            BlockPos top = new BlockPos(x, y0 + POLE_HEIGHT, z);
            Direction facing = Direction.getNearest(towardRoad.x, 0.0, towardRoad.z);
            set(top, level.getBlockState(top), ModBlocks.STREET_LAMP.get().defaultBlockState().setValue(StreetLampBlock.FACING, facing)
                    .setValue(StreetLampBlock.LIT, StreetLampBlock.dark(level)));
        }

        // ---- blocks ----

        /** Something the paver stands on or builds on: solid, not a plant, not a tree, not water. */
        boolean ground(BlockState state, BlockPos pos) {
            return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced() && !state.is(BlockTags.LEAVES)
                    && !state.is(BlockTags.LOGS) && !state.getCollisionShape(level, pos).isEmpty();
        }

        /** Things the paver leaves where they are: anything with a block entity, the unbreakable, and the mod's own road furniture. */
        boolean keep(BlockState state, BlockPos pos) {
            return state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0.0F
                    || BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals(ReikoCars.MODID) && !state.is(Asphalt.ROAD);
        }

        /** Whether the driver may change the block at {@code pos}: not spawn protection, nor a claim (any mod that cancels the break). */
        boolean allowed(BlockPos pos, BlockState there) {
            if (!level.isLoaded(pos) || !level.mayInteract(driver, pos) || !driver.mayBuild()) {
                return false;
            }
            if (there.isAir() || there.canBeReplaced()) {
                return !NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos),
                        level.getBlockState(pos.below()), driver)).isCanceled();
            }
            return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, there, driver)).isCanceled();
        }

        /** Clears a block out of the road's way (leaving water, and what it keeps). */
        void clear(BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty() || keep(state, pos) || !allowed(pos, state)) {
                return;
            }
            dig(pos, state);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        /** Plants and snow on the road just laid. */
        void clearPlants(BlockPos pos, int height) {
            for (int i = 0; i < height; i++) {
                BlockPos at = pos.above(i);
                BlockState state = level.getBlockState(at);
                if (!state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty() && allowed(at, state)) {
                    level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        /** Fills a block under the road with earth or stone. */
        void fill(BlockPos pos) {
            BlockState there = level.getBlockState(pos);
            if (keep(there, pos) || ground(there, pos) || !allowed(pos, there)) {
                return;
            }
            BlockState fill = use(Material.FILL);
            set(pos, there, fill);
        }

        /** Puts {@code state} where {@code there} was, keeping what was dug out of the way. */
        void set(BlockPos pos, BlockState there, BlockState state) {
            if (!there.isAir() && there.getFluidState().isEmpty()) {
                dig(pos, there);
            }
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }

        /** What's dug out: earth and stone into the hopper (to fill with later), ores and logs too, or beside the road if there's no room. */
        void dig(BlockPos pos, BlockState state) {
            if (free) {
                return;
            }
            boolean valuable = state.is(Tags.Blocks.ORES) || state.is(BlockTags.LOGS);
            if (!valuable && !state.is(FILL_BLOCKS)) {
                return; // plants, leaves and the like: cleared away
            }
            Item item = state.is(BlockTags.DIRT) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND) ? Items.DIRT : state.getBlock().asItem();
            if (item == Items.AIR) {
                return;
            }
            ItemStack left = paver.bed().addItem(new ItemStack(item));
            if (!left.isEmpty() && valuable) {
                Block.popResource(level, pos, left);
            }
        }

        static long key(int x, int z) {
            return ((long) x << 32) | (z & 0xFFFFFFFFL);
        }

        // ---- materials and paying ----

        BlockState use(Material material) {
            return use(material, 1);
        }

        /**
         * Takes {@code count} of {@code material}: from the hopper, then the driver's pockets, then bought; returns the
         * block to lay (the kind of slab or fill it found). Throws {@link OutOf} if it can't.
         */
        BlockState use(Material material, int count) {
            BlockState placed = fallback(material);
            if (free) {
                return placed;
            }
            for (int i = 0; i < count; i++) {
                BlockState found = take(material);
                if (found == null) {
                    if (!buy(material)) {
                        throw new OutOf(settings.pay()
                                ? Component.translatable("paver.reikocars.cant_afford", material.displayName(), money(material.price()))
                                : Component.translatable("paver.reikocars.out_of", material.displayName()));
                    }
                } else if (i == 0) {
                    placed = found;
                }
            }
            return placed;
        }

        /** What's laid when it's bought: plain asphalt, smooth stone slabs, cobblestone, the mod's poles and lamps. */
        static BlockState fallback(Material material) {
            return switch (material) {
                case ASPHALT -> ModBlocks.ASPHALT.get().defaultBlockState();
                case ASPHALT_SLAB -> ModBlocks.ASPHALT_SLAB.get().defaultBlockState();
                case PAINT -> Blocks.AIR.defaultBlockState();
                case CURB, SIDEWALK -> Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
                case FILL -> Blocks.COBBLESTONE.defaultBlockState();
                case POLE -> ModBlocks.STREET_POLE.get().defaultBlockState();
                case LAMP -> ModBlocks.STREET_LAMP.get().defaultBlockState();
                case RAILING -> Blocks.STONE_BRICK_WALL.defaultBlockState();
            };
        }

        /** One of {@code material} out of the hopper or the driver's inventory; the block it lays as, or null if there's none. */
        @Nullable
        BlockState take(Material material) {
            List<Container> sources = List.of(paver.bed(), driver.getInventory());
            switch (material) {
                case ASPHALT_SLAB -> {
                    BlockState slab = takeItem(sources, stack -> stack.is(ModItems.ASPHALT_SLAB.get()));
                    if (slab != null) {
                        return slab;
                    }
                    // Half a block of asphalt: the other half goes back in the hopper as a slab.
                    if (takeItem(sources, stack -> stack.is(ModItems.ASPHALT.get())) != null) {
                        ItemStack spare = paver.bed().addItem(new ItemStack(ModItems.ASPHALT_SLAB.get()));
                        if (!spare.isEmpty()) {
                            driver.getInventory().placeItemBackInInventory(spare);
                        }
                        return ModBlocks.ASPHALT_SLAB.get().defaultBlockState();
                    }
                    return null;
                }
                case PAINT -> {
                    for (Container source : sources) {
                        for (int slot = 0; slot < source.getContainerSize(); slot++) {
                            ItemStack stack = source.getItem(slot);
                            if (stack.getItem() instanceof RoadPaintItem) {
                                int damage = stack.getDamageValue() + 1;
                                if (damage >= stack.getMaxDamage()) {
                                    source.setItem(slot, ItemStack.EMPTY);
                                } else {
                                    stack.setDamageValue(damage);
                                }
                                source.setChanged();
                                return Blocks.AIR.defaultBlockState();
                            }
                        }
                    }
                    return null;
                }
                case CURB -> {
                    BlockState curb = takeItem(sources, stack -> stack.is(Items.SMOOTH_STONE_SLAB));
                    return curb != null ? curb : take(Material.SIDEWALK);
                }
                case SIDEWALK -> {
                    // The first stone slab that isn't asphalt or the curb's smooth stone; smooth stone if that's all there is.
                    BlockState slab = takeItem(sources, stack -> sidewalkSlab(stack) && !stack.is(Items.SMOOTH_STONE_SLAB));
                    return slab != null ? slab : takeItem(sources, stack -> stack.is(Items.SMOOTH_STONE_SLAB));
                }
                case FILL -> {
                    return takeItem(sources, stack -> stack.is(FILL_ITEMS) && stack.getItem() instanceof BlockItem);
                }
                case ASPHALT -> {
                    return takeItem(sources, stack -> stack.is(ModItems.ASPHALT.get()));
                }
                case POLE -> {
                    return takeItem(sources, stack -> stack.is(ModItems.STREET_POLE.get()));
                }
                case LAMP -> {
                    return takeItem(sources, stack -> stack.is(ModItems.STREET_LAMP.get()));
                }
                case RAILING -> {
                    return takeItem(sources, stack -> stack.is(ItemTags.WALLS) && stack.getItem() instanceof BlockItem);
                }
            }
            return null;
        }

        static boolean sidewalkSlab(ItemStack stack) {
            return stack.getItem() instanceof BlockItem block && block.getBlock() instanceof SlabBlock && !stack.is(ModItems.ASPHALT_SLAB.get())
                    && !stack.is(ItemTags.WOODEN_SLABS);
        }

        @Nullable
        private static BlockState takeItem(List<Container> sources, Predicate<ItemStack> wanted) {
            for (Container source : sources) {
                for (int slot = 0; slot < source.getContainerSize(); slot++) {
                    ItemStack stack = source.getItem(slot);
                    if (!stack.isEmpty() && wanted.test(stack)) {
                        BlockState state = stack.getItem() instanceof BlockItem block ? block.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState();
                        stack.shrink(1);
                        source.setChanged();
                        return state;
                    }
                }
            }
            return null;
        }

        /** Buys one of {@code material} on the driver's account, if they've said it may and they can afford it. */
        boolean buy(Material material) {
            if (!settings.pay()) {
                return false;
            }
            long price = material.price();
            EconomyService economy = EconomyService.INSTANCE;
            economy.ensureAccount(level.getServer(), driver.getUUID(), driver.getGameProfile().getName());
            if (economy.getBalance(level.getServer(), driver.getUUID()) - state.owed < price) {
                return false;
            }
            state.owed += price;
            state.spent += price;
            return true;
        }

        /** Takes what this tick's bought cost from the driver's account, in one go. */
        void settle() {
            if (state.owed > 0) {
                EconomyService.INSTANCE.withdraw(level.getServer(), driver.getUUID(), state.owed, "Road paver materials");
                state.owed = 0;
            }
        }
    }

    @FunctionalInterface
    interface ColumnAction {
        /** A column {@code o} blocks left of the road's middle, {@code along} blocks ahead of the cross-section. */
        void at(int x, int z, double o, double along);
    }
}
