package com.reiko.cars.pump;

import com.reiko.cars.ReikoCars;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Only a pump's owner (or an admin) can break it, like the economy mod's shops. */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class PumpProtection {
    private PumpProtection() {
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof GasPumpBlock)) {
            return;
        }
        GasPumpBlockEntity pump = GasPumpBlock.pumpAt(event.getLevel(), event.getPos(), state);
        if (pump != null && !pump.canManage(event.getPlayer())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("pump.reikocars.protected", pump.ownerName()), true);
        }
    }
}
