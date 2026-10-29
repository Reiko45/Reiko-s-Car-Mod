package com.reiko.cars.mail;

import com.reiko.cars.ReikoCars;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Only a mailbox's owner (or an admin) can break it, so nobody can spill someone else's mail. */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class MailProtection {
    private MailProtection() {
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() instanceof MailboxBlock
                && event.getLevel().getBlockEntity(event.getPos()) instanceof MailboxBlockEntity box
                && !box.canManage(event.getPlayer())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("mail.reikocars.protected", box.ownerName()), true);
        }
    }
}
