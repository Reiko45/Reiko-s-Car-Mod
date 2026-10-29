package com.reiko.cars.insurance;

import com.reiko.cars.ReikoCars;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Only an insurance office's owner (or an admin) can break it. */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class InsuranceProtection {
    private InsuranceProtection() {
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() instanceof InsuranceOfficeBlock
                && event.getLevel().getBlockEntity(event.getPos()) instanceof InsuranceOfficeBlockEntity office
                && !office.canManage(event.getPlayer())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("insurance.reikocars.protected", office.ownerName()), true);
        }
    }
}
