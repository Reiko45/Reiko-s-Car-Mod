package com.reiko.cars.client;

import org.jetbrains.annotations.Nullable;

/**
 * Where a car's livery goes on its model, in model pixels (y up from the ground, z back from the middle, the front
 * facing -z): the race number's roundel on each door (the side's x, the middle's y and z, and its size) and the runs of
 * the stripes along the top of the bodywork (from z, to z, at height y, three at a time), clear of glass and racks.
 * Generated from the model scripts; motorcycles have none.
 */
final class LiveryMounts {
    record Mounts(@Nullable float[] door, float[] stripes) {
    }

    private LiveryMounts() {
    }

    @Nullable
    static Mounts of(String id) {
        return switch (id) {
            case "sedan" -> new Mounts(new float[] {13.5F, 9.5F, 0F, 6F}, new float[] {-27F, -15F, 12F, -8F, 10F, 29F, 22F, 26F, 15F});
            case "sports_car" -> new Mounts(new float[] {13F, 9F, 0.5F, 5F}, new float[] {-28F, -24F, 8F, -18F, -11F, 10F, -3F, 9F, 26F, 23F, 28F, 19F});
            case "hatchback" -> new Mounts(new float[] {13.5F, 12.5F, 0F, 4F}, new float[] {-23F, -13F, 12F, -6F, 24F, 28F});
            case "muscle_car" -> new Mounts(new float[] {14F, 9.5F, 0F, 4F}, new float[] {-29F, -26F, 12F, -25F, -13F, 14F, -13F, -10F, 12F, -4F, 10F, 26F, 23F, 27F, 13F, 27F, 30F, 14F});
            case "convertible" -> new Mounts(new float[] {13F, 9F, 0F, 5F}, new float[] {-25F, -8F, 11F, 20F, 26F, 12F});
            case "minivan" -> new Mounts(new float[] {14F, 12F, 0F, 9F}, new float[] {-29F, -26F, 15F, -16F, 32F, 33F});
            case "pickup_truck" -> new Mounts(new float[] {15.5F, 13F, -1F, 9F}, new float[] {-29F, -13F, 19F, -9F, 6F, 32F});
            case "off_roader" -> new Mounts(new float[] {14.5F, 13.5F, -0.5F, 9F}, new float[] {-26F, -13F, 20F, -7F, 0F, 33F, 1F, 8F, 33F, 9F, 16F, 33F, 17F, 24F, 33F, 25F, 28F, 33F});
            case "delivery_van" -> new Mounts(new float[] {15.5F, 16.5F, -0.5F, 9F}, new float[] {-35F, -32F, 18F, -32F, -29F, 19F, -29F, -26F, 20F, -21F, -10F, 38F, -10F, 35F, 41.5F});
            case "city_bus" -> new Mounts(new float[] {16.25F, 14F, -2F, 9F}, new float[] {-61F, -40F, 52F, -32F, -12F, 52F, 12F, 46F, 52F});
            case "heavy_pickup" -> new Mounts(new float[] {15.5F, 17F, -1F, 9F}, new float[] {-32F, -19F, 25F, -11F, 12F, 38F});
            case "farm_tractor" -> new Mounts(new float[] {7F, 18.5F, -2.5F, 6F}, new float[] {-27F, -6F, 22F, -3F, 8F, 47F, 10F, 21F, 47F});
            case "flatbed" -> new Mounts(null, new float[] {-53F, -49F, 29F, -49F, -37F, 30F, -37F, -32F, 31F, -30F, -24F, 47F, -18F, -8F, 47F});
            case "wrecker" -> new Mounts(new float[] {18F, 21F, -9F, 9F}, new float[] {-53F, -49F, 29F, -49F, -37F, 30F, -37F, -32F, 31F, -30F, -24F, 47F, -18F, -8F, 47F});
            case "mail_van" -> new Mounts(new float[] {15.25F, 16.5F, 0F, 9F}, new float[] {-24F, -20F, 47F, -18F, 8F, 47F, 16F, 36F, 47F});
            case "taxi" -> new Mounts(new float[] {13.5F, 9.5F, 0F, 6F}, new float[] {-27F, -15F, 12F, -8F, 1F, 29F, 5F, 15F, 29F, 22F, 26F, 15F});
            case "police_car" -> new Mounts(new float[] {13.5F, 9.5F, 0F, 6F}, new float[] {-27F, -15F, 12F, -8F, -1F, 29F, 4F, 15F, 29F, 22F, 26F, 15F});
            case "ambulance" -> new Mounts(new float[] {15.5F, 16.5F, -0.5F, 9F}, new float[] {-35F, -32F, 18F, -32F, -29F, 19F, -29F, -26F, 20F, -16F, -10F, 38F, -10F, 35F, 41.5F});
            case "garbage_truck" -> new Mounts(new float[] {17.5F, 20F, -4F, 9F}, new float[] {-53F, -49F, 29F, -49F, -37F, 30F, -37F, -32F, 31F, -30F, -24F, 47F, -18F, -8F, 47F, -6F, 50F, 48F});
            case "ice_cream_truck" -> new Mounts(new float[] {15.25F, 16.5F, 0F, 9F}, new float[] {-19F, 6F, 47F, 18F, 36F, 47F});
            case "dump_truck" -> new Mounts(null, new float[] {-53F, -49F, 29F, -49F, -37F, 30F, -37F, -32F, 31F, -30F, -24F, 47F, -18F, -8F, 47F});
            case "cement_mixer" -> new Mounts(null, new float[] {-53F, -49F, 29F, -49F, -37F, 30F, -37F, -32F, 31F, -30F, -24F, 47F, -18F, -8F, 47F});
            case "city_ev" -> new Mounts(new float[] {12.5F, 9.5F, 0F, 6F}, new float[] {-19F, -16F, 12F});
            case "electric_sedan" -> new Mounts(new float[] {13.25F, 9F, 0F, 5F}, new float[] {-27F, -16F, 12F, 25F, 29F, 13F});
            case "electric_pickup" -> new Mounts(new float[] {15.25F, 14.5F, -0.5F, 9F}, new float[] {-33F, -15F, 20F});
            case "electric_van" -> new Mounts(new float[] {15.5F, 16.5F, -0.5F, 9F}, new float[] {-36F, -31F, 18F, -20F, 37F, 42F});
            case "fire_truck" -> new Mounts(new float[] {18.25F, 19F, 0.5F, 9F}, new float[] {-60F, -57F, 54F, -50F, -16F, 54F, -16F, -3F, 44F});
            default -> null;
        };
    }
}
