package com.aromara.item;

import com.aromara.ARRegistry;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A wax-sealed phial. Empty, it scoops oil from a Candle Vat; filled ({@link OilData}), it imbues gear in a crafting grid. */
public class ScentPhialItem extends Item {
    public ScentPhialItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        OilData oil = stack.get(ARRegistry.OIL.get());
        if (oil == null) {
            return super.getName(stack);
        }
        return oil.rancid() ? Component.translatable("item.aromara.scent_phial.rancid")
                : Component.translatable("item.aromara.scent_phial.filled", RedolentBundleItem.scentName(oil.scent()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        OilData oil = stack.get(ARRegistry.OIL.get());
        if (oil != null) {
            out.accept(Component.translatable("tooltip.aromara.oil.effect." + oil.scent().getNamespace() + "." + oil.scent().getPath())
                    .withStyle(oil.rancid() ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY));
        }
    }
}
