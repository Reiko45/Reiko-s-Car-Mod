package com.reiko.cars.gametest;

import com.mojang.authlib.GameProfile;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarItem;
import com.reiko.cars.car.CarFuel;
import com.reiko.cars.car.CarTank;
import com.reiko.cars.car.CarType;
import com.reiko.cars.car.FuelType;
import com.reiko.cars.pump.GasPumpBlock;
import com.reiko.cars.pump.GasPumpBlockEntity;
import com.reiko.cars.pump.PumpService;
import com.reiko.cars.registry.ModBlocks;
import com.reiko.cars.registry.ModEntities;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.repair.RepairBenchBlock;
import com.reiko.cars.repair.RepairBenchBlockEntity;
import com.reiko.cars.repair.RepairJobs;
import com.reiko.cars.repair.RepairService;
import com.reiko.cars.trailer.Hitching;
import com.reiko.cars.trailer.TankerService;
import com.reiko.cars.trailer.TrailerEntity;
import com.reiko.cars.trailer.TrailerType;
import com.reiko.cars.workbench.CarAssemblyRecipe;
import com.reiko.cars.workbench.CarPart;
import com.reiko.cars.workbench.CarWorkbenchMenu;
import com.reiko.economy.bank.Purse;
import com.reiko.economy.core.CardService;
import com.reiko.economy.core.EconomyService;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-world tests: selling fuel for cash and card through the economy mod, the owner's free fill-ups, pipes and
 * buckets, gasoline and diesel, selling into buckets, fuel burned by driving, BuildCraft's fuel values, car locks and
 * building cars at the Car Workbench, semis: diesel, hitching and towing trailers, tankers and car haulers, and wear and
 * tear: parts wearing out, breaking down, and repairs at the Repair Bench, free or paid for. Run them with
 * {@code ./gradlew runGameTestServer}. The economy mod turns taxes off on the test server, so amounts are exact.
 */
@GameTestHolder(ReikoCars.MODID)
@PrefixGameTestTemplate(false)
public final class CarGameTests {
    private static final String EMPTY = "empty";
    private static final BlockPos PUMP = new BlockPos(1, 1, 1);
    private static final long FOUR_DOLLARS = 400;
    private static final EconomyService ECO = EconomyService.INSTANCE;

    private CarGameTests() {
    }

    // ---- helpers ----

    private static Fluid bc(String name) {
        Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("buildcraftreborn", name));
        if (fluid == Fluids.EMPTY) {
            throw new IllegalStateException("BuildCraft Reborn has no fluid " + name);
        }
        return fluid;
    }

    /** Logs a fake player in, which opens their bank account (as the economy mod's own tests do). */
    private static ServerPlayer join(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        UUID id = UUID.randomUUID();
        String name = "car_" + id.toString().replace("-", "").substring(0, 10);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(id, name), false);
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        try {
            server.getPlayerList().placeNewPlayer(connection, player, cookie);
        } catch (UnsupportedOperationException e) {
            // BuildCraft sends a packet on login that a fake connection can't take. The player is in by then, but
            // other mods' login handlers may not have run, so open the bank account ourselves.
        }
        ECO.ensureAccount(server, id, name);
        player.getInventory().clearContent();
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        MinecraftServer server = helper.getLevel().getServer();
        for (ServerPlayer player : players) {
            if (player.isPassenger()) {
                player.stopRiding();
            }
            if (server.getPlayerList().getPlayer(player.getUUID()) == player) {
                server.getPlayerList().remove(player);
            }
        }
    }

    /** A gas pump owned by {@code owner}, holding {@code millibuckets} of {@code fuel} at $4.00 a bucket. */
    private static GasPumpBlockEntity pump(GameTestHelper helper, ServerPlayer owner, Fluid fuel, int millibuckets) {
        return pump(helper, ModBlocks.GAS_PUMP.get(), owner, fuel, millibuckets);
    }

    private static GasPumpBlockEntity pump(GameTestHelper helper, GasPumpBlock block, ServerPlayer owner, Fluid fuel, int millibuckets) {
        BlockState lower = block.defaultBlockState().setValue(GasPumpBlock.FACING, Direction.NORTH);
        helper.setBlock(PUMP, lower);
        helper.setBlock(PUMP.above(), lower.setValue(GasPumpBlock.HALF, DoubleBlockHalf.UPPER));
        GasPumpBlockEntity pump = (GasPumpBlockEntity) helper.getBlockEntity(PUMP);
        pump.setOwner(owner);
        pump.setPrice(FOUR_DOLLARS);
        if (millibuckets > 0) {
            pump.tank().fill(new FluidStack(fuel, millibuckets), IFluidHandler.FluidAction.EXECUTE);
        }
        return pump;
    }

    /** An empty car parked next to the pump. */
    private static CarEntity car(GameTestHelper helper, CarType type) {
        CarEntity car = ModEntities.car(type).create(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 1.0, 1.5));
        car.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(car);
        return car;
    }

    private static void check(GameTestHelper helper, boolean condition, String what) {
        helper.assertTrue(condition, what);
    }

    private static void near(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-3 * Math.max(1.0, Math.abs(expected)),
                what + ": expected " + expected + " but was " + actual);
    }

    // ---- tests ----

    @GameTest(template = EMPTY)
    public static void buildcraftFuelValues(GameTestHelper helper) {
        near(helper, CarFuel.energyPerBucket(new FluidStack(bc("fuel_light"), 1)), 90_000, "light fuel MJ per bucket");
        near(helper, CarFuel.energyPerBucket(new FluidStack(bc("fuel_dense"), 1)), 360_000, "dense fuel MJ per bucket");
        check(helper, FuelType.of(new FluidStack(bc("fuel_light"), 1)) == FuelType.GASOLINE, "light fuel is gasoline");
        check(helper, FuelType.of(new FluidStack(bc("fuel_dense"), 1)) == FuelType.DIESEL, "dense fuel is diesel");
        check(helper, FuelType.of(new FluidStack(bc("fuel_mixed_heavy"), 1)) == FuelType.DIESEL, "so are the mixed heavy fuels");
        check(helper, !CarFuel.isFuel(new FluidStack(bc("oil"), 1)), "crude oil isn't a fuel");
        check(helper, !CarFuel.isFuel(new FluidStack(Fluids.WATER, 1)), "nor is water");
        for (CarType type : CarType.values()) {
            boolean diesel = type.fuel() == FuelType.DIESEL;
            check(helper, CarFuel.runs(type, new FluidStack(bc("fuel_light"), 1)) != diesel, type.id() + " runs on gasoline unless it's diesel");
            check(helper, CarFuel.runs(type, new FluidStack(bc("fuel_dense"), 1)) == diesel, type.id() + " runs on diesel only if it's diesel");
        }
        check(helper, CarType.SEMI_TRUCK.fuel() == FuelType.DIESEL, "the semi runs on diesel");
        check(helper, CarType.SEMI_TRUCK.isTractor() && !CarType.PICKUP_TRUCK.isTractor(), "only the semi pulls trailers");
        near(helper, CarFuel.millibucketsThatFit(new FluidStack(bc("fuel_light"), 1), CarType.SEDAN.tankCapacity()), 8000,
                "a sedan holds 8 buckets of light fuel");
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void cashPaysTheOwner(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer owner = join(helper);
        ServerPlayer buyer = join(helper);
        try {
            GasPumpBlockEntity pump = pump(helper, owner, bc("fuel_light"), 5000);
            CarEntity car = car(helper, CarType.SEDAN);
            long ownerBefore = ECO.getBalance(server, owner.getUUID());
            Purse.give(buyer, 1000); // $10.00 in cash

            ShopActionResult result = PumpService.fill(buyer, pump, car, 2000, FOUR_DOLLARS, Payment.cash());
            check(helper, result.success(), "cash fill-up: " + result.message().getString());
            near(helper, car.fuel(), 180_000, "two buckets of light fuel in the tank");
            helper.assertValueEqual(pump.fuel().getAmount(), 3000, "fuel left in the pump");
            helper.assertValueEqual(Purse.value(buyer), 200L, "$8.00 cash spent");
            helper.assertValueEqual(ECO.getBalance(server, owner.getUUID()), ownerBefore + 800, "owner paid $8.00");
            helper.assertValueEqual(pump.millibucketsSold(), 2000L, "sale recorded");

            ShopActionResult broke = PumpService.fill(buyer, pump, car, 1000, FOUR_DOLLARS, Payment.cash());
            check(helper, !broke.success(), "$2.00 doesn't buy a $4.00 bucket");
            helper.assertValueEqual(pump.fuel().getAmount(), 3000, "nothing pumped when payment fails");
        } finally {
            leave(helper, owner, buyer);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void debitCardNeedsTheRightPin(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer owner = join(helper);
        ServerPlayer buyer = join(helper);
        try {
            GasPumpBlockEntity pump = pump(helper, owner, bc("fuel_light"), 4000);
            CarEntity car = car(helper, CarType.SPORTS_CAR);
            ECO.setBalance(server, buyer.getUUID(), 10_000, "gametest.setup");
            check(helper, CardService.issue(buyer, "4321").success(), "card issued");
            long ownerBefore = ECO.getBalance(server, owner.getUUID());

            ShopActionResult wrong = PumpService.fill(buyer, pump, car, 1500, FOUR_DOLLARS, Payment.card("1111"));
            check(helper, !wrong.success(), "wrong PIN refused");
            near(helper, car.fuel(), 0, "no fuel for a wrong PIN");

            ShopActionResult paid = PumpService.fill(buyer, pump, car, 1500, FOUR_DOLLARS, Payment.card("4321"));
            check(helper, paid.success(), "card fill-up: " + paid.message().getString());
            helper.assertValueEqual(ECO.getBalance(server, buyer.getUUID()), 10_000L - 600, "card charged $6.00");
            helper.assertValueEqual(ECO.getBalance(server, owner.getUUID()), ownerBefore + 600, "owner paid $6.00");
            near(helper, car.fuel(), 135_000, "a bucket and a half in the tank");
        } finally {
            leave(helper, owner, buyer);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fillUpStopsAtAFullTank(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        try {
            GasPumpBlockEntity pump = pump(helper, owner, bc("fuel_light"), 16_000);
            CarEntity car = car(helper, CarType.SEDAN);
            car.setFuel(CarType.SEDAN.tankCapacity() - 45_000); // half a bucket of room
            helper.assertValueEqual(PumpService.room(pump, car), 500, "room for half a bucket");

            ShopActionResult result = PumpService.fill(owner, pump, car, 10_000, FOUR_DOLLARS, Payment.cash());
            check(helper, result.success(), "owner fills up: " + result.message().getString());
            near(helper, car.fuel(), CarType.SEDAN.tankCapacity(), "tank full");
            helper.assertValueEqual(pump.fuel().getAmount(), 15_500, "only what fit was pumped");
            helper.assertValueEqual(pump.millibucketsSold(), 0L, "the owner's own fuel isn't a sale");
            check(helper, !PumpService.fill(owner, pump, car, 1000, FOUR_DOLLARS, Payment.cash()).success(), "a full tank takes no more");
        } finally {
            leave(helper, owner);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void priceChangeIsCaught(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        ServerPlayer buyer = join(helper);
        try {
            GasPumpBlockEntity pump = pump(helper, owner, bc("fuel_light"), 4000);
            CarEntity car = car(helper, CarType.SEDAN);
            Purse.give(buyer, 5000);
            check(helper, PumpService.setPrice(owner, pump, "9.99").success(), "owner sets a price");
            check(helper, !PumpService.setPrice(buyer, pump, "0.01").success(), "a customer can't");
            ShopActionResult stale = PumpService.fill(buyer, pump, car, 1000, FOUR_DOLLARS, Payment.cash());
            check(helper, !stale.success(), "buying at the old price is refused");
            helper.assertValueEqual(Purse.value(buyer), 5000L, "no money taken");
            check(helper, PumpService.fill(buyer, pump, car, 1000, 999, Payment.cash()).success(), "the new price works");
            helper.assertValueEqual(Purse.value(buyer), 5000L - 999, "$9.99 paid");
        } finally {
            leave(helper, owner, buyer);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void pipesFillButNeverDrain(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        try {
            GasPumpBlockEntity pump = pump(helper, owner, bc("fuel_light"), 0);
            IFluidHandler pipe = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    helper.absolutePos(PUMP.above()), Direction.UP);
            check(helper, pipe != null, "the top half takes pipes too");
            helper.assertValueEqual(pipe.fill(new FluidStack(bc("oil"), 1000), IFluidHandler.FluidAction.EXECUTE), 0, "no crude oil");
            helper.assertValueEqual(pipe.fill(new FluidStack(bc("fuel_light"), 1000), IFluidHandler.FluidAction.EXECUTE), 1000, "fuel goes in");
            helper.assertValueEqual(pipe.fill(new FluidStack(bc("fuel_dense"), 1000), IFluidHandler.FluidAction.EXECUTE), 0, "one fuel at a time");
            check(helper, pipe.drain(500, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "nothing comes out of a pipe");
            helper.assertValueEqual(pump.fuel().getAmount(), 1000, "the pump kept its fuel");
        } finally {
            leave(helper, owner);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void bucketsFillTheTank(GameTestHelper helper) {
        CarEntity car = car(helper, CarType.PICKUP_TRUCK);
        CarTank tank = new CarTank(car);
        helper.assertValueEqual(tank.fill(new FluidStack(bc("fuel_dense"), 1000), IFluidHandler.FluidAction.EXECUTE), 0, "no diesel");
        helper.assertValueEqual(tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE), 0, "no water");
        helper.assertValueEqual(tank.fill(new FluidStack(bc("fuel_light"), 1000), IFluidHandler.FluidAction.EXECUTE), 1000, "light fuel goes in");
        near(helper, car.fuel(), 90_000, "a bucket of light fuel");
        car.setFuel(CarType.PICKUP_TRUCK.tankCapacity() - 10_000);
        helper.assertValueEqual(tank.fill(new FluidStack(bc("fuel_light"), 1000), IFluidHandler.FluidAction.SIMULATE), 111,
                "only what fits");
        helper.succeed();
    }

    @GameTest(template = YARD, timeoutTicks = 60)
    public static void drivingBurnsFuel(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        CarEntity car = car(helper, CarType.SEDAN);
        car.setOwner(driver);
        car.setFuel(100_000);
        driver.startRiding(car, true);
        Vec3 start = car.position();
        // Drive 10 blocks south, one block a tick, as the driver's client would report it. Start a couple of ticks
        // in, so the car has ticked where it stands first and the first block counts. (Down the yard: a car that
        // leaves the test's area stops ticking.)
        for (int tick = 1; tick <= 10; tick++) {
            int step = tick;
            helper.runAtTickTime(tick + 2, () -> car.setPos(start.x, start.y, start.z + step));
        }
        helper.runAtTickTime(16, () -> {
            try {
                double burned = 100_000 - car.fuel();
                double expected = 10 * CarType.SEDAN.fuelPerBlock; // plus a little idling
                helper.assertTrue(burned >= expected && burned < expected + 20, "burned " + burned + " MJ for 10 blocks");
                driver.stopRiding();
                double parked = car.fuel();
                car.setPos(start.x, start.y, start.z + 20);
                helper.runAfterDelay(2, () -> {
                    near(helper, car.fuel(), parked, "a car with nobody in it burns nothing");
                    leave(helper, driver);
                    helper.succeed();
                });
            } catch (RuntimeException e) {
                leave(helper, driver);
                throw e;
            }
        });
    }

    @GameTest(template = EMPTY)
    public static void lockedCarsKeepStrangersOut(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        ServerPlayer stranger = join(helper);
        try {
            CarEntity car = car(helper, CarType.OFF_ROADER);
            car.setOwner(owner);
            car.setLocked(true);
            check(helper, car.canBeUsedBy(owner), "the owner can get in");
            check(helper, !car.canBeUsedBy(stranger), "a stranger can't");
            check(helper, !car.canManage(stranger), "nor pick it up");
            owner.startRiding(car, true);
            check(helper, car.canBeUsedBy(stranger), "but can ride along with the owner");
            check(helper, car.getControllingPassenger() == owner, "the owner drives");
            check(helper, car.toggleLock(owner) && !car.isLocked(), "the owner unlocks it");
            check(helper, !car.toggleLock(stranger), "a stranger can't lock it");
        } finally {
            leave(helper, owner, stranger);
        }
        helper.succeed();
    }

    // ---- the Car Workbench ----

    private static final BlockPos WORKBENCH = new BlockPos(2, 1, 2);

    private static CarWorkbenchMenu workbench(GameTestHelper helper, ServerPlayer player) {
        helper.setBlock(WORKBENCH, ModBlocks.CAR_WORKBENCH.get().defaultBlockState());
        return new CarWorkbenchMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(WORKBENCH)));
    }

    /** The car's button in the workbench's list. */
    private static int button(CarWorkbenchMenu menu, CarType type) {
        for (int i = 0; i < menu.recipes().size(); i++) {
            if (menu.recipes().get(i).value().result().is(ModItems.car(type))) {
                return i;
            }
        }
        throw new IllegalStateException("The workbench can't build a " + type.id());
    }

    /** The trailer's button in the workbench's list. */
    private static int button(CarWorkbenchMenu menu, TrailerType type) {
        for (int i = 0; i < menu.recipes().size(); i++) {
            if (menu.recipes().get(i).value().result().is(ModItems.trailer(type))) {
                return i;
            }
        }
        throw new IllegalStateException("The workbench can't build a " + type.id());
    }

    private static void giveParts(ServerPlayer player, CarType type) {
        for (CarPart part : CarPart.values()) {
            player.getInventory().add(new ItemStack(ModItems.part(type, part)));
        }
        player.getInventory().add(new ItemStack(ModItems.CAR_ENGINE.get()));
        player.getInventory().add(new ItemStack(ModItems.TIRE.get(), 4));
    }

    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }

    /** The menu slot showing the player's stack of {@code item}. */
    private static int inventorySlot(CarWorkbenchMenu menu, ServerPlayer player, Item item) {
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory() && slot.getItem().is(item)) {
                return slot.index;
            }
        }
        throw new IllegalStateException("No " + item + " in the inventory");
    }

    @GameTest(template = EMPTY)
    public static void workbenchBuildsACar(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        try {
            CarWorkbenchMenu menu = workbench(helper, player);
            helper.assertValueEqual(menu.recipes().size(), CarType.values().length + TrailerType.values().length,
                    "one recipe per car and trailer");
            for (CarType type : CarType.values()) {
                helper.assertValueEqual(button(menu, type), type.ordinal(), "cars listed in order");
            }
            for (TrailerType type : TrailerType.values()) {
                helper.assertValueEqual(button(menu, type), CarType.values().length + type.ordinal(), "then the trailers");
            }
            giveParts(player, CarType.SEDAN);
            check(helper, menu.clickMenuButton(player, button(menu, CarType.SEDAN)), "sedan picked");
            for (int slot = 0; slot < CarWorkbenchMenu.PARTS; slot++) {
                check(helper, menu.getSlot(slot).hasItem(), "part slot " + slot + " filled from the inventory");
            }
            helper.assertValueEqual(count(player, ModItems.TIRE.get()), 0, "all four tires went in");
            check(helper, menu.getSlot(CarWorkbenchMenu.RESULT).getItem().is(ModItems.car(CarType.SEDAN)), "the parts make a sedan");

            menu.clicked(CarWorkbenchMenu.RESULT, 0, ClickType.QUICK_MOVE, player);
            helper.assertValueEqual(count(player, ModItems.car(CarType.SEDAN)), 1, "the sedan is in the inventory");
            for (int slot = 0; slot < CarWorkbenchMenu.PARTS; slot++) {
                check(helper, !menu.getSlot(slot).hasItem(), "part " + slot + " used up");
            }
            check(helper, !menu.getSlot(CarWorkbenchMenu.RESULT).hasItem(), "nothing left to take");
            helper.assertValueEqual(count(player, ModItems.car(CarType.SEDAN)), 1, "only one sedan for one set of parts");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void mixedPartsMakeNothing(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        try {
            CarWorkbenchMenu menu = workbench(helper, player);
            // Everything for a sedan except its body, and a sports car's body instead.
            player.getInventory().add(new ItemStack(ModItems.part(CarType.SEDAN, CarPart.CHASSIS)));
            player.getInventory().add(new ItemStack(ModItems.part(CarType.SEDAN, CarPart.INTERIOR)));
            player.getInventory().add(new ItemStack(ModItems.CAR_ENGINE.get()));
            player.getInventory().add(new ItemStack(ModItems.TIRE.get(), 4));
            player.getInventory().add(new ItemStack(ModItems.part(CarType.SPORTS_CAR, CarPart.BODY)));

            menu.clickMenuButton(player, button(menu, CarType.SEDAN));
            check(helper, !menu.getSlot(CarAssemblyRecipe.BODY).hasItem(), "the sports car's body isn't loaded into a sedan");
            menu.clicked(inventorySlot(menu, player, ModItems.part(CarType.SPORTS_CAR, CarPart.BODY)), 0, ClickType.QUICK_MOVE, player);
            check(helper, menu.getSlot(CarAssemblyRecipe.BODY).getItem().is(ModItems.part(CarType.SPORTS_CAR, CarPart.BODY)),
                    "but it can be put in by hand");
            check(helper, !menu.getSlot(CarWorkbenchMenu.RESULT).hasItem(), "a sedan with a sports car's body makes nothing");

            menu.clickMenuButton(player, button(menu, CarType.SPORTS_CAR));
            check(helper, menu.getSlot(CarAssemblyRecipe.BODY).getItem().is(ModItems.part(CarType.SPORTS_CAR, CarPart.BODY)),
                    "picking the sports car keeps its body");
            check(helper, !menu.getSlot(CarAssemblyRecipe.CHASSIS).hasItem(), "and hands back the sedan's chassis");
            helper.assertValueEqual(count(player, ModItems.part(CarType.SEDAN, CarPart.CHASSIS)), 1, "the chassis is back in the inventory");
            check(helper, menu.getSlot(CarAssemblyRecipe.ENGINE).hasItem(), "the engine fits either car, so it stays");
            check(helper, !menu.getSlot(CarWorkbenchMenu.RESULT).hasItem(), "still no car");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void shiftClickSortsParts(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        try {
            CarWorkbenchMenu menu = workbench(helper, player);
            player.getInventory().add(new ItemStack(ModItems.TIRE.get(), 6));
            player.getInventory().add(new ItemStack(ModItems.part(CarType.PICKUP_TRUCK, CarPart.CHASSIS)));
            player.getInventory().add(new ItemStack(Items.DIRT, 5));

            menu.clicked(inventorySlot(menu, player, ModItems.TIRE.get()), 0, ClickType.QUICK_MOVE, player);
            for (int wheel = 0; wheel < 4; wheel++) {
                check(helper, menu.getSlot(CarAssemblyRecipe.FIRST_WHEEL + wheel).getItem().getCount() == 1, "one tire on wheel " + wheel);
            }
            helper.assertValueEqual(count(player, ModItems.TIRE.get()), 2, "two spare tires left over");
            menu.clicked(inventorySlot(menu, player, ModItems.part(CarType.PICKUP_TRUCK, CarPart.CHASSIS)), 0, ClickType.QUICK_MOVE, player);
            check(helper, menu.getSlot(CarAssemblyRecipe.CHASSIS).getItem().is(ModItems.part(CarType.PICKUP_TRUCK, CarPart.CHASSIS)),
                    "the chassis went to the chassis slot");
            menu.clicked(inventorySlot(menu, player, Items.DIRT), 0, ClickType.QUICK_MOVE, player);
            for (int slot = 0; slot < CarWorkbenchMenu.PARTS; slot++) {
                check(helper, !menu.getSlot(slot).getItem().is(Items.DIRT), "dirt isn't a car part");
            }
            helper.assertValueEqual(count(player, Items.DIRT), 5, "the dirt stays in the inventory");

            menu.removed(player);
            helper.assertValueEqual(count(player, ModItems.TIRE.get()), 6, "closing the workbench gives the tires back");
            helper.assertValueEqual(count(player, ModItems.part(CarType.PICKUP_TRUCK, CarPart.CHASSIS)), 1, "and the chassis");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    // ---- diesel pumps and selling by the bucket ----

    @GameTest(template = EMPTY)
    public static void dieselPumpSellsDieselByTheBucket(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer owner = join(helper);
        ServerPlayer buyer = join(helper);
        try {
            GasPumpBlockEntity pump = pump(helper, ModBlocks.DIESEL_PUMP.get(), owner, bc("fuel_dense"), 5000);
            check(helper, pump.fuelType() == FuelType.DIESEL, "a diesel pump");
            IFluidHandler pipe = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    helper.absolutePos(PUMP), Direction.UP);
            helper.assertValueEqual(pipe.fill(new FluidStack(bc("fuel_dense"), 1000), IFluidHandler.FluidAction.EXECUTE), 1000,
                    "pipes top it up with dense fuel");
            helper.assertValueEqual(pump.fuel().getAmount(), 6000, "six buckets in the pump");

            CarEntity car = car(helper, CarType.PICKUP_TRUCK);
            Purse.give(buyer, 2000);
            ShopActionResult wrongCar = PumpService.fill(buyer, pump, car, 1000, FOUR_DOLLARS, Payment.cash());
            check(helper, !wrongCar.success(), "no diesel in a gasoline car");
            near(helper, car.fuel(), 0, "the car got nothing");
            helper.assertValueEqual(Purse.value(buyer), 2000L, "and the buyer paid nothing");

            ShopActionResult noBuckets = PumpService.fillBuckets(buyer, pump, 2, FOUR_DOLLARS, Payment.cash());
            check(helper, !noBuckets.success(), "no empty buckets, no sale");

            buyer.getInventory().add(new ItemStack(Items.BUCKET, 3));
            long ownerBefore = ECO.getBalance(server, owner.getUUID());
            ShopActionResult sold = PumpService.fillBuckets(buyer, pump, 5, FOUR_DOLLARS, Payment.cash());
            check(helper, sold.success(), "bucket sale: " + sold.message().getString());
            helper.assertValueEqual(count(buyer, Items.BUCKET), 0, "all three empty buckets used");
            int full = 0;
            for (ItemStack stack : buyer.getInventory().items) {
                if (FluidUtil.getFluidContained(stack).filter(fluid -> fluid.getFluid() == bc("fuel_dense") && fluid.getAmount() == 1000).isPresent()) {
                    full += stack.getCount();
                }
            }
            helper.assertValueEqual(full, 3, "three buckets of dense fuel");
            helper.assertValueEqual(pump.fuel().getAmount(), 3000, "three buckets left in the pump");
            helper.assertValueEqual(Purse.value(buyer), 2000L - 1200, "$12.00 paid");
            helper.assertValueEqual(ECO.getBalance(server, owner.getUUID()), ownerBefore + 1200, "owner paid $12.00");

            // A bucket of diesel can't be poured into a gasoline car either.
            ItemStack dieselBucket = FluidUtil.getFilledBucket(new FluidStack(bc("fuel_dense"), 1000));
            buyer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dieselBucket.copy());
            car.interact(buyer, net.minecraft.world.InteractionHand.MAIN_HAND);
            near(helper, car.fuel(), 0, "no diesel poured into the pickup");
            check(helper, ItemStack.isSameItemSameComponents(buyer.getMainHandItem(), dieselBucket), "the bucket is still full");
            check(helper, !buyer.isPassenger(), "and the buyer didn't get in");
        } finally {
            leave(helper, owner, buyer);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void pumpsOnlyTakeTheirOwnFuel(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        try {
            GasPumpBlockEntity diesel = pump(helper, ModBlocks.DIESEL_PUMP.get(), owner, bc("fuel_light"), 0);
            helper.assertValueEqual(diesel.pipeInput().fill(new FluidStack(bc("fuel_light"), 1000), IFluidHandler.FluidAction.EXECUTE), 0,
                    "no gasoline in a diesel pump");
            helper.assertValueEqual(diesel.pipeInput().fill(new FluidStack(bc("fuel_mixed_heavy"), 1000), IFluidHandler.FluidAction.EXECUTE), 1000,
                    "the mixed heavy fuels count as diesel");
            helper.setBlock(PUMP.above(), net.minecraft.world.level.block.Blocks.AIR);
            helper.setBlock(PUMP, net.minecraft.world.level.block.Blocks.AIR);
            GasPumpBlockEntity gas = pump(helper, owner, bc("fuel_light"), 0);
            helper.assertValueEqual(gas.pipeInput().fill(new FluidStack(bc("fuel_dense"), 1000), IFluidHandler.FluidAction.EXECUTE), 0,
                    "no diesel in a gas pump");
            helper.assertValueEqual(gas.pipeInput().fill(new FluidStack(bc("fuel_light"), 1000), IFluidHandler.FluidAction.EXECUTE), 1000,
                    "gasoline goes in");

            // The owner can take fuel back out of their own pump in buckets, free.
            owner.getInventory().add(new ItemStack(Items.BUCKET, 1));
            check(helper, PumpService.fillBuckets(owner, gas, 1, FOUR_DOLLARS, Payment.cash()).success(), "owner fills a bucket");
            helper.assertValueEqual(gas.fuel().getAmount(), 0, "the pump is empty again");
            helper.assertValueEqual(gas.earned(), 0L, "and the owner didn't pay themselves");
        } finally {
            leave(helper, owner);
        }
        helper.succeed();
    }

    // ---- semis and trailers ----

    /** A long empty yard (10 x 34 blocks) for the semi tests: a rig is 15 blocks long. */
    private static final String YARD = "yard";

    /** A {@code type} in the yard, its middle at ({@code x}, {@code z}), facing south (+z). */
    private static CarEntity vehicle(GameTestHelper helper, CarType type, double x, double z) {
        CarEntity car = ModEntities.car(type).create(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(x, 1.0, z));
        car.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(car);
        return car;
    }

    private static TrailerEntity trailer(GameTestHelper helper, TrailerType type, Vec3 relative, float yaw) {
        Vec3 at = helper.absoluteVec(relative);
        return trailerAt(helper, type, at, yaw);
    }

    private static TrailerEntity trailerAt(GameTestHelper helper, TrailerType type, Vec3 at, float yaw) {
        TrailerEntity trailer = ModEntities.trailer(type).create(helper.getLevel());
        trailer.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        helper.getLevel().addFreshEntity(trailer);
        return trailer;
    }

    /** A trailer standing right behind {@code tractor}, lined up, its kingpin over the fifth wheel. */
    private static TrailerEntity readyToHitch(GameTestHelper helper, TrailerType type, CarEntity tractor) {
        Vec3 fifthWheel = tractor.fifthWheel();
        Vec3 forward = tractor.forward();
        Vec3 at = new Vec3(fifthWheel.x - forward.x * TrailerType.KINGPIN_FORWARD, tractor.getY(),
                fifthWheel.z - forward.z * TrailerType.KINGPIN_FORWARD);
        return trailerAt(helper, type, at, tractor.getYRot());
    }

    private static Vec3 facing(float yaw) {
        float radians = yaw * net.minecraft.util.Mth.DEG_TO_RAD;
        return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
    }

    private static double flatDistance(Vec3 a, Vec3 b) {
        return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z));
    }

    @GameTest(template = EMPTY)
    public static void semiRunsOnDiesel(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        ServerPlayer buyer = join(helper);
        try {
            GasPumpBlockEntity pump = pump(helper, ModBlocks.DIESEL_PUMP.get(), owner, bc("fuel_dense"), 10_000);
            CarEntity semi = car(helper, CarType.SEMI_TRUCK);
            near(helper, CarType.SEMI_TRUCK.tankCapacity(), CarType.SEMI_TRUCK.tankBuckets * 360_000.0, "the tank is measured in buckets of diesel");
            CarTank tank = new CarTank(semi);
            helper.assertValueEqual(tank.fill(new FluidStack(bc("fuel_light"), 1000), IFluidHandler.FluidAction.SIMULATE), 0,
                    "no gasoline in a semi");

            Purse.give(buyer, 2000);
            ShopActionResult result = PumpService.fill(buyer, pump, semi, 3000, FOUR_DOLLARS, Payment.cash());
            check(helper, result.success(), "diesel fill-up: " + result.message().getString());
            near(helper, semi.fuel(), 3 * 360_000, "three buckets of diesel in the tank");
            helper.assertValueEqual(Purse.value(buyer), 2000L - 1200, "$12.00 paid");

            helper.setBlock(PUMP.above(), net.minecraft.world.level.block.Blocks.AIR);
            helper.setBlock(PUMP, net.minecraft.world.level.block.Blocks.AIR);
            GasPumpBlockEntity gas = pump(helper, owner, bc("fuel_light"), 5000);
            check(helper, !PumpService.fill(buyer, gas, semi, 1000, FOUR_DOLLARS, Payment.cash()).success(), "a gas pump won't fill a semi");
            helper.assertValueEqual(gas.fuel().getAmount(), 5000, "and pumps nothing");
        } finally {
            leave(helper, owner, buyer);
        }
        helper.succeed();
    }

    @GameTest(template = YARD, timeoutTicks = 60)
    public static void semiTowsATrailer(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        CarEntity semi = vehicle(helper, CarType.SEMI_TRUCK, 5.0, 12.0);
        semi.setFuel(CarType.SEMI_TRUCK.tankCapacity());
        TrailerEntity trailer = readyToHitch(helper, TrailerType.BOX_TRAILER, semi);
        TrailerEntity spare = trailer(helper, TrailerType.TANKER_TRAILER, new Vec3(1.5, 1.0, 30.0), 0.0F);
        driver.startRiding(semi, true);
        try {
            check(helper, Hitching.available(semi, driver) == Hitching.Action.HITCH, "the trailer can be hitched");
            Hitching.use(driver);
            check(helper, trailer.getVehicle() == semi && semi.trailer() == trailer, "hitched");
            check(helper, semi.getControllingPassenger() == driver, "the driver still drives");
            check(helper, Hitching.available(semi, driver) == Hitching.Action.UNHITCH, "next press unhitches");
            check(helper, !spare.isPassenger(), "only one trailer at a time");
        } catch (RuntimeException e) {
            leave(helper, driver);
            throw e;
        }
        Vec3 start = semi.position();
        // 10 ticks straight ahead, a block a tick, then 10 ticks turning 4.5 degrees (about the back axles, as a
        // semi does) and half a block a tick, as the driver's client would report it. Each tick, the trailer's kingpin
        // must still be on the fifth wheel.
        Vec3[] at = new Vec3[21];
        float[] yaw = new float[21];
        at[0] = start;
        double back = CarType.SEMI_TRUCK.pivotBack();
        for (int tick = 1; tick <= 20; tick++) {
            yaw[tick] = tick <= 10 ? 0.0F : (tick - 10) * 4.5F;
            Vec3 before = facing(yaw[tick - 1]);
            Vec3 after = facing(yaw[tick]);
            double step = tick <= 10 ? 1.0 : 0.5;
            at[tick] = at[tick - 1].add(before.scale(-back)).subtract(after.scale(-back)).add(after.scale(step));
        }
        for (int tick = 1; tick <= 21; tick++) {
            int now = tick;
            helper.runAtTickTime(tick, () -> {
                try {
                    if (now > 1) {
                        double gap = flatDistance(trailer.kingpin(), semi.fifthWheel());
                        helper.assertTrue(gap < 0.01, "kingpin " + gap + " blocks off the fifth wheel after tick " + (now - 1));
                    }
                    if (now == 11) {
                        near(helper, trailer.getYRot(), 0.0, "straight behind on the straight");
                        near(helper, flatDistance(trailer.position(), semi.position()), 32 / 16.0 + TrailerType.KINGPIN_FORWARD,
                                "trailer middle behind the semi's");
                    }
                    if (now <= 20) {
                        semi.setYRot(yaw[now]);
                        semi.setPos(at[now].x, at[now].y, at[now].z);
                        return;
                    }
                    float lag = trailer.getYRot();
                    helper.assertTrue(lag > 8.0F && lag < 40.0F, "the trailer swings round behind the turn, at " + lag + " degrees");
                    double burned = CarType.SEMI_TRUCK.tankCapacity() - semi.fuel();
                    double distance = 10.0 + 10 * 0.5;
                    helper.assertTrue(burned > distance * CarType.SEMI_TRUCK.fuelPerBlock * 1.3,
                            "pulling a trailer burns more: " + burned + " MJ for " + distance + " blocks");

                    Hitching.use(driver);
                    check(helper, !trailer.isPassenger() && semi.trailer() == null, "unhitched");
                    check(helper, Hitching.available(semi, driver) == Hitching.Action.HITCH, "and it can be hitched again where it stands");
                    leave(helper, driver);
                    helper.succeed();
                } catch (RuntimeException e) {
                    leave(helper, driver);
                    throw e;
                }
            });
        }
    }

    @GameTest(template = YARD)
    public static void tankerFillsAndEmpties(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        try {
            GasPumpBlockEntity pump = pump(helper, ModBlocks.DIESEL_PUMP.get(), owner, bc("fuel_dense"), 0);
            TrailerEntity tanker = trailer(helper, TrailerType.TANKER_TRAILER, new Vec3(3.5, 1.0, 6.0), 0.0F);
            BlockPos cauldron = new BlockPos(6, 1, 5);
            helper.setBlock(cauldron, net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
            helper.setBlock(new BlockPos(9, 1, 30), net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));

            TankerService.load(owner, tanker);
            helper.assertValueEqual(tanker.tank().getFluidAmount(), 1000, "a bucket of water from the cauldron beside it, not the far one");
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.CAULDRON, cauldron);
            tanker.tank().setFluid(FluidStack.EMPTY);

            tanker.tank().fill(new FluidStack(bc("fuel_light"), 5000), IFluidHandler.FluidAction.EXECUTE);
            TankerService.unload(owner, tanker);
            helper.assertValueEqual(tanker.tank().getFluidAmount(), 5000, "a diesel pump takes no gasoline");
            helper.assertValueEqual(pump.fuel().getAmount(), 0, "so it stays empty");

            tanker.tank().setFluid(new FluidStack(bc("fuel_dense"), 20_000));
            TankerService.unload(owner, tanker);
            check(helper, pump.fuel().getAmount() > 0, "diesel goes into the diesel pump");
            helper.assertValueEqual(pump.fuel().getAmount() + tanker.tank().getFluidAmount(), 20_000, "none spilled");

            int left = tanker.tank().getFluidAmount();
            TankerService.load(owner, tanker);
            helper.assertValueEqual(tanker.tank().getFluidAmount(), left, "pumps never give fuel back to a tanker");
            check(helper, tanker.toItem().has(com.reiko.cars.registry.ModDataComponents.TANK_FLUID.get()), "a picked-up tanker keeps its load");
        } finally {
            leave(helper, owner);
        }
        helper.succeed();
    }

    @GameTest(template = YARD)
    public static void carHaulerCarriesTwoCars(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        try {
            TrailerEntity hauler = trailer(helper, TrailerType.CAR_HAULER, new Vec3(5.0, 1.0, 20.0), 0.0F);
            Vec3 behind = helper.relativeVec(hauler.rear()).add(0.0, 0.0, -(CarType.SEDAN.length / 2.0 + 1.0));
            CarEntity[] cars = new CarEntity[3];
            for (int i = 0; i < cars.length; i++) {
                cars[i] = vehicle(helper, CarType.SEDAN, behind.x, behind.z);
                driver.startRiding(cars[i], true);
                if (i < 2) {
                    check(helper, Hitching.available(cars[i], driver) == Hitching.Action.LOAD, "car " + (i + 1) + " can drive on");
                    Hitching.use(driver);
                    check(helper, cars[i].getVehicle() == hauler, "car " + (i + 1) + " is on the hauler");
                    driver.stopRiding();
                    check(helper, cars[i].getVehicle() == hauler, "and stays on when its driver gets out");
                } else {
                    check(helper, Hitching.available(cars[i], driver) == null, "no room for a third");
                    Hitching.use(driver);
                    check(helper, !cars[i].isPassenger(), "so it stays on the ground");
                    driver.stopRiding();
                    cars[i].discard();
                }
            }
            helper.assertValueEqual(hauler.cars().size(), 2, "two cars aboard");
            CarEntity extra = vehicle(helper, CarType.SPORTS_CAR, 1.5, 4.0);
            check(helper, !extra.startRiding(hauler), "and no more");
            extra.discard();

            driver.startRiding(cars[1], true);
            check(helper, Hitching.available(cars[1], driver) == Hitching.Action.UNLOAD, "the back car can drive off");
            Hitching.use(driver);
            check(helper, !cars[1].isPassenger(), "it's off");
            check(helper, flatDistance(cars[1].position(), hauler.rear()) < CarType.SEDAN.length / 2.0 + 1.5, "just behind the hauler");
            helper.assertValueEqual(hauler.cars().size(), 1, "one car left aboard");
        } finally {
            leave(helper, driver);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void workbenchBuildsATrailer(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        try {
            CarWorkbenchMenu menu = workbench(helper, player);
            // A stray engine in the engine slot: trailers have none, so it's handed back.
            check(helper, menu.getSlot(CarAssemblyRecipe.ENGINE).mayPlace(new ItemStack(ModItems.CAR_ENGINE.get())), "engines still fit for cars");
            menu.getSlot(CarAssemblyRecipe.ENGINE).set(new ItemStack(ModItems.CAR_ENGINE.get()));
            player.getInventory().add(new ItemStack(ModItems.TRAILER_CHASSIS.get()));
            player.getInventory().add(new ItemStack(ModItems.trailerBody(TrailerType.TANKER_TRAILER)));
            player.getInventory().add(new ItemStack(ModItems.TIRE.get(), 4));
            check(helper, menu.partsAvailable(player.getInventory(), menu.recipes().get(button(menu, TrailerType.TANKER_TRAILER)).value())[CarAssemblyRecipe.INTERIOR],
                    "a trailer needs no interior");
            check(helper, menu.clickMenuButton(player, button(menu, TrailerType.TANKER_TRAILER)), "tanker picked");
            check(helper, !menu.getSlot(CarAssemblyRecipe.ENGINE).hasItem(), "the engine came out");
            helper.assertValueEqual(count(player, ModItems.CAR_ENGINE.get()), 1, "and went back to the player");
            check(helper, !menu.getSlot(CarAssemblyRecipe.INTERIOR).hasItem(), "no interior");
            check(helper, menu.getSlot(CarAssemblyRecipe.CHASSIS).getItem().is(ModItems.TRAILER_CHASSIS.get()), "the trailer chassis went in");
            check(helper, menu.getSlot(CarAssemblyRecipe.BODY).getItem().is(ModItems.trailerBody(TrailerType.TANKER_TRAILER)), "and the tank");
            check(helper, menu.getSlot(CarWorkbenchMenu.RESULT).getItem().is(ModItems.trailer(TrailerType.TANKER_TRAILER)), "they make a tanker");

            // With the engine put back, the parts make nothing.
            menu.getSlot(CarAssemblyRecipe.ENGINE).set(new ItemStack(ModItems.CAR_ENGINE.get()));
            check(helper, !menu.getSlot(CarWorkbenchMenu.RESULT).hasItem(), "a tanker with an engine is nothing");
            menu.getSlot(CarAssemblyRecipe.ENGINE).set(ItemStack.EMPTY);

            menu.clicked(CarWorkbenchMenu.RESULT, 0, ClickType.QUICK_MOVE, player);
            helper.assertValueEqual(count(player, ModItems.trailer(TrailerType.TANKER_TRAILER)), 1, "the tanker is in the inventory");
            for (int slot = 0; slot < CarWorkbenchMenu.PARTS; slot++) {
                check(helper, !menu.getSlot(slot).hasItem(), "part " + slot + " used up");
            }
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void workbenchBuildsAMotorcycle(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        try {
            CarWorkbenchMenu menu = workbench(helper, player);
            for (CarPart part : CarPart.values()) {
                player.getInventory().add(new ItemStack(ModItems.part(CarType.SCOOTER, part)));
            }
            player.getInventory().add(new ItemStack(ModItems.MOTORCYCLE_ENGINE.get()));
            player.getInventory().add(new ItemStack(ModItems.TIRE.get(), 4));
            CarAssemblyRecipe recipe = menu.recipes().get(button(menu, CarType.SCOOTER)).value();
            helper.assertValueEqual(recipe.wheels(), 2, "a scooter takes two wheels");
            check(helper, menu.clickMenuButton(player, button(menu, CarType.SCOOTER)), "scooter picked");
            check(helper, menu.getSlot(CarAssemblyRecipe.FIRST_WHEEL).hasItem() && menu.getSlot(CarAssemblyRecipe.FIRST_WHEEL + 2).hasItem(),
                    "front and back wheels in");
            check(helper, !menu.getSlot(CarAssemblyRecipe.FIRST_WHEEL + 1).hasItem() && !menu.getSlot(CarAssemblyRecipe.FIRST_WHEEL + 3).hasItem(),
                    "no right-hand wheels on a bike");
            helper.assertValueEqual(count(player, ModItems.TIRE.get()), 2, "two tires left over");
            check(helper, menu.getSlot(CarWorkbenchMenu.RESULT).getItem().is(ModItems.car(CarType.SCOOTER)), "they make a scooter");
            // A third tire in a right-hand slot spoils it.
            menu.getSlot(CarAssemblyRecipe.FIRST_WHEEL + 1).set(new ItemStack(ModItems.TIRE.get()));
            check(helper, !menu.getSlot(CarWorkbenchMenu.RESULT).hasItem(), "a scooter with three wheels is nothing");
            menu.getSlot(CarAssemblyRecipe.FIRST_WHEEL + 1).set(ItemStack.EMPTY);
            menu.clicked(CarWorkbenchMenu.RESULT, 0, ClickType.QUICK_MOVE, player);
            helper.assertValueEqual(count(player, ModItems.car(CarType.SCOOTER)), 1, "the scooter is in the inventory");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void motorcyclesSeatTheirRiders(GameTestHelper helper) {
        ServerPlayer rider = join(helper);
        ServerPlayer passenger = join(helper);
        ServerPlayer third = join(helper);
        ServerPlayer fourth = join(helper);
        try {
            CarEntity bike = car(helper, CarType.SPORT_BIKE);
            check(helper, CarType.SPORT_BIKE.isBike() && !CarType.SEDAN.isBike(), "the sport bike is a motorcycle");
            check(helper, bike.carType().fuel() == FuelType.GASOLINE, "on gasoline");
            check(helper, rider.startRiding(bike), "the rider gets on");
            check(helper, passenger.startRiding(bike), "a passenger gets on behind");
            check(helper, !third.startRiding(bike), "but nobody else");
            check(helper, bike.getControllingPassenger() == rider, "the first one on rides");
            check(helper, Math.abs(rider.getX() - bike.getX()) < 0.01 || Math.abs(rider.getZ() - bike.getZ()) < 0.01,
                    "sitting on the centre line");
            helper.assertValueEqual(CarType.CRUISER.storageRows * 9, 9, "the cruiser's saddlebags hold 9 stacks");
            CarEntity cruiser = car(helper, CarType.CRUISER);
            helper.assertValueEqual(cruiser.bed().getContainerSize(), 9, "and that's the container's size");
            CarEntity scooter = car(helper, CarType.SCOOTER);
            check(helper, third.startRiding(scooter), "someone rides the scooter");
            check(helper, !fourth.startRiding(scooter), "a scooter has one seat");
        } finally {
            leave(helper, rider, passenger, third, fourth);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void newCarsHaveTheirSeatsAndFuel(GameTestHelper helper) {
        check(helper, CarType.HATCHBACK.fuel() == FuelType.GASOLINE && CarType.MUSCLE_CAR.fuel() == FuelType.GASOLINE
                && CarType.CONVERTIBLE.fuel() == FuelType.GASOLINE && CarType.MINIVAN.fuel() == FuelType.GASOLINE, "four new gasoline cars");
        check(helper, CarType.DELIVERY_VAN.fuel() == FuelType.DIESEL && CarType.CITY_BUS.fuel() == FuelType.DIESEL
                && CarType.HEAVY_PICKUP.fuel() == FuelType.DIESEL && CarType.FARM_TRACTOR.fuel() == FuelType.DIESEL, "four new diesel ones");
        helper.assertValueEqual(CarType.CITY_BUS.seatCount(), 10, "bus seats");
        helper.assertValueEqual(CarType.MINIVAN.seatCount(), 6, "minivan seats");
        helper.assertValueEqual(CarType.FARM_TRACTOR.seatCount(), 1, "tractor seat");
        CarEntity van = car(helper, CarType.DELIVERY_VAN);
        helper.assertValueEqual(van.bed().getContainerSize(), 54, "the van's cargo area");
        CarEntity minivan = car(helper, CarType.MINIVAN);
        helper.assertValueEqual(minivan.bed().getContainerSize(), 9, "the minivan's trunk");
        check(helper, CarType.HEAVY_PICKUP.offRoad && CarType.FARM_TRACTOR.offRoad, "the 4x4 and the tractor go off-road");
        check(helper, CarType.HATCHBACK.fitsOnHauler() && CarType.MINIVAN.fitsOnHauler(), "small cars fit on a car hauler");
        check(helper, !CarType.CITY_BUS.fitsOnHauler() && !CarType.DELIVERY_VAN.fitsOnHauler() && !CarType.SEMI_TRUCK.fitsOnHauler(),
                "buses, vans and semis don't");
        CarTank tank = new CarTank(car(helper, CarType.CITY_BUS));
        helper.assertValueEqual(tank.fill(new FluidStack(bc("fuel_light"), 1000), IFluidHandler.FluidAction.SIMULATE), 0, "no gasoline in a bus");
        helper.assertValueEqual(tank.fill(new FluidStack(bc("fuel_dense"), 1000), IFluidHandler.FluidAction.SIMULATE), 1000, "diesel goes in");
        helper.succeed();
    }

    // ---- wear and repairs ----

    private static final BlockPos BENCH = new BlockPos(1, 1, 1);

    /** A repair bench owned by {@code owner}, every part at {@code price} cents. */
    private static RepairBenchBlockEntity bench(GameTestHelper helper, ServerPlayer owner, long price) {
        helper.setBlock(BENCH, ModBlocks.REPAIR_BENCH.get().defaultBlockState().setValue(RepairBenchBlock.FACING, Direction.NORTH));
        RepairBenchBlockEntity bench = (RepairBenchBlockEntity) helper.getBlockEntity(BENCH);
        bench.setOwner(owner);
        for (Part part : Part.values()) {
            bench.setPrice(part, price);
        }
        return bench;
    }

    private static void close(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-5, what + ": expected " + expected + " but was " + actual);
    }

    @GameTest(template = YARD, timeoutTicks = 60)
    public static void drivingWearsTheCar(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        CarEntity car = car(helper, CarType.SEDAN);
        car.setOwner(driver);
        car.setFuel(100_000);
        driver.startRiding(car, true);
        Vec3 start = car.position();
        for (int tick = 1; tick <= 10; tick++) {
            int step = tick;
            helper.runAtTickTime(tick + 2, () -> car.setPos(start.x, start.y, start.z + step));
        }
        helper.runAtTickTime(16, () -> {
            try {
                CarCondition wear = car.condition();
                boolean offRoad = car.level().getBlockState(car.getOnPos()).is(CarEntity.OFF_ROAD)
                        || car.level().getBlockState(car.blockPosition()).is(CarEntity.OFF_ROAD);
                close(helper, wear.oil(), 1.0 - 10.0 / 50_000, "oil after 10 blocks");
                close(helper, wear.engine(), 1.0 - 10.0 / 400_000, "engine after 10 blocks");
                close(helper, wear.tires(), 1.0 - 10.0 / 80_000 * (offRoad ? 2.5 : 1.0), "tires after 10 blocks on the test floor");
                close(helper, wear.brakes(), 1.0, "no braking, no brake wear");
                close(helper, wear.body(), 1.0, "no crash, no dents");
                driver.stopRiding();
                CarCondition parked = car.condition();
                car.setPos(start.x, start.y, start.z + 20);
                helper.runAfterDelay(2, () -> {
                    check(helper, car.condition().equals(parked), "a car with nobody in it doesn't wear");
                    leave(helper, driver);
                    helper.succeed();
                });
            } catch (RuntimeException e) {
                leave(helper, driver);
                throw e;
            }
        });
    }

    @GameTest(template = EMPTY)
    public static void lowOilWearsTheEngineFaster(GameTestHelper helper) {
        double full = CarCondition.NEW.afterDriving(1000, 1, 1, 1).engine();
        double low = CarCondition.NEW.with(Part.OIL, 0.2F).afterDriving(1000, 1, 1, 1).engine();
        double dry = CarCondition.NEW.with(Part.OIL, 0.0F).afterDriving(1000, 1, 1, 1).engine();
        close(helper, 1 - full, 1000.0 / 400_000, "engine wear with good oil");
        close(helper, 1 - low, 4 * 1000.0 / 400_000, "four times as fast on low oil");
        close(helper, 1 - dry, 20 * 1000.0 / 400_000, "twenty times as fast with none");
        CarCondition offRoad = CarCondition.NEW.afterDriving(1000, 2.5, 1.4, 1);
        close(helper, 1 - offRoad.tires(), 1000.0 / 80_000 * 2.5 * 1.4, "tires wear faster off-road, towing");
        check(helper, CarCondition.NEW.afterDriving(1000, 1, 1, 0).isNew(), "wear off: nothing wears");
        CarCondition braked = CarCondition.NEW.afterBraking(10, 2, 1, 1);
        close(helper, braked.brakes(), 1 - 10 * 0.002, "brakes wear with the speed they take off");
        close(helper, braked.tires(), 1 - (10 + 3 * 2) * 0.0003, "skids are hard on tires");
        // The effects: worn parts make the car slower, thirstier and worse at stopping.
        CarCondition tired = new CarCondition(1, 0.25F, 0.15F, 0.2F, 0.25F);
        close(helper, tired.power(), 0.8, "a tired engine has less power");
        close(helper, tired.fuelUse(), 1.375, "and burns more fuel");
        close(helper, tired.grip(), 0.775, "bald tires grip less");
        close(helper, tired.braking(), 0.675, "worn brakes stop slower");
        close(helper, tired.drag(), 0.875, "a battered body is slower");
        check(helper, CarCondition.NEW.power() == 1 && CarCondition.NEW.braking() == 1 && CarCondition.NEW.grip() == 1,
                "a new car drives as it always did");
        helper.succeed();
    }

    @GameTest(template = YARD, timeoutTicks = 60)
    public static void brokenDownCarsDontRun(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        CarEntity car = car(helper, CarType.SEDAN);
        car.setOwner(driver);
        car.setFuel(100_000);
        car.setCondition(CarCondition.NEW.with(Part.ENGINE, 0.0F));
        driver.startRiding(car, true);
        check(helper, car.isBrokenDown(), "an engine at 0% breaks the car down");
        check(helper, !car.isEngineRunning(), "its engine won't run");
        Vec3 start = car.position();
        helper.runAtTickTime(3, () -> car.setPos(start.x, start.y, start.z + 2));
        helper.runAtTickTime(6, () -> {
            try {
                near(helper, car.fuel(), 100_000, "a broken-down car burns no fuel");
                car.setCondition(CarCondition.NEW);
                check(helper, !car.isBrokenDown() && car.isEngineRunning(), "repaired, it runs again");
            } finally {
                leave(helper, driver);
            }
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void crashesDentTheBody(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        ServerPlayer passenger = join(helper);
        try {
            CarEntity car = car(helper, CarType.SEDAN);
            car.crash(0.1);
            check(helper, car.condition().isNew(), "a bump leaves no mark");
            car.crash(0.9);
            close(helper, car.condition().body(), 1 - (0.9 - 0.15) * 0.6, "a hard crash dents the body");
            close(helper, car.condition().engine(), 1 - (0.9 - 0.6) * 0.25, "and knocks the engine about");

            car.setCondition(CarCondition.NEW);
            car.setOwner(driver);
            driver.startRiding(car, true);
            passenger.startRiding(car, true);
            car.applyWearReport(passenger, 0, 0, 0.9F);
            check(helper, car.condition().isNew(), "only the driver's reports count");
            car.applyWearReport(driver, 0, 0, 50.0F);
            double most = CarType.SEDAN.topSpeed * 1.6;
            close(helper, car.condition().body(), 1 - (most - 0.15) * 0.6, "a crash can't be harder than the car can go");
        } finally {
            leave(helper, driver, passenger);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void carsKeepTheirWear(GameTestHelper helper) {
        CarEntity car = car(helper, CarType.HATCHBACK);
        CarCondition worn = new CarCondition(0.4F, 0.8F, 0.3F, 0.6F, 0.7F);
        car.setCondition(worn);
        ItemStack item = car.toItem();
        check(helper, CarItem.conditionOf(item).equals(worn), "picked up, the item keeps the wear");
        check(helper, !new ItemStack(ModItems.car(CarType.HATCHBACK)).has(com.reiko.cars.registry.ModDataComponents.CONDITION.get()),
                "a new car item carries none");
        CompoundTag saved = new CompoundTag();
        car.saveWithoutId(saved);
        CarEntity loaded = ModEntities.car(CarType.HATCHBACK).create(helper.getLevel());
        loaded.load(saved);
        check(helper, loaded.condition().equals(worn), "saved and loaded, the car keeps the wear: " + loaded.condition());
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void fitYourOwnParts(GameTestHelper helper) {
        ServerPlayer mechanic = join(helper);
        ServerPlayer driver = join(helper);
        try {
            RepairBenchBlockEntity bench = bench(helper, mechanic, 1500);
            CarEntity car = car(helper, CarType.SEDAN);
            car.setOwner(driver);
            car.setCondition(new CarCondition(0.1F, 0.4F, 0.2F, 1.0F, 1.0F));
            check(helper, RepairService.findCar(bench, driver) == car, "the bench finds the car beside it");
            helper.assertValueEqual(RepairJobs.needed(Part.TIRES, CarType.SEDAN, car.condition()), 4, "a sedan takes 4 tires");
            helper.assertValueEqual(RepairJobs.needed(Part.ENGINE, CarType.SEDAN, car.condition()), 3, "and 3 engine parts");

            driver.getInventory().add(new ItemStack(ModItems.TIRE.get(), 2));
            int tiresOnly = 1 << Part.TIRES.ordinal();
            ShopActionResult half = RepairService.fitOwn(driver, bench, car, tiresOnly);
            check(helper, !half.success(), "two tires aren't a set: " + half.message().getString());
            helper.assertValueEqual(count(driver, ModItems.TIRE.get()), 2, "and none are used");

            driver.getInventory().add(new ItemStack(ModItems.TIRE.get(), 2));
            driver.getInventory().add(new ItemStack(ModItems.MOTOR_OIL.get(), 1));
            driver.getInventory().add(new ItemStack(ModItems.ENGINE_PARTS.get(), 2));
            int mask = tiresOnly | 1 << Part.OIL.ordinal() | 1 << Part.ENGINE.ordinal();
            ShopActionResult done = RepairService.fitOwn(driver, bench, car, mask);
            check(helper, done.success(), "fitting your own parts: " + done.message().getString());
            CarCondition after = car.condition();
            close(helper, after.tires(), 1.0, "new tires");
            close(helper, after.oil(), 1.0, "fresh oil");
            close(helper, after.engine(), 0.9, "two engine parts, half the engine back");
            helper.assertValueEqual(count(driver, ModItems.TIRE.get()) + count(driver, ModItems.MOTOR_OIL.get())
                    + count(driver, ModItems.ENGINE_PARTS.get()), 0, "every part used");
            check(helper, !RepairService.fitOwn(driver, bench, car, tiresOnly).success(), "the tires don't need doing again");
        } finally {
            leave(helper, mechanic, driver);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theMechanicGetsPaid(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer mechanic = join(helper);
        ServerPlayer driver = join(helper);
        try {
            RepairBenchBlockEntity bench = bench(helper, mechanic, 1500); // $15.00 a part
            check(helper, bench.pipeInput().insertItem(0, new ItemStack(ModItems.TIRE.get(), 4), false).isEmpty(), "a hopper stocks tires");
            check(helper, !bench.pipeInput().insertItem(1, new ItemStack(Items.DIRT), false).isEmpty(), "but not dirt");
            check(helper, bench.pipeInput().extractItem(0, 4, false).isEmpty(), "and nothing comes out by pipe");
            bench.stock().insertItem(1, new ItemStack(ModItems.BRAKE_PADS.get(), 1), false);

            CarEntity car = car(helper, CarType.SEDAN);
            car.setOwner(driver);
            car.setCondition(new CarCondition(1.0F, 1.0F, 0.2F, 0.3F, 1.0F));
            long mechanicBefore = ECO.getBalance(server, mechanic.getUUID());
            Purse.give(driver, 10_000); // $100.00 in cash
            int mask = 1 << Part.TIRES.ordinal() | 1 << Part.BRAKES.ordinal();

            ShopActionResult stale = RepairService.buy(driver, bench, car, mask, 6000, Payment.cash());
            check(helper, !stale.success(), "a bill that's out of date is caught");
            ShopActionResult paid = RepairService.buy(driver, bench, car, mask, 7500, Payment.cash());
            check(helper, paid.success(), "paying for repairs: " + paid.message().getString());
            close(helper, car.condition().tires(), 1.0, "the bench fitted tires");
            close(helper, car.condition().brakes(), 1.0, "and brake pads");
            helper.assertValueEqual(Purse.value(driver), 2500L, "$75.00 cash spent");
            helper.assertValueEqual(ECO.getBalance(server, mechanic.getUUID()), mechanicBefore + 7500, "the mechanic paid $75.00");
            helper.assertValueEqual(bench.count(Part.TIRES) + bench.count(Part.BRAKES), 0, "stock used");
        } finally {
            leave(helper, mechanic, driver);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void ownersRepairFreeFromStock(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer mechanic = join(helper);
        try {
            RepairBenchBlockEntity bench = bench(helper, mechanic, 1500);
            bench.stock().insertItem(0, new ItemStack(ModItems.BODY_PANEL.get(), 2), false);
            CarEntity car = car(helper, CarType.SEDAN);
            car.setOwner(mechanic);
            car.setCondition(CarCondition.NEW.with(Part.BODY, 0.0F));
            int body = 1 << Part.BODY.ordinal();
            check(helper, !RepairService.buy(mechanic, bench, car, 1 << Part.OIL.ordinal(), 0, Payment.cash()).success(),
                    "nothing to do on the oil");
            long before = ECO.getBalance(server, mechanic.getUUID());
            ShopActionResult done = RepairService.buy(mechanic, bench, car, body, 3000, Payment.cash());
            check(helper, done.success(), "the owner's own repair: " + done.message().getString());
            close(helper, car.condition().body(), 0.4, "two body panels mend 40%");
            check(helper, !car.isBrokenDown(), "and it runs again");
            helper.assertValueEqual(ECO.getBalance(server, mechanic.getUUID()), before, "free for the owner");
            helper.assertValueEqual(bench.count(Part.BODY), 0, "panels used from stock");
            check(helper, !RepairService.buy(mechanic, bench, car, body, 0, Payment.cash()).success(), "out of panels");
        } finally {
            leave(helper, mechanic);
        }
        helper.succeed();
    }
}
