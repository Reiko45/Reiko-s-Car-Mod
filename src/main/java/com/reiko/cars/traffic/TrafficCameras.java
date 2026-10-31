package com.reiko.cars.traffic;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.dmv.DrivingTests;
import com.reiko.cars.license.LicenseService;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.api.TransactionResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Red-light and speed cameras. Every loaded traffic light and speed sign is on its level's list; each tick, a moving
 * car checks the ones near it:
 *
 * <ul>
 *   <li>a <b>red-light camera</b> fines a car that crosses the line through a light (heading toward the light's face,
 *       within {@link #LIGHT_REACH} blocks of it across the road) while it shows red;</li>
 *   <li>a <b>speed camera</b> fines a car going more than a little over a sign's limit within {@link #SPEED_REACH}
 *       blocks of it.</li>
 * </ul>
 *
 * <p>The fine comes out of the car's owner's bank account (what they have, if it's less) and goes to the camera's
 * owner. The camera's owner's own cars, cars with nobody driving, and police cars and ambulances with their sirens on
 * aren't fined, and a car is fined at most once a minute by each kind of camera.
 */
public final class TrafficCameras {
    /** How far across the road from a light, either way, its camera watches. */
    public static final double LIGHT_REACH = 8.0;
    /** How far from a speed sign its camera watches. */
    public static final double SPEED_REACH = 12.0;
    /** How long before a car can be fined again by the same kind of camera, in ticks. */
    public static final long COOLDOWN = 1200;

    private static final Map<Level, Set<BlockPos>> CAMERAS = new WeakHashMap<>();

    private TrafficCameras() {
    }

    static synchronized void add(OwnedRoadsideBlockEntity camera) {
        if (camera.getLevel() != null && !camera.getLevel().isClientSide) {
            CAMERAS.computeIfAbsent(camera.getLevel(), level -> new HashSet<>()).add(camera.getBlockPos().immutable());
        }
    }

    static synchronized void remove(OwnedRoadsideBlockEntity camera) {
        if (camera.getLevel() != null) {
            Set<BlockPos> set = CAMERAS.get(camera.getLevel());
            if (set != null) {
                set.remove(camera.getBlockPos());
            }
        }
    }

    private static synchronized List<BlockPos> near(Level level, Vec3 at, double reach) {
        Set<BlockPos> set = CAMERAS.get(level);
        if (set == null || set.isEmpty()) {
            return Collections.emptyList();
        }
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos pos : set) {
            if (pos.getCenter().distanceToSqr(at) <= reach * reach) {
                out.add(pos);
            }
        }
        return out;
    }

    /**
     * The car moved from {@code from} to {@code to} this tick, at {@code speed} blocks per tick (averaged over the last
     * half second). Server side.
     */
    public static void check(CarEntity car, Vec3 from, Vec3 to, double speed) {
        Level level = car.level();
        if (car.getControllingPassenger() == null || car.sirenOn() || from.distanceToSqr(to) < 1.0E-6) {
            return;
        }
        // Someone on their driving test fails it for running any red light or passing any speed sign too fast.
        ServerPlayer examinee = car.getControllingPassenger() instanceof ServerPlayer driver && DrivingTests.taking(driver) ? driver : null;
        if (car.owner() == null && examinee == null) {
            return;
        }
        long now = level.getGameTime();
        for (BlockPos pos : near(level, to, Math.max(LIGHT_REACH + 4.0, SPEED_REACH))) {
            if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                if (examinee != null && crossedOnRed(light, from, to)) {
                    DrivingTests.caught(examinee, true);
                }
                if (car.owner() != null && now - car.lastRedLightFine >= COOLDOWN && ranRed(light, from, to)) {
                    car.lastRedLightFine = now;
                    fine(light, car, Component.translatable("traffic.reikocars.red_light_ticket", car.describe(), pos.getX(), pos.getY(), pos.getZ()),
                            "reikocars.red_light_fine");
                }
            } else if (level.getBlockEntity(pos) instanceof SpeedSignBlockEntity sign) {
                double over = sign.inUnit(speed);
                if (examinee != null && to.distanceToSqr(pos.getCenter()) <= SPEED_REACH * SPEED_REACH && over > sign.limit() + sign.tolerance()) {
                    DrivingTests.caught(examinee, false);
                }
                if (car.owner() != null && now - car.lastSpeedingFine >= COOLDOWN && sign.fine() > 0 && sign.owner() != null
                        && to.distanceToSqr(pos.getCenter()) <= SPEED_REACH * SPEED_REACH && over > sign.limit() + sign.tolerance()) {
                    car.lastSpeedingFine = now;
                    Component limit = Component.translatable(sign.kmh() ? "traffic.reikocars.kmh" : "traffic.reikocars.mph", sign.limit());
                    fine(sign, car, Component.translatable("traffic.reikocars.speeding_ticket", car.describe(), Math.round(over), limit,
                            pos.getX(), pos.getY(), pos.getZ()), "reikocars.speeding_fine");
                }
            }
        }
    }

    /** Whether a car going from {@code from} to {@code to} crossed {@code light}'s line toward it while it showed red, on its camera. */
    public static boolean ranRed(TrafficLightBlockEntity light, Vec3 from, Vec3 to) {
        return light.owner() != null && light.fine() > 0 && crossedOnRed(light, from, to);
    }

    /** Whether a car going from {@code from} to {@code to} crossed {@code light}'s line toward it while it showed red. */
    public static boolean crossedOnRed(TrafficLightBlockEntity light, Vec3 from, Vec3 to) {
        Level level = light.getLevel();
        BlockState state = light.getBlockState();
        if (level == null || !state.hasProperty(TrafficLightBlock.FACING) || TrafficLightBlock.signal(level, state) != Signal.RED) {
            return false;
        }
        Direction facing = state.getValue(TrafficLightBlock.FACING);
        Vec3 c = light.getBlockPos().getCenter();
        double nx = facing.getStepX();
        double nz = facing.getStepZ();
        double before = (from.x - c.x) * nx + (from.z - c.z) * nz;
        double after = (to.x - c.x) * nx + (to.z - c.z) * nz;
        double across = Math.abs((to.x - c.x) * -nz + (to.z - c.z) * nx);
        return before > 0 && after <= 0 && across <= LIGHT_REACH && Math.abs(to.y - c.y) <= 6.0;
    }

    /** Fines {@code car}'s owner for {@code camera}'s owner. */
    static void fine(OwnedRoadsideBlockEntity camera, CarEntity car, Component what, String reason) {
        Level level = camera.getLevel();
        MinecraftServer server = level == null ? null : level.getServer();
        UUID payer = car.owner();
        UUID payee = camera.owner();
        long fine = camera.fine();
        if (server == null || payer == null || payee == null || payer.equals(payee) || fine <= 0) {
            return;
        }
        EconomyApi eco = EconomyApi.get();
        long pay = Math.min(fine, Math.max(0, eco.getBalance(server, payer)));
        if (pay > 0) {
            TransactionResult result = eco.transfer(server, payer, payee, pay, reason);
            if (!result.isSuccess()) {
                pay = 0;
            }
        }
        camera.recordTicket(pay);
        BlockPos at = camera.getBlockPos();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLASH, at.getX() + 0.5, at.getY() + 0.7, at.getZ() + 0.5, 1, 0, 0, 0, 0);
        }
        level.playSound(null, at, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0F, 1.8F);
        Component line = pay >= fine
                ? Component.translatable("traffic.reikocars.fined", what, eco.format(fine))
                : Component.translatable("traffic.reikocars.fined_short", what, eco.format(fine), eco.format(pay));
        ServerPlayer owner = server.getPlayerList().getPlayer(payer);
        if (owner != null) {
            owner.sendSystemMessage(line.copy().withStyle(ChatFormatting.GOLD));
        }
        if (car.getControllingPassenger() instanceof ServerPlayer driver && driver != owner) {
            driver.sendSystemMessage(line.copy().withStyle(ChatFormatting.GOLD));
        }
        if (car.getControllingPassenger() instanceof ServerPlayer driver) {
            com.reiko.cars.advancement.CarAdvancements.award(driver, "camera");
        }
        ServerPlayer cameraOwner = server.getPlayerList().getPlayer(payee);
        if (cameraOwner != null) {
            cameraOwner.sendSystemMessage(Component.translatable("traffic.reikocars.camera_owner", car.ownerName(), what, eco.format(pay)));
        }
        LicenseService.cameraCaught(car, payee, "reikocars.red_light_fine".equals(reason));
    }
}
