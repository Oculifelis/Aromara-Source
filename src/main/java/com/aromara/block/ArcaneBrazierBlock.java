package com.aromara.block;

import com.aromara.ARRegistry;
import com.aromara.data.AromaraTuning;
import com.aromara.util.Casters;
import com.leclowndu93150.thaumaturge.api.casters.IInteractWithCaster;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Coal and a dried bundle go in by hand; a wand lights it for a little ignis vis (the original wanted a lens). */
public class ArcaneBrazierBlock extends BaseEntityBlock implements IInteractWithCaster {
    public static final int EMPTY = 0;
    public static final int FUELLED = 1;
    public static final int BURNING = 2;
    public static final IntegerProperty STATUS = IntegerProperty.create("status", 0, 2);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 12, 15);

    public ArcaneBrazierBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STATUS, EMPTY));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ArcaneBrazierBlock::new);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneBrazierBlockEntity(pos, state);
    }

    @Override
    public <E extends BlockEntity> @Nullable BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ARRegistry.ARCANE_BRAZIER_BE.get(), ArcaneBrazierBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ArcaneBrazierBlockEntity be)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        int moved = be.insert(held);
        if (moved <= 0) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(moved);
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ArcaneBrazierBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack out = be.takeOut();
        if (out.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!player.addItem(out)) {
            player.drop(out, false);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onCasterRightClick(Level level, ItemStack caster, Player player, BlockPos pos, Direction side, InteractionHand hand) {
        if (level.isClientSide()) {
            return true;
        }
        if (!(level.getBlockEntity(pos) instanceof ArcaneBrazierBlockEntity be) || !be.canIgnite()) {
            return false;
        }
        if (!Casters.spendIgnis(caster, player, AromaraTuning.getInt("brazier.ignite_centivis"))) {
            player.sendOverlayMessage(Component.translatable("message.aromara.brazier.no_ignis"));
            return true;
        }
        be.ignite();
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(STATUS) != BURNING) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
        level.addParticle(ParticleTypes.FLAME, x, pos.getY() + 0.8, z, 0, 0.01, 0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WITCH, x, pos.getY() + 1.0, z, 0, 0.05, 0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATUS);
    }
}
