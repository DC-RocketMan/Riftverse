package dev.riftverse.client;

import dev.riftverse.Riftverse;
import dev.riftverse.cable.InsulatedCableBlock;
import dev.riftverse.client.screen.GeneratorScreen;
import dev.riftverse.client.screen.ProcessingScreen;
import dev.riftverse.client.screen.WerkbankScreen;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModEntities;
import dev.riftverse.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Riftverse.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MACHINE.get(), MachineScreen::new);
        event.register(ModMenus.PROCESSING.get(), ProcessingScreen::new);
        event.register(ModMenus.GENERATOR.get(), GeneratorScreen::new);
        event.register(ModMenus.WERKBANK.get(), WerkbankScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RIFT_PORTAL.get(), RiftPortalRenderer::new);
        event.registerEntityRenderer(ModEntities.RIFT_BOSS.get(), RiftBossRenderer::new);
    }

    /** Streifen der isolierten Kabel: Xenit-Petrol oder die Farbe des Farbstoffs. */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> 0xFF000000 | InsulatedCableBlock.tint(state),
                ModBlocks.KUPFERKABEL_ISOLIERT.get());
    }

    private ClientSetup() {}
}
