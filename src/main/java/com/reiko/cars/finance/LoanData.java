package com.reiko.cars.finance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/** Every car loan being paid off, kept with the world. */
public final class LoanData extends SavedData {
    private static final String NAME = "reikocars_car_loans";
    private final Map<UUID, Loan> loans = new LinkedHashMap<>();

    public static LoanData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(LoanData::new, LoanData::load), NAME);
    }

    @Nullable
    public Loan get(UUID id) {
        return loans.get(id);
    }

    void add(Loan loan) {
        loans.put(loan.id, loan);
        setDirty();
    }

    void remove(UUID id) {
        if (loans.remove(id) != null) {
            setDirty();
        }
    }

    List<Loan> all() {
        return new ArrayList<>(loans.values());
    }

    /** {@code borrower}'s loans. */
    public List<Loan> of(UUID borrower) {
        return loans.values().stream().filter(loan -> loan.borrower.equals(borrower)).toList();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Loan loan : loans.values()) {
            list.add(loan.save());
        }
        tag.put("Loans", list);
        return tag;
    }

    private static LoanData load(CompoundTag tag, HolderLookup.Provider registries) {
        LoanData data = new LoanData();
        for (Tag t : tag.getList("Loans", Tag.TAG_COMPOUND)) {
            Loan loan = Loan.load((CompoundTag) t);
            if (loan != null) {
                data.loans.put(loan.id, loan);
            }
        }
        return data;
    }
}
