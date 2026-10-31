package com.reiko.cars.dev;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.key.CarKeys;
import com.reiko.cars.pump.GasPumpBlock;
import com.reiko.cars.pump.GasPumpBlockEntity;
import com.reiko.cars.registry.ModBlocks;
import com.reiko.cars.registry.ModEntities;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.trailer.TrailerEntity;
import com.reiko.cars.trailer.TrailerType;
import com.reiko.cars.workbench.CarPart;
import com.reiko.cars.workbench.CarWorkbenchBlock;
import com.reiko.economy.bank.Purse;
import com.reiko.economy.core.CardService;
import com.reiko.economy.core.EconomyService;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * DEV ONLY (left out of the jar). {@code /reikotest <scene>} builds the scenes for the multiplayer play test
 * ({@code -Pshowcase=mp}, against a dedicated server running a jar with this class in it), and {@code /reikotest
 * report} logs what the server sees, as "REIKOTEST" lines.
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class DevServerTest {
    /** Where the scenes are: the player's spawn area on a flat world. */
    static final int Y = -60;
    static final BlockPos BENCH = new BlockPos(3, Y, -2);
    static final BlockPos PUMP = new BlockPos(8, Y, 2);
    static final BlockPos REPAIR = new BlockPos(14, Y, -2);

    private DevServerTest() {
    }

    @SubscribeEvent
    static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("reikotest")
                .then(Commands.literal("setup").executes(context -> run(context, DevServerTest::setup)))
                .then(Commands.literal("semi").executes(context -> run(context, DevServerTest::semi)))
                .then(Commands.literal("camper").executes(context -> run(context, DevServerTest::camper)))
                .then(Commands.literal("livestock").executes(context -> run(context, DevServerTest::livestock)))
                .then(Commands.literal("rig").executes(context -> run(context, DevServerTest::rig)))
                .then(Commands.literal("night").executes(context -> run(context, player -> player.serverLevel().setDayTime(13000))))
                .then(Commands.literal("clearmanual").executes(context -> run(context, DevServerTest::clearManual)))
                .then(Commands.literal("garage").executes(context -> run(context, DevServerTest::garage)))
                .then(Commands.literal("parking").executes(context -> run(context, DevServerTest::parking)))
                .then(Commands.literal("op").executes(context -> run(context, player -> player.server.getPlayerList().op(player.getGameProfile()))))
                .then(Commands.literal("report").then(Commands.argument("what", StringArgumentType.word())
                        .executes(context -> run(context, player -> report(player, StringArgumentType.getString(context, "what")))))));
    }

    private static int run(CommandContext<CommandSourceStack> context, java.util.function.Consumer<ServerPlayer> action) {
        try {
            action.accept(context.getSource().getPlayerOrException());
            return 1;
        } catch (Exception e) {
            ReikoCars.LOGGER.error("REIKOTEST command failed", e);
            context.getSource().sendFailure(Component.literal("reikotest failed: " + e));
            return 0;
        }
    }

    private static void log(ServerPlayer player, String line) {
        ReikoCars.LOGGER.info("REIKOTEST {}", line);
        player.sendSystemMessage(Component.literal("[test] " + line));
    }

    /** The first scene: money, a workbench and a sedan's parts, a gas pump with fuel, a repair bench, an asphalt road. */
    private static void setup(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        player.server.getPlayerList().op(player.getGameProfile());
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (int x = -4; x <= 70; x++) {
            for (int z = -6; z <= 24; z++) {
                level.setBlockAndUpdate(new BlockPos(x, Y - 1, z), z >= 10 && z <= 14 ? ModBlocks.ASPHALT.get().defaultBlockState()
                        : Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = Y; y < Y + 6; y++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(BENCH, ModBlocks.CAR_WORKBENCH.get().defaultBlockState().setValue(CarWorkbenchBlock.FACING, Direction.SOUTH));
        placePump(level, PUMP, "fuel_light", 12_000, 375);
        level.setBlockAndUpdate(REPAIR, ModBlocks.REPAIR_BENCH.get().defaultBlockState());
        give(player, new ItemStack(ModItems.part(CarType.SEDAN, CarPart.CHASSIS)));
        give(player, new ItemStack(ModItems.part(CarType.SEDAN, CarPart.BODY)));
        give(player, new ItemStack(ModItems.part(CarType.SEDAN, CarPart.INTERIOR)));
        give(player, new ItemStack(ModItems.CAR_ENGINE.get()));
        give(player, new ItemStack(ModItems.TIRE.get(), 4));
        give(player, new ItemStack(ModItems.BLANK_KEY.get(), 2));
        EconomyService.INSTANCE.ensureAccount(player.server, player.getUUID(), player.getGameProfile().getName());
        Purse.give(player, 50_000);
        CardService.issue(player, "1234");
        player.teleportTo(level, BENCH.getX() + 0.5, Y, BENCH.getZ() + 2.5, 180.0F, 30.0F);
        log(player, "setup done");
    }

    /** Puts {@code stack} in the inventory, keeping the manual in slot 0 if it's there. */
    private static void give(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
    }

    private static void placePump(ServerLevel level, BlockPos pos, String fluid, int amount, long price) {
        BlockState lower = ModBlocks.GAS_PUMP.get().defaultBlockState().setValue(GasPumpBlock.FACING, Direction.NORTH);
        level.setBlockAndUpdate(pos, lower);
        level.setBlockAndUpdate(pos.above(), lower.setValue(GasPumpBlock.HALF, DoubleBlockHalf.UPPER));
        GasPumpBlockEntity pump = (GasPumpBlockEntity) level.getBlockEntity(pos);
        pump.setOwner(null);
        pump.setPrice(price);
        pump.tank().fill(new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("buildcraftreborn", fluid)), amount),
                IFluidHandler.FluidAction.EXECUTE);
    }

    private static CarEntity car(ServerPlayer player, CarType type, double x, double z, float yaw) {
        ServerLevel level = player.serverLevel();
        CarEntity car = ModEntities.car(type).create(level);
        car.moveTo(x, Y, z, yaw, 0.0F);
        car.setFuel(type.tankCapacity());
        car.setOwner(player);
        car.setLockCode(CarKeys.newCode());
        level.addFreshEntity(car);
        give(player, CarKeys.keyFor(car));
        return car;
    }

    private static TrailerEntity behind(ServerPlayer player, TrailerType type, CarEntity tractor) {
        ServerLevel level = player.serverLevel();
        float yaw = tractor.getYRot();
        float radians = yaw * net.minecraft.util.Mth.DEG_TO_RAD;
        net.minecraft.world.phys.Vec3 hitch = tractor.hitchPoint();
        TrailerEntity trailer = ModEntities.trailer(type).create(level);
        trailer.moveTo(hitch.x + Math.sin(radians) * type.couplerForward(), Y, hitch.z - Math.cos(radians) * type.couplerForward(), yaw, 0.0F);
        trailer.setOwner(player);
        level.addFreshEntity(trailer);
        return trailer;
    }

    /** A semi facing east along the road with a box trailer standing on its fifth wheel, not hitched. */
    private static void semi(ServerPlayer player) {
        CarEntity semi = car(player, CarType.SEMI_TRUCK, 30.5, 12.5, -90.0F);
        behind(player, TrailerType.BOX_TRAILER, semi);
        player.teleportTo(player.serverLevel(), 30.5, Y, 15.5, 180.0F, 20.0F);
        log(player, "semi ready");
    }

    /** A pickup facing east with a camper on its ball, not hitched. */
    private static void camper(ServerPlayer player) {
        CarEntity pickup = car(player, CarType.PICKUP_TRUCK, 55.5, 20.5, -90.0F);
        behind(player, TrailerType.CAMPER, pickup);
        player.teleportTo(player.serverLevel(), 55.5, Y, 23.0, 180.0F, 20.0F);
        log(player, "camper ready");
    }

    /** A parked livestock trailer, three cows by it, and three leads. */
    private static void livestock(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        TrailerEntity trailer = ModEntities.trailer(TrailerType.LIVESTOCK_TRAILER).create(level);
        trailer.moveTo(20.5, Y, 2.5, 0.0F, 0.0F);
        trailer.setOwner(player);
        level.addFreshEntity(trailer);
        for (int i = 0; i < 3; i++) {
            Cow cow = EntityType.COW.create(level);
            cow.moveTo(25.5 + i, Y, -3.5, 0.0F, 0.0F);
            cow.setNoAi(false);
            level.addFreshEntity(cow);
        }
        give(player, new ItemStack(Items.LEAD, 3));
        player.teleportTo(level, 26.5, Y, -2.0, 180.0F, 30.0F);
        log(player, "livestock ready");
    }

    /** A semi with a tanker hitched, and a livestock trailer with two cows aboard, to be saved and loaded again. */
    private static void rig(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        CarEntity semi = car(player, CarType.SEMI_TRUCK, 30.5, 21.5, -90.0F);
        TrailerEntity tanker = behind(player, TrailerType.TANKER_TRAILER, semi);
        check(tanker.startRiding(semi, true), "tanker hitched");
        TrailerEntity pens = ModEntities.trailer(TrailerType.LIVESTOCK_TRAILER).create(level);
        pens.moveTo(8.5, Y, 20.5, 0.0F, 0.0F);
        pens.setOwner(player);
        level.addFreshEntity(pens);
        for (int i = 0; i < 2; i++) {
            Cow cow = EntityType.COW.create(level);
            cow.moveTo(8.5, Y, 16.5 - i, 0.0F, 0.0F);
            level.addFreshEntity(cow);
            check(com.reiko.cars.trailer.Livestock.load(pens, cow), "cow " + i + " loaded");
        }
        log(player, "rig ready: tanker hitched=" + tanker.isPassenger() + ", cows aboard=" + pens.getPassengers().size());
    }

    /** Where the garages are: the house's door's anchor (its east end: it faces north), and the warehouse's. */
    static final BlockPos GARAGE_DOOR = new BlockPos(45, Y, 0);
    static final BlockPos SHUTTER_DOOR = new BlockPos(53, Y, 0);

    /** Walls and a roof from x0 to x1, z 0 to 8, with a doorway 3 high from doorX0 to doorX1 in the north wall. */
    private static void garageWalls(ServerLevel level, int x0, int x1, int doorX0, int doorX1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = 0; z <= 8; z++) {
                boolean edge = x == x0 || x == x1 || z == 0 || z == 8;
                for (int y = Y; y <= Y + 3; y++) {
                    boolean doorway = z == 0 && x >= doorX0 && x <= doorX1 && y <= Y + 2;
                    level.setBlockAndUpdate(new BlockPos(x, y, z), edge && !doorway ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                level.setBlockAndUpdate(new BlockPos(x, Y + 4, z), Blocks.GRAY_CONCRETE.defaultBlockState());
            }
        }
    }

    /**
     * Two garages: the player's, its sectional door's panels put up one by one; and a warehouse with a roller shutter
     * owned by "Sam", run as a parking garage. A garage remote for the player, who's no longer an admin (admins open
     * every door), in front of their door.
     */
    private static void garage(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        garageWalls(level, 40, 48, 42, 45);
        for (int y = Y; y <= Y + 2; y++) {
            for (int x = 42; x <= 45; x++) {
                BlockPos pos = new BlockPos(x, y, 0);
                BlockState panel = ModBlocks.GARAGE_DOOR.get().defaultBlockState().setValue(com.reiko.cars.garage.GarageDoorBlock.FACING, Direction.NORTH);
                level.setBlockAndUpdate(pos, panel);
                panel.getBlock().setPlacedBy(level, pos, level.getBlockState(pos), player, ItemStack.EMPTY);
            }
        }
        garageWalls(level, 50, 56, 51, 53);
        java.util.UUID sam = java.util.UUID.nameUUIDFromBytes("Sam".getBytes());
        EconomyService.INSTANCE.ensureAccount(player.server, sam, "Sam"); // the money goes somewhere (Sam's never been on)
        for (int y = Y; y <= Y + 2; y++) {
            for (int x = 51; x <= 53; x++) {
                BlockPos pos = new BlockPos(x, y, 0);
                level.setBlockAndUpdate(pos, ModBlocks.ROLLER_SHUTTER.get().defaultBlockState()
                        .setValue(com.reiko.cars.garage.GarageDoorBlock.FACING, Direction.NORTH));
                ((com.reiko.cars.garage.GarageDoorBlockEntity) level.getBlockEntity(pos)).setOwner(sam, "Sam");
            }
        }
        com.reiko.cars.garage.GarageDoors.rejoin(level, SHUTTER_DOOR);
        com.reiko.cars.garage.GarageDoorBlockEntity shutter = com.reiko.cars.garage.GarageDoors.doorAt(level, SHUTTER_DOOR);
        check(shutter != null && shutter.width() == 3, "the shutter's a door");
        check(com.reiko.cars.garage.ParkingService.save(player, shutter, "Sam's Parking", 1 | 2 | 8, "1.00", "8.00").success(), "parking set up");
        com.reiko.cars.garage.GarageDoorBlockEntity door = com.reiko.cars.garage.GarageDoors.doorAt(level, GARAGE_DOOR);
        check(door != null && door.width() == 4 && door.height() == 3, "the house's door is 4 by 3");
        give(player, new ItemStack(ModItems.GARAGE_REMOTE.get()));
        player.server.getPlayerList().deop(player.getGameProfile());
        player.teleportTo(level, 43.5, Y, -2.5, 0.0F, 10.0F);
        log(player, "garage ready: door " + door.width() + "x" + door.height() + " anchor " + door.getBlockPos().toShortString()
                + ", parking " + shutter.settings().name());
    }

    /** The player's hatchback, pulled up to Sam's shutter, the player at the wheel. */
    private static void parking(ServerPlayer player) {
        CarEntity car = car(player, CarType.HATCHBACK, 52.5, -4.2, 0.0F);
        check(player.startRiding(car, true), "in the hatchback");
        log(player, "parking ready");
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            throw new IllegalStateException("reikotest: " + what + " failed");
        }
    }

    private static void clearManual(ServerPlayer player) {
        int removed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("patchouli:guide_book")) {
                removed += stack.getCount();
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        log(player, "manuals removed: " + removed);
    }

    /** Logs what the server sees of the player's world. */
    private static void report(ServerPlayer player, String what) {
        ServerLevel level = player.serverLevel();
        AABB area = new AABB(-20, -70, -30, 90, -40, 40);
        long manuals = player.getInventory().items.stream()
                .filter(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("patchouli:guide_book"))
                .mapToInt(ItemStack::getCount).sum();
        log(player, what + ": manuals=" + manuals + " vehicle=" + (player.getVehicle() == null ? "none" : player.getVehicle().getType().toShortString())
                + " pos=" + String.format("%.1f,%.1f,%.1f", player.getX(), player.getY(), player.getZ()) + " sleeping=" + player.isSleeping()
                + " time=" + level.getDayTime() % 24000);
        for (CarEntity car : level.getEntitiesOfClass(CarEntity.class, area)) {
            log(player, what + ": car " + car.carType() + " at " + String.format("%.1f,%.1f,%.1f", car.getX(), car.getY(), car.getZ())
                    + " fuel=" + String.format("%.0f/%.0f", car.fuel(), (double) car.carType().tankCapacity())
                    + " owner=" + car.ownerName() + " trailer=" + (car.trailer() == null ? "none" : car.trailer().trailerType())
                    + " passengers=" + car.getPassengers().size());
        }
        for (TrailerEntity trailer : level.getEntitiesOfClass(TrailerEntity.class, area)) {
            List<Entity> riders = trailer.getPassengers();
            log(player, what + ": trailer " + trailer.trailerType() + " at " + String.format("%.1f,%.1f,%.1f", trailer.getX(), trailer.getY(), trailer.getZ())
                    + " hitched=" + trailer.isPassenger() + " riders=" + riders.size());
        }
        for (GasPumpBlockEntity pump : List.of(PUMP).stream().map(level::getBlockEntity)
                .filter(GasPumpBlockEntity.class::isInstance).map(GasPumpBlockEntity.class::cast).toList()) {
            log(player, what + ": pump tank=" + pump.tank().getFluidAmount());
        }
        for (BlockPos anchor : new BlockPos[] {GARAGE_DOOR, SHUTTER_DOOR}) {
            com.reiko.cars.garage.GarageDoorBlockEntity door = com.reiko.cars.garage.GarageDoors.doorAt(level, anchor);
            if (door != null) {
                log(player, what + ": garage door " + anchor.toShortString() + " " + door.width() + "x" + door.height() + " name=" + door.settings().name()
                        + " open=" + door.wantsOpen() + " progress=" + door.progress() + " parked=" + door.parked().size() + " earned=" + door.earned());
            }
        }
        log(player, what + ": cash=" + Purse.value(player));
    }
}
