package com.aromara.block;

import com.aromara.ARRegistry;
import com.aromara.data.AromaraTuning;
import com.aromara.data.Scents;
import com.aromara.item.ScentData;
import com.aromara.scent.BrazierEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Burns coal with a dried bundle on top. Once lit by a wand it uses one coal every {@code ticks_per_coal} ticks
 * (twice a furnace's rate) and each time spreads the bundle's vapour over the area around it.
 */
public class ArcaneBrazierBlockEntity extends BlockEntity {
    private ItemStack coal = ItemStack.EMPTY;
    private ItemStack bundle = ItemStack.EMPTY;
    private @Nullable Identifier burning;
    private int pulse;

    public ArcaneBrazierBlockEntity(BlockPos pos, BlockState state) {
        super(ARRegistry.ARCANE_BRAZIER_BE.get(), pos, state);
    }

    boolean isLit() {
        return burning != null;
    }

    int insert(ItemStack held) {
        if (held.is(ARRegistry.BRAZIER_FUEL)) {
            if (!coal.isEmpty() && !ItemStack.isSameItemSameComponents(coal, held)) {
                return 0;
            }
            int room = AromaraTuning.getInt("brazier.max_coal") - coal.getCount();
            int moved = Math.max(0, Math.min(room, held.getCount()));
            if (moved > 0) {
                if (coal.isEmpty()) {
                    coal = held.copyWithCount(moved);
                } else {
                    coal.grow(moved);
                }
                changed();
            }
            return moved;
        }
        ScentData scent = held.get(ARRegistry.SCENT.get());
        if (!isLit() && bundle.isEmpty() && scent != null && scent.dried()) {
            bundle = held.copyWithCount(1);
            changed();
            return 1;
        }
        return 0;
    }

    ItemStack takeOut() {
        if (isLit()) {
            return ItemStack.EMPTY;
        }
        ItemStack out;
        if (!bundle.isEmpty()) {
            out = bundle;
            bundle = ItemStack.EMPTY;
        } else {
            out = coal;
            coal = ItemStack.EMPTY;
        }
        changed();
        return out;
    }

    boolean canIgnite() {
        ScentData scent = bundle.get(ARRegistry.SCENT.get());
        return !isLit() && !coal.isEmpty() && scent != null && scent.dried() && Scents.get(scent.scent()) != null;
    }

    void ignite() {
        ScentData scent = bundle.get(ARRegistry.SCENT.get());
        if (scent == null) {
            return;
        }
        burning = scent.scent();
        bundle = ItemStack.EMPTY;
        pulse = 0;
        changed();
    }

    void extinguish() {
        burning = null;
        pulse = 0;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            int status = isLit() ? ArcaneBrazierBlock.BURNING : !coal.isEmpty() ? ArcaneBrazierBlock.FUELLED : ArcaneBrazierBlock.EMPTY;
            BlockState state = getBlockState();
            if (state.getValue(ArcaneBrazierBlock.STATUS) != status) {
                level.setBlock(worldPosition, state.setValue(ArcaneBrazierBlock.STATUS, status), 3);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneBrazierBlockEntity be) {
        if (be.burning == null || ++be.pulse < AromaraTuning.getInt("brazier.ticks_per_coal")) {
            return;
        }
        be.pulse = 0;
        if (be.coal.isEmpty()) {
            be.extinguish();
            return;
        }
        be.coal.shrink(1);
        Scents.Definition def = Scents.get(be.burning);
        if (def != null && level instanceof ServerLevel server) {
            BrazierEffects.apply(def.brazierEffect(), server, pos);
        }
        if (be.coal.isEmpty()) {
            be.extinguish();
        } else {
            be.setChanged();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), coal);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bundle);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("Coal", ItemStack.OPTIONAL_CODEC, coal);
        out.store("Bundle", ItemStack.OPTIONAL_CODEC, bundle);
        out.storeNullable("Burning", Identifier.CODEC, burning);
        out.putInt("Pulse", pulse);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        coal = in.read("Coal", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        bundle = in.read("Bundle", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        burning = in.read("Burning", Identifier.CODEC).orElse(null);
        pulse = in.getIntOr("Pulse", 0);
    }
}
