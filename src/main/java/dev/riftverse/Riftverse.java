package dev.riftverse;

import com.mojang.logging.LogUtils;
import dev.riftverse.energy.EnergyNet;
import dev.riftverse.registry.ModBiomeModifiers;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModCreativeTab;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModEntities;
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
 * Riftverse – Multiversum-Mod im FTB-Stil: Rift-Kanone, Universen mit eigener Physik,
 * Maschinen-Hierarchie mit IndustrialCraft-artigem Stromnetz.
 */
@Mod(Riftverse.MODID)
public class Riftverse {
    public static final String MODID = "riftverse";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Riftverse(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModWorldgen.CHUNK_GENERATORS.register(modBus);
        ModWorldgen.BIOME_SOURCES.register(modBus);
        ModCreativeTab.TABS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModDataComponents.COMPONENTS.register(modBus);
        ModBiomeModifiers.SERIALIZERS.register(modBus);

        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(ModBlockEntities::registerCapabilities);

        // Riftverse-Stromnetz: verteilt am Ende jedes Server-Ticks
        NeoForge.EVENT_BUS.addListener(EnergyNet::onLevelTick);
        NeoForge.EVENT_BUS.addListener(EnergyNet::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(EnergyNet::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(EnergyNet::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(EnergyNet::onServerStopped);

        LOGGER.info("Riftverse geladen – das Multiversum ist offen.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
