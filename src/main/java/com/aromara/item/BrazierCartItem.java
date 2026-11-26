package com.aromara.item;

import com.aromara.ARRegistry;
import com.aromara.entity.BrazierCart;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
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
        RailShape shape = state.getBlock() instanceof BaseRailBlock rail
                ? rail.getRailDirection(state, level, pos, null) : RailShape.NORTH_SOUTH;
        double lift = shape.isSlope() ? 0.5 : 0.0;
        BrazierCart cart = AbstractMinecart.createMinecart(level, pos.getX() + 0.5, pos.getY() + 0.0625 + lift,
                pos.getZ() + 0.5, ARRegistry.BRAZIER_CART_TYPE.get(), EntitySpawnReason.DISPENSER,
                context.getItemInHand(), context.getPlayer());
        if (cart == null) {
            return InteractionResult.FAIL;
        }
        if (AbstractMinecart.useExperimentalMovement(level)) {
            for (var other : level.getEntities(null, cart.getBoundingBox())) {
                if (other instanceof AbstractMinecart) {
                    return InteractionResult.FAIL;
                }
            }
        }
        if (level instanceof ServerLevel server) {
            server.addFreshEntity(cart);
            server.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(context.getPlayer(), server.getBlockState(pos.below())));
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
