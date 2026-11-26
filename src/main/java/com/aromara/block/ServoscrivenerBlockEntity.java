package com.aromara.block;

import com.aromara.ARRegistry;
import com.aromara.data.AromaraTuning;
import com.aromara.item.BriefData;
import com.aromara.item.ResearchBriefItem;
import com.aromara.research.Riddles;
import com.aromara.util.TTSounds;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.aromara.util.ValueInput;
import com.aromara.util.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one open Research Brief. On insertion it picks a few marked words of the brief's riddle; each stands for an
 * aspect and amount. A player answers by emptying a jar of that aspect nearby (see ScrivenerEvents). When every
 * picked word is paid for, the brief is marked done and pops out of the top.
 */
public class ServoscrivenerBlockEntity extends BlockEntity {
    /** One picked word: its index among the riddle's marked words, its aspect and how much is still owed. */
    public record Need(int word, ResourceKey<IAspect> aspect, int left) {
        public static final Codec<Need> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("word").forGetter(Need::word),
                ResourceKey.codec(TTAspects.IGNIS.registryKey()).fieldOf("aspect").forGetter(Need::aspect),
                Codec.INT.fieldOf("left").forGetter(Need::left)
        ).apply(i, Need::new));
    }

    private ItemStack brief = ItemStack.EMPTY;
    private final List<Need> needs = new ArrayList<>();
    private int delay;
    private boolean started;

    public ServoscrivenerBlockEntity(BlockPos pos, BlockState state) {
        super(ARRegistry.SERVOSCRIVENER_BE.get(), pos, state);
    }

    public boolean hasBrief() {
        return !brief.isEmpty();
    }

    /** Takes an open brief whose riddle is known; returns false (and says why) otherwise. */
    boolean insert(ItemStack held, Player player) {
        BriefData data = held.get(ARRegistry.BRIEF.get());
        if (hasBrief() || data == null || data.done() || level == null) {
            return false;
        }
        Riddles.Riddle riddle = Riddles.get(data.research());
        if (riddle == null || riddle.words().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.aromara.scrivener.unknown"), true);
            return false;
        }
        brief = held.copyWithCount(1);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < riddle.words().size(); i++) {
            order.add(i);
        }
        Collections.shuffle(order, new Random(level.getRandom().nextLong()));
        List<Integer> picked = new ArrayList<>(order.subList(0, Math.min(riddle.picks(), order.size())));
        Collections.sort(picked);
        needs.clear();
        for (int i : picked) {
            Riddles.Word w = riddle.words().get(i);
            needs.add(new Need(i, w.aspect(), w.amount()));
        }
        delay = 0;
        started = false;
        level.playSound(null, worldPosition, TTSounds.WRITE, SoundSource.BLOCKS, 2.0f, 1.0f);
        setPaper(true);
        setChanged();
        return true;
    }

    /** Sneak with an empty hand: gives the brief back unsolved. */
    ItemStack takeOut() {
        ItemStack out = brief;
        annul();
        return out;
    }

    private void annul() {
        brief = ItemStack.EMPTY;
        needs.clear();
        delay = 0;
        started = false;
        setPaper(false);
        setChanged();
    }

    private void setPaper(boolean paper) {
        if (level != null && getBlockState().getValue(ServoscrivenerBlock.HAS_PAPER) != paper) {
            level.setBlock(worldPosition, getBlockState().setValue(ServoscrivenerBlock.HAS_PAPER, paper), 3);
        }
    }

    public boolean wants(ResourceKey<IAspect> aspect) {
        return hasBrief() && needs.stream().anyMatch(n -> n.left() > 0 && n.aspect().equals(aspect));
    }

    /** Pays {@code amount} of {@code aspect} into the words that ask for it, in order. */
    public void offer(ResourceKey<IAspect> aspect, int amount) {
        for (int i = 0; i < needs.size() && amount > 0; i++) {
            Need n = needs.get(i);
            if (n.left() > 0 && n.aspect().equals(aspect)) {
                int paid = Math.min(amount, n.left());
                needs.set(i, new Need(n.word(), n.aspect(), n.left() - paid));
                amount -= paid;
            }
        }
        setChanged();
    }

    /** Sends the riddle with the picked words in purple and what each still needs, solved ones in green. */
    void inform(Player player) {
        BriefData data = brief.get(ARRegistry.BRIEF.get());
        Riddles.@Nullable Riddle riddle = data == null ? null : Riddles.get(data.research());
        if (riddle == null || level == null) {
            return;
        }
        level.playSound(null, worldPosition, TTSounds.CHANT, SoundSource.BLOCKS, 0.5f, 1.0f);
        player.sendSystemMessage(riddleText(riddle.text()));
    }

    private MutableComponent riddleText(String text) {
        MutableComponent out = Component.empty().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        StringBuilder plain = new StringBuilder();
        int word = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '@') {
                plain.append(c);
                continue;
            }
            int end = i + 1;
            while (end < text.length() && (Character.isLetter(text.charAt(end)) || text.charAt(end) == '-' || text.charAt(end) == '\'')) {
                end++;
            }
            String w = text.substring(i + 1, end);
            int index = word++;
            Need need = null;
            for (Need n : needs) {
                if (n.word() == index) {
                    need = n;
                }
            }
            if (need == null) {
                plain.append(w);
            } else {
                out.append(Component.literal(plain.toString()));
                plain.setLength(0);
                if (need.left() > 0) {
                    out.append(Component.literal(w + "(" + need.left() + ")").withStyle(ChatFormatting.DARK_PURPLE));
                } else {
                    out.append(Component.literal(w).withStyle(ChatFormatting.DARK_GREEN));
                }
            }
            i = end - 1;
        }
        out.append(Component.literal(plain.toString()));
        return out;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ServoscrivenerBlockEntity be) {
        if (!be.hasBrief() || be.needs.isEmpty()) {
            return;
        }
        if (!be.started) {
            if (++be.delay >= AromaraTuning.getInt("servoscrivener.riddle_delay")) {
                be.started = true;
                double r = AromaraTuning.get("servoscrivener.inform_radius");
                for (Player p : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(r))) {
                    be.inform(p);
                }
                be.setChanged();
            }
            return;
        }
        if (be.needs.stream().allMatch(n -> n.left() <= 0)) {
            be.finish(level, pos);
        }
    }

    private void finish(Level level, BlockPos pos) {
        ItemStack done = brief.copy();
        BriefData data = done.get(ARRegistry.BRIEF.get());
        if (data != null) {
            ResearchBriefItem.setBrief(done, data.asDone());
        }
        annul();
        level.playSound(null, pos, TTSounds.LEARN, SoundSource.BLOCKS, 3.0f, 1.0f);
        ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, done);
        drop.setDeltaMovement(0, 0.2, 0);
        level.addFreshEntity(drop);
    }

    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), brief);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput out = new ValueOutput(tag, registries);
        out.store("Brief", ItemStack.OPTIONAL_CODEC, brief);
        out.store("Needs", Need.CODEC.listOf(), List.copyOf(needs));
        out.putInt("Delay", delay);
        out.putBoolean("Started", started);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput in = new ValueInput(tag, registries);
        brief = in.read("Brief", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        needs.clear();
        in.read("Needs", Need.CODEC.listOf()).ifPresent(needs::addAll);
        delay = in.getIntOr("Delay", 0);
        started = in.getBooleanOr("Started", false);
    }
}
