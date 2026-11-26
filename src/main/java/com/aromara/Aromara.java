package com.aromara;

import com.aromara.data.AromaraDataSync;
import com.aromara.data.AromaraTuning;
import com.aromara.data.Scents;
import com.aromara.research.PortalRituals;
import com.aromara.research.Riddles;
import com.aromara.research.ScrivenerEvents;
import com.aromara.scent.BrazierBrews;
import com.aromara.scent.OilEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.slf4j.Logger;

/** Aromara - Thaumic Perfumery (kodesque, Thaumcraft 6), rebuilt on Thaumaturge's TC4 mechanics. See docs/COVERAGE.md. */
@Mod(Aromara.MODID)
public final class Aromara {
    public static final String MODID = "aromara";
    public static final Logger LOG = LogUtils.getLogger();

    public Aromara(IEventBus modBus) {
        ARRegistry.register(modBus);
        BrazierBrews.register(modBus);
        OilEvents.register(NeoForge.EVENT_BUS);
        ScrivenerEvents.register(NeoForge.EVENT_BUS);
        PortalRituals.register(NeoForge.EVENT_BUS);
        AromaraDataSync.register(modBus);
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent e) -> {
            e.addListener(id("tuning"), new AromaraTuning.Listener());
            e.addListener(id("scents"), new Scents.Listener());
            e.addListener(id("riddles"), new Riddles.Listener());
            e.addListener(id("portal_rituals"), new PortalRituals.Listener());
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
