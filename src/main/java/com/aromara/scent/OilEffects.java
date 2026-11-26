package com.aromara.scent;

import com.aromara.ARRegistry;
import com.aromara.Aromara;
import com.aromara.data.Scents;
import com.aromara.item.OilData;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** The oil effect types a scent can name, and which gear tag each one goes on. Behaviour is in {@link OilEvents}. */
public final class OilEffects {
    public static final ResourceLocation KINDLING_RETORT = Aromara.id("kindling_retort");
    public static final ResourceLocation MERCURIAL_EDGE = Aromara.id("mercurial_edge");
    public static final ResourceLocation VAPOR_VEIL = Aromara.id("vapor_veil");

    private static final Map<ResourceLocation, TagKey<Item>> TARGETS = Map.of(
            KINDLING_RETORT, ARRegistry.IMBUABLE_ARMOR,
            MERCURIAL_EDGE, ARRegistry.IMBUABLE_TOOL,
            VAPOR_VEIL, ARRegistry.IMBUABLE_ARMOR);

    private OilEffects() {
    }

    public static @Nullable TagKey<Item> target(ResourceLocation effect) {
        return TARGETS.get(effect);
    }

    /** The oil effect on a stack, or null for none, rancid oil or an unknown scent. */
    public static @Nullable ResourceLocation effectOf(ItemStack stack) {
        OilData oil = stack.get(ARRegistry.OIL.get());
        if (oil == null || oil.rancid()) {
            return null;
        }
        Scents.Definition def = Scents.get(oil.scent());
        return def == null ? null : def.oilEffect();
    }

    /** How many worn armour pieces carry {@code effect}. */
    public static int armourPieces(LivingEntity entity, ResourceLocation effect) {
        int count = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && effect.equals(effectOf(entity.getItemBySlot(slot)))) {
                count++;
            }
        }
        return count;
    }
}
