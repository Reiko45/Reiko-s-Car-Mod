package com.reiko.cars.guide;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.compat.patchouli.CarManual;
import com.reiko.cars.config.CarsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The Car Manual: the mod's guide book, shown by Patchouli. Everything here is safe to call without Patchouli
 * installed: there's simply no book then.
 *
 * <p>Every player is given one the first time they join a world (once, ever: it's remembered across deaths), it can
 * be crafted from a book and an iron ingot, and the Car Workbench has a button that opens it.
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class Guide {
    /** Set, in the player's persisted data, once they've been given the manual. */
    static final String GIVEN = "reikocars_manual_given";

    private Guide() {
    }

    /** Whether there's a manual: whether Patchouli is installed. */
    public static boolean available() {
        return ModList.get() != null && ModList.get().isLoaded("patchouli");
    }

    /** A copy of the manual, or nothing without Patchouli. */
    public static ItemStack book() {
        return available() ? CarManual.stack() : ItemStack.EMPTY;
    }

    /** Opens the manual at {@code entry} (like {@code getting_started/workbench}). Client side; does nothing without Patchouli. */
    public static void open(String entry) {
        open(entry, 0);
    }

    /** Opens the manual at page {@code page} of {@code entry}. Client side; does nothing without Patchouli. */
    public static void open(String entry, int page) {
        if (available()) {
            CarManual.openAt(ReikoCars.id(entry), page);
        }
    }

    /** Opens the manual at its front page. Client side; does nothing without Patchouli. */
    public static void openFront() {
        if (available()) {
            CarManual.open();
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && CarsConfig.get(CarsConfig.GIVE_MANUAL)) {
            giveFirstTime(player);
        }
    }

    /** Gives {@code player} the manual, unless they've had one before. Returns whether it did. */
    public static boolean giveFirstTime(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(GIVEN)) {
            return false;
        }
        ItemStack manual = book();
        if (manual.isEmpty()) {
            return false;
        }
        if (!player.getInventory().add(manual)) {
            player.drop(manual, false);
        }
        persisted.putBoolean(GIVEN, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        return true;
    }

    /** The id of the manual, for recipes and tests. */
    public static ResourceLocation id() {
        return ReikoCars.id("car_manual");
    }
}
