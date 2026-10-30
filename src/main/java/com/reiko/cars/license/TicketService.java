package com.reiko.cars.license;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.job.CarJobs;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.api.TransactionResult;
import com.reiko.economy.jobs.JobService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Police plate checks with a Ticket Book. An officer (a player in the Police Officer job, or an admin) right-clicks a
 * car to see its plate, owner and who's driving and their license, or a player to see their license. A driver with no
 * valid license (none, or suspended) is ticketed {@code licenses.unlicensedFine} from their bank account, at most once
 * every ten minutes; the fine leaves the economy, and the ticket pays the officer's Police Officer job.
 */
public final class TicketService {
    public static final ResourceLocation POLICE_JOB = ReikoCars.id("police_officer");
    /** How long before the same driver can be ticketed again, in ticks. */
    public static final long COOLDOWN = 12_000;

    private static final Map<UUID, Long> LAST_TICKET = new HashMap<>();

    private TicketService() {
    }

    public static boolean isOfficer(ServerPlayer player) {
        return player.hasPermissions(2) || JobService.activeJobs(player).contains(POLICE_JOB);
    }

    /** {@code officer} checks {@code car}: its plate, owner and driver, and tickets the driver if they're unlicensed. */
    public static void checkCar(ServerPlayer officer, CarEntity car) {
        if (!isOfficer(officer)) {
            officer.displayClientMessage(Component.translatable("ticket.reikocars.not_officer"), true);
            return;
        }
        MinecraftServer server = officer.server;
        String plate = car.plate().isEmpty() ? "-" : car.plate();
        officer.sendSystemMessage(Component.translatable("ticket.reikocars.plate_check", plate, car.kindName(),
                car.ownerName().isEmpty() ? Component.translatable("ticket.reikocars.nobody") : Component.literal(car.ownerName()))
                .withStyle(ChatFormatting.AQUA));
        if (car.isImpounded()) {
            officer.sendSystemMessage(Component.translatable("ticket.reikocars.impounded"));
        }
        if (car.rental() != null && car.rental().active(car.level().getGameTime())) {
            officer.sendSystemMessage(Component.translatable("ticket.reikocars.rented", car.rental().renterName()));
        }
        if (car.getControllingPassenger() instanceof ServerPlayer driver) {
            officer.sendSystemMessage(Component.translatable("ticket.reikocars.driver", driver.getGameProfile().getName(),
                    LicenseService.statusLine(server, driver.getUUID())));
            if (LicenseService.status(server, driver.getUUID()) != LicenseService.Status.VALID) {
                ticket(officer, driver);
            }
        } else {
            officer.sendSystemMessage(Component.translatable("ticket.reikocars.no_driver"));
        }
    }

    /** {@code officer} checks {@code player}'s license, and tickets them if they're driving without a valid one. */
    public static void checkPlayer(ServerPlayer officer, ServerPlayer player) {
        if (!isOfficer(officer)) {
            officer.displayClientMessage(Component.translatable("ticket.reikocars.not_officer"), true);
            return;
        }
        if (player.getVehicle() instanceof CarEntity car && car.getControllingPassenger() == player) {
            checkCar(officer, car);
            return;
        }
        officer.sendSystemMessage(Component.translatable("ticket.reikocars.license_of", player.getGameProfile().getName(),
                LicenseService.statusLine(officer.server, player.getUUID())).withStyle(ChatFormatting.AQUA));
    }

    /** Tickets {@code driver} for driving without a valid license. */
    static void ticket(ServerPlayer officer, ServerPlayer driver) {
        if (driver == officer) {
            return;
        }
        long fine = LicenseService.unlicensedFine();
        if (fine <= 0) {
            return;
        }
        long now = officer.serverLevel().getGameTime();
        Long last = LAST_TICKET.get(driver.getUUID());
        if (last != null && now - last < COOLDOWN && now >= last) {
            officer.sendSystemMessage(Component.translatable("ticket.reikocars.already", driver.getGameProfile().getName()));
            return;
        }
        LAST_TICKET.put(driver.getUUID(), now);
        MinecraftServer server = officer.server;
        EconomyApi eco = EconomyApi.get();
        long pay = Math.min(fine, Math.max(0, eco.getBalance(server, driver.getUUID())));
        if (pay > 0) {
            TransactionResult result = eco.withdraw(server, driver.getUUID(), pay, "reikocars.unlicensed_ticket");
            if (!result.isSuccess()) {
                pay = 0;
            }
        }
        driver.sendSystemMessage(Component.translatable("ticket.reikocars.ticketed", officer.getGameProfile().getName(), eco.format(fine),
                eco.format(pay)).withStyle(ChatFormatting.GOLD));
        officer.sendSystemMessage(Component.translatable("ticket.reikocars.wrote", driver.getGameProfile().getName(), eco.format(pay))
                .withStyle(ChatFormatting.GOLD));
        officer.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        CarJobs.work(officer, CarJobs.UNLICENSED_TICKET, 1);
    }

    /** Forgets every ticket's cooldown (for tests). */
    public static void forget() {
        LAST_TICKET.clear();
    }
}
