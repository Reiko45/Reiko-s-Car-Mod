package com.reiko.cars.mail;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * What a parcel holds and who it's for: the addressee and sender (their ids, and names to show), the postage paid
 * (which goes to whoever delivers it), the items inside, and where the addressee's mailbox was when it was sent, for
 * the mail carrier.
 */
public record Parcel(UUID to, String toName, UUID from, String fromName, long postage, ItemContainerContents contents,
                     Optional<GlobalPos> address) {
    public static final Codec<Parcel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("to").forGetter(Parcel::to),
            Codec.STRING.fieldOf("to_name").forGetter(Parcel::toName),
            UUIDUtil.CODEC.fieldOf("from").forGetter(Parcel::from),
            Codec.STRING.fieldOf("from_name").forGetter(Parcel::fromName),
            Codec.LONG.optionalFieldOf("postage", 0L).forGetter(Parcel::postage),
            ItemContainerContents.CODEC.optionalFieldOf("contents", ItemContainerContents.EMPTY).forGetter(Parcel::contents),
            GlobalPos.CODEC.optionalFieldOf("address").forGetter(Parcel::address)
    ).apply(instance, Parcel::new));

    private static final StreamCodec<ByteBuf, Optional<GlobalPos>> ADDRESS = ByteBufCodecs.optional(GlobalPos.STREAM_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, Parcel> STREAM_CODEC = StreamCodec.of(
            (buf, parcel) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, parcel.to());
                ByteBufCodecs.STRING_UTF8.encode(buf, parcel.toName());
                UUIDUtil.STREAM_CODEC.encode(buf, parcel.from());
                ByteBufCodecs.STRING_UTF8.encode(buf, parcel.fromName());
                ByteBufCodecs.VAR_LONG.encode(buf, parcel.postage());
                ItemContainerContents.STREAM_CODEC.encode(buf, parcel.contents());
                ADDRESS.encode(buf, parcel.address());
            },
            buf -> new Parcel(UUIDUtil.STREAM_CODEC.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), UUIDUtil.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), ItemContainerContents.STREAM_CODEC.decode(buf),
                    ADDRESS.decode(buf)));

    /** How many stacks are inside. */
    public int stacks() {
        int count = 0;
        for (var ignored : contents.nonEmptyItems()) {
            count++;
        }
        return count;
    }
}
