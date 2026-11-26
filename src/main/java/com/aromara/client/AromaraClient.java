package com.aromara.client;

import com.aromara.ARRegistry;
import com.aromara.Aromara;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client hooks: the Brazier Cart draws as a minecart carrying the brazier block. */
@EventBusSubscriber(modid = Aromara.MODID, value = Dist.CLIENT)
public final class AromaraClient {
    private AromaraClient() {
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ARRegistry.BRAZIER_CART_TYPE.get(), ctx -> new MinecartRenderer(ctx, ModelLayers.MINECART));
    }
}
