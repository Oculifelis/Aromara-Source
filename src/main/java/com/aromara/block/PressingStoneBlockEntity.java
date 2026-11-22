package com.aromara.block;

import com.aromara.ARRegistry;
import com.aromara.data.AromaraTuning;
import com.aromara.data.Scents;
import com.aromara.item.ScentData;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.aura.AuraHelper;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCrystalAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Dries a Redolent Bundle. While pressed (one wand click), each tick has a small chance to push one point of the
 * plant's Thaumaturge aspects out of the top as an essentia crystal, using up one quartz nugget; some of those
 * leave flux in the aura. When the time is up the stone lifts and the bundle is dried.
 */
public class PressingStoneBlockEntity extends BlockEntity {
    private ItemStack catalyst = ItemStack.EMPTY;
    private ItemStack bundle = ItemStack.EMPTY;
    private int pressLeft;
    /** Aspect points of the plant still to push out; one entry per point. */
    private final List<AspectInstance> toPull = new ArrayList<>();

    public PressingStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ARRegistry.PRESSING_STONE_BE.get(), pos, state);
    }

    boolean isPressing() {
        return pressLeft > 0;
    }

    /** Puts quartz or a fresh bundle in; returns how many items were taken from {@code held}. */
    int insert(ItemStack held) {
        if (isPressing()) {
            return 0;
        }
        if (held.is(ARRegistry.PRESSING_CATALYST)) {
            if (!catalyst.isEmpty() && !ItemStack.isSameItemSameComponents(catalyst, held)) {
                return 0;
            }
            int room = held.getMaxStackSize() - catalyst.getCount();
            int moved = Math.min(room, held.getCount());
            if (moved > 0) {
                if (catalyst.isEmpty()) {
                    catalyst = held.copyWithCount(moved);
                } else {
                    catalyst.grow(moved);
                }
                setChanged();
            }
            return moved;
        }
        ScentData scent = held.get(ARRegistry.SCENT.get());
        if (bundle.isEmpty() && scent != null && !scent.dried()) {
            bundle = held.copyWithCount(1);
            setChanged();
            return 1;
        }
        return 0;
    }

    /** Hands back the bundle first, then the quartz. */
    ItemStack takeOut() {
        if (isPressing()) {
            return ItemStack.EMPTY;
        }
        ItemStack out;
        if (!bundle.isEmpty()) {
            out = bundle;
            bundle = ItemStack.EMPTY;
        } else {
            out = catalyst;
            catalyst = ItemStack.EMPTY;
        }
        setChanged();
        return out;
    }

    /** The wand click: starts pressing a fresh bundle when there is quartz to work with. */
    boolean press() {
        ScentData scent = bundle.get(ARRegistry.SCENT.get());
        if (isPressing() || catalyst.isEmpty() || scent == null || scent.dried()) {
            return false;
        }
        Scents.Definition def = Scents.get(scent.scent());
        toPull.clear();
        if (def != null) {
            for (AspectInstance a : AspectIndexAccess.of(new ItemStack(def.plant())).entries()) {
                for (int i = 0; i < a.amount(); i++) {
                    toPull.add(a.withAmount(1));
                }
            }
        }
        pressLeft = AromaraTuning.getInt("pressing_stone.press_ticks");
        setDown(true);
        setChanged();
        return true;
    }

    private void setDown(boolean down) {
        if (level != null && getBlockState().getValue(PressingStoneBlock.DOWN) != down) {
            level.setBlock(worldPosition, getBlockState().setValue(PressingStoneBlock.DOWN, down), 3);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PressingStoneBlockEntity be) {
        if (be.pressLeft <= 0) {
            if (state.getValue(PressingStoneBlock.DOWN)) {
                be.setDown(false);
            }
            return;
        }
        be.pressLeft--;
        if (!be.toPull.isEmpty() && !be.catalyst.isEmpty()
                && level.getRandom().nextDouble() < AromaraTuning.get("pressing_stone.crystal_chance")) {
            AspectInstance a = be.toPull.remove(level.getRandom().nextInt(be.toPull.size()));
            ItemStack crystal = EssentiaCrystalAccess.create(a.aspect(), 1);
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, crystal);
            drop.setDeltaMovement(0, 0.2, 0);
            level.addFreshEntity(drop);
            be.catalyst.shrink(1);
            if (level.getRandom().nextDouble() < AromaraTuning.get("pressing_stone.flux_chance")) {
                AuraHelper.polluteAura(level, pos, (float) AromaraTuning.get("pressing_stone.flux_amount"), true);
            }
        }
        if (be.pressLeft == 0) {
            ScentData scent = be.bundle.get(ARRegistry.SCENT.get());
            if (scent != null) {
                be.bundle.set(ARRegistry.SCENT.get(), scent.asDried());
            }
            be.toPull.clear();
            be.setDown(false);
        }
        be.setChanged();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), catalyst);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bundle);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("Catalyst", ItemStack.OPTIONAL_CODEC, catalyst);
        out.store("Bundle", ItemStack.OPTIONAL_CODEC, bundle);
        out.putInt("PressLeft", pressLeft);
        out.store("ToPull", AspectInstance.CODEC.listOf(), List.copyOf(toPull));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        catalyst = in.read("Catalyst", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        bundle = in.read("Bundle", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        pressLeft = in.getIntOr("PressLeft", 0);
        toPull.clear();
        in.read("ToPull", AspectInstance.CODEC.listOf()).ifPresent(toPull::addAll);
    }
}
