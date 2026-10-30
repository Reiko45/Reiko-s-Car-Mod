package com.reiko.cars.weather;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Spray off the tires, on clients: water thrown up behind the wheels on a wet road and snow on a snowy one, more the
 * faster the car goes. Nothing below a crawl, or on a dry road or ice.
 */
public final class TireSpray {
    /** Below this speed (blocks a tick) there's no spray. */
    static final double FROM = 0.12;

    private TireSpray() {
    }

    /** One client tick of spray from {@code car}, going {@code speed} blocks a tick (backwards is negative). */
    public static void tick(CarEntity car, double speed) {
        Level level = car.level();
        if (!level.isClientSide || !car.onGround() || Math.abs(speed) < FROM || car.isPassenger()) {
            return;
        }
        RoadWeather.Condition road = RoadWeather.at(car);
        if (road == RoadWeather.Condition.DRY || road == RoadWeather.Condition.ICE) {
            return;
        }
        boolean snow = road == RoadWeather.Condition.SNOW;
        RandomSource random = car.getRandom();
        CarType type = car.carType();
        Vec3 forward = car.forward();
        Vec3 left = new Vec3(forward.z, 0.0, -forward.x);
        double back = Math.signum(speed);
        double pace = Math.min(1.0, Math.abs(speed) / 0.6);
        // The wheels that throw it: the back pair (going forward) or the front pair (reversing); a bike's one.
        double axle = -back * type.length * 0.32;
        double[] sides = type.isBike() ? new double[] {0.0} : new double[] {type.width * 0.38, -type.width * 0.38};
        ParticleOptions drop = snow ? ParticleTypes.SNOWFLAKE : ParticleTypes.SPLASH;
        for (double side : sides) {
            int count = (int) (pace * 7.0) + (random.nextFloat() < pace * 7.0 % 1.0 ? 1 : 0);
            Vec3 wheel = car.position().add(forward.scale(axle - back * 0.3)).add(left.scale(side));
            for (int i = 0; i < count; i++) {
                // Thrown back off the tire (a splash given no upward speed of its own flies the way it's thrown).
                double out = 0.12 + random.nextDouble() * 0.25 * (0.5 + pace);
                // (off the top of any snow on the road, or it's drawn dark, inside the snow)
                level.addParticle(drop, wheel.x + random.nextGaussian() * 0.12, car.getY() + (snow ? 0.3 : 0.1) + random.nextDouble() * 0.2,
                        wheel.z + random.nextGaussian() * 0.12, -forward.x * back * out + random.nextGaussian() * 0.05, snow ? 0.08 : 0.0,
                        -forward.z * back * out + random.nextGaussian() * 0.05);
            }
            if (pace > 0.3 && random.nextInt(snow ? 3 : 2) == 0) {
                // a fine mist (or powder) off the road at speed
                level.addParticle(ParticleTypes.CLOUD, wheel.x + random.nextGaussian() * 0.2, car.getY() + (snow ? 0.4 : 0.2), wheel.z + random.nextGaussian() * 0.2,
                        -forward.x * back * 0.04, 0.015, -forward.z * back * 0.04);
            }
        }
    }
}
