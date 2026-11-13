package dev.riftverse.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riftverse.Riftverse;
import dev.riftverse.cable.InsulatedCableBlock;
import dev.riftverse.client.screen.FluidMixerScreen;
import dev.riftverse.client.screen.GeneratorScreen;
import dev.riftverse.client.screen.IndustrieScreen;
import dev.riftverse.client.screen.ProcessingScreen;
import dev.riftverse.client.screen.RaffinerieScreen;
import dev.riftverse.client.screen.ResonatorScreen;
import dev.riftverse.client.screen.SpeicherScreen;
import dev.riftverse.client.screen.WerkbankScreen;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModDataComponents.PeilerZiel;
import dev.riftverse.registry.ModEntities;
import dev.riftverse.registry.ModFluids;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModMenus;
import dev.riftverse.universe.SpiegelZeit;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Riftverse.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    /** Rückkehr-Taste der Portal-Kanone (Standard R). */
    public static final KeyMapping RUECKKEHR = new KeyMapping("key.riftverse.rueckkehr", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.riftverse");
    /** Ziel-Taste der Kanone ab Mk III (Standard V). */
    public static final KeyMapping ZIEL = new KeyMapping("key.riftverse.ziel", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.riftverse");

    // --- Meilenstein 6: Multiversal-Rüstung (Phasenschritt: in der Luft noch einmal springen) ---
    public static final KeyMapping TARN = key("key.riftverse.tarn", GLFW.GLFW_KEY_H);
    public static final KeyMapping KANONE = key("key.riftverse.kanone", GLFW.GLFW_KEY_B);
    public static final KeyMapping GADGET_1 = key("key.riftverse.gadget_1", GLFW.GLFW_KEY_Z);
    public static final KeyMapping GADGET_2 = key("key.riftverse.gadget_2", GLFW.GLFW_KEY_X);
    public static final KeyMapping GRAVITATION = key("key.riftverse.gravitation", GLFW.GLFW_KEY_G);
    public static final KeyMapping SCHRUMPF = key("key.riftverse.schrumpf", GLFW.GLFW_KEY_N);

    private static KeyMapping key(String name, int code) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, "key.categories.riftverse");
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PROCESSING.get(), ProcessingScreen::new);
        event.register(ModMenus.GENERATOR.get(), GeneratorScreen::new);
        event.register(ModMenus.WERKBANK.get(), WerkbankScreen::new);
        event.register(ModMenus.FLUID_MIXER.get(), FluidMixerScreen::new);
        event.register(ModMenus.SPEICHER.get(), SpeicherScreen::new);
        event.register(ModMenus.RAFFINERIE.get(), RaffinerieScreen::new);
        event.register(ModMenus.RESONATOR.get(), ResonatorScreen::new);
        event.register(ModMenus.INDUSTRIE.get(), IndustrieScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.RIFT_PORTAL.get(), RiftPortalRenderer::new);
        event.registerEntityRenderer(ModEntities.RIFT_BOSS.get(), RiftBossRenderer::new);
        event.registerEntityRenderer(ModEntities.SCHROTT_GOLEM.get(), SchrottGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.HOLO_KOEDER.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerEntityRenderer(ModEntities.ANTI_GRAV_GRANATE.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(RUECKKEHR);
        event.register(ZIEL);
        for (KeyMapping k : new KeyMapping[]{TARN, KANONE, GADGET_1, GADGET_2, GRAVITATION, SCHRUMPF}) event.register(k);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.SAEURE.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.SAEURE_FLOWING.get(), RenderType.translucent());

            // Meteoritenpeiler: Nadel wie beim Kompass, grün in der Xenit-Zone
            ItemProperties.register(ModItems.METEORITENPEILER.get(), ResourceLocation.withDefaultNamespace("angle"),
                    (ItemPropertyFunction) new CompassItemPropertyFunction((level, stack, entity) -> {
                        PeilerZiel ziel = stack.get(ModDataComponents.PEILER.get());
                        return ziel == null ? null : ziel.pos();
                    }));
            ItemProperties.register(ModItems.METEORITENPEILER.get(), Riftverse.id("zone"),
                    (ItemPropertyFunction) (stack, level, entity, seed) -> {
                        PeilerZiel ziel = stack.get(ModDataComponents.PEILER.get());
                        return ziel != null && ziel.zone() ? 1.0f : 0.0f;
                    });
        });
    }

    /** Streifen der isolierten Kabel: Xenit-Petrol oder die Farbe des Farbstoffs. */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> 0xFF000000 | InsulatedCableBlock.tint(state),
                ModBlocks.KUPFERKABEL_ISOLIERT.get(), ModBlocks.FLUX_LEITUNG_ISOLIERT.get(),
                ModBlocks.GRAVITIT_LEITUNG_ISOLIERT.get(), ModBlocks.GRAVITIT_LEITUNG_DOPPELT.get());
    }

    /** Fluid-Texturen sind schon farbig, daher kein Tint. */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(fluid("saeure"), ModFluids.SAEURE_TYPE.get());
        event.registerFluidType(fluid("roh_portalfluid"), ModFluids.ROH_PORTALFLUID_TYPE.get());
        event.registerFluidType(fluid("portalfluid"), ModFluids.PORTALFLUID_TYPE.get());
        event.registerFluidType(fluid("portalfluid_konzentrat"), ModFluids.PORTALFLUID_KONZENTRAT_TYPE.get());
    }

    /** Spiegelwelt: eigener Himmelseffekt-Schlüssel (daran erkennt der Mixin die vertauschte Tageszeit), sonst wie die Overworld. */
    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(SpiegelZeit.EFFECTS, new DimensionSpecialEffects.OverworldEffects());
    }

    /** Portalfluid-Konzentrat durchscheinend rendern. */
    @SubscribeEvent
    public static void registerRenderLayers(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(ModFluids.PORTALFLUID_KONZENTRAT.get(), RenderType.translucent()));
    }

    private static IClientFluidTypeExtensions fluid(String name) {
        ResourceLocation still = Riftverse.id("block/" + name + "_still");
        ResourceLocation flow = Riftverse.id("block/" + name + "_flow");
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flow;
            }

            @Override
            public int getTintColor() {
                return 0xFFFFFFFF;
            }
        };
    }

    private ClientSetup() {}
}
