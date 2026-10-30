package com.reiko.cars.finance;

import com.reiko.cars.car.CarDeliveries;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.impound.Impound;
import com.reiko.cars.impound.ImpoundOfficeBlockEntity;
import com.reiko.cars.impound.ImpoundRegistry;
import com.reiko.cars.key.CarKeys;
import com.reiko.cars.sale.DealershipBlockEntity;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.api.TransactionResult;
import com.reiko.economy.shop.ShopActionResult;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Car loans. At a dealership, a car for sale on the lot can be bought on finance: a down payment now (a share of the
 * price, {@code loans.downPayment}, plus sales tax on the whole price), and the rest with interest ({@code
 * loans.interestRate}) paid from the buyer's bank account to the dealer once a Minecraft day, in {@code loans.days}
 * payments. Until it's paid off, the car can't be sold, traded in or picked up; it can be paid off early at the
 * dealership.
 *
 * <p>A payment the bank account can't cover is missed. After {@code loans.missedPaymentsToRepossess} in a row the car
 * is repossessed: it's taken to the impound lot nearest the dealership (straight away if it's loaded, or as soon as it
 * is), where its owner can get it back by paying the missed payments and the lot's fee, which puts the loan back on
 * track. Not got back within {@code loans.reclaimDays} days, it becomes the dealer's. With no impound lot anywhere,
 * it's the dealer's at once, back on their lot.
 */
public final class LoanService {
    public static final long DAY = 24_000;

    private LoanService() {
    }

    /** What financing a car at {@code price} (cents, before tax) comes to. */
    public record Terms(long price, long down, long total, long installment, int days) {
    }

    public static Terms terms(long price) {
        long down = (long) Math.ceil(price * CarsConfig.get(CarsConfig.LOAN_DOWN_PAYMENT));
        long financed = Math.max(0, price - down);
        long total = Math.round(financed * (1.0 + CarsConfig.get(CarsConfig.LOAN_INTEREST)));
        int days = CarsConfig.get(CarsConfig.LOAN_DAYS);
        long installment = total == 0 ? 0 : (total + days - 1) / days;
        return new Terms(price, down, total, installment, days);
    }

    /** Whether {@code car} can be bought on finance: on sale on its owner's dealership lot, with loans on. */
    public static boolean canFinance(CarEntity car, @Nullable DealershipBlockEntity lot) {
        return CarsConfig.get(CarsConfig.LOANS_ENABLED) && lot != null && car.isForSale() && car.salePrice() > 0 && terms(car.salePrice()).total() > 0;
    }

    private static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    /** Starts a loan for {@code buyer}'s just-bought {@code car}, owed to the dealer of {@code lot}. */
    public static Loan start(ServerPlayer buyer, CarEntity car, DealershipBlockEntity lot, Terms terms) {
        MinecraftServer server = buyer.server;
        Loan loan = new Loan(UUID.randomUUID(), car.getUUID(), car.nameKey(), buyer.getUUID(), buyer.getGameProfile().getName(), lot.owner(),
                lot.ownerName(), GlobalPos.of(lot.getLevel().dimension(), lot.getBlockPos()), terms.installment(), terms.total(), now(server) + DAY);
        LoanData.get(server).add(loan);
        car.setLoanId(loan.id);
        return loan;
    }

    @Nullable
    public static Loan loanOf(CarEntity car) {
        return car.loanId() == null || car.getServer() == null ? null : LoanData.get(car.getServer()).get(car.loanId());
    }

    /** Cents still owed on {@code car}'s loan, or -1 if it has none. */
    public static long owed(CarEntity car) {
        Loan loan = loanOf(car);
        return loan == null ? -1 : loan.remaining;
    }

    @Nullable
    private static ServerPlayer online(MinecraftServer server, UUID player) {
        return server.getPlayerList().getPlayer(player);
    }

    private static void tell(MinecraftServer server, UUID player, Component message) {
        ServerPlayer online = online(server, player);
        if (online != null) {
            online.sendSystemMessage(message);
        }
    }

    private static Component carName(Loan loan) {
        return Component.translatable(loan.carKey);
    }

    /** The car {@code loan} is for, if it's loaded somewhere. */
    @Nullable
    private static CarEntity findCar(MinecraftServer server, Loan loan) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(loan.car);
            if (entity instanceof CarEntity car && car.isAlive()) {
                return car;
            }
        }
        return null;
    }

    // ---- every few seconds, on the server ----

    public static void tick(MinecraftServer server) {
        LoanData data = LoanData.get(server);
        long now = now(server);
        for (Loan loan : data.all()) {
            if (loan.state == Loan.State.ACTIVE && now >= loan.nextDue) {
                collect(server, data, loan);
            } else if (loan.state == Loan.State.REPOSSESSED && now >= loan.deadline) {
                loan.state = Loan.State.FORECLOSED;
                data.setDirty();
                tell(server, loan.borrower, Component.translatable("loan.reikocars.foreclosed", carName(loan), loan.lenderName));
                CarEntity car = findCar(server, loan);
                if (car != null) {
                    foreclose(car, loan, data);
                }
            }
        }
    }

    /** A day's payment falls due. */
    static void collect(MinecraftServer server, LoanData data, Loan loan) {
        EconomyApi eco = EconomyApi.get();
        long amount = Math.min(loan.installment, loan.remaining);
        loan.nextDue += DAY;
        data.setDirty();
        TransactionResult paid = eco.getBalance(server, loan.borrower) >= amount
                ? eco.transfer(server, loan.borrower, loan.lender, amount, "reikocars.car_loan") : null;
        if (paid != null && paid.isSuccess()) {
            loan.remaining -= amount;
            loan.missed = 0;
            if (loan.remaining <= 0) {
                data.remove(loan.id);
                tell(server, loan.borrower, Component.translatable("loan.reikocars.paid_off", carName(loan), loan.lenderName));
                CarEntity car = findCar(server, loan);
                if (car != null) {
                    car.setLoanId(null);
                }
            } else {
                tell(server, loan.borrower, Component.translatable("loan.reikocars.payment", eco.format(amount), carName(loan), eco.format(loan.remaining)));
            }
            return;
        }
        loan.missed++;
        int limit = CarsConfig.get(CarsConfig.LOAN_MISSES);
        if (loan.missed < limit) {
            tell(server, loan.borrower, Component.translatable("loan.reikocars.missed", eco.format(amount), carName(loan), loan.missed, limit));
            return;
        }
        loan.state = Loan.State.REPOSSESSED;
        loan.deadline = now(server) + CarsConfig.get(CarsConfig.LOAN_RECLAIM_DAYS) * DAY;
        tell(server, loan.borrower, Component.translatable("loan.reikocars.repossessed", carName(loan), loan.lenderName, eco.format(loan.arrears()),
                CarsConfig.get(CarsConfig.LOAN_RECLAIM_DAYS)));
        tell(server, loan.lender, Component.translatable("loan.reikocars.repossessed_lender", carName(loan), loan.borrowerName));
        CarEntity car = findCar(server, loan);
        if (car != null) {
            repossess(car, loan, data);
        }
    }

    /** On the server, every few seconds, for a car with a loan: a repossession or foreclosure that waited for it to load. */
    public static void carTick(CarEntity car) {
        MinecraftServer server = car.getServer();
        if (server == null || car.loanId() == null) {
            return;
        }
        LoanData data = LoanData.get(server);
        Loan loan = data.get(car.loanId());
        if (loan == null || !loan.car.equals(car.getUUID())) {
            car.setLoanId(null); // paid off (or long gone)
        } else if (loan.state == Loan.State.REPOSSESSED && !loan.taken) {
            repossess(car, loan, data);
        } else if (loan.state == Loan.State.FORECLOSED) {
            foreclose(car, loan, data);
        }
    }

    /** Takes {@code car} away to the impound lot nearest its dealership, held until the missed payments are made. */
    static void repossess(CarEntity car, Loan loan, LoanData data) {
        MinecraftServer server = car.getServer();
        loan.taken = true;
        data.setDirty();
        ImpoundRegistry.Lot lot = ImpoundRegistry.get(server).nearest(loan.dealership.dimension(), loan.dealership.pos().getCenter());
        if (lot == null) {
            // Nowhere to hold it: it's the dealer's, back on their lot.
            loan.state = Loan.State.FORECLOSED;
            foreclose(car, loan, data);
            CarDeliveries.send(car, loan.dealership.dimension(), Vec3.atBottomCenterOf(loan.dealership.pos()), car.getYRot());
            return;
        }
        car.setImpound(new Impound(lot.pos().pos(), lot.owner(), lot.ownerName(), now(server), Impound.REPOSSESSED, loan.arrears(),
                Optional.of(loan.id)));
        ServerLevel lotLevel = server.getLevel(lot.pos().dimension());
        if (lotLevel != null && lotLevel.isLoaded(lot.pos().pos()) && lotLevel.getBlockEntity(lot.pos().pos()) instanceof ImpoundOfficeBlockEntity office) {
            office.recordImpound();
        }
        CarDeliveries.send(car, lot.pos().dimension(), Vec3.atBottomCenterOf(lot.pos().pos()), car.getYRot());
    }

    /** {@code car} becomes its lender's: on new locks, the loan done with. */
    static void foreclose(CarEntity car, Loan loan, LoanData data) {
        car.setOwner(loan.lender, loan.lenderName);
        car.setLockCode(CarKeys.newCode());
        car.setLocked(true);
        car.setSalePrice(-1);
        car.setLoanId(null);
        car.setImpound(null);
        data.remove(loan.id);
    }

    /**
     * A repossessed car's owner paid the missed payments ({@code arrears}, to {@code from}, the impound lot's owner, with
     * its fee): they go on to the lender, and the loan's back on track, its next payment a day from now.
     */
    public static void arrearsPaid(MinecraftServer server, UUID loanId, @Nullable UUID from, long arrears) {
        LoanData data = LoanData.get(server);
        Loan loan = data.get(loanId);
        if (loan == null) {
            return;
        }
        if (arrears > 0 && from != null && !from.equals(loan.lender)) {
            EconomyApi.get().transfer(server, from, loan.lender, arrears, "reikocars.car_loan");
        }
        loan.remaining = Math.max(0, loan.remaining - arrears);
        loan.missed = 0;
        loan.taken = false;
        loan.state = Loan.State.ACTIVE;
        loan.nextDue = now(server) + DAY;
        if (loan.remaining <= 0) {
            data.remove(loanId);
        }
        data.setDirty();
        tell(server, loan.lender, Component.translatable("loan.reikocars.caught_up", loan.borrowerName, carName(loan)));
    }

    /** Makes {@code loanId}'s next payment, and any repossession deadline, due now: for tests. */
    public static void dueNow(MinecraftServer server, UUID loanId) {
        Loan loan = LoanData.get(server).get(loanId);
        if (loan != null) {
            loan.nextDue = now(server);
            loan.deadline = Math.min(loan.deadline, now(server));
        }
    }

    /** {@code player} pays off what's left of {@code car}'s loan, from their bank account, to the dealer. */
    public static ShopActionResult payOff(ServerPlayer player, CarEntity car) {
        MinecraftServer server = player.server;
        LoanData data = LoanData.get(server);
        Loan loan = loanOf(car);
        if (loan == null || !car.isOwner(player)) {
            return ShopActionResult.fail("loan.reikocars.no_loan");
        }
        if (loan.state != Loan.State.ACTIVE) {
            return ShopActionResult.fail("loan.reikocars.not_active");
        }
        EconomyApi eco = EconomyApi.get();
        if (eco.getBalance(server, player.getUUID()) < loan.remaining) {
            return ShopActionResult.fail("loan.reikocars.cant_afford", eco.format(loan.remaining));
        }
        TransactionResult paid = eco.transfer(server, player.getUUID(), loan.lender, loan.remaining, "reikocars.car_loan");
        if (!paid.isSuccess()) {
            return ShopActionResult.fail(Component.translatable(paid.translationKey()));
        }
        long amount = loan.remaining;
        data.remove(loan.id);
        car.setLoanId(null);
        tell(server, loan.lender, Component.translatable("loan.reikocars.lender_paid_off", player.getGameProfile().getName(), carName(loan),
                eco.format(amount)));
        return ShopActionResult.ok(Component.translatable("loan.reikocars.you_paid_off", car.kindName(), eco.format(amount)));
    }
}
