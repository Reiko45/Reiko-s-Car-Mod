package com.reiko.cars.wash;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Car Wash: its owner, price and books (see {@link WashBusinessBlockEntity}), the washes paid for (a car's id and
 * when the pass runs out), and on clients where its brushes are and how far they've turned.
 */
public final class CarWashBlockEntity extends WashBusinessBlockEntity {
    /** Paid-for washes: car id to the game time the pass runs out. */
    final Map<UUID, Long> passes = new HashMap<>();
    /** Cars with a pass that have been in the lane: their pass is used up once they leave it. */
    final Set<UUID> washing = new HashSet<>();

    // On clients: the brushes. Distances in blocks across the lane from the left post's middle; heights from the road.
    private float spin;
    private float spinO;
    private float left = REST_LEFT;
    private float leftO = REST_LEFT;
    private float right = REST_RIGHT;
    private float rightO = REST_RIGHT;
    private float top = REST_TOP;
    private float topO = REST_TOP;
    /** Where the side brushes stand when nothing's being washed, and the top one. */
    public static final float REST_LEFT = 0.72F;
    public static final float REST_RIGHT = CarWashBlock.SPANS - 1 - 0.72F;
    public static final float REST_TOP = CarWashBlock.POSTS - 0.5F;
    public static final float BRUSH_RADIUS = 0.42F;

    public CarWashBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAR_WASH.get(), pos, state);
    }

    @Override
    protected long defaultPrice() {
        return Math.round(CarsConfig.get(CarsConfig.WASH_PRICE) * 100.0);
    }

    /** Whether {@code car} has a wash paid for here. */
    public boolean hasPass(CarEntity car, long now) {
        Long until = passes.get(car.getUUID());
        return until != null && until > now;
    }

    void givePass(CarEntity car, long until) {
        passes.put(car.getUUID(), until);
        setChanged();
    }

    public boolean running() {
        return getBlockState().getValue(CarWashBlock.RUNNING);
    }

    /** The cars in the lane, nearest the arch first. */
    public List<CarEntity> carsInLane() {
        if (level == null) {
            return List.of();
        }
        AABB lane = CarWashBlock.lane(getBlockState(), worldPosition);
        Vec3 middle = lane.getCenter();
        return level.getEntitiesOfClass(CarEntity.class, lane, car -> car.isAlive() && !car.isPassenger()).stream()
                .sorted(java.util.Comparator.comparingDouble(car -> car.position().distanceToSqr(middle.x, car.getY(), middle.z)))
                .toList();
    }

    // ---- on clients: the brushes ----

    void clientTick() {
        spinO = spin;
        leftO = left;
        rightO = right;
        topO = top;
        boolean on = running();
        float targetLeft = REST_LEFT;
        float targetRight = REST_RIGHT;
        float targetTop = REST_TOP;
        if (on) {
            spin += 24.0F;
            List<CarEntity> cars = carsInLane();
            if (!cars.isEmpty()) {
                // The brushes close in on the car's sides and come down on its roof.
                CarEntity car = cars.getFirst();
                Direction across = CarWashBlock.across(getBlockState());
                double offset = (car.getX() - (worldPosition.getX() + 0.5)) * across.getStepX() + (car.getZ() - (worldPosition.getZ() + 0.5)) * across.getStepZ();
                float half = car.carType().width * 0.5F;
                targetLeft = Mth.clamp((float) offset - half - BRUSH_RADIUS * 0.8F, REST_LEFT, REST_RIGHT - 1.0F);
                targetRight = Mth.clamp((float) offset + half + BRUSH_RADIUS * 0.8F, REST_LEFT + 1.0F, REST_RIGHT);
                targetTop = Mth.clamp((float) (car.getY() - worldPosition.getY()) + car.carType().height + BRUSH_RADIUS * 0.7F, 1.0F, REST_TOP);
            }
            spray();
        }
        left += Mth.clamp(targetLeft - left, -0.12F, 0.12F);
        right += Mth.clamp(targetRight - right, -0.12F, 0.12F);
        top += Mth.clamp(targetTop - top, -0.1F, 0.1F);
    }

    /** Water from the spray bar under the beam, and suds where the brushes are. */
    private void spray() {
        if (level == null) {
            return;
        }
        RandomSource random = level.getRandom();
        Direction across = CarWashBlock.across(getBlockState());
        double baseX = worldPosition.getX() + 0.5;
        double baseZ = worldPosition.getZ() + 0.5;
        double beamY = worldPosition.getY() + CarWashBlock.POSTS + 0.1;
        for (int i = 0; i < 4; i++) {
            double along = 1.0 + random.nextDouble() * (CarWashBlock.LANE - 1.0) + 0.5;
            double x = baseX + across.getStepX() * along + random.nextGaussian() * 0.05;
            double z = baseZ + across.getStepZ() * along + random.nextGaussian() * 0.05;
            level.addParticle(ParticleTypes.FALLING_WATER, x, beamY, z, 0.0, 0.0, 0.0);
            if (random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.SPLASH, x, beamY - 0.2, z, random.nextGaussian() * 0.05, -0.3, random.nextGaussian() * 0.05);
            }
        }
        if (random.nextInt(3) == 0) {
            float side = random.nextBoolean() ? left : right;
            double y = worldPosition.getY() + 0.3 + random.nextDouble() * 2.5;
            level.addParticle(ParticleTypes.CLOUD, baseX + across.getStepX() * side, y, baseZ + across.getStepZ() * side, 0.0, 0.01, 0.0);
        }
    }

    public float spin(float partialTick) {
        return Mth.lerp(partialTick, spinO, spin);
    }

    public float leftBrush(float partialTick) {
        return Mth.lerp(partialTick, leftO, left);
    }

    public float rightBrush(float partialTick) {
        return Mth.lerp(partialTick, rightO, right);
    }

    public float topBrush(float partialTick) {
        return Mth.lerp(partialTick, topO, top);
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        passes.forEach((car, until) -> {
            CompoundTag pass = new CompoundTag();
            pass.putUUID("Car", car);
            pass.putLong("Until", until);
            list.add(pass);
        });
        if (!list.isEmpty()) {
            tag.put("Passes", list);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        passes.clear();
        for (Tag entry : tag.getList("Passes", Tag.TAG_COMPOUND)) {
            CompoundTag pass = (CompoundTag) entry;
            if (pass.hasUUID("Car")) {
                passes.put(pass.getUUID("Car"), pass.getLong("Until"));
            }
        }
    }
}
