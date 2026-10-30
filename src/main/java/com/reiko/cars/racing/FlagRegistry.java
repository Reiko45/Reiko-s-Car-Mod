package com.reiko.cars.racing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Every numbered race flag: where, whose and its number. A Race Control's track is its owner's numbered flags near it,
 * in number order, and they're found here even when their chunks aren't loaded.
 */
public final class FlagRegistry extends SavedData {
    private static final String NAME = "reikocars_race_flags";

    public record Flag(GlobalPos pos, UUID owner, int number) {
    }

    private final List<Flag> flags = new ArrayList<>();

    public static FlagRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(FlagRegistry::new, FlagRegistry::load), NAME);
    }

    /** Records the flag at {@code pos}: numbered {@code number}, or not on any track if 0. */
    public void set(GlobalPos pos, UUID owner, int number) {
        boolean changed = flags.removeIf(flag -> flag.pos().equals(pos));
        if (number > 0) {
            flags.add(new Flag(pos, owner, number));
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    public void remove(GlobalPos pos) {
        if (flags.removeIf(flag -> flag.pos().equals(pos))) {
            setDirty();
        }
    }

    /** {@code owner}'s numbered flags within {@code radius} of {@code center} in {@code dimension}, in number order. */
    public List<BlockPos> route(ResourceKey<Level> dimension, BlockPos center, double radius, UUID owner) {
        return flags.stream()
                .filter(flag -> flag.pos().dimension() == dimension && owner.equals(flag.owner())
                        && flag.pos().pos().distSqr(center) <= radius * radius)
                .sorted(Comparator.comparingInt(Flag::number).thenComparing(flag -> flag.pos().pos()))
                .map(flag -> flag.pos().pos())
                .toList();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Flag flag : flags) {
            CompoundTag entry = new CompoundTag();
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, flag.pos()).result().ifPresent(saved -> entry.put("Pos", saved));
            entry.putUUID("Owner", flag.owner());
            entry.putInt("Number", flag.number());
            list.add(entry);
        }
        tag.put("Flags", list);
        return tag;
    }

    private static FlagRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        FlagRegistry registry = new FlagRegistry();
        for (Tag t : tag.getList("Flags", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            if (entry.hasUUID("Owner")) {
                GlobalPos.CODEC.parse(NbtOps.INSTANCE, entry.get("Pos")).result()
                        .ifPresent(pos -> registry.flags.add(new Flag(pos, entry.getUUID("Owner"), entry.getInt("Number"))));
            }
        }
        return registry;
    }
}
