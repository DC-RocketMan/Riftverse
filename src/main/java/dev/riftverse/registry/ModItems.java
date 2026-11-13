package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.item.EnergiezelleItem;
import dev.riftverse.item.HandkurbelItem;
import dev.riftverse.item.KoordinatenkarteItem;
import dev.riftverse.item.UniversumsprobeItem;
import dev.riftverse.tech.ChronoSpeicherBlockEntity;
import dev.riftverse.tech.FluxZelleBlockEntity;
import dev.riftverse.item.FluidKartuscheItem;
import dev.riftverse.item.LoreItem;
import dev.riftverse.item.MeteoritenpeilerItem;
import dev.riftverse.item.PortalKanoneItem;
import dev.riftverse.item.UniverseScannerItem;
import dev.riftverse.item.XenitZelleItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DoubleHighBlockItem;
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

    // =====================================================================
    // Meilenstein 3 – T2 Rift-Labor
    // =====================================================================

    // --- Netz, Speicher, Maschinen, Generatoren ---
    public static final DeferredItem<BlockItem> FLUX_LEITUNG = blockItem("flux_leitung", ModBlocks.FLUX_LEITUNG);
    public static final DeferredItem<BlockItem> FLUX_LEITUNG_ISOLIERT = blockItem("flux_leitung_isoliert", ModBlocks.FLUX_LEITUNG_ISOLIERT);
    public static final DeferredItem<BlockItem> UMSPANNER = blockItem("umspanner", ModBlocks.UMSPANNER);
    public static final DeferredItem<XenitZelleItem> FLUX_ZELLE = ITEMS.register("flux_zelle",
            () -> new XenitZelleItem(ModBlocks.FLUX_ZELLE.get(), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON),
                    FluxZelleBlockEntity.FLUX_CAPACITY, "tooltip.riftverse.flux_zelle"));
    public static final DeferredItem<BlockItem> LABOR_GEHAEUSE = blockItem("labor_gehaeuse", ModBlocks.LABOR_GEHAEUSE);
    public static final DeferredItem<BlockItem> LABOR_HUELLE = blockItem("labor_huelle", ModBlocks.LABOR_HUELLE);
    public static final DeferredItem<BlockItem> PRAEZISIONS_MONTIERER = blockItem("praezisions_montierer", ModBlocks.PRAEZISIONS_MONTIERER);
    public static final DeferredItem<BlockItem> QUANTENZENTRIFUGE = blockItem("quantenzentrifuge", ModBlocks.QUANTENZENTRIFUGE);
    public static final DeferredItem<BlockItem> GRAVITATIONSPRESSE = blockItem("gravitationspresse", ModBlocks.GRAVITATIONSPRESSE);
    public static final DeferredItem<BlockItem> DIMENSIONS_SCANNER = blockItem("dimensions_scanner", ModBlocks.DIMENSIONS_SCANNER);
    public static final DeferredItem<BlockItem> CHRONO_BESCHLEUNIGER = blockItem("chrono_beschleuniger", ModBlocks.CHRONO_BESCHLEUNIGER);
    public static final DeferredItem<BlockItem> PORTALFLUID_RAFFINERIE = blockItem("portalfluid_raffinerie", ModBlocks.PORTALFLUID_RAFFINERIE);
    public static final DeferredItem<BlockItem> BIOMASSE_KESSEL = blockItem("biomasse_kessel", ModBlocks.BIOMASSE_KESSEL);
    public static final DeferredItem<BlockItem> ZEITSCHLEIFEN_SOLAR = blockItem("zeitschleifen_solar", ModBlocks.ZEITSCHLEIFEN_SOLAR);
    public static final DeferredItem<BlockItem> FLUXKRISTALL_RESONATOR = blockItem("fluxkristall_resonator", ModBlocks.FLUXKRISTALL_RESONATOR);
    public static final DeferredItem<BlockItem> RESONATOR_KAMMER = blockItem("resonator_kammer", ModBlocks.RESONATOR_KAMMER);
    public static final DeferredItem<BlockItem> GRAVITATIONS_PENDEL = blockItem("gravitations_pendel", ModBlocks.GRAVITATIONS_PENDEL);
    public static final DeferredItem<BlockItem> SCHWEBSTEIN_KERN = blockItem("schwebstein_kern", ModBlocks.SCHWEBSTEIN_KERN);

    // --- Kanone und Fluid ---
    public static final DeferredItem<PortalKanoneItem> PORTAL_KANONE_MK2 = ITEMS.register("portal_kanone_mk2",
            () -> new PortalKanoneItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2));
    public static final DeferredItem<Item> FLUID_KARTUSCHE_STABIL = ITEMS.register("fluid_kartusche_stabil",
            () -> new LoreItem(new Item.Properties().stacksTo(16), "item.riftverse.fluid_kartusche_stabil.lore"));
    public static final DeferredItem<UniversumsprobeItem> UNIVERSUMSPROBE = ITEMS.register("universumsprobe",
            () -> new UniversumsprobeItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<KoordinatenkarteItem> KOORDINATENKARTE = ITEMS.register("koordinatenkarte",
            () -> new KoordinatenkarteItem(new Item.Properties().stacksTo(1)));

    // --- Zwischenprodukte ---
    public static final DeferredItem<Item> VERBUNDPLATTE = simple("verbundplatte");
    public static final DeferredItem<Item> FLUX_SPULE = simple("flux_spule");
    public static final DeferredItem<Item> RIFT_SCHALTKREIS = simple("rift_schaltkreis");
    public static final DeferredItem<Item> STABILISIERTE_LINSE = simple("stabilisierte_linse");
    public static final DeferredItem<Item> KANONENKERN = simple("kanonenkern");
    public static final DeferredItem<Item> CHRONOGLAS_GRIFF = simple("chronoglas_griff");
    public static final DeferredItem<Item> CHRONOGLAS_SCHEIBE = simple("chronoglas_scheibe");
    public static final DeferredItem<Item> ANTIGRAV_PLATTE = simple("antigrav_platte");
    public static final DeferredItem<Item> AETZLOESUNG = simple("aetzloesung");
    public static final DeferredItem<Item> TOXINREST = simple("toxinrest");
    public static final DeferredItem<Item> FLUX_STABILISATOR = ITEMS.register("flux_stabilisator",
            () -> new LoreItem(new Item.Properties(), "item.riftverse.flux_stabilisator.lore"));
    public static final DeferredItem<Item> NAEHRSERUM = ITEMS.register("naehrserum",
            () -> new LoreItem(new Item.Properties(), "item.riftverse.naehrserum.lore"));
    public static final DeferredItem<Item> FLUXKRISTALL_STAB = ITEMS.register("fluxkristall_stab",
            () -> new Item(new Item.Properties().durability(20_000)));
    public static final DeferredItem<Item> KUEHLKOERPER = simple("kuehlkoerper");

    // --- Rohstoffe aus den T2-Universen ---
    public static final DeferredItem<BlockItem> SCHWEBSTEIN = blockItem("schwebstein", ModBlocks.SCHWEBSTEIN);
    public static final DeferredItem<Item> SCHWEBSTEIN_BROCKEN = simple("schwebstein_brocken");
    public static final DeferredItem<BlockItem> CHRONOGLAS_SAND = blockItem("chronoglas_sand", ModBlocks.CHRONOGLAS_SAND);
    public static final DeferredItem<BlockItem> CHRONOGLAS = blockItem("chronoglas", ModBlocks.CHRONOGLAS);
    public static final DeferredItem<BlockItem> BIOMASSE_BLOCK = blockItem("biomasse_block", ModBlocks.BIOMASSE_BLOCK);
    public static final DeferredItem<Item> BIOMASSE_PULPE = simple("biomasse_pulpe");
    public static final DeferredItem<BlockItem> FRESSENDE_FLORA = blockItem("fressende_flora", ModBlocks.FRESSENDE_FLORA);

    // =====================================================================
    // Meilenstein 4 – T3 Multiversum-Industrie
    // =====================================================================

    // --- Netz, Speicher, Gehäuse ---
    public static final DeferredItem<BlockItem> GRAVITIT_LEITUNG = blockItem("gravitit_leitung", ModBlocks.GRAVITIT_LEITUNG);
    public static final DeferredItem<BlockItem> GRAVITIT_LEITUNG_ISOLIERT = blockItem("gravitit_leitung_isoliert", ModBlocks.GRAVITIT_LEITUNG_ISOLIERT);
    public static final DeferredItem<BlockItem> GRAVITIT_LEITUNG_DOPPELT = blockItem("gravitit_leitung_doppelt", ModBlocks.GRAVITIT_LEITUNG_DOPPELT);
    public static final DeferredItem<BlockItem> UMSPANNER_II = blockItem("umspanner_ii", ModBlocks.UMSPANNER_II);
    public static final DeferredItem<BlockItem> UMSPANNER_III = blockItem("umspanner_iii", ModBlocks.UMSPANNER_III);
    public static final DeferredItem<XenitZelleItem> CHRONO_SPEICHER = ITEMS.register("chrono_speicher",
            () -> new XenitZelleItem(ModBlocks.CHRONO_SPEICHER.get(), new Item.Properties().stacksTo(1).rarity(Rarity.RARE),
                    ChronoSpeicherBlockEntity.CHRONO_CAPACITY, "tooltip.riftverse.chrono_speicher"));
    public static final DeferredItem<BlockItem> GRAPHIT_GEHAEUSE = blockItem("graphit_gehaeuse", ModBlocks.GRAPHIT_GEHAEUSE);
    public static final DeferredItem<BlockItem> GRAPHIT_HUELLE = blockItem("graphit_huelle", ModBlocks.GRAPHIT_HUELLE);

    // --- Maschinen und Generatoren ---
    public static final DeferredItem<BlockItem> QUANTEN_LITHOGRAF = blockItem("quanten_lithograf", ModBlocks.QUANTEN_LITHOGRAF);
    public static final DeferredItem<BlockItem> MATERIE_REKONFIGURATOR = blockItem("materie_rekonfigurator", ModBlocks.MATERIE_REKONFIGURATOR);
    public static final DeferredItem<BlockItem> KLON_TANK = ITEMS.register("klon_tank",
            () -> new DoubleHighBlockItem(ModBlocks.KLON_TANK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> UNIVERSUMS_DESTILLATOR = blockItem("universums_destillator", ModBlocks.UNIVERSUMS_DESTILLATOR);
    public static final DeferredItem<BlockItem> RIFT_KABELKNOTEN = blockItem("rift_kabelknoten", ModBlocks.RIFT_KABELKNOTEN);
    public static final DeferredItem<BlockItem> MIKROKOSMOS_ZELLE = blockItem("mikrokosmos_zelle", ModBlocks.MIKROKOSMOS_ZELLE);
    public static final DeferredItem<BlockItem> PARALLELZAPFER = blockItem("parallelzapfer", ModBlocks.PARALLELZAPFER);

    // --- Kanone, Fluid, Werkzeuge ---
    public static final DeferredItem<PortalKanoneItem> PORTAL_KANONE_MK3 = ITEMS.register("portal_kanone_mk3",
            () -> new PortalKanoneItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 3));
    public static final DeferredItem<Item> FLUID_KARTUSCHE_KONZENTRAT = ITEMS.register("fluid_kartusche_konzentrat",
            () -> new LoreItem(new Item.Properties().stacksTo(16), "item.riftverse.fluid_kartusche_konzentrat.lore"));
    public static final DeferredItem<Item> KOPPLUNGSKARTE = ITEMS.register("kopplungskarte",
            () -> new LoreItem(new Item.Properties().stacksTo(1), "item.riftverse.kopplungskarte.lore"));

    // --- Zwischenprodukte ---
    public static final DeferredItem<Item> QUANTEN_SCHALTKREIS = simple("quanten_schaltkreis");
    public static final DeferredItem<Item> CHRONO_LINSE = simple("chrono_linse");
    public static final DeferredItem<Item> GRAVITIT_SPULE = simple("gravitit_spule");
    public static final DeferredItem<Item> GRAVITIT_RAHMEN = simple("gravitit_rahmen");
    public static final DeferredItem<Item> GRAPHITPLATTE = simple("graphitplatte");
    /** Ersatz für Story-Fragmente bis zum Story-Modus (Tag {@code riftverse:story_fragmente}). */
    public static final DeferredItem<Item> FRAGMENT_REPLIK = ITEMS.register("fragment_replik",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.UNCOMMON), "item.riftverse.fragment_replik.lore"));

    // --- Rohstoffe aus den T3-Universen ---
    public static final DeferredItem<BlockItem> GRAVITIT_ORE = blockItem("gravitit_ore", ModBlocks.GRAVITIT_ORE);
    public static final DeferredItem<Item> GRAVITIT = ITEMS.register("gravitit",
            () -> new LoreItem(new Item.Properties(), "item.riftverse.gravitit.lore"));
    public static final DeferredItem<BlockItem> SPIEGELKRISTALL = blockItem("spiegelkristall", ModBlocks.SPIEGELKRISTALL);
    public static final DeferredItem<Item> INVERSIONSSPLITTER = ITEMS.register("inversionssplitter",
            () -> new LoreItem(new Item.Properties(), "item.riftverse.inversionssplitter.lore"));

    // =====================================================================
    // Meilenstein 5 – T4 Weltenformer
    // =====================================================================

    // --- Netz, Gehäuse, Maschinen ---
    public static final DeferredItem<BlockItem> NULL_LEITUNG = blockItem("null_leitung", ModBlocks.NULL_LEITUNG);
    public static final DeferredItem<BlockItem> NULL_GEHAEUSE = blockItem("null_gehaeuse", ModBlocks.NULL_GEHAEUSE);
    public static final DeferredItem<BlockItem> NULL_HUELLE = blockItem("null_huelle", ModBlocks.NULL_HUELLE);
    public static final DeferredItem<BlockItem> EINDAEMMUNGSRING = blockItem("eindaemmungsring", ModBlocks.EINDAEMMUNGSRING);
    public static final DeferredItem<BlockItem> RIFT_SCHMIEDE = blockItem("rift_schmiede", ModBlocks.RIFT_SCHMIEDE);
    public static final DeferredItem<BlockItem> WELTENFORMER = ITEMS.register("weltenformer",
            () -> new BlockItem(ModBlocks.WELTENFORMER.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> SINGULARITAETSKERN = ITEMS.register("singularitaetskern",
            () -> new BlockItem(ModBlocks.SINGULARITAETSKERN.get(), new Item.Properties().rarity(Rarity.EPIC)));

    // --- Kanone und Fluid ---
    public static final DeferredItem<PortalKanoneItem> PORTAL_KANONE_MK4 = ITEMS.register("portal_kanone_mk4",
            () -> new PortalKanoneItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), 4));
    public static final DeferredItem<Item> FLUID_KARTUSCHE_ESSENZ = ITEMS.register("fluid_kartusche_essenz",
            () -> new LoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), "item.riftverse.fluid_kartusche_essenz.lore"));

    // --- Zwischenprodukte der T4-Leitern ---
    public static final DeferredItem<Item> MULTIVERSAL_PROZESSOR = ITEMS.register("multiversal_prozessor",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> SINGULARITAETSLINSE = ITEMS.register("singularitaetslinse",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> NULL_SPULE = ITEMS.register("null_spule",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> NULLMATERIE_FASER = simple("nullmaterie_faser");
    public static final DeferredItem<Item> NULLMATERIE_KERN = ITEMS.register("nullmaterie_kern",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.EPIC), "item.riftverse.nullmaterie_kern.lore"));

    // --- Rohstoffe aus der Leeren Welt ---
    public static final DeferredItem<Item> NULLMATERIE = ITEMS.register("nullmaterie",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.RARE).fireResistant(), "item.riftverse.nullmaterie.lore"));
    public static final DeferredItem<BlockItem> NULLMATERIE_ERZ = blockItem("nullmaterie_erz", ModBlocks.NULLMATERIE_ERZ);
    public static final DeferredItem<BlockItem> NULLMATERIE_BLOCK = ITEMS.register("nullmaterie_block",
            () -> new BlockItem(ModBlocks.NULLMATERIE_BLOCK.get(), new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    public static final DeferredItem<BlockItem> LEERSTEIN = blockItem("leerstein", ModBlocks.LEERSTEIN);

    // =====================================================================
    // Meilenstein 6 – Gadgets und Rüstung
    // =====================================================================

    // --- Gadgets ---
    public static final DeferredItem<dev.riftverse.gadget.EinzweckDrohneItem> EINZWECK_DROHNE = ITEMS.register("einzweck_drohne",
            () -> new dev.riftverse.gadget.EinzweckDrohneItem(new Item.Properties()));
    public static final DeferredItem<dev.riftverse.gadget.HoloKoederItem> HOLO_KOEDER = ITEMS.register("holo_koeder",
            () -> new dev.riftverse.gadget.HoloKoederItem(new Item.Properties()));
    public static final DeferredItem<dev.riftverse.gadget.SchrumpfstrahlerItem> SCHRUMPFSTRAHLER = ITEMS.register("schrumpfstrahler",
            () -> new dev.riftverse.gadget.SchrumpfstrahlerItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<dev.riftverse.gadget.AntiGravGranateItem> ANTI_GRAV_GRANATE = ITEMS.register("anti_grav_granate",
            () -> new dev.riftverse.gadget.AntiGravGranateItem(new Item.Properties()));
    public static final DeferredItem<dev.riftverse.gadget.UebersetzerHalsbandItem> UEBERSETZER_HALSBAND = ITEMS.register("uebersetzer_halsband",
            () -> new dev.riftverse.gadget.UebersetzerHalsbandItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<dev.riftverse.gadget.ErinnerungsphioleItem> ERINNERUNGSPHIOLE = ITEMS.register("erinnerungsphiole",
            () -> new dev.riftverse.gadget.ErinnerungsphioleItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<dev.riftverse.gadget.TarnDetektorItem> TARN_DETEKTOR = ITEMS.register("tarn_detektor",
            () -> new dev.riftverse.gadget.TarnDetektorItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<dev.riftverse.gadget.MultitoolItem> MULTIVERSAL_MULTITOOL = ITEMS.register("multiversal_multitool",
            () -> new dev.riftverse.gadget.MultitoolItem(new Item.Properties().rarity(Rarity.UNCOMMON), 1));
    public static final DeferredItem<dev.riftverse.gadget.MultitoolItem> MULTIVERSAL_MULTITOOL_MK2 = ITEMS.register("multiversal_multitool_mk2",
            () -> new dev.riftverse.gadget.MultitoolItem(new Item.Properties().rarity(Rarity.RARE), 2));
    public static final DeferredItem<dev.riftverse.gadget.MultitoolItem> MULTIVERSAL_MULTITOOL_MK3 = ITEMS.register("multiversal_multitool_mk3",
            () -> new dev.riftverse.gadget.MultitoolItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant(), 3));
    public static final DeferredItem<dev.riftverse.gadget.ZeitstopperItem> ZEITSTOPPER = ITEMS.register("zeitstopper",
            () -> new dev.riftverse.gadget.ZeitstopperItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<dev.riftverse.gadget.MutagenSpritzeItem> MUTAGEN_SPRITZE = ITEMS.register("mutagen_spritze",
            () -> new dev.riftverse.gadget.MutagenSpritzeItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<dev.riftverse.gadget.AufgabenBoxItem> AUFGABEN_BOX = ITEMS.register("aufgaben_box",
            () -> new dev.riftverse.gadget.AufgabenBoxItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // --- Maschinen-Upgrades ---
    public static final java.util.Map<dev.riftverse.tech.upgrade.UpgradeChip, DeferredItem<dev.riftverse.tech.upgrade.UpgradeChipItem>> CHIPS =
            chips();

    // --- Multiversal-Rüstung: vier Teile in drei Ausbaustufen ---
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> ANALYSE_VISIER_ROHLING = armor("analyse_visier_rohling",
            net.minecraft.world.item.ArmorItem.Type.HELMET, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.ROHLING);
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> KERNMANTEL_ROHLING = armor("kernmantel_rohling",
            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.ROHLING);
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> EXO_RAHMEN_ROHLING = armor("exo_rahmen_rohling",
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.ROHLING);
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> GRAVI_STIEFEL_ROHLING = armor("gravi_stiefel_rohling",
            net.minecraft.world.item.ArmorItem.Type.BOOTS, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.ROHLING);
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> ANALYSE_VISIER_KALIBRIERT = armor("analyse_visier_kalibriert",
            net.minecraft.world.item.ArmorItem.Type.HELMET, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.KALIBRIERT);
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> KERNMANTEL_KALIBRIERT = armor("kernmantel_kalibriert",
            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.KALIBRIERT);
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> EXO_RAHMEN_KALIBRIERT = armor("exo_rahmen_kalibriert",
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.KALIBRIERT);
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> GRAVI_STIEFEL_KALIBRIERT = armor("gravi_stiefel_kalibriert",
            net.minecraft.world.item.ArmorItem.Type.BOOTS, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.KALIBRIERT);
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> ANALYSE_VISIER_PERFEKTIONIERT = armor("analyse_visier_perfektioniert",
            net.minecraft.world.item.ArmorItem.Type.HELMET, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.PERFEKTIONIERT);
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> KERNMANTEL_PERFEKTIONIERT = armor("kernmantel_perfektioniert",
            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.PERFEKTIONIERT);
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> EXO_RAHMEN_PERFEKTIONIERT = armor("exo_rahmen_perfektioniert",
            net.minecraft.world.item.ArmorItem.Type.LEGGINGS, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.PERFEKTIONIERT);
    public static final DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> GRAVI_STIEFEL_PERFEKTIONIERT = armor("gravi_stiefel_perfektioniert",
            net.minecraft.world.item.ArmorItem.Type.BOOTS, dev.riftverse.ruestung.MultiversalArmorItem.Ausbau.PERFEKTIONIERT);

    // --- Module ---
    public static final java.util.Map<dev.riftverse.ruestung.Modul, DeferredItem<dev.riftverse.ruestung.ModulItem>> MODULE = module();

    // --- Zwischenprodukte der Rüstung ---
    /** Material aus drei Universumstypen und ein Story-Fragment, für die Kalibrierung am Quanten-Lithografen. */
    public static final DeferredItem<Item> KALIBRIER_SATZ = ITEMS.register("kalibrier_satz",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.UNCOMMON), "item.riftverse.kalibrier_satz.lore"));
    /** Material aus sechs Universumstypen, für die Perfektionierung an der Rift-Schmiede. */
    public static final DeferredItem<Item> MULTIVERSUM_LEGIERUNG = ITEMS.register("multiversum_legierung",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.RARE), "item.riftverse.multiversum_legierung.lore"));

    public static Item modul(dev.riftverse.ruestung.Modul m) {
        return MODULE.get(m).get();
    }

    public static Item chip(dev.riftverse.tech.upgrade.UpgradeChip c) {
        return CHIPS.get(c).get();
    }

    private static java.util.Map<dev.riftverse.tech.upgrade.UpgradeChip, DeferredItem<dev.riftverse.tech.upgrade.UpgradeChipItem>> chips() {
        java.util.EnumMap<dev.riftverse.tech.upgrade.UpgradeChip, DeferredItem<dev.riftverse.tech.upgrade.UpgradeChipItem>> map =
                new java.util.EnumMap<>(dev.riftverse.tech.upgrade.UpgradeChip.class);
        for (dev.riftverse.tech.upgrade.UpgradeChip c : dev.riftverse.tech.upgrade.UpgradeChip.values()) {
            map.put(c, ITEMS.register(c.itemName(), () -> new dev.riftverse.tech.upgrade.UpgradeChipItem(
                    new Item.Properties().rarity(c == dev.riftverse.tech.upgrade.UpgradeChip.KI ? Rarity.RARE : Rarity.COMMON), c)));
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    private static java.util.Map<dev.riftverse.ruestung.Modul, DeferredItem<dev.riftverse.ruestung.ModulItem>> module() {
        java.util.EnumMap<dev.riftverse.ruestung.Modul, DeferredItem<dev.riftverse.ruestung.ModulItem>> map =
                new java.util.EnumMap<>(dev.riftverse.ruestung.Modul.class);
        for (dev.riftverse.ruestung.Modul m : dev.riftverse.ruestung.Modul.values()) {
            map.put(m, ITEMS.register(m.itemName(), () -> new dev.riftverse.ruestung.ModulItem(new Item.Properties().stacksTo(1)
                    .rarity(m == dev.riftverse.ruestung.Modul.UNIVERSUMS_KERN ? Rarity.EPIC : Rarity.UNCOMMON), m)));
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    private static DeferredItem<dev.riftverse.ruestung.MultiversalArmorItem> armor(String name, net.minecraft.world.item.ArmorItem.Type type,
                                                                                 dev.riftverse.ruestung.MultiversalArmorItem.Ausbau ausbau) {
        var material = switch (ausbau) {
            case ROHLING -> ModArmorMaterials.ROHLING;
            case KALIBRIERT -> ModArmorMaterials.KALIBRIERT;
            case PERFEKTIONIERT -> ModArmorMaterials.PERFEKTIONIERT;
        };
        return ITEMS.register(name, () -> new dev.riftverse.ruestung.MultiversalArmorItem(material, type, ausbau));
    }

    private static DeferredItem<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<BlockItem> blockItem(String name, DeferredBlock<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private ModItems() {}
}
