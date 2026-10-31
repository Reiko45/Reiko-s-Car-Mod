package com.reiko.cars.road;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * What a road paver lays: the road's width in blocks, its lines, sidewalks, street lamps, and whether it shapes the
 * land and pays for what's missing. Set on the paver's screen.
 *
 * @param width     the road's width in blocks, curb to curb (not counting sidewalks)
 * @param center    the center line
 * @param lanes     lanes each way; lines between them, dashed white
 * @param edges     solid white lines down both edges
 * @param sidewalks which sides get a raised sidewalk, with a curb along the road
 * @param sidewalkWidth how wide each sidewalk is, the curb included
 * @param lampSpacing blocks between street lamps along each side, or 0 for none
 * @param lampSides where the lamps go
 * @param shape     cut through hills and fill dips to an even grade, bridging water and gaps; otherwise the road
 *                  lies on the ground as it is
 * @param pay       pay from the driver's bank account for what the hopper and the driver haven't got
 */
public record PaverSettings(int width, Center center, int lanes, boolean edges, Sides sidewalks, int sidewalkWidth, int lampSpacing,
                            LampSides lampSides, boolean shape, boolean pay) {
    public static final int MIN_WIDTH = 3;
    public static final int MAX_WIDTH = 15;
    public static final int MAX_LANES = 3;
    public static final int MAX_SIDEWALK = 4;
    /** The lamp spacings to pick from, 0 for none. */
    public static final int[] LAMP_SPACINGS = {0, 8, 12, 16, 24, 32};
    public static final PaverSettings DEFAULT = new PaverSettings(7, Center.DASHED, 1, true, Sides.BOTH, 2, 16, LampSides.BOTH, true, true);

    public static final Codec<PaverSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("width", DEFAULT.width).forGetter(PaverSettings::width),
            Codec.STRING.optionalFieldOf("center", DEFAULT.center.name()).xmap(Center::of, Center::name).forGetter(PaverSettings::center),
            Codec.INT.optionalFieldOf("lanes", DEFAULT.lanes).forGetter(PaverSettings::lanes),
            Codec.BOOL.optionalFieldOf("edges", DEFAULT.edges).forGetter(PaverSettings::edges),
            Codec.STRING.optionalFieldOf("sidewalks", DEFAULT.sidewalks.name()).xmap(Sides::of, Sides::name).forGetter(PaverSettings::sidewalks),
            Codec.INT.optionalFieldOf("sidewalk_width", DEFAULT.sidewalkWidth).forGetter(PaverSettings::sidewalkWidth),
            Codec.INT.optionalFieldOf("lamp_spacing", DEFAULT.lampSpacing).forGetter(PaverSettings::lampSpacing),
            Codec.STRING.optionalFieldOf("lamp_sides", DEFAULT.lampSides.name()).xmap(LampSides::of, LampSides::name).forGetter(PaverSettings::lampSides),
            Codec.BOOL.optionalFieldOf("shape", DEFAULT.shape).forGetter(PaverSettings::shape),
            Codec.BOOL.optionalFieldOf("pay", DEFAULT.pay).forGetter(PaverSettings::pay)
    ).apply(i, PaverSettings::new));

    public PaverSettings {
        width = Mth.clamp(width, MIN_WIDTH, MAX_WIDTH);
        lanes = Mth.clamp(lanes, 1, MAX_LANES);
        sidewalkWidth = Mth.clamp(sidewalkWidth, 1, MAX_SIDEWALK);
        if (!validSpacing(lampSpacing)) {
            lampSpacing = 16;
        }
    }

    private static boolean validSpacing(int spacing) {
        for (int s : LAMP_SPACINGS) {
            if (s == spacing) {
                return true;
            }
        }
        return false;
    }

    /** The center line. */
    public enum Center {
        NONE,
        DASHED,
        DOUBLE;

        static Center of(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return DEFAULT.center;
            }
        }

        public RoadMarking marking() {
            return this == DOUBLE ? RoadMarking.CENTER_SOLID : RoadMarking.CENTER_DASHED;
        }
    }

    /** Which sides of the road, looking the way the paver drives. */
    public enum Sides {
        NONE,
        LEFT,
        RIGHT,
        BOTH;

        static Sides of(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return DEFAULT.sidewalks;
            }
        }

        public boolean left() {
            return this == LEFT || this == BOTH;
        }

        public boolean right() {
            return this == RIGHT || this == BOTH;
        }
    }

    /** Where the street lamps stand: down one side, both, or both taking turns. */
    public enum LampSides {
        LEFT,
        RIGHT,
        BOTH,
        STAGGERED;

        static LampSides of(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return DEFAULT.lampSides;
            }
        }

        /** Whether lamp number {@code index} along the road gets one on its {@code left} side. */
        public boolean has(int index, boolean left) {
            return switch (this) {
                case LEFT -> left;
                case RIGHT -> !left;
                case BOTH -> true;
                case STAGGERED -> left == (index % 2 == 0);
            };
        }

        public boolean any(boolean left) {
            return this == BOTH || this == STAGGERED || (this == LEFT) == left;
        }
    }

    /** Lanes each way that fit: each at least two blocks wide. */
    public int lanesThatFit() {
        return Math.max(1, Math.min(lanes, (int) Math.floor(width / 2.0 / 2.0)));
    }

    public boolean lamps() {
        return lampSpacing > 0;
    }

    // ---- the screen's settings, by number (its buttons step them) ----

    public static final int WIDTH = 0;
    public static final int CENTER = 1;
    public static final int LANES = 2;
    public static final int EDGES = 3;
    public static final int SIDEWALKS = 4;
    public static final int SIDEWALK_WIDTH = 5;
    public static final int LAMP_SPACING = 6;
    public static final int LAMP_SIDES = 7;
    public static final int SHAPE = 8;
    public static final int PAY = 9;
    public static final int COUNT = 10;

    /** Setting {@code setting} as a number, for the screen. */
    public int get(int setting) {
        return switch (setting) {
            case WIDTH -> width;
            case CENTER -> center.ordinal();
            case LANES -> lanes;
            case EDGES -> edges ? 1 : 0;
            case SIDEWALKS -> sidewalks.ordinal();
            case SIDEWALK_WIDTH -> sidewalkWidth;
            case LAMP_SPACING -> lampSpacing;
            case LAMP_SIDES -> lampSides.ordinal();
            case SHAPE -> shape ? 1 : 0;
            case PAY -> pay ? 1 : 0;
            default -> 0;
        };
    }

    /** The settings with {@code setting} stepped one along ({@code up}) or back. Choices wrap round; numbers stop at their ends. */
    public PaverSettings step(int setting, boolean up) {
        int d = up ? 1 : -1;
        return switch (setting) {
            case WIDTH -> new PaverSettings(width + d, center, lanes, edges, sidewalks, sidewalkWidth, lampSpacing, lampSides, shape, pay);
            case CENTER -> new PaverSettings(width, Center.values()[Math.floorMod(center.ordinal() + d, 3)], lanes, edges, sidewalks, sidewalkWidth,
                    lampSpacing, lampSides, shape, pay);
            case LANES -> new PaverSettings(width, center, lanes + d, edges, sidewalks, sidewalkWidth, lampSpacing, lampSides, shape, pay);
            case EDGES -> new PaverSettings(width, center, lanes, !edges, sidewalks, sidewalkWidth, lampSpacing, lampSides, shape, pay);
            case SIDEWALKS -> new PaverSettings(width, center, lanes, edges, Sides.values()[Math.floorMod(sidewalks.ordinal() + d, 4)], sidewalkWidth,
                    lampSpacing, lampSides, shape, pay);
            case SIDEWALK_WIDTH -> new PaverSettings(width, center, lanes, edges, sidewalks, sidewalkWidth + d, lampSpacing, lampSides, shape, pay);
            case LAMP_SPACING -> {
                int at = 0;
                for (int i = 0; i < LAMP_SPACINGS.length; i++) {
                    if (LAMP_SPACINGS[i] == lampSpacing) {
                        at = i;
                    }
                }
                yield new PaverSettings(width, center, lanes, edges, sidewalks, sidewalkWidth, LAMP_SPACINGS[Math.floorMod(at + d, LAMP_SPACINGS.length)],
                        lampSides, shape, pay);
            }
            case LAMP_SIDES -> new PaverSettings(width, center, lanes, edges, sidewalks, sidewalkWidth, lampSpacing,
                    LampSides.values()[Math.floorMod(lampSides.ordinal() + d, 4)], shape, pay);
            case SHAPE -> new PaverSettings(width, center, lanes, edges, sidewalks, sidewalkWidth, lampSpacing, lampSides, !shape, pay);
            case PAY -> new PaverSettings(width, center, lanes, edges, sidewalks, sidewalkWidth, lampSpacing, lampSides, shape, !pay);
            default -> this;
        };
    }

    /** The settings from the screen's numbers. */
    public static PaverSettings of(int[] values) {
        return new PaverSettings(values[WIDTH], Center.values()[Mth.clamp(values[CENTER], 0, 2)], values[LANES], values[EDGES] != 0,
                Sides.values()[Mth.clamp(values[SIDEWALKS], 0, 3)], values[SIDEWALK_WIDTH], values[LAMP_SPACING],
                LampSides.values()[Mth.clamp(values[LAMP_SIDES], 0, 3)], values[SHAPE] != 0, values[PAY] != 0);
    }

    /** Setting {@code setting}'s name, for the screen. */
    public static Component label(int setting) {
        return Component.translatable("gui.reikocars.paver.setting." + key(setting));
    }

    /** Setting {@code setting}'s key in the language file. */
    public static String key(int setting) {
        return switch (setting) {
            case WIDTH -> "width";
            case CENTER -> "center";
            case LANES -> "lanes";
            case EDGES -> "edges";
            case SIDEWALKS -> "sidewalks";
            case SIDEWALK_WIDTH -> "sidewalk_width";
            case LAMP_SPACING -> "lamps";
            case LAMP_SIDES -> "lamp_sides";
            case SHAPE -> "shape";
            default -> "pay";
        };
    }

    /** Setting {@code setting}'s value, for the screen. */
    public Component valueText(int setting) {
        return switch (setting) {
            case WIDTH -> Component.translatable("gui.reikocars.paver.blocks", width);
            case CENTER -> Component.translatable("gui.reikocars.paver.center." + center.name().toLowerCase(java.util.Locale.ROOT));
            case LANES -> Component.literal(Integer.toString(lanesThatFit()));
            case EDGES, SHAPE, PAY -> Component.translatable(get(setting) != 0 ? "gui.reikocars.paver.on" : "gui.reikocars.paver.off");
            case SIDEWALKS -> Component.translatable("gui.reikocars.paver.sides." + sidewalks.name().toLowerCase(java.util.Locale.ROOT));
            case SIDEWALK_WIDTH -> Component.translatable("gui.reikocars.paver.blocks", sidewalkWidth);
            case LAMP_SPACING -> lampSpacing == 0 ? Component.translatable("gui.reikocars.paver.off")
                    : Component.translatable("gui.reikocars.paver.every", lampSpacing);
            case LAMP_SIDES -> Component.translatable("gui.reikocars.paver.lamp_sides." + lampSides.name().toLowerCase(java.util.Locale.ROOT));
            default -> Component.empty();
        };
    }
}
