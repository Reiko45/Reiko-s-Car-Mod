package com.reiko.cars.compat.patchouli;

import com.reiko.cars.ReikoCars;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * The Car Manual, through Patchouli's API. Only {@link com.reiko.cars.guide.Guide} calls this, and only once it has
 * checked that Patchouli is installed, so this class (and Patchouli's) is never loaded without it.
 */
public final class CarManual {
    /** The book's id: its book.json is {@code data/reikocars/patchouli_books/car_manual/book.json}. */
    public static final ResourceLocation ID = ReikoCars.id("car_manual");

    private CarManual() {
    }

    /** A copy of the book. */
    public static ItemStack stack() {
        return PatchouliAPI.get().getBookStack(ID);
    }

    /** Opens the book at page {@code page} of {@code entry}. Client side. */
    public static void openAt(ResourceLocation entry, int page) {
        PatchouliAPI.get().openBookEntry(ID, entry, page);
    }

    /** Opens the book at its front page. Client side. */
    public static void open() {
        PatchouliAPI.get().openBookGUI(ID);
    }
}
