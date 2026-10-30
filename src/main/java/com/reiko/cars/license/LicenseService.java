package com.reiko.cars.license;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.dmv.DrivingTests;
import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.rental.RentalService;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.api.TransactionResult;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Driver's licenses. A player gets one by passing the driving test at a DMV. Red-light and speed cameras put penalty
 * points on the license of whoever was driving ({@code licenses.redLightPoints}, {@code speedingPoints}); points drop
 * off after {@code licenses.pointsLastDays}, and reaching {@code licenses.maxPoints} suspends it for
 * {@code licenses.suspensionDays}. Without a valid license (none, or suspended) a driver a camera catches pays
 * {@code licenses.unlicensedFine} on top, and police can ticket them; with {@code licenses.required} they can't drive
 * at all, except on their driving test.
 */
public final class LicenseService {
    public static final long DAY = 24_000;

    public enum Status { NONE, VALID, SUSPENDED }

    private LicenseService() {
    }

    public static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    public static boolean required() {
        return CarsConfig.get(CarsConfig.LICENSE_REQUIRED);
    }

    /** The fine for driving without a valid license, in cents (0: none). */
    public static long unlicensedFine() {
        return Math.round(CarsConfig.get(CarsConfig.UNLICENSED_FINE) * 100);
    }

    public static int maxPoints() {
        return CarsConfig.get(CarsConfig.MAX_POINTS);
    }

    /** When the oldest points that still count were got. */
    private static long pointsSince(long now) {
        return now - CarsConfig.get(CarsConfig.POINTS_DAYS) * DAY;
    }

    @Nullable
    public static DriverLicense of(MinecraftServer server, UUID player) {
        return LicenseData.get(server).of(player);
    }

    public static Status status(MinecraftServer server, UUID player) {
        DriverLicense license = of(server, player);
        return license == null ? Status.NONE : license.suspended(now(server)) ? Status.SUSPENDED : Status.VALID;
    }

    public static boolean valid(ServerPlayer player) {
        return status(player.server, player.getUUID()) == Status.VALID;
    }

    /** Whether {@code player} may drive: they hold a valid license, licenses aren't required, or they're on their test. */
    public static boolean mayDrive(ServerPlayer player) {
        return !required() || valid(player) || DrivingTests.taking(player);
    }

    /** The points on {@code license} now. */
    public static int points(MinecraftServer server, DriverLicense license) {
        return license.points(pointsSince(now(server)));
    }

    /** Issues {@code player} a license (on passing the test) and hands them the card. */
    public static DriverLicense issue(ServerPlayer player, String issuedBy) {
        LicenseData data = LicenseData.get(player.server);
        DriverLicense license = data.of(player.getUUID());
        if (license == null) {
            license = data.issue(player.getUUID(), player.getGameProfile().getName(), now(player.server), issuedBy, player.getRandom());
        }
        give(player, license);
        return license;
    }

    /** The card for {@code license}, as it stands. */
    public static ItemStack card(MinecraftServer server, DriverLicense license) {
        ItemStack stack = new ItemStack(ModItems.DRIVERS_LICENSE.get());
        stack.set(ModDataComponents.LICENSE.get(), cardOf(server, license));
        return stack;
    }

    private static LicenseCard cardOf(MinecraftServer server, DriverLicense license) {
        return new LicenseCard(license.holder(), license.name(), license.number(), points(server, license), license.suspendedUntil());
    }

    /** Puts a card for {@code license} in {@code player}'s inventory (or at their feet). */
    public static void give(ServerPlayer player, DriverLicense license) {
        ItemStack card = card(player.server, license);
        if (!player.getInventory().add(card)) {
            player.drop(card, false);
        }
    }

    /** Brings the cards its holder carries up to date with {@code license}. */
    public static void refreshCards(MinecraftServer server, DriverLicense license) {
        ServerPlayer holder = server.getPlayerList().getPlayer(license.holder());
        if (holder == null) {
            return;
        }
        LicenseCard card = cardOf(server, license);
        var inventory = holder.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            LicenseCard old = stack.get(ModDataComponents.LICENSE.get());
            if (old != null && old.holder().equals(license.holder()) && !old.equals(card)) {
                stack.set(ModDataComponents.LICENSE.get(), card);
            }
        }
    }

    /**
     * {@code points} more on {@code driver}'s license, for {@code reason} (a lang key): a suspension if that takes them
     * to the limit. Nothing if they have no license, or it's suspended already.
     */
    public static void addPoints(MinecraftServer server, UUID driver, int points, String reason) {
        DriverLicense license = of(server, driver);
        long now = now(server);
        if (license == null || points <= 0 || license.suspended(now)) {
            return;
        }
        license.expire(pointsSince(now));
        license.addOffence(new DriverLicense.Offence(now, points, reason));
        int total = license.points(pointsSince(now));
        ServerPlayer player = server.getPlayerList().getPlayer(driver);
        int max = maxPoints();
        if (total >= max) {
            long days = CarsConfig.get(CarsConfig.SUSPENSION_DAYS);
            license.suspend(now + days * DAY);
            license.clearOffences();
            if (player != null) {
                player.sendSystemMessage(Component.translatable("license.reikocars.suspended", total, max, days).withStyle(ChatFormatting.RED));
            }
        } else if (player != null) {
            player.sendSystemMessage(Component.translatable("license.reikocars.points", points, Component.translatable(reason), total, max)
                    .withStyle(ChatFormatting.GOLD));
        }
        LicenseData.get(server).setDirty();
        refreshCards(server, license);
    }

    /**
     * A camera owned by {@code cameraOwner} fined {@code car} for running a red light or speeding: points for its
     * driver, or, if they have no valid license, the unlicensed driving fine on top, paid to the camera's owner.
     */
    public static void cameraCaught(CarEntity car, UUID cameraOwner, boolean redLight) {
        MinecraftServer server = car.getServer();
        if (server == null || !(car.getControllingPassenger() instanceof ServerPlayer driver)) {
            return;
        }
        Status status = status(server, driver.getUUID());
        if (status == Status.VALID) {
            addPoints(server, driver.getUUID(), CarsConfig.get(redLight ? CarsConfig.RED_LIGHT_POINTS : CarsConfig.SPEEDING_POINTS),
                    redLight ? "license.reikocars.reason.red_light" : "license.reikocars.reason.speeding");
            return;
        }
        long fine = unlicensedFine();
        if (fine <= 0 || driver.getUUID().equals(cameraOwner)) {
            return;
        }
        EconomyApi eco = EconomyApi.get();
        long pay = Math.min(fine, Math.max(0, eco.getBalance(server, driver.getUUID())));
        if (pay > 0) {
            TransactionResult result = eco.transfer(server, driver.getUUID(), cameraOwner, pay, "reikocars.unlicensed_fine");
            if (!result.isSuccess()) {
                pay = 0;
            }
        }
        driver.sendSystemMessage(Component.translatable(status == Status.SUSPENDED ? "license.reikocars.camera_suspended" : "license.reikocars.camera_unlicensed",
                eco.format(fine), eco.format(pay)).withStyle(ChatFormatting.GOLD));
    }

    /** {@code player}'s license in a few words: "valid, 3 of 12 points", "suspended for 1h 20m", "none". */
    public static Component statusLine(MinecraftServer server, UUID player) {
        DriverLicense license = of(server, player);
        if (license == null) {
            return Component.translatable("license.reikocars.status.none");
        }
        long now = now(server);
        if (license.suspended(now)) {
            return Component.translatable("license.reikocars.status.suspended", license.number(), RentalService.timeLeft(license.suspendedUntil() - now));
        }
        return Component.translatable("license.reikocars.status.valid", license.number(), points(server, license), maxPoints());
    }

    /** Lifts a suspension and clears the points (for admins and tests). */
    public static void clear(MinecraftServer server, UUID player) {
        DriverLicense license = of(server, player);
        if (license != null) {
            license.clearOffences();
            license.suspend(0);
            LicenseData.get(server).setDirty();
            refreshCards(server, license);
        }
    }
}
