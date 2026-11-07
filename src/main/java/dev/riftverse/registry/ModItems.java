package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.item.EnergiezelleItem;
import dev.riftverse.item.HandkurbelItem;
import dev.riftverse.item.FluidKartuscheItem;
import dev.riftverse.item.LoreItem;
import dev.riftverse.item.MeteoritenpeilerItem;
import dev.riftverse.item.PortalKanoneItem;
import dev.riftverse.item.UniverseScannerItem;
import dev.riftverse.item.XenitZelleItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
    // Meilenstein 2 – Spielbarer Prototyp
    // =====================================================================

    // --- Maschinen, Speicher, Anker ---
    public static final DeferredItem<BlockItem> XENIT_SCHMELZE = blockItem("xenit_schmelze", ModBlocks.XENIT_SCHMELZE);
    public static final DeferredItem<BlockItem> PLATINEN_AETZER = blockItem("platinen_aetzer", ModBlocks.PLATINEN_AETZER);
    public static final DeferredItem<BlockItem> KRISTALL_SCHLEIFER = blockItem("kristall_schleifer", ModBlocks.KRISTALL_SCHLEIFER);
    public static final DeferredItem<BlockItem> RECYCLER = blockItem("recycler", ModBlocks.RECYCLER);
    public static final DeferredItem<BlockItem> FLUID_MIXER = blockItem("fluid_mixer", ModBlocks.FLUID_MIXER);
    public static final DeferredItem<XenitZelleItem> XENIT_ZELLE = ITEMS.register("xenit_zelle",
            () -> new XenitZelleItem(ModBlocks.XENIT_ZELLE.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> RIFT_ANKER = blockItem("rift_anker", ModBlocks.RIFT_ANKER);

    // --- Kanone, Werkzeuge ---
    public static final DeferredItem<PortalKanoneItem> PORTAL_KANONE_MK1 = ITEMS.register("portal_kanone_mk1",
            () -> new PortalKanoneItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), 1));
    public static final DeferredItem<Item> FLUID_KARTUSCHE = ITEMS.register("fluid_kartusche",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<FluidKartuscheItem> FLUID_KARTUSCHE_VOLL = ITEMS.register("fluid_kartusche_voll",
            () -> new FluidKartuscheItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<MeteoritenpeilerItem> METEORITENPEILER = ITEMS.register("meteoritenpeiler",
            () -> new MeteoritenpeilerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UniverseScannerItem> UNIVERSE_SCANNER = ITEMS.register("universe_scanner",
            () -> new UniverseScannerItem(new Item.Properties().stacksTo(1)));

    // --- Bauteile und Materialien ---
    public static final DeferredItem<Item> KANONENGEHAEUSE = simple("kanonengehaeuse");
    public static final DeferredItem<Item> BASIS_SCHALTKREIS = simple("basis_schaltkreis");
    public static final DeferredItem<Item> FLUXKRISTALL = simple("fluxkristall");
    public static final DeferredItem<Item> FLUXSTAUB = simple("fluxstaub");
    public static final DeferredItem<Item> FLUXLINSE = simple("fluxlinse");
    public static final DeferredItem<Item> SAEUREGEL = ITEMS.register("saeuregel",
            () -> new LoreItem(new Item.Properties(), "item.riftverse.saeuregel.lore"));
    public static final DeferredItem<BucketItem> SAEURE_EIMER = ITEMS.register("saeure_eimer",
            () -> new BucketItem(ModFluids.SAEURE.get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // --- Blöcke aus der Welt ---
    public static final DeferredItem<BlockItem> METEORITENKERN = blockItem("meteoritenkern", ModBlocks.METEORITENKERN);
    public static final DeferredItem<BlockItem> SCHROTT_HAUFEN = blockItem("schrott_haufen", ModBlocks.SCHROTT_HAUFEN);
    public static final DeferredItem<BlockItem> FLUXKRISTALL_ORE = blockItem("fluxkristall_ore", ModBlocks.FLUXKRISTALL_ORE);
    public static final DeferredItem<BlockItem> FLUXKRISTALL_BLOCK = blockItem("fluxkristall_block", ModBlocks.FLUXKRISTALL_BLOCK);
    public static final DeferredItem<BlockItem> FLUXKRISTALL_CLUSTER = blockItem("fluxkristall_cluster", ModBlocks.FLUXKRISTALL_CLUSTER);
    public static final DeferredItem<BlockItem> KNOSPENDER_FLUXKRISTALL = blockItem("knospender_fluxkristall", ModBlocks.KNOSPENDER_FLUXKRISTALL);
    public static final DeferredItem<BlockItem> RIFT_STONE = blockItem("rift_stone", ModBlocks.RIFT_STONE);

    // --- Kreaturen ---
    public static final DeferredItem<DeferredSpawnEggItem> SCHROTT_GOLEM_SPAWN_EGG = ITEMS.register("schrott_golem_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SCHROTT_GOLEM, 0x5C2F1A, 0x39FF6A, new Item.Properties()));

    private static DeferredItem<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<BlockItem> blockItem(String name, DeferredBlock<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private ModItems() {}
}
