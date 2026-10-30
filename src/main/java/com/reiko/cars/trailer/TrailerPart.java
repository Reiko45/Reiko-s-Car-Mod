package com.reiko.cars.trailer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * A stretch of a trailer's length. A trailer is far longer than an entity's square hitbox can be, so the front and
 * back are covered by parts like these (as the ender dragon's body is). They make the trailer clickable and solid
 * along its whole length: clicks and hits go to the trailer, and other vehicles, players and mobs bump into them.
 *
 * <p>A camper also has a part for each of its camp chairs: clickable (to sit down) while its camp is set up, and never
 * solid.
 */
public final class TrailerPart extends PartEntity<TrailerEntity> {
    /** What the part covers. */
    public enum Role {
        /** The front of the trailer, over the kingpin or behind the tongue. */
        FRONT,
        /** The back of the trailer. */
        BACK,
        /** A camp chair beside a parked camper. */
        CHAIR
    }

    private final EntityDimensions size;
    private final Role role;
    /** Which chair, for a chair. */
    private final int index;

    public TrailerPart(TrailerEntity trailer, float width, float height, Role role, int index) {
        super(trailer);
        this.size = EntityDimensions.scalable(width, height);
        this.role = role;
        this.index = index;
        refreshDimensions();
    }

    /** Whether this is the front part, over the kingpin, which a semi backs under to hitch. */
    public boolean front() {
        return role == Role.FRONT;
    }

    public Role role() {
        return role;
    }

    public int index() {
        return index;
    }

    @Override
    public boolean canBeCollidedWith() {
        return role != Role.CHAIR && getParent().canBeCollidedWith();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return role == Role.CHAIR ? getParent().campOut() && !getParent().isRemoved() : getParent().isPickable();
    }

    @Override
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return role == Role.CHAIR ? getParent().sitOnChair(player, index) : getParent().interact(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return role != Role.CHAIR && !isInvulnerableTo(source) && getParent().hurt(source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
