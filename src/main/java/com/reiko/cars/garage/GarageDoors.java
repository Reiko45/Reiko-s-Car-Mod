package com.reiko.cars.garage;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.trailer.TrailerEntity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Garage doors: panels joining into doors, opening and closing them (by hand, by remote, by wall button, and by
 * themselves for cars driving up), who may open them, and whether a car is safe in a closed garage.
 *
 * <p>Who may open a door: its owner, an admin, anyone carrying a remote linked to it, and anyone on the inside (nobody's
 * shut in). It opens by itself for a car driving up to it, on either side, if the car's driver may open it, the car is
 * its owner's, or the car's paid to park there; it shuts once the car's through.
 */
public final class GarageDoors {
    /** How deep behind a closed door a car counts as in the garage, in blocks. */
    public static final double GARAGE_DEPTH = 16.0;

    /** What came of joining panels: a finished door (so big), or not, and what to tell whoever put the panel up. */
    public record Joined(boolean formed, int width, int height, Component message) {
    }

    private GarageDoors() {
    }

    public static int maxWidth() {
        return CarsConfig.get(CarsConfig.GARAGE_MAX_WIDTH);
    }

    public static int maxHeight() {
        return CarsConfig.get(CarsConfig.GARAGE_MAX_HEIGHT);
    }

    /** How long a door {@code height} panels tall takes to open or shut, in ticks. */
    public static int travelTicks(int height) {
        return Mth.clamp(height * 8, 20, 60);
    }

    /** The way a door with panels facing {@code facing} runs: to the right, seen from in front. */
    public static Direction along(Direction facing) {
        return facing.getCounterClockWise();
    }

    private static int alongCoord(BlockPos pos, Direction along) {
        return along.getAxis().choose(pos.getX(), pos.getY(), pos.getZ()) * along.getAxisDirection().getStep();
    }

    /** The way out of the garage: the way its panels face, unless it was built from inside. */
    public static Direction outward(GarageDoorBlockEntity door) {
        return door.settings().inward() ? door.facing().getOpposite() : door.facing();
    }

    /** The door's panels, bottom row first. */
    public static List<BlockPos> cells(GarageDoorBlockEntity door) {
        List<BlockPos> cells = new ArrayList<>();
        Direction along = along(door.facing());
        for (int y = 0; y < door.height(); y++) {
            for (int x = 0; x < door.width(); x++) {
                cells.add(door.getBlockPos().relative(along, x).above(y));
            }
        }
        return cells;
    }

    /** The finished door the panel at {@code pos} is part of, if it is. */
    @Nullable
    public static GarageDoorBlockEntity doorAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel ? panel.anchorEntity() : null;
    }

    /** The door's name, or "the garage door at x, y, z". */
    public static Component describe(GarageDoorBlockEntity door) {
        BlockPos pos = door.getBlockPos();
        return door.settings().name().isEmpty()
                ? Component.translatable("garage.reikocars.door_at", pos.getX(), pos.getY(), pos.getZ())
                : Component.literal(door.settings().name());
    }

    /** The nearest finished door within {@code radius} blocks of {@code pos} (to its doorway), if there's one. */
    @Nullable
    public static GarageDoorBlockEntity nearestDoor(Level level, BlockPos pos, double radius) {
        GarageDoorBlockEntity best = null;
        double bestDistance = radius * radius;
        int reach = Mth.ceil(radius / 16.0);
        ChunkPos chunk = new ChunkPos(pos);
        Vec3 point = Vec3.atCenterOf(pos);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (!(level.getChunk(chunk.x + dx, chunk.z + dz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) instanceof LevelChunk loaded)) {
                    continue;
                }
                for (BlockEntity entity : loaded.getBlockEntities().values()) {
                    if (entity instanceof GarageDoorBlockEntity door && door.isAnchor() && door.width() > 0) {
                        AABB doorway = doorway(door);
                        double x = Mth.clamp(point.x, doorway.minX, doorway.maxX);
                        double y = Mth.clamp(point.y, doorway.minY, doorway.maxY);
                        double z = Mth.clamp(point.z, doorway.minZ, doorway.maxZ);
                        double distance = point.distanceToSqr(x, y, z);
                        if (distance <= bestDistance) {
                            best = door;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }
        return best;
    }

    // ---- joining ----

    /**
     * Joins the panel at {@code start} with every panel touching it in its plane (same kind, same way, same owner): a
     * door if they make a rectangle small enough, else loose panels. A door rebuilt keeps its settings, parking records
     * and takings.
     */
    @Nullable
    public static Joined rejoin(ServerLevel level, BlockPos start) {
        return rejoin(level, start, null);
    }

    @Nullable
    private static Joined rejoin(ServerLevel level, BlockPos start, @Nullable GarageDoorBlockEntity removed) {
        BlockState startState = level.getBlockState(start);
        if (!(startState.getBlock() instanceof GarageDoorBlock block) || !(level.getBlockEntity(start) instanceof GarageDoorBlockEntity first)) {
            return null;
        }
        Direction facing = startState.getValue(GarageDoorBlock.FACING);
        Direction along = along(facing);
        UUID owner = first.owner();
        List<BlockPos> cells = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty() && cells.size() < 4096) {
            BlockPos pos = queue.poll();
            cells.add(pos);
            for (Direction side : new Direction[] {Direction.UP, Direction.DOWN, along, along.getOpposite()}) {
                BlockPos next = pos.relative(side).immutable();
                if (seen.contains(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (state.is(block) && state.getValue(GarageDoorBlock.FACING) == facing
                        && level.getBlockEntity(next) instanceof GarageDoorBlockEntity panel && Objects.equals(panel.owner(), owner)) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        int minA = Integer.MAX_VALUE;
        int maxA = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BlockPos pos : cells) {
            int a = alongCoord(pos, along);
            minA = Math.min(minA, a);
            maxA = Math.max(maxA, a);
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
        }
        int width = maxA - minA + 1;
        int height = maxY - minY + 1;
        boolean rectangle = cells.size() == width * height;
        boolean fits = width <= maxWidth() && height <= maxHeight();

        // What to keep: the old door's settings, parking records and takings.
        List<GarageDoorBlockEntity> oldAnchors = new ArrayList<>();
        if (removed != null && removed.isAnchor()) {
            oldAnchors.add(removed);
        }
        GarageSettings settings = null;
        for (BlockPos pos : cells) {
            if (level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel && panel.isAnchor()) {
                oldAnchors.add(panel);
            }
        }
        for (GarageDoorBlockEntity old : oldAnchors) {
            if (settings == null || old.width() * old.height() > 0) {
                settings = old.settings();
                break;
            }
        }
        if (settings == null) {
            for (BlockPos pos : cells) {
                if (level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel && panel.settings().code() != 0L) {
                    settings = panel.settings();
                    break;
                }
            }
        }
        if (settings == null) {
            settings = GarageSettings.fresh(newCode(), cents(CarsConfig.get(CarsConfig.GARAGE_HOUR_PRICE)), cents(CarsConfig.get(CarsConfig.GARAGE_DAY_PRICE)));
        }

        if (rectangle && fits) {
            BlockPos anchorPos = null;
            for (BlockPos pos : cells) {
                if (alongCoord(pos, along) == minA && pos.getY() == minY) {
                    anchorPos = pos;
                }
            }
            // Which side's the garage: under a roof on one side and not the other, that's inside; if it can't tell, as it was.
            Boolean inside = builtFromInside(level, anchorPos, facing, width, height);
            if (inside != null) {
                settings = settings.withInward(inside);
            }
            for (BlockPos pos : cells) {
                BlockState state = level.getBlockState(pos)
                        .setValue(GarageDoorBlock.PART, pos.equals(anchorPos) ? GarageDoorBlock.Part.ANCHOR : GarageDoorBlock.Part.MEMBER)
                        .setValue(GarageDoorBlock.OPEN, false);
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                if (level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel) {
                    panel.join(anchorPos, pos.getY() == maxY, settings);
                }
            }
            GarageDoorBlockEntity door = (GarageDoorBlockEntity) level.getBlockEntity(anchorPos);
            for (GarageDoorBlockEntity old : oldAnchors) {
                door.takeOver(old);
            }
            door.setSize(width, height);
            door.setProgress(0.0F);
            door.rowsOpen = 0;
            door.setWantOpen(false, false);
            for (BlockPos pos : cells) {
                if (level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel) {
                    panel.sync();
                }
            }
            return new Joined(true, width, height, Component.translatable("garage.reikocars.formed", block.getName(), width, height));
        }
        for (BlockPos pos : cells) {
            BlockState state = level.getBlockState(pos).setValue(GarageDoorBlock.PART, GarageDoorBlock.Part.SINGLE).setValue(GarageDoorBlock.OPEN, false);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel) {
                panel.join(null, false, settings);
                panel.sync();
            }
        }
        Component message = !fits
                ? Component.translatable("garage.reikocars.too_big", maxWidth(), maxHeight())
                : Component.translatable("garage.reikocars.unfinished", cells.size(), width, height);
        return new Joined(false, width, height, message);
    }

    /** A panel's gone: rejoins what's left of its door (which may now be two, or none). */
    static void afterRemoved(ServerLevel level, BlockPos pos, BlockState old, @Nullable GarageDoorBlockEntity removed) {
        Direction along = along(old.getValue(GarageDoorBlock.FACING));
        UUID owner = removed == null ? null : removed.owner();
        Set<BlockPos> done = new HashSet<>();
        for (Direction side : new Direction[] {Direction.UP, Direction.DOWN, along, along.getOpposite()}) {
            BlockPos next = pos.relative(side);
            if (done.contains(next) || !(level.getBlockState(next).getBlock() instanceof GarageDoorBlock)) {
                continue;
            }
            BlockState state = level.getBlockState(next);
            if (!state.is(old.getBlock()) || state.getValue(GarageDoorBlock.FACING) != old.getValue(GarageDoorBlock.FACING)
                    || (owner != null && level.getBlockEntity(next) instanceof GarageDoorBlockEntity other && !Objects.equals(other.owner(), owner))) {
                continue; // not part of the same door: nothing's changed for it
            }
            // (the gone panel's records only go to what's left of its own door)
            boolean same = removed != null && level.getBlockEntity(next) instanceof GarageDoorBlockEntity neighbor
                    && Objects.equals(neighbor.anchor(), removed.anchor());
            rejoin(level, next, same ? removed : null);
            if (level.getBlockEntity(next) instanceof GarageDoorBlockEntity panel && panel.anchorEntity() != null) {
                done.addAll(cells(panel.anchorEntity()));
            } else {
                done.add(next);
            }
            if (same) {
                removed = null; // its records go to one door only
            }
        }
    }

    /**
     * Whether the door was put up from inside the garage: there's a roof over its front and not over its back, so its
     * inside is the way the panels face (true); or over its back and not its front (false). Null if it can't tell.
     */
    @Nullable
    private static Boolean builtFromInside(Level level, BlockPos anchor, Direction facing, int width, int height) {
        BlockPos top = anchor.relative(along(facing), width / 2).above(height - 1);
        boolean roofInFront = roofed(level, top.relative(facing));
        boolean roofBehind = roofed(level, top.relative(facing.getOpposite()));
        return roofInFront == roofBehind ? null : roofInFront;
    }

    /** Whether there's something solid somewhere over {@code pos}. */
    private static boolean roofed(Level level, BlockPos pos) {
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY() + 1;
    }

    public static long newCode() {
        long code;
        do {
            code = ThreadLocalRandom.current().nextLong();
        } while (code == 0L);
        return code;
    }

    private static long cents(double dollars) {
        return Math.round(dollars * 100.0);
    }

    // ---- who may open it ----

    /** Whether {@code player} may open {@code door}: its owner, an admin, or someone carrying a remote linked to it. */
    public static boolean canOpen(Player player, GarageDoorBlockEntity door) {
        return door.owner() == null || door.isOwner(player) || player.hasPermissions(2) || holdsRemote(player, door);
    }

    /** Whether {@code player} carries a remote that opens {@code door}. */
    public static boolean holdsRemote(Player player, GarageDoorBlockEntity door) {
        String dimension = player.level().dimension().location().toString();
        for (List<ItemStack> list : List.of(player.getInventory().items, player.getInventory().offhand)) {
            for (ItemStack stack : list) {
                RemoteLinks links = stack.get(ModDataComponents.GARAGE_LINKS.get());
                if (links != null && links.opens(dimension, door.getBlockPos(), door.settings().code())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** How far {@code point} is in front of the door: out of the garage if positive, in it if negative. */
    public static double depth(Vec3 point, GarageDoorBlockEntity door) {
        Direction out = outward(door);
        double plane = out.getAxis().choose(door.getBlockPos().getX(), door.getBlockPos().getY(), door.getBlockPos().getZ()) + 0.5;
        return (out.getAxis().choose(point.x, point.y, point.z) - plane) * out.getAxisDirection().getStep();
    }

    /** Where {@code point} is along the door, from its left edge (seen from in front), in blocks. */
    public static double across(Vec3 point, GarageDoorBlockEntity door) {
        Direction along = along(door.facing());
        BlockPos anchor = door.getBlockPos();
        double edge = along.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? along.getAxis().choose(anchor.getX(), anchor.getY(), anchor.getZ())
                : along.getAxis().choose(anchor.getX(), anchor.getY(), anchor.getZ()) + 1.0;
        return (along.getAxis().choose(point.x, point.y, point.z) - edge) * along.getAxisDirection().getStep();
    }

    // ---- opening and closing ----

    /** Someone clicked a panel. */
    static void click(Player player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel level) || !(level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel)) {
            return;
        }
        GarageDoorBlockEntity door = panel.anchorEntity();
        if (door == null) {
            Joined joined = rejoin(level, pos);
            if (joined != null && !joined.formed()) {
                player.displayClientMessage(joined.message(), true);
                return;
            }
            door = panel.anchorEntity();
            if (door == null) {
                return;
            }
        }
        if (player.isSecondaryUseActive() && door.canManage(player) && door.owner() != null && player instanceof ServerPlayer serverPlayer) {
            GarageMenu.open(serverPlayer, door);
            return;
        }
        if (canOpen(player, door) || depth(player.position(), door) < 0.0) {
            toggle(door);
        } else {
            player.displayClientMessage(Component.translatable("garage.reikocars.locked", door.ownerName()), true);
        }
    }

    /** Paints the door (or the loose panel) at {@code pos}, if {@code player} may. */
    static void paint(Player player, BlockPos pos, DyeItem dye, ItemStack stack) {
        if (!(player.level().getBlockEntity(pos) instanceof GarageDoorBlockEntity panel)) {
            return;
        }
        if (!panel.canManage(player)) {
            player.displayClientMessage(Component.translatable("garage.reikocars.not_yours", panel.ownerName()), true);
            return;
        }
        int rgb = dye.getDyeColor().getTextureDiffuseColor() & 0xFFFFFF;
        GarageDoorBlockEntity door = panel.anchorEntity();
        List<BlockPos> cells = door != null ? cells(door) : List.of(pos);
        for (BlockPos cell : cells) {
            if (player.level().getBlockEntity(cell) instanceof GarageDoorBlockEntity each) {
                each.setSettings(each.settings().withColor(rgb));
                each.sync();
            }
        }
        stack.consume(1, player);
        player.level().playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** Applies new settings to every panel of the door. */
    static void applySettings(GarageDoorBlockEntity door, GarageSettings settings) {
        for (BlockPos cell : cells(door)) {
            if (door.getLevel().getBlockEntity(cell) instanceof GarageDoorBlockEntity each) {
                each.setSettings(settings);
            }
        }
        door.sync();
    }

    /** Opens the door if it's shut or shutting, shuts it if it's open or opening. */
    public static void toggle(GarageDoorBlockEntity door) {
        if (door.wantsOpen()) {
            close(door);
        } else {
            open(door, false);
        }
    }

    /** Opens the door. {@code byCar}: for a car driving up, so it shuts again by itself once the car's through. */
    public static void open(GarageDoorBlockEntity door, boolean byCar) {
        if (door.wantsOpen()) {
            return;
        }
        door.setWantOpen(true, byCar);
        sound(door, true);
    }

    public static void close(GarageDoorBlockEntity door) {
        if (!door.wantsOpen()) {
            return;
        }
        door.setWantOpen(false, false);
        sound(door, false);
    }

    private static void sound(GarageDoorBlockEntity door, boolean opening) {
        Level level = door.getLevel();
        if (level == null) {
            return;
        }
        Vec3 middle = middle(door);
        boolean shutter = door.style() == GarageDoorBlock.Style.SHUTTER;
        level.playSound(null, middle.x, middle.y, middle.z, opening ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                SoundSource.BLOCKS, 0.7F, shutter ? 0.55F : 0.75F);
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, shutter ? 0.6F : 0.8F);
    }

    /** The middle of the doorway. */
    public static Vec3 middle(GarageDoorBlockEntity door) {
        Direction along = along(door.facing());
        Vec3 corner = Vec3.atBottomCenterOf(door.getBlockPos());
        return corner.add(Vec3.atLowerCornerOf(along.getNormal()).scale((door.width() - 1) / 2.0)).add(0.0, door.height() / 2.0, 0.0);
    }

    /** The doorway itself: the door's panels' blocks. */
    public static AABB doorway(GarageDoorBlockEntity door) {
        Direction along = along(door.facing());
        BlockPos far = door.getBlockPos().relative(along, door.width() - 1).above(door.height() - 1);
        return new AABB(Vec3.atLowerCornerOf(door.getBlockPos()), Vec3.atLowerCornerOf(far).add(1.0, 1.0, 1.0));
    }

    /** Whether anything's in the doorway that the door mustn't come down on. */
    static boolean doorwayBusy(Level level, GarageDoorBlockEntity door) {
        return !level.getEntities((Entity) null, doorway(door).inflate(0.05), entity -> entity.isAlive() && !entity.isSpectator()
                && (entity instanceof LivingEntity || entity instanceof CarEntity || entity instanceof TrailerEntity)).isEmpty();
    }

    /** Each tick, on the server: the door moves, its rows clear or close, and it opens and shuts for cars. */
    static void serverTick(ServerLevel level, GarageDoorBlockEntity door) {
        int height = door.height();
        if (height <= 0 || door.width() <= 0) {
            return;
        }
        boolean want = door.wantsOpen();
        float progress = door.progress();
        if (!want && progress > 0.0F && level.getGameTime() % 2 == 0 && doorwayBusy(level, door)) {
            // Something's in the way: back up, as a real door's safety sensor would.
            open(door, door.openedByCar());
            want = true;
        }
        float step = 1.0F / travelTicks(height);
        float next = want ? Math.min(1.0F, progress + step) : Math.max(0.0F, progress - step);
        if (next != progress) {
            door.setProgress(next);
            if (next == 1.0F || next == 0.0F) {
                Vec3 middle = middle(door);
                level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 0.6F);
            }
        }
        int rows = next >= 1.0F ? height : (int) Math.floor(next * height + 1.0E-4F);
        if (rows != door.rowsOpen) {
            setRows(level, door, rows);
            door.rowsOpen = rows;
        }
        if (level.getGameTime() % 4 == 0) {
            forCars(level, door);
        }
        if (level.getGameTime() % 1200 == 0) {
            ParkingService.forgetOld(door, level.getGameTime());
        }
    }

    /** Clears the bottom {@code rows} rows of the doorway, and closes the rest. */
    private static void setRows(ServerLevel level, GarageDoorBlockEntity door, int rows) {
        Direction along = along(door.facing());
        for (int y = 0; y < door.height(); y++) {
            for (int x = 0; x < door.width(); x++) {
                BlockPos cell = door.getBlockPos().relative(along, x).above(y);
                BlockState state = level.getBlockState(cell);
                if (state.getBlock() instanceof GarageDoorBlock && state.getValue(GarageDoorBlock.OPEN) != y < rows) {
                    level.setBlock(cell, state.setValue(GarageDoorBlock.OPEN, y < rows), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /** Opens the door for a car driving up to it that it lets in, and shuts it again once there's none. */
    private static void forCars(ServerLevel level, GarageDoorBlockEntity door) {
        boolean wanted = false;
        double reach = CarsConfig.get(CarsConfig.GARAGE_REACH);
        for (CarEntity car : level.getEntitiesOfClass(CarEntity.class, doorway(door).inflate(reach))) {
            if (!(car.getControllingPassenger() instanceof Player driver) || car.standingStill()) {
                continue;
            }
            double across = across(car.position(), door);
            if (across < -1.0 || across > door.width() + 1.0 || Math.abs(depth(car.position(), door)) > reach
                    || car.getY() < door.getBlockPos().getY() - 1.0 || car.getY() > door.getBlockPos().getY() + door.height()) {
                continue;
            }
            // Heading in or out, not driving past.
            Vec3 normal = Vec3.atLowerCornerOf(outward(door).getNormal());
            if (Math.abs(car.forward().dot(normal)) < 0.5) {
                continue;
            }
            if (letsIn(car, driver, door)) {
                wanted = true;
                if (driver instanceof net.minecraft.server.level.ServerPlayer player) {
                    com.reiko.cars.advancement.CarAdvancements.award(player, "garage");
                }
                break;
            }
        }
        if (wanted) {
            open(door, true);
            door.idle = 0;
        } else if (door.wantsOpen() && door.openedByCar() && door.progress() >= 1.0F) {
            door.idle = doorwayBusy(level, door) ? 0 : door.idle + 4;
            if (door.idle >= CarsConfig.get(CarsConfig.GARAGE_CLOSE_SECONDS) * 20) {
                close(door);
            }
        }
    }

    /**
     * Whether the door opens by itself for {@code car}, driven by {@code driver}: the driver may open it, or it's the
     * door's owner's car (if it opens for cars at all); or the car's paid to park there.
     */
    public static boolean letsIn(CarEntity car, Player driver, GarageDoorBlockEntity door) {
        GarageSettings settings = door.settings();
        if (settings.autoOpen() && (canOpen(driver, door) || (door.owner() != null && door.owner().equals(car.owner())))) {
            return true;
        }
        return settings.parking() && ParkingService.paidUp(car, door);
    }

    // ---- safe in the garage ----

    /**
     * Whether {@code car} is shut in a garage: behind a closed garage door (within {@link #GARAGE_DEPTH} blocks, across
     * its width) and under a roof. Nobody without its key can get in, tow it away or impound it there, and it doesn't
     * rust.
     */
    public static boolean garaged(CarEntity car) {
        Level level = car.level();
        // Something solid over it (glass will do).
        int top = BlockPos.containing(car.getX(), car.getY() + car.getBbHeight(), car.getZ()).getY();
        if (level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, car.getBlockX(), car.getBlockZ()) <= top) {
            return false;
        }
        ChunkPos chunk = car.chunkPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!(level.getChunk(chunk.x + dx, chunk.z + dz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false) instanceof LevelChunk loaded)) {
                    continue;
                }
                for (BlockEntity entity : loaded.getBlockEntities().values()) {
                    if (entity instanceof GarageDoorBlockEntity door && door.isAnchor() && door.isShut() && inGarage(car.position(), door)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Whether {@code point} is behind {@code door}, across its width, at its height: in its garage. */
    public static boolean inGarage(Vec3 point, GarageDoorBlockEntity door) {
        double depth = depth(point, door);
        double across = across(point, door);
        return depth < -0.5 && depth > -GARAGE_DEPTH && across > -0.5 && across < door.width() + 0.5
                && point.y >= door.getBlockPos().getY() - 1.0 && point.y < door.getBlockPos().getY() + door.height();
    }
}
