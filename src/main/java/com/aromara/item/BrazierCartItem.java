package com.aromara.item;

import com.aromara.ARRegistry;
import com.aromara.entity.BrazierCart;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;

/** Places a Brazier Cart on a rail, like a minecart item (the entity type is looked up late, at use). */
public class BrazierCartItem extends Item {
    public BrazierCartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.RAILS)) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = context.getItemInHand();
        if (level instanceof ServerLevel server) {
            RailShape shape = state.getBlock() instanceof BaseRailBlock rail
                    ? rail.getRailDirection(state, level, pos, null) : RailShape.NORTH_SOUTH;
            double lift = shape.isAscending() ? 0.5 : 0.0;
            BrazierCart cart = new BrazierCart(ARRegistry.BRAZIER_CART_TYPE.get(), server);
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.0625 + lift;
            double z = pos.getZ() + 0.5;
            cart.setPos(x, y, z);
            cart.xo = x;
            cart.yo = y;
            cart.zo = z;
            EntityType.<BrazierCart>createDefaultStackConfig(server, stack, context.getPlayer()).accept(cart);
            server.addFreshEntity(cart);
            server.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(context.getPlayer(), server.getBlockState(pos.below())));
        }
        stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
