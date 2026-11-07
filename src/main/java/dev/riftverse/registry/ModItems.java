package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.item.EnergiezelleItem;
import dev.riftverse.item.HandkurbelItem;
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

    // =====================================================================
    // Meilenstein 1 – Fundament (steht vorne im Kreativ-Tab)
    // =====================================================================

    // --- Maschinen und Netz ---
    public static final DeferredItem<BlockItem> GARAGEN_WERKBANK = blockItem("garagen_werkbank", ModBlocks.GARAGEN_WERKBANK);
    public static final DeferredItem<BlockItem> GARAGENPRESSE = blockItem("garagenpresse", ModBlocks.GARAGENPRESSE);
    public static final DeferredItem<BlockItem> KURBEL_DYNAMO = blockItem("kurbel_dynamo", ModBlocks.KURBEL_DYNAMO);
    public static final DeferredItem<BlockItem> TONNENREAKTOR = blockItem("tonnenreaktor", ModBlocks.TONNENREAKTOR);
    public static final DeferredItem<BlockItem> ERZMUEHLE = blockItem("erzmuehle", ModBlocks.ERZMUEHLE);
    public static final DeferredItem<BlockItem> WALZBANK = blockItem("walzbank", ModBlocks.WALZBANK);
    public static final DeferredItem<BlockItem> LEGIERUNGSOFEN = blockItem("legierungsofen", ModBlocks.LEGIERUNGSOFEN);
    public static final DeferredItem<BlockItem> KUPFERKABEL = blockItem("kupferkabel", ModBlocks.KUPFERKABEL);
    public static final DeferredItem<BlockItem> KUPFERKABEL_ISOLIERT = blockItem("kupferkabel_isoliert", ModBlocks.KUPFERKABEL_ISOLIERT);
    public static final DeferredItem<BlockItem> EINSPEISER = blockItem("einspeiser", ModBlocks.EINSPEISER);
    public static final DeferredItem<BlockItem> AUSSPEISER = blockItem("ausspeiser", ModBlocks.AUSSPEISER);
    public static final DeferredItem<BlockItem> GARAGEN_GEHAEUSE = blockItem("garagen_gehaeuse", ModBlocks.GARAGEN_GEHAEUSE);
    public static final DeferredItem<BlockItem> XENIT_GEHAEUSE = blockItem("xenit_gehaeuse", ModBlocks.XENIT_GEHAEUSE);
    public static final DeferredItem<BlockItem> KREATIV_QUELLE = ITEMS.register("kreativ_quelle",
            () -> new BlockItem(ModBlocks.KREATIV_QUELLE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    // --- Werkzeuge ---
    public static final DeferredItem<Item> GARAGEN_HAMMER = ITEMS.register("garagen_hammer",
            () -> new Item(new Item.Properties().durability(128)));
    public static final DeferredItem<Item> LOETKOLBEN = ITEMS.register("loetkolben",
            () -> new Item(new Item.Properties().durability(256)));
    public static final DeferredItem<HandkurbelItem> HANDKURBEL = ITEMS.register("handkurbel",
            () -> new HandkurbelItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<EnergiezelleItem> ENERGIEZELLE = ITEMS.register("energiezelle",
            () -> new EnergiezelleItem(new Item.Properties().stacksTo(1)));

    // --- Erze und Blöcke ---
    public static final DeferredItem<BlockItem> TIN_ORE = blockItem("tin_ore", ModBlocks.TIN_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_TIN_ORE = blockItem("deepslate_tin_ore", ModBlocks.DEEPSLATE_TIN_ORE);
    public static final DeferredItem<BlockItem> TIN_BLOCK = blockItem("tin_block", ModBlocks.TIN_BLOCK);
    public static final DeferredItem<BlockItem> XENIT_ORE = blockItem("xenit_ore", ModBlocks.XENIT_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_XENIT_ORE = blockItem("deepslate_xenit_ore", ModBlocks.DEEPSLATE_XENIT_ORE);
    public static final DeferredItem<BlockItem> XENIT_BLOCK = blockItem("xenit_block", ModBlocks.XENIT_BLOCK);

    // --- Metalle ---
    public static final DeferredItem<Item> RAW_TIN = simple("raw_tin");
    public static final DeferredItem<Item> TIN_INGOT = simple("tin_ingot");
    public static final DeferredItem<Item> RAW_XENIT = simple("raw_xenit");
    public static final DeferredItem<Item> XENIT_INGOT = simple("xenit_ingot");
    public static final DeferredItem<Item> BRONZE_INGOT = simple("bronze_ingot");
    public static final DeferredItem<Item> STEEL_INGOT = simple("steel_ingot");
    public static final DeferredItem<Item> MISCHMETALL = simple("mischmetall");

    // --- Staub (Erzmühle) ---
    public static final DeferredItem<Item> EISEN_STAUB = simple("eisen_staub");
    public static final DeferredItem<Item> KUPFER_STAUB = simple("kupfer_staub");
    public static final DeferredItem<Item> GOLD_STAUB = simple("gold_staub");
    public static final DeferredItem<Item> ZINN_STAUB = simple("zinn_staub");
    public static final DeferredItem<Item> XENIT_STAUB = simple("xenit_staub");
    public static final DeferredItem<Item> BRONZE_STAUB = simple("bronze_staub");
    public static final DeferredItem<Item> KOHLE_STAUB = simple("kohle_staub");

    // --- Platten (Hammer, Garagenpresse) ---
    public static final DeferredItem<Item> EISEN_PLATTE = simple("eisen_platte");
    public static final DeferredItem<Item> KUPFER_PLATTE = simple("kupfer_platte");
    public static final DeferredItem<Item> ZINN_PLATTE = simple("zinn_platte");
    public static final DeferredItem<Item> GOLD_PLATTE = simple("gold_platte");
    public static final DeferredItem<Item> BRONZE_PLATTE = simple("bronze_platte");
    public static final DeferredItem<Item> STAHL_PLATTE = simple("stahl_platte");
    public static final DeferredItem<Item> XENIT_PLATTE = simple("xenit_platte");

    // --- Walzbank ---
    public static final DeferredItem<Item> KUPFERDRAHT = simple("kupferdraht");
    public static final DeferredItem<Item> GOLDDRAHT = simple("golddraht");
    public static final DeferredItem<Item> LOETZINN = simple("loetzinn");
    public static final DeferredItem<Item> ZINN_HUELSE = simple("zinn_huelse");
    public static final DeferredItem<Item> EISEN_STANGE = simple("eisen_stange");
    public static final DeferredItem<Item> XENIT_STANGE = simple("xenit_stange");

    // --- Bauteile ---
    public static final DeferredItem<Item> BRONZE_ZAHNRAD = simple("bronze_zahnrad");
    public static final DeferredItem<Item> XENIT_SPULE = simple("xenit_spule");
    public static final DeferredItem<Item> ALIEN_SCHROTT = simple("alien_schrott");
    public static final DeferredItem<Item> SCHROTTPLATTE = simple("schrottplatte");
    public static final DeferredItem<Item> POLYMERFETZEN = simple("polymerfetzen");
    public static final DeferredItem<Item> ISOLIERFOLIE = simple("isolierfolie");
    public static final DeferredItem<Item> FEDER = simple("feder");
    public static final DeferredItem<Item> MINI_MOTOR = simple("mini_motor");

    // =====================================================================
    // Vorhandener Prototyp (Quanten-Kette) – wird in Meilenstein 2 ersetzt
    // =====================================================================

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

    private static DeferredItem<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<BlockItem> blockItem(String name, DeferredBlock<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private ModItems() {}
}
