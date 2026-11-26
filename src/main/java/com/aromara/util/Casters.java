package com.aromara.util;

import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.casters.ICaster;
import com.leclowndu93150.thaumaturge.content.wands.ItemWand;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The TC4 "wand it" step that replaces the original's TC6 gauntlet command. Thaumaturge wands pay a primal's vis
 * from the wand itself; other casters (addons) pay through {@link ICaster#consumeVis}. Thaumaturge 1.0.2's API has
 * no per-primal spend, so wands go through {@code WandVisHelper.consumeVis} (as Fieldframe Arcana does).
 */
public final class Casters {
    private Casters() {
    }

    /** True for a Thaumaturge wand or any addon caster. */
    public static boolean isCaster(ItemStack stack) {
        return stack.getItem() instanceof ItemWand || stack.getItem() instanceof ICaster;
    }

    /** Spends {@code centivis} ignis (100 per vis); free when 0, and always free in creative. */
    public static boolean spendIgnis(ItemStack caster, Player player, int centivis) {
        if (centivis <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        if (caster.getItem() instanceof ItemWand) {
            return WandVisHelper.drainOne(caster, player, TTAspects.IGNIS, centivis, false);
        }
        if (caster.getItem() instanceof ICaster c) {
            return c.consumeVis(caster, player, centivis / 100f, false, false);
        }
        return false;
    }
}
