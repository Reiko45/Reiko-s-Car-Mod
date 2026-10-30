package com.reiko.cars.client;

import org.jetbrains.annotations.Nullable;

/**
 * Where each vehicle's number plates are on its model: the plate panels the models were built with, in model pixels
 * (y up from the ground, z back from the middle, the front facing -z). Generated from the model scripts; a vehicle
 * missing here (the tractor, the dirt bike) has no plates to write on.
 */
final class PlateMounts {
    /** A plate: the height of its middle, the z of the face it's read from, and its width and height. */
    record Mount(float y, float z, float width, float height) {
    }

    private PlateMounts() {
    }

    /** The front and back plates of the vehicle {@code id} (either may be null), or null for none. */
    @Nullable
    static Mount[] of(String id) {
        return switch (id) {
            case "sedan" -> new Mount[] {new Mount(6F, -29.5F, 6F, 3F), new Mount(9.5F, 29.5F, 6F, 3F)};
            case "sports_car" -> new Mount[] {null, new Mount(8F, 29.5F, 6F, 3F)};
            case "hatchback" -> new Mount[] {new Mount(5.5F, -26F, 6F, 3F), new Mount(9.5F, 25.5F, 6F, 3F)};
            case "muscle_car" -> new Mount[] {null, new Mount(7.5F, 31.5F, 6F, 3F)};
            case "convertible" -> new Mount[] {null, new Mount(7F, 29.75F, 6F, 3F)};
            case "minivan" -> new Mount[] {new Mount(7.5F, -34F, 6F, 3F), new Mount(12.5F, 33.25F, 6F, 3F)};
            case "pickup_truck" -> new Mount[] {new Mount(7F, -32.75F, 6F, 3F), new Mount(7F, 32.75F, 6F, 3F)};
            case "off_roader" -> new Mount[] {new Mount(8F, -31.75F, 6F, 3F), new Mount(11F, 28.75F, 6F, 3F)};
            case "delivery_van" -> new Mount[] {new Mount(7F, -37.25F, 6F, 3F), new Mount(6.5F, 38.75F, 6F, 3F)};
            case "city_bus" -> new Mount[] {new Mount(12F, -61.75F, 6F, 3F), new Mount(27.5F, 62.75F, 6F, 3F)};
            case "heavy_pickup" -> new Mount[] {new Mount(11F, -36.75F, 6F, 3F), new Mount(15F, 34.75F, 6F, 3F)};
            case "semi_truck" -> new Mount[] {new Mount(9F, -53F, 6F, 3F), null};
            case "sport_bike" -> new Mount[] {null, new Mount(10.5F, 17F, 4F, 3F)};
            case "cruiser" -> new Mount[] {null, new Mount(8.5F, 20.25F, 4F, 3F)};
            case "scooter" -> new Mount[] {null, new Mount(7.5F, 14F, 4F, 3F)};
            case "flatbed" -> new Mount[] {new Mount(11F, -58.75F, 6F, 3F), new Mount(10F, 59.75F, 6F, 3F)};
            case "wrecker" -> new Mount[] {new Mount(11F, -58.75F, 6F, 3F), new Mount(11F, 41.75F, 6F, 3F)};
            case "mail_van" -> new Mount[] {new Mount(7F, -38.75F, 6F, 3F), new Mount(10F, 38.75F, 6F, 3F)};
            case "taxi" -> new Mount[] {new Mount(6F, -29.5F, 6F, 3F), new Mount(9.5F, 29.5F, 6F, 3F)};
            case "police_car" -> new Mount[] {new Mount(6F, -29.5F, 6F, 3F), new Mount(9.5F, 29.5F, 6F, 3F)};
            case "ambulance" -> new Mount[] {new Mount(7F, -37.25F, 6F, 3F), new Mount(6.5F, 38.75F, 6F, 3F)};
            case "garbage_truck" -> new Mount[] {new Mount(11F, -58.75F, 6F, 3F), new Mount(10F, 64.75F, 6F, 3F)};
            case "ice_cream_truck" -> new Mount[] {new Mount(7F, -38.75F, 6F, 3F), new Mount(10F, 38.75F, 6F, 3F)};
            case "dump_truck" -> new Mount[] {new Mount(11F, -58.75F, 6F, 3F), new Mount(10F, 59.25F, 6F, 3F)};
            case "cement_mixer" -> new Mount[] {new Mount(11F, -58.75F, 6F, 3F), new Mount(10F, 60.75F, 6F, 3F)};
            case "city_ev" -> new Mount[] {null, new Mount(8.5F, 22.75F, 6F, 3F)};
            case "electric_sedan" -> new Mount[] {null, new Mount(9F, 30.75F, 6F, 3F)};
            case "electric_pickup" -> new Mount[] {new Mount(11.5F, -35.75F, 6F, 3F), new Mount(11.5F, 36.75F, 6F, 3F)};
            case "electric_van" -> new Mount[] {new Mount(7F, -39.25F, 6F, 3F), new Mount(6.5F, 40.75F, 6F, 3F)};
            case "electric_moped" -> new Mount[] {null, new Mount(7.5F, 16F, 4F, 3F)};
            case "fire_truck" -> new Mount[] {new Mount(10F, -65.75F, 6F, 3F), new Mount(21F, 57.75F, 6F, 3F)};
            default -> null;
        };
    }
}
