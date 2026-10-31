package com.reiko.cars.gametest;

import static com.reiko.cars.gametest.CarGameTests.bench;
import static com.reiko.cars.gametest.CarGameTests.car;
import static com.reiko.cars.gametest.CarGameTests.check;
import static com.reiko.cars.gametest.CarGameTests.close;
import static com.reiko.cars.gametest.CarGameTests.join;
import static com.reiko.cars.gametest.CarGameTests.leave;
import static com.reiko.cars.gametest.CarGameTests.vehicle;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
import com.reiko.cars.car.CarDamage;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.repair.RepairBenchBlockEntity;
import com.reiko.cars.repair.RepairJobs;
import com.reiko.cars.repair.RepairService;
import com.reiko.economy.bank.Purse;
import com.reiko.economy.core.EconomyService;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Crash damage: where a crash dents a car, cracks and shatters its glass and smashes its lamps, how a bent car pulls,
 * and mending it all at a repair bench (Body Panels for the dents, Car Windows and Car Lights for the rest).
 */
@GameTestHolder(ReikoCars.MODID)
@PrefixGameTestTemplate(false)
public final class CrashDamageGameTests {
    private static final String YARD = "yard";
    private static final EconomyService ECO = EconomyService.INSTANCE;

    private CrashDamageGameTests() {
    }

    @GameTest(template = YARD, timeoutTicks = 40)
    public static void crashesShowWhereTheyHit(GameTestHelper helper) {
        ServerPlayer owner = join(helper);
        try {
            // Head on, hard: the front crumpled, the windshield shattered, both headlights smashed.
            CarEntity headOn = vehicle(helper, CarType.SEDAN, 4.0, 4.0);
            headOn.setOwner(owner);
            headOn.crash(1.2, 0.0);
            CarDamage front = headOn.condition().damage();
            check(helper, front.front() >= 0.99F && front.rear() == 0.0F && front.left() < 0.01F && front.right() < 0.01F,
                    "the front's crumpled, nothing else: " + front);
            check(helper, front.window(CarDamage.WINDSHIELD) == CarDamage.SHATTERED && front.window(CarDamage.BACK_WINDOW) == CarDamage.WHOLE,
                    "the windshield's shattered, the back window whole");
            check(helper, front.lightBroken(CarDamage.FRONT_LEFT) && front.lightBroken(CarDamage.FRONT_RIGHT)
                    && !front.lightBroken(CarDamage.REAR_LEFT) && !front.lightBroken(CarDamage.REAR_RIGHT), "the headlights smashed, the taillights not");
            check(helper, headOn.condition().damage().workingHeadlights() == 0, "no headlights left");
            check(helper, front.signalWorks(true) && front.signalWorks(false), "the turn signals still blink at the back");

            // Hit on its left side: that side caved in, its window shattered, its two lamps smashed.
            CarEntity side = vehicle(helper, CarType.SEDAN, 4.0, 12.0);
            side.setOwner(owner);
            side.crash(1.5, Mth.HALF_PI);
            CarDamage left = side.condition().damage();
            check(helper, left.left() >= 0.99F && left.right() == 0.0F && left.front() < 0.01F && left.rear() < 0.01F, "the left side's caved in: " + left);
            check(helper, left.window(CarDamage.LEFT_WINDOW) == CarDamage.SHATTERED && left.window(CarDamage.RIGHT_WINDOW) == CarDamage.WHOLE,
                    "its window shattered, the other side's whole");
            check(helper, left.lightBroken(CarDamage.FRONT_LEFT) && left.lightBroken(CarDamage.REAR_LEFT)
                    && !left.lightBroken(CarDamage.FRONT_RIGHT) && !left.lightBroken(CarDamage.REAR_RIGHT), "the left lamps smashed");
            check(helper, !left.signalWorks(true) && left.signalWorks(false), "so the left turn signal's out");
            check(helper, left.pull() > 0.5F, "and it pulls to the left: " + left.pull());

            // Bumped from behind, gently: a small dent at the back, no shattered glass, nothing at the front.
            CarEntity bumped = vehicle(helper, CarType.SEDAN, 4.0, 20.0);
            bumped.setOwner(owner);
            bumped.crash(0.4, Mth.PI);
            CarDamage rear = bumped.condition().damage();
            close(helper, rear.rear(), (0.4 - CarCondition.CRASH_THRESHOLD) * 0.6 * 2.0, "a dent at the back, as much as the body lost, twice");
            check(helper, rear.front() == 0.0F && rear.window(CarDamage.BACK_WINDOW) != CarDamage.SHATTERED && rear.window(CarDamage.WINDSHIELD) == CarDamage.WHOLE,
                    "nothing shattered, nothing at the front");
            check(helper, rear.pull() == 0.0F, "a car bent front or back runs straight");

            // Too gentle to leave a mark.
            CarEntity nudged = vehicle(helper, CarType.SEDAN, 4.0, 28.0);
            nudged.crash(CarCondition.CRASH_THRESHOLD * 0.9, 0.0);
            check(helper, nudged.condition().damage().isNone() && nudged.condition().isNew(), "a nudge leaves nothing");

            // A motorcycle's one headlight breaks whole.
            CarEntity bike = vehicle(helper, CarType.SPORT_BIKE, 8.0, 28.0);
            bike.crash(1.2, 0.0);
            CarDamage bent = bike.condition().damage();
            check(helper, bent.lightBroken(CarDamage.FRONT_LEFT) == bent.lightBroken(CarDamage.FRONT_RIGHT), "a bike's headlight is one lamp");
            check(helper, !bike.hasPart(Part.GLASS) && bike.hasPart(Part.LIGHTS), "a bike has no windows to mend");

            // It's saved with the car, and carried on its item.
            net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
            side.saveWithoutId(saved);
            CarEntity copy = vehicle(helper, CarType.SEDAN, 8.0, 4.0);
            copy.load(saved);
            check(helper, copy.condition().damage().equals(left), "saved and loaded: " + copy.condition().damage());
            check(helper, !copy.condition().isNew(), "a damaged car isn't as new");
        } finally {
            leave(helper, owner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bodyPanelsTakeTheDentsOut(GameTestHelper helper) {
        CarCondition wrecked = new CarCondition(1.0F, 1.0F, 1.0F, 1.0F, 0.4F, CarDamage.NONE.withDent(CarDamage.Zone.FRONT, 0.8F)
                .withDent(CarDamage.Zone.LEFT, 0.4F));
        CarCondition half = wrecked.with(Part.BODY, 0.7F);
        close(helper, half.damage().front(), 0.4, "mending half the lost body takes half the dent out");
        close(helper, half.damage().left(), 0.2, "on every side");
        CarCondition mended = wrecked.with(Part.BODY, 1.0F);
        check(helper, mended.damage().worstDent() == 0.0F, "a body as new has no dents");
        check(helper, wrecked.with(Part.BODY, 0.3F).damage().front() == 0.8F, "wear on the body doesn't add dents or take them out");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void theRepairBenchMendsGlassAndLamps(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer mechanic = join(helper);
        ServerPlayer driver = join(helper);
        try {
            RepairBenchBlockEntity bench = bench(helper, mechanic, 1000); // $10.00 a part
            check(helper, bench.stock().insertItem(0, new ItemStack(ModItems.CAR_WINDOW.get(), 1), false).isEmpty(), "the bench stocks windows");
            check(helper, bench.stock().insertItem(1, new ItemStack(ModItems.CAR_LIGHT.get(), 3), false).isEmpty(), "and lamps");
            CarEntity car = car(helper, CarType.SEDAN);
            car.setOwner(driver);
            CarDamage damage = CarDamage.NONE.withWindow(CarDamage.WINDSHIELD, CarDamage.CRACKED).withWindow(CarDamage.LEFT_WINDOW, CarDamage.SHATTERED)
                    .withLight(CarDamage.FRONT_LEFT, true).withLight(CarDamage.REAR_RIGHT, true);
            car.setCondition(CarCondition.NEW.withDamage(damage));
            helper.assertValueEqual(RepairJobs.needed(Part.GLASS, car), 2, "two windows to mend");
            helper.assertValueEqual(RepairJobs.needed(Part.LIGHTS, car), 2, "two lamps");
            close(helper, car.condition().get(Part.GLASS), 1.0 - 1.5 / 4.0, "the glass at 63%: a cracked window half, a shattered one all");
            close(helper, car.condition().get(Part.LIGHTS), 0.5, "the lamps at half");
            check(helper, car.condition().brokenPart() == null && !car.isBrokenDown(), "broken glass and lamps don't stop a car");

            // The driver's own window: the shattered one first.
            driver.getInventory().add(new ItemStack(ModItems.CAR_WINDOW.get()));
            ShopActionResult own = RepairService.fitOwn(driver, bench, car, 1 << Part.GLASS.ordinal());
            check(helper, own.success(), "fitting a window: " + own.message().getString());
            check(helper, car.condition().damage().window(CarDamage.LEFT_WINDOW) == CarDamage.WHOLE
                    && car.condition().damage().window(CarDamage.WINDSHIELD) == CarDamage.CRACKED, "the shattered one's mended first");

            // The bench's stock for the rest: a window and two lamps, paid to the mechanic.
            Purse.give(driver, 10_000);
            long before = ECO.getBalance(server, mechanic.getUUID());
            int mask = 1 << Part.GLASS.ordinal() | 1 << Part.LIGHTS.ordinal();
            ShopActionResult paid = RepairService.buy(driver, bench, car, mask, 3000, Payment.cash());
            check(helper, paid.success(), "paying for glass and lamps: " + paid.message().getString());
            check(helper, car.condition().damage().isNone(), "all mended: " + car.condition().damage());
            helper.assertValueEqual(ECO.getBalance(server, mechanic.getUUID()), before + 3000, "$30.00 to the mechanic");
            helper.assertValueEqual(bench.count(Part.GLASS) + bench.count(Part.LIGHTS), 1, "a lamp left in stock");
            check(helper, !RepairService.buy(driver, bench, car, mask, 0, Payment.cash()).success(), "nothing left to do");
        } finally {
            leave(helper, mechanic, driver);
        }
        helper.succeed();
    }
}
