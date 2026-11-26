package com.aromara.util;

import com.aromara.Aromara;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Reads every JSON file under a data folder and decodes it with a codec; files that fail are logged and skipped. */
public abstract class CodecJsonListener<T> extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private final Codec<T> codec;
    private final String directory;

    protected CodecJsonListener(Codec<T> codec, String directory) {
        super(GSON, directory);
        this.codec = codec;
        this.directory = directory;
    }

    @Override
    protected final void apply(Map<ResourceLocation, JsonElement> json, ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, T> decoded = new LinkedHashMap<>();
        json.forEach((id, element) -> codec.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(err -> Aromara.LOG.error("Aromara {} {}: {}", directory, id, err))
                .ifPresent(value -> decoded.put(id, value)));
        applyDecoded(decoded, rm, profiler);
    }

    protected abstract void applyDecoded(Map<ResourceLocation, T> files, ResourceManager rm, ProfilerFiller profiler);
}
