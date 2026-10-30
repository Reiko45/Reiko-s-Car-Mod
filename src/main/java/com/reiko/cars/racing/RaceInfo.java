package com.reiko.cars.racing;

/**
 * Where a car is in its race or time trial, packed into an int for its synced data: the lap it's on and how many,
 * the next checkpoint and how many, and its place and how many are racing (0 places on a time trial).
 */
public record RaceInfo(int lap, int laps, int checkpoint, int checkpoints, int place, int racers) {
    public int pack() {
        return clamp(lap, 15) | clamp(laps, 15) << 4 | clamp(checkpoint, 63) << 8 | clamp(checkpoints, 63) << 14 | clamp(place, 15) << 20
                | clamp(racers, 15) << 24;
    }

    public static RaceInfo unpack(int packed) {
        return new RaceInfo(packed & 15, packed >> 4 & 15, packed >> 8 & 63, packed >> 14 & 63, packed >> 20 & 15, packed >> 24 & 15);
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }
}
