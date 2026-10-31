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
    /** How near (in degrees) to a multiple of 45 the paver must be heading to follow a straight line. */
    public static final float STRAIGHT = 0.5F;
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
        /** Beyond the road, cleared for the paver to pass. */
        CLEARANCE
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
        LAMP;

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
        Vec3 rearPoint = paver.position().subtract(forward.scale(half - 0.25));
        Job job = new Job(level, paver, driver, paver.paverSettings(), state);
        if (state.front.at == null) {
            // Just started: the road starts at the paver's own level, under its back end.
            state.grade = Math.round(paver.getY() * 2.0) / 2.0;
            state.front.at = rearPoint.subtract(forward.scale(STEP));
            state.front.yaw = yaw;
            state.rear.at = rearPoint.subtract(forward.scale(STEP));
            state.rear.yaw = yaw;
            state.nextLamp = job.settings.lampSpacing() / 2.0;
            state.lampIndex = 0;
            state.nextPillar = PILLAR_EVERY / 2.0;
        }
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
        float turn = Mth.wrapDegrees(yaw - fromYaw);
        // Enough cross-sections that they're STEP apart along the middle, and not much more round the outside of a bend.
        int count = Mth.clamp((int) Math.ceil(Math.max(distance / STEP, Math.abs(turn) * Mth.DEG_TO_RAD * extent / 0.8)), 1, 64);
        for (int i = 1; i <= count; i++) {
            double t = i / (double) count;
            Vec3 at = new Vec3(from.x + dx * t, to.y, from.z + dz * t);
            float sectionYaw = fromYaw + turn * (float) t;
            pass.distance += distance / count;
            section.at(at, forward(sectionYaw), left(sectionYaw), sectionYaw, distance / count);
        }
        pass.at = to;
        pass.yaw = yaw;
    }

    @FunctionalInterface
    interface Section {
        void at(Vec3 point, Vec3 forward, Vec3 left, float yaw, double step);
    }

    /** The line the paver's following, if it's heading straight along a multiple of 45 degrees. */
    @Nullable
    private static Line line(State state, CarEntity paver) {
        float yaw = paver.getYRot();
        float snapped = Math.round(yaw / 45.0F) * 45.0F;
        if (Math.abs(Mth.wrapDegrees(yaw - snapped)) > STRAIGHT) {
            state.line = null;
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

    /** The marking a road block {@code o} blocks to the left of the paver's line gets, and which way it's drawn. */
    public record Mark(RoadMarking marking, Direction facing) {
    }

    /**
     * The marking for a road block whose center is {@code o} blocks left of the line down the middle of the road, the
     * road going {@code yaw}.
     */
    public static Mark markingFor(PaverSettings settings, double o, float yaw) {
        Vec3 left = left(yaw);
        // Half the width of the band of blocks a line down the road passes through: half a block on a road along the
        // grid, more (0.71) on one at 45 degrees.
        double tol = 0.5 * (Math.abs(left.x) + Math.abs(left.z));
        double eps = 1.0E-4; // the lines fall exactly on block centers or between them: don't let rounding pick the wrong side
        double roadLeft = roadLeft(settings);
        double roadRight = roadRight(settings);
        Direction travel = Direction.fromYRot(yaw);
        RoadMarking marking = RoadMarking.NONE;
        Direction facing = travel;
        if (settings.edges() && o >= roadLeft - tol - eps) {
            marking = RoadMarking.EDGE;
        } else if (settings.edges() && o <= -roadRight + tol + eps) {
            marking = RoadMarking.EDGE;
            facing = travel.getOpposite(); // drawn down its left, which is the road's right
        } else if (settings.center() != PaverSettings.Center.NONE && Math.abs(o) < tol - eps) {
            marking = settings.center().marking();
        } else {
            int lanes = settings.lanesThatFit();
            for (int k = 1; k < lanes; k++) {
                if (Math.abs(o - k * roadLeft / lanes) < tol - eps || Math.abs(o + k * roadRight / lanes) < tol - eps) {
                    marking = RoadMarking.LANE_DASHED;
                }
            }
        }
        int eighth = Math.floorMod(Math.round(yaw / 45.0F), 8); // 0 south, 1 south-west, 2 west, ... 5 north-east
        if (eighth % 2 == 1 && marking != RoadMarking.NONE) {
            marking = marking.diagonal();
            facing = eighth == 1 || eighth == 5 ? Direction.NORTH : Direction.EAST; // south-west to north-east, or the other way
        }
        return new Mark(marking, facing);
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
        }

        double extentLeft() {
            return roadLeft + sidewalkLeft + vergeLeft;
        }

        double extentRight() {
            return roadRight + sidewalkRight + vergeRight;
        }

        /** How far out either side a pass works; the front one clears room for the paver too. */
        double extent(boolean front) {
            double reach = Math.max(extentLeft(), extentRight());
            return front ? Math.max(reach, paver.carType().width / 2.0 + 0.6) : reach;
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
            return Role.CLEARANCE;
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
                        action.at(x, z, o);
                    }
                }
            }
        }

        // ---- the front: getting the ground ready ----

        /** The ground ahead of the paver at {@code point}: the grade worked out, then each column cut and filled to it. */
        void prepare(Vec3 point, Vec3 forward, Vec3 side, float yaw, double step) {
            double target = terrainAhead(point.add(forward.scale(LOOKAHEAD)), side);
            if (!Double.isNaN(target)) {
                if (target < state.grade - FILL_DEPTH) {
                    target = state.grade; // a gap or deep water ahead: carry on level, as a bridge
                }
                state.grade += Mth.clamp(target - state.grade, -MAX_SLOPE * step, MAX_SLOPE * step);
            }
            int here = (int) Math.round(state.grade * 2.0);
            double reach = extent(true);
            boolean[] bridge = {false};
            columns(point, forward, side, Math.max(extentLeft(), reach), Math.max(extentRight(), reach), (x, z, o) -> {
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
            int head = role == Role.ROAD || role == Role.CLEARANCE ? HEADROOM : role == Role.VERGE ? POLE_HEIGHT + 1 : SIDE_HEADROOM;
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
            if (role == Role.CLEARANCE) {
                return false;
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
        void lay(Vec3 point, Vec3 forward, Vec3 side, float yaw, double step) {
            int here = settings.shape() ? (int) Math.round(state.grade * 2.0) : Integer.MIN_VALUE;
            columns(point, forward, side, extentLeft(), extentRight(), (x, z, o) -> {
                Role role = role(o);
                int level2 = levelOf(x, z, here, role);
                if (level2 == Integer.MIN_VALUE) {
                    return;
                }
                switch (role) {
                    case ROAD -> layRoad(x, z, level2, markingFor(settings, o, yaw));
                    case CURB, SIDEWALK -> laySidewalk(x, z, level2, role);
                    default -> {
                    }
                }
            });
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

        void layRoad(int x, int z, int level2, Mark mark) {
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
            if (mark.marking() != RoadMarking.NONE) {
                use(Material.PAINT);
                road = road.setValue(Asphalt.MARKING, mark.marking()).setValue(Asphalt.FACING, mark.facing());
            }
            set(at, there, road);
            state.laid++;
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
        void at(int x, int z, double o);
    }
}
