package com.aromara.research;

import com.aromara.Aromara;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

/**
 * Servoscrivener riddles: {@code data/<ns>/aromara/riddle/<research>.json}. The file id is the research the solved
 * brief unlocks. {@code hypothesis} is the entry whose completion hands out the open brief; {@code text} is the
 * riddle, with each aspect-bearing word marked by a leading {@code @}; {@code words} gives, in the same order, the
 * aspect and amount behind each marked word. The Servoscrivener picks {@code picks} of them at random.
 */
public final class Riddles {
    public record Word(ResourceKey<IAspect> aspect, int amount) {
        public static final Codec<Word> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceKey.codec(TTAspects.IGNIS.registryKey()).fieldOf("aspect").forGetter(Word::aspect),
                Codec.intRange(1, 10000).fieldOf("amount").forGetter(Word::amount)
        ).apply(i, Word::new));
    }

    public record Riddle(Identifier hypothesis, String text, List<Word> words, int picks) {
        public static final Codec<Riddle> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("hypothesis").forGetter(Riddle::hypothesis),
                Codec.STRING.fieldOf("text").forGetter(Riddle::text),
                Word.CODEC.listOf().fieldOf("words").forGetter(Riddle::words),
                Codec.intRange(1, 64).optionalFieldOf("picks", 3).forGetter(Riddle::picks)
        ).apply(i, Riddle::new));
    }

    private static volatile Map<Identifier, Riddle> riddles = Map.of();

    private Riddles() {
    }

    public static @Nullable Riddle get(Identifier research) {
        return riddles.get(research);
    }

    public static Map<Identifier, Riddle> all() {
        return riddles;
    }

    public static String researchTitleKey(Identifier research) {
        return "research." + research.getNamespace() + "." + research.getPath() + ".title";
    }

    public static String riddleTitleKey(Identifier research) {
        return "riddle." + research.getNamespace() + "." + research.getPath() + ".title";
    }

    public static final class Listener extends SimpleJsonResourceReloadListener<Riddle> {
        public Listener() {
            super(Riddle.CODEC, FileToIdConverter.json("aromara/riddle"));
        }

        @Override
        protected void apply(Map<Identifier, Riddle> files, ResourceManager rm, ProfilerFiller profiler) {
            Map<Identifier, Riddle> ok = new LinkedHashMap<>();
            files.forEach((id, r) -> {
                long marked = r.text().chars().filter(c -> c == '@').count();
                if (marked != r.words().size()) {
                    Aromara.LOG.warn("Aromara riddle {} marks {} words but lists {} aspects; skipped", id, marked, r.words().size());
                } else {
                    ok.put(id, r);
                }
            });
            riddles = Map.copyOf(ok);
            Aromara.LOG.info("Aromara riddles: {}", riddles.keySet());
        }
    }
}
