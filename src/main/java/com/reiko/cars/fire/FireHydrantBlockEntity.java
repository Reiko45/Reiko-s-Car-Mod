package com.reiko.cars.fire;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/** A fire hydrant's water, and filling the fire trucks parked by it. See {@link FireHydrantBlock}. */
public final class FireHydrantBlockEntity extends BlockEntity {
    /** Millibuckets a tick it fills a fire truck at: two buckets a second. */
    public static final int RATE = 100;
    static final int EVERY = 10;

    private final FluidTank tank = new FluidTank(CarsConfig.get(CarsConfig.HYDRANT_CAPACITY) * 1000, stack -> stack.getFluid().is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    /** What pipes and buckets see: water goes in; nothing comes out but into a fire truck. */
    private final IFluidHandler input = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int index) {
            return tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return tank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public FireHydrantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIRE_HYDRANT.get(), pos, state);
    }

    /** For pipes and buckets: water in only. */
    public IFluidHandler pipeInput() {
        return input;
    }

    public FluidTank tank() {
        return tank;
    }

    /** Whether it never runs dry (config {@code fireTruck.endlessHydrants}). */
    public static boolean endless() {
        return CarsConfig.get(CarsConfig.ENDLESS_HYDRANTS);
    }

    /** "Fire hydrant: 6 of 8 buckets of water", or that it's on the mains. */
    public Component levelLine() {
        return endless() ? Component.translatable("fire.reikocars.hydrant_endless")
                : Component.translatable("fire.reikocars.hydrant_level", tank.getFluidAmount() / 1000, tank.getCapacity() / 1000);
    }

    /** The fire truck it would fill: the nearest one parked within a hose's length that has room. */
    @Nullable
    public CarEntity truckToFill() {
        double reach = CarsConfig.get(CarsConfig.HOSE_LENGTH);
        Vec3 at = worldPosition.getCenter();
        return level.getEntitiesOfClass(CarEntity.class, new AABB(worldPosition).inflate(reach + 4.0),
                        truck -> truck.carType().isFireTruck() && truck.isAlive() && truck.standingStill()
                                && truck.water() < FireTrucks.tankCapacity() && distance(at, truck) <= reach)
                .stream().min(Comparator.comparingDouble(truck -> distance(at, truck))).orElse(null);
    }

    private static double distance(Vec3 at, CarEntity truck) {
        AABB box = truck.getBoundingBox();
        double dx = Math.max(Math.max(box.minX - at.x, 0.0), at.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - at.y, 0.0), at.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - at.z, 0.0), at.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    void serverTick() {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % EVERY != 0) {
            return;
        }
        boolean endless = endless();
        if (!endless && tank.isEmpty()) {
            return;
        }
        CarEntity truck = truckToFill();
        if (truck == null) {
            return;
        }
        int amount = Math.min(RATE * EVERY, FireTrucks.tankCapacity() - truck.water());
        if (!endless) {
            amount = Math.min(amount, tank.getFluidAmount());
            tank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        }
        truck.setWater(truck.water() + amount);
        server.sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5, worldPosition.getY() + 0.7, worldPosition.getZ() + 0.5, 8,
                0.25, 0.1, 0.25, 0.1);
        if (server.getGameTime() % (EVERY * 4) == 0) {
            server.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 0.8F);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Water", tank.getFluidAmount());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int water = Math.min(tag.getInt("Water"), tank.getCapacity());
        tank.setFluid(water > 0 ? new FluidStack(Fluids.WATER, water) : FluidStack.EMPTY);
    }
}
