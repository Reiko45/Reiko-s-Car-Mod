package com.reiko.cars.plate;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Every plate given out on the server, so no two cars are given the same one. A plate stays taken while its car is
 * picked up (it's kept on the item), and is freed when a custom plate replaces it.
 */
public final class PlateRegistry extends SavedData {
    private static final String NAME = "reikocars_plates";

    private final Set<String> taken = new HashSet<>();

    public static PlateRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(PlateRegistry::new, PlateRegistry::load), NAME);
    }

    /** A new random plate nobody has, now taken. */
    public String issue(RandomSource random) {
        String plate;
        int tries = 0;
        do {
            plate = Plates.random(random);
        } while (taken.contains(plate) && ++tries < 10_000);
        taken.add(plate);
        setDirty();
        return plate;
    }

    public boolean isTaken(String plate) {
        return taken.contains(plate);
    }

    /** Takes {@code plate}: false if someone has it already. */
    public boolean take(String plate) {
        if (!taken.add(plate)) {
            return false;
        }
        setDirty();
        return true;
    }

    /** Marks {@code plate} as in use (a car put down with it), whether or not it was already. */
    public void keep(String plate) {
        if (taken.add(plate)) {
            setDirty();
        }
    }

    public void release(String plate) {
        if (taken.remove(plate)) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        taken.stream().sorted().forEach(plate -> list.add(StringTag.valueOf(plate)));
        tag.put("Plates", list);
        return tag;
    }

    private static PlateRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        PlateRegistry registry = new PlateRegistry();
        for (Tag t : tag.getList("Plates", Tag.TAG_STRING)) {
            registry.taken.add(t.getAsString());
        }
        return registry;
    }
}
