package com.reiko.cars.insurance;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.api.TransactionResult;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Car insurance. A car's owner parks it by an insurance office and buys cover, a term at a time, paying the premium
 * (plus sales tax, by card or cash) to the office's owner. While it's insured, a paid repair at a Repair Bench that
 * includes body work (crash damage) is a claim: the insurer pays back the body work's cost above the deductible, up to
 * the claim limit, from their bank account to whoever paid the bill. Wear (oil, engine, tires, brakes) isn't covered.
 * Cover lapses when it runs out, or if the car changes hands.
 */
public final class InsuranceService {
    /** How far ahead cover can be bought, in terms. */
    public static final int MAX_TERMS = 4;

    private InsuranceService() {
    }

    public static double reach() {
        return CarsConfig.get(CarsConfig.INSURANCE_REACH);
    }

    /** How long one term of cover lasts, in ticks. */
    public static long termTicks() {
        return CarsConfig.get(CarsConfig.INSURANCE_TERM_DAYS) * 24_000L;
    }

    /** {@code player}'s cars parked within reach of {@code office}, nearest first. */
    public static List<CarEntity> carsAt(InsuranceOfficeBlockEntity office, ServerPlayer player) {
        if (office.getLevel() == null) {
            return List.of();
        }
        double reach = reach();
        var center = office.getBlockPos().getCenter();
        return office.getLevel().getEntitiesOfClass(CarEntity.class, new AABB(office.getBlockPos()).inflate(reach),
                        car -> car.isAlive() && car.isOwner(player) && car.distanceToSqr(center) <= reach * reach)
                .stream().sorted(Comparator.comparingDouble(car -> car.distanceToSqr(center))).toList();
    }

    /**
     * {@code player} buys a term of cover for {@code car} at {@code office}: a new policy, or more time on one from this
     * insurer. Cover from another insurer is replaced.
     *
     * @param expected the premium the screen showed, before tax
     */
    public static ShopActionResult buy(ServerPlayer player, InsuranceOfficeBlockEntity office, @Nullable CarEntity car, long expected,
                                       Payment payment) {
        if (office.owner() == null) {
            return ShopActionResult.fail("insurance.reikocars.no_insurer");
        }
        if (office.isOwner(player)) {
            return ShopActionResult.fail("insurance.reikocars.own_office");
        }
        if (car == null || !carsAt(office, player).contains(car)) {
            return ShopActionResult.fail("insurance.reikocars.no_car", (int) reach());
        }
        long premium = office.premium();
        EconomyApi eco = EconomyApi.get();
        if (premium != expected) {
            return ShopActionResult.fail("insurance.reikocars.premium_changed", eco.format(premium + EconomyConfig.taxRates().salesTax(premium)));
        }
        long now = player.serverLevel().getGameTime();
        Policy current = car.activePolicy();
        boolean extending = current != null && current.insurer().equals(office.owner());
        long from = extending ? current.until() : now;
        long until = from + termTicks();
        if (until - now > MAX_TERMS * termTicks()) {
            return ShopActionResult.fail("insurance.reikocars.too_far_ahead", MAX_TERMS * CarsConfig.get(CarsConfig.INSURANCE_TERM_DAYS));
        }
        long tax = premium > 0 ? EconomyConfig.taxRates().salesTax(premium) : 0;
        if (premium + tax > Money.MAX_BALANCE) {
            return ShopActionResult.fail("insurance.reikocars.bad_price");
        }
        ShopPayments.Charge charge = null;
        if (premium > 0) {
            Component description = Component.translatable("insurance.reikocars.bill_line", car.kindName(), office.ownerName());
            charge = ShopPayments.charge(player, payment, premium, tax, office.owner(), "reikocars.insurance", description);
            if (!charge.success()) {
                return ShopActionResult.fail(charge.message());
            }
        }
        car.setPolicy(new Policy(office.owner(), office.ownerName(), player.getUUID(), until, office.deductible(), office.limit(),
                Optional.of(GlobalPos.of(player.serverLevel().dimension(), office.getBlockPos()))));
        office.recordPolicy(premium);
        player.serverLevel().playSound(null, office.getBlockPos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
        int days = car.policy().daysLeft(now);
        Component line = Component.translatable(extending ? "insurance.reikocars.extended" : "insurance.reikocars.insured",
                car.kindName(), days, eco.format(premium + tax));
        return charge == null ? ShopActionResult.ok(line) : ShopActionResult.ok(ShopPayments.receipt(line, payment, charge));
    }

    /**
     * A paid repair on {@code car} included {@code bodyCost} of body work: if it's insured, its insurer pays back the
     * cost above the deductible, up to the limit, to {@code payer}. Tells the payer what happened, and returns the
     * amount paid back.
     */
    public static long claim(ServerPlayer payer, CarEntity car, long bodyCost) {
        Policy policy = car.activePolicy();
        if (policy == null || bodyCost <= 0 || policy.insurer().equals(payer.getUUID())) {
            return 0;
        }
        long payout = policy.payout(bodyCost);
        EconomyApi eco = EconomyApi.get();
        if (payout <= 0) {
            payer.sendSystemMessage(Component.translatable("insurance.reikocars.under_deductible", eco.format(bodyCost), eco.format(policy.deductible())));
            return 0;
        }
        MinecraftServer server = payer.server;
        if (eco.getBalance(server, policy.insurer()) < payout) {
            payer.sendSystemMessage(Component.translatable("insurance.reikocars.claim_unpaid", policy.insurerName(), eco.format(payout)));
            notifyInsurer(server, policy, Component.translatable("insurance.reikocars.claim_missed", car.ownerName(), eco.format(payout)));
            return 0;
        }
        TransactionResult result = eco.transfer(server, policy.insurer(), payer.getUUID(), payout, "reikocars.insurance_claim");
        if (!result.isSuccess()) {
            payer.sendSystemMessage(Component.translatable("insurance.reikocars.claim_unpaid", policy.insurerName(), eco.format(payout)));
            return 0;
        }
        payer.sendSystemMessage(Component.translatable("insurance.reikocars.claim_paid", policy.insurerName(), eco.format(payout),
                car.kindName(), eco.format(policy.deductible())));
        notifyInsurer(server, policy, Component.translatable("insurance.reikocars.claim_made", car.ownerName(), car.kindName(), eco.format(payout)));
        office(server, policy).ifPresent(office -> office.recordClaim(payout));
        return payout;
    }

    private static void notifyInsurer(MinecraftServer server, Policy policy, Component message) {
        ServerPlayer insurer = server.getPlayerList().getPlayer(policy.insurer());
        if (insurer != null) {
            insurer.sendSystemMessage(message);
        }
    }

    /** The office a policy was bought at, if it's still there (and loaded) and still the insurer's. */
    private static Optional<InsuranceOfficeBlockEntity> office(MinecraftServer server, Policy policy) {
        if (policy.office().isEmpty()) {
            return Optional.empty();
        }
        GlobalPos at = policy.office().get();
        ServerLevel level = server.getLevel(at.dimension());
        if (level == null || !level.isLoaded(at.pos()) || !(level.getBlockEntity(at.pos()) instanceof InsuranceOfficeBlockEntity office)
                || !policy.insurer().equals(office.owner())) {
            return Optional.empty();
        }
        return Optional.of(office);
    }

    /** The office's owner sets its terms. */
    public static ShopActionResult setTerms(ServerPlayer player, InsuranceOfficeBlockEntity office, String premiumText, String deductibleText,
                                            String limitText) {
        if (!office.isOwner(player) && !player.hasPermissions(2)) {
            return ShopActionResult.fail("insurance.reikocars.owner_only", office.ownerName());
        }
        OptionalLong premium = parse(premiumText);
        OptionalLong deductible = parse(deductibleText);
        OptionalLong limit = parse(limitText);
        if (premium.isEmpty() || deductible.isEmpty() || limit.isEmpty()) {
            return ShopActionResult.fail("insurance.reikocars.bad_price");
        }
        office.setTerms(premium.getAsLong(), deductible.getAsLong(), limit.getAsLong());
        EconomyApi eco = EconomyApi.get();
        return ShopActionResult.ok(Component.translatable("insurance.reikocars.terms_set", eco.format(office.premium()),
                eco.format(office.deductible()), eco.format(office.limit())));
    }

    private static OptionalLong parse(String text) {
        OptionalLong value = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        return value.isPresent() && value.getAsLong() >= 0 && value.getAsLong() <= Money.MAX_BALANCE / 100 ? value : OptionalLong.empty();
    }
}
