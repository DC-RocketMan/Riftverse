package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.item.LoreItem;
import dev.riftverse.item.RiftGunItem;
import dev.riftverse.item.UniverseScannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Riftverse.MODID);

    // --- Rohstoffe & Zwischenprodukte ---
    public static final DeferredItem<Item> RAW_QUANTUM = ITEMS.register("raw_quantum",
            () -> new LoreItem(new Item.Properties(), "item.riftverse.raw_quantum.lore"));
    public static final DeferredItem<Item> QUANTUM_INGOT = ITEMS.register("quantum_ingot",
            () -> new LoreItem(new Item.Properties(), "item.riftverse.quantum_ingot.lore"));
    public static final DeferredItem<Item> RIFT_FLUID_CELL = ITEMS.register("rift_fluid_cell",
            () -> new LoreItem(new Item.Properties().stacksTo(32), "item.riftverse.rift_fluid_cell.lore"));
    public static final DeferredItem<Item> RIFT_CORE = ITEMS.register("rift_core",
            () -> new LoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), "item.riftverse.rift_core.lore"));
    public static final DeferredItem<Item> GUN_FRAME = ITEMS.register("gun_frame",
            () -> new LoreItem(new Item.Properties().stacksTo(1), "item.riftverse.gun_frame.lore"));
    public static final DeferredItem<Item> UNIVERSE_SHARD = ITEMS.register("universe_shard",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.RARE), "item.riftverse.universe_shard.lore"));

    // --- Werkzeuge ---
    public static final DeferredItem<RiftGunItem> RIFT_GUN = ITEMS.register("rift_gun",
            () -> new RiftGunItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 1));
    public static final DeferredItem<RiftGunItem> RIFT_GUN_MK2 = ITEMS.register("rift_gun_mk2",
            () -> new RiftGunItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 2));
    public static final DeferredItem<UniverseScannerItem> UNIVERSE_SCANNER = ITEMS.register("universe_scanner",
            () -> new UniverseScannerItem(new Item.Properties().stacksTo(1)));

    // --- Block-Items ---
    public static final DeferredItem<BlockItem> QUANTUM_ORE = blockItem("quantum_ore", ModBlocks.QUANTUM_ORE);
    public static final DeferredItem<BlockItem> RIFT_STONE = blockItem("rift_stone", ModBlocks.RIFT_STONE);
    public static final DeferredItem<BlockItem> QUANTUM_REFINER = blockItem("quantum_refiner", ModBlocks.QUANTUM_REFINER);
    public static final DeferredItem<BlockItem> FLUID_SYNTHESIZER = blockItem("fluid_synthesizer", ModBlocks.FLUID_SYNTHESIZER);
    public static final DeferredItem<BlockItem> RIFT_ASSEMBLER = blockItem("rift_assembler", ModBlocks.RIFT_ASSEMBLER);

    // --- Test-/Debug-Hilfe: Boss direkt beschwören ---
    public static final DeferredItem<DeferredSpawnEggItem> RIFT_BOSS_SPAWN_EGG = ITEMS.register("rift_boss_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.RIFT_BOSS, 0x2B0F45, 0x39FF14, new Item.Properties()));

    private static DeferredItem<BlockItem> blockItem(String name, DeferredBlock<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private ModItems() {}
}
