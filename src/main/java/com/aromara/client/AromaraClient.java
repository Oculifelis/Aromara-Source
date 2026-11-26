package com.aromara.client;

import com.aromara.ARRegistry;
import com.aromara.Aromara;
import com.aromara.item.ScentData;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client hooks: the Brazier Cart draws as a minecart carrying the brazier block, and the bundle and phial pick their
 * model from their scent data ({@code aromara:bundle}, {@code aromara:filled} in the item models).
 */
@EventBusSubscriber(modid = Aromara.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AromaraClient {
    private AromaraClient() {
    }

    @SubscribeEvent
    static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ARRegistry.BRAZIER_CART_TYPE.get(), ctx -> new MinecartRenderer<>(ctx, ModelLayers.MINECART));
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ARRegistry.REDOLENT_BUNDLE.get(), Aromara.id("bundle"), (stack, level, entity, seed) -> {
                ScentData data = stack.get(ARRegistry.SCENT.get());
                if (data == null || !data.scent().getNamespace().equals(Aromara.MODID)) {
                    return 0.0f;
                }
                float base = switch (data.scent().getPath()) {
                    case "cinderpearl" -> 0.1f;
                    case "shimmerleaf" -> 0.3f;
                    case "vishroom" -> 0.5f;
                    default -> 0.0f;
                };
                return base == 0.0f ? 0.0f : base + (data.dried() ? 0.05f : 0.0f);
            });
            ItemProperties.register(ARRegistry.SCENT_PHIAL.get(), Aromara.id("filled"),
                    (stack, level, entity, seed) -> stack.has(ARRegistry.OIL.get()) ? 1.0f : 0.0f);
        });
    }
}
