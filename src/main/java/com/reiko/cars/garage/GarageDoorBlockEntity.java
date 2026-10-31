package com.reiko.cars.garage;

import com.reiko.cars.registry.ModBlockEntities;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * One garage door panel: whose it is, which door it's part of (its anchor), and a copy of that door's settings. A
 * door's anchor panel also keeps the door itself: its size, whether it's opening or closing and how far it's got, and,
 * for a parking garage, the cars that have paid to park and what it's taken.
 */
public final class GarageDoorBlockEntity extends BlockEntity {
    /** A car that's paid to park: until when (game time), and its plate and driver, for the owner's screen. */
    public record Parked(long until, String plate, String driver) {
    }

    @Nullable
    private UUID owner;
    private String ownerName = "";
    @Nullable
    private BlockPos anchor;
    private boolean top;
    private GarageSettings settings = GarageSettings.fresh(0L, 100L, 1000L);

    // The anchor's: the door
    private int width;
    private int height;
    private boolean wantOpen;
    /** Opened by a car driving up, so it closes again by itself once it's through. */
    private boolean byCar;
    private float progress;
    private float progressO;
    /** Server: ticks nothing's needed it open (for a door a car opened). */
    int idle;
    /** Server: rows of the doorway clear, from the bottom (-1 until it's first looked: then they're set to match). */
    int rowsOpen = -1;
    private final Map<UUID, Parked> parked = new HashMap<>();
    private long earned;
    private int visits;

    public GarageDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GARAGE_DOOR.get(), pos, state);
    }

    // ---- the panel ----

    @Nullable
    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        setChanged();
    }

    public void setOwner(@Nullable UUID id, String name) {
        owner = id;
        ownerName = id == null ? "" : name;
        setChanged();
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    /** Its owner, or an admin (or anyone, if it has no owner). */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    /** The door's anchor panel, or null if this isn't part of a finished door. */
    @Nullable
    public BlockPos anchor() {
        return anchor;
    }

    /** The anchor's block entity, if it's there. */
    @Nullable
    public GarageDoorBlockEntity anchorEntity() {
        if (anchor == null || level == null) {
            return null;
        }
        return level.getBlockEntity(anchor) instanceof GarageDoorBlockEntity door && door.isAnchor() ? door : null;
    }

    public boolean isAnchor() {
        return anchor != null && anchor.equals(worldPosition);
    }

    /** Whether it's in its door's top row. */
    public boolean isTopRow() {
        return top;
    }

    public GarageSettings settings() {
        return settings;
    }

    public void setSettings(GarageSettings settings) {
        this.settings = settings;
        setChanged();
    }

    /** Joins it to the door at {@code anchor} (null: no door), in its top row or not. */
    void join(@Nullable BlockPos anchor, boolean top, GarageSettings settings) {
        this.anchor = anchor == null ? null : anchor.immutable();
        this.top = top;
        this.settings = settings;
        setChanged();
    }

    /** The door's paint. */
    public int color() {
        return settings.color() >= 0 ? settings.color() : style().defaultColor;
    }

    public GarageDoorBlock.Style style() {
        return getBlockState().getBlock() instanceof GarageDoorBlock block ? block.style() : GarageDoorBlock.Style.SECTIONAL;
    }

    /** The way its panels face. */
    public Direction facing() {
        return getBlockState().hasProperty(GarageDoorBlock.FACING) ? getBlockState().getValue(GarageDoorBlock.FACING) : Direction.SOUTH;
    }

    // ---- the anchor: the door ----

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        setChanged();
    }

    public boolean wantsOpen() {
        return wantOpen;
    }

    public boolean openedByCar() {
        return byCar;
    }

    /** Starts it opening (or closing). {@code car}: a car drove up, so it closes by itself once the car's through. */
    void setWantOpen(boolean open, boolean car) {
        wantOpen = open;
        byCar = open && car;
        idle = 0;
        setChanged();
        sync();
    }

    /** How far open it is, 0 shut to 1 right up. */
    public float progress() {
        return progress;
    }

    public float progress(float partialTick) {
        return progressO + (progress - progressO) * partialTick;
    }

    /** Fully shut. */
    public boolean isShut() {
        return !wantOpen && progress <= 0.0F;
    }

    void setProgress(float progress) {
        this.progress = progress;
        setChanged();
    }

    /** Client: moves the door along as the server does. */
    void clientTick() {
        progressO = progress;
        float step = 1.0F / GarageDoors.travelTicks(height);
        progress = wantOpen ? Math.min(1.0F, progress + step) : Math.max(0.0F, progress - step);
    }

    /** The cars that have paid to park here. */
    public Map<UUID, Parked> parked() {
        return parked;
    }

    public long earned() {
        return earned;
    }

    public int visits() {
        return visits;
    }

    void recordParking(long amount) {
        earned += amount;
        visits++;
        setChanged();
    }

    /** Takes over a door's parking records and takings, when a door's rebuilt. */
    void takeOver(GarageDoorBlockEntity old) {
        if (old == this) {
            return;
        }
        parked.putAll(old.parked);
        earned += old.earned;
        visits += old.visits;
        setChanged();
    }

    /** Sends the door's state to everyone watching it. */
    public void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        if (anchor != null) {
            tag.put("Anchor", NbtUtils.writeBlockPos(anchor));
        }
        tag.putBoolean("Top", top);
        tag.put("Settings", settings.save());
        if (isAnchor()) {
            tag.putInt("Width", width);
            tag.putInt("Height", height);
            tag.putBoolean("WantOpen", wantOpen);
            tag.putBoolean("ByCar", byCar);
            tag.putFloat("Progress", progress);
            ListTag list = new ListTag();
            parked.forEach((car, ticket) -> {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("Car", car);
                entry.putLong("Until", ticket.until());
                entry.putString("Plate", ticket.plate());
                entry.putString("Driver", ticket.driver());
                list.add(entry);
            });
            tag.put("Parked", list);
            tag.putLong("Earned", earned);
            tag.putInt("Visits", visits);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        anchor = NbtUtils.readBlockPos(tag, "Anchor").orElse(null);
        top = tag.getBoolean("Top");
        settings = GarageSettings.load(tag.getCompound("Settings"), settings);
        width = tag.getInt("Width");
        height = tag.getInt("Height");
        boolean wasOpen = wantOpen;
        wantOpen = tag.getBoolean("WantOpen");
        byCar = tag.getBoolean("ByCar");
        float loaded = tag.getFloat("Progress");
        if (level == null || !level.isClientSide || wasOpen != wantOpen || Math.abs(loaded - progress) > 0.5F) {
            progress = loaded; // (on the client, an update while it's moving doesn't make it jump back)
            progressO = loaded;
        }
        parked.clear();
        for (Tag entry : tag.getList("Parked", Tag.TAG_COMPOUND)) {
            CompoundTag ticket = (CompoundTag) entry;
            parked.put(ticket.getUUID("Car"), new Parked(ticket.getLong("Until"), ticket.getString("Plate"), ticket.getString("Driver")));
        }
        earned = tag.getLong("Earned");
        visits = tag.getInt("Visits");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        int before = color();
        super.onDataPacket(connection, packet, registries);
        if (level != null && level.isClientSide && color() != before) {
            // A loose panel's paint is its block model's tint: draw it again.
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_IMMEDIATE);
        }
    }
}
