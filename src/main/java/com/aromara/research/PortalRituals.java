package com.aromara.research;

import com.aromara.ARRegistry;
import com.aromara.Aromara;
import com.aromara.util.TTSounds;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeAccess;
import com.leclowndu93150.thaumaturge.api.capability.ResearchStatus;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Portal rituals ({@code data/<ns>/aromara/portal_ritual/<name>.json}): a player who knows {@code research} throws
 * each of the {@code offerings} into a {@code portal} creature; the portal remembers every offering it swallowed
 * (the {@code aromara:portal_offerings} attachment), crackles once it has them all, and drops {@code drop} when it
 * dies. Aromara ships one: brain, scribing tools and an ender pearl into a lesser cultist portal for the
 * Servoscrivener. Only items the right player threw count (the {@code aromara:scholar_toss} attachment).
 */
public final class PortalRituals {
    public record Ritual(EntityType<?> portal, Identifier research, List<Item> offerings, Item drop) {
        public static final Codec<Ritual> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("portal").forGetter(Ritual::portal),
                Identifier.CODEC.fieldOf("research").forGetter(Ritual::research),
                BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("offerings").forGetter(Ritual::offerings),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("drop").forGetter(Ritual::drop)
        ).apply(i, Ritual::new));

        boolean completeOn(Entity portal) {
            return portal.getType() == this.portal && portal.getData(ARRegistry.PORTAL_OFFERINGS.get()).containsAll(offerings);
        }
    }

    private static volatile List<Ritual> rituals = List.of();

    private PortalRituals() {
    }

    public static List<Ritual> all() {
        return rituals;
    }

    /** Client side: the server's rituals, for JEI (see {@code AromaraDataSync}). */
    public static void set(List<Ritual> next) {
        rituals = List.copyOf(next);
    }

    public static void register(IEventBus bus) {
        bus.addListener(PortalRituals::onToss);
        bus.addListener(PortalRituals::onEntityTick);
        bus.addListener(PortalRituals::onDrops);
    }

    private static boolean knows(Player player, Identifier research) {
        return KnowledgeAccess.of(player).researchStatus(research) != ResearchStatus.UNKNOWN;
    }

    private static void onToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) {
            return;
        }
        ItemStack stack = event.getEntity().getItem();
        for (Ritual r : rituals) {
            if (r.offerings().contains(stack.getItem()) && knows(player, r.research())) {
                event.getEntity().setData(ARRegistry.SCHOLAR_TOSS.get(), true);
                return;
            }
        }
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || rituals.isEmpty()) {
            return;
        }
        if (entity instanceof ItemEntity item) {
            if (item.tickCount % 10 == 0 && item.hasData(ARRegistry.SCHOLAR_TOSS.get())) {
                feed(level, item);
            }
        } else if (entity instanceof LivingEntity && entity.tickCount % 4 == 0 && entity.hasData(ARRegistry.PORTAL_OFFERINGS.get())) {
            for (Ritual r : rituals) {
                if (r.completeOn(entity)) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight(),
                            entity.getZ(), 3, entity.getBbWidth() * 0.4, 0.2, entity.getBbWidth() * 0.4, 0.05);
                    if (level.getRandom().nextInt(40) == 0) {
                        level.playSound(null, entity.blockPosition(), TTSounds.JACOBS, SoundSource.HOSTILE, 1.0f, 1.0f);
                    }
                    break;
                }
            }
        }
    }

    /** A scholar's offering that touches a ritual portal is swallowed, one item per offering kind. */
    private static void feed(ServerLevel level, ItemEntity item) {
        Item offered = item.getItem().getItem();
        for (LivingEntity portal : level.getEntitiesOfClass(LivingEntity.class, item.getBoundingBox().inflate(0.25))) {
            for (Ritual r : rituals) {
                if (portal.getType() != r.portal() || !r.offerings().contains(offered)) {
                    continue;
                }
                List<Item> have = portal.getData(ARRegistry.PORTAL_OFFERINGS.get());
                if (have.contains(offered)) {
                    continue;
                }
                List<Item> now = new ArrayList<>(have);
                now.add(offered);
                portal.setData(ARRegistry.PORTAL_OFFERINGS.get(), List.copyOf(now));
                item.getItem().shrink(1);
                if (item.getItem().isEmpty()) {
                    item.discard();
                }
                level.sendParticles(ParticleTypes.PORTAL, item.getX(), item.getY(), item.getZ(), 20, 0.2, 0.2, 0.2, 0.3);
                level.playSound(null, portal.blockPosition(), TTSounds.ZAP, SoundSource.HOSTILE, 1.0f, 0.8f);
                return;
            }
        }
    }

    private static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !entity.hasData(ARRegistry.PORTAL_OFFERINGS.get())) {
            return;
        }
        for (Ritual r : rituals) {
            if (r.completeOn(entity)) {
                event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), new ItemStack(r.drop())));
                level.playSound(null, entity.blockPosition(), TTSounds.EGSCREECH, SoundSource.HOSTILE, 1.0f, 1.0f);
            }
        }
    }

    public static final class Listener extends SimpleJsonResourceReloadListener<Ritual> {
        public Listener() {
            super(Ritual.CODEC, FileToIdConverter.json("aromara/portal_ritual"));
        }

        @Override
        protected void apply(Map<Identifier, Ritual> files, ResourceManager rm, ProfilerFiller profiler) {
            rituals = List.copyOf(files.values());
            Aromara.LOG.info("Aromara portal rituals: {}", files.keySet());
        }
    }
}
