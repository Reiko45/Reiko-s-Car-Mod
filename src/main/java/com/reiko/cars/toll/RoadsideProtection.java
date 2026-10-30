package com.reiko.cars.toll;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.parking.ParkingMeterBlock;
import com.reiko.cars.parking.ParkingMeterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Only a parking meter's, toll booth's, toll gate's, traffic light's or speed sign's owner (or an admin) can break
 * it, so nobody knocks down a gate to get through, or a meter or camera to dodge a ticket.
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class RoadsideProtection {
    private RoadsideProtection() {
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        String owner = blockingOwner(event.getLevel(), event.getPos(), event.getState(), event.getPlayer());
        if (owner != null) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("toll.reikocars.protected", owner), true);
        }
    }

    /** The owner's name, if {@code player} isn't allowed to break this; null if they are. */
    private static String blockingOwner(LevelAccessor level, BlockPos pos, BlockState state, Player player) {
        if (state.getBlock() instanceof ParkingMeterBlock && level.getBlockEntity(pos) instanceof ParkingMeterBlockEntity meter) {
            return meter.canManage(player) ? null : meter.ownerName();
        }
        if (state.getBlock() instanceof TollBoothBlock && level.getBlockEntity(pos) instanceof TollBoothBlockEntity booth) {
            return booth.canManage(player) ? null : booth.ownerName();
        }
        if (level.getBlockEntity(pos) instanceof com.reiko.cars.traffic.OwnedRoadsideBlockEntity sign) {
            return sign.canManage(player) ? null : sign.ownerName();
        }
        if (state.getBlock() instanceof TollGateBlock && level.getBlockEntity(TollGateBlock.polePos(state, pos)) instanceof TollGateBlockEntity gate) {
            return gate.canManage(player) ? null : gate.ownerName();
        }
        return null;
    }
}
