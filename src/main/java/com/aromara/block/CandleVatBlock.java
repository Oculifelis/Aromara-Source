package com.aromara.block;

import com.aromara.ARRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Items go in by hand or by dropping them in; an empty Scent Phial scoops the oil. Fed ignis from underneath. */
public class CandleVatBlock extends BaseEntityBlock {
    public static final int EMPTY = 0;
    public static final int FLESH = 1;
    public static final int IMPURE = 2;
    public static final int LIQUID = 3;
    public static final int RANCID = 4;
    public static final int IMBUED = 5;
    public static final IntegerProperty STATUS = IntegerProperty.create("status", 0, 5);

    public CandleVatBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STATUS, EMPTY));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(CandleVatBlock::new);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CandleVatBlockEntity(pos, state);
    }

    @Override
    public <E extends BlockEntity> @Nullable BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ARRegistry.CANDLE_VAT_BE.get(), CandleVatBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CandleVatBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        boolean emptyPhial = held.is(ARRegistry.SCENT_PHIAL.get()) && !held.has(ARRegistry.OIL.get());
        if (level.isClientSide()) {
            return emptyPhial || held.is(ARRegistry.VAT_FAT) || held.is(ARRegistry.VAT_PURIFIER) || held.has(ARRegistry.SCENT.get())
                    ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (emptyPhial) {
            ItemStack filled = be.scoop();
            if (filled.isEmpty()) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            held.consume(1, player);
            if (!player.addItem(filled)) {
                player.drop(filled, false);
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8f, 1.0f);
            return ItemInteractionResult.SUCCESS;
        }
        if (be.mixIn(held)) {
            held.consume(1, player);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int status = state.getValue(STATUS);
        if (status == EMPTY || random.nextInt(3) != 0) {
            return;
        }
        double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
        double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
        level.addParticle(status == RANCID ? ParticleTypes.SMOKE : status == IMBUED ? ParticleTypes.WITCH : ParticleTypes.BUBBLE_POP,
                x, pos.getY() + 0.9, z, 0, 0.02, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATUS);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CandleVatBlockEntity be) {
            be.preRemoveSideEffects(pos, state);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
