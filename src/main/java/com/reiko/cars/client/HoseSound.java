package com.reiko.cars.client;

import com.reiko.cars.fire.FireHoseItem;
import com.reiko.cars.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** The fire hose's rush of water, looping and following whoever's spraying it while they do. */
public final class HoseSound extends AbstractTickableSoundInstance {
    private final Player player;

    public HoseSound(Player player) {
        super(ModSounds.HOSE_SPRAY.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.9F;
        this.x = player.getX();
        this.y = player.getY() + 1.0;
        this.z = player.getZ();
    }

    /** Whether {@code player} is spraying a fire hose or a Wash Bay's lance. */
    public static boolean spraying(Player player) {
        return player.isAlive() && player.isUsingItem()
                && (player.getUseItem().getItem() instanceof FireHoseItem || player.getUseItem().getItem() instanceof com.reiko.cars.wash.PressureWasherItem);
    }

    @Override
    public void tick() {
        if (player.isRemoved() || !spraying(player)) {
            stop();
            return;
        }
        x = player.getX();
        y = player.getY() + 1.0;
        z = player.getZ();
    }
}
