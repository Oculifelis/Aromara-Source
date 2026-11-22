package com.aromara.data;

import com.aromara.Aromara;
import com.aromara.research.PortalRituals;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Sends the scent and portal ritual datapacks to clients on every datapack sync, so JEI can show the pressing, vat,
 * brazier and ritual pages on a dedicated server too. Everything that acts on them still runs on the server.
 */
public record AromaraDataSync(Map<Identifier, Scents.Definition> scents, List<PortalRituals.Ritual> rituals) implements CustomPacketPayload {
    public static final Type<AromaraDataSync> TYPE = new Type<>(Aromara.id("data_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AromaraDataSync> CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.fromCodecWithRegistries(Scents.Definition.CODEC)),
            AromaraDataSync::scents,
            ByteBufCodecs.fromCodecWithRegistries(PortalRituals.Ritual.CODEC).apply(ByteBufCodecs.list()), AromaraDataSync::rituals,
            AromaraDataSync::new);

    @Override
    public Type<AromaraDataSync> type() {
        return TYPE;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").optional().playToClient(TYPE, CODEC, (payload, ctx) -> {
            Scents.set(payload.scents());
            PortalRituals.set(payload.rituals());
        }));
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> {
            AromaraDataSync payload = new AromaraDataSync(Scents.all(), PortalRituals.all());
            e.getRelevantPlayers().forEach(p -> PacketDistributor.sendToPlayer(p, payload));
        });
    }
}
