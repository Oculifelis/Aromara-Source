package com.aromara.scent;

import com.aromara.Aromara;
import com.aromara.data.AromaraTuning;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.entity.ThaumaturgeEntityTypeTags;
import com.leclowndu93150.thaumaturge.api.taint.TaintApi;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * What a burning scent does around an Arcane Brazier, once per coal. The scent JSON names one of these ids, so a
 * pack's own scents can reuse them. Numbers come from {@link AromaraTuning}.
 */
public final class BrazierEffects {
    @FunctionalInterface
    public interface Effect {
        void apply(ServerLevel level, BlockPos pos, AABB area);
    }

    private static final Identifier FLUX_GOO = Identifier.fromNamespaceAndPath("thaumaturge", "flux_goo");

    public static final Map<Identifier, Effect> EFFECTS = Map.of(
            Aromara.id("purge_taint"), BrazierEffects::purgeTaint,
            Aromara.id("alchemical_metabolism"), BrazierEffects::metabolism,
            Aromara.id("confusion"), BrazierEffects::confusion);

    private BrazierEffects() {
    }

    public static void apply(Identifier effect, ServerLevel level, BlockPos pos) {
        Effect e = EFFECTS.get(effect);
        if (e == null) {
            Aromara.LOG.warn("Unknown brazier effect {} at {}", effect, pos);
            return;
        }
        e.apply(level, pos, new AABB(pos).inflate(AromaraTuning.get("brazier.radius")));
    }

    /** Cinderpearl: burns flux out of the aura and sets tainted creatures alight. Rain condenses the flux into goo. */
    private static void purgeTaint(ServerLevel level, BlockPos pos, AABB area) {
        AuraHelper.drainFlux(level, pos, (float) AromaraTuning.get("purge_taint.flux_drained"), false);
        float pressure = (float) AromaraTuning.get("purge_taint.pressure_cleaned");
        if (pressure > 0) {
            TaintApi.cleanEcologicalPressure(level, pos, pressure);
        }
        float seconds = (float) AromaraTuning.get("purge_taint.burn_seconds");
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.getType().builtInRegistryHolder().is(ThaumaturgeEntityTypeTags.TAINTED))) {
            e.igniteForSeconds(seconds);
        }
        RandomSource random = level.getRandom();
        if (level.isRainingAt(pos.above()) && random.nextDouble() < AromaraTuning.get("purge_taint.goo_chance")) {
            Block goo = BuiltInRegistries.BLOCK.getValue(FLUX_GOO);
            if (goo == Blocks.AIR) {
                return;
            }
            for (int i = 0; i < AromaraTuning.getInt("purge_taint.goo_tries"); i++) {
                if (!random.nextBoolean()) {
                    continue;
                }
                int dx = (int) ((random.nextFloat() - random.nextFloat()) * 4);
                int dz = (int) ((random.nextFloat() - random.nextFloat()) * 4);
                for (BlockPos p : List.of(pos.offset(dx, 0, dz), pos.offset(dx, -1, dz))) {
                    if (level.getBlockState(p).canBeReplaced() && level.canSeeSky(p)
                            && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                        level.setBlock(p, goo.defaultBlockState(), 3);
                        break;
                    }
                }
            }
        }
    }

    /** Shimmerleaf: hungry players nearby are fed a little, and the aura pays for it in vis. */
    private static void metabolism(ServerLevel level, BlockPos pos, AABB area) {
        int food = AromaraTuning.getInt("alchemical_metabolism.food");
        float vis = (float) AromaraTuning.get("alchemical_metabolism.vis_drained");
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            if (player.getFoodData().needsFood()) {
                player.getFoodData().eat(food, 0);
                AuraHelper.drainVis(level, pos, vis, false);
            }
        }
    }

    /** Vishroom: hostile mobs lose track of who is on whose side; players nearby may see things (warp). */
    private static void confusion(ServerLevel level, BlockPos pos, AABB area) {
        List<Mob> hostile = new ArrayList<>(level.getEntitiesOfClass(Mob.class, area, m -> m instanceof Enemy && m.isAlive()));
        RandomSource random = level.getRandom();
        for (int i = 0; i < AromaraTuning.getInt("confusion.pairs") && hostile.size() >= 2; i++) {
            Mob attacker = hostile.remove(random.nextInt(hostile.size()));
            Mob target = hostile.get(random.nextInt(hostile.size()));
            attacker.setTarget(target);
        }
        WarpType type = switch (AromaraTuning.getInt("confusion.warp_type")) {
            case 0 -> WarpType.PERMANENT;
            case 1 -> WarpType.NORMAL;
            default -> WarpType.TEMPORARY;
        };
        int warp = AromaraTuning.getInt("confusion.warp_amount");
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            if (warp > 0 && random.nextDouble() < AromaraTuning.get("confusion.warp_chance")) {
                WarpHelper.addWarp(player, warp, type);
            }
        }
    }
}
