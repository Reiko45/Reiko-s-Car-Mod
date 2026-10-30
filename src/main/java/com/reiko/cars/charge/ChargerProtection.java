package com.reiko.cars.charge;

import com.reiko.cars.ReikoCars;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Only a charging station's owner (or an admin) can break it, like the pumps. */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class ChargerProtection {
    private ChargerProtection() {
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof ChargingStationBlock)) {
            return;
        }
        ChargingStationBlockEntity station = ChargingStationBlock.stationAt(event.getLevel(), event.getPos(), state);
        if (station != null && !station.canManage(event.getPlayer())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("charge.reikocars.protected", station.ownerName()), true);
        }
    }
}
