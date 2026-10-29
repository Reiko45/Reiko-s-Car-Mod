package com.reiko.cars.key;

import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.registry.ModItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Car keys. A vehicle's owner cuts keys for it from Blank Keys (sneak and right-click it with one), as many as they
 * like, and hands them out. A key in your inventory starts the car (when the server has {@code driving.requireKeys}
 * on, even its owner needs one), gets you into it when it's locked, and locks and unlocks it, from inside with L or
 * from up to {@link #FOB_RANGE} blocks away by right-clicking with the key. Its owner can change the locks (sneak and
 * right-click it twice with one of its keys): that key gets the new lock and every other key stops fitting.
 */
public final class CarKeys {
    /** How far away a key locks and unlocks its vehicle, in blocks. */
    public static final double FOB_RANGE = 32.0;
    /** How long the owner has to confirm changing the locks, in ticks. */
    private static final int REKEY_CONFIRM = 100;
    /** Who asked to change which vehicle's locks, and when. */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private record Pending(int entityId, long time) {
    }

    private CarKeys() {
    }

    /** Whether a car needs its key to start: when the server says so, and someone owns it. */
    public static boolean required(Keyed vehicle) {
        return CarsConfig.get(CarsConfig.REQUIRE_KEYS) && vehicle.owner() != null;
    }

    /** The key's lock, or 0 if {@code stack} isn't a cut key. */
    public static long codeOf(ItemStack stack) {
        CarKey key = stack.get(ModDataComponents.CAR_KEY.get());
        return key == null ? 0L : key.code();
    }

    public static boolean fits(ItemStack stack, long code) {
        return code != 0L && codeOf(stack) == code;
    }

    /** Whether {@code player} carries a key that fits {@code code}: anywhere in their inventory, or the off hand. */
    public static boolean holds(Player player, long code) {
        if (code == 0L) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (List<ItemStack> list : List.of(inventory.items, inventory.offhand)) {
            for (ItemStack stack : list) {
                if (fits(stack, code)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A key cut for {@code vehicle}. */
    public static ItemStack keyFor(Keyed vehicle) {
        ItemStack key = new ItemStack(ModItems.CAR_KEY.get());
        key.set(ModDataComponents.CAR_KEY.get(), new CarKey(vehicle.lockCode(), vehicle.nameKey(), vehicle.color(), vehicle.ownerName()));
        return key;
    }

    private static long newCode() {
        long code;
        do {
            code = ThreadLocalRandom.current().nextLong();
        } while (code == 0L);
        return code;
    }

    /**
     * Sneak and right-click on a vehicle with a key or a blank: cuts a key (with a blank, for the owner) or changes
     * the locks (with one of its keys, for the owner, asked twice). Returns null when there's nothing to do with what
     * {@code player} holds, so the vehicle does what it usually does.
     */
    @Nullable
    public static InteractionResult useOn(Keyed vehicle, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean blank = held.is(ModItems.BLANK_KEY.get());
        boolean cut = held.is(ModItems.CAR_KEY.get());
        if (!player.isSecondaryUseActive() || (!blank && !cut)) {
            return null;
        }
        Entity entity = vehicle.asEntity();
        boolean client = entity.level().isClientSide;
        if (!vehicle.canManage(player) || vehicle.owner() == null) {
            if (blank) {
                if (!client) {
                    player.displayClientMessage(Component.translatable("key.reikocars.owner_only", vehicle.ownerName(), vehicle.kindName()), true);
                }
                return InteractionResult.sidedSuccess(client);
            }
            return null; // someone else's key: carry on as usual (open the storage)
        }
        if (client) {
            return InteractionResult.SUCCESS;
        }
        if (blank) {
            if (vehicle.lockCode() == 0L) {
                vehicle.setLockCode(newCode());
            }
            held.consume(1, player);
            ItemStack key = keyFor(vehicle);
            if (!player.getInventory().add(key)) {
                player.drop(key, false);
            }
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.6F, 1.4F);
            player.displayClientMessage(Component.translatable("key.reikocars.cut", vehicle.kindName()), true);
            return InteractionResult.CONSUME;
        }
        if (!fits(held, vehicle.lockCode())) {
            player.displayClientMessage(Component.translatable("key.reikocars.doesnt_fit", vehicle.kindName()), true);
            return InteractionResult.CONSUME;
        }
        long now = entity.level().getGameTime();
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || pending.entityId() != entity.getId() || now - pending.time() > REKEY_CONFIRM) {
            PENDING.put(player.getUUID(), new Pending(entity.getId(), now));
            player.displayClientMessage(Component.translatable("key.reikocars.rekey_confirm", vehicle.kindName()), true);
            return InteractionResult.CONSUME;
        }
        PENDING.remove(player.getUUID());
        rekey(vehicle, held);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.PLAYERS, 0.8F, 1.6F);
        player.displayClientMessage(Component.translatable("key.reikocars.rekeyed", vehicle.kindName()), true);
        return InteractionResult.CONSUME;
    }

    /** Changes {@code vehicle}'s locks: {@code key} gets the new lock, and every other key stops fitting. */
    public static void rekey(Keyed vehicle, ItemStack key) {
        long code = newCode();
        vehicle.setLockCode(code);
        CarKey data = key.get(ModDataComponents.CAR_KEY.get());
        if (data != null) {
            key.set(ModDataComponents.CAR_KEY.get(), data.withCode(code));
        }
    }

    /** Right-click with a key: locks or unlocks its vehicle, if it's within reach. */
    public static void fob(ServerPlayer player, ItemStack key) {
        long code = codeOf(key);
        Keyed nearest = null;
        double best = FOB_RANGE * FOB_RANGE;
        for (Entity entity : player.level().getEntities((Entity) null, player.getBoundingBox().inflate(FOB_RANGE),
                entity -> entity instanceof Keyed keyed && code != 0L && keyed.lockCode() == code)) {
            double distance = entity.distanceToSqr(player);
            if (distance <= best) {
                best = distance;
                nearest = (Keyed) entity;
            }
        }
        CarKey data = key.get(ModDataComponents.CAR_KEY.get());
        Component name = data == null ? Component.empty() : Component.translatable(data.vehicle());
        if (nearest == null) {
            player.displayClientMessage(Component.translatable("key.reikocars.out_of_range", name, (int) FOB_RANGE), true);
            return;
        }
        nearest.toggleLock(player);
    }
}
