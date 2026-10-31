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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
 * The road paver: the road it lays (asphalt, lines, sidewalks, lamps), shaping the land under it (a hill cut through, a
 * dip filled, a gap bridged on pillars), roads at 45 degrees, and what it takes from the hopper and pays for.
 */
@GameTestHolder(ReikoCars.MODID)
@PrefixGameTestTemplate(false)
public final class RoadPaverGameTests {
    /** 17 wide, 20 high, 44 long: room for a road with sidewalks and lamps, and land to shape. */
    private static final String PAVING = "paving";
    private static final EconomyService ECO = EconomyService.INSTANCE;

    private RoadPaverGameTests() {
    }

    /** Lays ground of {@code block} from y 0 up to {@code top} (inclusive) across the whole test area. */
    private static void ground(GameTestHelper helper, BlockState block, int top) {
        for (int x = 0; x < 17; x++) {
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

    /** Drives the paver {@code blocks} blocks the way it faces, a fifth of a block a tick, paving as it goes. */
    private static void drive(CarEntity paver, double blocks) {
        Vec3 step = paver.forward().scale(0.2);
        int steps = (int) Math.round(blocks / 0.2);
        for (int i = 0; i < steps && paver.paving(); i++) {
            paver.setPos(paver.position().add(step));
            RoadPaver.tick(paver);
        }
    }

    private static BlockState at(GameTestHelper helper, int x, int y, int z) {
        return helper.getBlockState(new BlockPos(x, y, z));
    }

    private static boolean asphalt(BlockState state, RoadMarking marking, Direction facing) {
        return state.is(ModBlocks.ASPHALT.get()) && state.getValue(Asphalt.MARKING) == marking
                && (marking == RoadMarking.NONE || state.getValue(Asphalt.FACING) == facing);
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
            CarEntity paver = paver(helper, driver, 8.5, 1.0, 39.5, 180.0F, settings);
            check(helper, Hitching.available(paver, driver) == Hitching.Action.PAVE_START, "G would lower the screed");
            RoadPaver.toggle(driver, paver);
            check(helper, paver.paving(), "paving");
            drive(paver, 30.0);
            for (int z = 14; z <= 36; z++) {
                check(helper, asphalt(at(helper, 8, 0, z), RoadMarking.CENTER_DASHED, Direction.NORTH), "the center line down the middle at z " + z);
                check(helper, asphalt(at(helper, 7, 0, z), RoadMarking.NONE, Direction.NORTH) && asphalt(at(helper, 9, 0, z), RoadMarking.NONE, Direction.NORTH),
                        "plain asphalt either side of it at z " + z);
                check(helper, asphalt(at(helper, 6, 0, z), RoadMarking.EDGE, Direction.NORTH), "the left edge line, along its outside at z " + z);
                check(helper, asphalt(at(helper, 10, 0, z), RoadMarking.EDGE, Direction.SOUTH), "the right edge line, along its outside at z " + z);
                for (int x : new int[] {5, 11}) {
                    BlockState side = at(helper, x, 1, z);
                    boolean pole = side.is(ModBlocks.STREET_POLE.get());
                    check(helper, pole || side.is(Blocks.SMOOTH_STONE_SLAB) && side.getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                            "a raised sidewalk (or a lamp's pole) at x " + x + " z " + z + ": " + side);
                }
                check(helper, at(helper, 4, 0, z).is(Blocks.STONE) && at(helper, 12, 0, z).is(Blocks.STONE), "nothing beyond the sidewalks at z " + z);
            }
            int lamps = 0;
            for (int z = 0; z < 44; z++) {
                for (int x : new int[] {5, 11}) {
                    if (at(helper, x, 1, z).is(ModBlocks.STREET_POLE.get())) {
                        check(helper, at(helper, x, 4, z).is(ModBlocks.STREET_POLE.get()) && at(helper, x, 5, z).is(ModBlocks.STREET_LAMP.get()),
                                "a lamp on four poles at x " + x + " z " + z);
                        Direction arm = at(helper, x, 5, z).getValue(com.reiko.cars.road.StreetLampBlock.FACING);
                        check(helper, arm == (x == 11 ? Direction.WEST : Direction.EAST), "reaching out over the road: " + arm);
                        lamps++;
                    }
                }
            }
            check(helper, lamps >= 6 && lamps <= 8, "a lamp each side every 8 blocks: " + lamps);
            RoadPaver.toggle(driver, paver);
            check(helper, !paver.paving(), "G raises the screed");
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
            for (int x = 0; x < 17; x++) {
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
            CarEntity paver = paver(helper, driver, 8.5, 10.0, 3.5, 0.0F, settings);
            RoadPaver.toggle(driver, paver);
            drive(paver, 36.0);
            double last = Double.NaN;
            for (int z = 6; z <= 33; z++) {
                // The road's surface at the middle: a block of asphalt, or a slab of it at a half.
                double surface = Double.NaN;
                for (int y = 4; y <= 15; y++) {
                    BlockState state = at(helper, 8, y, z);
                    if (state.is(ModBlocks.ASPHALT.get())) {
                        surface = y + 1.0;
                    } else if (state.is(ModBlocks.ASPHALT_SLAB.get())) {
                        surface = y + 0.5;
                    }
                }
                check(helper, !Double.isNaN(surface), "road all the way along, at z " + z);
                if (!Double.isNaN(last)) {
                    check(helper, Math.abs(surface - last) <= 0.5, "no step steeper than a slab at z " + z + ": " + last + " to " + surface);
                }
                last = surface;
                int base = (int) Math.floor(surface);
                for (int x = 6; x <= 10; x++) {
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
            // Over the gap: a deck, and pillars down to the bottom at its edges.
            int pillars = 0;
            for (int z = 28; z <= 35; z++) {
                for (int x : new int[] {6, 10}) {
                    if (!at(helper, x, 1, z).isAir() && !at(helper, x, 4, z).isAir()) {
                        pillars++;
                    }
                }
                check(helper, at(helper, 7, 3, z).isAir(), "the gap left open under the bridge at z " + z);
            }
            check(helper, pillars >= 2, "pillars under the bridge: " + pillars);
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
            CarEntity paver = paver(helper, driver, 3.5, 1.0, 5.5, 315.0F, settings);
            RoadPaver.toggle(driver, paver);
            drive(paver, 20.0);
            int lined = 0;
            for (int k = 2; k <= 9; k++) {
                // The line down the middle runs through the blocks along the diagonal from where it started.
                BlockState middle = at(helper, 3 + k, 0, 5 + k);
                check(helper, asphalt(middle, RoadMarking.CENTER_DASHED_DIAGONAL, Direction.EAST), "a diagonal center line at k " + k + ": " + middle);
                lined++;
            }
            check(helper, lined > 0, "the center line's there");
            // The edges are diagonal lines too, in the outermost row of blocks each side: three rows out from the middle.
            for (int k = 3; k <= 9; k++) {
                for (int[] d : new int[][] {{2, -1}, {-1, 2}}) {
                    BlockState state = at(helper, 3 + k + d[0], 0, 5 + k + d[1]);
                    check(helper, state.is(ModBlocks.ASPHALT.get()) && state.getValue(Asphalt.MARKING) == RoadMarking.EDGE_DIAGONAL,
                            "a diagonal edge line at k " + k + " " + d[0] + "," + d[1] + ": " + state);
                }
            }
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
            CarEntity paver = paver(helper, driver, 8.5, 1.0, 4.5, 0.0F, settings);
            paver.bed().setItem(0, new ItemStack(ModItems.ASPHALT.get(), 20));
            paver.bed().setItem(1, new ItemStack(ModItems.ROAD_PAINT.get()));
            RoadPaver.toggle(driver, paver);
            drive(paver, 10.0);
            long laid = paver.paverState().laid();
            check(helper, laid >= 27 && laid <= 36, "about ten blocks of road three wide: " + laid);
            check(helper, paver.bed().countItem(ModItems.ASPHALT.get()) == 0, "the hopper's asphalt used first");
            ItemStack paint = paver.bed().getItem(1);
            check(helper, paint.is(ModItems.ROAD_PAINT.get()) && paint.getDamageValue() == laid, "a can of paint for the lines: " + paint.getDamageValue());
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
