package com.reiko.cars.garage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The garage doors a remote opens: up to {@link #MAX} of them. */
public record RemoteLinks(List<Link> links) {
    public static final int MAX = 4;

    /**
     * One door: where its anchor is, in which dimension, the code it had when it was linked (its owner resetting the
     * remotes changes it, and this stops working), and its name to show.
     */
    public record Link(String dimension, BlockPos anchor, long code, String name) {
        public static final Codec<Link> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dimension").forGetter(Link::dimension),
                BlockPos.CODEC.fieldOf("anchor").forGetter(Link::anchor),
                Codec.LONG.fieldOf("code").forGetter(Link::code),
                Codec.STRING.optionalFieldOf("name", "").forGetter(Link::name)
        ).apply(instance, Link::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Link> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Link::dimension,
                BlockPos.STREAM_CODEC, Link::anchor,
                ByteBufCodecs.VAR_LONG, Link::code,
                ByteBufCodecs.STRING_UTF8, Link::name,
                Link::new);

        public boolean matches(String dimension, BlockPos anchor) {
            return this.dimension.equals(dimension) && this.anchor.equals(anchor);
        }
    }

    public static final RemoteLinks EMPTY = new RemoteLinks(List.of());

    public static final Codec<RemoteLinks> CODEC = Link.CODEC.listOf().xmap(RemoteLinks::new, RemoteLinks::links);
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteLinks> STREAM_CODEC =
            Link.STREAM_CODEC.apply(ByteBufCodecs.list()).map(RemoteLinks::new, RemoteLinks::links);

    public RemoteLinks {
        links = List.copyOf(links);
    }

    /** These, with {@code link} added, or taken off if it was there already. */
    public RemoteLinks toggled(Link link) {
        List<Link> next = new ArrayList<>(links);
        if (next.removeIf(old -> old.matches(link.dimension(), link.anchor()))) {
            return new RemoteLinks(next);
        }
        next.add(link);
        return new RemoteLinks(next);
    }

    public boolean has(String dimension, BlockPos anchor) {
        return links.stream().anyMatch(link -> link.matches(dimension, anchor));
    }

    /** Whether it opens the door at {@code anchor} with code {@code code}. */
    public boolean opens(String dimension, BlockPos anchor, long code) {
        return links.stream().anyMatch(link -> link.matches(dimension, anchor) && link.code() == code);
    }
}
