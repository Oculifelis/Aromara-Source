package com.aromara.data;

import com.aromara.Aromara;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Every number the devices and scents use. Read from {@code data/<ns>/aromara/tuning/*.json} as
 * {@code {"brazier": {"radius": 12}}}. Files merge in load order; an unknown key or an out-of-range value is skipped
 * with a log line. The defaults are the original's constants (see docs/COVERAGE.md).
 */
public final class AromaraTuning {
    public record Spec(double def, double min, double max) {
    }

    public static final Map<String, Spec> DEFAULTS = new LinkedHashMap<>();

    private static void def(String key, double def, double min, double max) {
        DEFAULTS.put(key, new Spec(def, min, max));
    }

    static {
        def("pressing_stone.press_ticks", 3600, 20, 72_000);
        def("pressing_stone.crystal_chance", 0.01, 0, 1);
        def("pressing_stone.flux_chance", 0.2, 0, 1);
        def("pressing_stone.flux_amount", 1, 0, 100);

        def("brazier.ticks_per_coal", 100, 1, 2400);
        def("brazier.max_coal", 64, 1, 64);
        def("brazier.radius", 10, 1, 32);
        def("brazier.ignite_centivis", 200, 0, 10_000);

        def("brew.burn_seconds", 60, 1, 3600);
        def("brew.extended_multiplier", 2, 1, 8);
        def("brew.effect_ticks", 160, 20, 2400);
        def("brew.refresh_ticks", 80, 10, 1200);
        def("brew.instant_every", 3, 1, 60);
        def("brew.coal_rate", 2, 1, 8);

        def("purge_taint.flux_drained", 1, 0, 100);
        def("purge_taint.burn_seconds", 10, 0, 120);
        def("purge_taint.pressure_cleaned", 0.5, 0, 100);
        def("purge_taint.goo_chance", 0.1, 0, 1);
        def("purge_taint.goo_tries", 10, 0, 64);

        def("alchemical_metabolism.food", 1, 0, 20);
        def("alchemical_metabolism.vis_drained", 10, 0, 1000);

        def("confusion.pairs", 1, 0, 16);
        def("confusion.warp_chance", 0.1, 0, 1);
        def("confusion.warp_amount", 1, 0, 100);
        // 0 permanent, 1 normal, 2 temporary
        def("confusion.warp_type", 2, 0, 2);

        def("candle_vat.stage_ticks", 2400, 20, 72_000);
        def("candle_vat.spoil_ticks", 200, 1, 72_000);
        def("candle_vat.essentia_max", 30, 1, 1000);
        def("candle_vat.essentia_interval", 160, 1, 72_000);
        def("candle_vat.suction", 128, 1, 256);

        def("kindling_retort.radius", 10, 1, 32);
        def("kindling_retort.seconds_per_piece", 2, 0, 60);
        def("mercurial_edge.food_cost", 1, 0, 20);
        def("vapor_veil.chance_per_piece", 0.05, 0, 0.25);
        def("servoscrivener.riddle_delay", 40, 1, 1200);
        def("servoscrivener.inform_radius", 5, 0, 32);
        def("servoscrivener.jar_radius", 10, 1, 32);
    }

    private static volatile Map<String, Double> values = Map.of();

    private AromaraTuning() {
    }

    public static double get(String key) {
        Double v = values.get(key);
        return v != null ? v : DEFAULTS.get(key).def();
    }

    public static int getInt(String key) {
        return (int) Math.round(get(key));
    }

    public static final class Listener extends SimpleJsonResourceReloadListener<JsonElement> {
        public Listener() {
            super(ExtraCodecs.JSON, FileToIdConverter.json("aromara/tuning"));
        }

        @Override
        protected void apply(Map<Identifier, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
            Map<String, Double> next = new LinkedHashMap<>();
            new TreeMap<>(files).forEach((file, json) -> {
                if (!json.isJsonObject()) {
                    Aromara.LOG.error("Aromara tuning {}: not an object (skipped)", file);
                    return;
                }
                for (Map.Entry<String, JsonElement> group : json.getAsJsonObject().entrySet()) {
                    if (!group.getValue().isJsonObject()) {
                        Aromara.LOG.error("Aromara tuning {}: '{}' is not an object (skipped)", file, group.getKey());
                        continue;
                    }
                    JsonObject g = group.getValue().getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : g.entrySet()) {
                        String key = group.getKey() + "." + e.getKey();
                        Spec spec = DEFAULTS.get(key);
                        if (spec == null || !e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isNumber()) {
                            Aromara.LOG.error("Aromara tuning {}: unknown or non-numeric '{}' (skipped)", file, key);
                            continue;
                        }
                        double v = e.getValue().getAsDouble();
                        if (v < spec.min() || v > spec.max()) {
                            Aromara.LOG.error("Aromara tuning {}: '{}' = {} outside {}..{} (skipped)", file, key, v, spec.min(), spec.max());
                            continue;
                        }
                        next.put(key, v);
                    }
                }
            });
            values = Map.copyOf(next);
            Aromara.LOG.info("Aromara tuning: {} files, {} values", files.size(), next.size());
        }
    }
}
