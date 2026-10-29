package com.reiko.cars.mail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/** Where every player's mailboxes are, so a parcel can carry its addressee's address for the mail carrier. */
public final class MailRegistry extends SavedData {
    private static final String NAME = "reikocars_mailboxes";
    private final Map<UUID, List<GlobalPos>> boxes = new HashMap<>();

    public static MailRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(MailRegistry::new, MailRegistry::load), NAME);
    }

    public void add(UUID owner, GlobalPos pos) {
        List<GlobalPos> list = boxes.computeIfAbsent(owner, id -> new ArrayList<>());
        if (!list.contains(pos)) {
            list.add(pos);
            setDirty();
        }
    }

    public void remove(UUID owner, GlobalPos pos) {
        List<GlobalPos> list = boxes.get(owner);
        if (list != null && list.remove(pos)) {
            if (list.isEmpty()) {
                boxes.remove(owner);
            }
            setDirty();
        }
    }

    /** The first mailbox {@code owner} put up that's still standing, if any. */
    @Nullable
    public GlobalPos first(UUID owner) {
        List<GlobalPos> list = boxes.get(owner);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public List<GlobalPos> all(UUID owner) {
        return List.copyOf(boxes.getOrDefault(owner, List.of()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        boxes.forEach((owner, positions) -> {
            for (GlobalPos pos : positions) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("Owner", owner);
                GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(saved -> entry.put("Pos", saved));
                list.add(entry);
            }
        });
        tag.put("Mailboxes", list);
        return tag;
    }

    private static MailRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        MailRegistry registry = new MailRegistry();
        for (Tag t : tag.getList("Mailboxes", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            if (entry.hasUUID("Owner")) {
                GlobalPos.CODEC.parse(NbtOps.INSTANCE, entry.get("Pos")).result()
                        .ifPresent(pos -> registry.boxes.computeIfAbsent(entry.getUUID("Owner"), id -> new ArrayList<>()).add(pos));
            }
        }
        return registry;
    }
}
