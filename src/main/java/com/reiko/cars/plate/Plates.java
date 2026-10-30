package com.reiko.cars.plate;

import java.util.Locale;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * License plate numbers. Every car gets one the first time it's in the world, three letters and three digits
 * ("RKC 482": no I, O or Q, which read as 1 and 0), unique on the server; at a DMV its owner can buy a custom one
 * of 2 to 8 letters, digits and single spaces.
 */
public final class Plates {
    public static final int MAX_LENGTH = 8;
    private static final String LETTERS = "ABCDEFGHJKLMNPRSTUVWXYZ";

    private Plates() {
    }

    /** A random standard plate. */
    public static String random(RandomSource random) {
        StringBuilder plate = new StringBuilder(7);
        for (int i = 0; i < 3; i++) {
            plate.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }
        plate.append(' ');
        for (int i = 0; i < 3; i++) {
            plate.append((char) ('0' + random.nextInt(10)));
        }
        return plate.toString();
    }

    /** {@code text} as a custom plate (capitals, single spaces, trimmed), or null if it can't be one. */
    @Nullable
    public static String clean(String text) {
        String plate = text.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        if (plate.length() < 2 || plate.length() > MAX_LENGTH) {
            return null;
        }
        for (int i = 0; i < plate.length(); i++) {
            char c = plate.charAt(i);
            if (!(c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == ' ')) {
                return null;
            }
        }
        return plate;
    }
}
