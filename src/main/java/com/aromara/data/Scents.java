package com.aromara.data;

import com.aromara.Aromara;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * The scents a pack knows: {@code data/<ns>/aromara/scent/<name>.json} names the plant that goes into a Redolent
 * Bundle, the brazier effect its vapour has and the oil effect its Candle Vat oil gives. Effect ids are the types
 * in {@link com.aromara.scent.BrazierEffects} and {@link com.aromara.scent.OilEffects}; a pack can add scents
 * that reuse them. The scent id is the file id ({@code aromara:cinderpearl}).
 */
public final class Scents {
    public record Definition(Item plant, Identifier brazierEffect, Identifier oilEffect) {
        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("plant").forGetter(Definition::plant),
                Identifier.CODEC.fieldOf("brazier_effect").forGetter(Definition::brazierEffect),
                Identifier.CODEC.fieldOf("oil_effect").forGetter(Definition::oilEffect)
        ).apply(i, Definition::new));
    }

    private static volatile Map<Identifier, Definition> scents = Map.of();

    private Scents() {
    }

    public static @Nullable Definition get(Identifier id) {
        return scents.get(id);
    }

    public static Map<Identifier, Definition> all() {
        return scents;
    }

    /** Client side: the server's scents, from {@link AromaraDataSync}. */
    static void set(Map<Identifier, Definition> next) {
        scents = Map.copyOf(next);
    }

    public static final class Listener extends SimpleJsonResourceReloadListener<Definition> {
        public Listener() {
            super(Definition.CODEC, FileToIdConverter.json("aromara/scent"));
        }

        @Override
        protected void apply(Map<Identifier, Definition> files, ResourceManager rm, ProfilerFiller profiler) {
            scents = Map.copyOf(new LinkedHashMap<>(files));
            Aromara.LOG.info("Aromara scents: {}", scents.keySet());
        }
    }
}
