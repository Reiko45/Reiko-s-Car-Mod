package com.reiko.cars.dmv;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.license.LicenseService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Driving tests under way. Having paid at a DMV, the candidate gets into any car they may drive and drives through
 * its owner's numbered cones in order (within {@link #CHECKPOINT_REACH} blocks of each), inside
 * {@code licenses.testMinutes}. Crashing (any body damage), running a red light, speeding past a speed limit sign,
 * getting out or into another car, or running out of time fails it; logging out calls it off. Passing it issues
 * their license. Tests aren't saved: a restart calls them all off.
 */
public final class DrivingTests {
    /** How close (across the ground) the car must pass each cone. */
    public static final double CHECKPOINT_REACH = 3.5;

    private static final Map<UUID, Test> TESTS = new HashMap<>();

    /** One candidate's test: where it's from, the cones, how far they've got and by when. */
    private static final class Test {
        final GlobalPos dmv;
        final String dmvOwner;
        final List<BlockPos> route;
        final long deadline;
        int next;
        @Nullable
        UUID car;
        float body;

        Test(GlobalPos dmv, String dmvOwner, List<BlockPos> route, long deadline) {
            this.dmv = dmv;
            this.dmvOwner = dmvOwner;
            this.route = route;
            this.deadline = deadline;
        }
    }

    private DrivingTests() {
    }

    public static boolean taking(Player player) {
        return TESTS.containsKey(player.getUUID());
    }

    /** How many cones {@code player} has got through on their test, or -1 if they aren't taking one. For tests. */
    public static int progress(Player player) {
        Test test = TESTS.get(player.getUUID());
        return test == null ? -1 : test.next;
    }

    /** Starts {@code player}'s test round {@code route} for the DMV at {@code dmv}. */
    static void start(ServerPlayer player, DmvOfficeBlockEntity dmv, List<BlockPos> route) {
        long minutes = CarsConfig.get(CarsConfig.TEST_MINUTES);
        long deadline = player.serverLevel().getGameTime() + minutes * 1200;
        TESTS.put(player.getUUID(), new Test(GlobalPos.of(player.serverLevel().dimension(), dmv.getBlockPos()), dmv.ownerName(), List.copyOf(route), deadline));
        BlockPos first = route.get(0);
        player.sendSystemMessage(Component.translatable("dmv.reikocars.test_started", route.size(), minutes, first.getX(), first.getY(), first.getZ())
                .withStyle(ChatFormatting.AQUA));
    }

    /** Calls off {@code player}'s test without a word (logging out, or for tests). */
    public static void cancel(Player player) {
        TESTS.remove(player.getUUID());
    }

    /** A camera's eye saw {@code driver} run a red light or speed: that's their test failed. */
    public static void caught(ServerPlayer driver, boolean redLight) {
        if (taking(driver)) {
            fail(driver, redLight ? "dmv.reikocars.fail.red_light" : "dmv.reikocars.fail.speeding");
        }
    }

    private static void fail(ServerPlayer player, String why) {
        TESTS.remove(player.getUUID());
        player.sendSystemMessage(Component.translatable("dmv.reikocars.test_failed", Component.translatable(why)).withStyle(ChatFormatting.RED));
        player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void pass(ServerPlayer player, Test test) {
        TESTS.remove(player.getUUID());
        MinecraftServer server = player.server;
        ServerLevel dmvLevel = server.getLevel(test.dmv.dimension());
        if (dmvLevel != null && dmvLevel.isLoaded(test.dmv.pos()) && dmvLevel.getBlockEntity(test.dmv.pos()) instanceof DmvOfficeBlockEntity dmv) {
            dmv.recordPass();
        }
        var license = LicenseService.issue(player, test.dmvOwner);
        player.sendSystemMessage(Component.translatable("dmv.reikocars.test_passed", license.number()).withStyle(ChatFormatting.GREEN));
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    /** Checks every test. On the server, every few ticks. */
    public static void tick(MinecraftServer server, boolean announce) {
        Iterator<Map.Entry<UUID, Test>> it = TESTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Test> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove(); // logged out: called off
                continue;
            }
            Test test = entry.getValue();
            ServerLevel level = player.serverLevel();
            long now = level.getGameTime();
            CarEntity car = player.getVehicle() instanceof CarEntity riding && riding.getControllingPassenger() == player ? riding : null;
            if (now > test.deadline) {
                fail(player, "dmv.reikocars.fail.time");
                continue;
            }
            if (test.car == null) {
                if (car != null) {
                    test.car = car.getUUID();
                    test.body = car.condition().body();
                }
            } else if (car == null || !car.getUUID().equals(test.car)) {
                fail(player, "dmv.reikocars.fail.left_car");
                continue;
            } else if (car.condition().body() < test.body - 0.0005F) {
                fail(player, "dmv.reikocars.fail.crash");
                continue;
            }
            if (car != null && level.dimension() == test.dmv.dimension()) {
                BlockPos cone = test.route.get(test.next);
                double dx = car.getX() - (cone.getX() + 0.5);
                double dz = car.getZ() - (cone.getZ() + 0.5);
                if (dx * dx + dz * dz <= CHECKPOINT_REACH * CHECKPOINT_REACH && Math.abs(car.getY() - cone.getY()) <= 4.0) {
                    test.next++;
                    if (test.next >= test.route.size()) {
                        pass(player, test);
                        continue;
                    }
                    player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8F, 1.0F + 0.05F * test.next);
                    announce = true;
                }
            }
            if (announce) {
                BlockPos cone = test.route.get(test.next);
                long seconds = Math.max(0, (test.deadline - now) / 20);
                Component line = test.car == null
                        ? Component.translatable("dmv.reikocars.test_get_in", test.route.size(), seconds / 60, String.format("%02d", seconds % 60))
                        : Component.translatable("dmv.reikocars.test_progress", test.next + 1, test.route.size(), cone.getX(), cone.getZ(), seconds / 60,
                        String.format("%02d", seconds % 60));
                player.displayClientMessage(line, true);
                if (level.dimension() == test.dmv.dimension()) {
                    // A column of sparkles over the next cone, for the candidate's eyes only.
                    level.sendParticles(player, ParticleTypes.END_ROD, true, cone.getX() + 0.5, cone.getY() + 2.0, cone.getZ() + 0.5, 8, 0.05, 1.0, 0.05, 0.0);
                }
            }
        }
    }
}
