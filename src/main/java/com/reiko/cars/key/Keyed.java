package com.reiko.cars.key;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** A car or trailer that keys can be cut for. */
public interface Keyed {
    /** Its lock: 0 until the first key is cut, then a random number that its keys carry. */
    long lockCode();

    void setLockCode(long code);

    @Nullable
    UUID owner();

    String ownerName();

    /** Its owner, an admin, or anyone if nobody owns it: who may cut keys and change the locks. */
    boolean canManage(Player player);

    boolean isLocked();

    /** Locks or unlocks it for {@code player}, who has its key; false if they may not. */
    boolean toggleLock(ServerPlayer player);

    /** Its paint, for the key's fob. */
    int color();

    /** What it is, like "Sedan". */
    Component kindName();

    /** Its name's translation key, which the key keeps. */
    String nameKey();

    default Entity asEntity() {
        return (Entity) this;
    }
}
