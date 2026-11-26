package com.aromara.compat;

import com.aromara.ARRegistry;
import com.aromara.Aromara;
import com.aromara.data.Scents;
import com.aromara.item.OilData;
import com.aromara.item.ScentData;
import com.aromara.research.PortalRituals;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCrystalAccess;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * JEI: Pressing Stone (fresh bundle and quartz to essentia crystals of the plant's aspects and a dried bundle), Candle
 * Vat (dried bundle to a phial of scented oil), Arcane Brazier (what each scent's vapour does) and Portal Ritual (the
 * offerings that make a portal drop its prize), all from the scent and ritual data {@code AromaraDataSync} sends; plus
 * information pages for the items that come from the world, a ritual or research. Arcane recipes and the imbue
 * crafting recipe are shown by Thaumaturge and JEI. Loaded only with JEI.
 */
@JeiPlugin
public final class AromaraJeiPlugin implements IModPlugin {
    record Scent(ResourceLocation id, Scents.Definition def) {
    }

    static final RecipeType<Scent> PRESSING = RecipeType.create(Aromara.MODID, "pressing", Scent.class);
    static final RecipeType<Scent> CANDLE_VAT = RecipeType.create(Aromara.MODID, "candle_vat", Scent.class);
    static final RecipeType<Scent> BRAZIER = RecipeType.create(Aromara.MODID, "brazier", Scent.class);
    static final RecipeType<PortalRituals.Ritual> PORTAL_RITUAL = RecipeType.create(Aromara.MODID, "portal_ritual", PortalRituals.Ritual.class);

    private static final String[] INFO = {"heating_element", "candle_vat", "servoscrivener", "research_brief", "vishroom_cap", "vishroom_stem"};

    @Override
    public ResourceLocation getPluginUid() {
        return Aromara.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new PressingCategory(gui), new VatCategory(gui), new BrazierCategory(gui), new RitualCategory(gui));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(PRESSING, ARRegistry.PRESSING_STONE.get());
        registration.addRecipeCatalysts(CANDLE_VAT, ARRegistry.CANDLE_VAT.get(), ARRegistry.HEATING_ELEMENT.get());
        registration.addRecipeCatalysts(BRAZIER, ARRegistry.ARCANE_BRAZIER.get());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<Scent> scents = new ArrayList<>();
        Scents.all().forEach((id, def) -> scents.add(new Scent(id, def)));
        scents.sort((a, b) -> a.id().compareTo(b.id()));
        registration.addRecipes(PRESSING, scents);
        registration.addRecipes(CANDLE_VAT, scents);
        registration.addRecipes(BRAZIER, scents);
        registration.addRecipes(PORTAL_RITUAL, PortalRituals.all());
        for (String name : INFO) {
            BuiltInRegistries.ITEM.getOptional(Aromara.id(name)).ifPresent(item ->
                    registration.addIngredientInfo(item, Component.translatable("jei.aromara.info." + name)));
        }
    }

    private static ItemStack bundle(ResourceLocation scent, boolean dried) {
        ItemStack stack = new ItemStack(ARRegistry.REDOLENT_BUNDLE.get());
        stack.set(ARRegistry.SCENT.get(), new ScentData(scent, dried));
        return stack;
    }

    private static List<ItemStack> tag(TagKey<Item> tag) {
        List<ItemStack> out = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(h -> out.add(new ItemStack(h)));
        return out;
    }

    private static Component note(String key, Object... args) {
        return Component.translatable("jei.aromara." + key, args);
    }

    private static final class PressingCategory extends AbstractRecipeCategory<Scent> {
        PressingCategory(IGuiHelper gui) {
            super(PRESSING, note("pressing"), gui.createDrawableItemLike(ARRegistry.PRESSING_STONE.get()), 150, 52);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Scent recipe, IFocusGroup focuses) {
            builder.addInputSlot(4, 4).setStandardSlotBackground().addItemStack(bundle(recipe.id(), false));
            builder.addInputSlot(4, 22).setStandardSlotBackground().addItemStacks(tag(ARRegistry.PRESSING_CATALYST));
            builder.addOutputSlot(60, 4).setOutputSlotBackground().addItemStack(bundle(recipe.id(), true));
            int x = 84;
            for (AspectInstance a : AspectIndexAccess.of(new ItemStack(recipe.def().plant())).entries()) {
                builder.addOutputSlot(x, 4).setOutputSlotBackground().addItemStack(EssentiaCrystalAccess.create(a.aspect(), 1));
                x += 18;
                if (x > 132) {
                    break;
                }
            }
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, Scent recipe, IFocusGroup focuses) {
            builder.addRecipeArrowWidget().setPosition(28, 4);
            builder.addText(note("pressing.note"), 150, 10).setPosition(0, 42).setColor(0xFF707070);
        }
    }

    private static final class VatCategory extends AbstractRecipeCategory<Scent> {
        VatCategory(IGuiHelper gui) {
            super(CANDLE_VAT, note("candle_vat"), gui.createDrawableItemLike(ARRegistry.CANDLE_VAT.get()), 150, 40);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Scent recipe, IFocusGroup focuses) {
            builder.addInputSlot(4, 4).setStandardSlotBackground().addItemStack(bundle(recipe.id(), true));
            builder.addInputSlot(24, 4).setStandardSlotBackground().addItemStack(new ItemStack(ARRegistry.SCENT_PHIAL.get()));
            ItemStack oil = new ItemStack(ARRegistry.SCENT_PHIAL.get());
            oil.set(ARRegistry.OIL.get(), new OilData(recipe.id(), false));
            builder.addOutputSlot(84, 4).setOutputSlotBackground().addItemStack(oil);
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, Scent recipe, IFocusGroup focuses) {
            builder.addRecipeArrowWidget().setPosition(50, 4);
            builder.addText(note("candle_vat.note"), 150, 10).setPosition(0, 28).setColor(0xFF707070);
        }
    }

    private static final class BrazierCategory extends AbstractRecipeCategory<Scent> {
        BrazierCategory(IGuiHelper gui) {
            super(BRAZIER, note("brazier"), gui.createDrawableItemLike(ARRegistry.ARCANE_BRAZIER.get()), 150, 56);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Scent recipe, IFocusGroup focuses) {
            builder.addInputSlot(4, 4).setStandardSlotBackground().addItemStack(bundle(recipe.id(), true));
            builder.addInputSlot(24, 4).setStandardSlotBackground().addItemStacks(tag(ItemTags.COALS));
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, Scent recipe, IFocusGroup focuses) {
            ResourceLocation effect = recipe.def().brazierEffect();
            builder.addText(Component.translatableWithFallback("jei.aromara.brazier." + effect.getNamespace() + "." + effect.getPath(),
                    effect.toString()), 150, 30).setPosition(0, 26).setColor(0xFF404040);
        }
    }

    private static final class RitualCategory extends AbstractRecipeCategory<PortalRituals.Ritual> {
        RitualCategory(IGuiHelper gui) {
            super(PORTAL_RITUAL, note("portal_ritual"), gui.createDrawableItemLike(ARRegistry.SERVOSCRIVENER.get()), 150, 40);
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, PortalRituals.Ritual recipe, IFocusGroup focuses) {
            for (int i = 0; i < recipe.offerings().size() && i < 5; i++) {
                builder.addInputSlot(4 + i * 18, 4).setStandardSlotBackground().addItemStack(new ItemStack(recipe.offerings().get(i)));
            }
            builder.addOutputSlot(128, 4).setOutputSlotBackground().addItemStack(new ItemStack(recipe.drop()));
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, PortalRituals.Ritual recipe, IFocusGroup focuses) {
            builder.addText(note("portal_ritual.note", recipe.portal().getDescription()), 150, 10).setPosition(0, 28).setColor(0xFF707070);
        }
    }
}
