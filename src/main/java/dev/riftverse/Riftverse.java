package dev.riftverse;

import com.mojang.logging.LogUtils;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModCreativeTab;
import dev.riftverse.registry.ModEntities;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModMenus;
import dev.riftverse.registry.ModWorldgen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Riftverse – Rift-Kanone, zufällige Universen mit eigener Physik und
 * Bosse mit prozedural generierten Geschichten.
 */
@Mod(Riftverse.MODID)
public class Riftverse {
    public static final String MODID = "riftverse";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Riftverse(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModWorldgen.CHUNK_GENERATORS.register(modBus);
        ModWorldgen.BIOME_SOURCES.register(modBus);
        ModCreativeTab.TABS.register(modBus);

        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(ModBlockEntities::registerCapabilities);

        LOGGER.info("Riftverse geladen – das Multiversum ist offen.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
