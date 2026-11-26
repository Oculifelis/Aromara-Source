package com.aromara.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** Writes named values into a CompoundTag through codecs, with registry access. */
public final class ValueOutput {
    private final CompoundTag tag;
    private final DynamicOps<Tag> ops;

    public ValueOutput(CompoundTag tag, HolderLookup.Provider registries) {
        this.tag = tag;
        this.ops = registries.createSerializationContext(NbtOps.INSTANCE);
    }

    public <T> void store(String key, Codec<T> codec, T value) {
        codec.encodeStart(ops, value).result().ifPresent(t -> tag.put(key, t));
    }

    public <T> void storeNullable(String key, Codec<T> codec, @Nullable T value) {
        if (value != null) {
            store(key, codec, value);
        }
    }

    public void putInt(String key, int value) {
        tag.putInt(key, value);
    }

    public void putBoolean(String key, boolean value) {
        tag.putBoolean(key, value);
    }
}
