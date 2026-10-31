package com.reiko.cars.gametest;

import static com.reiko.cars.gametest.CarGameTests.check;
import static com.reiko.cars.gametest.CarGameTests.join;
import static com.reiko.cars.gametest.CarGameTests.leave;
import static com.reiko.cars.gametest.CarGameTests.run;
import static com.reiko.cars.gametest.CarGameTests.vehicle;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.garage.GarageButtonBlock;
import com.reiko.cars.garage.GarageButtonBlockEntity;
import com.reiko.cars.garage.GarageDoorBlock;
import com.reiko.cars.garage.GarageDoorBlockEntity;
import com.reiko.cars.garage.GarageDoors;
import com.reiko.cars.garage.GarageSettings;
import com.reiko.cars.garage.ParkingService;
import com.reiko.cars.garage.RemoteLinks;
import com.reiko.cars.gps.PlaceKind;
import com.reiko.cars.gps.PlacesRegistry;
import com.reiko.cars.registry.ModBlocks;
import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.tow.Towing;
import com.reiko.cars.trailer.Hitching;
import com.reiko.economy.bank.Purse;
import com.reiko.economy.core.EconomyService;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Garages: panels joining into doors, who opens them and how (by hand, remote, wall button, and by themselves for
 * cars), the door coming down on nothing, cars safe in a closed garage, and parking garages taking money.
 *
 * <p>Each test builds a garage in an empty 14 x 7 x 24 lot: walls of stone round x 1 to 12 and z 5 to 22, a roof at
 * y 5, and a doorway 4 wide and 3 high in its front wall (x 5 to 8, z 5), facing north, onto the street at z 0 to 4.
 */
@GameTestHolder(ReikoCars.MODID)
@PrefixGameTestTemplate(false)
public final class GarageGameTests {
    private static final String GARAGE = "garage";
    /** The door's anchor: its left end, from outside, at the bottom. Facing north, its left is east. */
    private static final BlockPos ANCHOR = new BlockPos(8, 1, 5);
    private static final EconomyService ECO = EconomyService.INSTANCE;

    private GarageGameTests() {
    }

    /** The walls and roof, and, if {@code owner}'s given, the door put up panel by panel. */
    private static GarageDoorBlockEntity build(GameTestHelper helper, ServerPlayer owner) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = 1; x <= 12; x++) {
            for (int z = 5; z <= 22; z++) {
                helper.setBlock(new BlockPos(x, 5, z), stone);
                boolean wall = x == 1 || x == 12 || z == 5 || z == 22;
                for (int y = 1; y <= 4 && wall; y++) {
                    boolean doorway = z == 5 && x >= 5 && x <= 8 && y <= 3;
                    if (!doorway) {
                        helper.setBlock(new BlockPos(x, y, z), stone);
                    }
                }
            }
        }
        if (owner == null) {
            return null;
        }
        for (int y = 1; y <= 3; y++) {
            for (int x = 5; x <= 8; x++) {
                panel(helper, ModBlocks.GARAGE_DOOR.get(), new BlockPos(x, y, 5), owner);
            }
        }
        return door(helper);
    }

    private static void panel(GameTestHelper helper, Block block, BlockPos rel, ServerPlayer owner) {
        BlockState state = block.defaultBlockState().setValue(GarageDoorBlock.FACING, Direction.NORTH);
        helper.setBlock(rel, state);
        BlockPos pos = helper.absolutePos(rel);
        block.setPlacedBy(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), owner, ItemStack.EMPTY);
    }

    /** The finished door the anchor's panel is part of, or null. */
    private static GarageDoorBlockEntity door(GameTestHelper helper) {
        return GarageDoors.doorAt(helper.getLevel(), helper.absolutePos(ANCHOR));
    }

    /** Joins a player in survival, so they use things up and can't break what isn't theirs. */
    private static ServerPlayer survivor(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    private static void standAt(GameTestHelper helper, ServerPlayer player, double x, double z) {
        Vec3 at = helper.absoluteVec(new Vec3(x, 1.0, z));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
    }

    /** {@code player} right-clicks the block at {@code rel} with an empty hand. */
    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos rel) {
        BlockPos pos = helper.absolutePos(rel);
        helper.getLevel().getBlockState(pos).useWithoutItem(helper.getLevel(), player,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
    }

    @GameTest(template = GARAGE, timeoutTicks = 60, skyAccess = true)
    public static void garageDoorPanelsJoinIntoADoor(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        ServerPlayer neighbor = join(helper);
        try {
            build(helper, null);
            // All but one panel: not a rectangle, so no door yet.
            for (int y = 1; y <= 3; y++) {
                for (int x = 5; x <= 8; x++) {
                    if (x != 5 || y != 3) {
                        panel(helper, ModBlocks.GARAGE_DOOR.get(), new BlockPos(x, y, 5), owner);
                    }
                }
            }
            check(helper, door(helper) == null, "eleven panels aren't a door");
            check(helper, helper.getBlockState(ANCHOR).getValue(GarageDoorBlock.PART) == GarageDoorBlock.Part.SINGLE, "they stay loose panels");

            panel(helper, ModBlocks.GARAGE_DOOR.get(), new BlockPos(5, 3, 5), owner);
            GarageDoorBlockEntity door = door(helper);
            check(helper, door != null && door.width() == 4 && door.height() == 3, "the twelfth makes a door 4 wide and 3 high");
            check(helper, door.getBlockPos().equals(helper.absolutePos(ANCHOR)), "anchored at its bottom left, from outside");
            for (int y = 1; y <= 3; y++) {
                for (int x = 5; x <= 8; x++) {
                    BlockPos rel = new BlockPos(x, y, 5);
                    GarageDoorBlock.Part part = helper.getBlockState(rel).getValue(GarageDoorBlock.PART);
                    check(helper, part == (rel.equals(ANCHOR) ? GarageDoorBlock.Part.ANCHOR : GarageDoorBlock.Part.MEMBER), "panel " + rel + " joined");
                    GarageDoorBlockEntity panel = (GarageDoorBlockEntity) helper.getBlockEntity(rel);
                    check(helper, panel.anchorEntity() == door && panel.isTopRow() == (y == 3), "panel " + rel + " knows its door");
                }
            }
            check(helper, !door.settings().inward(), "the garage's on the roofed side, behind the panels' backs");
            check(helper, door.settings().code() != 0L, "it has a code for its remotes");
            check(helper, GarageDoors.depth(helper.absoluteVec(new Vec3(7.0, 1.0, 10.0)), door) < 0.0, "z 10 is inside");
            check(helper, GarageDoors.depth(helper.absoluteVec(new Vec3(7.0, 1.0, 2.0)), door) > 0.0, "z 2 is outside");

            // A neighbor's panel beside it doesn't join it.
            helper.setBlock(new BlockPos(4, 1, 5), Blocks.AIR);
            panel(helper, ModBlocks.GARAGE_DOOR.get(), new BlockPos(4, 1, 5), neighbor);
            check(helper, door(helper) == door && door.width() == 4, "someone else's panel stays out of it");
            helper.setBlock(new BlockPos(4, 1, 5), Blocks.STONE);

            // The owner names it; nobody else can.
            check(helper, !ParkingService.save(neighbor, door, "Mine", 3, "1", "1").success(), "only its owner sets it up");
            ShopActionResult saved = ParkingService.save(owner, door, "Home", 3, "1.00", "10.00");
            check(helper, saved.success() && door.settings().name().equals("Home"), "named Home");
            long code = door.settings().code();

            // Knock a corner out and it falls apart; put it back and it's the same door again.
            helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(5, 3, 5)), false);
            check(helper, door(helper) == null, "a panel short, it's loose panels again");
            check(helper, helper.getBlockState(ANCHOR).getValue(GarageDoorBlock.PART) == GarageDoorBlock.Part.SINGLE, "the anchor's loose too");
            panel(helper, ModBlocks.GARAGE_DOOR.get(), new BlockPos(5, 3, 5), owner);
            GarageDoorBlockEntity again = door(helper);
            check(helper, again != null && again.width() == 4 && again.height() == 3, "whole again");
            check(helper, again.settings().name().equals("Home") && again.settings().code() == code, "with its name and code kept");

            // A roller shutter's panels don't join a sectional door's.
            helper.setBlock(new BlockPos(4, 1, 5), Blocks.AIR);
            panel(helper, ModBlocks.ROLLER_SHUTTER.get(), new BlockPos(4, 1, 5), owner);
            check(helper, door(helper).width() == 4, "a shutter panel beside it stays out of it");
            GarageDoorBlockEntity shutter = GarageDoors.doorAt(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 5)));
            check(helper, shutter != null && shutter.style() == GarageDoorBlock.Style.SHUTTER && shutter.width() == 1, "it's a little shutter of its own");

            // Put up from inside (its panels facing in), a door still knows which side's the garage: the roofed one.
            for (int y = 1; y <= 2; y++) {
                for (int x = 3; x <= 4; x++) {
                    BlockPos rel = new BlockPos(x, y, 22);
                    helper.setBlock(rel, Blocks.AIR);
                    BlockState state = ModBlocks.GARAGE_DOOR.get().defaultBlockState().setValue(GarageDoorBlock.FACING, Direction.NORTH);
                    helper.setBlock(rel, state);
                    BlockPos pos = helper.absolutePos(rel);
                    state.getBlock().setPlacedBy(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), owner, ItemStack.EMPTY);
                }
            }
            GarageDoorBlockEntity back = GarageDoors.doorAt(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 22)));
            check(helper, back != null && back.width() == 2 && back.height() == 2, "a 2 by 2 door in the back wall");
            check(helper, back.settings().inward() && GarageDoors.outward(back) == Direction.SOUTH, "its outside is south, the way its panels' backs face");
            check(helper, GarageDoors.depth(helper.absoluteVec(new Vec3(3.5, 1.0, 20.0)), back) < 0.0, "z 20 is inside it");
        } finally {
            leave(helper, owner, neighbor);
        }
        helper.succeed();
    }

    @GameTest(template = GARAGE, timeoutTicks = 200)
    public static void garageDoorsOpenForTheirOwnersAndRemotes(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        ServerPlayer friend = join(helper);
        Runnable done = () -> leave(helper, owner, friend);
        GarageDoorBlockEntity door = build(helper, owner);
        BlockPos middle = new BlockPos(6, 1, 5);
        run(helper, done, () -> {
            check(helper, door != null, "the door's up");
            standAt(helper, friend, 7.0, 2.0);
            click(helper, friend, middle);
            check(helper, !door.wantsOpen(), "locked to anyone else outside");
            standAt(helper, owner, 7.0, 2.0);
            click(helper, owner, middle);
            check(helper, door.wantsOpen(), "its owner opens it");
        });
        helper.runAtTickTime(30, () -> run(helper, done, () -> {
            check(helper, door.progress() >= 1.0F, "all the way up");
            for (int y = 1; y <= 3; y++) {
                BlockPos rel = new BlockPos(6, y, 5);
                check(helper, helper.getBlockState(rel).getValue(GarageDoorBlock.OPEN), "row " + y + " clear");
                check(helper, helper.getBlockState(rel).getCollisionShape(helper.getLevel(), helper.absolutePos(rel), CollisionContext.empty()).isEmpty(),
                        "nothing to bump into in row " + y);
            }
            click(helper, owner, middle);
            check(helper, !door.wantsOpen(), "shutting");
        }));
        helper.runAtTickTime(36, () -> run(helper, done, () -> {
            // Something walks under it: it backs up.
            Pig pig = EntityType.PIG.create(helper.getLevel());
            Vec3 at = helper.absoluteVec(new Vec3(6.5, 1.0, 5.5));
            pig.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
            pig.setNoAi(true);
            helper.getLevel().addFreshEntity(pig);
        }));
        helper.runAtTickTime(42, () -> run(helper, done, () -> {
            check(helper, door.wantsOpen(), "it went back up rather than come down on the pig");
            helper.getLevel().getEntitiesOfClass(Pig.class, GarageDoors.doorway(door).inflate(1.0)).forEach(Pig::discard);
            GarageDoors.close(door);
        }));
        helper.runAtTickTime(80, () -> run(helper, done, () -> {
            check(helper, door.isShut(), "and shut once it's gone");
            check(helper, !helper.getBlockState(new BlockPos(6, 1, 5)).getValue(GarageDoorBlock.OPEN), "the doorway's closed off");
            // Inside, anyone can open it: nobody's shut in.
            standAt(helper, friend, 7.0, 10.0);
            click(helper, friend, middle);
            check(helper, door.wantsOpen(), "the friend let themselves out");
            GarageDoors.close(door);

            // The owner links a remote and gives it to the friend.
            ItemStack remote = new ItemStack(ModItems.GARAGE_REMOTE.get());
            owner.setItemInHand(InteractionHand.MAIN_HAND, remote);
            owner.setShiftKeyDown(true);
            BlockPos panel = helper.absolutePos(middle);
            remote.useOn(new UseOnContext(owner, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(panel), Direction.NORTH, panel, false)));
            owner.setShiftKeyDown(false);
            RemoteLinks links = remote.get(ModDataComponents.GARAGE_LINKS.get());
            check(helper, links != null && links.links().size() == 1 && links.links().getFirst().anchor().equals(door.getBlockPos()), "linked");
            friend.setItemInHand(InteractionHand.MAIN_HAND, remote.copy());
            owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            standAt(helper, friend, 7.0, 2.0);
            check(helper, GarageDoors.canOpen(friend, door), "the remote lets the friend in");
            friend.getMainHandItem().use(helper.getLevel(), friend, InteractionHand.MAIN_HAND);
            check(helper, door.wantsOpen(), "a click of the remote opens it");
            GarageDoors.close(door);

            // The friend can't link remotes to someone else's door.
            ItemStack another = new ItemStack(ModItems.GARAGE_REMOTE.get());
            friend.setItemInHand(InteractionHand.OFF_HAND, another);
            friend.setShiftKeyDown(true);
            another.useOn(new UseOnContext(friend, InteractionHand.OFF_HAND, new BlockHitResult(Vec3.atCenterOf(panel), Direction.NORTH, panel, false)));
            friend.setShiftKeyDown(false);
            check(helper, another.get(ModDataComponents.GARAGE_LINKS.get()) == null, "only its owner links remotes");

            // Resetting the remotes stops them all.
            check(helper, ParkingService.resetRemotes(owner, door).success(), "reset");
            check(helper, !GarageDoors.canOpen(friend, door), "the friend's remote no longer works");
            friend.getCooldowns().removeCooldown(ModItems.GARAGE_REMOTE.get());
            friend.getMainHandItem().use(helper.getLevel(), friend, InteractionHand.MAIN_HAND);
            check(helper, !door.wantsOpen(), "the door stays shut");
            done.run();
            helper.succeed();
        }));
    }

    @GameTest(template = GARAGE, timeoutTicks = 220, skyAccess = true)
    public static void garagesOpenForCarsAndKeepThemSafe(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        ServerPlayer stranger = join(helper);
        Runnable done = () -> leave(helper, owner, stranger);
        GarageDoorBlockEntity door = build(helper, owner);
        CarEntity car = vehicle(helper, CarType.SEDAN, 7.0, 2.0);
        car.setOwner(owner);
        car.setLocked(false);
        owner.startRiding(car, true);
        run(helper, done, () -> {
            check(helper, ParkingService.doorAhead(car) == door, "the door's just ahead");
            check(helper, Hitching.available(car, owner) == Hitching.Action.GARAGE, "the hitch key works the door");
            check(helper, GarageDoors.letsIn(car, owner, door), "it opens for its owner's car");
        });
        helper.runAtTickTime(8, () -> run(helper, done, () -> {
            check(helper, door.wantsOpen() && door.openedByCar(), "it opened as the car drove up");
            // In it goes, and its driver gets out.
            owner.stopRiding();
            Vec3 inside = helper.absoluteVec(new Vec3(7.0, 1.0, 11.26));
            car.moveTo(inside.x, inside.y, inside.z, 0.0F, 0.0F);
        }));
        helper.runAtTickTime(140, () -> run(helper, done, () -> {
            check(helper, door.isShut(), "it shut by itself behind the car");
            check(helper, GarageDoors.garaged(car), "the car's safe in the garage");
            // Unlocked, but nobody without its key gets in.
            car.interact(stranger, InteractionHand.MAIN_HAND);
            check(helper, stranger.getVehicle() == null, "a stranger can't get in");
            CarEntity truck = vehicle(helper, CarType.FLATBED, 7.0, 17.0);
            check(helper, Towing.towable(truck, stranger) == null, "or tow it away");
            check(helper, Towing.towable(truck, owner) == car, "though its owner can");
            car.interact(owner, InteractionHand.MAIN_HAND);
            check(helper, owner.getVehicle() == car, "and get in");
            owner.stopRiding();

            // Under the open sky it isn't safe; nor with the door open.
            for (BlockPos roof : BlockPos.betweenClosed(new BlockPos(4, 5, 8), new BlockPos(10, 5, 15))) {
                helper.setBlock(roof, Blocks.AIR);
            }
            check(helper, !GarageDoors.garaged(car), "no roof, no garage");
            for (BlockPos roof : BlockPos.betweenClosed(new BlockPos(4, 5, 8), new BlockPos(10, 5, 15))) {
                helper.setBlock(roof, Blocks.GLASS);
            }
            check(helper, GarageDoors.garaged(car), "a glass roof will do");
            GarageDoors.open(door, false);
            check(helper, !GarageDoors.garaged(car), "with the door open, anyone could walk in");
            check(helper, Towing.towable(truck, stranger) == car, "so an unlocked car can be towed again");
            truck.discard();

            // A stranger's car doesn't open it.
            CarEntity theirs = vehicle(helper, CarType.SEDAN, 3.0, 2.0);
            theirs.setOwner(stranger);
            check(helper, !GarageDoors.letsIn(theirs, stranger, door), "it doesn't open for a stranger's car");
            check(helper, ParkingService.through(car, stranger, door), "but the owner's car's let through, whoever's driving it");
            done.run();
            helper.succeed();
        }));
    }

    @GameTest(template = GARAGE, timeoutTicks = 60)
    public static void parkingGaragesChargeByTheHour(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer owner = join(helper);
        ServerPlayer driver = join(helper);
        try {
            GarageDoorBlockEntity door = build(helper, owner);
            check(helper, PlaceKind.of(door) == null, "not on the GPS till it's a parking garage");
            int flags = 1 | 2 | 8;
            check(helper, ParkingService.save(owner, door, "City Parking", flags, "1.00", "10.00").success(), "set up as a parking garage");
            GarageSettings settings = door.settings();
            check(helper, settings.parking() && settings.hourPrice() == 100 && settings.dayPrice() == 1000, "$1 an hour, $10 a day");
            check(helper, ParkingService.price(settings, 1) == 200 && ParkingService.price(settings, 5) == 2000, "2 hours $2, 2 days $20");
            check(helper, PlaceKind.of(door) == PlaceKind.PARKING, "on the GPS as parking");
            PlacesRegistry.Place place = PlacesRegistry.describe(door);
            check(helper, place != null && place.owner().equals("City Parking") && place.price() == 100, "by its name and hourly price");

            CarEntity car = vehicle(helper, CarType.SEDAN, 7.0, 2.0);
            car.setOwner(driver);
            driver.startRiding(car, true);
            check(helper, ParkingService.wantsTicket(car, driver, door), "a stranger's car pays to go in");
            check(helper, Hitching.available(car, driver) == Hitching.Action.PARK, "the hitch key's for paying");
            check(helper, !GarageDoors.letsIn(car, driver, door), "it doesn't open for them yet");

            Purse.give(driver, 5_000);
            long ownerBefore = ECO.getBalance(server, owner.getUUID());
            check(helper, !ParkingService.pay(driver, door.getBlockPos(), 1, 100, Payment.cash()).success(), "an out-of-date price is caught");
            ShopActionResult paid = ParkingService.pay(driver, door.getBlockPos(), 1, 200, Payment.cash());
            check(helper, paid.success(), "paying: " + paid.message().getString());
            helper.assertValueEqual(Purse.value(driver), 4_800L, "$2 in cash");
            helper.assertValueEqual(ECO.getBalance(server, owner.getUUID()), ownerBefore + 200, "to the garage's owner");
            check(helper, door.wantsOpen(), "the door opens");
            check(helper, ParkingService.paidUp(car, door) && GarageDoors.letsIn(car, driver, door), "and lets the car in and out till its time's up");
            check(helper, Hitching.available(car, driver) == Hitching.Action.GARAGE, "nothing more to pay");
            check(helper, door.earned() == 200 && door.visits() == 1, "the garage counts it");
            long now = helper.getLevel().getGameTime();
            check(helper, door.parked().get(car.getUUID()).until() >= now + 2 * ParkingService.HOUR - 1, "two hours on its ticket");

            // Inside, its time runs out: it pays for the hours over to get out.
            Vec3 inside = helper.absoluteVec(new Vec3(7.0, 1.0, 8.0));
            car.moveTo(inside.x, inside.y, inside.z, 180.0F, 0.0F);
            door.parked().put(car.getUUID(), new com.reiko.cars.garage.GarageDoorBlockEntity.Parked(now - 2_500, car.plate(), "x"));
            check(helper, ParkingService.wantsTicket(car, driver, door), "overstayed");
            check(helper, Hitching.available(car, driver) == Hitching.Action.LEAVE_PARKING, "the hitch key's for paying to leave");
            helper.assertValueEqual(ParkingService.overstayHours(car, door), 3, "three hours over, rounded up");
            GarageDoors.close(door);
            ShopActionResult left = ParkingService.pay(driver, door.getBlockPos(), ParkingService.LEAVE, 300, Payment.cash());
            check(helper, left.success(), "paying the overstay: " + left.message().getString());
            helper.assertValueEqual(ECO.getBalance(server, owner.getUUID()), ownerBefore + 500, "the overstay to the owner too");
            check(helper, door.wantsOpen() && ParkingService.paidUp(car, door), "the door opens to let it out");
            driver.stopRiding();

            // The owner's own car goes in and out free.
            CarEntity ownCar = vehicle(helper, CarType.SEDAN, 7.0, 2.0);
            ownCar.setOwner(owner);
            check(helper, !ParkingService.wantsTicket(ownCar, owner, door), "the owner doesn't pay to park");
            check(helper, ParkingService.through(ownCar, driver, door), "nor anyone driving the owner's car");
        } finally {
            leave(helper, owner, driver);
        }
        helper.succeed();
    }

    @GameTest(template = GARAGE, timeoutTicks = 80)
    public static void garageButtonsAndPaint(GameTestHelper helper) {
        ServerPlayer owner = survivor(helper);
        ServerPlayer stranger = survivor(helper);
        Runnable done = () -> leave(helper, owner, stranger);
        GarageDoorBlockEntity door = build(helper, owner);
        BlockPos insideButton = new BlockPos(3, 2, 6);
        BlockPos outsideButton = new BlockPos(3, 2, 4);
        run(helper, done, () -> {
            for (BlockPos rel : new BlockPos[] {insideButton, outsideButton}) {
                Direction facing = rel.getZ() > 5 ? Direction.SOUTH : Direction.NORTH;
                BlockState state = ModBlocks.GARAGE_BUTTON.get().defaultBlockState().setValue(GarageButtonBlock.FACING, facing);
                helper.setBlock(rel, state);
                BlockPos pos = helper.absolutePos(rel);
                state.getBlock().setPlacedBy(helper.getLevel(), pos, state, owner, ItemStack.EMPTY);
                GarageButtonBlockEntity button = (GarageButtonBlockEntity) helper.getBlockEntity(rel);
                check(helper, door.getBlockPos().equals(button.door()), "the button at " + rel + " works the door");
            }
            standAt(helper, stranger, 4.0, 8.0);
            click(helper, stranger, insideButton);
            check(helper, door.wantsOpen(), "anyone inside can press the button to get out");
            check(helper, helper.getBlockState(insideButton).getValue(GarageButtonBlock.PRESSED), "it's pressed in");
            GarageDoors.close(door);
            standAt(helper, stranger, 4.0, 2.0);
            click(helper, stranger, outsideButton);
            check(helper, !door.wantsOpen(), "but outside it's only for whoever may open the door");

            // Paint: the owner's dye colors the whole door; nobody else's.
            int before = door.color();
            ItemStack blue = new ItemStack(Items.BLUE_DYE, 2);
            stranger.setItemInHand(InteractionHand.MAIN_HAND, blue);
            BlockPos top = helper.absolutePos(new BlockPos(5, 3, 5));
            helper.getLevel().getBlockState(top).useItemOn(blue, helper.getLevel(), stranger, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(top), Direction.NORTH, top, false));
            check(helper, door.color() == before && blue.getCount() == 2, "a stranger can't paint it");
            ItemStack red = new ItemStack(Items.RED_DYE, 2);
            owner.setItemInHand(InteractionHand.MAIN_HAND, red);
            helper.getLevel().getBlockState(top).useItemOn(red, helper.getLevel(), owner, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(top), Direction.NORTH, top, false));
            int rgb = net.minecraft.world.item.DyeColor.RED.getTextureDiffuseColor() & 0xFFFFFF;
            check(helper, door.color() == rgb, "the owner paints it red");
            check(helper, ((GarageDoorBlockEntity) helper.getBlockEntity(new BlockPos(5, 3, 5))).color() == rgb, "every panel of it");
            check(helper, red.getCount() == 1, "using up one dye");

            // Nobody else can knock it down, or its buttons.
            BlockPos panel = helper.absolutePos(new BlockPos(6, 1, 5));
            check(helper, !stranger.gameMode.destroyBlock(panel), "a stranger can't break a panel");
            check(helper, !stranger.gameMode.destroyBlock(helper.absolutePos(outsideButton)), "or a button");
            check(helper, helper.getBlockState(new BlockPos(6, 1, 5)).is(ModBlocks.GARAGE_DOOR.get()), "it's still there");
        });
        helper.runAtTickTime(15, () -> run(helper, done, () -> {
            check(helper, !helper.getBlockState(insideButton).getValue(GarageButtonBlock.PRESSED), "the button springs back");
            standAt(helper, owner, 4.0, 2.0);
            click(helper, owner, outsideButton);
            check(helper, door.wantsOpen(), "the owner's press outside opens it");
            done.run();
            helper.succeed();
        }));
    }
}
