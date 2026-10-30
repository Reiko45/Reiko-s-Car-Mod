package com.reiko.cars.racing;

import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A race track's Race Control: whose it is, a race's laps, entry fee and the owner's cut of the pot, the best laps
 * ever driven here, and its books.
 */
public final class RaceControlBlockEntity extends BlockEntity {
    public static final int BOARD = 10;

    /** A lap on the board: who drove it, in what, how long it took (ticks) and when (game time). */
    public record Lap(UUID driver, String name, String car, long ticks, long when) {
    }

    @Nullable
    private UUID owner;
    private String ownerName = "";
    private int laps = CarsConfig.get(CarsConfig.RACE_LAPS);
    private long entryFee = Math.round(CarsConfig.get(CarsConfig.ENTRY_FEE) * 100);
    private int cut = (int) Math.round(CarsConfig.get(CarsConfig.HOUSE_CUT) * 100);
    private final List<Lap> board = new ArrayList<>();
    private int races;
    private int trials;
    private long earned;

    public RaceControlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RACE_CONTROL.get(), pos, state);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(@Nullable UUID id, String name) {
        owner = id;
        ownerName = id == null ? "" : name;
        setChanged();
    }

    public void setOwner(Player player) {
        setOwner(player.getUUID(), player.getGameProfile().getName());
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    public int laps() {
        return laps;
    }

    public void setLaps(int laps) {
        this.laps = Math.max(1, Math.min(10, laps));
        setChanged();
    }

    /** A race's entry fee, in cents. */
    public long entryFee() {
        return entryFee;
    }

    public void setEntryFee(long cents) {
        entryFee = Math.max(0, cents);
        setChanged();
    }

    /** The owner's cut of a race's pot, in percent. */
    public int cut() {
        return cut;
    }

    public void setCut(int percent) {
        cut = Math.max(0, Math.min(100, percent));
        setChanged();
    }

    /** The best laps driven here, fastest first. */
    public List<Lap> board() {
        return List.copyOf(board);
    }

    /** Puts a lap on the board if it's one of the best (each driver's best in each kind of car only). Its place, or -1. */
    public int recordLap(Lap lap) {
        Lap same = board.stream().filter(old -> old.driver().equals(lap.driver()) && old.car().equals(lap.car())).findFirst().orElse(null);
        if (same != null) {
            if (same.ticks() <= lap.ticks()) {
                return -1;
            }
            board.remove(same);
        }
        board.add(lap);
        board.sort(Comparator.comparingLong(Lap::ticks).thenComparingLong(Lap::when));
        while (board.size() > BOARD) {
            board.remove(board.size() - 1);
        }
        setChanged();
        return board.indexOf(lap);
    }

    public void clearBoard() {
        board.clear();
        setChanged();
    }

    public int races() {
        return races;
    }

    public int trials() {
        return trials;
    }

    public long earned() {
        return earned;
    }

    void recordRace(long houseTake) {
        races++;
        earned += houseTake;
        setChanged();
    }

    void recordTrial() {
        trials++;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putInt("Laps", laps);
        tag.putLong("EntryFee", entryFee);
        tag.putInt("Cut", cut);
        ListTag list = new ListTag();
        for (Lap lap : board) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Driver", lap.driver());
            entry.putString("Name", lap.name());
            entry.putString("Car", lap.car());
            entry.putLong("Ticks", lap.ticks());
            entry.putLong("When", lap.when());
            list.add(entry);
        }
        tag.put("Board", list);
        tag.putInt("Races", races);
        tag.putInt("Trials", trials);
        tag.putLong("Earned", earned);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        if (tag.contains("Laps")) {
            laps = tag.getInt("Laps");
            entryFee = tag.getLong("EntryFee");
            cut = tag.getInt("Cut");
        }
        board.clear();
        for (Tag t : tag.getList("Board", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            if (entry.hasUUID("Driver")) {
                board.add(new Lap(entry.getUUID("Driver"), entry.getString("Name"), entry.getString("Car"), entry.getLong("Ticks"), entry.getLong("When")));
            }
        }
        races = tag.getInt("Races");
        trials = tag.getInt("Trials");
        earned = tag.getLong("Earned");
    }
}
