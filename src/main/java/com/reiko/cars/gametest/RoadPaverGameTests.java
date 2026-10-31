package com.reiko.cars.gametest;

import static com.reiko.cars.gametest.CarGameTests.check;
import static com.reiko.cars.gametest.CarGameTests.join;
import static com.reiko.cars.gametest.CarGameTests.leave;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.registry.ModBlocks;
import com.reiko.cars.registry.ModEntities;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.road.Asphalt;
import com.reiko.cars.road.PaverSettings;
import com.reiko.cars.road.RoadMarking;
import com.reiko.cars.road.RoadPaver;
import com.reiko.cars.trailer.Hitching;
import com.reiko.economy.core.EconomyService;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The road paver: the road it lays (asphalt, lines, sidewalks, lamps), shaping the land under it (a hill cut through
 * with sloped sides, a dip filled with banks, a gap bridged on pillars with railings), roads at 45 degrees and round
 * bends with their lines unbroken, and what it takes from the hopper and pays for.
 */
@GameTestHolder(ReikoCars.MODID)
@PrefixGameTestTemplate(false)
public final class RoadPaverGameTests {
    /** 29 wide, 20 high, 44 long: room for a road with sidewalks and lamps, the slopes beside it, and land to shape. */
    private static final String PAVING = "paving";
    private static final int WIDE = 29;
    /** The middle of the test area, across. */
    private static final int CX = 14;
    private static final EconomyService ECO = EconomyService.INSTANCE;

    private RoadPaverGameTests() {
    }

    /** Lays ground of {@code block} from y 0 up to {@code top} (inclusive) across the whole test area. */
    private static void ground(GameTestHelper helper, BlockState block, int top) {
        for (int x = 0; x < WIDE; x++) {
            for (int z = 0; z < 44; z++) {
                for (int y = 0; y <= top; y++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    /** A road paver driven by {@code driver}, its middle at ({@code x}, {@code y}, {@code z}), facing {@code yaw}. */
    private static CarEntity paver(GameTestHelper helper, ServerPlayer driver, double x, double y, double z, float yaw, PaverSettings settings) {
        CarEntity paver = ModEntities.car(CarType.ROAD_PAVER).create(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        paver.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        paver.setFuel(CarType.ROAD_PAVER.tankCapacity());
        paver.setOwner(driver);
        paver.setPaverSettings(settings);
        helper.getLevel().addFreshEntity(paver);
        driver.startRiding(paver, true);
        return paver;
    }

    /** Drives the paver {@code blocks} blocks, a fifth of a block a tick, turning {@code turn} degrees a tick, paving as it goes. */
    private static void drive(CarEntity paver, double blocks, float turn) {
        int steps = (int) Math.round(blocks / 0.2);
        double pivot = CarType.ROAD_PAVER.pivotBack();
        for (int i = 0; i < steps && paver.paving(); i++) {
            // Turning about its drive wheels, as it does when driven.
            Vec3 before = paver.forward().scale(-pivot);
            paver.setYRot(paver.getYRot() + turn);
            Vec3 shift = before.subtract(paver.forward().scale(-pivot));
            paver.setPos(paver.position().add(shift).add(paver.forward().scale(0.2)));
            RoadPaver.tick(paver);
        }
    }

    private static void drive(CarEntity paver, double blocks) {
        drive(paver, blocks, 0.0F);
    }

    private static BlockState at(GameTestHelper helper, int x, int y, int z) {
        return helper.getBlockState(new BlockPos(x, y, z));
    }

    private static boolean asphalt(BlockState state, RoadMarking marking, Direction facing) {
        return state.is(ModBlocks.ASPHALT.get()) && state.getValue(Asphalt.MARKING) == marking
                && (marking == RoadMarking.NONE || state.getValue(Asphalt.FACING) == facing);
    }

    private static boolean marked(BlockState state, RoadMarking marking) {
        return state.is(ModBlocks.ASPHALT.get()) && state.getValue(Asphalt.MARKING) == marking;
    }

    /**
     * How many loose ends the lines of a kind (those {@code which} picks) have at layer {@code y}: each piece runs
     * between two anchors, and an unbroken line shares every anchor between two pieces, but its own two ends.
     */
    private static int looseEnds(GameTestHelper helper, int y, Predicate<RoadMarking> which, int[] pieces) {
        Map<Long, Integer> ends = new HashMap<>();
        for (int x = 0; x < WIDE; x++) {
            for (int z = 0; z < 44; z++) {
                BlockState state = at(helper, x, y, z);
                if (!state.is(ModBlocks.ASPHALT.get()) || !which.test(state.getValue(Asphalt.MARKING))) {
                    continue;
                }
                int[][] anchors = state.getValue(Asphalt.MARKING).anchors(state.getValue(Asphalt.FACING));
                if (anchors == null) {
                    continue;
                }
                pieces[0]++;
                for (int[] a : anchors) {
                    ends.merge(((long) (2 * x + a[0]) << 32) | ((2 * z + a[1]) & 0xFFFFFFFFL), 1, Integer::sum);
                }
            }
        }
        return (int) ends.values().stream().filter(n -> n == 1).count();
    }

    @GameTest(template = PAVING, timeoutTicks = 40, skyAccess = true)
    public static void paverLaysARoad(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        try {
            driver.setGameMode(GameType.CREATIVE); // free: this one's about what it lays
            ground(helper, Blocks.STONE.defaultBlockState(), 0);
            // Five wide, a dashed center line, edge lines, a one-block sidewalk each side, a lamp each side every 8 blocks.
            PaverSettings settings = new PaverSettings(5, PaverSettings.Center.DASHED, 1, true, PaverSettings.Sides.BOTH, 1, 8,
                    PaverSettings.LampSides.BOTH, true, false);
            // Facing north (-z), where the sums for which block's which come out a hair off whole numbers: its left is -x.
            CarEntity paver = paver(helper, driver, CX + 0.5, 1.0, 39.5, 180.0F, settings);
            check(helper, Hitching.available(paver, driver) == Hitching.Action.PAVE_START, "G would lower the screed");
            RoadPaver.toggle(driver, paver);
            check(helper, paver.paving(), "paving");
            drive(paver, 30.0);
            for (int z = 14; z <= 36; z++) {
                check(helper, asphalt(at(helper, CX, 0, z), RoadMarking.CENTER_DASHED, Direction.NORTH), "the center line down the middle at z " + z);
                check(helper, asphalt(at(helper, CX - 1, 0, z), RoadMarking.NONE, Direction.NORTH)
                        && asphalt(at(helper, CX + 1, 0, z), RoadMarking.NONE, Direction.NORTH), "plain asphalt either side of it at z " + z);
                check(helper, marked(at(helper, CX - 2, 0, z), RoadMarking.EDGE_LINE) && marked(at(helper, CX + 2, 0, z), RoadMarking.EDGE_LINE),
                        "edge lines down both edges at z " + z);
                for (int x : new int[] {CX - 3, CX + 3}) {
                    BlockState side = at(helper, x, 1, z);
                    boolean pole = side.is(ModBlocks.STREET_POLE.get());
                    check(helper, pole || side.is(Blocks.SMOOTH_STONE_SLAB) && side.getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                            "a raised sidewalk (or a lamp's pole) at x " + x + " z " + z + ": " + side);
                }
                check(helper, at(helper, CX - 4, 0, z).is(Blocks.STONE) && at(helper, CX + 4, 0, z).is(Blocks.STONE)
                        && at(helper, CX - 4, 1, z).isAir() && at(helper, CX + 4, 1, z).isAir(), "level ground left beyond the sidewalks at z " + z);
            }
            int lamps = 0;
            for (int z = 0; z < 44; z++) {
                for (int x : new int[] {CX - 3, CX + 3}) {
                    if (at(helper, x, 1, z).is(ModBlocks.STREET_POLE.get())) {
                        check(helper, at(helper, x, 4, z).is(ModBlocks.STREET_POLE.get()) && at(helper, x, 5, z).is(ModBlocks.STREET_LAMP.get()),
                                "a lamp on four poles at x " + x + " z " + z);
                        Direction arm = at(helper, x, 5, z).getValue(com.reiko.cars.road.StreetLampBlock.FACING);
                        check(helper, arm == (x == CX + 3 ? Direction.WEST : Direction.EAST), "reaching out over the road: " + arm);
                        lamps++;
                    }
                }
            }
            check(helper, lamps >= 6 && lamps <= 8, "a lamp each side every 8 blocks: " + lamps);
            int[] pieces = {0};
            check(helper, looseEnds(helper, 0, m -> m == RoadMarking.CENTER_DASHED, pieces) == 2 && pieces[0] >= 25,
                    "one unbroken center line: " + pieces[0] + " pieces");
            RoadPaver.toggle(driver, paver);
            check(helper, !paver.paving(), "G raises the screed");
            // The line goes right to the end of the road, on the last block laid too.
            for (int z = 0; z < 44; z++) {
                BlockState middle = at(helper, CX, 0, z);
                if (middle.is(Asphalt.ROAD)) {
                    check(helper, middle.getValue(Asphalt.MARKING) == RoadMarking.CENTER_DASHED, "the center line on every block laid, at z " + z + ": " + middle);
                }
            }
            helper.succeed();
        } finally {
            leave(helper, driver);
        }
    }

    @GameTest(template = PAVING, timeoutTicks = 40, skyAccess = true)
    public static void paverShapesTheLand(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        try {
            driver.setGameMode(GameType.CREATIVE);
            ground(helper, Blocks.DIRT.defaultBlockState(), 9); // ground's top at y 10
            // A hill three high across the way, a ditch two deep, and a gap down to the bottom.
            for (int x = 0; x < WIDE; x++) {
                for (int z = 12; z <= 15; z++) {
                    for (int y = 10; y <= 12; y++) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                    }
                }
                for (int z = 20; z <= 21; z++) {
                    for (int y = 8; y <= 9; y++) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                    }
                }
                for (int z = 28; z <= 35; z++) {
                    for (int y = 1; y <= 9; y++) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                    }
                }
            }
            PaverSettings settings = new PaverSettings(5, PaverSettings.Center.DOUBLE, 1, true, PaverSettings.Sides.NONE, 1, 0,
                    PaverSettings.LampSides.BOTH, true, false);
            CarEntity paver = paver(helper, driver, CX + 0.5, 10.0, 3.5, 0.0F, settings);
            RoadPaver.toggle(driver, paver);
            drive(paver, 36.0);
            double last = Double.NaN;
            double[] surfaces = new double[44];
            for (int z = 6; z <= 33; z++) {
                // The road's surface at the middle: a block of asphalt, or a slab of it at a half.
                double surface = Double.NaN;
                for (int y = 4; y <= 15; y++) {
                    BlockState state = at(helper, CX, y, z);
                    if (state.is(ModBlocks.ASPHALT.get())) {
                        surface = y + 1.0;
                    } else if (state.is(ModBlocks.ASPHALT_SLAB.get())) {
                        surface = y + 0.5;
                    }
                }
                check(helper, !Double.isNaN(surface), "road all the way along, at z " + z);
                surfaces[z] = surface;
                if (!Double.isNaN(last)) {
                    check(helper, Math.abs(surface - last) <= 0.5, "no step steeper than a slab at z " + z + ": " + last + " to " + surface);
                }
                last = surface;
                int base = (int) Math.floor(surface);
                for (int x = CX - 2; x <= CX + 2; x++) {
                    for (int y = base; y < base + RoadPaver.HEADROOM; y++) {
                        BlockState over = at(helper, x, y, z);
                        check(helper, over.isAir() || over.is(Asphalt.ROAD), "the hill cut away over the road at x " + x + " y " + y + " z " + z + ": " + over);
                    }
                    BlockPos under = new BlockPos(x, (surface % 1.0 == 0.0 ? base - 2 : base - 1), z);
                    boolean bridge = z >= 28 && z <= 35;
                    if (!bridge) {
                        check(helper, !helper.getBlockState(under).isAir(), "filled in under the road at " + under);
                    }
                }
            }
            // Beside the cut through the hill, its sides slope back: k blocks out, nothing more than k blocks above the road.
            for (int z = 12; z <= 15; z++) {
                int base = (int) Math.floor(surfaces[z]);
                for (int k = 1; k <= 4; k++) {
                    for (int x : new int[] {CX - 2 - k, CX + 2 + k}) {
                        for (int y = base + k; y <= 12; y++) {
                            check(helper, !at(helper, x, y, z).is(Blocks.STONE), "the cut's side sloped back at x " + x + " y " + y + " z " + z);
                        }
                    }
                }
                check(helper, at(helper, CX + 9, 12, z).is(Blocks.STONE), "the hill left alone further out, at z " + z);
            }
            // Beside the filled ditch, a bank: the ground built up a block out from the road.
            for (int z = 20; z <= 21; z++) {
                int base = (int) Math.floor(surfaces[z]);
                for (int x : new int[] {CX - 3, CX + 3}) {
                    check(helper, !at(helper, x, base - 2, z).isAir(), "a bank beside the road at x " + x + " z " + z);
                }
            }
            // Over the gap: a deck, pillars down to the bottom at its edges, and a railing along both sides.
            int pillars = 0;
            int rails = 0;
            for (int z = 28; z <= 35; z++) {
                for (int x : new int[] {CX - 2, CX + 2}) {
                    if (!at(helper, x, 1, z).isAir() && !at(helper, x, 4, z).isAir()) {
                        pillars++;
                    }
                }
                int base = (int) Math.floor(surfaces[Math.min(z, 33)]);
                for (int x : new int[] {CX - 3, CX + 3}) {
                    if (at(helper, x, base, z).is(BlockTags.WALLS)) {
                        rails++;
                    }
                }
                check(helper, at(helper, CX - 1, 3, z).isAir(), "the gap left open under the bridge at z " + z);
            }
            // No banks built into the gap beside the bridge: it's left as it was.
            for (int z = 29; z <= 34; z++) {
                for (int x = 0; x < WIDE; x++) {
                    if (Math.abs(x - CX) < 4) {
                        continue;
                    }
                    for (int y = 1; y <= 8; y++) {
                        check(helper, at(helper, x, y, z).isAir(), "the gap beside the bridge left open at x " + x + " y " + y + " z " + z);
                    }
                }
            }
            check(helper, pillars >= 2, "pillars under the bridge: " + pillars);
            check(helper, rails >= 14, "a railing along both sides of the bridge: " + rails + " of 16");
            check(helper, !at(helper, CX + 3, 10, 22).is(BlockTags.WALLS) && !at(helper, CX + 3, 10, 16).is(BlockTags.WALLS),
                    "no railing where it's on the ground");
            helper.succeed();
        } finally {
            leave(helper, driver);
        }
    }

    @GameTest(template = PAVING, timeoutTicks = 40, skyAccess = true)
    public static void paverMarksDiagonalRoads(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        try {
            driver.setGameMode(GameType.CREATIVE);
            ground(helper, Blocks.STONE.defaultBlockState(), 0);
            PaverSettings settings = new PaverSettings(5, PaverSettings.Center.DASHED, 1, true, PaverSettings.Sides.NONE, 1, 0,
                    PaverSettings.LampSides.BOTH, false, false);
            // Heading south-east (yaw 315: +x and +z), its middle on a block's middle.
            CarEntity paver = paver(helper, driver, 6.5, 1.0, 5.5, 315.0F, settings);
            RoadPaver.toggle(driver, paver);
            drive(paver, 20.0);
            for (int k = 2; k <= 9; k++) {
                // The line down the middle runs through the blocks along the diagonal from where it started.
                BlockState middle = at(helper, 6 + k, 0, 5 + k);
                check(helper, asphalt(middle, RoadMarking.CENTER_DASHED_DIAGONAL, Direction.EAST), "a diagonal center line at k " + k + ": " + middle);
            }
            // The edges are diagonal lines too, in the outermost row of blocks each side: three rows out from the middle.
            for (int k = 3; k <= 9; k++) {
                for (int[] d : new int[][] {{2, -1}, {-1, 2}}) {
                    BlockState state = at(helper, 6 + k + d[0], 0, 5 + k + d[1]);
                    check(helper, marked(state, RoadMarking.EDGE_DIAGONAL), "a diagonal edge line at k " + k + " " + d[0] + "," + d[1] + ": " + state);
                }
            }
            int[] pieces = {0};
            check(helper, looseEnds(helper, 0, m -> m == RoadMarking.CENTER_DASHED_DIAGONAL, pieces) == 2, "one unbroken diagonal center line");
            helper.succeed();
        } finally {
            leave(helper, driver);
        }
    }

    @GameTest(template = PAVING, timeoutTicks = 40, skyAccess = true)
    public static void paverFollowsABend(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        try {
            driver.setGameMode(GameType.CREATIVE);
            ground(helper, Blocks.STONE.defaultBlockState(), 0);
            PaverSettings settings = new PaverSettings(5, PaverSettings.Center.DASHED, 1, true, PaverSettings.Sides.NONE, 1, 0,
                    PaverSettings.LampSides.BOTH, false, false);
            // South, then a quarter turn to the left (east) about 7.6 blocks out, then on east.
            CarEntity paver = paver(helper, driver, 5.5, 1.0, 4.5, 0.0F, settings);
            RoadPaver.toggle(driver, paver);
            drive(paver, 4.0);
            drive(paver, 12.0, -1.5F);
            drive(paver, 9.0);
            check(helper, Math.abs(net.minecraft.util.Mth.wrapDegrees(paver.getYRot() - 270.0F)) < 0.5F, "heading east after the bend: " + paver.getYRot());
            int[] center = {0};
            int centerEnds = looseEnds(helper, 0, m -> m.name().startsWith("CENTER_DASHED"), center);
            check(helper, centerEnds == 2 && center[0] >= 20, "the center line unbroken round the bend: " + centerEnds + " loose ends, " + center[0] + " pieces");
            int[] edges = {0};
            int edgeEnds = looseEnds(helper, 0, m -> m.name().startsWith("EDGE"), edges);
            check(helper, edgeEnds == 4 && edges[0] >= 40, "both edge lines unbroken round the bend: " + edgeEnds + " loose ends, " + edges[0] + " pieces");
            boolean turned = false;
            for (int x = 0; x < WIDE; x++) {
                for (int z = 0; z < 44; z++) {
                    RoadMarking marking = at(helper, x, 0, z).is(ModBlocks.ASPHALT.get()) ? at(helper, x, 0, z).getValue(Asphalt.MARKING) : RoadMarking.NONE;
                    RoadMarking.Shape shape = marking.shape();
                    turned |= shape == RoadMarking.Shape.TURN || shape == RoadMarking.Shape.SHALLOW_LEFT || shape == RoadMarking.Shape.SHALLOW_RIGHT;
                }
            }
            check(helper, turned, "pieces that bend the lines round");
            // No holes in the road: no stone left with asphalt either side of it, across or along.
            int holes = 0;
            for (int x = 1; x < WIDE - 1; x++) {
                for (int z = 1; z < 43; z++) {
                    if (at(helper, x, 0, z).is(Blocks.STONE)
                            && (at(helper, x - 1, 0, z).is(Asphalt.ROAD) && at(helper, x + 1, 0, z).is(Asphalt.ROAD)
                            || at(helper, x, 0, z - 1).is(Asphalt.ROAD) && at(helper, x, 0, z + 1).is(Asphalt.ROAD))) {
                        holes++;
                    }
                }
            }
            check(helper, holes == 0, "no holes in the road round the bend: " + holes);
            helper.succeed();
        } finally {
            leave(helper, driver);
        }
    }

    @GameTest(template = PAVING, timeoutTicks = 40, skyAccess = true)
    public static void paverUsesTheHopperThenPays(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer driver = join(helper);
        try {
            driver.setGameMode(GameType.SURVIVAL);
            ground(helper, Blocks.DIRT.defaultBlockState(), 0);
            ECO.setBalance(server, driver.getUUID(), 500, "test"); // $5.00
            // Three wide, all lines, nothing else: three blocks of asphalt and three of paint a block of road.
            PaverSettings settings = new PaverSettings(3, PaverSettings.Center.DASHED, 1, true, PaverSettings.Sides.NONE, 1, 0,
                    PaverSettings.LampSides.BOTH, false, true);
            CarEntity paver = paver(helper, driver, CX + 0.5, 1.0, 4.5, 0.0F, settings);
            paver.bed().setItem(0, new ItemStack(ModItems.ASPHALT.get(), 20));
            paver.bed().setItem(1, new ItemStack(ModItems.ROAD_PAINT.get()));
            RoadPaver.toggle(driver, paver);
            drive(paver, 10.0);
            long laid = paver.paverState().laid();
            check(helper, laid >= 27 && laid <= 36, "about ten blocks of road three wide: " + laid);
            check(helper, paver.bed().countItem(ModItems.ASPHALT.get()) == 0, "the hopper's asphalt used first");
            ItemStack paint = paver.bed().getItem(1);
            // Every block's lined (each line's last block gets its piece when the line leaves it, so not those three yet).
            check(helper, paint.is(ModItems.ROAD_PAINT.get()) && paint.getDamageValue() >= laid - 3 && paint.getDamageValue() <= laid,
                    "a can of paint for the lines: " + paint.getDamageValue() + " for " + laid);
            long bought = laid - 20;
            check(helper, paver.paverState().spent() == bought * 10, "the rest bought, at 10 cents a block: " + paver.paverState().spent());
            check(helper, ECO.getBalance(server, driver.getUUID()) == 500 - bought * 10, "from the driver's account: " + ECO.getBalance(server, driver.getUUID()));
            check(helper, paver.bed().countItem(Items.DIRT) == laid, "the dirt it dug up in the hopper: " + paver.bed().countItem(Items.DIRT));
            // Not paying for more: out of asphalt, the screed comes up.
            paver.setPaverSettings(new PaverSettings(3, PaverSettings.Center.DASHED, 1, true, PaverSettings.Sides.NONE, 1, 0,
                    PaverSettings.LampSides.BOTH, false, false));
            drive(paver, 4.0);
            check(helper, !paver.paving(), "out of asphalt, and not buying it: stopped");
            // Too poor to pay: the same.
            paver.setPaverSettings(settings);
            ECO.setBalance(server, driver.getUUID(), 5, "test");
            RoadPaver.toggle(driver, paver);
            drive(paver, 4.0);
            check(helper, !paver.paving(), "can't afford it: stopped");
            check(helper, ECO.getBalance(server, driver.getUUID()) >= 0, "never overdrawn");
            helper.succeed();
        } finally {
            leave(helper, driver);
        }
    }
}
