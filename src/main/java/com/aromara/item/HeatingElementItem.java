package com.aromara.item;

import com.aromara.ARRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Used on a Thaumaturge crucible, it turns the crucible into a Candle Vat. */
public class HeatingElementItem extends Item {
    private static final ResourceLocation CRUCIBLE = ResourceLocation.fromNamespaceAndPath("thaumaturge", "crucible");

    public HeatingElementItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).equals(CRUCIBLE)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            // Remove the crucible's own block entity (and its contents) cleanly before the vat takes its place.
            level.removeBlock(pos, false);
            level.setBlock(pos, ARRegistry.CANDLE_VAT.get().defaultBlockState(), 3);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.4f);
            if (context.getPlayer() != null) {
                context.getItemInHand().consume(1, context.getPlayer());
            } else {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
