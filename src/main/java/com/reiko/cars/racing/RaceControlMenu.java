package com.reiko.cars.racing;

import com.reiko.cars.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A Race Control's screen, server side. The track, the settings, who's entered and the best laps are sent when it
 * opens (and it's reopened when they change); the race's state, entrants, pot and whether the viewer's in it are kept
 * live through data slots. Actions go through {@code LotActionPayload}.
 */
public final class RaceControlMenu extends AbstractContainerMenu {
    public static final int TAB_RACE = 0;
    public static final int TAB_BOARD = 1;
    public static final int TAB_SETTINGS = 2;

    private static final int STATE = 0;
    private static final int COUNT = 1;
    private static final int POT = 2;
    private static final int ENTERED = 6;
    private static final int TRIAL = 7;
    private static final int SLOTS = 8;

    /** A best lap, as the screen shows it. */
    public record BoardLine(String name, String car, long ticks) {
    }

    /** What the screen knows from when it opened. */
    public record Info(String ownerName, boolean manager, int laps, long entryFee, int cut, int flags, List<String> entrants,
                       List<BoardLine> board, int races, int trials, long earned) {
    }

    @Nullable
    private final RaceControlBlockEntity control; // null on the client
    @Nullable
    private ServerPlayer viewer;
    private final BlockPos pos;
    private final Info info;
    private final ContainerData data;

    public RaceControlMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), read(buf), new SimpleContainerData(SLOTS));
    }

    private RaceControlMenu(int containerId, @Nullable RaceControlBlockEntity control, BlockPos pos, Info info, ContainerData data) {
        super(ModMenus.RACE_CONTROL.get(), containerId);
        this.control = control;
        this.pos = pos;
        this.info = info;
        this.data = data;
        addDataSlots(data);
    }

    private static Info read(RegistryFriendlyByteBuf buf) {
        String owner = buf.readUtf(64);
        boolean manager = buf.readBoolean();
        int laps = buf.readVarInt();
        long fee = buf.readVarLong();
        int cut = buf.readVarInt();
        int flags = buf.readVarInt();
        List<String> entrants = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            entrants.add(buf.readUtf(64));
        }
        List<BoardLine> board = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            board.add(new BoardLine(buf.readUtf(64), buf.readUtf(64), buf.readVarLong()));
        }
        return new Info(owner, manager, laps, fee, cut, flags, entrants, board, buf.readVarInt(), buf.readVarInt(), buf.readVarLong());
    }

    private static void write(RegistryFriendlyByteBuf buf, Info info) {
        buf.writeUtf(info.ownerName(), 64);
        buf.writeBoolean(info.manager());
        buf.writeVarInt(info.laps());
        buf.writeVarLong(info.entryFee());
        buf.writeVarInt(info.cut());
        buf.writeVarInt(info.flags());
        buf.writeVarInt(info.entrants().size());
        info.entrants().forEach(name -> buf.writeUtf(name, 64));
        buf.writeVarInt(info.board().size());
        for (BoardLine line : info.board()) {
            buf.writeUtf(line.name(), 64);
            buf.writeUtf(line.car(), 64);
            buf.writeVarLong(line.ticks());
        }
        buf.writeVarInt(info.races());
        buf.writeVarInt(info.trials());
        buf.writeVarLong(info.earned());
    }

    private static Info infoFor(ServerPlayer player, RaceControlBlockEntity control) {
        return new Info(control.ownerName(), control.canManage(player), control.laps(), control.entryFee(), control.cut(), RaceService.track(control).size(),
                RaceService.entrants(control), control.board().stream().map(lap -> new BoardLine(lap.name(), lap.car(), lap.ticks())).toList(),
                control.races(), control.trials(), control.earned());
    }

    public static void open(ServerPlayer player, RaceControlBlockEntity control) {
        Component title = control.ownerName().isEmpty() ? Component.translatable("container.reikocars.race_control")
                : Component.translatable("container.reikocars.race_control_of", control.ownerName());
        Info info = infoFor(player, control);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, control), title), buf -> {
            buf.writeBlockPos(control.getBlockPos());
            write(buf, info);
        });
    }

    public static RaceControlMenu create(int containerId, ServerPlayer player, RaceControlBlockEntity control) {
        RaceControlMenu menu = new RaceControlMenu(containerId, control, control.getBlockPos(), infoFor(player, control), new SimpleContainerData(SLOTS));
        menu.viewer = player;
        menu.refresh();
        return menu;
    }

    public void refresh() {
        if (control == null || viewer == null) {
            return;
        }
        RaceService.State state = RaceService.state(control);
        data.set(STATE, state == null ? 0 : state.ordinal() + 1);
        data.set(COUNT, RaceService.entrants(control).size());
        long pot = RaceService.pot(control);
        for (int i = 0; i < 4; i++) {
            data.set(POT + i, (int) ((pot >> (16 * i)) & 0xFFFF));
        }
        data.set(ENTERED, RaceService.entered(control, viewer.getUUID()) ? 1 : 0);
        data.set(TRIAL, RaceService.onTrial(viewer.getUUID()) ? 1 : 0);
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Nullable
    public RaceControlBlockEntity control() {
        return control;
    }

    public BlockPos pos() {
        return pos;
    }

    public Info info() {
        return info;
    }

    public boolean isManager() {
        return info.manager();
    }

    /** The race's state, or null if there's none. */
    @Nullable
    public RaceService.State state() {
        int state = data.get(STATE);
        return state <= 0 || state > RaceService.State.values().length ? null : RaceService.State.values()[state - 1];
    }

    public int entrants() {
        return data.get(COUNT);
    }

    public long pot() {
        long value = 0;
        for (int i = 0; i < 4; i++) {
            value |= ((long) (data.get(POT + i) & 0xFFFF)) << (16 * i);
        }
        return value;
    }

    public boolean entered() {
        return data.get(ENTERED) != 0;
    }

    public boolean onTrial() {
        return data.get(TRIAL) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return control == null || (!control.isRemoved() && Container.stillValidBlockEntity(control, player, 8.0F));
    }
}
