package com.reiko.cars.weather;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;

/**
 * Weather on a car, on the server: snow settling on it while it stands in falling snow (and blowing off as it's
 * driven, melting in the rain or out of the cold), and lightning striking it in a thunderstorm. A strike scorches the
 * body ({@code weather.lightningBodyDamage}); the people inside are safe, as in a real car.
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class CarWeather {
    /** Snow settling a second in falling snow: a car's covered in a minute and a half. */
    static final float SNOWFALL = 0.012F;
    /** Snow blown off a block driven. */
    static final float BLOWN_OFF = 0.05F;
    /** Snow melting a second: in the rain, where it's too warm to snow, and slowly anywhere else it isn't falling. */
    static final float RAIN_MELT = 0.03F;
    static final float WARM_MELT = 0.01F;
    static final float COLD_MELT = 0.001F;

    private CarWeather() {
    }

    /** One server tick of the weather on a car that moved {@code moved} blocks this tick. */
    public static void tick(CarEntity car, double moved) {
        Level level = car.level();
        float snow = car.snow();
        if (snow > 0.0F && moved > 0.05) {
            snow = Math.max(0.0F, snow - (float) moved * BLOWN_OFF);
        }
        if (car.tickCount % 20 == 0) {
            BlockPos top = BlockPos.containing(car.getX(), car.getY() + car.carType().height, car.getZ());
            if (CarsConfig.SNOW_ON_CARS.get() && RoadWeather.snowingAt(level, top)) {
                snow = Math.min(1.0F, snow + SNOWFALL);
            } else if (snow > 0.0F) {
                boolean rain = level.isRainingAt(top) || level.isRainingAt(top.above());
                boolean cold = level.getBiome(top).value().getPrecipitationAt(top) == Biome.Precipitation.SNOW;
                snow = Math.max(0.0F, snow - (rain ? RAIN_MELT : cold ? COLD_MELT : WARM_MELT));
            }
            double perHour = CarsConfig.get(CarsConfig.LIGHTNING_PER_HOUR);
            if (perHour > 0.0 && level.isThundering() && (level.isRainingAt(top) || level.isRainingAt(top.above()))
                    && car.getRandom().nextDouble() < perHour / 3600.0) {
                strike(car);
            }
        }
        if (snow != car.snow()) {
            car.setSnow(snow);
        }
    }

    /** Lightning strikes {@code car}: a bolt that's all show, and a scorch on its body. */
    public static void strike(CarEntity car) {
        if (!(car.level() instanceof ServerLevel level)) {
            return;
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(car.getX(), car.getY(), car.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        struck(car);
    }

    /** {@code car} was struck: its body's scorched, and whoever's inside is told they're all right. */
    public static void struck(CarEntity car) {
        if (car.level().isClientSide) {
            return;
        }
        long now = car.level().getGameTime();
        if (now - car.lastStruck() < 20) {
            return; // the same bolt, again
        }
        car.setLastStruck(now);
        CarCondition condition = car.condition();
        float damage = (float) CarsConfig.get(CarsConfig.LIGHTNING_DAMAGE);
        car.setCondition(condition.with(CarCondition.Part.BODY, Math.max(0.0F, condition.body() - damage)));
        for (Entity rider : car.getPassengers()) {
            if (rider instanceof Player player) {
                player.displayClientMessage(Component.translatable("weather.reikocars.struck"), true);
            }
        }
    }

    /** Lightning doesn't hurt anyone sitting in a car (and a car it hits is scorched, not set alight). */
    @SubscribeEvent
    static void onStruck(EntityStruckByLightningEvent event) {
        if (event.getEntity().getVehicle() instanceof CarEntity car) {
            event.setCanceled(true);
            struck(car);
        }
    }
}
