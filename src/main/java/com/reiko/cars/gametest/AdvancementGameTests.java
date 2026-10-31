package com.reiko.cars.gametest;

import static com.reiko.cars.gametest.CarGameTests.car;
import static com.reiko.cars.gametest.CarGameTests.check;
import static com.reiko.cars.gametest.CarGameTests.join;
import static com.reiko.cars.gametest.CarGameTests.leave;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.advancement.CarAdvancements;
import com.reiko.cars.advancement.CarTrigger;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.Criterion;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The mod's advancements: they load, each can be earned by its event, and driving counts a trip. */
@GameTestHolder(ReikoCars.MODID)
@PrefixGameTestTemplate(false)
public final class AdvancementGameTests {
    private static final String EMPTY = "empty";
    /** How many there are (tools/advancements/advancements.py). */
    private static final int COUNT = 26;

    private AdvancementGameTests() {
    }

    /** Whether {@code player} has the mod's advancement {@code id}. */
    static boolean earned(ServerPlayer player, String id) {
        AdvancementHolder holder = player.server.getAdvancements().get(ReikoCars.id(id));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    @GameTest(template = EMPTY)
    public static void everyAdvancementCanBeEarned(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        try {
            List<AdvancementHolder> ours = new ArrayList<>();
            for (AdvancementHolder holder : player.server.getAdvancements().getAllAdvancements()) {
                if (holder.id().getNamespace().equals(ReikoCars.MODID)) {
                    ours.add(holder);
                }
            }
            check(helper, ours.size() == COUNT, "the mod's advancements loaded: " + ours.size() + " of " + COUNT);
            AdvancementHolder root = player.server.getAdvancements().get(ReikoCars.id("root"));
            check(helper, root != null && root.value().parent().isEmpty() && root.value().display().flatMap(d -> d.getBackground()).isPresent(),
                    "a root, with the tab's background");
            int xp = player.totalExperience;
            for (AdvancementHolder holder : ours) {
                String name = holder.id().getPath();
                check(helper, holder.value().display().isPresent(), name + " shows in the tab");
                check(helper, holder == root || holder.value().parent().map(p -> p.getNamespace().equals(ReikoCars.MODID)).orElse(false),
                        name + " hangs off another of the mod's");
                check(helper, !player.getAdvancements().getOrStartProgress(holder).isDone(), name + " not earned yet");
                for (Map.Entry<String, Criterion<?>> entry : holder.value().criteria().entrySet()) {
                    check(helper, entry.getValue().trigger() == CarAdvancements.CAR.get(), name + " waits on the mod's trigger");
                    CarTrigger.Instance instance = (CarTrigger.Instance) entry.getValue().triggerInstance();
                    if (instance.min() > 1.0) {
                        CarAdvancements.reach(player, instance.event(), instance.min() - 1.0);
                        check(helper, !player.getAdvancements().getOrStartProgress(holder).isDone(), name + " not earned short of " + instance.min());
                    }
                    CarAdvancements.reach(player, instance.event(), Math.max(1.0, instance.min()));
                }
                check(helper, player.getAdvancements().getOrStartProgress(holder).isDone(), name + " earned by its event");
            }
            check(helper, player.totalExperience >= xp + 100, "Cross Country pays 100 experience: " + (player.totalExperience - xp));
            helper.succeed();
        } finally {
            leave(helper, player);
        }
    }

    @GameTest(template = EMPTY, timeoutTicks = 100)
    public static void aTripCountsTheBlocksDriven(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        CarEntity car = car(helper, CarType.SEDAN);
        CarAdvancements.drove(player, car, 600.0);
        check(helper, earned(player, "root"), "behind the wheel: the tab opens");
        check(helper, !earned(player, "first_road_trip"), "600 blocks isn't a road trip yet");
        CarAdvancements.drove(player, car, 600.0);
        check(helper, earned(player, "first_road_trip"), "1,200 blocks without getting out is");
        check(helper, !earned(player, "cross_country"), "not cross country yet");
        // Out of the car for a bit: the next drive's a new trip.
        helper.runAfterDelay(CarAdvancements.TRIP_BREAK + 5, () -> {
            CarAdvancements.drove(player, car, 10.0);
            check(helper, Math.abs(CarAdvancements.tripBlocks(player.getUUID()) - 10.0) < 1.0E-6,
                    "a new trip after getting out: " + CarAdvancements.tripBlocks(player.getUUID()));
            leave(helper, player);
            helper.succeed();
        });
    }
}
