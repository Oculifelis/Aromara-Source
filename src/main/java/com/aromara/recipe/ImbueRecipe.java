package com.aromara.recipe;

import com.aromara.ARRegistry;
import com.aromara.data.Scents;
import com.aromara.item.OilData;
import com.aromara.scent.OilEffects;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A filled Scent Phial and one piece of gear in a crafting grid: the gear comes out carrying the oil
 * ({@code aromara:oil}). Armour oils only go on {@code #aromara:imbuable/armor}, tool oils on
 * {@code #aromara:imbuable/tool}; rancid oil goes on either and does nothing, as in the original.
 */
public class ImbueRecipe extends CustomRecipe {
    public static final ImbueRecipe INSTANCE = new ImbueRecipe();
    public static final MapCodec<ImbueRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ImbueRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<ImbueRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private record Pair(ItemStack gear, OilData oil) {
    }

    private static @Nullable Pair find(CraftingInput input) {
        ItemStack gear = ItemStack.EMPTY;
        OilData oil = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            OilData o = stack.is(ARRegistry.SCENT_PHIAL.get()) ? stack.get(ARRegistry.OIL.get()) : null;
            if (o != null) {
                if (oil != null) {
                    return null;
                }
                oil = o;
            } else if (stack.is(ARRegistry.IMBUABLE_ARMOR) || stack.is(ARRegistry.IMBUABLE_TOOL)) {
                if (!gear.isEmpty()) {
                    return null;
                }
                gear = stack;
            } else {
                return null;
            }
        }
        if (gear.isEmpty() || oil == null || !fits(gear, oil)) {
            return null;
        }
        return new Pair(gear, oil);
    }

    private static boolean fits(ItemStack gear, OilData oil) {
        if (oil.rancid()) {
            return true;
        }
        Scents.Definition def = Scents.get(oil.scent());
        TagKey<Item> target = def == null ? null : OilEffects.target(def.oilEffect());
        return target != null && gear.is(target);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Pair pair = find(input);
        if (pair == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = pair.gear().copyWithCount(1);
        result.set(ARRegistry.OIL.get(), pair.oil());
        return result;
    }

    @Override
    public RecipeSerializer<ImbueRecipe> getSerializer() {
        return SERIALIZER;
    }
}
