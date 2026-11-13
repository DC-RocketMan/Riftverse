package dev.riftverse;

import com.mojang.logging.LogUtils;
import dev.riftverse.devtest.RauchTest;
import dev.riftverse.energy.EnergyNet;
import dev.riftverse.network.RueckkehrPayload;
import dev.riftverse.network.ZielPayload;
import dev.riftverse.registry.ModArmorMaterials;
import dev.riftverse.registry.ModAttachments;
import dev.riftverse.registry.ModBiomeModifiers;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModCreativeTab;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModEntities;
import dev.riftverse.registry.ModFluids;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModMenus;
import dev.riftverse.registry.ModRecipes;
import dev.riftverse.registry.ModWorldgen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Riftverse – Multiversum-Mod im FTB-Stil: Portal-Kanone, Universen mit eigener Physik,
 * Maschinen-Hierarchie mit IndustrialCraft-artigem Stromnetz.
 */
@Mod(Riftverse.MODID)
public class Riftverse {
    public static final String MODID = "riftverse";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Riftverse(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ModFluids.FLUID_TYPES.register(modBus);
        ModFluids.FLUIDS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModArmorMaterials.MATERIALS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModWorldgen.CHUNK_GENERATORS.register(modBus);
        ModWorldgen.BIOME_SOURCES.register(modBus);
        ModWorldgen.STRUCTURE_TYPES.register(modBus);
        ModWorldgen.STRUCTURE_PIECES.register(modBus);
        ModWorldgen.PLACEMENT_MODIFIERS.register(modBus);
        ModWorldgen.FEATURES.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModBiomeModifiers.SERIALIZERS.register(modBus);
        ModAttachments.ATTACHMENT_TYPES.register(modBus);

        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(ModEntities::registerSpawnPlacements);
        modBus.addListener(RueckkehrPayload::register);
        modBus.addListener(ZielPayload::register);
        modBus.addListener(dev.riftverse.network.RuestungPayload::register);
        modBus.addListener(ModBlockEntities::registerCapabilities);

        // Riftverse-Stromnetz: verteilt am Ende jedes Server-Ticks
        NeoForge.EVENT_BUS.addListener(EnergyNet::onLevelTick);
        NeoForge.EVENT_BUS.addListener(EnergyNet::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(EnergyNet::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(EnergyNet::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(EnergyNet::onServerStopped);

        // Nur mit -Driftverse.rauchtest=true (GitHub Actions: ./gradlew runRauchtest)
        RauchTest.registerIfEnabled();

        LOGGER.info("Riftverse geladen – das Multiversum ist offen.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
