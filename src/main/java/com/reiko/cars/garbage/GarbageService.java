package com.reiko.cars.garbage;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.job.CarJobs;
import com.reiko.cars.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The garbage truck's round: G empties the trash cans by its hopper, at the back, and the trash is crushed and gone.
 * Each can with anything in it pays the Garbage Collector job, at most once a Minecraft day per can.
 */
public final class GarbageService {
    /** How far from the hopper a trash can may be, in blocks. */
    public static final double REACH = 3.5;
    /** The hopper's distance behind the truck's middle, in blocks. */
    public static final double HOPPER_BACK = 64 / 16.0;
    /** How long before a can pays again, in ticks: a Minecraft day. */
    public static final long PAY_EVERY = 24_000;

    private GarbageService() {
    }

    /** Where the truck's hopper is. */
    public static Vec3 hopper(CarEntity truck) {
        return truck.position().add(truck.forward().scale(-HOPPER_BACK));
    }

    /** The trash cans within reach of the truck's hopper. Works on either side. */
    public static List<TrashCanBlockEntity> cansNear(CarEntity truck) {
        Level level = truck.level();
        Vec3 hopper = hopper(truck);
        BlockPos at = BlockPos.containing(hopper);
        int r = (int) Math.ceil(REACH);
        List<TrashCanBlockEntity> cans = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, -2, -r), at.offset(r, 2, r))) {
            if (level.getBlockEntity(pos) instanceof TrashCanBlockEntity can
                    && pos.getCenter().distanceToSqr(hopper.x, pos.getCenter().y, hopper.z) <= REACH * REACH) {
                cans.add(can);
            }
        }
        return cans;
    }

    /** The driver's G: empty every can by the hopper. */
    public static void collect(ServerPlayer driver, CarEntity truck) {
        List<TrashCanBlockEntity> cans = cansNear(truck);
        long now = truck.level().getGameTime();
        int emptied = 0;
        int items = 0;
        int paid = 0;
        for (TrashCanBlockEntity can : cans) {
            int count = can.itemCount();
            if (count <= 0) {
                continue;
            }
            can.clearContent();
            can.setChanged();
            emptied++;
            items += count;
            if (can.lastPaid() < 0 || now - can.lastPaid() >= PAY_EVERY) {
                can.setLastPaid(now);
                paid++;
            }
        }
        if (emptied == 0) {
            driver.displayClientMessage(Component.translatable(cans.isEmpty() ? "garbage.reikocars.no_cans" : "garbage.reikocars.cans_empty"), true);
            return;
        }
        CarJobs.work(driver, CarJobs.TRASH_COLLECTED, paid);
        truck.level().playSound(null, hopper(truck).x, truck.getY() + 1, hopper(truck).z, ModSounds.COMPACTOR.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        driver.displayClientMessage(Component.translatable("garbage.reikocars.collected", emptied, items), true);
    }
}
