package com.aromara.block;

import com.aromara.ARRegistry;
import com.aromara.Aromara;
import com.aromara.data.AromaraTuning;
import com.aromara.item.OilData;
import com.aromara.item.ScentData;
import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.essentia.EssentiaCapabilities;
import com.leclowndu93150.thaumaturge.api.essentia.IEssentiaTransport;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * A crucible fitted with a Heating Element. It burns ignis essentia drawn up a tube from below. Flesh melts into
 * impure fat, salis mundus cleans it, a dried bundle scents it, and the oil is scooped with a Scent Phial. Each
 * stage takes {@code stage_ticks} ticks; fat left alone {@code spoil_ticks} past its stage turns rancid.
 */
public class CandleVatBlockEntity extends BlockEntity {
    private static final Identifier RANCID = Aromara.id("rancid");

    private int ignis;
    private int timer;
    private ItemStack bundle = ItemStack.EMPTY;
    private @Nullable Identifier oil;
    private final Sink sink = new Sink();

    public CandleVatBlockEntity(BlockPos pos, BlockState state) {
        super(ARRegistry.CANDLE_VAT_BE.get(), pos, state);
    }

    public IEssentiaTransport sink() {
        return sink;
    }

    private int status() {
        return getBlockState().getValue(CandleVatBlock.STATUS);
    }

    private void setStatus(int status) {
        if (level != null && status() != status) {
            level.setBlock(worldPosition, getBlockState().setValue(CandleVatBlock.STATUS, status), 3);
        }
    }

    private @Nullable Holder<IAspect> ignisHolder() {
        return level == null ? null : Aspects.resolve(level, TTAspects.IGNIS);
    }

    /** Tries one item into the vat; returns true when it was used. */
    boolean mixIn(ItemStack stack) {
        int status = status();
        int stage = AromaraTuning.getInt("candle_vat.stage_ticks");
        if (status == CandleVatBlock.EMPTY && stack.is(ARRegistry.VAT_FAT)) {
            setStatus(CandleVatBlock.FLESH);
            timer = stage;
        } else if (status == CandleVatBlock.IMPURE && stack.is(ARRegistry.VAT_PURIFIER)) {
            setStatus(CandleVatBlock.LIQUID);
            timer = stage;
        } else if (status == CandleVatBlock.LIQUID && bundle.isEmpty() && isDriedBundle(stack)) {
            bundle = stack.copyWithCount(1);
            timer = stage;
        } else {
            return false;
        }
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.4f, 1.4f);
        }
        setChanged();
        return true;
    }

    private static boolean isDriedBundle(ItemStack stack) {
        ScentData scent = stack.get(ARRegistry.SCENT.get());
        return scent != null && scent.dried();
    }

    /** Fills one empty phial from a finished or rancid vat; returns the filled phial, or empty if there is no oil. */
    ItemStack scoop() {
        int status = status();
        if (status != CandleVatBlock.IMBUED && status != CandleVatBlock.RANCID) {
            return ItemStack.EMPTY;
        }
        ItemStack phial = new ItemStack(ARRegistry.SCENT_PHIAL.get());
        Identifier scent = oil != null ? oil : RANCID;
        phial.set(ARRegistry.OIL.get(), new OilData(scent, status == CandleVatBlock.RANCID));
        oil = null;
        bundle = ItemStack.EMPTY;
        timer = 0;
        setStatus(CandleVatBlock.EMPTY);
        setChanged();
        return phial;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CandleVatBlockEntity be) {
        be.drawIgnis(level, pos);
        if (level.getGameTime() % 10 == 0) {
            be.catchThrownItems(level, pos);
        }
        int status = be.status();
        if (be.ignis <= 0 || status == CandleVatBlock.EMPTY || status == CandleVatBlock.RANCID) {
            return;
        }
        if (level.getGameTime() % AromaraTuning.getInt("candle_vat.essentia_interval") == 0) {
            be.ignis--;
        }
        be.timer--;
        if (be.timer == 0) {
            if (status == CandleVatBlock.FLESH) {
                be.setStatus(CandleVatBlock.IMPURE);
            } else if (status == CandleVatBlock.LIQUID && !be.bundle.isEmpty()) {
                ScentData scent = be.bundle.get(ARRegistry.SCENT.get());
                be.oil = scent != null ? scent.scent() : null;
                be.bundle = ItemStack.EMPTY;
                be.setStatus(CandleVatBlock.IMBUED);
            }
        } else if (be.timer < -AromaraTuning.getInt("candle_vat.spoil_ticks") && status != CandleVatBlock.FLESH) {
            ScentData scent = be.bundle.get(ARRegistry.SCENT.get());
            if (be.oil == null && scent != null) {
                be.oil = scent.scent();
            }
            be.bundle = ItemStack.EMPTY;
            be.timer = 0;
            be.setStatus(CandleVatBlock.RANCID);
            level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.6f, 0.6f);
        }
        be.setChanged();
    }

    /** Tubes are pulled from, not pushed into: take one ignis from the tube or jar under the vat. */
    private void drawIgnis(Level level, BlockPos pos) {
        int max = AromaraTuning.getInt("candle_vat.essentia_max");
        Holder<IAspect> want = ignisHolder();
        if (ignis >= max || want == null) {
            return;
        }
        IEssentiaTransport source = level.getCapability(EssentiaCapabilities.TRANSPORT, pos.below(), Direction.UP);
        int suction = sink.getSuctionAmount(Direction.DOWN);
        if (source == null || source == sink || !source.canOutputTo(Direction.UP) || source.getEssentiaAmount(Direction.UP) <= 0
                || source.getSuctionAmount(Direction.UP) >= suction || suction < source.getMinimumSuction()) {
            return;
        }
        Holder<IAspect> type = source.getEssentiaType(Direction.UP);
        if (type != null && type.equals(want) && source.takeEssentia(type, 1, Direction.UP) > 0) {
            ignis++;
            setChanged();
        }
    }

    private void catchThrownItems(Level level, BlockPos pos) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).deflate(0.1).move(0, 0.2, 0), ItemEntity::isAlive);
        for (ItemEntity item : items) {
            ItemStack stack = item.getItem();
            if (!stack.isEmpty() && mixIn(stack)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(stack);
                }
                return;
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bundle);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("Ignis", ignis);
        out.putInt("Timer", timer);
        out.store("Bundle", ItemStack.OPTIONAL_CODEC, bundle);
        out.storeNullable("Oil", Identifier.CODEC, oil);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        ignis = in.getIntOr("Ignis", 0);
        timer = in.getIntOr("Timer", 0);
        bundle = in.read("Bundle", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        oil = in.read("Oil", Identifier.CODEC).orElse(null);
    }

    /** The vat's bottom face as a tube endpoint: asks for ignis while it has room, never gives any out. */
    private final class Sink implements IEssentiaTransport {
        @Override
        public boolean isConnectable(Direction side) {
            return side == Direction.DOWN;
        }

        @Override
        public boolean canInputFrom(Direction side) {
            return side == Direction.DOWN;
        }

        @Override
        public boolean canOutputTo(Direction side) {
            return false;
        }

        @Override
        public void setSuction(@Nullable Holder<IAspect> type, int amount) {
        }

        @Override
        public @Nullable Holder<IAspect> getSuctionType(@Nullable Direction side) {
            return ignis < AromaraTuning.getInt("candle_vat.essentia_max") ? ignisHolder() : null;
        }

        @Override
        public int getSuctionAmount(@Nullable Direction side) {
            return ignis < AromaraTuning.getInt("candle_vat.essentia_max") ? AromaraTuning.getInt("candle_vat.suction") : 0;
        }

        @Override
        public int takeEssentia(Holder<IAspect> type, int amount, Direction side) {
            return 0;
        }

        @Override
        public int addEssentia(Holder<IAspect> type, int amount, Direction side) {
            return addEssentia(type, amount, side, false);
        }

        @Override
        public int addEssentia(Holder<IAspect> type, int amount, Direction side, boolean simulate) {
            if (!type.is(TTAspects.IGNIS)) {
                return 0;
            }
            int moved = Math.max(0, Math.min(amount, AromaraTuning.getInt("candle_vat.essentia_max") - ignis));
            if (!simulate && moved > 0) {
                ignis += moved;
                setChanged();
            }
            return moved;
        }

        @Override
        public @Nullable Holder<IAspect> getEssentiaType(@Nullable Direction side) {
            return null;
        }

        @Override
        public int getEssentiaAmount(@Nullable Direction side) {
            return 0;
        }

        @Override
        public int getMinimumSuction() {
            return 0;
        }
    }
}
