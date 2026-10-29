package com.reiko.cars.sale;

import com.reiko.cars.ReikoCars;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Only a dealership's owner (or an admin) can break it. */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class SaleProtection {
    private SaleProtection() {
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() instanceof DealershipBlock
                && event.getLevel().getBlockEntity(event.getPos()) instanceof DealershipBlockEntity lot
                && !lot.canManage(event.getPlayer())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("sale.reikocars.protected", lot.ownerName()), true);
        }
    }
}
