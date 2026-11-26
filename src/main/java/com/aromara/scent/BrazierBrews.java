package com.aromara.scent;

import com.aromara.Aromara;
import com.aromara.data.AromaraTuning;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Brews poured on a lit Arcane Brazier. The brew burns beside the bundle's scent for {@code brew.burn_seconds}
 * (twice that for a potion whose id starts with {@code long_}), and every {@code brew.refresh_ticks} puts its effects
 * on for {@code brew.effect_ticks} at the brew's own level. Helpful effects reach players and tamed animals, harmful
 * ones reach hostile mobs. The {@code aromara:brazier_brew} data map on potions changes a potion's burn time or
 * blocks it; the {@code #aromara:brazier_brew} item tag says which items can be poured.
 */
public final class BrazierBrews {
    public static final TagKey<Item> POURABLE = TagKey.create(Registries.ITEM, Aromara.id("brazier_brew"));

    /** Per-potion settings: {@code {"burn_seconds": 90}} or {@code {"blocked": true}}. */
    public record Settings(Optional<Integer> burnSeconds, boolean blocked) {
        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 3600).optionalFieldOf("burn_seconds").forGetter(Settings::burnSeconds),
                Codec.BOOL.optionalFieldOf("blocked", false).forGetter(Settings::blocked)
        ).apply(i, Settings::new));
    }

    public static final DataMapType<Potion, Settings> SETTINGS =
            DataMapType.builder(Aromara.id("brazier_brew"), Registries.POTION, Settings.CODEC).build();

    private BrazierBrews() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterDataMapTypesEvent e) -> e.register(SETTINGS));
    }

    /** The brew an item would pour, or null if it cannot go on a brazier. */
    public static @Nullable PotionContents pourable(ItemStack stack) {
        if (!stack.is(POURABLE)) {
            return null;
        }
        PotionContents contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (contents == null || !contents.hasEffects()) {
            return null;
        }
        Settings settings = settings(contents);
        return settings != null && settings.blocked() ? null : contents;
    }

    private static @Nullable Settings settings(PotionContents contents) {
        return contents.potion().map(p -> p.getData(SETTINGS)).orElse(null);
    }

    /** Burn time in ticks for a brew. */
    public static int burnTicks(PotionContents contents) {
        Settings settings = settings(contents);
        if (settings != null && settings.burnSeconds().isPresent()) {
            return settings.burnSeconds().get() * 20;
        }
        int ticks = AromaraTuning.getInt("brew.burn_seconds") * 20;
        boolean extended = contents.potion().flatMap(Holder::unwrapKey)
                .map(k -> k.location().getPath().startsWith("long_")).orElse(false);
        return extended ? ticks * AromaraTuning.getInt("brew.extended_multiplier") : ticks;
    }

    /** One refresh: every effect of the brew on whoever it is meant for. Instant effects land every few refreshes. */
    public static void pulse(PotionContents contents, ServerLevel level, BlockPos pos, int refreshCount) {
        AABB area = new AABB(pos).inflate(AromaraTuning.get("brazier.radius"));
        int duration = AromaraTuning.getInt("brew.effect_ticks");
        boolean instantNow = refreshCount % Math.max(1, AromaraTuning.getInt("brew.instant_every")) == 0;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            Holder<MobEffect> type = effect.getEffect();
            boolean instant = type.value().isInstantenous();
            if (instant && !instantNow) {
                continue;
            }
            boolean harmful = type.value().getCategory() == MobEffectCategory.HARMFUL;
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e.isAlive() && (harmful ? e instanceof Enemy : isAlly(e)))) {
                if (instant) {
                    type.value().applyInstantenousEffect(null, null, target, effect.getAmplifier(), 1.0);
                } else {
                    target.addEffect(new MobEffectInstance(type, duration, effect.getAmplifier(), true, true));
                }
            }
        }
    }

    private static boolean isAlly(LivingEntity e) {
        return e instanceof Player p ? !p.isSpectator() : e instanceof TamableAnimal t && t.isTame();
    }
}
