package com.reiko.cars.repair;

import com.reiko.cars.ReikoCars;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Only a repair bench's owner (or an admin) can break it, so its stock is safe, as with the pumps. */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class RepairProtection {
    private RepairProtection() {
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() instanceof RepairBenchBlock
                && event.getLevel().getBlockEntity(event.getPos()) instanceof RepairBenchBlockEntity bench
                && !bench.canManage(event.getPlayer())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("repair.reikocars.protected", bench.ownerName()), true);
        }
    }
}
