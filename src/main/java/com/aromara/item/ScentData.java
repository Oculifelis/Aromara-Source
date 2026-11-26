package com.aromara.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** What a Redolent Bundle holds ({@code aromara:scent}): a scent id from the scent datapack, fresh or dried. */
public record ScentData(ResourceLocation scent, boolean dried) {
    public static final Codec<ScentData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("scent").forGetter(ScentData::scent),
            Codec.BOOL.optionalFieldOf("dried", false).forGetter(ScentData::dried)
    ).apply(i, ScentData::new));
    public static final StreamCodec<ByteBuf, ScentData> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ScentData::scent, ByteBufCodecs.BOOL, ScentData::dried, ScentData::new);

    public ScentData asDried() {
        return new ScentData(scent, true);
    }
}
