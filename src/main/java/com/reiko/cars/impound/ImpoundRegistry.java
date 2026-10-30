package com.reiko.cars.impound;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Where every impound lot is, and whose, so a repossessed car can be sent to one nobody's near. */
public final class ImpoundRegistry extends SavedData {
    private static final String NAME = "reikocars_impound_lots";

    public record Lot(GlobalPos pos, UUID owner, String ownerName) {
    }

    private final List<Lot> lots = new ArrayList<>();

    public static ImpoundRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ImpoundRegistry::new, ImpoundRegistry::load), NAME);
    }

    public void add(GlobalPos pos, UUID owner, String ownerName) {
        lots.removeIf(lot -> lot.pos().equals(pos));
        lots.add(new Lot(pos, owner, ownerName));
        setDirty();
    }

    public void remove(GlobalPos pos) {
        if (lots.removeIf(lot -> lot.pos().equals(pos))) {
            setDirty();
        }
    }

    public List<Lot> all() {
        return List.copyOf(lots);
    }

    /** The lot nearest {@code near} in {@code dimension}, or null if there's none there. */
    @Nullable
    public Lot nearest(ResourceKey<Level> dimension, Vec3 near) {
        return lots.stream().filter(lot -> lot.pos().dimension() == dimension)
                .min(Comparator.comparingDouble(lot -> lot.pos().pos().getCenter().distanceToSqr(near))).orElse(null);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Lot lot : lots) {
            CompoundTag entry = new CompoundTag();
            GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, lot.pos()).result().ifPresent(saved -> entry.put("Pos", saved));
            entry.putUUID("Owner", lot.owner());
            entry.putString("OwnerName", lot.ownerName());
            list.add(entry);
        }
        tag.put("Lots", list);
        return tag;
    }

    private static ImpoundRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        ImpoundRegistry registry = new ImpoundRegistry();
        for (Tag t : tag.getList("Lots", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            if (entry.hasUUID("Owner")) {
                GlobalPos.CODEC.parse(NbtOps.INSTANCE, entry.get("Pos")).result()
                        .ifPresent(pos -> registry.lots.add(new Lot(pos, entry.getUUID("Owner"), entry.getString("OwnerName"))));
            }
        }
        return registry;
    }
}
