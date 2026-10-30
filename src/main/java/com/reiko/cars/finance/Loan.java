package com.reiko.cars.finance;

import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

/**
 * A car loan: a car bought at a dealership for a down payment, the rest (with interest) owed to the dealer and paid
 * from the borrower's bank account a day at a time. See {@link LoanService}.
 */
public final class Loan {
    public enum State {
        /** Being paid off. */
        ACTIVE,
        /** Too many payments missed: the car's been (or is being) taken to an impound lot, to be got back by paying up. */
        REPOSSESSED,
        /** Not got back in time: the car's the lender's, as soon as it's found. */
        FORECLOSED
    }

    final UUID id;
    final UUID car;
    final String carKey;
    final UUID borrower;
    final String borrowerName;
    final UUID lender;
    final String lenderName;
    final GlobalPos dealership;
    final long installment;
    long remaining;
    long nextDue;
    int missed;
    State state = State.ACTIVE;
    /** When a repossessed car becomes the lender's, if it isn't got back first. */
    long deadline;
    /** Whether the repossessed car has been taken yet (it's taken when it's next loaded, if it wasn't then). */
    boolean taken;

    Loan(UUID id, UUID car, String carKey, UUID borrower, String borrowerName, UUID lender, String lenderName, GlobalPos dealership,
         long installment, long remaining, long nextDue) {
        this.id = id;
        this.car = car;
        this.carKey = carKey;
        this.borrower = borrower;
        this.borrowerName = borrowerName;
        this.lender = lender;
        this.lenderName = lenderName;
        this.dealership = dealership;
        this.installment = installment;
        this.remaining = remaining;
        this.nextDue = nextDue;
    }

    public UUID id() {
        return id;
    }

    public UUID car() {
        return car;
    }

    public UUID borrower() {
        return borrower;
    }

    public UUID lender() {
        return lender;
    }

    public String lenderName() {
        return lenderName;
    }

    /** Cents still owed. */
    public long remaining() {
        return remaining;
    }

    /** Cents a day. */
    public long installment() {
        return installment;
    }

    public long nextDue() {
        return nextDue;
    }

    public int missed() {
        return missed;
    }

    public State state() {
        return state;
    }

    /** The missed payments, owed before a repossessed car is given back. */
    public long arrears() {
        return Math.min(remaining, installment * missed);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putUUID("Car", car);
        tag.putString("CarKey", carKey);
        tag.putUUID("Borrower", borrower);
        tag.putString("BorrowerName", borrowerName);
        tag.putUUID("Lender", lender);
        tag.putString("LenderName", lenderName);
        GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, dealership).result().ifPresent(saved -> tag.put("Dealership", saved));
        tag.putLong("Installment", installment);
        tag.putLong("Remaining", remaining);
        tag.putLong("NextDue", nextDue);
        tag.putInt("Missed", missed);
        tag.putString("State", state.name());
        tag.putLong("Deadline", deadline);
        tag.putBoolean("Taken", taken);
        return tag;
    }

    static Loan load(CompoundTag tag) {
        GlobalPos dealership = GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Dealership")).result().orElse(null);
        if (dealership == null || !tag.hasUUID("Id") || !tag.hasUUID("Car") || !tag.hasUUID("Borrower") || !tag.hasUUID("Lender")) {
            return null;
        }
        Loan loan = new Loan(tag.getUUID("Id"), tag.getUUID("Car"), tag.getString("CarKey"), tag.getUUID("Borrower"), tag.getString("BorrowerName"),
                tag.getUUID("Lender"), tag.getString("LenderName"), dealership, tag.getLong("Installment"), tag.getLong("Remaining"),
                tag.getLong("NextDue"));
        loan.missed = tag.getInt("Missed");
        try {
            loan.state = State.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException e) {
            loan.state = State.ACTIVE;
        }
        loan.deadline = tag.getLong("Deadline");
        loan.taken = tag.getBoolean("Taken");
        return loan;
    }
}
