package com.reiko.cars.bus;

import com.reiko.cars.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * An invisible seat on a bus shelter's bench, made when someone sits down and gone as soon as they get up (or the
 * shelter does). Never saved.
 */
public final class BenchSeatEntity extends Entity {
    /** How far above the seat entity a sitter's riding point is, so they sit on the bench rather than in it. */
    static final double SIT_HEIGHT = -0.1;

    public BenchSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A seat on the bench at {@code pos}, the lower block of a shelter facing {@code state}'s way. */
    public static BenchSeatEntity at(Level level, BlockState state, BlockPos pos) {
        BenchSeatEntity seat = new BenchSeatEntity(ModEntities.BENCH_SEAT.get(), level);
        Vec3 at = BusShelterBlock.seat(state, pos);
        seat.moveTo(at.x, at.y, at.z, state.getValue(BusShelterBlock.FACING).toYRot(), 0.0F);
        return seat;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty() || !(level().getBlockState(blockPosition()).getBlock() instanceof BusShelterBlock))) {
            ejectPassengers();
            discard();
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, SIT_HEIGHT, 0.0);
    }

    /** Getting up: a step out in front of the bench. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 out = Vec3.directionFromRotation(0.0F, getYRot()).scale(0.7);
        return new Vec3(getX() + out.x, blockPosition().getY(), getZ() + out.z);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
