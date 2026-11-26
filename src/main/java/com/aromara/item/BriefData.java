package com.aromara.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What a Research Brief is about ({@code aromara:brief}): the riddle id, which is also the research it unlocks, and
 * whether the Servoscrivener has finished it.
 */
public record BriefData(ResourceLocation research, boolean done) {
    public static final Codec<BriefData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("research").forGetter(BriefData::research),
            Codec.BOOL.optionalFieldOf("done", false).forGetter(BriefData::done)
    ).apply(i, BriefData::new));
    public static final StreamCodec<ByteBuf, BriefData> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, BriefData::research, ByteBufCodecs.BOOL, BriefData::done, BriefData::new);

    public BriefData asDone() {
        return new BriefData(research, true);
    }
}
