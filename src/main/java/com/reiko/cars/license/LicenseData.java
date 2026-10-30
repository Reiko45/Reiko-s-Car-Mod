package com.reiko.cars.license;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/** Every driver's license on the server, by holder. */
public final class LicenseData extends SavedData {
    private static final String NAME = "reikocars_licenses";

    private final Map<UUID, DriverLicense> licenses = new HashMap<>();
    private final Set<String> numbers = new HashSet<>();

    public static LicenseData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(LicenseData::new, LicenseData::load), NAME);
    }

    @Nullable
    public DriverLicense of(UUID holder) {
        return licenses.get(holder);
    }

    /** A new license for {@code holder}, with a number nobody else has ("D 482-913"). */
    DriverLicense issue(UUID holder, String name, long now, String issuedBy, RandomSource random) {
        String number;
        do {
            number = String.format(java.util.Locale.ROOT, "D %03d-%03d", random.nextInt(1000), random.nextInt(1000));
        } while (numbers.contains(number));
        DriverLicense license = new DriverLicense(holder, name, number, now, issuedBy);
        licenses.put(holder, license);
        numbers.add(number);
        setDirty();
        return license;
    }

    /** Drops {@code holder}'s license entirely (for tests and admins). */
    public void revoke(UUID holder) {
        DriverLicense gone = licenses.remove(holder);
        if (gone != null) {
            numbers.remove(gone.number());
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        licenses.values().forEach(license -> list.add(license.save()));
        tag.put("Licenses", list);
        return tag;
    }

    private static LicenseData load(CompoundTag tag, HolderLookup.Provider registries) {
        LicenseData data = new LicenseData();
        for (Tag t : tag.getList("Licenses", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            if (entry.hasUUID("Holder")) {
                DriverLicense license = DriverLicense.load(entry);
                data.licenses.put(license.holder(), license);
                data.numbers.add(license.number());
            }
        }
        return data;
    }
}
