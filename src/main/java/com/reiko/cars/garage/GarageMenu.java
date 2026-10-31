package com.reiko.cars.garage;

import com.reiko.cars.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A garage door's owner's screen: its name, its switches (windows, opening for cars, which side's inside), resetting
 * its remotes, and running it as a parking garage (on or off, the prices, the takings).
 */
public final class GarageMenu extends AbstractContainerMenu {
    private final BlockPos anchor;
    private final GarageSettings settings;
    private final GarageDoorBlock.Style style;
    private final int width;
    private final int height;
    private final long earned;
    private final int visits;
    private final int parkedNow;
    /** Whether whoever opened it was standing inside the garage, as the door was set up then. */
    private final boolean inside;

    public GarageMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, buf.readBlockPos(),
                new GarageSettings(buf.readUtf(GarageSettings.MAX_NAME), buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), 0L,
                        buf.readBoolean(), buf.readVarLong(), buf.readVarLong()),
                buf.readEnum(GarageDoorBlock.Style.class), buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean());
    }

    private GarageMenu(int containerId, BlockPos anchor, GarageSettings settings, GarageDoorBlock.Style style, int width, int height, long earned,
                       int visits, int parkedNow, boolean inside) {
        super(ModMenus.GARAGE.get(), containerId);
        this.anchor = anchor;
        this.settings = settings;
        this.style = style;
        this.width = width;
        this.height = height;
        this.earned = earned;
        this.visits = visits;
        this.parkedNow = parkedNow;
        this.inside = inside;
    }

    public static void open(ServerPlayer player, GarageDoorBlockEntity door) {
        BlockPos anchor = door.getBlockPos();
        GarageSettings settings = door.settings();
        GarageDoorBlock.Style style = door.style();
        int width = door.width();
        int height = door.height();
        long earned = door.earned();
        int visits = door.visits();
        long now = player.level().getGameTime();
        int parkedNow = (int) door.parked().values().stream().filter(ticket -> ticket.until() > now).count();
        boolean inside = GarageDoors.depth(player.position(), door) < 0.0;
        Component title = Component.translatable("container.reikocars.garage", door.getBlockState().getBlock().getName());
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new GarageMenu(id, anchor, settings, style, width, height, earned, visits, parkedNow, inside),
                title), buf -> {
                    buf.writeBlockPos(anchor);
                    buf.writeUtf(settings.name(), GarageSettings.MAX_NAME);
                    buf.writeInt(settings.color());
                    buf.writeBoolean(settings.windows());
                    buf.writeBoolean(settings.autoOpen());
                    buf.writeBoolean(settings.inward());
                    buf.writeBoolean(settings.parking());
                    buf.writeVarLong(settings.hourPrice());
                    buf.writeVarLong(settings.dayPrice());
                    buf.writeEnum(style);
                    buf.writeVarInt(width);
                    buf.writeVarInt(height);
                    buf.writeVarLong(earned);
                    buf.writeVarInt(visits);
                    buf.writeVarInt(parkedNow);
                    buf.writeBoolean(inside);
                });
    }

    /** The door, on the server. */
    @Nullable
    public GarageDoorBlockEntity door(Player player) {
        return player.level().getBlockEntity(anchor) instanceof GarageDoorBlockEntity door && door.isAnchor() ? door : null;
    }

    public BlockPos anchor() {
        return anchor;
    }

    public GarageSettings settings() {
        return settings;
    }

    public GarageDoorBlock.Style style() {
        return style;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public long earned() {
        return earned;
    }

    public int visits() {
        return visits;
    }

    public int parkedNow() {
        return parkedNow;
    }

    public boolean inside() {
        return inside;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(anchor.getCenter()) < 16.0 * 16.0;
    }
}
