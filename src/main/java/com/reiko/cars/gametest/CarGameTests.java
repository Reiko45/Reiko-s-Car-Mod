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
import com.reiko.cars.fare.FareMenu;
import com.reiko.cars.fare.FareService;
import com.reiko.cars.key.CarKeys;
import com.reiko.cars.sale.DealershipBlockEntity;
import com.reiko.cars.sale.DealershipMenu;
import com.reiko.cars.sale.SaleService;
import com.reiko.cars.key.Keyed;
import com.reiko.cars.mail.MailService;
import com.reiko.cars.insurance.InsuranceMenu;
import com.reiko.cars.insurance.InsuranceOfficeBlockEntity;
import com.reiko.cars.insurance.InsuranceService;
import com.reiko.cars.job.CarJobs;
import com.reiko.cars.parking.MeterService;
import com.reiko.cars.parking.ParkingMeterBlock;
import com.reiko.cars.parking.ParkingMeterBlockEntity;
import com.reiko.cars.toll.GatePart;
import com.reiko.cars.toll.TollBoothBlockEntity;
import com.reiko.cars.toll.TollGateBlock;
import com.reiko.cars.toll.TollGateBlockEntity;
import com.reiko.cars.toll.TollService;
import com.reiko.cars.mail.MailboxBlock;
import com.reiko.cars.mail.MailboxBlockEntity;
import com.reiko.cars.mail.MailboxMenu;
import com.reiko.cars.mail.Parcel;
import com.reiko.cars.mail.ParcelItem;
import com.reiko.cars.pump.GasPumpBlock;
import com.reiko.cars.pump.GasPumpBlockEntity;
import com.reiko.cars.pump.PumpService;
import com.reiko.cars.registry.ModBlocks;
import com.reiko.cars.registry.ModEntities;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.repair.RepairBenchBlock;
import com.reiko.cars.repair.RepairBenchBlockEntity;
import com.reiko.cars.repair.RepairBenchMenu;
import com.reiko.cars.repair.RepairJobs;
import com.reiko.cars.repair.RepairService;
import com.reiko.cars.tow.Towing;
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
import java.util.List;
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
import net.minecraft.world.InteractionHand;
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
 * tear: parts wearing out, trailers too, breaking down, and repairs at the Repair Bench, free or paid for, on any
 * vehicle beside it. Run them with
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

    /** Gives {@code player} a key for {@code vehicle}, as if its owner had cut one. */
    private static void giveKey(ServerPlayer player, Keyed vehicle) {
        if (vehicle.lockCode() == 0L) {
            vehicle.setLockCode(new java.util.Random().nextLong() | 1L);
        }
        player.getInventory().add(CarKeys.keyFor(vehicle));
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
            boolean gasoline = type.fuel() == FuelType.GASOLINE;
            check(helper, CarFuel.runs(type, new FluidStack(bc("fuel_light"), 1)) == gasoline, type.id() + " runs on light fuel only if it's gasoline");
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
        giveKey(driver, car);
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
        giveKey(driver, car);
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
            check(helper, RepairService.defaultTarget(bench, driver) == car, "the bench finds the car beside it");
            helper.assertValueEqual(RepairJobs.needed(Part.TIRES, car), 4, "a sedan takes 4 tires");
            helper.assertValueEqual(RepairJobs.needed(Part.ENGINE, car), 3, "and 3 engine parts");

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

    @GameTest(template = EMPTY)
    public static void lockedCarsCanBeRepaired(GameTestHelper helper) {
        ServerPlayer mechanic = join(helper);
        ServerPlayer owner = join(helper);
        try {
            RepairBenchBlockEntity bench = bench(helper, mechanic, 1500);
            CarEntity car = car(helper, CarType.SEDAN);
            car.setOwner(owner);
            car.setLocked(true);
            car.setCondition(CarCondition.NEW.with(Part.BRAKES, 0.1F));
            check(helper, !car.canBeUsedBy(mechanic), "the car is locked to the mechanic");
            check(helper, RepairService.defaultTarget(bench, mechanic) == car, "but the bench still finds it");
            mechanic.getInventory().add(new ItemStack(ModItems.BRAKE_PADS.get(), 1));
            ShopActionResult done = RepairService.fitOwn(mechanic, bench, car, 1 << Part.BRAKES.ordinal());
            check(helper, done.success(), "and the mechanic can fix it: " + done.message().getString());
            close(helper, car.condition().brakes(), 1.0, "new brakes");
            check(helper, car.isLocked(), "it stays locked");
        } finally {
            leave(helper, mechanic, owner);
        }
        helper.succeed();
    }

    @GameTest(template = YARD, timeoutTicks = 60)
    public static void trailersWearToo(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        CarEntity semi = vehicle(helper, CarType.SEMI_TRUCK, 5.0, 8.0);
        semi.setFuel(CarType.SEMI_TRUCK.tankCapacity());
        TrailerEntity trailer = readyToHitch(helper, TrailerType.BOX_TRAILER, semi);
        driver.startRiding(semi, true);
        Hitching.use(driver);
        check(helper, semi.trailer() == trailer, "hitched");
        Vec3 start = semi.position();
        for (int tick = 1; tick <= 10; tick++) {
            int step = tick;
            helper.runAtTickTime(tick + 2, () -> semi.setPos(start.x, start.y, start.z + step));
        }
        helper.runAtTickTime(16, () -> {
            try {
                CarCondition wear = trailer.condition();
                double tires = 1.0 - wear.tires();
                double perBlock = 1.0 / 80_000;
                helper.assertTrue(tires > 9 * perBlock && tires < 11 * perBlock * 2.5, "trailer tires after 10 blocks: " + tires);
                check(helper, wear.oil() == 1.0F && wear.engine() == 1.0F, "a trailer has no oil or engine to wear");
                check(helper, wear.brakes() == 1.0F && wear.body() == 1.0F, "no braking or crashes yet");

                semi.applyWearReport(driver, 2.0F, 0.0F, 0.0F);
                close(helper, trailer.condition().brakes(), 1 - 2 * 0.002, "the trailer's brakes wear when the semi brakes");
                semi.crash(0.9);
                close(helper, trailer.condition().body(), 1 - (0.9 * TrailerEntity.CRASH_SHARE - 0.15) * 0.6, "a crash dents the trailer too");

                // Worn trailer parts hold the rig up; one worn out stops it.
                check(helper, !semi.isBrokenDown(), "still going");
                trailer.setCondition(trailer.condition().with(Part.TIRES, 0.0F));
                check(helper, semi.isBrokenDown() && !semi.isEngineRunning(), "a trailer with bald-through tires stops the rig");
                Hitching.use(driver);
                check(helper, semi.trailer() == null && !semi.isBrokenDown(), "unhitched, the semi runs again");
                Hitching.use(driver);
                check(helper, semi.trailer() == null, "and the worn-out trailer won't hitch");

                CarCondition worn = trailer.condition();
                check(helper, CarItem.conditionOf(trailer.toItem()).equals(worn), "a picked-up trailer keeps its wear");
                CompoundTag saved = new CompoundTag();
                trailer.saveWithoutId(saved);
                TrailerEntity loaded = ModEntities.trailer(TrailerType.BOX_TRAILER).create(helper.getLevel());
                loaded.load(saved);
                check(helper, loaded.condition().equals(worn), "and so does a saved one");
                leave(helper, driver);
                helper.succeed();
            } catch (RuntimeException e) {
                leave(helper, driver);
                throw e;
            }
        });
    }

    @GameTest(template = YARD)
    public static void theBenchFixesTrailersAndPicksBetweenVehicles(GameTestHelper helper) {
        ServerPlayer mechanic = join(helper);
        try {
            helper.setBlock(new BlockPos(5, 1, 10), ModBlocks.REPAIR_BENCH.get().defaultBlockState());
            RepairBenchBlockEntity bench = (RepairBenchBlockEntity) helper.getBlockEntity(new BlockPos(5, 1, 10));
            bench.setOwner(mechanic);
            CarEntity car = vehicle(helper, CarType.SEDAN, 5.5, 6.0);
            TrailerEntity trailer = trailer(helper, TrailerType.BOX_TRAILER, new Vec3(5.0, 1.0, 15.5), 0.0F);
            helper.assertValueEqual(RepairService.vehiclesInReach(bench).size(), 2, "a car and a trailer beside the bench");

            RepairBenchMenu menu = RepairBenchMenu.create(1, mechanic, bench);
            Object first = menu.target();
            check(helper, first == car || first == trailer, "it starts on one of them");
            menu.clickMenuButton(mechanic, RepairBenchMenu.NEXT);
            check(helper, menu.target() != first && menu.target() != null, "the arrow moves to the other");
            menu.clickMenuButton(mechanic, RepairBenchMenu.NEXT);
            check(helper, menu.target() == first, "and round again");
            if (menu.target() != trailer) {
                menu.clickMenuButton(mechanic, RepairBenchMenu.PREVIOUS);
            }
            check(helper, menu.target() == trailer && !menu.hasPart(Part.ENGINE) && menu.hasPart(Part.TIRES),
                    "on the trailer: tires, but no engine");

            trailer.setCondition(new CarCondition(1.0F, 1.0F, 0.2F, 0.3F, 0.5F));
            helper.assertValueEqual(RepairJobs.needed(Part.TIRES, trailer), 4, "a trailer takes 4 tires");
            helper.assertValueEqual(RepairJobs.needed(Part.BRAKES, trailer), 2, "2 sets of brake pads");
            helper.assertValueEqual(RepairJobs.needed(Part.BODY, trailer), 5, "and a tenth of its body a panel");
            helper.assertValueEqual(RepairJobs.needed(Part.OIL, trailer), 0, "it has no oil to change");
            check(helper, !RepairService.fitOwn(mechanic, bench, trailer, 1 << Part.OIL.ordinal()).success(), "so oil won't go in");
            mechanic.getInventory().add(new ItemStack(ModItems.TIRE.get(), 4));
            mechanic.getInventory().add(new ItemStack(ModItems.BRAKE_PADS.get(), 2));
            mechanic.getInventory().add(new ItemStack(ModItems.BODY_PANEL.get(), 5));
            int mask = 1 << Part.TIRES.ordinal() | 1 << Part.BRAKES.ordinal() | 1 << Part.BODY.ordinal();
            ShopActionResult done = RepairService.fitOwn(mechanic, bench, trailer, mask);
            check(helper, done.success(), "fixing the trailer: " + done.message().getString());
            check(helper, trailer.condition().isNew(), "as good as new: " + trailer.condition());
        } finally {
            leave(helper, mechanic);
        }
        helper.succeed();
    }

    // ---- tow trucks ----

    /** A car standing behind {@code truck}, facing the same way, its front {@code gap} blocks short of the pickup point. */
    private static CarEntity behind(GameTestHelper helper, CarEntity truck, CarType type, double gap) {
        Vec3 point = helper.relativeVec(Towing.pickupPoint(truck));
        return vehicle(helper, type, point.x, point.z - type.length / 2.0 - gap);
    }

    @GameTest(template = YARD)
    public static void flatbedLoadsAndDropsACar(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        try {
            CarEntity truck = vehicle(helper, CarType.FLATBED, 5.0, 22.0);
            CarEntity semi = behind(helper, truck, CarType.SEMI_TRUCK, 0.3);
            driver.startRiding(truck, true);
            check(helper, Hitching.available(truck, driver) == null, "a semi is too big for the flatbed");
            semi.discard();

            CarEntity car = behind(helper, truck, CarType.SEDAN, 0.3);
            car.setCondition(new CarCondition(1.0F, 0.0F, 1.0F, 1.0F, 1.0F)); // broken down: its engine's gone
            check(helper, Hitching.available(truck, driver) == Hitching.Action.TOW, "the flatbed can take the car behind, broken down or not");
            Hitching.use(driver);
            check(helper, car.getVehicle() == truck && Towing.towed(truck) == car, "the car is on the flatbed");
            truck.positionRider(car);
            close(helper, car.getY(), truck.getY() + Towing.DECK_TOP, "up on the deck");
            check(helper, flatDistance(car.position(), truck.position()) < CarType.FLATBED.length / 2.0, "within the truck's length");
            near(helper, net.minecraft.util.Mth.wrapDegrees(car.getYRot() - truck.getYRot()), 0.0, "facing forward");
            check(helper, Hitching.available(truck, driver) == Hitching.Action.DROP, "then G drops it off");
            check(helper, Towing.towable(truck, driver) == null, "and it can't take a second");

            Hitching.use(driver);
            check(helper, !car.isPassenger(), "the car is off");
            close(helper, car.getY(), truck.getY(), "on the ground");
            check(helper, flatDistance(car.position(), truck.position()) > CarType.FLATBED.length / 2.0, "behind the truck");
        } finally {
            leave(helper, driver);
        }
        helper.succeed();
    }

    @GameTest(template = YARD)
    public static void wreckerLiftsACarByItsFront(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        ServerPlayer stranger = join(helper);
        try {
            CarEntity truck = vehicle(helper, CarType.WRECKER, 5.0, 22.0);
            CarEntity backwards = behind(helper, truck, CarType.SEDAN, 0.3);
            backwards.setYRot(180.0F); // facing the truck: its back's toward the forks, not its front
            driver.startRiding(truck, true);
            check(helper, Towing.towable(truck, driver) == null, "the wrecker needs the car's front at its forks");
            backwards.discard();

            CarEntity locked = behind(helper, truck, CarType.HATCHBACK, 0.3);
            locked.setOwner(stranger);
            locked.setLocked(true);
            check(helper, Towing.towable(truck, driver) == null, "nor a stranger's locked car");
            locked.discard();

            CarEntity car = behind(helper, truck, CarType.SEDAN, 0.3);
            Hitching.use(driver);
            check(helper, car.getVehicle() == truck, "the wrecker has the car");
            truck.positionRider(car);
            check(helper, car.towPitch(1.0F) != 0.0F, "its front lifted");
            check(helper, car.getY() > truck.getY() && car.getY() < truck.getY() + Towing.LIFT_RISE, "its back wheels on the road");
            check(helper, Math.abs(net.minecraft.util.Mth.wrapDegrees(car.getYRot() - truck.getYRot())) < 1.0F, "trailing straight behind");

            Hitching.use(driver);
            check(helper, !car.isPassenger() && car.towPitch(1.0F) == 0.0F, "let down");
            close(helper, car.getY(), truck.getY(), "where it was");
        } finally {
            leave(helper, driver, stranger);
        }
        helper.succeed();
    }

    // ---- the mail ----

    private static MailboxBlockEntity mailbox(GameTestHelper helper, BlockPos pos, ServerPlayer owner) {
        helper.setBlock(pos, ModBlocks.MAILBOX.get().defaultBlockState());
        MailboxBlockEntity box = (MailboxBlockEntity) helper.getBlockEntity(pos);
        box.setOwner(owner);
        return box;
    }

    private static ShopActionResult send(ServerPlayer sender, MailboxBlockEntity box, String to, long postage, ItemStack... items) {
        MailboxMenu menu = MailboxMenu.create(0, sender, box);
        for (int i = 0; i < items.length; i++) {
            menu.packing().setItem(i, items[i]);
        }
        ShopActionResult result = MailService.send(sender, menu, to, postage, Payment.cash());
        menu.removed(sender); // what wasn't sent goes back
        return result;
    }

    private static int parcels(net.minecraft.world.SimpleContainer bag) {
        int count = 0;
        for (int i = 0; i < bag.getContainerSize(); i++) {
            count += ParcelItem.parcel(bag.getItem(i)) != null ? bag.getItem(i).getCount() : 0;
        }
        return count;
    }

    @GameTest(template = YARD)
    public static void theMailVanDeliversParcels(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer alice = join(helper);
        ServerPlayer bob = join(helper);
        ServerPlayer carrier = join(helper);
        try {
            BlockPos aPos = new BlockPos(1, 1, 6);
            BlockPos bPos = new BlockPos(1, 1, 26);
            MailboxBlockEntity aliceBox = mailbox(helper, aPos, alice);
            MailboxBlockEntity bobBox = mailbox(helper, bPos, bob);
            check(helper, !aliceBox.canManage(bob), "only its owner empties a mailbox");

            // Into the addressee's own mailbox: free.
            ShopActionResult dropped = send(bob, aliceBox, alice.getGameProfile().getName(), 0, new ItemStack(Items.DIAMOND, 2));
            check(helper, dropped.success(), "dropping a parcel in: " + dropped.message().getString());
            helper.assertValueEqual(aliceBox.count(aliceBox.inbox()), 1, "a parcel in Alice's inbox");
            Parcel gift = ParcelItem.parcel(aliceBox.inbox().getStackInSlot(0));
            check(helper, gift != null && gift.to().equals(alice.getUUID()) && gift.postage() == 0, "for Alice, no postage");

            // To anyone else: postage, and it waits for the van.
            String bobName = bob.getGameProfile().getName();
            Purse.give(alice, 1000);
            check(helper, !send(alice, aliceBox, "nobody_here", 200, new ItemStack(Items.APPLE)).success(), "nobody by that name");
            check(helper, !send(alice, aliceBox, bobName, 100, new ItemStack(Items.APPLE)).success(), "out-of-date postage is caught");
            check(helper, alice.getInventory().countItem(Items.APPLE) == 2, "and the apples come back");
            alice.getInventory().clearContent();
            Purse.give(alice, 1000);
            ShopActionResult posted = send(alice, aliceBox, bobName, MailService.postage(), new ItemStack(Items.APPLE, 3));
            check(helper, posted.success(), "posting to Bob: " + posted.message().getString());
            helper.assertValueEqual(Purse.value(alice), 1000L - MailService.postage(), "postage paid in cash");
            helper.assertValueEqual(aliceBox.count(aliceBox.outbox()), 1, "the parcel waits for the van");
            check(helper, helper.getBlockState(aPos).getValue(MailboxBlock.FLAG), "with the flag up");

            // The van collects it at Alice's, and delivers it at Bob's.
            CarEntity van = vehicle(helper, CarType.MAIL_VAN, 4.5, 6.5);
            carrier.startRiding(van, true);
            check(helper, Hitching.available(van, carrier) == Hitching.Action.MAIL, "the van can stop for the mail");
            long carrierBefore = ECO.getBalance(server, carrier.getUUID());
            Hitching.use(carrier);
            helper.assertValueEqual(parcels(van.bed()), 1, "collected into the mail compartment");
            helper.assertValueEqual(aliceBox.count(aliceBox.outbox()), 0, "the outbox is empty");
            check(helper, !helper.getBlockState(aPos).getValue(MailboxBlock.FLAG), "and the flag down");
            helper.assertValueEqual(aliceBox.count(aliceBox.inbox()), 1, "Alice's own mail stays put");

            Vec3 atBob = helper.absoluteVec(new Vec3(4.5, 1.0, 26.5));
            van.moveTo(atBob.x, atBob.y, atBob.z, 0.0F, 0.0F);
            Hitching.use(carrier);
            helper.assertValueEqual(parcels(van.bed()), 0, "delivered");
            helper.assertValueEqual(bobBox.count(bobBox.inbox()), 1, "into Bob's mailbox");
            helper.assertValueEqual(ECO.getBalance(server, carrier.getUUID()), carrierBefore + MailService.postage(), "the carrier earns the postage");

            // Only Bob (or Alice, who sent it) can open it.
            ItemStack parcel = bobBox.inbox().extractItem(0, 1, false);
            carrier.setItemInHand(InteractionHand.MAIN_HAND, parcel.copy());
            ModItems.PARCEL.get().use(helper.getLevel(), carrier, InteractionHand.MAIN_HAND);
            check(helper, carrier.getMainHandItem().is(ModItems.PARCEL.get()), "the carrier can't open it");
            bob.setItemInHand(InteractionHand.MAIN_HAND, parcel);
            ModItems.PARCEL.get().use(helper.getLevel(), bob, InteractionHand.MAIN_HAND);
            helper.assertValueEqual(bob.getInventory().countItem(Items.APPLE), 3, "Bob opens it: three apples");
            check(helper, bob.getInventory().countItem(ModItems.PARCEL.get()) == 0, "and the box is gone");
        } finally {
            leave(helper, alice, bob, carrier);
        }
        helper.succeed();
    }

    // ---- keys ----

    /** Sneak and right-click {@code vehicle} with {@code held}, as {@code player}. */
    private static void sneakUse(ServerPlayer player, net.minecraft.world.entity.Entity vehicle, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.setShiftKeyDown(true);
        vehicle.interact(player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
    }

    private static int keysFitting(ServerPlayer player, long code) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            count += CarKeys.fits(stack, code) ? 1 : 0;
        }
        return count;
    }

    @GameTest(template = EMPTY)
    public static void keysStartTheCar(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        ServerPlayer stranger = join(helper);
        try {
            owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            CarEntity car = car(helper, CarType.SEDAN);
            car.setOwner(owner);
            car.setFuel(100_000);
            owner.startRiding(car, true);
            check(helper, !car.keyedIn() && !car.isEngineRunning(), "even its owner can't start it without a key");
            owner.stopRiding();

            sneakUse(stranger, car, new ItemStack(ModItems.BLANK_KEY.get(), 2));
            check(helper, car.lockCode() == 0L && stranger.getInventory().countItem(ModItems.CAR_KEY.get()) == 0, "only the owner cuts keys");

            sneakUse(owner, car, new ItemStack(ModItems.BLANK_KEY.get(), 2));
            check(helper, car.lockCode() != 0L, "cutting the first key gives the car its lock");
            helper.assertValueEqual(keysFitting(owner, car.lockCode()), 1, "the owner has a key");
            helper.assertValueEqual(owner.getMainHandItem().getCount(), 1, "one blank used");
            check(helper, owner.getInventory().items.stream().anyMatch(stack -> stack.is(ModItems.CAR_KEY.get())
                    && stack.getHoverName().getString().equals("Sedan Key")), "called the Sedan Key");

            owner.startRiding(car, true);
            check(helper, car.keyedIn() && car.isEngineRunning(), "with the key it starts");
            owner.stopRiding();
            ItemStack item = car.toItem();
            helper.assertValueEqual(item.get(com.reiko.cars.registry.ModDataComponents.LOCK.get()), car.lockCode(),
                    "picked up, it keeps its lock, so the keys still fit");
        } finally {
            leave(helper, owner, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = YARD)
    public static void keysShareLockedVehicles(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        ServerPlayer friend = join(helper);
        try {
            CarEntity car = vehicle(helper, CarType.SEDAN, 5.0, 6.0);
            car.setOwner(owner);
            car.setLocked(true);
            car.setFuel(100_000);
            check(helper, !car.canBeUsedBy(friend), "locked, a friend can't get in");
            check(helper, !car.toggleLock(friend), "or unlock it");

            sneakUse(owner, car, new ItemStack(ModItems.BLANK_KEY.get()));
            sneakUse(owner, car, new ItemStack(ModItems.BLANK_KEY.get()));
            helper.assertValueEqual(keysFitting(owner, car.lockCode()), 2, "two keys cut");
            ItemStack spare = owner.getInventory().items.stream().filter(stack -> CarKeys.fits(stack, car.lockCode())).findFirst().orElseThrow();
            friend.getInventory().add(spare.split(1));
            check(helper, car.canBeUsedBy(friend), "with a key the friend can get in");
            check(helper, car.toggleLock(friend) && !car.isLocked(), "and unlock it");
            check(helper, car.toggleLock(friend) && car.isLocked(), "and lock it again");
            friend.startRiding(car, true);
            check(helper, car.isEngineRunning(), "and drive it");
            friend.stopRiding();
            check(helper, !car.canManage(friend), "but not pick it up");

            // The owner's key locks and unlocks it from a distance.
            Vec3 across = car.position().add(12.0, 0.0, 0.0);
            owner.moveTo(across.x, across.y, across.z);
            ItemStack ownersKey = owner.getInventory().items.stream().filter(stack -> CarKeys.fits(stack, car.lockCode())).findFirst().orElseThrow();
            CarKeys.fob(owner, ownersKey);
            check(helper, !car.isLocked(), "the key fob unlocks it");
            CarKeys.fob(owner, ownersKey);
            check(helper, car.isLocked(), "and locks it");

            // Changing the locks: asked twice, then only the owner's key fits.
            long before = car.lockCode();
            int slot = owner.getInventory().findSlotMatchingItem(ownersKey);
            ItemStack inHand = owner.getInventory().removeItemNoUpdate(slot);
            sneakUse(owner, car, inHand);
            helper.assertValueEqual(car.lockCode(), before, "asked once, the locks stay");
            sneakUse(owner, car, owner.getMainHandItem());
            check(helper, car.lockCode() != before, "asked twice, the locks change");
            check(helper, CarKeys.fits(owner.getMainHandItem(), car.lockCode()), "the owner's key has the new lock");
            check(helper, !CarKeys.holds(friend, car.lockCode()) && !car.canBeUsedBy(friend), "the friend's key doesn't fit any more");

            // Trailers take keys too.
            TrailerEntity trailer = trailer(helper, TrailerType.BOX_TRAILER, new Vec3(5.0, 1.0, 20.0), 0.0F);
            trailer.setOwner(owner);
            trailer.setLocked(true);
            check(helper, !trailer.canBeUsedBy(friend), "a locked trailer keeps the friend out");
            sneakUse(owner, trailer, new ItemStack(ModItems.BLANK_KEY.get()));
            ItemStack trailerKey = owner.getInventory().items.stream().filter(stack -> CarKeys.fits(stack, trailer.lockCode())).findFirst().orElseThrow();
            friend.getInventory().add(trailerKey.split(1));
            check(helper, trailer.canBeUsedBy(friend), "until they have its key");
            helper.assertValueEqual(friend.getInventory().items.stream().filter(stack -> stack.is(ModItems.CAR_KEY.get()))
                    .map(stack -> stack.getHoverName().getString()).filter(name -> name.equals("Box Trailer Key")).count(), 1L, "the Box Trailer Key");
        } finally {
            leave(helper, owner, friend);
        }
        helper.succeed();
    }

    // ---- fares ----

    @GameTest(template = YARD)
    public static void busRidersPayTheDriver(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer owner = join(helper);
        ServerPlayer driver = join(helper);
        ServerPlayer rider = join(helper);
        ServerPlayer dodger = join(helper);
        try {
            CarEntity bus = vehicle(helper, CarType.CITY_BUS, 5.0, 10.0);
            bus.setOwner(owner);
            giveKey(driver, bus);
            driver.startRiding(bus, true);
            rider.startRiding(bus, true);
            long fare = bus.fares().start();
            check(helper, fare == 200, "a new bus charges $2.00, as the server's set");

            check(helper, !FareService.payBusFare(rider, bus, 100, Payment.cash()).success(), "an out-of-date fare is caught");
            Purse.give(rider, 1000);
            long driverBefore = ECO.getBalance(server, driver.getUUID());
            ShopActionResult paid = FareService.payBusFare(rider, bus, fare, Payment.cash());
            check(helper, paid.success(), "paying the fare: " + paid.message().getString());
            helper.assertValueEqual(Purse.value(rider), 800L, "$2.00 cash spent");
            helper.assertValueEqual(ECO.getBalance(server, driver.getUUID()), driverBefore + fare, "the driver is paid");
            check(helper, !FareService.payBusFare(rider, bus, fare, Payment.cash()).success(), "and nobody pays twice");
            check(helper, bus.fares().rides() == 1 && bus.fares().earned() == fare, "the bus counts the ride");

            dodger.startRiding(bus, true);
            FareMenu screen = FareMenu.create(0, dodger, bus, driver.getGameProfile().getName());
            screen.removed(dodger);
            check(helper, !dodger.isPassenger(), "no fare, no ride");

            check(helper, !FareService.setFares(rider, bus, "0.50", "").success(), "only the owner sets the fares");
            check(helper, FareService.setFares(owner, bus, "3.50", "").success() && bus.fares().start() == 350, "the owner raises the fare");
        } finally {
            leave(helper, owner, driver, rider, dodger);
        }
        helper.succeed();
    }

    /** Drives {@code taxi} {@code blocks} blocks south, five at a time, running its meter. */
    private static void drive(CarEntity taxi, int blocks) {
        taxi.fares().tick();
        for (int done = 0; done < blocks; done += 5) {
            taxi.setPos(taxi.getX(), taxi.getY(), taxi.getZ() + 5);
            taxi.fares().tick();
        }
    }

    @GameTest(template = YARD)
    public static void taxiMeterCharges(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer driver = join(helper);
        ServerPlayer rider = join(helper);
        ServerPlayer broke = join(helper);
        try {
            CarEntity taxi = vehicle(helper, CarType.TAXI, 5.0, 4.0);
            taxi.setOwner(driver);
            giveKey(driver, taxi);
            driver.startRiding(taxi, true);
            check(helper, taxi.meterCents() < 0, "no passenger, no meter");

            rider.startRiding(taxi, true);
            ECO.deposit(server, rider.getUUID(), 1000, "test");
            long riderBefore = ECO.getBalance(server, rider.getUUID());
            long driverBefore = ECO.getBalance(server, driver.getUUID());
            taxi.fares().tick();
            check(helper, rider.getUUID().equals(taxi.fares().hirer()), "the passenger hires the taxi");
            helper.assertValueEqual(taxi.meterCents(), 300L, "the meter starts at $3.00");
            drive(taxi, 100);
            helper.assertValueEqual(taxi.meterCents(), 400L, "and adds $1.00 for 100 blocks");
            rider.stopRiding();
            helper.assertValueEqual(ECO.getBalance(server, rider.getUUID()), riderBefore - 400, "the fare comes out of the passenger's bank account");
            helper.assertValueEqual(ECO.getBalance(server, driver.getUUID()), driverBefore + 400, "and goes to the driver");
            check(helper, taxi.meterCents() < 0, "the meter's off");

            // Getting out straight away costs nothing.
            rider.startRiding(taxi, true);
            drive(taxi, 0);
            rider.stopRiding();
            helper.assertValueEqual(ECO.getBalance(server, rider.getUUID()), riderBefore - 400, "a ride that never started is free");

            // Someone who can't pay it all pays what they have.
            ECO.setBalance(server, broke.getUUID(), 100, "test"); // $1.00 to their name
            broke.startRiding(taxi, true);
            long brokeBalance = ECO.getBalance(server, broke.getUUID());
            drive(taxi, 50);
            long owed = taxi.meterCents();
            broke.stopRiding();
            check(helper, owed > brokeBalance, "a fare they can't cover");
            long brokePaid = brokeBalance;
            helper.assertValueEqual(ECO.getBalance(server, broke.getUUID()), brokeBalance - brokePaid, "they pay what they can");
            helper.assertValueEqual(ECO.getBalance(server, driver.getUUID()), driverBefore + 400 + brokePaid, "and the driver gets it");
        } finally {
            leave(helper, driver, rider, broke);
        }
        helper.succeed();
    }

    // ---- buying and selling ----

    @GameTest(template = YARD)
    public static void carsChangeHands(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer seller = join(helper);
        ServerPlayer buyer = join(helper);
        try {
            CarEntity car = vehicle(helper, CarType.SEDAN, 5.0, 8.0);
            car.setOwner(seller);
            giveKey(seller, car);
            long oldLock = car.lockCode();
            buyer.moveTo(car.getX() + 2, car.getY(), car.getZ());
            check(helper, !SaleService.setPrice(buyer, car, "1.00").success(), "only the owner sells it");
            check(helper, SaleService.setPrice(seller, car, "150.00").success() && car.salePrice() == 15000, "up for sale at $150.00");
            check(helper, !SaleService.buy(seller, car, 15000, Payment.cash()).success(), "you can't buy your own car");
            check(helper, !SaleService.buy(buyer, car, 10000, Payment.cash()).success(), "an out-of-date price is caught");

            Purse.give(buyer, 20_000);
            long sellerBefore = ECO.getBalance(server, seller.getUUID());
            ShopActionResult bought = SaleService.buy(buyer, car, 15000, Payment.cash());
            check(helper, bought.success(), "buying it: " + bought.message().getString());
            helper.assertValueEqual(Purse.value(buyer), 5_000L, "$150.00 cash spent");
            helper.assertValueEqual(ECO.getBalance(server, seller.getUUID()), sellerBefore + 15000, "the seller is paid");
            check(helper, car.isOwner(buyer) && !car.isForSale(), "it's the buyer's now, and off sale");
            check(helper, car.lockCode() != oldLock && CarKeys.holds(buyer, car.lockCode()), "on new locks, with a key for the buyer");
            check(helper, !CarKeys.holds(seller, car.lockCode()) && !car.canBeUsedBy(seller), "the seller's key doesn't fit any more");
        } finally {
            leave(helper, seller, buyer);
        }
        helper.succeed();
    }

    @GameTest(template = YARD)
    public static void dealershipsSellAndTradeIn(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer dealer = join(helper);
        ServerPlayer customer = join(helper);
        try {
            BlockPos desk = new BlockPos(5, 1, 2);
            helper.setBlock(desk, ModBlocks.DEALERSHIP.get().defaultBlockState());
            DealershipBlockEntity lot = (DealershipBlockEntity) helper.getBlockEntity(desk);
            lot.setOwner(dealer);
            ECO.deposit(server, dealer.getUUID(), 100_000, "test");

            CarEntity taxi = vehicle(helper, CarType.TAXI, 5.0, 8.0);
            taxi.setOwner(dealer);
            CarEntity farAway = vehicle(helper, CarType.SEDAN, 5.0, 30.0);
            farAway.setOwner(dealer);
            check(helper, SaleService.setPrice(dealer, taxi, "100.00").success(), "the dealer prices the taxi");
            SaleService.setPrice(dealer, farAway, "50.00");
            check(helper, SaleService.forSale(lot).equals(List.of(taxi)), "only cars on the lot are on its books");

            DealershipMenu browsing = DealershipMenu.create(0, customer, lot);
            helper.assertValueEqual(browsing.carPrice(), 10000L, "the screen shows the price");
            check(helper, browsing.carType() == CarType.TAXI && browsing.carCount() == 1, "and the car");

            customer.moveTo(taxi.getX() + 2, taxi.getY(), taxi.getZ());
            Purse.give(customer, 20_000);
            check(helper, SaleService.buy(customer, taxi, 10000, Payment.cash()).success(), "the customer buys the taxi");
            check(helper, lot.sold() == 1 && lot.earned() == 10000, "the dealership counts the sale");

            // Trading in: the customer's sedan, a bit worn.
            CarEntity sedan = vehicle(helper, CarType.SEDAN, 5.0, 12.0);
            sedan.setOwner(customer);
            giveKey(customer, sedan);
            sedan.setCondition(new CarCondition(1.0F, 1.0F, 0.5F, 1.0F, 1.0F));
            check(helper, SaleService.offerFor(lot, sedan) == 0 && !SaleService.tradeIn(customer, lot, sedan, 0).success(),
                    "not taking sedans yet");
            check(helper, !SaleService.setOffer(customer, lot, CarType.SEDAN, "80.00").success(), "only the dealer sets offers");
            check(helper, SaleService.setOffer(dealer, lot, CarType.SEDAN, "80.00").success(), "the dealer will pay $80 for a sedan");
            long offer = SaleService.offerFor(lot, sedan);
            check(helper, offer < 8000 && offer > 8000 * SaleService.WORN_OUT_SHARE, "less for wear: " + offer);

            DealershipMenu trading = DealershipMenu.create(0, customer, lot);
            trading.clickMenuButton(customer, DealershipMenu.TAB_TRADE);
            helper.assertValueEqual(trading.carCount(), 2, "the customer's taxi and sedan are on the lot");
            for (int i = 0; i < 2 && trading.carType() != CarType.SEDAN; i++) {
                trading.clickMenuButton(customer, DealershipMenu.NEXT);
            }
            helper.assertValueEqual(trading.carOffer(), offer, "the Trade in tab shows the sedan's offer");

            long customerBefore = ECO.getBalance(server, customer.getUUID());
            long dealerBefore = ECO.getBalance(server, dealer.getUUID());
            ShopActionResult traded = SaleService.tradeIn(customer, lot, sedan, offer);
            check(helper, traded.success(), "trading it in: " + traded.message().getString());
            helper.assertValueEqual(ECO.getBalance(server, customer.getUUID()), customerBefore + offer, "paid the offer");
            helper.assertValueEqual(ECO.getBalance(server, dealer.getUUID()), dealerBefore - offer, "from the dealer's account");
            check(helper, dealer.getUUID().equals(sedan.owner()) && !CarKeys.holds(customer, sedan.lockCode()), "the sedan's the dealer's, on new locks");
            check(helper, lot.bought() == 1, "the dealership counts the trade-in");
            check(helper, !SaleService.tradeIn(dealer, lot, sedan, offer).success(), "the dealer can't trade in at their own lot");

            // A dealer who can't pay doesn't buy.
            CarEntity another = vehicle(helper, CarType.SEDAN, 7.0, 6.0);
            another.setOwner(customer);
            ECO.setBalance(server, dealer.getUUID(), 0, "test");
            check(helper, !SaleService.tradeIn(customer, lot, another, SaleService.offerFor(lot, another)).success(), "no money, no trade-in");
            check(helper, another.isOwner(customer), "so the customer keeps it");
        } finally {
            leave(helper, dealer, customer);
        }
        helper.succeed();
    }

    // ---- parking meters and tolls ----

    private static ParkingMeterBlockEntity meter(GameTestHelper helper, BlockPos pos, ServerPlayer owner) {
        helper.setBlock(pos, ModBlocks.PARKING_METER.get().defaultBlockState());
        ParkingMeterBlockEntity meter = (ParkingMeterBlockEntity) helper.getBlockEntity(pos);
        meter.setOwner(owner);
        return meter;
    }

    @GameTest(template = YARD)
    public static void parkingMetersTicketOverstayers(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer council = join(helper);
        ServerPlayer driver = join(helper);
        try {
            ParkingMeterBlockEntity meter = meter(helper, new BlockPos(2, 1, 8), council);
            ParkingMeterBlockEntity next = meter(helper, new BlockPos(2, 1, 13), council);
            CarEntity car = vehicle(helper, CarType.SEDAN, 4.0, 9.0);
            car.setOwner(driver);
            check(helper, MeterService.parkedAt(meter) == car, "the sedan's parked at the meter");
            check(helper, MeterService.parkedAt(next) == null, "and not at the next one along, which is further");
            ECO.setBalance(server, driver.getUUID(), 5_000, "test");
            long councilBefore = ECO.getBalance(server, council.getUUID());

            long now = helper.getLevel().getGameTime();
            meter.update(now);
            meter.update(now + 1_100);
            check(helper, !meter.isExpired() && meter.tickets() == 0, "a minute's grace");
            meter.update(now + 1_201);
            check(helper, meter.isExpired() && meter.tickets() == 1, "then the flag goes up and a ticket");
            helper.assertValueEqual(ECO.getBalance(server, driver.getUUID()), 4_000L, "a $10 fine from the bank");
            helper.assertValueEqual(ECO.getBalance(server, council.getUUID()), councilBefore + 1_000, "to the meter's owner");
            check(helper, helper.getBlockState(new BlockPos(2, 1, 8)).getValue(ParkingMeterBlock.EXPIRED), "the block shows it");
            meter.update(now + 5_000);
            helper.assertValueEqual(meter.tickets(), 1, "no second ticket straight away");
            meter.update(now + 1_201 + 18_000);
            helper.assertValueEqual(meter.tickets(), 2, "another after fifteen minutes");

            // Paying stops it, for as long as was paid.
            long cost = MeterService.cost(meter, 30);
            helper.assertValueEqual(cost, 150L, "30 minutes at 50c per 10");
            check(helper, !MeterService.pay(driver, meter, 30, 100, Payment.cash()).success(), "an out-of-date price is caught");
            Purse.give(driver, 1_000);
            ShopActionResult paid = MeterService.pay(driver, meter, 30, cost, Payment.cash());
            check(helper, paid.success(), "paying: " + paid.message().getString());
            helper.assertValueEqual(Purse.value(driver), 1_000 - cost - com.reiko.economy.config.EconomyConfig.taxRates().salesTax(cost),
                    "cash spent, with tax");
            check(helper, !meter.isExpired() && meter.earned() == cost, "the flag's down and the meter counts it");
            long start = helper.getLevel().getGameTime();
            check(helper, Math.abs(meter.timeLeft(car, start) - 30 * 1_200) <= 20, "half an hour on the clock");
            Purse.give(driver, 2_000);
            check(helper, MeterService.pay(driver, meter, 120, MeterService.cost(meter, 120), Payment.cash()).success(), "two hours more");
            check(helper, !MeterService.pay(driver, meter, 120, MeterService.cost(meter, 120), Payment.cash()).success(),
                    "but never more than four on the meter");
            long paidUntil = start + 150 * 1_200;
            ParkingMeterBlockEntity fresh = meter;
            fresh.update(paidUntil + 600);
            check(helper, !fresh.isExpired(), "still in its grace when the time runs out");
            int before = fresh.tickets();
            fresh.update(paidUntil + 1_300);
            check(helper, fresh.isExpired() && fresh.tickets() == before + 1, "ticketed after it");

            // The meter's owner parks free, and adds time free; nobody's ticketed for sitting in their car.
            CarEntity councilCar = vehicle(helper, CarType.HATCHBACK, 4.0, 26.0);
            councilCar.setOwner(council);
            ParkingMeterBlockEntity third = meter(helper, new BlockPos(2, 1, 26), council);
            third.update(now);
            third.update(now + 50_000);
            check(helper, !third.isExpired() && third.tickets() == 0, "the owner's own car is never ticketed");
            ShopActionResult free = MeterService.pay(council, third, 10, MeterService.cost(third, 10), Payment.cash());
            check(helper, free.success() && third.earned() == 0, "and the owner adds time free");
        } finally {
            leave(helper, council, driver);
        }
        helper.succeed();
    }

    @GameTest(template = YARD, timeoutTicks = 200)
    public static void tollGatesLiftForTheToll(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer owner = join(helper);
        ServerPlayer driver = join(helper);
        try {
            // A gate across the road at z = 16, its pole at x = 7 and its arm reaching west over x = 6, 5, 4.
            BlockPos poleRel = new BlockPos(7, 1, 16);
            BlockState gateState = ModBlocks.TOLL_GATE.get().defaultBlockState().setValue(TollGateBlock.FACING, Direction.NORTH);
            helper.setBlock(poleRel, gateState);
            BlockPos pole = helper.absolutePos(poleRel);
            gateState.getBlock().setPlacedBy(helper.getLevel(), pole, gateState, owner, ItemStack.EMPTY);
            for (int i = 1; i <= TollGateBlock.ARMS; i++) {
                BlockState part = helper.getBlockState(poleRel.west(i));
                check(helper, part.is(ModBlocks.TOLL_GATE.get()) && part.getValue(TollGateBlock.PART) == GatePart.at(i), "arm block " + i);
            }
            check(helper, ((TollGateBlockEntity) helper.getBlockEntity(poleRel)).isOwner(owner), "the gate's the owner's");

            CarEntity car = vehicle(helper, CarType.SEDAN, 5.0, 16.0 - 0.6 - CarType.SEDAN.length / 2.0);
            car.setOwner(driver);
            check(helper, pole.equals(TollService.gateAhead(car)), "the car's pulled up to the gate");
            check(helper, TollService.boothFor(helper.getLevel(), pole) == null, "no booth yet");
            TollService.use(driver, car);
            check(helper, !helper.getBlockState(poleRel).getValue(TollGateBlock.OPEN), "so it only opens for its owner");

            BlockPos boothRel = new BlockPos(9, 1, 15);
            helper.setBlock(boothRel, ModBlocks.TOLL_BOOTH.get().defaultBlockState());
            TollBoothBlockEntity booth = (TollBoothBlockEntity) helper.getBlockEntity(boothRel);
            booth.setOwner(owner);
            check(helper, TollService.boothFor(helper.getLevel(), pole) == booth, "the owner's booth sets its toll");
            check(helper, !TollService.setToll(driver, booth, "0.10").success(), "only the owner sets the toll");
            check(helper, TollService.setToll(owner, booth, "2.50").success() && booth.toll() == 250, "the toll is $2.50");

            Purse.give(driver, 1_000);
            long ownerBefore = ECO.getBalance(server, owner.getUUID());
            BlockPos boothPos = helper.absolutePos(boothRel);
            check(helper, !TollService.pay(driver, pole, boothPos, 200, Payment.cash()).success(), "an out-of-date toll is caught");
            ShopActionResult paid = TollService.pay(driver, pole, boothPos, 250, Payment.cash());
            check(helper, paid.success(), "paying: " + paid.message().getString());
            long tax = com.reiko.economy.config.EconomyConfig.taxRates().salesTax(250);
            helper.assertValueEqual(Purse.value(driver), 1_000 - 250 - tax, "$2.50 and tax in cash");
            helper.assertValueEqual(ECO.getBalance(server, owner.getUUID()), ownerBefore + 250, "to the gate's owner");
            check(helper, booth.cars() == 1 && booth.earned() == 250, "the booth counts it");
            for (int i = 0; i <= TollGateBlock.ARMS; i++) {
                check(helper, helper.getBlockState(poleRel.west(i)).getValue(TollGateBlock.OPEN), "the whole gate lifts: " + i);
            }
            check(helper, TollService.gateAhead(car) == null, "and there's no closed gate ahead now");

            // It waits while the car's under the arm, then shuts behind it.
            car.setPos(car.getX(), car.getY(), pole.getZ() + 0.5);
            TollService.tryClose(helper.getLevel(), pole);
            check(helper, helper.getBlockState(poleRel).getValue(TollGateBlock.OPEN), "it won't come down on the car");
            car.setPos(car.getX(), car.getY(), pole.getZ() + 6.0);
            TollService.tryClose(helper.getLevel(), pole);
            check(helper, !helper.getBlockState(poleRel).getValue(TollGateBlock.OPEN), "and shuts once it's through");

            // The owner drives through free, and can hold it open.
            car.setPos(car.getX(), car.getY(), pole.getZ() - 0.1 - CarType.SEDAN.length / 2.0);
            long ownerCash = Purse.value(owner);
            TollService.use(owner, car);
            check(helper, helper.getBlockState(poleRel).getValue(TollGateBlock.OPEN) && Purse.value(owner) == ownerCash, "free for its owner");
            TollService.tryClose(helper.getLevel(), pole);
            TollService.toggleHeld(owner, helper.getLevel(), pole);
            check(helper, ((TollGateBlockEntity) helper.getBlockEntity(poleRel)).isHeld(), "held open");
            car.discard();
            TollService.tryClose(helper.getLevel(), pole);
            check(helper, helper.getBlockState(poleRel).getValue(TollGateBlock.OPEN), "held gates stay up");
            TollService.toggleHeld(owner, helper.getLevel(), pole);
            TollService.tryClose(helper.getLevel(), pole);
            check(helper, !helper.getBlockState(poleRel).getValue(TollGateBlock.OPEN), "let go, it shuts");

            // Breaking any block of it takes the whole gate down, and gives one gate back.
            helper.getLevel().destroyBlock(helper.absolutePos(poleRel.west(2)), true);
            for (int i = 0; i <= TollGateBlock.ARMS; i++) {
                check(helper, helper.getBlockState(poleRel.west(i)).isAir(), "gone: " + i);
            }
            List<net.minecraft.world.entity.item.ItemEntity> drops = helper.getLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pole).inflate(5));
            int gates = drops.stream().filter(e -> e.getItem().is(ModItems.TOLL_GATE.get())).mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertValueEqual(gates, 1, "one toll gate dropped");
        } finally {
            leave(helper, owner, driver);
        }
        helper.succeed();
    }

    // ---- jobs and insurance ----

    private static ResourceLocation carJob(String name) {
        return ReikoCars.id(name);
    }

    /** {@code player}'s XP in a job. */
    private static long jobXp(ServerPlayer player, ResourceLocation job) {
        return com.reiko.economy.jobs.JobsData.get(player.server).progress(player.getUUID(), job).xp();
    }

    private static ServerPlayer worker(GameTestHelper helper, String... jobs) {
        ServerPlayer player = join(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        for (String job : jobs) {
            helper.assertTrue(com.reiko.economy.jobs.JobService.join(player, carJob(job)).success(), "joining the " + job + " job");
        }
        return player;
    }

    @GameTest(template = YARD)
    public static void carJobsPayForCarWork(GameTestHelper helper) {
        ServerPlayer worker = worker(helper, "mechanic", "trucker");
        ServerPlayer customer = join(helper);
        ResourceLocation mechanic = carJob("mechanic");
        ResourceLocation trucker = carJob("trucker");
        try {
            check(helper, com.reiko.economy.jobs.JobDefinitions.get(carJob("driver")).isPresent()
                    && com.reiko.economy.jobs.JobDefinitions.get(carJob("mail_carrier")).isPresent(), "the car jobs are on the job board");
            check(helper, CarJobs.paid(CarJobs.TAXI_RIDE) && CarJobs.paid(CarJobs.HAUL_DISTANCE), "and pay for car work");

            // Mechanic: the bench's stock on a customer's car, and the worker's own parts on it.
            RepairBenchBlockEntity bench = bench(helper, worker, 1500);
            bench.stock().insertItem(0, new ItemStack(ModItems.TIRE.get(), 4), false);
            CarEntity customers = car(helper, CarType.SEDAN);
            customers.setOwner(customer);
            customers.setCondition(new CarCondition(1.0F, 1.0F, 0.2F, 0.3F, 1.0F));
            ShopActionResult fitted = RepairService.buy(worker, bench, customers, 1 << Part.TIRES.ordinal(), 6000, Payment.cash());
            check(helper, fitted.success(), "the mechanic fits tires from stock: " + fitted.message().getString());
            helper.assertValueEqual(jobXp(worker, mechanic), 20L, "4 parts at 5 XP");
            worker.getInventory().add(new ItemStack(ModItems.BRAKE_PADS.get(), 1));
            check(helper, RepairService.fitOwn(worker, bench, customers, 1 << Part.BRAKES.ordinal()).success(), "and fits their own brake pads");
            helper.assertValueEqual(jobXp(worker, mechanic), 25L, "one more part");
            CarEntity own = vehicle(helper, CarType.HATCHBACK, 3.0, 4.0);
            own.setOwner(worker);
            own.setCondition(new CarCondition(1.0F, 1.0F, 0.2F, 1.0F, 1.0F));
            bench.stock().insertItem(0, new ItemStack(ModItems.TIRE.get(), 4), false);
            RepairService.buy(worker, bench, own, 1 << Part.TIRES.ordinal(), 6000, Payment.cash());
            helper.assertValueEqual(jobXp(worker, mechanic), 25L, "their own car isn't paid work");
            own.discard();
            customers.discard();

            // Mechanic: towing a breakdown in to the bench, once it's come far enough.
            CarEntity truck = vehicle(helper, CarType.FLATBED, 5.0, 9.0);
            CarEntity broken = behind(helper, truck, CarType.SEDAN, 0.3);
            broken.setOwner(customer);
            broken.setCondition(new CarCondition(1.0F, 0.0F, 1.0F, 1.0F, 1.0F));
            worker.startRiding(truck, true);
            Hitching.use(worker);
            check(helper, Towing.towed(truck) == broken, "the flatbed picks it up");
            Hitching.use(worker);
            check(helper, !broken.isPassenger() && RepairService.benchNear(broken) != null, "and puts it down by the bench");
            helper.assertValueEqual(jobXp(worker, mechanic), 25L, "a tow of a few blocks doesn't pay");
            Hitching.use(worker);
            broken.setTowedFrom(broken.position().add(0, 0, -40)); // as if it came from 40 blocks away
            Hitching.use(worker);
            long towed = jobXp(worker, mechanic);
            check(helper, towed >= 45L, "a real tow-in pays (and may finish a tow-in contract): " + towed);
            Hitching.use(worker);
            broken.setTowedFrom(broken.position().add(0, 0, -40));
            Hitching.use(worker);
            helper.assertValueEqual(jobXp(worker, mechanic), towed, "but only once per breakdown");
            worker.stopRiding();
            truck.discard();
            broken.discard();

            // Trucker: fuel from a tanker into a pump, a bucket at a time.
            BlockPos pumpAt = new BlockPos(8, 1, 6);
            BlockState lower = ModBlocks.GAS_PUMP.get().defaultBlockState().setValue(GasPumpBlock.FACING, Direction.WEST);
            helper.setBlock(pumpAt, lower);
            helper.setBlock(pumpAt.above(), lower.setValue(GasPumpBlock.HALF, DoubleBlockHalf.UPPER));
            ((GasPumpBlockEntity) helper.getBlockEntity(pumpAt)).setOwner(customer);
            TrailerEntity tanker = trailer(helper, TrailerType.TANKER_TRAILER, new Vec3(5.5, 1.0, 6.0), 0.0F);
            tanker.tank().fill(new FluidStack(bc("fuel_light"), 3500), IFluidHandler.FluidAction.EXECUTE);
            TankerService.unload(worker, tanker);
            helper.assertValueEqual(tanker.tank().getFluidAmount(), 0, "the pump takes it all");
            helper.assertValueEqual(jobXp(worker, trucker), 9L, "3 whole buckets at 3 XP");
            tanker.tank().fill(new FluidStack(bc("fuel_light"), 500), IFluidHandler.FluidAction.EXECUTE);
            TankerService.unload(worker, tanker);
            helper.assertValueEqual(jobXp(worker, trucker), 12L, "the half bucket left over makes a fourth");
        } finally {
            leave(helper, worker, customer);
        }
        helper.succeed();
    }

    @GameTest(template = YARD)
    public static void insuranceCoversCrashRepairs(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer insurer = join(helper);
        ServerPlayer customer = join(helper);
        ServerPlayer mechanicOwner = join(helper);
        try {
            BlockPos officeAt = new BlockPos(8, 1, 12);
            helper.setBlock(officeAt, ModBlocks.INSURANCE_OFFICE.get().defaultBlockState());
            InsuranceOfficeBlockEntity office = (InsuranceOfficeBlockEntity) helper.getBlockEntity(officeAt);
            office.setOwner(insurer);
            check(helper, office.premium() == 1000 && office.deductible() == 1000 && office.limit() == 10_000, "default terms: $10, $10, $100");
            CarEntity car = vehicle(helper, CarType.SEDAN, 5.0, 10.0);
            car.setOwner(customer);
            CarEntity insurers = vehicle(helper, CarType.HATCHBACK, 5.0, 16.0);
            insurers.setOwner(insurer);

            InsuranceMenu screen = InsuranceMenu.create(0, customer, office);
            check(helper, screen.hasCar() && screen.carType() == CarType.SEDAN && screen.status() == InsuranceMenu.UNINSURED,
                    "the customer's sedan, not insured");
            check(helper, !InsuranceService.buy(insurer, office, insurers, 1000, Payment.cash()).success(), "no insuring at your own office");
            check(helper, !InsuranceService.buy(customer, office, car, 500, Payment.cash()).success(), "an out-of-date premium is caught");

            Purse.give(customer, 10_000);
            ECO.setBalance(server, insurer.getUUID(), 0, "test");
            long tax = com.reiko.economy.config.EconomyConfig.taxRates().salesTax(1000);
            ShopActionResult bought = InsuranceService.buy(customer, office, car, 1000, Payment.cash());
            check(helper, bought.success(), "buying cover: " + bought.message().getString());
            helper.assertValueEqual(Purse.value(customer), 10_000 - 1000 - tax, "$10 and tax in cash");
            helper.assertValueEqual(ECO.getBalance(server, insurer.getUUID()), 1000L, "to the insurer");
            check(helper, car.activePolicy() != null && car.activePolicy().daysLeft(helper.getLevel().getGameTime()) == 7, "7 days of cover");
            screen.refresh();
            check(helper, screen.status() == InsuranceMenu.INSURED_HERE && screen.daysLeft() == 7, "the screen shows it");
            for (int i = 0; i < 3; i++) {
                check(helper, InsuranceService.buy(customer, office, car, 1000, Payment.cash()).success(), "extending, term " + (i + 2));
            }
            helper.assertValueEqual(car.activePolicy().daysLeft(helper.getLevel().getGameTime()), 28, "four terms ahead");
            check(helper, !InsuranceService.buy(customer, office, car, 1000, Payment.cash()).success(), "but no further");
            check(helper, office.policies() == 4 && office.premiums() == 4000, "the office's books");
            check(helper, car.toItem().has(com.reiko.cars.registry.ModDataComponents.INSURANCE.get()), "a picked-up car keeps its cover");

            // A crash: the body repairs, paid for at a mechanic's bench, are claimed back above the deductible.
            RepairBenchBlockEntity bench = bench(helper, mechanicOwner, 2000);
            bench.stock().insertItem(Part.BODY.ordinal(), new ItemStack(ModItems.BODY_PANEL.get(), 16), false);
            bench.stock().insertItem(Part.TIRES.ordinal(), new ItemStack(ModItems.TIRE.get(), 4), false);
            car.moveTo(helper.absoluteVec(new Vec3(4.5, 1.0, 2.5)));
            car.setCondition(new CarCondition(1.0F, 1.0F, 1.0F, 1.0F, 0.5F));
            int panels = RepairService.jobSize(Part.BODY, com.reiko.cars.repair.RepairJobs.needed(Part.BODY, car, car.condition()), 16);
            long bodyCost = panels * 2000L;
            long payout = Math.min(10_000, bodyCost - 1000);
            check(helper, payout > 0, "a claim worth making: " + panels + " panels");
            ECO.setBalance(server, insurer.getUUID(), 50_000, "test");
            ECO.setBalance(server, customer.getUUID(), 0, "test");
            Purse.give(customer, 50_000);
            ShopActionResult repaired = RepairService.buy(customer, bench, car, 1 << Part.BODY.ordinal(), bodyCost, Payment.cash());
            check(helper, repaired.success(), "paying for the body work: " + repaired.message().getString());
            helper.assertValueEqual(ECO.getBalance(server, customer.getUUID()), payout, "the insurer pays back the cost above the deductible");
            helper.assertValueEqual(ECO.getBalance(server, insurer.getUUID()), 50_000 - payout, "from their bank");
            check(helper, office.claims() == 1 && office.paidOut() == payout, "the office counts the claim");

            // Wear isn't covered.
            car.setCondition(new CarCondition(1.0F, 1.0F, 0.2F, 1.0F, 1.0F));
            RepairService.buy(customer, bench, car, 1 << Part.TIRES.ordinal(), 8000, Payment.cash());
            helper.assertValueEqual(ECO.getBalance(server, insurer.getUUID()), 50_000 - payout, "tires are the owner's cost");

            // An insurer who can't pay doesn't.
            ECO.setBalance(server, insurer.getUUID(), 0, "test");
            long customerBank = ECO.getBalance(server, customer.getUUID());
            car.setCondition(new CarCondition(1.0F, 1.0F, 1.0F, 1.0F, 0.5F));
            RepairService.buy(customer, bench, car, 1 << Part.BODY.ordinal(), bodyCost, Payment.cash());
            helper.assertValueEqual(ECO.getBalance(server, customer.getUUID()), customerBank, "no money, no claim");

            // Sold: the cover was the old owner's.
            car.setOwner(mechanicOwner);
            check(helper, car.activePolicy() == null, "a sold car's cover lapses");
        } finally {
            leave(helper, insurer, customer, mechanicOwner);
        }
        helper.succeed();
    }
    // ---- work vehicles ----

    /** How many items are lying on the ground in the box from {@code from} to {@code to} (test coordinates). */
    private static int itemsOnGround(GameTestHelper helper, Vec3 from, Vec3 to) {
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(helper.absoluteVec(from), helper.absoluteVec(to));
        int count = 0;
        for (net.minecraft.world.entity.item.ItemEntity item : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            count += item.getItem().getCount();
        }
        return count;
    }

    @GameTest(template = YARD)
    public static void emergencyVehiclesAndTheMixer(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        ServerPlayer patient = join(helper);
        try {
            // The police car's lights and siren, on and off; a sedan has none.
            CarEntity police = vehicle(helper, CarType.POLICE_CAR, 3.0, 4.0);
            driver.startRiding(police, true);
            com.reiko.cars.car.WorkVehicles.toggleSiren(driver, police);
            check(helper, police.sirenOn(), "K turns the siren on");
            com.reiko.cars.car.WorkVehicles.toggleSiren(driver, police);
            check(helper, !police.sirenOn(), "and off");
            com.reiko.cars.car.WorkVehicles.toggleSiren(patient, police);
            check(helper, !police.sirenOn(), "only the driver works it");
            CarEntity sedan = vehicle(helper, CarType.SEDAN, 7.0, 4.0);
            sedan.setSiren(true);
            check(helper, !sedan.sirenOn(), "a sedan has no siren");
            CompoundTag saved = new CompoundTag();
            police.setSiren(true);
            police.saveWithoutId(saved);
            police.setSiren(false);
            police.load(saved);
            check(helper, police.sirenOn(), "the siren's saved");
            driver.stopRiding();
            police.discard();
            sedan.discard();

            // The ambulance heals whoever rides in the back, but not its driver.
            CarEntity ambulance = vehicle(helper, CarType.AMBULANCE, 5.0, 6.0);
            driver.startRiding(ambulance, true);
            patient.startRiding(ambulance, true);
            check(helper, ambulance.getControllingPassenger() == driver && patient.getVehicle() == ambulance, "a driver and a patient");
            driver.setHealth(10.0F);
            patient.setHealth(10.0F);
            com.reiko.cars.car.WorkVehicles.heal(ambulance);
            com.reiko.cars.car.WorkVehicles.heal(ambulance);
            near(helper, patient.getHealth(), 12.0, "the patient's healed a heart");
            near(helper, driver.getHealth(), 10.0, "the driver isn't");
            driver.stopRiding();
            patient.stopRiding();
            ambulance.discard();

            // The cement mixer: concrete powder sets in the drum, which pours out at the back.
            CarEntity mixer = vehicle(helper, CarType.CEMENT_MIXER, 5.0, 12.0);
            mixer.bed().addItem(new ItemStack(Items.WHITE_CONCRETE_POWDER, 3));
            mixer.bed().addItem(new ItemStack(Items.RED_CONCRETE_POWDER, 1));
            mixer.bed().addItem(new ItemStack(Items.COBBLESTONE, 2));
            int mixed = 0;
            while (com.reiko.cars.car.WorkVehicles.mixOne(mixer)) {
                mixed++;
            }
            helper.assertValueEqual(mixed, 4, "every block of powder sets");
            check(helper, mixer.bed().countItem(Items.WHITE_CONCRETE) == 3 && mixer.bed().countItem(Items.RED_CONCRETE) == 1
                    && mixer.bed().countItem(Items.WHITE_CONCRETE_POWDER) == 0, "into concrete of the same colour");
            helper.assertValueEqual(mixer.bed().countItem(Items.COBBLESTONE), 2, "anything else just rides along");
            driver.startRiding(mixer, true);
            check(helper, Hitching.available(mixer, driver) == Hitching.Action.POUR, "G pours");
            Hitching.use(driver);
            check(helper, mixer.bed().isEmpty(), "the drum's empty");
            helper.assertValueEqual(itemsOnGround(helper, new Vec3(2, 0, 0), new Vec3(9, 4, 9)), 6, "and it's all behind the mixer");
            driver.stopRiding();
            mixer.discard();
        } finally {
            leave(helper, driver, patient);
        }
        helper.succeed();
    }

    @GameTest(template = YARD, timeoutTicks = 200)
    public static void dumpTrucksTipTheirLoad(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        CarEntity truck = vehicle(helper, CarType.DUMP_TRUCK, 5.0, 20.0);
        truck.bed().addItem(new ItemStack(Items.DIRT, 64));
        truck.bed().addItem(new ItemStack(Items.GRAVEL, 10));
        driver.startRiding(truck, true);
        check(helper, Hitching.available(truck, driver) == Hitching.Action.DUMP, "G tips the bed");
        Hitching.use(driver);
        check(helper, truck.tipStart() >= 0 && !truck.bed().isEmpty(), "the bed starts up, still loaded");
        check(helper, Hitching.available(truck, driver) == null, "and can't be tipped again while it's up");
        helper.runAfterDelay(com.reiko.cars.car.WorkVehicles.TIP_DUMP_AT + 2, () -> {
            check(helper, truck.bed().isEmpty(), "the load slides out");
            helper.assertValueEqual(itemsOnGround(helper, new Vec3(2, 0, 9), new Vec3(9, 4, 17)), 74, "behind the truck");
            near(helper, truck.bedTilt(0.0F), 1.0, "with the bed all the way up");
        });
        helper.runAfterDelay(com.reiko.cars.car.WorkVehicles.TIP_UP + com.reiko.cars.car.WorkVehicles.TIP_HOLD
                + com.reiko.cars.car.WorkVehicles.TIP_DOWN + 5, () -> {
            check(helper, truck.tipStart() < 0 && truck.bedTilt(0.0F) == 0.0F, "then it comes back down");
            leave(helper, driver);
            helper.succeed();
        });
    }

    @GameTest(template = YARD)
    public static void garbageTrucksEmptyTrashCans(GameTestHelper helper) {
        ServerPlayer worker = worker(helper, "garbage_collector");
        ResourceLocation job = carJob("garbage_collector");
        try {
            CarEntity truck = vehicle(helper, CarType.GARBAGE_TRUCK, 5.5, 14.0); // its hopper's at z 10
            BlockPos left = new BlockPos(7, 1, 10);
            BlockPos right = new BlockPos(3, 1, 10);
            BlockPos far = new BlockPos(5, 1, 2);
            for (BlockPos pos : new BlockPos[] {left, right, far}) {
                helper.setBlock(pos, ModBlocks.TRASH_CAN.get().defaultBlockState());
            }
            var leftCan = (com.reiko.cars.garbage.TrashCanBlockEntity) helper.getBlockEntity(left);
            var rightCan = (com.reiko.cars.garbage.TrashCanBlockEntity) helper.getBlockEntity(right);
            var farCan = (com.reiko.cars.garbage.TrashCanBlockEntity) helper.getBlockEntity(far);
            leftCan.setItem(0, new ItemStack(Items.ROTTEN_FLESH, 20));
            rightCan.setItem(3, new ItemStack(Items.BONE, 5));
            farCan.setItem(0, new ItemStack(Items.STICK, 7));

            worker.startRiding(truck, true);
            check(helper, Hitching.available(truck, worker) == Hitching.Action.TRASH, "G empties the cans by the hopper");
            Hitching.use(worker);
            check(helper, leftCan.isEmpty() && rightCan.isEmpty(), "both cans by the hopper are emptied");
            helper.assertValueEqual(farCan.itemCount(), 7, "the one down the road isn't");
            helper.assertValueEqual(itemsOnGround(helper, new Vec3(0, 0, 0), new Vec3(10, 4, 34)), 0, "the trash is gone, not dropped");
            helper.assertValueEqual(jobXp(worker, job), 20L, "two cans at 10 XP");

            leftCan.setItem(0, new ItemStack(Items.ROTTEN_FLESH, 3));
            Hitching.use(worker);
            check(helper, leftCan.isEmpty(), "a can can be emptied again");
            helper.assertValueEqual(jobXp(worker, job), 20L, "but it only pays once a day");
            leftCan.setLastPaid(helper.getLevel().getGameTime() - com.reiko.cars.garbage.GarbageService.PAY_EVERY);
            leftCan.setItem(0, new ItemStack(Items.ROTTEN_FLESH, 3));
            Hitching.use(worker);
            long nextDay = jobXp(worker, job);
            check(helper, nextDay >= 30L, "the next day it pays again"); // (and maybe finishes a contract for 3 cans)
            Hitching.use(worker);
            helper.assertValueEqual(jobXp(worker, job), nextDay, "empty cans don't pay");

            CompoundTag saved = leftCan.saveWithoutMetadata(helper.getLevel().registryAccess());
            check(helper, saved.getLong("LastPaid") == leftCan.lastPaid(), "when it was last paid is saved");
            worker.stopRiding();
            truck.discard();
        } finally {
            leave(helper, worker);
        }
        helper.succeed();
    }

    @GameTest(template = YARD)
    public static void iceCreamTrucksSellTreats(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer owner = join(helper);
        ServerPlayer customer = join(helper);
        try {
            CarEntity truck = vehicle(helper, CarType.ICE_CREAM_TRUCK, 5.0, 8.0);
            truck.setOwner(owner);
            truck.bed().addItem(new ItemStack(ModItems.VANILLA_ICE_CREAM.get(), 5));
            truck.bed().addItem(new ItemStack(ModItems.CHOCOLATE_ICE_CREAM.get(), 3));
            Item vanilla = ModItems.VANILLA_ICE_CREAM.get();
            Item chocolate = ModItems.CHOCOLATE_ICE_CREAM.get();
            check(helper, com.reiko.cars.icecream.IceCreamService.menu(truck, false).isEmpty(), "nothing's for sale until it's priced");
            helper.assertValueEqual(com.reiko.cars.icecream.IceCreamService.menu(truck, true).size(), 2, "though the owner sees the stock");
            check(helper, !com.reiko.cars.icecream.IceCreamService.setPrice(customer, truck, vanilla, "0.01").success(), "only the owner sets prices");
            check(helper, com.reiko.cars.icecream.IceCreamService.setPrice(owner, truck, vanilla, "2.00").success(), "the owner prices vanilla");
            check(helper, !com.reiko.cars.icecream.IceCreamService.setPrice(owner, truck, chocolate, "lots").success(), "a price has to be money");
            helper.assertValueEqual(com.reiko.cars.icecream.IceCreamService.menu(truck, false), List.of(vanilla), "vanilla's on the menu");

            // The shop screen: the serving hatch is on the left (+x, facing south).
            check(helper, com.reiko.cars.car.WorkVehicles.atHatch(truck, new Vec3(1.1, 1.0, 0.0))
                    && !com.reiko.cars.car.WorkVehicles.atHatch(truck, new Vec3(-1.1, 1.0, 0.0)), "the hatch is on the left");
            var menu = com.reiko.cars.icecream.IceCreamMenu.create(1, customer, truck);
            check(helper, !menu.isOwner() && menu.count() == 1 && menu.item().is(vanilla) && menu.price() == 200 && menu.stock() == 5,
                    "the shop shows vanilla at $2.00, 5 left");
            check(helper, !truck.interactAt(owner, new Vec3(-1.1, 1.0, 0.0), InteractionHand.MAIN_HAND).consumesAction(),
                    "the owner's other door is just a door");
            var own = com.reiko.cars.icecream.IceCreamMenu.create(2, owner, truck);
            check(helper, own.isOwner() && own.count() == 2 && own.price() == 200, "the owner sees the whole freezer to price");
            own.clickMenuButton(owner, com.reiko.cars.icecream.IceCreamMenu.NEXT);
            check(helper, own.item().is(chocolate) && own.price() == -1 && own.stock() == 3, "and picks chocolate, not priced yet");

            // Buying one.
            Purse.give(customer, 1_000);
            long tax = com.reiko.economy.config.EconomyConfig.taxRates().salesTax(200);
            long ownerBefore = ECO.getBalance(server, owner.getUUID());
            check(helper, !com.reiko.cars.icecream.IceCreamService.buy(owner, truck, vanilla, 200, Payment.cash()).success(), "the owner can't buy their own");
            check(helper, !com.reiko.cars.icecream.IceCreamService.buy(customer, truck, vanilla, 100, Payment.cash()).success(), "an old price is caught");
            check(helper, !com.reiko.cars.icecream.IceCreamService.buy(customer, truck, chocolate, 0, Payment.cash()).success(), "chocolate isn't for sale");
            ShopActionResult bought = com.reiko.cars.icecream.IceCreamService.buy(customer, truck, vanilla, 200, Payment.cash());
            check(helper, bought.success(), "buying a vanilla: " + bought.message().getString());
            check(helper, bought.message().getString().contains("Vanilla Ice Cream"), "the receipt names it: " + bought.message().getString());
            helper.assertValueEqual(customer.getInventory().countItem(vanilla), 1, "one vanilla ice cream");
            helper.assertValueEqual(truck.bed().countItem(vanilla), 4, "out of the freezer");
            helper.assertValueEqual(Purse.value(customer), 1_000L - 200 - tax, "paid $2.00 and the tax");
            helper.assertValueEqual(ECO.getBalance(server, owner.getUUID()), ownerBefore + 200, "the owner gets the price");

            check(helper, com.reiko.cars.icecream.IceCreamService.setPrice(owner, truck, vanilla, "").success(), "clearing the price");
            check(helper, com.reiko.cars.icecream.IceCreamService.menu(truck, false).isEmpty(), "takes it off the menu");
            com.reiko.cars.icecream.IceCreamService.setPrice(owner, truck, chocolate, "1.50");
            CompoundTag saved = new CompoundTag();
            truck.saveWithoutId(saved);
            truck.shopPrices().clear();
            truck.load(saved);
            helper.assertValueEqual(truck.shopPrices().get(chocolate), 150L, "prices are saved");
            check(helper, ModItems.STRAWBERRY_ICE_CREAM.get().components().has(net.minecraft.core.component.DataComponents.FOOD), "ice cream is food");
        } finally {
            leave(helper, owner, customer);
        }
        helper.succeed();
    }
    // ---- turn signals, hazards and the alarm ----

    /** Turns {@code car} by {@code degrees} (negative is left) over {@code ticks} ticks, then runs it straight as long. */
    private static void steer(CarEntity car, float degrees, int ticks) {
        for (int i = 0; i < ticks; i++) {
            car.setYRot(car.getYRot() + degrees / ticks);
            com.reiko.cars.car.CarSignals.serverTick(car);
        }
    }

    @GameTest(template = YARD)
    public static void turnSignalsAndHazards(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        ServerPlayer rider = join(helper);
        try {
            CarEntity car = vehicle(helper, CarType.SEDAN, 5.0, 8.0);
            driver.startRiding(car, true);
            rider.startRiding(car, true);
            com.reiko.cars.car.CarSignals.serverTick(car);
            com.reiko.cars.car.CarSignals.toggle(driver, car, com.reiko.cars.car.CarSignals.LEFT);
            check(helper, car.signal() == com.reiko.cars.car.CarSignals.LEFT, "the left arrow signals left");
            check(helper, com.reiko.cars.car.CarSignals.blinking(car, true) && !com.reiko.cars.car.CarSignals.blinking(car, false), "the left side blinks");
            com.reiko.cars.car.CarSignals.toggle(driver, car, com.reiko.cars.car.CarSignals.LEFT);
            check(helper, car.signal() == com.reiko.cars.car.CarSignals.OFF, "again turns it off");
            com.reiko.cars.car.CarSignals.toggle(rider, car, com.reiko.cars.car.CarSignals.RIGHT);
            check(helper, car.signal() == com.reiko.cars.car.CarSignals.OFF, "only the driver signals");

            // A signal goes off by itself once the car's turned that way and straightened up, not before.
            com.reiko.cars.car.CarSignals.toggle(driver, car, com.reiko.cars.car.CarSignals.LEFT);
            steer(car, -20.0F, 10);
            steer(car, 0.0F, 20);
            check(helper, car.signal() == com.reiko.cars.car.CarSignals.LEFT, "a lane change's worth of turn leaves it on");
            steer(car, 60.0F, 12);
            steer(car, 0.0F, 20);
            check(helper, car.signal() == com.reiko.cars.car.CarSignals.LEFT, "turning right doesn't cancel the left signal");
            steer(car, -90.0F, 18);
            check(helper, car.signal() == com.reiko.cars.car.CarSignals.LEFT, "still on mid-turn");
            steer(car, 0.0F, com.reiko.cars.car.CarSignals.STRAIGHT_TICKS + 1);
            check(helper, car.signal() == com.reiko.cars.car.CarSignals.OFF, "after the left turn, straightening up cancels it");
            com.reiko.cars.car.CarSignals.toggle(driver, car, com.reiko.cars.car.CarSignals.RIGHT);
            steer(car, 90.0F, 18);
            steer(car, 0.0F, com.reiko.cars.car.CarSignals.STRAIGHT_TICKS + 1);
            check(helper, car.signal() == com.reiko.cars.car.CarSignals.OFF, "and the same for a right turn");

            // Hazards: the down arrow, and on by themselves when it breaks down. They don't cancel.
            com.reiko.cars.car.CarSignals.toggle(driver, car, com.reiko.cars.car.CarSignals.HAZARDS);
            check(helper, com.reiko.cars.car.CarSignals.blinking(car, true) && com.reiko.cars.car.CarSignals.blinking(car, false), "hazards blink both sides");
            steer(car, -90.0F, 18);
            steer(car, 0.0F, 20);
            check(helper, car.signal() == com.reiko.cars.car.CarSignals.HAZARDS, "and stay on through turns");
            CompoundTag saved = new CompoundTag();
            car.saveWithoutId(saved);
            car.setSignal(com.reiko.cars.car.CarSignals.OFF);
            car.load(saved);
            check(helper, car.signal() == com.reiko.cars.car.CarSignals.HAZARDS, "they're saved");
            com.reiko.cars.car.CarSignals.toggle(driver, car, com.reiko.cars.car.CarSignals.HAZARDS);
            check(helper, car.signal() == com.reiko.cars.car.CarSignals.OFF, "the down arrow turns them off");
            car.setCondition(new CarCondition(1.0F, 0.0F, 1.0F, 1.0F, 1.0F));
            com.reiko.cars.car.CarSignals.serverTick(car);
            check(helper, car.isBrokenDown() && car.signal() == com.reiko.cars.car.CarSignals.HAZARDS, "a breakdown puts the hazards on");
        } finally {
            leave(helper, driver, rider);
        }
        helper.succeed();
    }

    @GameTest(template = YARD)
    public static void carAlarmsCatchStrangers(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        ServerPlayer stranger = join(helper);
        ServerPlayer friend = join(helper);
        try {
            CarEntity car = vehicle(helper, CarType.SEDAN, 5.0, 8.0);
            car.setOwner(owner);
            giveKey(owner, car);
            giveKey(friend, car);
            car.setFuel(100_000);
            car.setLocked(false);
            stranger.moveTo(car.getX() + 2, car.getY(), car.getZ());
            car.interact(stranger, InteractionHand.MAIN_HAND);
            check(helper, stranger.getVehicle() == car && !car.alarmOn(), "unlocked, anyone gets in and there's no alarm");
            stranger.stopRiding();

            car.setLocked(true);
            car.interact(stranger, InteractionHand.MAIN_HAND);
            check(helper, stranger.getVehicle() == null, "locked, the stranger can't get in");
            check(helper, car.alarmOn(), "and the alarm goes off");
            long now = helper.getLevel().getGameTime();
            helper.assertValueEqual(car.alarmUntil(), now + 30 * 20L, "for half a minute");
            check(helper, com.reiko.cars.car.CarSignals.blinking(car, true) && com.reiko.cars.car.CarSignals.blinking(car, false), "flashing every light");
            check(helper, car.toggleLock(owner) && !car.alarmOn() && car.isLocked(), "the owner's L stops it, still locked");

            car.hurt(helper.getLevel().damageSources().playerAttack(stranger), 1.0F);
            check(helper, car.alarmOn(), "hitting it sets it off too");
            friend.startRiding(car, true);
            com.reiko.cars.car.CarSignals.serverTick(car);
            check(helper, !car.alarmOn(), "someone with the key getting in stops it");
            friend.stopRiding();

            car.setAlarmUntil(helper.getLevel().getGameTime() - 1);
            com.reiko.cars.car.CarSignals.serverTick(car);
            helper.assertValueEqual(car.alarmUntil(), 0L, "it runs out");
            car.hurt(helper.getLevel().damageSources().playerAttack(friend), 1.0F);
            check(helper, !car.alarmOn(), "a friend with the key doesn't set it off");
            car.hurt(helper.getLevel().damageSources().playerAttack(owner), 0.1F);
            check(helper, !car.alarmOn(), "nor does its owner");

            com.reiko.cars.config.CarsConfig.ALARM_SECONDS.set(0);
            try {
                car.interact(stranger, InteractionHand.MAIN_HAND);
                check(helper, !car.alarmOn(), "alarmSeconds 0 turns alarms off");
            } finally {
                com.reiko.cars.config.CarsConfig.ALARM_SECONDS.set(30);
            }
        } finally {
            leave(helper, owner, stranger, friend);
        }
        helper.succeed();
    }
    // ---- roads, traffic lights and cameras ----

    /** {@code player} right-clicks the top of the block at {@code pos} (test coordinates) with what's in their main hand. */
    private static void useOnTop(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos at = helper.absolutePos(pos);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false);
        player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    @GameTest(template = YARD)
    public static void asphaltAndRoadPaint(GameTestHelper helper) {
        ServerPlayer painter = join(helper);
        try {
            BlockPos road = new BlockPos(5, 0, 6);
            helper.setBlock(road, ModBlocks.ASPHALT.get().defaultBlockState());
            BlockPos slab = new BlockPos(5, 0, 8);
            helper.setBlock(slab, ModBlocks.ASPHALT_SLAB.get().defaultBlockState());
            ItemStack paint = new ItemStack(ModItems.ROAD_PAINT.get());
            painter.setItemInHand(InteractionHand.MAIN_HAND, paint);
            check(helper, com.reiko.cars.road.RoadPaintItem.marking(paint) == com.reiko.cars.road.RoadMarking.CENTER_DASHED, "a new can paints the center line");
            paint.getItem().use(helper.getLevel(), painter, InteractionHand.MAIN_HAND);
            check(helper, com.reiko.cars.road.RoadPaintItem.marking(paint) == com.reiko.cars.road.RoadMarking.CENTER_SOLID, "right-clicking the air picks the next marking");
            painter.setShiftKeyDown(true);
            paint.getItem().use(helper.getLevel(), painter, InteractionHand.MAIN_HAND);
            paint.getItem().use(helper.getLevel(), painter, InteractionHand.MAIN_HAND);
            painter.setShiftKeyDown(false);
            check(helper, com.reiko.cars.road.RoadPaintItem.marking(paint) == com.reiko.cars.road.RoadMarking.ARROW_RIGHT, "sneaking goes back, round past the first");
            paint.getItem().use(helper.getLevel(), painter, InteractionHand.MAIN_HAND);

            painter.moveTo(helper.absoluteVec(new Vec3(5.5, 1.0, 9.5)));
            painter.setYRot(180.0F); // facing north
            painter.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            useOnTop(helper, painter, road);
            net.minecraft.world.level.block.state.BlockState painted = helper.getBlockState(road);
            check(helper, painted.getValue(com.reiko.cars.road.Asphalt.MARKING) == com.reiko.cars.road.RoadMarking.CENTER_DASHED
                    && painted.getValue(com.reiko.cars.road.Asphalt.FACING) == Direction.NORTH, "painted a center line, running north");
            helper.assertValueEqual(paint.getDamageValue(), 1, "a coat of paint used");
            useOnTop(helper, painter, road);
            check(helper, helper.getBlockState(road).getValue(com.reiko.cars.road.Asphalt.MARKING) == com.reiko.cars.road.RoadMarking.NONE, "painting it again scrapes it off");
            helper.assertValueEqual(paint.getDamageValue(), 1, "which uses no paint");
            painter.setYRot(270.0F); // facing east
            useOnTop(helper, painter, slab);
            check(helper, helper.getBlockState(slab).getValue(com.reiko.cars.road.Asphalt.FACING) == Direction.EAST
                    && helper.getBlockState(slab).getValue(com.reiko.cars.road.Asphalt.MARKING) == com.reiko.cars.road.RoadMarking.CENTER_DASHED, "slabs take paint too");

            CarEntity car = vehicle(helper, CarType.SEDAN, 5.5, 6.5);
            check(helper, car.isOnRoad(), "a car on asphalt is on the road");
            car.moveTo(helper.absoluteVec(new Vec3(2.5, 1.0, 20.5)));
            check(helper, !car.isOnRoad(), "and off it elsewhere");
        } finally {
            leave(helper, painter);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 700)
    public static void trafficLightsTakeTurns(GameTestHelper helper) {
        int cycle = (int) Math.round((com.reiko.cars.config.CarsConfig.get(com.reiko.cars.config.CarsConfig.GREEN_SECONDS)
                + com.reiko.cars.config.CarsConfig.get(com.reiko.cars.config.CarsConfig.YELLOW_SECONDS)
                + com.reiko.cars.config.CarsConfig.get(com.reiko.cars.config.CarsConfig.ALL_RED_SECONDS)) * 40);
        java.util.EnumMap<com.reiko.cars.traffic.Signal, Integer> seen = new java.util.EnumMap<>(com.reiko.cars.traffic.Signal.class);
        int allRed = 0;
        for (int t = 0; t < cycle; t++) {
            com.reiko.cars.traffic.Signal ns = com.reiko.cars.traffic.TrafficLightBlock.signalAt(t, Direction.NORTH, false);
            com.reiko.cars.traffic.Signal ew = com.reiko.cars.traffic.TrafficLightBlock.signalAt(t, Direction.EAST, false);
            check(helper, ns == com.reiko.cars.traffic.Signal.RED || ew == com.reiko.cars.traffic.Signal.RED, "a crossing never lets both ways go, at tick " + t);
            check(helper, com.reiko.cars.traffic.TrafficLightBlock.signalAt(t, Direction.SOUTH, false) == ns, "opposite lights match");
            seen.merge(ns, 1, Integer::sum);
            if (ns == com.reiko.cars.traffic.Signal.RED && ew == com.reiko.cars.traffic.Signal.RED) {
                allRed++;
            }
        }
        helper.assertValueEqual(seen.get(com.reiko.cars.traffic.Signal.GREEN), 200, "10 s of green");
        helper.assertValueEqual(seen.get(com.reiko.cars.traffic.Signal.YELLOW), 60, "3 s of yellow");
        helper.assertValueEqual(allRed, 40, "1 s all red, twice a cycle");
        check(helper, com.reiko.cars.traffic.TrafficLightBlock.signalAt(0, Direction.NORTH, true) == com.reiko.cars.traffic.Signal.RED, "redstone holds it on red");

        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, ModBlocks.TRAFFIC_LIGHT.get().defaultBlockState().setValue(com.reiko.cars.traffic.TrafficLightBlock.FACING, Direction.SOUTH));
        helper.runAfterDelay(6, () -> {
            net.minecraft.world.level.block.state.BlockState state = helper.getBlockState(at);
            check(helper, state.getValue(com.reiko.cars.traffic.TrafficLightBlock.SIGNAL) == com.reiko.cars.traffic.TrafficLightBlock.signal(helper.getLevel(), state),
                    "the light shows what the clock says");
            helper.setBlock(at.east(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState());
            check(helper, helper.getBlockState(at).getValue(com.reiko.cars.traffic.TrafficLightBlock.POWERED)
                    && helper.getBlockState(at).getValue(com.reiko.cars.traffic.TrafficLightBlock.SIGNAL) == com.reiko.cars.traffic.Signal.RED, "powered, it goes red");
            helper.setBlock(at.east(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            // Unpowered, it runs on the clock again: it turns green within a cycle.
            helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(at).getValue(com.reiko.cars.traffic.TrafficLightBlock.SIGNAL)
                    == com.reiko.cars.traffic.Signal.GREEN, "the light turns green"));
        });
    }

    @GameTest(template = YARD)
    public static void trafficCamerasFine(GameTestHelper helper) {
        // A traffic light facing south, and a speed limit sign. Cameras join their level's list on the next tick.
        BlockPos lightAt = new BlockPos(7, 2, 10);
        helper.setBlock(lightAt, ModBlocks.TRAFFIC_LIGHT.get().defaultBlockState().setValue(com.reiko.cars.traffic.TrafficLightBlock.FACING, Direction.SOUTH));
        BlockPos signAt = new BlockPos(2, 1, 20);
        helper.setBlock(signAt, ModBlocks.SPEED_SIGN.get().defaultBlockState());
        helper.runAfterDelay(2, () -> camerasFine(helper, lightAt, signAt));
    }

    private static void camerasFine(GameTestHelper helper, BlockPos lightAt, BlockPos signAt) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer city = join(helper);
        ServerPlayer driver = join(helper);
        com.reiko.cars.license.LicenseService.issue(driver, "City"); // licensed: the fines here are the cameras' own
        try {
            var light = (com.reiko.cars.traffic.TrafficLightBlockEntity) helper.getBlockEntity(lightAt);
            light.setOwner(city);
            helper.setBlock(lightAt.east(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState());
            check(helper, com.reiko.cars.traffic.TrafficLightBlock.signal(helper.getLevel(), helper.getBlockState(lightAt)) == com.reiko.cars.traffic.Signal.RED, "the light is red");
            helper.assertValueEqual(light.fine(), 1500L, "a $15.00 fine");

            CarEntity car = vehicle(helper, CarType.SEDAN, 5.5, 13.0);
            car.setOwner(driver);
            driver.startRiding(car, true);
            ECO.deposit(server, driver.getUUID(), 10_000, "test");
            long cityBefore = ECO.getBalance(server, city.getUUID());
            long driverBefore = ECO.getBalance(server, driver.getUUID());
            Vec3 south = helper.absoluteVec(new Vec3(5.5, 1.0, 12.0));
            Vec3 past = helper.absoluteVec(new Vec3(5.5, 1.0, 10.0));
            com.reiko.cars.traffic.TrafficCameras.check(car, past, south, 0.5);
            helper.assertValueEqual(light.tickets(), 0, "driving away from the light isn't running it");
            com.reiko.cars.traffic.TrafficCameras.check(car, south, past, 0.5);
            helper.assertValueEqual(light.tickets(), 1, "crossing its line toward it on red is");
            helper.assertValueEqual(ECO.getBalance(server, driver.getUUID()), driverBefore - 1500, "the owner pays the fine");
            helper.assertValueEqual(ECO.getBalance(server, city.getUUID()), cityBefore + 1500, "to the light's owner");
            com.reiko.cars.traffic.TrafficCameras.check(car, south, past, 0.5);
            helper.assertValueEqual(light.tickets(), 1, "once a minute at most");
            car.lastRedLightFine = -1_000_000L;
            com.reiko.cars.traffic.TrafficCameras.check(car, helper.absoluteVec(new Vec3(20.5, 1.0, 12.0)), helper.absoluteVec(new Vec3(20.5, 1.0, 10.0)), 0.5);
            helper.assertValueEqual(light.tickets(), 1, "a road 13 blocks over isn't this light's");

            CarEntity police = vehicle(helper, CarType.POLICE_CAR, 3.5, 13.0);
            police.setOwner(driver);
            ServerPlayer officer = join(helper);
            officer.startRiding(police, true);
            police.setSiren(true);
            com.reiko.cars.traffic.TrafficCameras.check(police, south, past, 0.5);
            helper.assertValueEqual(light.tickets(), 1, "a police car with its siren on isn't fined");
            police.setSiren(false);
            police.setOwner(city);
            com.reiko.cars.traffic.TrafficCameras.check(police, south, past, 0.5);
            helper.assertValueEqual(light.tickets(), 1, "nor are the camera owner's own cars");
            officer.stopRiding();
            leave(helper, officer);

            // A speed limit sign: 30 mph, then 50 km/h.
            var sign = (com.reiko.cars.traffic.SpeedSignBlockEntity) helper.getBlockEntity(signAt);
            sign.setOwner(city);
            check(helper, com.reiko.cars.traffic.RoadSignService.set(city, sign, "20.00", "30", false).success() && sign.limit() == 30 && sign.fine() == 2000,
                    "the owner sets 30 mph and a $20.00 fine");
            check(helper, !com.reiko.cars.traffic.RoadSignService.set(driver, sign, "0", "100", false).success(), "nobody else can");
            Vec3 a = helper.absoluteVec(new Vec3(4.5, 1.0, 18.0));
            Vec3 b = helper.absoluteVec(new Vec3(4.5, 1.0, 18.6));
            long before = ECO.getBalance(server, driver.getUUID());
            com.reiko.cars.traffic.TrafficCameras.check(car, a, b, 0.6); // 27 mph
            helper.assertValueEqual(sign.tickets(), 0, "27 mph in a 30 is fine");
            com.reiko.cars.traffic.TrafficCameras.check(car, a, b, 0.9); // 40 mph
            helper.assertValueEqual(sign.tickets(), 1, "40 mph isn't");
            helper.assertValueEqual(ECO.getBalance(server, driver.getUUID()), before - 2000, "$20.00");
            car.lastSpeedingFine = -1_000_000L;
            com.reiko.cars.traffic.TrafficCameras.check(car, helper.absoluteVec(new Vec3(4.5, 1.0, 34.0)), helper.absoluteVec(new Vec3(4.5, 1.0, 34.9)), 0.9);
            helper.assertValueEqual(sign.tickets(), 1, "out of the camera's sight it isn't seen");
            com.reiko.cars.traffic.RoadSignService.set(city, sign, "20.00", "50", true);
            com.reiko.cars.traffic.TrafficCameras.check(car, a, b, 0.72); // 51.8 km/h
            helper.assertValueEqual(sign.tickets(), 1, "52 km/h in a 50 is within the 3 km/h");
            com.reiko.cars.traffic.TrafficCameras.check(car, a, b, 0.8); // 57.6 km/h
            helper.assertValueEqual(sign.tickets(), 2, "58 km/h isn't");
            ECO.withdraw(server, driver.getUUID(), ECO.getBalance(server, driver.getUUID()) - 500, "test");
            car.lastSpeedingFine = -1_000_000L;
            com.reiko.cars.traffic.TrafficCameras.check(car, a, b, 0.8);
            helper.assertValueEqual(ECO.getBalance(server, driver.getUUID()), 0L, "a driver short of the fine pays what they have");
            helper.assertValueEqual(sign.collected(), 4500L, "$45.00 collected");
            CompoundTag saved = sign.saveWithoutMetadata(helper.getLevel().registryAccess());
            check(helper, saved.getInt("Limit") == 50 && saved.getBoolean("Kmh") && saved.getInt("Tickets") == 3, "the sign keeps its limit and books");
        } finally {
            leave(helper, city, driver);
        }
        helper.succeed();
    }
    // ---- the radio ----

    @GameTest(template = YARD)
    public static void carRadiosPlayStationsAndDiscs(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        ServerPlayer stranger = join(helper);
        try {
            CarEntity car = vehicle(helper, CarType.SEDAN, 5.0, 8.0);
            int state = car.radioState();
            check(helper, !com.reiko.cars.radio.CarRadio.on(state) && com.reiko.cars.radio.CarRadio.source(state) == 0
                    && com.reiko.cars.radio.CarRadio.volume(state) == 6, "a new radio is off, on Neon FM, at 6");
            driver.startRiding(car, true);
            var menu = com.reiko.cars.radio.RadioMenu.create(1, driver.getInventory(), car);
            check(helper, menu.stillValid(driver) && !menu.stillValid(stranger), "only someone in the car works its radio");
            menu.clickMenuButton(driver, com.reiko.cars.radio.CarRadio.BUTTON_POWER);
            check(helper, com.reiko.cars.radio.CarRadio.on(car.radioState()), "power turns it on");
            menu.clickMenuButton(driver, com.reiko.cars.radio.CarRadio.BUTTON_POWER);
            menu.clickMenuButton(driver, com.reiko.cars.radio.CarRadio.BUTTON_SOURCE + 2);
            check(helper, com.reiko.cars.radio.CarRadio.on(car.radioState()) && com.reiko.cars.radio.CarRadio.source(car.radioState()) == 2,
                    "picking Dusty Roads turns it on, tuned there");
            for (int i = 0; i < 8; i++) {
                menu.clickMenuButton(driver, com.reiko.cars.radio.CarRadio.BUTTON_LOUDER);
            }
            helper.assertValueEqual(com.reiko.cars.radio.CarRadio.volume(car.radioState()), 10, "the volume tops out at 10");
            for (int i = 0; i < 12; i++) {
                menu.clickMenuButton(driver, com.reiko.cars.radio.CarRadio.BUTTON_QUIETER);
            }
            helper.assertValueEqual(com.reiko.cars.radio.CarRadio.volume(car.radioState()), 0, "and bottoms out at 0");
            check(helper, !menu.clickMenuButton(driver, 99), "other buttons do nothing");

            // The disc slot takes music discs only.
            net.minecraft.world.inventory.Slot slot = menu.getSlot(0);
            check(helper, !slot.mayPlace(new ItemStack(Items.DIAMOND)), "a diamond isn't a disc");
            check(helper, slot.mayPlace(new ItemStack(Items.MUSIC_DISC_CAT)), "a music disc is");
            driver.getInventory().add(new ItemStack(Items.MUSIC_DISC_CAT));
            int discSlot = driver.getInventory().findSlotMatchingItem(new ItemStack(Items.MUSIC_DISC_CAT));
            int menuSlot = discSlot < 9 ? 28 + discSlot : discSlot - 8;
            menu.quickMoveStack(driver, menuSlot);
            check(helper, car.radioDisc().getItem(0).is(Items.MUSIC_DISC_CAT) && car.radioDiscPlaying().is(Items.MUSIC_DISC_CAT),
                    "shift-clicking puts the disc in, and every client hears of it");
            menu.clickMenuButton(driver, com.reiko.cars.radio.CarRadio.BUTTON_SOURCE + com.reiko.cars.radio.CarRadio.DISC);
            check(helper, com.reiko.cars.radio.CarRadio.source(car.radioState()) == com.reiko.cars.radio.CarRadio.DISC, "DISC plays it");

            CompoundTag saved = new CompoundTag();
            car.saveWithoutId(saved);
            car.radioDisc().setItem(0, ItemStack.EMPTY);
            car.setRadioState(com.reiko.cars.radio.CarRadio.DEFAULT);
            car.load(saved);
            check(helper, car.radioDisc().getItem(0).is(Items.MUSIC_DISC_CAT) && com.reiko.cars.radio.CarRadio.source(car.radioState()) == com.reiko.cars.radio.CarRadio.DISC,
                    "the radio's settings and disc are saved");

            check(helper, BuiltInRegistries.SOUND_EVENT.containsKey(com.reiko.cars.radio.RadioStation.sound("neon_highway"))
                    && BuiltInRegistries.SOUND_EVENT.containsKey(com.reiko.cars.radio.RadioStation.sound("dust_on_the_dash")), "the stations' tracks are registered");

            driver.stopRiding();
            Vec3 at = car.position();
            car.kill();
            check(helper, !helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(3),
                    item -> item.getItem().is(Items.MUSIC_DISC_CAT)).isEmpty(), "breaking the car drops its disc");
        } finally {
            leave(helper, driver, stranger);
        }
        helper.succeed();
    }
    @GameTest(template = EMPTY)
    public static void radioStationsBroadcastInStep(GameTestHelper helper) {
        com.reiko.cars.radio.RadioStation neon = com.reiko.cars.radio.RadioStation.NEON;
        // Walk a whole round of the station: each track in turn, each from its start, with a gap between.
        int ticks = (int) Math.ceil(neon.roundSeconds() * 20) + 1;
        long base = 1_000_000L;
        java.util.List<Integer> order = new java.util.ArrayList<>();
        int gaps = 0;
        com.reiko.cars.radio.RadioStation.Slot last = null;
        for (int t = 0; t < ticks; t++) {
            com.reiko.cars.radio.RadioStation.Slot slot = neon.at(base + t);
            if (slot == null) {
                gaps++;
            } else {
                check(helper, slot.offsetSeconds() >= 0 && slot.offsetSeconds() < neon.playlist().get(slot.track()).seconds(), "inside its track");
                if (last == null || last.track() != slot.track() || last.round() != slot.round()) {
                    order.add(slot.track());
                }
                if (last != null && last.track() == slot.track() && last.round() == slot.round()) {
                    near(helper, slot.offsetSeconds() - last.offsetSeconds(), 0.05, "a tick plays 1/20 s");
                }
            }
            last = slot;
        }
        check(helper, order.size() >= 2 && order.contains(0) && order.contains(1), "both tracks play in a round: " + order);
        check(helper, Math.abs(gaps - 2 * com.reiko.cars.radio.RadioStation.GAP_SECONDS * 20) <= 2, "two gaps of 2 s: " + gaps);
        // Tuning in later lands mid-song, where everyone else is.
        com.reiko.cars.radio.RadioStation.Slot a = neon.at(base + 300);
        com.reiko.cars.radio.RadioStation.Slot b = neon.at(base + 300);
        check(helper, a != null && a.equals(b), "two radios on one station agree");
        check(helper, !java.util.Objects.equals(neon.at(base), com.reiko.cars.radio.RadioStation.CHILL.at(base)), "different stations play different things");

        // A disc: plays its length, pauses, plays again.
        int length = 185 * 20;
        check(helper, com.reiko.cars.radio.CarRadio.discPosition(500, 500, length) == 0, "starts at the top");
        helper.assertValueEqual(com.reiko.cars.radio.CarRadio.discPosition(500, 500 + 1200, length), 1200L, "a minute in");
        helper.assertValueEqual(com.reiko.cars.radio.CarRadio.discPosition(500, 500 + length + 10, length), -1L, "pauses after the song");
        helper.assertValueEqual(com.reiko.cars.radio.CarRadio.discPosition(500, 500 + length + com.reiko.cars.radio.CarRadio.DISC_PAUSE_TICKS, length), 0L,
                "then plays again from the top");
        helper.assertValueEqual(com.reiko.cars.radio.CarRadio.discRound(500, 500 + length + com.reiko.cars.radio.CarRadio.DISC_PAUSE_TICKS, length), 1L, "the second play");
        helper.succeed();
    }

    @GameTest(template = YARD)
    public static void radioDiscsStartWhenPlayed(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        try {
            CarEntity car = vehicle(helper, CarType.SEDAN, 5.0, 8.0);
            car.radioDisc().setItem(0, new ItemStack(Items.MUSIC_DISC_CAT));
            car.radioDisc().setChanged();
            long put = car.radioStart();
            helper.assertValueEqual(put, helper.getLevel().getGameTime(), "putting a disc in starts it");
            car.setRadioState(com.reiko.cars.radio.CarRadio.pack(false, com.reiko.cars.radio.CarRadio.DISC, 5));
            helper.runAfterDelay(5, () -> {
                driver.startRiding(car, true);
                helper.runAfterDelay(2, () -> {
                    check(helper, car.radioStart() > put, "getting in starts it over");
                    long before = car.radioStart();
                    helper.runAfterDelay(3, () -> {
                        com.reiko.cars.radio.CarRadio.press(car, com.reiko.cars.radio.CarRadio.BUTTON_LOUDER);
                        helper.assertValueEqual(car.radioStart(), before, "turning it up doesn't");
                        com.reiko.cars.radio.CarRadio.press(car, com.reiko.cars.radio.CarRadio.BUTTON_POWER);
                        check(helper, car.radioStart() > before, "switching it on does");
                        leave(helper, driver);
                        helper.succeed();
                    });
                });
            });
        } catch (RuntimeException e) {
            leave(helper, driver);
            throw e;
        }
    }
    // ---- solid trailers ----

    @GameTest(template = YARD)
    public static void trailersAreSolid(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        try {
            CarEntity semi = vehicle(helper, CarType.SEMI_TRUCK, 5.0, 12.0);
            TrailerEntity trailer = readyToHitch(helper, TrailerType.BOX_TRAILER, semi);
            CarEntity sedan = vehicle(helper, CarType.SEDAN, 2.0, 30.0);
            CarEntity otherSemi = vehicle(helper, CarType.SEMI_TRUCK, 8.0, 30.0);
            net.minecraft.world.entity.Entity[] parts = trailer.getParts();
            net.minecraft.world.entity.Entity front = java.util.Arrays.stream(parts).filter(p -> ((com.reiko.cars.trailer.TrailerPart) p).front()).findFirst().orElseThrow();
            net.minecraft.world.entity.Entity back = java.util.Arrays.stream(parts).filter(p -> !((com.reiko.cars.trailer.TrailerPart) p).front()).findFirst().orElseThrow();
            check(helper, trailer.canBeCollidedWith() && front.canBeCollidedWith() && back.canBeCollidedWith(), "a trailer is solid all along");
            check(helper, sedan.canCollideWith(trailer) && sedan.canCollideWith(front) && sedan.canCollideWith(back), "cars bump into it");
            check(helper, !otherSemi.canCollideWith(front) && !otherSemi.canCollideWith(trailer) && otherSemi.canCollideWith(back),
                    "a semi backs under the front of one on its legs, to hitch it");
            driver.startRiding(semi, true);
            Hitching.use(driver);
            check(helper, semi.trailer() == trailer, "hitched");
            check(helper, !semi.canCollideWith(front) && !semi.canCollideWith(back) && !semi.canCollideWith(trailer), "a tractor never snags on its own trailer");
            check(helper, otherSemi.canCollideWith(front), "but another semi meets a hitched one's front");

            // Walls: the trailer's body is checked a block apart down its length, a block up and higher.
            TrailerEntity.Pose here = new TrailerEntity.Pose(trailer.position(), trailer.getYRot());
            check(helper, !trailer.blockedAt(here), "clear where it stands");
            BlockPos beside = BlockPos.containing(trailer.position().add(trailer.forward().scale(-3.0))).offset(2, 0, 0);
            for (int y = 0; y < 3; y++) {
                helper.getLevel().setBlockAndUpdate(beside.above(y), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
            check(helper, !trailer.blockedAt(here), "a wall alongside doesn't touch it");
            check(helper, trailer.blockedAt(new TrailerEntity.Pose(trailer.position().add(1.5, 0.0, 0.0), trailer.getYRot())), "swung into it, it's blocked");
            BlockPos curb = BlockPos.containing(trailer.position()).offset(-1, 0, 0);
            helper.getLevel().setBlockAndUpdate(curb, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            check(helper, !trailer.blockedAt(new TrailerEntity.Pose(trailer.position().add(-0.5, 0.0, 0.0), trailer.getYRot())), "a curb a block high is no wall");
            TrailerEntity.Pose behind = trailer.poseBehind(semi.fifthWheel(), semi.getYRot());
            near(helper, behind.position().distanceTo(trailer.position()), 0.0, "standing still, it'd stay where it is");
        } finally {
            leave(helper, driver);
        }
        helper.succeed();
    }

    // ---- headlights ----

    /** The yard's headlight lights, as positions relative to the test. */
    private static List<BlockPos> glows(GameTestHelper helper) {
        List<BlockPos> found = new java.util.ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 9, 3, 33)) {
            if (helper.getBlockState(pos).getBlock() instanceof com.reiko.cars.car.HeadlightGlowBlock) {
                found.add(pos.immutable());
            }
        }
        return found;
    }

    private static int blockLight(GameTestHelper helper, int x, int y, int z) {
        return helper.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, helper.absolutePos(new BlockPos(x, y, z)));
    }

    @GameTest(template = YARD, timeoutTicks = 200)
    public static void headlightsLightTheWayInTheDark(GameTestHelper helper) {
        // A closed box over the whole yard (which stands on the ground, at y = -1): as dark as a tunnel.
        net.minecraft.world.level.block.state.BlockState stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        for (int x = 0; x <= 9; x++) {
            for (int z = 0; z <= 33; z++) {
                helper.setBlock(x, 3, z, stone);
                if (x == 0 || x == 9 || z == 0 || z == 33) {
                    for (int y = 0; y <= 2; y++) {
                        helper.setBlock(x, y, z, stone);
                    }
                }
            }
        }
        CarEntity car = vehicle(helper, CarType.SEDAN, 5.0, 8.5);
        car.setFuel(CarType.SEDAN.tankCapacity());
        ServerPlayer driver = join(helper);
        driver.startRiding(car, true);
        CarEntity[] police = new CarEntity[1];
        java.util.function.Function<Runnable, Runnable> step = body -> () -> {
            try {
                body.run();
            } catch (RuntimeException e) {
                leave(helper, driver);
                throw e;
            }
        };
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(step.apply(() -> {
                    check(helper, car.isEngineRunning(), "the engine's running");
                    check(helper, com.reiko.cars.car.Headlights.isDark(helper.getLevel(), car.blockPosition(), false), "it's dark in the box: "
                            + helper.getLevel().getBrightness(net.minecraft.world.level.LightLayer.SKY, car.blockPosition()));
                    List<BlockPos> lit = glows(helper);
                    check(helper, lit.size() == 2, "two lights, near and far: " + lit);
                    for (BlockPos pos : lit) {
                        check(helper, pos.getZ() > 11 && pos.getY() == 0 && Math.abs(pos.getX() - 5) <= 1, "on the road ahead: " + pos);
                    }
                    check(helper, lit.stream().anyMatch(pos -> pos.getZ() >= 19), "the far one well ahead: " + lit);
                    check(helper, blockLight(helper, 5, 0, 24) >= 10, "the road ahead is lit: " + blockLight(helper, 5, 0, 24));
                    check(helper, blockLight(helper, 5, 0, 2) <= 2, "behind is dark: " + blockLight(helper, 5, 0, 2));
                }))
                .thenExecute(step.apply(() -> {
                    for (int x = 1; x <= 8; x++) {
                        for (int y = 0; y <= 2; y++) {
                            helper.setBlock(x, y, 19, stone);
                        }
                    }
                }))
                .thenIdle(10)
                .thenExecute(step.apply(() -> {
                    List<BlockPos> lit = glows(helper);
                    check(helper, lit.stream().noneMatch(pos -> pos.getZ() >= 19), "no light through the wall: " + lit);
                    check(helper, lit.stream().anyMatch(pos -> pos.getZ() == 18), "the far one stops at it: " + lit);
                    check(helper, blockLight(helper, 5, 0, 21) <= 2, "dark beyond it: " + blockLight(helper, 5, 0, 21));
                    driver.stopRiding();
                }))
                .thenIdle(6)
                .thenExecute(step.apply(() -> {
                    check(helper, glows(helper).isEmpty(), "engine off, lights out");
                    // A light nobody owns (left by a car that unloaded, say) takes itself away.
                    helper.setBlock(2, 0, 30, ModBlocks.HEADLIGHT_GLOW.get().defaultBlockState());
                }))
                .thenIdle(45)
                .thenExecute(step.apply(() -> {
                    check(helper, glows(helper).isEmpty(), "a stray light goes out by itself");
                    check(helper, helper.getBlockState(new BlockPos(2, 0, 30)).isAir(), "leaving air");
                    car.discard();
                    police[0] = vehicle(helper, CarType.POLICE_CAR, 5.0, 8.5);
                    police[0].setSiren(true);
                }))
                .thenIdle(10)
                .thenExecute(step.apply(() -> {
                    List<BlockPos> lit = glows(helper);
                    check(helper, lit.size() == 1, "the beacons light up the car: " + lit);
                    BlockPos beacon = lit.getFirst();
                    check(helper, helper.getBlockState(beacon).getValue(com.reiko.cars.car.HeadlightGlowBlock.LEVEL) == 10
                            && Math.abs(beacon.getX() - 5) <= 1 && Math.abs(beacon.getZ() - 8) <= 1, "on the car: " + beacon);
                    police[0].discard();
                    check(helper, glows(helper).isEmpty(), "picked up, the light goes with it");
                    leave(helper, driver);
                }))
                .thenSucceed();
    }

    // ---- electric cars and charging stations ----

    /** A charging station owned by {@code owner}, holding {@code mj} MJ, at 5 cents per 1,000 MJ, where the pumps go. */
    private static com.reiko.cars.charge.ChargingStationBlockEntity station(GameTestHelper helper, ServerPlayer owner, long mj) {
        BlockState lower = ModBlocks.CHARGING_STATION.get().defaultBlockState()
                .setValue(com.reiko.cars.charge.ChargingStationBlock.FACING, Direction.NORTH);
        helper.setBlock(PUMP, lower);
        helper.setBlock(PUMP.above(), lower.setValue(com.reiko.cars.charge.ChargingStationBlock.HALF, DoubleBlockHalf.UPPER));
        var station = (com.reiko.cars.charge.ChargingStationBlockEntity) helper.getBlockEntity(PUMP);
        station.setOwner(owner);
        station.setPrice(5);
        station.addMj(mj);
        return station;
    }

    @GameTest(template = EMPTY)
    public static void chargingStationsTakeBuildcraftPower(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        try {
            var station = station(helper, owner, 0);
            var level = helper.getLevel();
            BlockPos lower = helper.absolutePos(PUMP);
            long mj = buildcraftreborn.api.mj.MjAPI.MJ;
            var receiver = level.getCapability(buildcraftreborn.api.mj.MjAPI.CAP_RECEIVER, lower, Direction.EAST);
            check(helper, receiver != null, "an engine sees a receiver beside it");
            check(helper, level.getCapability(buildcraftreborn.api.mj.MjAPI.CAP_RECEIVER, lower.above(), Direction.WEST) != null,
                    "beside the top half too");
            check(helper, level.getCapability(buildcraftreborn.api.mj.MjAPI.CAP_REDSTONE_RECEIVER, lower, Direction.SOUTH) != null,
                    "redstone engines can charge it too");
            check(helper, level.getCapability(buildcraftreborn.api.mj.MjAPI.CAP_CONNECTOR, lower, Direction.DOWN) != null,
                    "kinesis pipes connect to it");

            // Other mods' energy, if BuildCraft's config converts it.
            var energy = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, lower, Direction.EAST);
            if (energy != null) {
                int took = energy.receiveEnergy(1000, false);
                check(helper, took > 0 && station.storedMj() > 0, "energy goes in as MJ: took " + took + ", stored " + station.storedMj());
                check(helper, energy.extractEnergy(1000, true) == 0, "and never comes out");
            }
            long before = station.storedMj();
            helper.assertValueEqual(receiver.receivePower(5_000 * mj, false), 0L, "it takes all 5,000 MJ");
            helper.assertValueEqual(station.storedMj(), before + 5_000, "and stores it");
            helper.assertValueEqual(receiver.getPowerRequested(), (station.capacityMj() - station.storedMj()) * mj, "asks for the rest");
            long room = station.capacityMj() - station.storedMj();
            helper.assertValueEqual(receiver.receivePower((room + 2_000) * mj, false), 2_000 * mj, "hands back what doesn't fit");
            helper.assertValueEqual(station.storedMj(), station.capacityMj(), "full");
            var readable = level.getCapability(buildcraftreborn.api.mj.MjAPI.CAP_READABLE, lower, Direction.EAST);
            check(helper, readable != null && readable.getStored() == station.capacityMj() * mj, "gauges can read it");
        } finally {
            leave(helper, owner);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void chargingAnElectricCar(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer owner = join(helper);
        ServerPlayer buyer = join(helper);
        try {
            var station = station(helper, owner, 100_000);
            CarEntity ev = car(helper, CarType.CITY_EV);
            check(helper, ev.carType().isElectric(), "the city EV is electric");
            near(helper, ev.carType().tankCapacity(), 80_000, "an 80,000 MJ battery");
            check(helper, !ev.hasPart(com.reiko.cars.car.CarCondition.Part.OIL) && ev.hasPart(com.reiko.cars.car.CarCondition.Part.ENGINE),
                    "no oil to change, but a motor");
            check(helper, !CarFuel.runs(CarType.CITY_EV, new FluidStack(bc("fuel_light"), 1)), "no gasoline in it");
            check(helper, CarType.CITY_EV.fitsOnHauler() && !CarType.ELECTRIC_PICKUP.fitsOnHauler(), "small electric cars fit a hauler");
            long ownerBefore = ECO.getBalance(server, owner.getUUID());
            Purse.give(buyer, 1000); // $10.00 in cash

            ShopActionResult result = com.reiko.cars.charge.ChargerService.charge(buyer, station, ev, 20_000, 5, Payment.cash());
            check(helper, result.success(), "cash charge: " + result.message().getString());
            near(helper, ev.fuel(), 20_000, "20,000 MJ in the battery");
            helper.assertValueEqual(station.storedMj(), 80_000L, "taken from the station's store");
            helper.assertValueEqual(Purse.value(buyer), 900L, "$1.00 cash spent");
            helper.assertValueEqual(ECO.getBalance(server, owner.getUUID()), ownerBefore + 100, "owner paid $1.00");
            helper.assertValueEqual(station.mjSold(), 20_000L, "sale recorded");
            helper.assertValueEqual(station.earned(), 100L, "earnings recorded");

            check(helper, com.reiko.cars.charge.ChargerService.charge(buyer, station, ev, 1_000_000, 5, Payment.cash()).success(), "fill it up");
            near(helper, ev.fuel(), 80_000, "only what fits");
            helper.assertValueEqual(Purse.value(buyer), 600L, "60,000 MJ for $3.00");
            check(helper, !com.reiko.cars.charge.ChargerService.charge(buyer, station, ev, 1000, 5, Payment.cash()).success(),
                    "a full battery takes no more");

            station.setPrice(7);
            ev.setFuel(0);
            check(helper, !com.reiko.cars.charge.ChargerService.charge(buyer, station, ev, 1000, 5, Payment.cash()).success(),
                    "a price change is caught");
            CarEntity sedan = car(helper, CarType.SEDAN);
            ShopActionResult gas = com.reiko.cars.charge.ChargerService.charge(buyer, station, sedan, 1000, 7, Payment.cash());
            check(helper, !gas.success(), "a gasoline car can't charge");
            near(helper, sedan.fuel(), 0, "and gets nothing");

            ShopActionResult own = com.reiko.cars.charge.ChargerService.charge(owner, station, ev, 15_000, 7, Payment.cash());
            check(helper, own.success(), "the owner charges: " + own.message().getString());
            helper.assertValueEqual(station.earned(), 400L, "free: nothing earned");
            ShopActionResult last = com.reiko.cars.charge.ChargerService.charge(buyer, station, ev, 50_000, 7, Payment.cash());
            check(helper, last.success(), "the last of the store");
            near(helper, ev.fuel(), 20_000, "only what was left, 5,000 MJ");
            check(helper, !com.reiko.cars.charge.ChargerService.charge(buyer, station, ev, 1000, 7, Payment.cash()).success(),
                    "an empty station charges nothing");
        } finally {
            leave(helper, owner, buyer);
        }
        helper.succeed();
    }

    @GameTest(template = YARD)
    public static void electricCarsIdleFreeAndRegenerate(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        CarEntity ev = vehicle(helper, CarType.ELECTRIC_SEDAN, 5.0, 8.0);
        ev.setFuel(50_000);
        driver.startRiding(ev, true);
        helper.runAfterDelay(20, () -> {
            try {
                check(helper, ev.isEngineRunning(), "it's on");
                near(helper, ev.fuel(), 50_000, "no draw standing still");
                ev.applyWearReport(driver, 0.5F, 0.0F, 0.0F);
                near(helper, ev.fuel(), 50_000 + 0.5 * CarType.ELECTRIC_SEDAN.fuelPerBlock * 8, "braking puts some back");
                check(helper, ev.condition().oil() >= 1.0F, "never any oil wear");
            } catch (RuntimeException e) {
                leave(helper, driver);
                throw e;
            }
            helper.runAfterDelay(8, () -> {
                double before = ev.fuel();
                ev.applyWearReport(driver, 0.4F, 0.4F, 0.0F);
                near(helper, ev.fuel(), before, "not with the wheels locked");
                leave(helper, driver);
                helper.succeed();
            });
        });
    }

    // ---- the fire truck ----

    private static net.minecraft.world.level.block.state.BlockState stoneBlock() {
        return net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
    }

    @GameTest(template = YARD, timeoutTicks = 200)
    public static void fireTrucksTakeWater(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        CarEntity truck = vehicle(helper, CarType.FIRE_TRUCK, 5.0, 12.0);
        int capacity = com.reiko.cars.fire.FireTrucks.tankCapacity();
        check(helper, truck.carType().isFireTruck() && truck.carType().hasSiren() && truck.water() == 0, "a fire truck, with a siren, empty");
        check(helper, CarType.FIRE_TRUCK.seatCount() == 4 && CarType.FIRE_TRUCK.storageRows == 2, "a crew cab and its compartments");
        check(helper, !CarType.FIRE_TRUCK.fitsOnHauler() && !CarType.FIRE_TRUCK.towableBy(CarType.FLATBED), "too big to haul or tow");
        CarEntity sedan = vehicle(helper, CarType.SEDAN, 5.0, 28.0);
        sedan.setWater(5000);
        check(helper, sedan.water() == 0, "only a fire truck takes water");

        // By the bucket.
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        com.reiko.cars.fire.FireTrucks.pourWater(truck, player, net.minecraft.world.InteractionHand.MAIN_HAND);
        check(helper, truck.water() == 1000, "a bucket in: " + truck.water());
        check(helper, player.getMainHandItem().is(net.minecraft.world.item.Items.BUCKET), "the empty bucket back");
        check(helper, new com.reiko.cars.fire.FireTrucks.WaterTank(truck).drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty()
                && truck.water() == 1000, "nothing comes back out");
        check(helper, new com.reiko.cars.fire.FireTrucks.WaterTank(truck).fill(new FluidStack(bc("fuel_light"), 1000),
                IFluidHandler.FluidAction.EXECUTE) == 0, "no fuel in the water tank");

        // A hydrant: piped water in, none out, none of anything else.
        BlockPos hydrantAt = new BlockPos(1, 0, 12);
        helper.setBlock(hydrantAt, ModBlocks.FIRE_HYDRANT.get().defaultBlockState());
        com.reiko.cars.fire.FireHydrantBlockEntity hydrant = (com.reiko.cars.fire.FireHydrantBlockEntity) helper.getBlockEntity(hydrantAt);
        IFluidHandler pipe = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(hydrantAt), Direction.UP);
        check(helper, pipe != null, "pipes can reach it");
        helper.assertValueEqual(pipe.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 3000), IFluidHandler.FluidAction.EXECUTE),
                3000, "3 buckets piped in");
        helper.assertValueEqual(pipe.fill(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE),
                0, "no lava");
        check(helper, pipe.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "pipes can't take it back out");
        // A pond to draw from, walled in so it stays put, beyond the hydrant's reach of the second truck.
        CarEntity drafter = vehicle(helper, CarType.FIRE_TRUCK, 5.0, 27.0);
        BlockPos pond = new BlockPos(9, 0, 27);
        for (BlockPos wall : List.of(pond.north(), pond.south(), pond.west())) {
            helper.setBlock(wall, stoneBlock());
        }
        helper.setBlock(pond, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
        helper.runAfterDelay(60, () -> {
            try {
                helper.assertValueEqual(truck.water(), 4000, "the hydrant's three buckets went into the parked truck");
                check(helper, hydrant.tank().isEmpty(), "leaving it dry");
                check(helper, drafter.water() > 0 && drafter.water() < capacity, "the other draws from the pond: " + drafter.water());
                check(helper, helper.getBlockState(pond).getFluidState().isSource(), "which is still there");
                com.reiko.cars.config.CarsConfig.ENDLESS_HYDRANTS.set(true);
            } catch (RuntimeException e) {
                leave(helper, player);
                throw e;
            }
            helper.runAfterDelay(80, () -> {
                try {
                    helper.assertValueEqual(truck.water(), capacity, "an endless hydrant fills it right up");
                    helper.assertValueEqual(truck.toItem().get(com.reiko.cars.registry.ModDataComponents.WATER.get()), capacity,
                            "picked up, it keeps its water");
                    CompoundTag saved = new CompoundTag();
                    truck.saveWithoutId(saved);
                    CarEntity loaded = ModEntities.car(CarType.FIRE_TRUCK).create(helper.getLevel());
                    loaded.load(saved);
                    helper.assertValueEqual(loaded.water(), capacity, "and saved");
                } finally {
                    com.reiko.cars.config.CarsConfig.ENDLESS_HYDRANTS.set(false);
                    helper.setBlock(pond, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    leave(helper, player);
                }
                helper.succeed();
            });
        });
    }

    @GameTest(template = YARD)
    public static void fireHosesPutOutFires(GameTestHelper helper) {
        ServerPlayer firefighter = worker(helper, "firefighter");
        ServerPlayer stranger = join(helper);
        net.minecraft.world.level.GameRules.BooleanValue fireTick = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOFIRETICK);
        boolean ticked = fireTick.get();
        fireTick.set(false, helper.getLevel().getServer());
        try {
            CarEntity truck = vehicle(helper, CarType.FIRE_TRUCK, 2.0, 8.0);
            truck.setWater(5000);
            firefighter.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIRE_HOSE.get()));
            Vec3 stand = helper.absoluteVec(new Vec3(7.5, 0.0, 6.5));
            firefighter.teleportTo(helper.getLevel(), stand.x, stand.y, stand.z, 0.0F, 0.0F);
            check(helper, com.reiko.cars.fire.Firefighting.truckFor(firefighter) == truck, "the hose reaches the truck");

            // A fire on the ground ahead: aimed at, it goes out, and pays.
            BlockPos fire = new BlockPos(7, 0, 14);
            helper.setBlock(fire, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            check(helper, helper.getBlockState(fire).is(net.minecraft.world.level.block.Blocks.FIRE), "it's burning");
            firefighter.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(7.5, 1.6, 14.5)));
            com.reiko.cars.fire.Firefighting.Jet jet = com.reiko.cars.fire.Firefighting.trace(helper.getLevel(), firefighter, 1.0F);
            check(helper, jet.impact() != null && jet.impact().distanceTo(helper.absoluteVec(new Vec3(7.5, 0.0, 14.5))) < 3.0,
                    "the jet lands by it: " + jet.impact());
            long xp = jobXp(firefighter, carJob("firefighter"));
            check(helper, com.reiko.cars.fire.Firefighting.spray(firefighter), "it sprays");
            check(helper, helper.getBlockState(fire).isAir(), "the fire's out");
            helper.assertValueEqual(truck.water(), 5000 - com.reiko.cars.config.CarsConfig.get(com.reiko.cars.config.CarsConfig.HOSE_FLOW),
                    "a tick's water used");
            check(helper, jobXp(firefighter, carJob("firefighter")) > xp, "the Firefighter job pays for it");

            // One they lit themselves doesn't pay; nor does one that never goes out.
            xp = jobXp(firefighter, carJob("firefighter"));
            com.reiko.cars.fire.Firefighting.lit(firefighter, helper.absolutePos(fire));
            helper.setBlock(fire, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            com.reiko.cars.fire.Firefighting.spray(firefighter);
            check(helper, helper.getBlockState(fire).isAir(), "their own fire's out too");
            helper.assertValueEqual(jobXp(firefighter, carJob("firefighter")), xp, "but doesn't pay");
            com.reiko.cars.fire.Firefighting.forget(firefighter.getUUID());
            helper.setBlock(fire.below(), net.minecraft.world.level.block.Blocks.NETHERRACK.defaultBlockState());
            helper.setBlock(fire, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            com.reiko.cars.fire.Firefighting.spray(firefighter);
            check(helper, helper.getBlockState(fire).isAir(), "a netherrack fire goes out");
            helper.assertValueEqual(jobXp(firefighter, carJob("firefighter")), xp, "and doesn't pay either");
            helper.setBlock(fire.below(), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());

            // A lit campfire is dowsed, a burning pig put out.
            helper.setBlock(fire, net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState());
            net.minecraft.world.entity.animal.Pig pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(7, 0, 13));
            pig.setRemainingFireTicks(200);
            com.reiko.cars.fire.Firefighting.spray(firefighter);
            check(helper, !helper.getBlockState(fire).getValue(net.minecraft.world.level.block.CampfireBlock.LIT), "the campfire's out");
            check(helper, !pig.isOnFire(), "the pig's out");

            // Nothing without water, or with someone else's locked truck, or too far off.
            truck.setWater(0);
            check(helper, !com.reiko.cars.fire.Firefighting.spray(firefighter), "no water, no spray");
            check(helper, com.reiko.cars.fire.Firefighting.whyNot(firefighter).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents empty
                    && empty.getKey().equals("fire.reikocars.tank_empty"), "the tank's empty");
            truck.setWater(5000);
            truck.setOwner(stranger);
            truck.setLocked(true);
            check(helper, com.reiko.cars.fire.Firefighting.truckFor(firefighter) == null, "a locked truck isn't theirs to use");
            check(helper, ((net.minecraft.network.chat.contents.TranslatableContents) com.reiko.cars.fire.Firefighting.whyNot(firefighter).getContents())
                    .getKey().equals("fire.reikocars.truck_locked"), "it says so");
            truck.setLocked(false);
            Vec3 far = helper.absoluteVec(new Vec3(7.5, 0.0, 33.5));
            firefighter.teleportTo(helper.getLevel(), far.x, far.y, far.z, 0.0F, 0.0F);
            check(helper, com.reiko.cars.fire.Firefighting.truckFor(firefighter) == null, "the hose doesn't reach 25 blocks");
            truck.discard();
            pig.discard();
        } finally {
            fireTick.set(ticked, helper.getLevel().getServer());
            com.reiko.cars.fire.Firefighting.forget(firefighter.getUUID());
            leave(helper, firefighter, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = YARD, timeoutTicks = 400, skyAccess = true) // (the ladder reaches up out of the yard)
    public static void fireTruckLaddersGoUpAndDown(GameTestHelper helper) {
        ServerPlayer driver = join(helper);
        ServerPlayer climber = join(helper);
        CarEntity truck = vehicle(helper, CarType.FIRE_TRUCK, 5.0, 14.0);
        driver.startRiding(truck, true);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos[] at = new BlockPos[1];
        int height = com.reiko.cars.config.CarsConfig.get(com.reiko.cars.config.CarsConfig.LADDER_HEIGHT);
        java.util.function.IntPredicate ladderAt = i -> level.getBlockState(at[0].above(i)).getBlock() instanceof com.reiko.cars.fire.FireLadderBlock;
        java.util.function.Function<Runnable, Runnable> step = body -> () -> {
            try {
                body.run();
            } catch (RuntimeException e) {
                if (at[0] != null) {
                    level.setBlockAndUpdate(at[0].above(3), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(at[0].above(9), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                leave(helper, driver, climber);
                throw e;
            }
        };
        helper.startSequence()
                .thenIdle(15)
                .thenExecute(step.apply(() -> {
                    BlockPos base = com.reiko.cars.fire.FireLadder.base(truck);
                    at[0] = base;
                    check(helper, base.getX() == net.minecraft.util.Mth.floor(truck.getX()) && base.getY() == net.minecraft.util.Mth.floor(truck.getY() + 0.01)
                            && Math.abs(base.getZ() + 0.5 - (truck.getZ() - CarType.FIRE_TRUCK.length / 2.0 - 0.45)) <= 0.5,
                            "the ladder stands just behind the truck: " + base + ", truck at " + truck.position());
                    check(helper, Hitching.available(truck, driver) == Hitching.Action.LADDER_UP, "G would raise the ladder");
                    Hitching.use(driver);
                    check(helper, truck.ladderUp() && truck.ladderDeployed() && truck.ladderHeight() == height, "it's going up, " + truck.ladderHeight());
                    check(helper, !ladderAt.test(1), "not climbable till it's up");
                }))
                .thenIdle(com.reiko.cars.fire.FireLadder.RAISE_TICKS + 2)
                .thenExecute(step.apply(() -> {
                    for (int i = 0; i < height; i++) {
                        check(helper, ladderAt.test(i), "climbable " + i + " up");
                    }
                    check(helper, !ladderAt.test(height), "and no higher");
                    climber.teleportTo(level, at[0].getX() + 0.5, at[0].getY() + 2.0, at[0].getZ() + 0.5, 0.0F, 0.0F);
                    check(helper, climber.onClimbable(), "someone on it is climbing");
                    check(helper, Hitching.available(truck, driver) == Hitching.Action.LADDER_DOWN, "G would lower it");
                    CompoundTag saved = new CompoundTag();
                    truck.saveWithoutId(saved);
                    CarEntity loaded = ModEntities.car(CarType.FIRE_TRUCK).create(level);
                    loaded.load(saved);
                    check(helper, loaded.ladderUp() && loaded.ladderOut(0.0F) >= 1.0F && loaded.ladderHeight() == height, "saved with it up");
                    Hitching.use(driver);
                    check(helper, !truck.ladderUp() && truck.ladderDeployed(), "coming down");
                    check(helper, !ladderAt.test(0) && !ladderAt.test(5), "not climbable once it starts down");
                }))
                .thenIdle(com.reiko.cars.fire.FireLadder.LOWER_TICKS + 2)
                .thenExecute(step.apply(() -> {
                    check(helper, !truck.ladderDeployed(), "stowed");
                    // Something low behind: no room. Something higher: it stops short.
                    level.setBlockAndUpdate(at[0].above(3), stoneBlock());
                    Hitching.use(driver);
                    check(helper, !truck.ladderDeployed(), "no room under a low roof");
                    level.setBlockAndUpdate(at[0].above(3), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(at[0].above(9), stoneBlock());
                    Hitching.use(driver);
                    check(helper, truck.ladderUp() && truck.ladderHeight() == 9, "it stops below the overhang: " + truck.ladderHeight());
                }))
                .thenIdle(com.reiko.cars.fire.FireLadder.RAISE_TICKS + 2)
                .thenExecute(step.apply(() -> {
                    check(helper, ladderAt.test(8) && !ladderAt.test(9), "up to the overhang");
                    // Moved with the ladder up (pushed, towed): it comes down, and nothing's left behind.
                    truck.setPos(truck.getX() + 2.0, truck.getY(), truck.getZ());
                }))
                .thenIdle(3)
                .thenExecute(step.apply(() -> {
                    check(helper, !truck.ladderUp(), "moved: it comes down");
                    for (int i = 0; i < 17; i++) {
                        check(helper, !ladderAt.test(i), "the old column's gone " + i);
                    }
                    level.setBlockAndUpdate(at[0].above(9), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    // A stray rung nobody owns takes itself away.
                    level.setBlockAndUpdate(at[0].above(4), ModBlocks.FIRE_LADDER.get().defaultBlockState());
                }))
                .thenIdle(45)
                .thenExecute(step.apply(() -> {
                    check(helper, level.getBlockState(at[0].above(4)).isAir(), "a stray rung goes by itself");
                    leave(helper, driver, climber);
                }))
                .thenSucceed();
    }

    // ---- car loans, impound lots and rentals ----

    private static com.reiko.cars.impound.ImpoundOfficeBlockEntity impoundOffice(GameTestHelper helper, BlockPos pos, ServerPlayer owner, long fee) {
        helper.setBlock(pos, ModBlocks.IMPOUND_OFFICE.get().defaultBlockState());
        com.reiko.cars.impound.ImpoundOfficeBlockEntity office = (com.reiko.cars.impound.ImpoundOfficeBlockEntity) helper.getBlockEntity(pos);
        office.setOwner(owner);
        office.setFee(fee);
        return office;
    }

    @org.jetbrains.annotations.Nullable
    private static CarEntity carById(GameTestHelper helper, UUID id) {
        return helper.getLevel().getEntity(id) instanceof CarEntity car && car.isAlive() ? car : null;
    }

    @GameTest(template = YARD)
    public static void carLoansPayAndRepossess(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer dealer = join(helper);
        ServerPlayer buyer = join(helper);
        ServerPlayer city = join(helper);
        try {
            helper.setBlock(new BlockPos(5, 1, 2), ModBlocks.DEALERSHIP.get().defaultBlockState());
            DealershipBlockEntity lot = (DealershipBlockEntity) helper.getBlockEntity(new BlockPos(5, 1, 2));
            lot.setOwner(dealer);
            com.reiko.cars.impound.ImpoundOfficeBlockEntity office = impoundOffice(helper, new BlockPos(5, 1, 28), city, 5_000);
            CarEntity car = vehicle(helper, CarType.SEDAN, 5.0, 8.0);
            car.setOwner(dealer);
            check(helper, SaleService.setPrice(dealer, car, "1000.00").success(), "on sale for $1,000");
            CarEntity elsewhere = vehicle(helper, CarType.SEDAN, 5.0, 20.0);
            elsewhere.setOwner(dealer);
            SaleService.setPrice(dealer, elsewhere, "500.00");

            var menu = com.reiko.cars.sale.CarSaleMenu.create(0, buyer, car);
            var terms = menu.terms();
            check(helper, terms != null && terms.down() == 20_000 && terms.total() == 88_000 && terms.installment() == 8_800 && terms.days() == 10,
                    "20% down, the rest plus 10% over 10 days: " + terms);
            check(helper, com.reiko.cars.sale.CarSaleMenu.create(0, buyer, elsewhere).terms() == null, "only on a dealership's lot");

            buyer.moveTo(car.getX() + 2, car.getY(), car.getZ());
            Purse.give(buyer, 30_000);
            long dealerBefore = ECO.getBalance(server, dealer.getUUID());
            ShopActionResult bought = SaleService.buy(buyer, car, 100_000, Payment.cash(), true);
            check(helper, bought.success(), "bought on finance: " + bought.message().getString());
            helper.assertValueEqual(Purse.value(buyer), 10_000L, "the $200 down payment in cash");
            helper.assertValueEqual(ECO.getBalance(server, dealer.getUUID()), dealerBefore + 20_000, "to the dealer");
            check(helper, car.isOwner(buyer) && car.loanId() != null, "the car's theirs, on a loan");
            helper.assertValueEqual(com.reiko.cars.finance.LoanService.owed(car), 88_000L, "$880 owed");
            check(helper, !SaleService.setPrice(buyer, car, "2000.00").success(), "it can't be sold till it's paid off");
            car.hurt(buyer.damageSources().playerAttack(buyer), 50.0F);
            check(helper, car.isAlive(), "nor picked up");
            UUID loanId = car.loanId();

            // A day's payment, from the bank to the dealer.
            ECO.setBalance(server, buyer.getUUID(), 50_000, "test");
            dealerBefore = ECO.getBalance(server, dealer.getUUID());
            com.reiko.cars.finance.LoanService.dueNow(server, loanId);
            com.reiko.cars.finance.LoanService.tick(server);
            helper.assertValueEqual(ECO.getBalance(server, buyer.getUUID()), 41_200L, "$88 from the bank");
            helper.assertValueEqual(ECO.getBalance(server, dealer.getUUID()), dealerBefore + 8_800, "to the dealer");
            helper.assertValueEqual(com.reiko.cars.finance.LoanService.owed(car), 79_200L, "$792 left");

            // Three missed in a row: repossessed, to the impound lot.
            ECO.setBalance(server, buyer.getUUID(), 0, "test");
            for (int i = 1; i <= 3; i++) {
                com.reiko.cars.finance.LoanService.dueNow(server, loanId);
                com.reiko.cars.finance.LoanService.tick(server);
                if (i < 3) {
                    check(helper, car.isAlive() && com.reiko.cars.finance.LoanData.get(server).get(loanId).missed() == i, "missed " + i);
                }
            }
            check(helper, car.isRemoved(), "repossessed: taken away");
            CarEntity held = carById(helper, car.getUUID());
            check(helper, held != null && held.isImpounded() && held.impound().repossessed(), "it's at the impound lot");
            check(helper, Math.abs(held.getZ() - helper.absoluteVec(new Vec3(5.5, 1, 28.5)).z) <= 10, "by the office: " + held.position());
            helper.assertValueEqual(held.impound().arrears(), 26_400L, "three missed payments owed");
            check(helper, !held.canBeUsedBy(buyer) && held.canBeUsedBy(city), "only the lot's owner gets in");
            check(helper, !com.reiko.cars.impound.ImpoundService.releaseFree(city, office, held).success(), "not let go free: the dealer's owed");

            // Paying up gets it back, and the loan's back on track.
            var screen = com.reiko.cars.impound.ImpoundMenu.create(0, buyer, office);
            helper.assertValueEqual(screen.cost(), 31_400L, "the fee and the missed payments");
            Purse.give(buyer, 31_400);
            long cityBefore = ECO.getBalance(server, city.getUUID());
            dealerBefore = ECO.getBalance(server, dealer.getUUID());
            ShopActionResult released = com.reiko.cars.impound.ImpoundService.release(buyer, office, held, 31_400, Payment.cash());
            check(helper, released.success(), "paying: " + released.message().getString());
            helper.assertValueEqual(ECO.getBalance(server, city.getUUID()), cityBefore + 5_000, "the fee to the lot");
            helper.assertValueEqual(ECO.getBalance(server, dealer.getUUID()), dealerBefore + 26_400, "the missed payments to the dealer");
            check(helper, !held.isImpounded() && held.canBeUsedBy(buyer), "the car's theirs again");
            helper.assertValueEqual(com.reiko.cars.finance.LoanService.owed(held), 52_800L, "less what was caught up");
            check(helper, com.reiko.cars.finance.LoanData.get(server).get(loanId).state() == com.reiko.cars.finance.Loan.State.ACTIVE, "active again");

            // Not got back in time: the dealer's.
            for (int i = 1; i <= 3; i++) {
                com.reiko.cars.finance.LoanService.dueNow(server, loanId);
                com.reiko.cars.finance.LoanService.tick(server);
            }
            CarEntity again = carById(helper, held.getUUID());
            check(helper, again != null && again.isImpounded(), "repossessed again");
            com.reiko.cars.finance.LoanService.dueNow(server, loanId);
            com.reiko.cars.finance.LoanService.tick(server);
            check(helper, again.isOwner(dealer) && !again.isImpounded() && again.loanId() == null, "foreclosed: the dealer's, free of the lot");
            check(helper, com.reiko.cars.finance.LoanData.get(server).get(loanId) == null, "and the loan's done");
            check(helper, !CarKeys.holds(buyer, again.lockCode()), "on new locks");

            // Paying one off early.
            CarEntity second = vehicle(helper, CarType.HATCHBACK, 7.0, 6.0);
            second.setOwner(dealer);
            SaleService.setPrice(dealer, second, "100.00");
            Purse.give(buyer, 2_000);
            buyer.moveTo(second.getX() + 2, second.getY(), second.getZ());
            check(helper, SaleService.buy(buyer, second, 10_000, Payment.cash(), true).success(), "a second car on finance");
            ECO.setBalance(server, buyer.getUUID(), 100_000, "test");
            ShopActionResult paidOff = com.reiko.cars.finance.LoanService.payOff(buyer, second);
            check(helper, paidOff.success() && second.loanId() == null, "paid off early: " + paidOff.message().getString());
            helper.assertValueEqual(ECO.getBalance(server, buyer.getUUID()), 100_000L - 8_800, "the $88 owed, from the bank");
        } finally {
            leave(helper, dealer, buyer, city);
        }
        helper.succeed();
    }

    @GameTest(template = YARD, timeoutTicks = 200)
    public static void impoundLotsHoldBadlyParkedCars(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer council = join(helper);
        ServerPlayer driver = join(helper);
        ServerPlayer towie = worker(helper, "mechanic");
        com.reiko.cars.impound.ImpoundOfficeBlockEntity office = impoundOffice(helper, new BlockPos(2, 1, 24), council, 5_000);
        CarEntity truck = vehicle(helper, CarType.FLATBED, 5.0, 16.0);
        CarEntity car = behind(helper, truck, CarType.SEDAN, 0.3);
        car.setOwner(driver);
        car.setLocked(true);
        Vec3 carAt = helper.relativeVec(car.position());
        ParkingMeterBlockEntity meter = meter(helper, new BlockPos((int) Math.floor(carAt.x), 1, (int) Math.floor(carAt.z) - 4), council);
        towie.startRiding(truck, true);
        helper.runAfterDelay(2, () -> {
            try {
                check(helper, Towing.towable(truck, towie) == null, "a stranger's locked car can't be towed");
                long now = helper.getLevel().getGameTime();
                meter.update(now);
                meter.update(now + 1_300);
                check(helper, meter.isExpired() && com.reiko.cars.impound.ImpoundService.badlyParked(car),
                        "left at an expired meter: " + MeterService.parkedAt(meter) + " / " + car + " / " + MeterService.nearestMeter(car) + " at " + meter.getBlockPos()
                                + " exp " + meter.isExpired());
                check(helper, Towing.towable(truck, towie) == car, "so a tow truck can take it, locked or not");
                Hitching.use(towie);
                check(helper, car.getVehicle() == truck, "on the flatbed");
                // Off to the impound lot, and down.
                Vec3 lotSpot = helper.absoluteVec(new Vec3(6.0, 1.0, 27.0));
                truck.setPos(lotSpot.x, truck.getY(), lotSpot.z);
                truck.positionRider(car);
                long xp = jobXp(towie, carJob("mechanic"));
                Hitching.use(towie);
                check(helper, !car.isPassenger() && car.isImpounded(), "dropped on the lot: impounded, at " + helper.relativeVec(car.position()));
                check(helper, jobXp(towie, carJob("mechanic")) > xp, "and the tow pays the Mechanic job");
                check(helper, !car.canBeUsedBy(driver) && !car.canManage(driver), "its owner can't get in or pick it up");
                car.hurt(driver.damageSources().playerAttack(driver), 50.0F);
                check(helper, car.isAlive(), "it can't be broken");
                check(helper, car.canBeUsedBy(council), "the lot's owner can move it");

                var screen = com.reiko.cars.impound.ImpoundMenu.create(0, driver, office);
                check(helper, screen.carCount() == 1 && screen.cost() == 5_000 && !screen.repossessed(), "the owner sees it and the $50 fee");
                check(helper, com.reiko.cars.impound.ImpoundMenu.create(0, towie, office).carCount() == 0, "nobody else sees it");
                check(helper, !com.reiko.cars.impound.ImpoundService.release(driver, office, car, 4_000, Payment.cash()).success(),
                        "an out-of-date cost is caught");
                Purse.give(driver, 5_000);
                long councilBefore = ECO.getBalance(server, council.getUUID());
                ShopActionResult paid = com.reiko.cars.impound.ImpoundService.release(driver, office, car, 5_000, Payment.cash());
                check(helper, paid.success(), "paying: " + paid.message().getString());
                helper.assertValueEqual(ECO.getBalance(server, council.getUUID()), councilBefore + 5_000, "the fee to the lot's owner");
                check(helper, !car.isImpounded() && car.canBeUsedBy(driver), "released");
                check(helper, office.impounded() == 1 && office.released() == 1 && office.earned() == 5_000, "on the office's books");

                // The lot's owner can let one go; a broken office lets them all go.
                com.reiko.cars.impound.ImpoundService.impound(car, office, com.reiko.cars.impound.Impound.PARKING, 0, null);
                check(helper, !com.reiko.cars.impound.ImpoundService.releaseFree(driver, office, car).success(), "not the car's owner");
                check(helper, com.reiko.cars.impound.ImpoundService.releaseFree(council, office, car).success() && !car.isImpounded(), "the lot's owner");
                com.reiko.cars.impound.ImpoundService.impound(car, office, com.reiko.cars.impound.Impound.PARKING, 0, null);
                helper.setBlock(new BlockPos(2, 1, 24), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                check(helper, !car.isImpounded(), "the office broken, the car's free");
            } finally {
                leave(helper, council, driver, towie);
            }
            helper.succeed();
        });
    }

    @GameTest(template = YARD, timeoutTicks = 200)
    public static void carRentalsGoOutAndComeBack(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer agency = join(helper);
        ServerPlayer customer = join(helper);
        ServerPlayer stranger = join(helper);
        helper.setBlock(new BlockPos(5, 1, 2), ModBlocks.RENTAL_DESK.get().defaultBlockState());
        com.reiko.cars.rental.RentalDeskBlockEntity desk = (com.reiko.cars.rental.RentalDeskBlockEntity) helper.getBlockEntity(new BlockPos(5, 1, 2));
        desk.setOwner(agency);
        CarEntity sedan = vehicle(helper, CarType.SEDAN, 3.0, 7.0);
        sedan.setOwner(agency);
        sedan.setLocked(true);
        CarEntity hatch = vehicle(helper, CarType.HATCHBACK, 7.5, 8.0);
        hatch.setOwner(agency);
        hatch.setLocked(true);
        CarEntity notTheirs = vehicle(helper, CarType.SEDAN, 5.0, 11.0);
        notTheirs.setOwner(stranger);
        CarEntity far = vehicle(helper, CarType.SEDAN, 5.0, 30.0);
        far.setOwner(agency);
        java.util.function.Function<Runnable, Runnable> step = body -> () -> {
            try {
                body.run();
            } catch (RuntimeException e) {
                leave(helper, agency, customer, stranger);
                throw e;
            }
        };
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(step.apply(() -> {
                    check(helper, com.reiko.cars.rental.RentalService.fleet(desk).size() == 2, "the agency's two cars on the lot are the fleet");
                    check(helper, !com.reiko.cars.rental.RentalService.setPrice(customer, desk, CarType.SEDAN, "1.00").success(), "only the owner prices");
                    check(helper, com.reiko.cars.rental.RentalService.setPrice(agency, desk, CarType.SEDAN, "20.00").success(), "$20 a day for a sedan");
                    var screen = com.reiko.cars.rental.RentalMenu.create(0, customer, desk);
                    helper.assertValueEqual(screen.carCount(), 2, "the customer sees both");
                    Purse.give(customer, 10_000);
                    long agencyBefore = ECO.getBalance(server, agency.getUUID());
                    check(helper, !com.reiko.cars.rental.RentalService.rent(customer, desk, sedan, 2, 3_000, Payment.cash()).success(), "a stale price is caught");
                    ShopActionResult rented = com.reiko.cars.rental.RentalService.rent(customer, desk, sedan, 2, 4_000, Payment.cash());
                    check(helper, rented.success(), "renting for two days: " + rented.message().getString());
                    helper.assertValueEqual(Purse.value(customer), 6_000L, "$40 in cash");
                    helper.assertValueEqual(ECO.getBalance(server, agency.getUUID()), agencyBefore + 4_000, "to the agency");
                    check(helper, sedan.rentedBy(customer) && sedan.canBeUsedBy(customer) && !sedan.canBeUsedBy(stranger), "theirs to open, nobody else's");
                    customer.startRiding(sedan, true);
                    check(helper, sedan.checkKey(), "and to start, without a key");
                    check(helper, com.reiko.cars.rental.RentalService.fleet(desk).equals(List.of(hatch)), "out of the fleet while it's rented");
                    check(helper, !SaleService.setPrice(agency, sedan, "10.00").success(), "not for sale while it's out");
                    customer.stopRiding();
                    // Driven off the lot...
                    Vec3 away = helper.absoluteVec(new Vec3(5.0, 1.0, 26.0));
                    sedan.setPos(away.x, sedan.getY(), away.z);
                    com.reiko.cars.rental.RentalService.carTick(sedan);
                    check(helper, sedan.rental() != null && sedan.rental().leftLot(), "it's left the lot");
                    // ...and back again, parked and empty: done early.
                    Vec3 back = helper.absoluteVec(new Vec3(3.0, 1.0, 7.0));
                    sedan.setPos(back.x, sedan.getY(), back.z);
                }))
                .thenIdle(14)
                .thenExecute(step.apply(() -> {
                    com.reiko.cars.rental.RentalService.carTick(sedan);
                    check(helper, sedan.rental() == null && !sedan.rentedBy(customer), "brought back early, the rental's over");

                    // Out of time, left far off: it goes back to its spot by itself.
                    com.reiko.cars.rental.RentalService.rent(customer, desk, hatch, 1, 1_500, Payment.cash());
                    check(helper, hatch.rentedBy(customer), "the hatchback's rented");
                    Vec3 home = hatch.position();
                    Vec3 away = helper.absoluteVec(new Vec3(6.0, 1.0, 28.0));
                    hatch.setPos(away.x, hatch.getY(), away.z);
                    var rental = hatch.rental();
                    long now = helper.getLevel().getGameTime();
                    hatch.setRental(new com.reiko.cars.rental.Rental(rental.renter(), rental.renterName(), rental.desk(), rental.home(),
                            rental.homeYaw(), now - 1, true));
                    customer.startRiding(hatch, true);
                    com.reiko.cars.rental.RentalService.carTick(hatch);
                    check(helper, !hatch.checkKey(), "time's up: it won't start for them");
                    check(helper, hatch.isAlive(), "and it waits while they're in it");
                    customer.stopRiding();
                    UUID id = hatch.getUUID();
                    com.reiko.cars.rental.RentalService.carTick(hatch);
                    check(helper, hatch.isRemoved(), "left alone, it's gone");
                    CarEntity returned = carById(helper, id);
                    check(helper, returned != null && returned.position().distanceTo(home) < 2.0 && returned.rental() == null,
                            "back in its spot on the lot: " + (returned == null ? null : returned.position()) + " vs " + home);
                    check(helper, returned.isOwner(agency) && com.reiko.cars.rental.RentalService.fleet(desk).contains(returned), "back in the fleet");
                    helper.assertValueEqual(com.reiko.cars.car.CarDeliveries.get(server).pending(), 0, "nothing left on its way");
                    leave(helper, agency, customer, stranger);
                }))
                .thenSucceed();
    }

    // ---- license plates, driver's licenses and the DMV ----

    private static com.reiko.cars.dmv.DmvOfficeBlockEntity dmvAt(GameTestHelper helper, BlockPos pos, ServerPlayer owner) {
        helper.setBlock(pos, ModBlocks.DMV_OFFICE.get().defaultBlockState());
        com.reiko.cars.dmv.DmvOfficeBlockEntity dmv = (com.reiko.cars.dmv.DmvOfficeBlockEntity) helper.getBlockEntity(pos);
        dmv.setOwner(owner);
        return dmv;
    }

    private static BlockPos cone(GameTestHelper helper, BlockPos pos, ServerPlayer owner, int number) {
        helper.setBlock(pos, ModBlocks.TRAFFIC_CONE.get().defaultBlockState());
        com.reiko.cars.road.TrafficConeBlockEntity cone = (com.reiko.cars.road.TrafficConeBlockEntity) helper.getBlockEntity(pos);
        cone.setOwner(owner);
        cone.setNumber(number);
        return helper.absolutePos(pos);
    }

    private static int cards(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.DRIVERS_LICENSE.get())) {
                count += player.getInventory().getItem(i).getCount();
            }
        }
        return count;
    }

    @GameTest(template = YARD, timeoutTicks = 100)
    public static void licensePlatesOnEveryCar(GameTestHelper helper) {
        CarEntity sedan = vehicle(helper, CarType.SEDAN, 3.0, 6.0);
        CarEntity bike = vehicle(helper, CarType.SPORT_BIKE, 7.0, 6.0);
        helper.runAfterDelay(2, () -> {
            check(helper, sedan.plate().matches("[A-HJ-NPR-Z]{3} [0-9]{3}"), "a standard plate: " + sedan.plate());
            check(helper, !bike.plate().isEmpty() && !bike.plate().equals(sedan.plate()), "every car its own: " + bike.plate());
            var registry = com.reiko.cars.plate.PlateRegistry.get(helper.getLevel().getServer());
            check(helper, registry.isTaken(sedan.plate()) && registry.isTaken(bike.plate()), "both on the books");
            check(helper, sedan.describe().getString().contains(sedan.plate()), "tickets name the plate: " + sedan.describe().getString());
            net.minecraft.world.item.ItemStack item = sedan.toItem();
            helper.assertValueEqual(item.get(com.reiko.cars.registry.ModDataComponents.PLATE.get()), sedan.plate(), "kept on the item");
            helper.assertValueEqual(com.reiko.cars.plate.Plates.clean("  reiko   1 "), "REIKO 1", "custom plates tidied up");
            check(helper, com.reiko.cars.plate.Plates.clean("A") == null && com.reiko.cars.plate.Plates.clean("TOOLONGXX") == null
                    && com.reiko.cars.plate.Plates.clean("AB-12") == null, "and bad ones refused");
            helper.succeed();
        });
    }

    @GameTest(template = YARD, timeoutTicks = 200)
    public static void dmvDrivingTestAndPlates(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer city = join(helper);
        ServerPlayer learner = join(helper);
        ServerPlayer stranger = join(helper);
        try {
            com.reiko.cars.dmv.DmvOfficeBlockEntity dmv = dmvAt(helper, new BlockPos(2, 1, 2), city);
            check(helper, com.reiko.cars.dmv.DmvService.route(dmv).isEmpty(), "no cones, no route");
            Purse.give(learner, 5_000);
            check(helper, !com.reiko.cars.dmv.DmvService.takeTest(learner, dmv, 2_500, Payment.cash()).success(), "no test without a route");
            BlockPos second = cone(helper, new BlockPos(8, 1, 24), city, 2);
            BlockPos first = cone(helper, new BlockPos(3, 1, 18), city, 1);
            cone(helper, new BlockPos(6, 1, 12), stranger, 1);
            cone(helper, new BlockPos(9, 1, 12), city, 0);
            helper.assertValueEqual(com.reiko.cars.dmv.DmvService.route(dmv), List.of(first, second), "the owner's numbered cones, in order");

            long cityBefore = ECO.getBalance(server, city.getUUID());
            check(helper, !com.reiko.cars.dmv.DmvService.takeTest(learner, dmv, 2_000, Payment.cash()).success(), "a stale price is caught");
            ShopActionResult paid = com.reiko.cars.dmv.DmvService.takeTest(learner, dmv, 2_500, Payment.cash());
            check(helper, paid.success(), "paying for the test: " + paid.message().getString());
            helper.assertValueEqual(ECO.getBalance(server, city.getUUID()), cityBefore + 2_500, "the fee to the DMV's owner");
            check(helper, com.reiko.cars.dmv.DrivingTests.taking(learner), "the test's on");

            CarEntity car = vehicle(helper, CarType.SEDAN, 5.0, 8.0);
            learner.startRiding(car, true);
            com.reiko.cars.dmv.DrivingTests.tick(server, true);
            helper.assertValueEqual(com.reiko.cars.dmv.DrivingTests.progress(learner), 0, "in a car, no cones yet");
            Vec3 at = helper.absoluteVec(new Vec3(4.5, 1.0, 19.0));
            car.setPos(at.x, car.getY(), at.z);
            com.reiko.cars.dmv.DrivingTests.tick(server, true);
            helper.assertValueEqual(com.reiko.cars.dmv.DrivingTests.progress(learner), 1, "through cone 1");
            at = helper.absoluteVec(new Vec3(8.5, 1.0, 22.0));
            car.setPos(at.x, car.getY(), at.z);
            com.reiko.cars.dmv.DrivingTests.tick(server, true);
            check(helper, !com.reiko.cars.dmv.DrivingTests.taking(learner), "through cone 2: done");
            check(helper, com.reiko.cars.license.LicenseService.valid(learner), "passed: a license");
            helper.assertValueEqual(cards(learner), 1, "and the card");
            check(helper, dmv.tests() == 1 && dmv.passes() == 1, "on the DMV's books");
            check(helper, !com.reiko.cars.dmv.DmvService.takeTest(learner, dmv, 2_500, Payment.cash()).success(), "no second test with a license");
            learner.stopRiding();

            // A new card, and a custom plate.
            check(helper, com.reiko.cars.dmv.DmvService.replace(learner, dmv, 1_000, Payment.cash()).success(), "a new card");
            at = helper.absoluteVec(new Vec3(5.0, 1.0, 8.0));
            car.setPos(at.x, car.getY(), at.z); // parked back by the DMV
            helper.assertValueEqual(cards(learner), 2, "two cards now");
            car.setOwner(learner);
            Purse.give(learner, 20_000);
            check(helper, !com.reiko.cars.dmv.DmvService.buyPlate(learner, dmv, car, "NO-WAY", 10_000, Payment.cash()).success(), "a bad plate");
            String old = car.plate();
            String custom = "t " + (100_000 + helper.getLevel().getRandom().nextInt(900_000)); // the server's plates are kept between runs
            String wanted = custom.toUpperCase(java.util.Locale.ROOT);
            ShopActionResult plate = com.reiko.cars.dmv.DmvService.buyPlate(learner, dmv, car, custom, 10_000, Payment.cash());
            check(helper, plate.success(), "a custom plate: " + plate.message().getString());
            helper.assertValueEqual(car.plate(), wanted, "on the car");
            var plates = com.reiko.cars.plate.PlateRegistry.get(server);
            check(helper, plates.isTaken(wanted) && !plates.isTaken(old), "the old one freed");
            CarEntity other = vehicle(helper, CarType.HATCHBACK, 8.0, 6.0);
            other.setOwner(learner);
            check(helper, !com.reiko.cars.dmv.DmvService.buyPlate(learner, dmv, other, wanted, 10_000, Payment.cash()).success(), "one car to a plate");
            check(helper, com.reiko.cars.dmv.DmvService.price(dmv, city, com.reiko.cars.dmv.DmvOfficeBlockEntity.PLATE_FEE) == 0, "free for the owner");
            check(helper, com.reiko.cars.dmv.DmvService.setFee(city, dmv, 0, "40.00").success() && dmv.fee(0) == 4_000, "the owner sets the fee");
            check(helper, !com.reiko.cars.dmv.DmvService.setFee(learner, dmv, 0, "1.00").success(), "nobody else");

            // Failing: crashing, and getting out.
            Purse.give(stranger, 10_000);
            check(helper, com.reiko.cars.dmv.DmvService.takeTest(stranger, dmv, 4_000, Payment.cash()).success(), "another test");
            CarEntity crash = vehicle(helper, CarType.SEDAN, 12.0, 8.0);
            stranger.startRiding(crash, true);
            com.reiko.cars.dmv.DrivingTests.tick(server, false);
            CarCondition c = crash.condition();
            crash.setCondition(new CarCondition(c.oil(), c.engine(), c.tires(), c.brakes(), c.body() - 0.05F));
            com.reiko.cars.dmv.DrivingTests.tick(server, false);
            check(helper, !com.reiko.cars.dmv.DrivingTests.taking(stranger) && !com.reiko.cars.license.LicenseService.valid(stranger), "a crash fails it");
            check(helper, com.reiko.cars.dmv.DmvService.takeTest(stranger, dmv, 4_000, Payment.cash()).success(), "and again");
            com.reiko.cars.dmv.DrivingTests.tick(server, false);
            stranger.stopRiding();
            com.reiko.cars.dmv.DrivingTests.tick(server, false);
            check(helper, !com.reiko.cars.dmv.DrivingTests.taking(stranger), "getting out fails it");
        } finally {
            com.reiko.cars.dmv.DrivingTests.cancel(learner);
            com.reiko.cars.dmv.DrivingTests.cancel(stranger);
            leave(helper, city, learner, stranger);
        }
        helper.succeed();
    }

    @GameTest(template = YARD, timeoutTicks = 100)
    public static void licensePointsSuspensionAndTickets(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer driver = join(helper);
        ServerPlayer nobody = join(helper);
        ServerPlayer camera = join(helper);
        ServerPlayer officer = worker(helper, "police_officer");
        ServerPlayer civilian = join(helper);
        try {
            var license = com.reiko.cars.license.LicenseService.issue(driver, "City");
            check(helper, license.number().matches("D \\d{3}-\\d{3}"), "a license number: " + license.number());
            CarEntity car = vehicle(helper, CarType.SEDAN, 4.0, 8.0); // nobody's: no key needed
            driver.startRiding(car, true);

            // A camera puts points on the driver's license...
            com.reiko.cars.license.LicenseService.cameraCaught(car, camera.getUUID(), true);
            com.reiko.cars.license.LicenseService.cameraCaught(car, camera.getUUID(), false);
            helper.assertValueEqual(com.reiko.cars.license.LicenseService.points(server, license), 5, "3 for a red light, 2 for speeding");
            // ...and enough suspends it.
            com.reiko.cars.license.LicenseService.addPoints(server, driver.getUUID(), 4, "license.reikocars.reason.speeding");
            check(helper, com.reiko.cars.license.LicenseService.valid(driver), "9 of 12: still good");
            com.reiko.cars.license.LicenseService.addPoints(server, driver.getUUID(), 3, "license.reikocars.reason.red_light");
            helper.assertValueEqual(com.reiko.cars.license.LicenseService.status(server, driver.getUUID()),
                    com.reiko.cars.license.LicenseService.Status.SUSPENDED, "12: suspended");
            helper.assertValueEqual(com.reiko.cars.license.LicenseService.points(server, license), 0, "the points wiped");
            var card = driver.getInventory().items.stream().filter(stack -> stack.is(ModItems.DRIVERS_LICENSE.get())).findFirst().orElseThrow()
                    .get(com.reiko.cars.registry.ModDataComponents.LICENSE.get());
            check(helper, card.suspendedUntil() > helper.getLevel().getGameTime(), "the card says so");

            // Without a valid license, a camera adds a fine.
            ECO.setBalance(server, driver.getUUID(), 10_000, "test");
            long cameraBefore = ECO.getBalance(server, camera.getUUID());
            com.reiko.cars.license.LicenseService.cameraCaught(car, camera.getUUID(), false);
            helper.assertValueEqual(ECO.getBalance(server, driver.getUUID()), 7_500L, "$25 for driving suspended");
            helper.assertValueEqual(ECO.getBalance(server, camera.getUUID()), cameraBefore + 2_500, "to the camera's owner");

            // Licenses required: no driving without one.
            check(helper, car.checkKey() && !car.unlicensed(), "not required: anyone drives");
            com.reiko.cars.config.CarsConfig.LICENSE_REQUIRED.set(true);
            try {
                check(helper, !car.checkKey() && car.unlicensed(), "required: a suspended license can't");
                com.reiko.cars.license.LicenseService.clear(server, driver.getUUID());
                check(helper, car.checkKey() && !car.unlicensed(), "a good one can");
            } finally {
                com.reiko.cars.config.CarsConfig.LICENSE_REQUIRED.set(false);
            }
            car.checkKey();

            // The police: a plate check tickets an unlicensed driver.
            driver.stopRiding();
            nobody.startRiding(car, true);
            ECO.setBalance(server, nobody.getUUID(), 10_000, "test");
            com.reiko.cars.license.TicketService.forget();
            com.reiko.cars.license.TicketService.checkCar(civilian, car);
            helper.assertValueEqual(ECO.getBalance(server, nobody.getUUID()), 10_000L, "only police can");
            long xp = jobXp(officer, carJob("police_officer"));
            com.reiko.cars.license.TicketService.checkCar(officer, car);
            helper.assertValueEqual(ECO.getBalance(server, nobody.getUUID()), 7_500L, "ticketed $25");
            check(helper, jobXp(officer, carJob("police_officer")) > xp, "paid as a Police Officer");
            com.reiko.cars.license.TicketService.checkCar(officer, car);
            helper.assertValueEqual(ECO.getBalance(server, nobody.getUUID()), 7_500L, "not twice in ten minutes");
            nobody.stopRiding();
            driver.startRiding(car, true);
            ECO.setBalance(server, driver.getUUID(), 10_000, "test");
            com.reiko.cars.license.TicketService.checkCar(officer, car);
            helper.assertValueEqual(ECO.getBalance(server, driver.getUUID()), 10_000L, "a licensed driver goes on their way");
        } finally {
            com.reiko.cars.config.CarsConfig.LICENSE_REQUIRED.set(false);
            com.reiko.cars.license.LicenseData.get(server).revoke(driver.getUUID());
            leave(helper, driver, nobody, camera, officer, civilian);
        }
        helper.succeed();
    }

    // ---- tuning and racing ----

    private static void giveKit(ServerPlayer player, com.reiko.cars.tuning.TuningKit kit, int count) {
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.kit(kit), count));
    }

    @GameTest(template = YARD, timeoutTicks = 100)
    public static void tuningBenchUpgradesCars(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer shop = join(helper);
        ServerPlayer driver = join(helper);
        ServerPlayer stranger = join(helper);
        try {
            com.reiko.cars.tuning.Upgrades stock = com.reiko.cars.tuning.Upgrades.STOCK;
            check(helper, stock.topSpeed() == 1.0 && stock.acceleration() == 1.0 && stock.fuelUse() == 1.0 && stock.offRoad(0.6) == 0.6, "stock is stock");
            var tuned = stock.withEngine(3).withTurbo(true).withTires(com.reiko.cars.tuning.Upgrades.Tires.OFF_ROAD);
            near(helper, tuned.topSpeed(), 1.18 * 0.97, "stage 3 and off-road tires");
            near(helper, tuned.acceleration(), 1.12 * 1.25, "with a turbo");
            near(helper, tuned.fuelUse(), 1.34, "thirstier");
            near(helper, tuned.offRoad(0.6), 0.84, "off-road tires keep more speed off-road");
            helper.assertValueEqual(com.reiko.cars.tuning.Upgrades.unpack(tuned.pack()), tuned, "packs and unpacks");

            helper.setBlock(new BlockPos(3, 1, 2), ModBlocks.TUNING_BENCH.get().defaultBlockState());
            var bench = (com.reiko.cars.tuning.TuningBenchBlockEntity) helper.getBlockEntity(new BlockPos(3, 1, 2));
            bench.setOwner(shop);
            CarEntity car = vehicle(helper, CarType.SEDAN, 4.0, 6.0);
            car.setOwner(driver);

            // Your own kits, fitted free.
            giveKit(driver, com.reiko.cars.tuning.TuningKit.ENGINE, 1);
            giveKit(driver, com.reiko.cars.tuning.TuningKit.SPORT_TIRES, 1);
            giveKit(driver, com.reiko.cars.tuning.TuningKit.OFF_ROAD_TIRES, 1);
            check(helper, !com.reiko.cars.tuning.TuningService.fitOwn(stranger, bench, car, com.reiko.cars.tuning.TuningKit.ENGINE).success(),
                    "only its owner tunes it");
            check(helper, com.reiko.cars.tuning.TuningService.fitOwn(driver, bench, car, com.reiko.cars.tuning.TuningKit.ENGINE).success(), "a stage");
            helper.assertValueEqual(car.upgrades().engine(), 1, "stage 1");
            helper.assertValueEqual(com.reiko.cars.tuning.TuningService.carried(driver, com.reiko.cars.tuning.TuningKit.ENGINE), 0, "the kit's used");
            check(helper, com.reiko.cars.tuning.TuningService.fitOwn(driver, bench, car, com.reiko.cars.tuning.TuningKit.SPORT_TIRES).success(), "sport tires");
            check(helper, com.reiko.cars.tuning.TuningService.fitOwn(driver, bench, car, com.reiko.cars.tuning.TuningKit.OFF_ROAD_TIRES).success(),
                    "then off-road tires");
            helper.assertValueEqual(car.upgrades().tires(), com.reiko.cars.tuning.Upgrades.Tires.OFF_ROAD, "on");
            helper.assertValueEqual(com.reiko.cars.tuning.TuningService.carried(driver, com.reiko.cars.tuning.TuningKit.SPORT_TIRES), 1,
                    "the sport tires back in hand");

            // Kits from the bench's stock, paid for.
            bench.stock().insertItem(0, new net.minecraft.world.item.ItemStack(ModItems.kit(com.reiko.cars.tuning.TuningKit.TURBO), 2), false);
            bench.stock().insertItem(1, new net.minecraft.world.item.ItemStack(ModItems.kit(com.reiko.cars.tuning.TuningKit.BRAKES), 2), false);
            int mask = 1 << com.reiko.cars.tuning.TuningKit.TURBO.ordinal() | 1 << com.reiko.cars.tuning.TuningKit.BRAKES.ordinal();
            Purse.give(driver, 10_000);
            long shopBefore = ECO.getBalance(server, shop.getUUID());
            check(helper, !com.reiko.cars.tuning.TuningService.buy(driver, bench, car, mask, 5_000, Payment.cash()).success(), "a stale total is caught");
            ShopActionResult bought = com.reiko.cars.tuning.TuningService.buy(driver, bench, car, mask, 8_000, Payment.cash());
            check(helper, bought.success(), "a turbo and brakes: " + bought.message().getString());
            helper.assertValueEqual(ECO.getBalance(server, shop.getUUID()), shopBefore + 8_000, "$40 each to the bench's owner");
            check(helper, car.upgrades().turbo() && car.upgrades().brakes() == 1, "fitted");
            check(helper, bench.count(com.reiko.cars.tuning.TuningKit.TURBO) == 1 && bench.count(com.reiko.cars.tuning.TuningKit.BRAKES) == 1,
                    "from its stock");
            check(helper, !com.reiko.cars.tuning.TuningService.buy(driver, bench, car, 1 << com.reiko.cars.tuning.TuningKit.TURBO.ordinal(), 4_000,
                    Payment.cash()).success(), "no second turbo");

            // A livery, and both kept on the item.
            check(helper, com.reiko.cars.tuning.TuningService.setLivery(driver, bench, car, 7, net.minecraft.world.item.DyeColor.RED).success(), "number 7");
            helper.assertValueEqual(car.livery(), com.reiko.cars.tuning.Livery.of(7, net.minecraft.world.item.DyeColor.RED), "red stripes");
            net.minecraft.world.item.ItemStack item = car.toItem();
            helper.assertValueEqual(item.get(com.reiko.cars.registry.ModDataComponents.UPGRADES.get()), car.upgrades(), "the item keeps the tuning");
            helper.assertValueEqual(item.get(com.reiko.cars.registry.ModDataComponents.LIVERY.get()), car.livery(), "and the livery");

            // A bigger fuel tank: it holds more, and keeps it through a save.
            giveKit(driver, com.reiko.cars.tuning.TuningKit.FUEL_TANK, 3);
            check(helper, com.reiko.cars.tuning.TuningService.fitOwn(driver, bench, car, com.reiko.cars.tuning.TuningKit.FUEL_TANK).success()
                    && com.reiko.cars.tuning.TuningService.fitOwn(driver, bench, car, com.reiko.cars.tuning.TuningKit.FUEL_TANK).success(), "two tank stages");
            check(helper, !com.reiko.cars.tuning.TuningService.fitOwn(driver, bench, car, com.reiko.cars.tuning.TuningKit.FUEL_TANK).success(), "and no more");
            near(helper, car.tankCapacity(), CarType.SEDAN.tankCapacity() * 1.5, "half as much again");
            car.setFuel(car.tankCapacity());
            near(helper, car.fuel(), CarType.SEDAN.tankCapacity() * 1.5, "filled to the brim");
            check(helper, car.fuelRoom() == 0.0 && car.fuelFraction() == 1.0F, "a full gauge");
            net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
            car.saveWithoutId(saved);
            CarEntity reloaded = ModEntities.car(CarType.SEDAN).create(helper.getLevel());
            reloaded.load(saved);
            near(helper, reloaded.fuel(), CarType.SEDAN.tankCapacity() * 1.5, "still full after a save");
            near(helper, car.upgrades().acceleration(), 1.04 * 1.25 * 0.94, "the extra weight takes a little pull");
            CarEntity ev = vehicle(helper, CarType.CITY_EV, 0.5, 6.0);
            ev.setOwner(driver);
            ShopActionResult refused = com.reiko.cars.tuning.TuningService.fitOwn(driver, bench, ev, com.reiko.cars.tuning.TuningKit.FUEL_TANK);
            check(helper, !refused.success() && refused.message().getString().contains("no fuel tank"),
                    "no fuel tank in an electric car: " + refused.message().getString());
            // ...but an electric car takes bigger batteries, and a fuel car doesn't.
            giveKit(driver, com.reiko.cars.tuning.TuningKit.BATTERY, 3);
            check(helper, !com.reiko.cars.tuning.TuningService.fitOwn(driver, bench, car, com.reiko.cars.tuning.TuningKit.BATTERY).success(),
                    "no battery module in a gasoline car");
            check(helper, com.reiko.cars.tuning.TuningService.fitOwn(driver, bench, ev, com.reiko.cars.tuning.TuningKit.BATTERY).success()
                    && com.reiko.cars.tuning.TuningService.fitOwn(driver, bench, ev, com.reiko.cars.tuning.TuningKit.BATTERY).success(), "two battery stages");
            check(helper, !com.reiko.cars.tuning.TuningService.fitOwn(driver, bench, ev, com.reiko.cars.tuning.TuningKit.BATTERY).success(), "and no more");
            near(helper, ev.tankCapacity(), CarType.CITY_EV.tankCapacity() * 1.5, "half as much charge again");
            ev.setFuel(ev.tankCapacity());
            check(helper, ev.fuelFraction() == 1.0F && ev.fuelRoom() == 0.0, "charged to the full");
            check(helper, ev.upgrades().describe(true).getLast().getString().contains("battery"), "a battery on its tooltip");

            // A tuned engine wears faster and drinks more.
            CarEntity plain = vehicle(helper, CarType.SEDAN, 8.0, 12.0);
            check(helper, car.upgrades().engineWear() > 1.0 && car.upgrades().fuelUse() > plain.upgrades().fuelUse(), "the price of speed");
        } finally {
            leave(helper, shop, driver, stranger);
        }
        helper.succeed();
    }

    private static com.reiko.cars.racing.RaceControlBlockEntity raceTrack(GameTestHelper helper, ServerPlayer owner) {
        helper.setBlock(new BlockPos(2, 1, 2), ModBlocks.RACE_CONTROL.get().defaultBlockState());
        var control = (com.reiko.cars.racing.RaceControlBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
        control.setOwner(owner);
        for (int[] at : new int[][] {{6, 16, 1}, {6, 28, 2}}) {
            helper.setBlock(new BlockPos(at[0], 1, at[1]), ModBlocks.RACE_FLAG.get().defaultBlockState());
            var flag = (com.reiko.cars.racing.RaceFlagBlockEntity) helper.getBlockEntity(new BlockPos(at[0], 1, at[1]));
            flag.setOwner(owner);
            flag.setNumber(at[2]);
        }
        return control;
    }

    private static void driveTo(GameTestHelper helper, CarEntity car, double x, double z) {
        Vec3 at = helper.absoluteVec(new Vec3(x, 1.0, z));
        car.setPos(at.x, car.getY(), at.z);
        com.reiko.cars.racing.RaceService.tick(helper.getLevel().getServer());
    }

    /** Round the test track once: out of the line, through both flags, and back over the line. */
    private static void lap(GameTestHelper helper, CarEntity car) {
        driveTo(helper, car, 6.0, 14.0);
        driveTo(helper, car, 6.0, 18.0);
        driveTo(helper, car, 6.0, 28.0);
        driveTo(helper, car, 6.0, 16.0);
        driveTo(helper, car, 3.0, 4.0);
    }

    @GameTest(template = YARD, timeoutTicks = 300)
    public static void timeTrialsAndRaces(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer track = join(helper);
        ServerPlayer a = join(helper);
        ServerPlayer b = join(helper);
        ServerPlayer c = join(helper);
        var control = raceTrack(helper, track);
        CarEntity carA = vehicle(helper, CarType.SPORTS_CAR, 3.0, 5.0);
        CarEntity carB = vehicle(helper, CarType.SEDAN, 8.0, 6.0);
        Runnable done = () -> leave(helper, track, a, b, c);
        helper.startSequence()
                .thenExecute(() -> {
                    helper.assertValueEqual(com.reiko.cars.racing.RaceService.track(control).size(), 2, "two flags");
                    // A time trial: the clock starts over the line, and the lap goes on the board.
                    check(helper, com.reiko.cars.racing.RaceService.trial(a, control).success(), "a time trial");
                    a.startRiding(carA, true);
                    com.reiko.cars.racing.RaceService.tick(server);
                    helper.assertValueEqual(carA.raceStart(), -1L, "waiting for the line");
                    driveTo(helper, carA, 6.0, 14.0);
                    driveTo(helper, carA, 3.0, 4.0);
                    check(helper, carA.raceStart() > 0, "over the line: the clock's running");
                    lap(helper, carA);
                    helper.assertValueEqual(control.board().size(), 1, "a lap on the board");
                    helper.assertValueEqual(control.board().get(0).name(), a.getGameProfile().getName(), "theirs");
                    a.stopRiding();
                    com.reiko.cars.racing.RaceService.tick(server);
                    check(helper, !com.reiko.cars.racing.RaceService.onTrial(a.getUUID()) && carA.raceStart() == 0, "out of the car: over");

                    // A race: entry fees into the pot.
                    ECO.setBalance(server, a.getUUID(), 0, "test");
                    ECO.setBalance(server, c.getUUID(), 0, "test");
                    Purse.give(a, 5_000);
                    Purse.give(b, 5_000);
                    Purse.give(c, 5_000);
                    check(helper, !com.reiko.cars.racing.RaceService.enter(a, control, 500, Payment.cash()).success(), "a stale fee is caught");
                    check(helper, com.reiko.cars.racing.RaceService.enter(a, control, 1_000, Payment.cash()).success(), "a's in");
                    check(helper, !com.reiko.cars.racing.RaceService.start(a, control).success(), "not alone");
                    check(helper, com.reiko.cars.racing.RaceService.enter(b, control, 1_000, Payment.cash()).success(), "b's in");
                    check(helper, com.reiko.cars.racing.RaceService.enter(c, control, 1_000, Payment.cash()).success(), "c's in");
                    check(helper, com.reiko.cars.racing.RaceService.leave(c, control).success(), "c pulls out");
                    helper.assertValueEqual(ECO.getBalance(server, c.getUUID()), 1_000L, "with the fee back");
                    helper.assertValueEqual(com.reiko.cars.racing.RaceService.pot(control), 2_000L, "$20 in the pot");
                    a.startRiding(carA, true);
                    b.startRiding(carB, true);
                    check(helper, com.reiko.cars.racing.RaceService.start(a, control).success(), "lights!");
                    helper.assertValueEqual(com.reiko.cars.racing.RaceService.state(control), com.reiko.cars.racing.RaceService.State.COUNTDOWN, "counting down");
                })
                .thenIdle(com.reiko.cars.racing.RaceService.COUNTDOWN + 2)
                .thenExecute(() -> {
                    try {
                        helper.assertValueEqual(com.reiko.cars.racing.RaceService.state(control), com.reiko.cars.racing.RaceService.State.RUNNING, "go!");
                        var info = com.reiko.cars.racing.RaceInfo.unpack(carA.raceInfo());
                        check(helper, info.laps() == 3 && info.racers() == 2 && carA.raceStart() > 0, "the dashboard knows: " + info);
                        long trackBefore = ECO.getBalance(server, track.getUUID());
                        for (int i = 0; i < 3; i++) {
                            lap(helper, carA);
                        }
                        helper.assertValueEqual(ECO.getBalance(server, a.getUUID()), 1_800L, "a wins the pot, less 10%");
                        helper.assertValueEqual(ECO.getBalance(server, track.getUUID()), trackBefore + 200, "the track's cut");
                        helper.assertValueEqual(control.races(), 1, "on the books");
                        b.stopRiding();
                        com.reiko.cars.racing.RaceService.tick(server);
                        check(helper, com.reiko.cars.racing.RaceService.state(control) == null, "b's out, and it's all over");
                        check(helper, carA.raceStart() == 0 && carB.raceStart() == 0, "the clocks off");
                    } finally {
                        done.run();
                    }
                })
                .thenSucceed();
    }
}
