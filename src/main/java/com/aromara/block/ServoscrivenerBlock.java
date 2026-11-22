package com.aromara.block;

import com.aromara.ARRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The research table the void gave back. An open Research Brief goes in by hand; an empty hand repeats the riddle;
 * sneaking with an empty hand takes the brief back out.
 */
public class ServoscrivenerBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_PAPER = BooleanProperty.create("has_paper");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 10, 0, 16, 13, 16), Block.box(2, 0, 2, 14, 10, 14));

    public ServoscrivenerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HAS_PAPER, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ServoscrivenerBlock::new);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ServoscrivenerBlockEntity(pos, state);
    }

    @Override
    public <E extends BlockEntity> @Nullable BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ARRegistry.SERVOSCRIVENER_BE.get(), ServoscrivenerBlockEntity::serverTick);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!held.is(ARRegistry.RESEARCH_BRIEF.get()) || !(level.getBlockEntity(pos) instanceof ServoscrivenerBlockEntity be)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (be.insert(held, player)) {
            held.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ServoscrivenerBlockEntity be) || !be.hasBrief()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            ItemStack out = be.takeOut();
            if (!player.addItem(out)) {
                player.drop(out, false);
            }
        } else {
            be.inform(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(HAS_PAPER)) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8, pos.getY() + 1.1,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8, 0, 0.15 + random.nextDouble() * 0.2, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 0, 0.02, 0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_PAPER);
    }
}
