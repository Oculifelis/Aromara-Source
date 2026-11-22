package com.aromara.research;

import com.aromara.block.ServoscrivenerBlockEntity;
import com.aromara.data.AromaraTuning;
import com.aromara.item.ResearchBriefItem;
import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.research.ResearchEvent;
import com.leclowndu93150.thaumaturge.content.essentia.jar.AbstractBlockEntityJar;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

/**
 * The two halves of the riddle research around the Servoscrivener: finishing a hypothesis entry hands out its open
 * Research Brief, and emptying a jar (sneak, empty hand) near a Servoscrivener that asks for that aspect pays the
 * jar's essentia into the riddle instead of letting it out as flux. A jar of the wrong aspect empties as usual.
 */
public final class ScrivenerEvents {
    private ScrivenerEvents() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ScrivenerEvents::onResearchCompleted);
        bus.addListener(ScrivenerEvents::onRightClickBlock);
    }

    private static void onResearchCompleted(ResearchEvent.Completed event) {
        if (!(event.player() instanceof ServerPlayer player)) {
            return;
        }
        for (Map.Entry<Identifier, Riddles.Riddle> e : Riddles.all().entrySet()) {
            if (e.getValue().hypothesis().equals(event.research())) {
                ItemStack brief = ResearchBriefItem.create(e.getKey(), false);
                if (!player.addItem(brief)) {
                    player.drop(brief, false);
                }
            }
        }
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isCrouching() || !player.getMainHandItem().isEmpty()
                || !(event.getLevel() instanceof ServerLevel level)
                || !(level.getBlockEntity(event.getPos()) instanceof AbstractBlockEntityJar jar)) {
            return;
        }
        ResourceKey<IAspect> aspect = jar.aspectKey();
        int amount = jar.amount();
        if (aspect == null || amount <= 0) {
            return;
        }
        ServoscrivenerBlockEntity scrivener = nearestWanting(level, event.getPos(), aspect);
        Holder<IAspect> holder = Aspects.resolve(level, aspect);
        if (scrivener == null || holder == null || !jar.drain(holder, amount)) {
            return;
        }
        scrivener.offer(aspect, amount);
        jar.clearAspectIfEmpty();
        BlockPos p = event.getPos();
        level.sendParticles(ParticleTypes.WITCH, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.02);
        level.playSound(null, p, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6f, 1.4f);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static @Nullable ServoscrivenerBlockEntity nearestWanting(ServerLevel level, BlockPos pos, ResourceKey<IAspect> aspect) {
        double radius = AromaraTuning.get("servoscrivener.jar_radius");
        int r = (int) Math.ceil(radius);
        ServoscrivenerBlockEntity best = null;
        double bestDist = radius * radius;
        for (int cx = (pos.getX() - r) >> 4; cx <= (pos.getX() + r) >> 4; cx++) {
            for (int cz = (pos.getZ() - r) >> 4; cz <= (pos.getZ() + r) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof ServoscrivenerBlockEntity s && s.wants(aspect)) {
                        double d = s.getBlockPos().distSqr(pos);
                        if (d <= bestDist) {
                            best = s;
                            bestDist = d;
                        }
                    }
                }
            }
        }
        return best;
    }
}
