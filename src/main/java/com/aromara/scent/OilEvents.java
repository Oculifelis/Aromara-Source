package com.aromara.scent;

import com.aromara.ARRegistry;
import com.aromara.data.AromaraTuning;
import com.aromara.item.OilData;
import com.aromara.item.RedolentBundleItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;

/** What oiled gear does: Cinderpearl and Vishroom on armour, Shimmerleaf on tools. */
public final class OilEvents {
    private OilEvents() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(OilEvents::onIncomingDamage);
        bus.addListener(OilEvents::onToolBreak);
        bus.addListener(OilEvents::onTooltip);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        // Vishroom: the attacker's aim wanders; each oiled piece is a chance to miss entirely.
        if (event.getSource().getEntity() instanceof LivingEntity) {
            int veil = OilEffects.armourPieces(player, OilEffects.VAPOR_VEIL);
            if (veil > 0 && player.getRandom().nextDouble() < AromaraTuning.get("vapor_veil.chance_per_piece") * veil) {
                event.setCanceled(true);
                return;
            }
        }
        // Cinderpearl: catching fire lets the armour's pent-up anger out on every hostile nearby.
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            int pieces = OilEffects.armourPieces(player, OilEffects.KINDLING_RETORT);
            if (pieces > 0) {
                float seconds = (float) (AromaraTuning.get("kindling_retort.seconds_per_piece") * pieces);
                double r = AromaraTuning.get("kindling_retort.radius");
                for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(r), m -> m instanceof Enemy)) {
                    mob.igniteForSeconds(seconds);
                }
            }
        }
    }

    /** Shimmerleaf: a tool about to break keeps its last point of durability and feeds on its wielder instead. */
    private static void onToolBreak(PlayerDestroyItemEvent event) {
        Player player = event.getEntity();
        ItemStack broken = event.getOriginal();
        InteractionHand hand = event.getHand();
        if (hand == null || player.level().isClientSide() || broken.isEmpty() || !broken.is(ARRegistry.IMBUABLE_TOOL)
                || !OilEffects.MERCURIAL_EDGE.equals(OilEffects.effectOf(broken))) {
            return;
        }
        int cost = AromaraTuning.getInt("mercurial_edge.food_cost");
        if (player.getFoodData().getFoodLevel() < Math.max(1, cost)) {
            return;
        }
        player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - cost);
        ItemStack restored = broken.copyWithCount(1);
        restored.setDamageValue(restored.getMaxDamage() - 1);
        player.setItemInHand(hand, restored);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        OilData oil = stack.get(ARRegistry.OIL.get());
        if (oil == null || stack.is(ARRegistry.SCENT_PHIAL.get())) {
            return;
        }
        event.getToolTip().add(oil.rancid()
                ? Component.translatable("tooltip.aromara.oil.rancid").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("tooltip.aromara.oil.base", RedolentBundleItem.scentName(oil.scent())).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
