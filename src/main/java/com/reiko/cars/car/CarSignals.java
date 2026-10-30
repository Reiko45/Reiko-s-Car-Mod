package com.reiko.cars.car;

import com.reiko.cars.config.CarsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Turn signals, hazards and the car alarm.
 *
 * <p>The driver signals left or right with the arrow keys; a signal goes off by itself once the car has turned and is
 * running straight again. The down arrow works the hazards, which also come on by themselves when a car breaks down.
 *
 * <p>A locked car's alarm goes off when someone without a key hits it or tries its doors: its horn blares and its
 * lights flash for half a minute (the {@code alarmSeconds} setting), and its owner hears about it. Its owner, or anyone
 * with its key, stops it with L or the key's lock button, or by getting in.
 */
public final class CarSignals {
    public static final byte OFF = 0;
    public static final byte LEFT = 1;
    public static final byte RIGHT = 2;
    public static final byte HAZARDS = 3;
    /** A blink: the lamps are lit this many ticks, then dark as many. */
    public static final int BLINK = 8;
    /** How far, in degrees, the car turns the way it's signalling before the signal can cancel itself. */
    public static final float CANCEL_AFTER = 40.0F;
    /** How long it then runs straight, in ticks, before the signal goes off. */
    public static final int STRAIGHT_TICKS = 10;

    private CarSignals() {
    }

    /** The driver's arrow keys: {@code which} on, or off if it already was. */
    public static void toggle(ServerPlayer driver, CarEntity car, byte which) {
        if (car.getControllingPassenger() != driver || which < LEFT || which > HAZARDS) {
            return;
        }
        car.setSignal(car.signal() == which ? OFF : which);
    }

    /** Every server tick: the alarm running out or stopping, hazards on a breakdown, and signals cancelling. */
    public static void serverTick(CarEntity car) {
        long now = car.level().getGameTime();
        long until = car.alarmUntil();
        if (until != 0L && (now >= until || !car.isLocked()
                || car.getControllingPassenger() instanceof Player driver && car.opensLocks(driver))) {
            car.setAlarmUntil(0L); // run out, unlocked, or someone with the key got in
        }

        boolean broken = car.isBrokenDown();
        if (broken && !car.wasBrokenDown) {
            car.setSignal(HAZARDS);
        }
        car.wasBrokenDown = broken;

        float yaw = car.getYRot();
        float turned = Mth.wrapDegrees(yaw - car.signalYaw);
        car.signalYaw = yaw;
        byte signal = car.signal();
        if (signal != LEFT && signal != RIGHT) {
            return;
        }
        // Turning left lowers the yaw. Count the turn the signal's way; turning back the other way takes it off again.
        car.signalTurned = Math.max(0.0F, car.signalTurned + (signal == LEFT ? -turned : turned));
        if (car.signalTurned < CANCEL_AFTER) {
            return;
        }
        if (Math.abs(turned) < 0.75F) {
            if (++car.signalStraight >= STRAIGHT_TICKS) {
                car.setSignal(OFF);
            }
        } else {
            car.signalStraight = 0;
        }
    }

    /** Whether one side's signal lamps are blinking (they're lit for half of each blink: see {@link #lit}). */
    public static boolean blinking(CarEntity car, boolean left) {
        byte signal = car.signal();
        return car.alarmOn() || signal == HAZARDS || signal == (left ? LEFT : RIGHT);
    }

    /** Whether the blinking lamps are lit this tick, in a car's own rhythm. */
    public static boolean blinkOn(CarEntity car) {
        return Math.floorMod(car.level().getGameTime() + car.getId(), 2 * BLINK) < BLINK;
    }

    /** Whether one side's signal lamps are lit right now. */
    public static boolean lit(CarEntity car, boolean left) {
        return blinking(car, left) && blinkOn(car);
    }

    // ---- the alarm ----

    /** {@code culprit}, who has no key, hit the car or tried its doors. Server side. */
    public static void triggerAlarm(CarEntity car, Player culprit) {
        int seconds = CarsConfig.get(CarsConfig.ALARM_SECONDS);
        if (seconds <= 0 || car.owner() == null || !car.isLocked() || car.level().isClientSide) {
            return;
        }
        boolean sounding = car.alarmOn();
        car.setAlarmUntil(car.level().getGameTime() + seconds * 20L);
        MinecraftServer server = car.level().getServer();
        if (sounding || server == null) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(car.owner());
        if (owner != null && owner != culprit) {
            BlockPos at = car.blockPosition();
            owner.sendSystemMessage(Component.translatable("car.reikocars.alarm_going_off", car.kindName(), at.getX(), at.getY(), at.getZ(),
                    culprit.getDisplayName()).withStyle(ChatFormatting.RED));
        }
    }

    public static void silenceAlarm(CarEntity car) {
        car.setAlarmUntil(0L);
    }
}
