package com.reiko.cars.dmv;

import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A DMV: whose it is, what it charges (in cents) for the driving test, a replacement card and a custom plate, and
 * its books.
 */
public final class DmvOfficeBlockEntity extends BlockEntity {
    public static final int TEST_FEE = 0;
    public static final int REPLACE_FEE = 1;
    public static final int PLATE_FEE = 2;

    @Nullable
    private UUID owner;
    private String ownerName = "";
    private final long[] fees = {
            Math.round(CarsConfig.get(CarsConfig.TEST_FEE) * 100),
            Math.round(CarsConfig.get(CarsConfig.REPLACE_FEE) * 100),
            Math.round(CarsConfig.get(CarsConfig.CUSTOM_PLATE_PRICE) * 100)};
    private long earned;
    private int tests;
    private int passes;
    private int plates;

    public DmvOfficeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DMV_OFFICE.get(), pos, state);
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

    /** The owner or an admin: they set the fees and may break it. */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    /** One of its fees ({@link #TEST_FEE}, {@link #REPLACE_FEE}, {@link #PLATE_FEE}), in cents before tax. */
    public long fee(int which) {
        return fees[which];
    }

    public void setFee(int which, long cents) {
        fees[which] = Math.max(0, cents);
        setChanged();
    }

    public void recordTest(long paid) {
        tests++;
        earned += paid;
        setChanged();
    }

    public void recordPass() {
        passes++;
        setChanged();
    }

    public void recordEarnings(long paid) {
        earned += paid;
        setChanged();
    }

    public void recordPlate(long paid) {
        plates++;
        earned += paid;
        setChanged();
    }

    public long earned() {
        return earned;
    }

    public int tests() {
        return tests;
    }

    public int passes() {
        return passes;
    }

    public int platesSold() {
        return plates;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putLong("TestFee", fees[TEST_FEE]);
        tag.putLong("ReplaceFee", fees[REPLACE_FEE]);
        tag.putLong("PlateFee", fees[PLATE_FEE]);
        tag.putLong("Earned", earned);
        tag.putInt("Tests", tests);
        tag.putInt("Passes", passes);
        tag.putInt("Plates", plates);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        String[] keys = {"TestFee", "ReplaceFee", "PlateFee"};
        for (int i = 0; i < keys.length; i++) {
            if (tag.contains(keys[i])) {
                fees[i] = tag.getLong(keys[i]);
            }
        }
        earned = tag.getLong("Earned");
        tests = tag.getInt("Tests");
        passes = tag.getInt("Passes");
        plates = tag.getInt("Plates");
    }
}
