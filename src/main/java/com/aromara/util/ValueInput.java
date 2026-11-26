package com.aromara.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/** Reads named values from a CompoundTag through codecs, with registry access. */
public final class ValueInput {
    private final CompoundTag tag;
    private final DynamicOps<Tag> ops;

    public ValueInput(CompoundTag tag, HolderLookup.Provider registries) {
        this.tag = tag;
        this.ops = registries.createSerializationContext(NbtOps.INSTANCE);
    }

    public <T> Optional<T> read(String key, Codec<T> codec) {
        Tag t = tag.get(key);
        return t == null ? Optional.empty() : codec.parse(ops, t).result();
    }

    public int getIntOr(String key, int fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }

    public boolean getBooleanOr(String key, boolean fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback;
    }
}
