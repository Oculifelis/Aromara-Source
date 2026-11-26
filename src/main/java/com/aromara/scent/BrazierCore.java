package com.aromara.scent;

import com.aromara.ARRegistry;
import com.aromara.data.AromaraTuning;
import com.aromara.data.Scents;
import com.aromara.item.ScentData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import com.aromara.util.ValueInput;
import com.aromara.util.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * What burns inside an Arcane Brazier, shared by the block and the Brazier Cart. Coal and a dried bundle go in by
 * hand; once lit it uses one coal every {@code ticks_per_coal} ticks and each time spreads the bundle's vapour. A brew
 * poured on top burns beside the scent (see {@link BrazierBrews}).
 */
public final class BrazierCore {
    public static final int EMPTY = 0;
    public static final int FUELLED = 1;
    public static final int BURNING = 2;

    private final Runnable onChange;
    private ItemStack coal = ItemStack.EMPTY;
    private ItemStack bundle = ItemStack.EMPTY;
    private @Nullable ResourceLocation burning;
    private int pulse;
    private @Nullable PotionContents brew;
    private int brewTicks;
    private int brewRefreshes;

    /** {@code onChange} runs after anything that may change {@link #status()}. */
    public BrazierCore(Runnable onChange) {
        this.onChange = onChange;
    }

    public boolean isLit() {
        return burning != null;
    }

    public int status() {
        return isLit() ? BURNING : !coal.isEmpty() ? FUELLED : EMPTY;
    }

    /** The live coal stack (hoppers grow it in place, then call {@link #changed()}). */
    public ItemStack coal() {
        return coal;
    }

    public void setCoal(ItemStack stack) {
        coal = stack;
        changed();
    }

    public static int maxCoal() {
        return AromaraTuning.getInt("brazier.max_coal");
    }

    public boolean acceptsCoal(ItemStack stack) {
        return stack.is(ARRegistry.BRAZIER_FUEL) && (coal.isEmpty() || ItemStack.isSameItemSameComponents(coal, stack));
    }

    /** Takes what it can of the held stack; returns how many items went in. */
    public int insert(ItemStack held) {
        if (held.is(ARRegistry.BRAZIER_FUEL)) {
            if (!acceptsCoal(held)) {
                return 0;
            }
            int moved = Math.max(0, Math.min(maxCoal() - coal.getCount(), held.getCount()));
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

    /** Hands back the bundle, then the coal; nothing while lit. */
    public ItemStack takeOut() {
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

    /** Pours a brew on; false if unlit, already burning a brew, or the item is no brew. */
    public boolean pour(ItemStack held) {
        PotionContents contents = BrazierBrews.pourable(held);
        if (!isLit() || brew != null || contents == null) {
            return false;
        }
        brew = contents;
        brewTicks = BrazierBrews.burnTicks(contents);
        brewRefreshes = 0;
        changed();
        return true;
    }

    /** Uses up a poured potion item, handing back a glass bottle for a drinkable one. */
    public static void usePoured(Player player, InteractionHand hand, ItemStack held) {
        if (player.getAbilities().instabuild) {
            return;
        }
        boolean bottle = held.is(Items.POTION);
        held.shrink(1);
        if (bottle) {
            ItemStack glass = new ItemStack(Items.GLASS_BOTTLE);
            if (held.isEmpty()) {
                player.setItemInHand(hand, glass);
            } else if (!player.getInventory().add(glass)) {
                player.drop(glass, false);
            }
        }
    }

    public boolean canIgnite() {
        ScentData scent = bundle.get(ARRegistry.SCENT.get());
        return !isLit() && !coal.isEmpty() && scent != null && scent.dried() && Scents.get(scent.scent()) != null;
    }

    public void ignite() {
        ScentData scent = bundle.get(ARRegistry.SCENT.get());
        if (scent == null) {
            return;
        }
        burning = scent.scent();
        bundle = ItemStack.EMPTY;
        pulse = 0;
        changed();
    }

    public void extinguish() {
        burning = null;
        brew = null;
        brewTicks = 0;
        pulse = 0;
        changed();
    }

    public void changed() {
        onChange.run();
    }

    /** One server tick at {@code pos}; returns true if anything was saved-worthy changed. */
    public boolean tick(ServerLevel level, BlockPos pos) {
        if (burning == null) {
            return false;
        }
        if (brew != null) {
            if (brewTicks % Math.max(1, AromaraTuning.getInt("brew.refresh_ticks")) == 0) {
                BrazierBrews.pulse(brew, level, pos, brewRefreshes++);
            }
            if (--brewTicks <= 0) {
                brew = null;
            }
        }
        // A brew and a scent together burn the coal faster.
        int perCoal = AromaraTuning.getInt("brazier.ticks_per_coal");
        if (brew != null) {
            perCoal = Math.max(1, perCoal / Math.max(1, AromaraTuning.getInt("brew.coal_rate")));
        }
        if (++pulse < perCoal) {
            return true;
        }
        pulse = 0;
        if (coal.isEmpty()) {
            extinguish();
            return true;
        }
        coal.shrink(1);
        Scents.Definition def = Scents.get(burning);
        if (def != null) {
            BrazierEffects.apply(def.brazierEffect(), level, pos);
        }
        if (coal.isEmpty()) {
            extinguish();
        }
        return true;
    }

    /** The coal and bundle, emptied out for dropping. */
    public ItemStack[] drain() {
        ItemStack[] out = {coal, bundle};
        coal = ItemStack.EMPTY;
        bundle = ItemStack.EMPTY;
        return out;
    }

    public void save(ValueOutput out) {
        out.store("Coal", ItemStack.OPTIONAL_CODEC, coal);
        out.store("Bundle", ItemStack.OPTIONAL_CODEC, bundle);
        out.storeNullable("Burning", ResourceLocation.CODEC, burning);
        out.putInt("Pulse", pulse);
        out.storeNullable("Brew", PotionContents.CODEC, brew);
        out.putInt("BrewTicks", brewTicks);
        out.putInt("BrewRefreshes", brewRefreshes);
    }

    public void load(ValueInput in) {
        coal = in.read("Coal", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        bundle = in.read("Bundle", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        burning = in.read("Burning", ResourceLocation.CODEC).orElse(null);
        pulse = in.getIntOr("Pulse", 0);
        brew = in.read("Brew", PotionContents.CODEC).orElse(null);
        brewTicks = in.getIntOr("BrewTicks", 0);
        brewRefreshes = in.getIntOr("BrewRefreshes", 0);
    }
}
