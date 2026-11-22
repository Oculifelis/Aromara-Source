package com.aromara.item;

import com.aromara.ARRegistry;
import com.aromara.research.Riddles;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The reward of a hypothesis entry. Placed in a Servoscrivener it becomes a riddle; once solved it is "done" and is
 * what the follow-up research asks for. The done flag is mirrored into custom model data so the item model can show
 * the sealed brief.
 */
public class ResearchBriefItem extends Item {
    public ResearchBriefItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Identifier research, boolean done) {
        ItemStack stack = new ItemStack(ARRegistry.RESEARCH_BRIEF.get());
        setBrief(stack, new BriefData(research, done));
        return stack;
    }

    public static void setBrief(ItemStack stack, BriefData brief) {
        stack.set(ARRegistry.BRIEF.get(), brief);
        if (brief.done()) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(true), List.of(), List.of()));
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        BriefData brief = stack.get(ARRegistry.BRIEF.get());
        if (brief == null) {
            return;
        }
        Identifier research = brief.research();
        if (brief.done()) {
            out.accept(Component.translatable(Riddles.researchTitleKey(research)).withStyle(ChatFormatting.GOLD));
            out.accept(Component.translatable("tooltip.aromara.brief.done").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        } else {
            out.accept(Component.translatable(Riddles.riddleTitleKey(research)).withStyle(ChatFormatting.GOLD));
            out.accept(Component.translatable("tooltip.aromara.brief.open").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
