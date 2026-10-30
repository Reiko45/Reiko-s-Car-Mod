package com.reiko.cars.fire;

import com.reiko.cars.ReikoCars;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Notes who starts fires, so the Firefighter job doesn't pay anyone for putting out their own: lighting one with flint
 * and steel or a fire charge, or pouring lava (anything in the {@code reikocars:fire_starters} item tag), and any fire
 * or lava a player places.
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class FireEvents {
    /** Items that start fires. */
    public static final TagKey<Item> FIRE_STARTERS = TagKey.create(Registries.ITEM, ReikoCars.id("fire_starters"));

    private FireEvents() {
    }

    private static boolean startsFires(ItemStack stack) {
        return stack.is(FIRE_STARTERS);
    }

    @SubscribeEvent
    static void onUseOnBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && startsFires(event.getItemStack())) {
            Firefighting.lit(player, event.getPos());
        }
    }

    @SubscribeEvent
    static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && startsFires(event.getItemStack())) {
            Firefighting.lit(player, player.blockPosition());
        }
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && (event.getPlacedBlock().is(BlockTags.FIRE)
                || event.getPlacedBlock().getFluidState().is(FluidTags.LAVA))) {
            Firefighting.lit(player, event.getPos());
        }
    }
}
