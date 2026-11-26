package com.aromara.entity;

import com.aromara.ARRegistry;
import com.aromara.block.ArcaneBrazierBlock;
import com.aromara.data.AromaraTuning;
import com.aromara.scent.BrazierBrews;
import com.aromara.scent.BrazierCore;
import com.aromara.util.Casters;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameRules;
import com.aromara.util.ValueInput;
import com.aromara.util.ValueOutput;

/**
 * An Arcane Brazier riding a minecart. It works the same as the block: coal and a dried bundle by hand, a wand click
 * spends Ignis to light it, a potion poured on top burns beside the scent, and the scent or brew cloud travels with the
 * cart. Its one coal slot is open to hoppers and cart loaders, which can fill it but not empty it. Breaking it drops
 * the minecart, the brazier and whatever was inside.
 */
public class BrazierCart extends AbstractMinecart implements Container {
    private final BrazierCore core = new BrazierCore(this::changed);

    public BrazierCart(EntityType<? extends BrazierCart> type, Level level) {
        super(type, level);
    }

    private void changed() {
        if (!level().isClientSide()) {
            BlockState shown = getDefaultDisplayBlockState().setValue(ArcaneBrazierBlock.STATUS, core.status());
            if (getDisplayBlockState() != shown) {
                setDisplayBlockState(shown);
            }
        }
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ARRegistry.ARCANE_BRAZIER.get().defaultBlockState();
    }

    @Override
    protected Item getDropItem() {
        return Items.MINECART;
    }

    @Override
    public AbstractMinecart.Type getMinecartType() {
        return AbstractMinecart.Type.CHEST;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ARRegistry.BRAZIER_CART.get());
    }

    @Override
    protected void destroy(DamageSource source) {
        if (level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            spawnAtLocation(ARRegistry.ARCANE_BRAZIER.get());
            for (ItemStack stack : core.drain()) {
                if (!stack.isEmpty()) {
                    spawnAtLocation(stack);
                }
            }
        }
        destroy(getDropItem());
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            core.tick(server, blockPosition());
        } else if (getDisplayBlockState().getValue(ArcaneBrazierBlock.STATUS) == ArcaneBrazierBlock.BURNING
                && random.nextInt(2) == 0) {
            double x = getX() + (random.nextDouble() - 0.5) * 0.5;
            double z = getZ() + (random.nextDouble() - 0.5) * 0.5;
            double y = getY() + 1.2;
            level().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.01, 0);
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.WITCH, x, y + 0.2, z, 0, 0.05, 0);
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide()) {
            return held.isEmpty() || Casters.isCaster(held) || held.is(ARRegistry.BRAZIER_FUEL)
                    || held.has(ARRegistry.SCENT.get()) || held.is(BrazierBrews.POURABLE)
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (Casters.isCaster(held)) {
            if (!core.canIgnite()) {
                return InteractionResult.PASS;
            }
            if (!Casters.spendIgnis(held, player, AromaraTuning.getInt("brazier.ignite_centivis"))) {
                player.displayClientMessage(Component.translatable("message.aromara.brazier.no_ignis"), true);
                return InteractionResult.SUCCESS;
            }
            core.ignite();
            playSound(SoundEvents.FIRECHARGE_USE, 0.8f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty()) {
            ItemStack out = core.takeOut();
            if (out.isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!player.addItem(out)) {
                player.drop(out, false);
            }
            return InteractionResult.SUCCESS;
        }
        if (core.pour(held)) {
            BrazierCore.usePoured(player, hand, held);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.NEUTRAL, 0.8f, 1.2f);
            return InteractionResult.SUCCESS;
        }
        int moved = core.insert(held);
        if (moved <= 0) {
            return InteractionResult.PASS;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(moved);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.NEUTRAL, 0.8f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        core.save(new ValueOutput(tag, level().registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        core.load(new ValueInput(tag, level().registryAccess()));
        changed();
    }

    // The coal slot, for hoppers and cart loaders.

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return core.coal().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? core.coal() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot != 0 || core.coal().isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = core.coal().split(count);
        core.changed();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = core.coal();
        core.setCoal(ItemStack.EMPTY);
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            core.setCoal(stack);
        }
    }

    @Override
    public void setChanged() {
        core.changed();
    }

    @Override
    public boolean stillValid(Player player) {
        return !isRemoved() && player.canInteractWithEntity(this, 0.0);
    }

    @Override
    public void clearContent() {
        core.setCoal(ItemStack.EMPTY);
    }

    @Override
    public int getMaxStackSize() {
        return Math.max(1, Math.min(99, BrazierCore.maxCoal()));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && core.acceptsCoal(stack);
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return false;
    }
}
