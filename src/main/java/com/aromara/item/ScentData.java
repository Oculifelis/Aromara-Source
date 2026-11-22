package com.aromara.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** What a Redolent Bundle holds ({@code aromara:scent}): a scent id from the scent datapack, fresh or dried. */
public record ScentData(Identifier scent, boolean dried) {
    public static final Codec<ScentData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("scent").forGetter(ScentData::scent),
            Codec.BOOL.optionalFieldOf("dried", false).forGetter(ScentData::dried)
    ).apply(i, ScentData::new));
    public static final StreamCodec<ByteBuf, ScentData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ScentData::scent, ByteBufCodecs.BOOL, ScentData::dried, ScentData::new);

    public ScentData asDried() {
        return new ScentData(scent, true);
    }
}
