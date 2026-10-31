package com.reiko.cars.garage;

import com.reiko.cars.registry.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A garage door remote. Its door's owner links it (sneak-right-click the door with it; again to unlink): up to
 * {@link RemoteLinks#MAX} doors. Right-click, anywhere, even driving, opens or shuts the nearest of them in range;
 * carrying it, the door opens for you as you drive up, and you can open it by hand. Give one to whoever may use your
 * garage. Resetting the door's remotes, on its screen, stops every remote linked to it working.
 */
public final class GarageRemoteItem extends Item {
    /** How far a remote reaches, in blocks. */
    public static final double RANGE = 64.0;

    public GarageRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()
                || !(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof GarageDoorBlock)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel() instanceof ServerLevel level) {
            link(player, context.getItemInHand(), level, context.getClickedPos());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    /** Links the remote to the door at {@code pos}, or unlinks it. */
    private static void link(Player player, ItemStack stack, ServerLevel level, BlockPos pos) {
        GarageDoorBlockEntity door = GarageDoors.doorAt(level, pos);
        if (door == null) {
            if (level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel) {
                GarageDoors.Joined joined = GarageDoors.rejoin(level, pos);
                if (joined != null && !joined.formed()) {
                    player.displayClientMessage(joined.message(), true);
                }
                door = panel.anchorEntity();
            }
            if (door == null) {
                return;
            }
        }
        if (!door.canManage(player) || door.owner() == null) {
            player.displayClientMessage(Component.translatable("garage.reikocars.not_yours", door.ownerName()), true);
            return;
        }
        String dimension = level.dimension().location().toString();
        RemoteLinks links = stack.getOrDefault(ModDataComponents.GARAGE_LINKS.get(), RemoteLinks.EMPTY);
        Component name = GarageDoors.describe(door);
        if (links.has(dimension, door.getBlockPos())) {
            stack.set(ModDataComponents.GARAGE_LINKS.get(), links.toggled(new RemoteLinks.Link(dimension, door.getBlockPos(), 0L, "")));
            player.displayClientMessage(Component.translatable("garage.reikocars.remote_unlinked", name), true);
        } else if (links.links().size() >= RemoteLinks.MAX) {
            player.displayClientMessage(Component.translatable("garage.reikocars.remote_full", RemoteLinks.MAX), true);
            return;
        } else {
            stack.set(ModDataComponents.GARAGE_LINKS.get(), links.toggled(new RemoteLinks.Link(dimension, door.getBlockPos(), door.settings().code(),
                    door.settings().name())));
            player.displayClientMessage(Component.translatable("garage.reikocars.remote_linked", name), true);
        }
        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 1.6F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            press(serverPlayer, stack);
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Opens or shuts the nearest door in range that the remote's linked to (and still opens). */
    static void press(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3F, 2.0F);
        RemoteLinks links = stack.getOrDefault(ModDataComponents.GARAGE_LINKS.get(), RemoteLinks.EMPTY);
        if (links.links().isEmpty()) {
            player.displayClientMessage(Component.translatable("garage.reikocars.remote_empty"), true);
            return;
        }
        GarageDoorBlockEntity nearest = nearest(level, player, links);
        if (nearest == null) {
            player.displayClientMessage(Component.translatable("garage.reikocars.remote_far", (int) RANGE), true);
            return;
        }
        GarageDoors.toggle(nearest);
    }

    @Nullable
    private static GarageDoorBlockEntity nearest(ServerLevel level, Player player, RemoteLinks links) {
        String dimension = level.dimension().location().toString();
        GarageDoorBlockEntity best = null;
        double bestDistance = RANGE * RANGE;
        for (RemoteLinks.Link link : links.links()) {
            if (!link.dimension().equals(dimension) || !level.isLoaded(link.anchor())) {
                continue;
            }
            if (!(level.getBlockEntity(link.anchor()) instanceof GarageDoorBlockEntity door) || !door.isAnchor()
                    || door.settings().code() != link.code()) {
                continue;
            }
            double distance = player.position().distanceToSqr(GarageDoors.middle(door));
            if (distance <= bestDistance) {
                best = door;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RemoteLinks links = stack.getOrDefault(ModDataComponents.GARAGE_LINKS.get(), RemoteLinks.EMPTY);
        if (links.links().isEmpty()) {
            tooltip.add(Component.translatable("item.reikocars.garage_remote.empty").withStyle(ChatFormatting.GRAY));
        }
        for (RemoteLinks.Link link : links.links()) {
            BlockPos pos = link.anchor();
            tooltip.add(link.name().isEmpty()
                    ? Component.translatable("item.reikocars.garage_remote.door_at", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GRAY)
                    : Component.translatable("item.reikocars.garage_remote.door", link.name(), pos.getX(), pos.getY(), pos.getZ())
                            .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.reikocars.garage_remote.use", (int) RANGE).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.reikocars.garage_remote.link", RemoteLinks.MAX).withStyle(ChatFormatting.DARK_GRAY));
    }
}
