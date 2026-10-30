package com.reiko.cars.license;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * A player's driver's license, as the server keeps it: its number, who issued it, the penalty points on it (each
 * offence with when it happened, so old ones drop off) and any suspension.
 */
public final class DriverLicense {
    /** Points for one offence: when, how many, and what for (a lang key). */
    public record Offence(long time, int points, String reason) {
    }

    private final UUID holder;
    private String name;
    private final String number;
    private final long issued;
    private final String issuedBy;
    private final List<Offence> offences = new ArrayList<>();
    private long suspendedUntil;
    private int suspensions;

    public DriverLicense(UUID holder, String name, String number, long issued, String issuedBy) {
        this.holder = holder;
        this.name = name;
        this.number = number;
        this.issued = issued;
        this.issuedBy = issuedBy;
    }

    public UUID holder() {
        return holder;
    }

    public String name() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String number() {
        return number;
    }

    public long issued() {
        return issued;
    }

    /** Whose DMV issued it. */
    public String issuedBy() {
        return issuedBy;
    }

    public List<Offence> offences() {
        return List.copyOf(offences);
    }

    void addOffence(Offence offence) {
        offences.add(offence);
    }

    void clearOffences() {
        offences.clear();
    }

    /** Drops offences older than {@code since}. */
    void expire(long since) {
        offences.removeIf(offence -> offence.time() < since);
    }

    /** The points from offences since {@code since}. */
    public int points(long since) {
        return offences.stream().filter(offence -> offence.time() >= since).mapToInt(Offence::points).sum();
    }

    public long suspendedUntil() {
        return suspendedUntil;
    }

    public boolean suspended(long now) {
        return now < suspendedUntil;
    }

    void suspend(long until) {
        suspendedUntil = until;
        suspensions++;
    }

    public int suspensions() {
        return suspensions;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Holder", holder);
        tag.putString("Name", name);
        tag.putString("Number", number);
        tag.putLong("Issued", issued);
        tag.putString("IssuedBy", issuedBy);
        ListTag list = new ListTag();
        for (Offence offence : offences) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Time", offence.time());
            entry.putInt("Points", offence.points());
            entry.putString("Reason", offence.reason());
            list.add(entry);
        }
        tag.put("Offences", list);
        tag.putLong("SuspendedUntil", suspendedUntil);
        tag.putInt("Suspensions", suspensions);
        return tag;
    }

    static DriverLicense load(CompoundTag tag) {
        DriverLicense license = new DriverLicense(tag.getUUID("Holder"), tag.getString("Name"), tag.getString("Number"), tag.getLong("Issued"),
                tag.getString("IssuedBy"));
        for (Tag t : tag.getList("Offences", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            license.offences.add(new Offence(entry.getLong("Time"), entry.getInt("Points"), entry.getString("Reason")));
        }
        license.suspendedUntil = tag.getLong("SuspendedUntil");
        license.suspensions = tag.getInt("Suspensions");
        return license;
    }
}
