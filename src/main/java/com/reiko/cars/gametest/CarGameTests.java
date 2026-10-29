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
            helper.assertValueEqual(jobXp(worker, mechanic), 45L, "a real tow-in pays");
            Hitching.use(worker);
            broken.setTowedFrom(broken.position().add(0, 0, -40));
            Hitching.use(worker);
            helper.assertValueEqual(jobXp(worker, mechanic), 45L, "but only once per breakdown");
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
}
