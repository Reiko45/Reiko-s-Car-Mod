package com.reiko.cars.road;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * What's painted on a block of asphalt. Each is drawn for traffic heading the block's {@code facing} way: lines run
 * along it, arrows point along it, a stop line lies across its far edge, and an edge line runs down its left side.
 *
 * <p>The road paver's lines are pieces that join up from block to block: each runs between two of the block's eight
 * <em>anchors</em> (its corners and the middles of its sides), and the next block's piece starts where it ended. Drawn
 * facing north, a straight line runs from the middle of the south side to the middle of the north; a diagonal from
 * the south-west corner to the north-east; a turn bends from the middle of the south side round to the middle of the
 * east; and a shallow line runs from the middle of the south side to the north-east corner (right) or north-west
 * corner (left). Facing east, south or west turns them a quarter, half or three quarters clockwise.
 */
public enum RoadMarking implements StringRepresentable {
    NONE("none"),
    CENTER_DASHED("center_dashed"),
    CENTER_SOLID("center_solid"),
    LANE_DASHED("lane_dashed"),
    EDGE("edge"),
    STOP_LINE("stop_line"),
    CROSSWALK("crosswalk"),
    ARROW_STRAIGHT("arrow_straight"),
    ARROW_LEFT("arrow_left"),
    ARROW_RIGHT("arrow_right"),
    // Lines across the block from corner to corner, for roads at 45 degrees: facing north the line runs south-west to
    // north-east, facing east north-west to south-east. A road at 45 degrees gets one of these in each block along the
    // line, so the line runs straight through them.
    CENTER_DASHED_DIAGONAL("center_dashed_diagonal"),
    CENTER_SOLID_DIAGONAL("center_solid_diagonal"),
    LANE_DASHED_DIAGONAL("lane_dashed_diagonal"),
    EDGE_DIAGONAL("edge_diagonal"),
    // The road paver's: a white line down the middle of the block (its edge lines), and the pieces that take lines
    // round bends. Not on the Road Paint can.
    EDGE_LINE("edge_line"),
    CENTER_DASHED_TURN("center_dashed_turn"),
    CENTER_DASHED_SHALLOW_RIGHT("center_dashed_shallow_right"),
    CENTER_DASHED_SHALLOW_LEFT("center_dashed_shallow_left"),
    CENTER_SOLID_TURN("center_solid_turn"),
    CENTER_SOLID_SHALLOW_RIGHT("center_solid_shallow_right"),
    CENTER_SOLID_SHALLOW_LEFT("center_solid_shallow_left"),
    LANE_DASHED_TURN("lane_dashed_turn"),
    LANE_DASHED_SHALLOW_RIGHT("lane_dashed_shallow_right"),
    LANE_DASHED_SHALLOW_LEFT("lane_dashed_shallow_left"),
    EDGE_TURN("edge_turn"),
    EDGE_SHALLOW_RIGHT("edge_shallow_right"),
    EDGE_SHALLOW_LEFT("edge_shallow_left");

    public static final Codec<RoadMarking> CODEC = StringRepresentable.fromEnum(RoadMarking::values);
    public static final StreamCodec<ByteBuf, RoadMarking> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);
    /** The markings a can of Road Paint offers: all but the paver's own pieces. */
    private static final int PAINTABLE = EDGE_LINE.ordinal();

    private final String name;

    RoadMarking(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** The same line from corner to corner, for a road at 45 degrees; itself if it has no diagonal. */
    public RoadMarking diagonal() {
        return switch (this) {
            case CENTER_DASHED -> CENTER_DASHED_DIAGONAL;
            case CENTER_SOLID -> CENTER_SOLID_DIAGONAL;
            case LANE_DASHED -> LANE_DASHED_DIAGONAL;
            case EDGE, EDGE_LINE -> EDGE_DIAGONAL;
            default -> this;
        };
    }

    public Component displayName() {
        return Component.translatable("road.reikocars.marking." + name);
    }

    /** The next marking a can of Road Paint offers ({@code step} 1) or the one before ({@code step} -1), skipping NONE and the paver's pieces. */
    public RoadMarking cycle(int step) {
        int n = PAINTABLE - 1;
        int at = ordinal() < PAINTABLE ? ordinal() : 1;
        return values()[1 + Math.floorMod(at - 1 + step, n)];
    }

    // ---- the paver's pieces ----

    /** The kinds of line the paver paints. */
    public enum Line {
        CENTER_DASHED,
        CENTER_SOLID,
        LANE,
        EDGE
    }

    /** The shapes a piece of line takes through a block (see the class comment). */
    public enum Shape {
        STRAIGHT,
        DIAGONAL,
        TURN,
        SHALLOW_RIGHT,
        SHALLOW_LEFT
    }

    /** The piece of {@code line} in shape {@code shape}. */
    public static RoadMarking piece(Line line, Shape shape) {
        return switch (line) {
            case CENTER_DASHED -> switch (shape) {
                case STRAIGHT -> CENTER_DASHED;
                case DIAGONAL -> CENTER_DASHED_DIAGONAL;
                case TURN -> CENTER_DASHED_TURN;
                case SHALLOW_RIGHT -> CENTER_DASHED_SHALLOW_RIGHT;
                case SHALLOW_LEFT -> CENTER_DASHED_SHALLOW_LEFT;
            };
            case CENTER_SOLID -> switch (shape) {
                case STRAIGHT -> CENTER_SOLID;
                case DIAGONAL -> CENTER_SOLID_DIAGONAL;
                case TURN -> CENTER_SOLID_TURN;
                case SHALLOW_RIGHT -> CENTER_SOLID_SHALLOW_RIGHT;
                case SHALLOW_LEFT -> CENTER_SOLID_SHALLOW_LEFT;
            };
            case LANE -> switch (shape) {
                case STRAIGHT -> LANE_DASHED;
                case DIAGONAL -> LANE_DASHED_DIAGONAL;
                case TURN -> LANE_DASHED_TURN;
                case SHALLOW_RIGHT -> LANE_DASHED_SHALLOW_RIGHT;
                case SHALLOW_LEFT -> LANE_DASHED_SHALLOW_LEFT;
            };
            case EDGE -> switch (shape) {
                case STRAIGHT -> EDGE_LINE;
                case DIAGONAL -> EDGE_DIAGONAL;
                case TURN -> EDGE_TURN;
                case SHALLOW_RIGHT -> EDGE_SHALLOW_RIGHT;
                case SHALLOW_LEFT -> EDGE_SHALLOW_LEFT;
            };
        };
    }

    /** This marking's shape, if it's one of the line pieces (the hand-painted edge line, along the block's side, isn't). */
    @Nullable
    public Shape shape() {
        return switch (this) {
            case CENTER_DASHED, CENTER_SOLID, LANE_DASHED, EDGE_LINE -> Shape.STRAIGHT;
            case CENTER_DASHED_DIAGONAL, CENTER_SOLID_DIAGONAL, LANE_DASHED_DIAGONAL, EDGE_DIAGONAL -> Shape.DIAGONAL;
            case CENTER_DASHED_TURN, CENTER_SOLID_TURN, LANE_DASHED_TURN, EDGE_TURN -> Shape.TURN;
            case CENTER_DASHED_SHALLOW_RIGHT, CENTER_SOLID_SHALLOW_RIGHT, LANE_DASHED_SHALLOW_RIGHT, EDGE_SHALLOW_RIGHT -> Shape.SHALLOW_RIGHT;
            case CENTER_DASHED_SHALLOW_LEFT, CENTER_SOLID_SHALLOW_LEFT, LANE_DASHED_SHALLOW_LEFT, EDGE_SHALLOW_LEFT -> Shape.SHALLOW_LEFT;
            default -> null;
        };
    }

    /**
     * Anchors are numbered in half blocks from the block's north-west corner: {@code {x, z}}, each 0, 1 or 2 (so the
     * middle of the north side is {@code {1, 0}}, the south-east corner {@code {2, 2}}).
     */
    private static final int[][][] CANON = {
            {{1, 2}, {1, 0}}, // straight: south middle to north middle
            {{0, 2}, {2, 0}}, // diagonal: south-west to north-east
            {{1, 2}, {2, 1}}, // turn: south middle round to east middle
            {{1, 2}, {2, 0}}, // shallow right: south middle to north-east
            {{1, 2}, {0, 0}}, // shallow left: south middle to north-west
    };

    /** An anchor turned a quarter clockwise (seen from above), {@code turns} times. */
    public static int[] rotate(int[] anchor, int turns) {
        int x = anchor[0];
        int z = anchor[1];
        for (int i = 0; i < Math.floorMod(turns, 4); i++) {
            int t = x;
            x = 2 - z;
            z = t;
        }
        return new int[] {x, z};
    }

    private static Direction facing(int turns) {
        return switch (Math.floorMod(turns, 4)) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    private static int turns(Direction facing) {
        return switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    /** The two anchors this piece joins, drawn facing {@code facing}: where it starts and where it ends; null if it isn't a piece. */
    @Nullable
    public int[][] anchors(Direction facing) {
        Shape shape = shape();
        if (shape == null) {
            return null;
        }
        int[][] canon = CANON[shape.ordinal()];
        return new int[][] {rotate(canon[0], turns(facing)), rotate(canon[1], turns(facing))};
    }

    /** A piece of {@code line} from anchor {@code from} to anchor {@code to}, and which way it faces; null if they don't make one. */
    @Nullable
    public static Piece between(Line line, int[] from, int[] to) {
        for (Shape shape : Shape.values()) {
            int[][] canon = CANON[shape.ordinal()];
            for (int turns = 0; turns < 4; turns++) {
                int[] a = rotate(canon[0], turns);
                int[] b = rotate(canon[1], turns);
                // Straight and diagonal dashes are painted in the half the line's heading into, so those go the way the
                // road does; the rest are the same either way round.
                boolean directed = shape == Shape.STRAIGHT || shape == Shape.DIAGONAL;
                if (same(a, from) && same(b, to) || !directed && same(a, to) && same(b, from)) {
                    return new Piece(piece(line, shape), facing(turns));
                }
            }
        }
        return null;
    }

    private static boolean same(int[] a, int[] b) {
        return a[0] == b[0] && a[1] == b[1];
    }

    /** A marking and which way it's drawn. */
    public record Piece(RoadMarking marking, Direction facing) {
    }
}
