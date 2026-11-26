package com.aromara.block;

import com.aromara.ARRegistry;
import com.aromara.scent.BrazierCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.aromara.util.ValueInput;
import com.aromara.util.ValueOutput;

/** The block form of the Arcane Brazier; the burning itself is {@link BrazierCore}. */
public class ArcaneBrazierBlockEntity extends BlockEntity {
    final BrazierCore core = new BrazierCore(this::changed);

    public ArcaneBrazierBlockEntity(BlockPos pos, BlockState state) {
        super(ARRegistry.ARCANE_BRAZIER_BE.get(), pos, state);
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            int status = core.status();
            BlockState state = getBlockState();
            if (state.getValue(ArcaneBrazierBlock.STATUS) != status) {
                level.setBlock(worldPosition, state.setValue(ArcaneBrazierBlock.STATUS, status), 3);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneBrazierBlockEntity be) {
        if (level instanceof ServerLevel server && be.core.tick(server, pos)) {
            be.setChanged();
        }
    }

    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            for (ItemStack stack : core.drain()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput out = new ValueOutput(tag, registries);
        core.save(out);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput in = new ValueInput(tag, registries);
        core.load(in);
    }
}
