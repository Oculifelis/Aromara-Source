package com.aromara;

import com.aromara.block.ArcaneBrazierBlock;
import com.aromara.block.ArcaneBrazierBlockEntity;
import com.aromara.block.CandleVatBlock;
import com.aromara.block.CandleVatBlockEntity;
import com.aromara.block.PressingStoneBlock;
import com.aromara.block.PressingStoneBlockEntity;
import com.aromara.block.ServoscrivenerBlock;
import com.aromara.block.ServoscrivenerBlockEntity;
import com.aromara.item.BriefData;
import com.aromara.item.HeatingElementItem;
import com.aromara.item.OilData;
import com.aromara.item.RedolentBundleItem;
import com.aromara.item.ResearchBriefItem;
import com.aromara.item.ScentData;
import com.aromara.item.ScentPhialItem;
import com.aromara.recipe.ImbueRecipe;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ARRegistry {
    private static final String MODID = Aromara.MODID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    private static final List<Supplier<? extends Item>> TAB_ITEMS = new ArrayList<>();

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ScentData>> SCENT = COMPONENTS.registerComponentType("scent",
            b -> b.persistent(ScentData.CODEC).networkSynchronized(ScentData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OilData>> OIL = COMPONENTS.registerComponentType("oil",
            b -> b.persistent(OilData.CODEC).networkSynchronized(OilData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BriefData>> BRIEF = COMPONENTS.registerComponentType("brief",
            b -> b.persistent(BriefData.CODEC).networkSynchronized(BriefData.STREAM_CODEC));

    /** Which ritual offerings a portal creature has swallowed (see research/PortalRituals). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<Item>>> PORTAL_OFFERINGS = ATTACHMENTS.register("portal_offerings",
            () -> AttachmentType.<List<Item>>builder(() -> List.of()).serialize(BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("items")).build());
    /** Set on an item a player who knows a ritual's research threw, so only those count as offerings. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> SCHOLAR_TOSS = ATTACHMENTS.register("scholar_toss",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("tossed")).build());

    /** Gear that oils with an armour effect may go on, and gear for tool effects (filled from vanilla enchantable tags). */
    public static final TagKey<Item> IMBUABLE_ARMOR = TagKey.create(Registries.ITEM, Aromara.id("imbuable/armor"));
    public static final TagKey<Item> IMBUABLE_TOOL = TagKey.create(Registries.ITEM, Aromara.id("imbuable/tool"));
    /** What the Pressing Stone uses up as it pushes aspects out (the original's quartz). */
    public static final TagKey<Item> PRESSING_CATALYST = TagKey.create(Registries.ITEM, Aromara.id("pressing_catalyst"));
    /** Brazier fuel ("only coal will do"). */
    public static final TagKey<Item> BRAZIER_FUEL = TagKey.create(Registries.ITEM, Aromara.id("brazier_fuel"));
    /** What melts into fat in the Candle Vat, and what purifies it. */
    public static final TagKey<Item> VAT_FAT = TagKey.create(Registries.ITEM, Aromara.id("vat_fat"));
    public static final TagKey<Item> VAT_PURIFIER = TagKey.create(Registries.ITEM, Aromara.id("vat_purifier"));

    public static final DeferredBlock<PressingStoneBlock> PRESSING_STONE = block("pressing_stone", PressingStoneBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(2.5f).sound(SoundType.STONE).noOcclusion());
    public static final DeferredBlock<ArcaneBrazierBlock> ARCANE_BRAZIER = block("arcane_brazier", ArcaneBrazierBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(2.5f).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(s -> s.getValue(ArcaneBrazierBlock.STATUS) == ArcaneBrazierBlock.BURNING ? 13 : 0));
    public static final DeferredBlock<CandleVatBlock> CANDLE_VAT = block("candle_vat", CandleVatBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<ServoscrivenerBlock> SERVOSCRIVENER = block("servoscrivener", ServoscrivenerBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<HugeMushroomBlock> VISHROOM_CAP = block("vishroom_cap", HugeMushroomBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(0.2f).sound(SoundType.WOOD).lightLevel(s -> 4));
    public static final DeferredBlock<HugeMushroomBlock> VISHROOM_STEM = block("vishroom_stem", HugeMushroomBlock::new,
            p -> p.mapColor(MapColor.WOOL).strength(0.2f).sound(SoundType.WOOD));

    public static final DeferredItem<RedolentBundleItem> REDOLENT_BUNDLE = item("redolent_bundle", RedolentBundleItem::new, p -> p.stacksTo(1));
    public static final DeferredItem<ScentPhialItem> SCENT_PHIAL = item("scent_phial", ScentPhialItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<HeatingElementItem> HEATING_ELEMENT = item("heating_element", HeatingElementItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<Item> PARCHMENT = item("parchment", Item::new, p -> p);
    public static final DeferredItem<ResearchBriefItem> RESEARCH_BRIEF = item("research_brief", ResearchBriefItem::new, p -> p.stacksTo(1));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressingStoneBlockEntity>> PRESSING_STONE_BE = BLOCK_ENTITIES.register("pressing_stone",
            () -> new BlockEntityType<>(PressingStoneBlockEntity::new, PRESSING_STONE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArcaneBrazierBlockEntity>> ARCANE_BRAZIER_BE = BLOCK_ENTITIES.register("arcane_brazier",
            () -> new BlockEntityType<>(ArcaneBrazierBlockEntity::new, ARCANE_BRAZIER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CandleVatBlockEntity>> CANDLE_VAT_BE = BLOCK_ENTITIES.register("candle_vat",
            () -> new BlockEntityType<>(CandleVatBlockEntity::new, CANDLE_VAT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServoscrivenerBlockEntity>> SERVOSCRIVENER_BE = BLOCK_ENTITIES.register("servoscrivener",
            () -> new BlockEntityType<>(ServoscrivenerBlockEntity::new, SERVOSCRIVENER.get()));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ImbueRecipe>> IMBUE = RECIPE_SERIALIZERS.register("imbue",
            () -> ImbueRecipe.SERIALIZER);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.aromara"))
            .icon(() -> new ItemStack(ARCANE_BRAZIER.get()))
            .displayItems((params, out) -> TAB_ITEMS.forEach(i -> out.accept(i.get())))
            .build());

    private ARRegistry() {
    }

    private static <B extends Block> DeferredBlock<B> block(String name, Function<Block.Properties, B> factory,
                                                           UnaryOperator<Block.Properties> props) {
        DeferredBlock<B> block = BLOCKS.registerBlock(name, factory, props);
        TAB_ITEMS.add(ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    private static <I extends Item> DeferredItem<I> item(String name, Function<Item.Properties, I> factory, UnaryOperator<Item.Properties> props) {
        DeferredItem<I> item = ITEMS.registerItem(name, factory, props);
        TAB_ITEMS.add(item);
        return item;
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        COMPONENTS.register(bus);
        BLOCK_ENTITIES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        ATTACHMENTS.register(bus);
        TABS.register(bus);
        bus.addListener(ARRegistry::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(EssentiaCapabilities.TRANSPORT, CANDLE_VAT_BE.get(), (be, side) -> side == null || side == net.minecraft.core.Direction.DOWN ? be.sink() : null);
    }
}
