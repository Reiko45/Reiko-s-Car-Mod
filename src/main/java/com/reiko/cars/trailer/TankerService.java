package com.reiko.cars.trailer;

import com.reiko.cars.job.CarJobs;
import com.reiko.cars.registry.ModSounds;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Filling and emptying a tanker. Park it with any tank, pump or machine that holds fluid within {@link #REACH} blocks
 * of its sides or ends (BuildCraft's tanks and refinery, other mods' tanks, gas and diesel pumps), then load or
 * unload from its screen. Loading takes from everything nearby that will give its fluid up; unloading fills
 * everything nearby that will take it, nearest first. Pumps only take their own kind of fuel, as with pipes.
 */
public final class TankerService {
    public static final int REACH = 2;

    private TankerService() {
    }

    /**
     * Every fluid handler on a block near the tanker, nearest first, each one once: only those {@code player} could
     * reach in and use (not inside spawn protection, for one).
     */
    public static List<IFluidHandler> nearbyTanks(TrailerEntity tanker, ServerPlayer player) {
        return nearby(tanker, player).stream().map(Tank::handler).toList();
    }

    /** A fluid handler near the tanker, and whether it's a gas or diesel pump. */
    private record Tank(IFluidHandler handler, boolean pump) {
    }

    private static List<Tank> nearby(TrailerEntity tanker, ServerPlayer player) {
        Level level = tanker.level();
        double half = tanker.trailerType().length / 2.0 + REACH;
        AABB area = new AABB(tanker.getX() - half, tanker.getY() - 1, tanker.getZ() - half,
                tanker.getX() + half, tanker.getY() + 4, tanker.getZ() + half);
        Vec3 middle = tanker.position();
        List<BlockPos> positions = new ArrayList<>();
        BlockPos.betweenClosedStream(area).forEach(pos -> {
            // Not just blocks with block entities: a cauldron is a tank too.
            if (nearTrailer(tanker, pos) && !level.getBlockState(pos).isAir() && player.mayInteract(level, pos)) {
                positions.add(pos.immutable());
            }
        });
        positions.sort(Comparator.comparingDouble(pos -> pos.getCenter().distanceToSqr(middle)));
        Set<IFluidHandler> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Tank> handlers = new ArrayList<>();
        for (BlockPos pos : positions) {
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
            if (handler != null && seen.add(handler)) {
                handlers.add(new Tank(handler, level.getBlockState(pos).getBlock() instanceof com.reiko.cars.pump.GasPumpBlock));
            }
        }
        return handlers;
    }

    /** Within {@link #REACH} blocks of the trailer's sides or ends, measured along and across it. */
    private static boolean nearTrailer(TrailerEntity tanker, BlockPos pos) {
        Vec3 offset = pos.getCenter().subtract(tanker.position());
        Vec3 forward = tanker.forward();
        double along = Math.abs(offset.x * forward.x + offset.z * forward.z);
        double across = Math.abs(offset.x * forward.z - offset.z * forward.x);
        return along <= tanker.trailerType().length / 2.0 + REACH && across <= tanker.trailerType().width / 2.0 + REACH;
    }

    public static void load(ServerPlayer player, TrailerEntity tanker) {
        FluidTank tank = tanker.tank();
        int before = tank.getFluidAmount();
        int sources = 0;
        for (IFluidHandler handler : nearbyTanks(tanker, player)) {
            int space = tank.getSpace();
            if (space <= 0) {
                break;
            }
            FluidStack moved = FluidUtil.tryFluidTransfer(tank, handler, space, true);
            if (!moved.isEmpty()) {
                sources++;
            }
        }
        int loaded = tank.getFluidAmount() - before;
        if (loaded <= 0) {
            player.displayClientMessage(Component.translatable(tank.getSpace() <= 0 ? "trailer.reikocars.tanker_full"
                    : "trailer.reikocars.nothing_to_load"), true);
            return;
        }
        tanker.playSound(ModSounds.PUMP.get(), 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("trailer.reikocars.loaded_fluid", buckets(loaded),
                tank.getFluid().getHoverName(), sources), true);
    }

    public static void unload(ServerPlayer player, TrailerEntity tanker) {
        FluidTank tank = tanker.tank();
        if (tank.isEmpty()) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.tanker_empty"), true);
            return;
        }
        FluidStack carried = tank.getFluid().copy();
        int before = tank.getFluidAmount();
        int targets = 0;
        int toPumps = 0;
        for (Tank target : nearby(tanker, player)) {
            if (tank.isEmpty()) {
                break;
            }
            FluidStack moved = FluidUtil.tryFluidTransfer(target.handler(), tank, tank.getFluidAmount(), true);
            if (!moved.isEmpty()) {
                targets++;
                if (target.pump()) {
                    toPumps += moved.getAmount();
                }
            }
        }
        // Fuel into a pump is a trucker's delivery (pumps don't give it back, so each bucket only pays once).
        if (toPumps > 0) {
            int millibuckets = tanker.addDelivered(toPumps);
            CarJobs.work(player, CarJobs.FUEL_DELIVERED, millibuckets / 1000);
        }
        int unloaded = before - tank.getFluidAmount();
        if (unloaded <= 0) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.nothing_takes", carried.getHoverName()), true);
            return;
        }
        tanker.level().playSound(null, tanker.getX(), tanker.getY(), tanker.getZ(), ModSounds.PUMP.get(), SoundSource.NEUTRAL, 1.0F, 0.9F);
        player.displayClientMessage(Component.translatable("trailer.reikocars.unloaded_fluid", buckets(unloaded),
                carried.getHoverName(), targets), true);
    }

    /** "4.5" for 4,500 millibuckets. */
    static String buckets(int millibuckets) {
        return millibuckets % 1000 == 0 ? Integer.toString(millibuckets / 1000)
                : String.format(java.util.Locale.ROOT, "%.1f", millibuckets / 1000.0);
    }
}
