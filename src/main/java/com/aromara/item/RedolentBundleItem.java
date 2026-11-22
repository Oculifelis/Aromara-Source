package com.aromara.item;

import com.aromara.ARRegistry;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Four aromatic plants tied with string. Its {@link ScentData} says which plant and whether it has been pressed dry. */
public class RedolentBundleItem extends Item {
    public RedolentBundleItem(Properties properties) {
        super(properties);
    }

    public static MutableComponent scentName(Identifier scent) {
        return Component.translatable("scent." + scent.getNamespace() + "." + scent.getPath());
    }

    @Override
    public Component getName(ItemStack stack) {
        ScentData scent = stack.get(ARRegistry.SCENT.get());
        return scent != null && scent.dried() ? Component.translatable("item.aromara.redolent_bundle.dried") : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        ScentData scent = stack.get(ARRegistry.SCENT.get());
        if (scent != null) {
            out.accept(scentName(scent.scent()).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
