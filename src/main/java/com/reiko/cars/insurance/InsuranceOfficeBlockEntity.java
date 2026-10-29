package com.reiko.cars.insurance;

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
 * An insurance office: its owner (the insurer, who pays the claims from their bank account), the terms it sells
 * cover on (premium per term, deductible and claim limit, in cents; -1 means the server's default), and its books.
 */
public final class InsuranceOfficeBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String ownerName = "";
    private long premium = -1;
    private long deductible = -1;
    private long limit = -1;
    private long premiums;
    private int policies;
    private long paidOut;
    private int claims;

    public InsuranceOfficeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INSURANCE_OFFICE.get(), pos, state);
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

    /** The owner or an admin: they may break it. */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    private static long cents(double dollars) {
        return Math.round(dollars * 100.0);
    }

    /** What one term of cover costs, before tax. */
    public long premium() {
        return premium >= 0 ? premium : cents(CarsConfig.get(CarsConfig.INSURANCE_PREMIUM));
    }

    /** What the car's owner pays of each claim. */
    public long deductible() {
        return deductible >= 0 ? deductible : cents(CarsConfig.get(CarsConfig.INSURANCE_DEDUCTIBLE));
    }

    /** The most one claim pays out. */
    public long limit() {
        return limit >= 0 ? limit : cents(CarsConfig.get(CarsConfig.INSURANCE_LIMIT));
    }

    public void setTerms(long premium, long deductible, long limit) {
        this.premium = Math.max(0, premium);
        this.deductible = Math.max(0, deductible);
        this.limit = Math.max(0, limit);
        setChanged();
    }

    void recordPolicy(long premium) {
        premiums += premium;
        policies++;
        setChanged();
    }

    void recordClaim(long paid) {
        paidOut += paid;
        claims++;
        setChanged();
    }

    public long premiums() {
        return premiums;
    }

    public int policies() {
        return policies;
    }

    public long paidOut() {
        return paidOut;
    }

    public int claims() {
        return claims;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putLong("Premium", premium);
        tag.putLong("Deductible", deductible);
        tag.putLong("Limit", limit);
        tag.putLong("Premiums", premiums);
        tag.putInt("Policies", policies);
        tag.putLong("PaidOut", paidOut);
        tag.putInt("Claims", claims);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        premium = tag.contains("Premium") ? tag.getLong("Premium") : -1;
        deductible = tag.contains("Deductible") ? tag.getLong("Deductible") : -1;
        limit = tag.contains("Limit") ? tag.getLong("Limit") : -1;
        premiums = tag.getLong("Premiums");
        policies = tag.getInt("Policies");
        paidOut = tag.getLong("PaidOut");
        claims = tag.getInt("Claims");
    }
}
