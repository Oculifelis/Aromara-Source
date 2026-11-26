package com.aromara.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Scented oil ({@code aromara:oil}) on a filled Scent Phial and on imbued gear. A rancid oil keeps the scent it
 * spoiled from but has no effect, as in the original.
 */
public record OilData(ResourceLocation scent, boolean rancid) {
    public static final Codec<OilData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("scent").forGetter(OilData::scent),
            Codec.BOOL.optionalFieldOf("rancid", false).forGetter(OilData::rancid)
    ).apply(i, OilData::new));
    public static final StreamCodec<ByteBuf, OilData> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, OilData::scent, ByteBufCodecs.BOOL, OilData::rancid, OilData::new);
}
