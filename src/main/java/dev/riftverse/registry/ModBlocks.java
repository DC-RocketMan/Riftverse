package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.cable.CableBlock;
import dev.riftverse.cable.InsulatedCableBlock;
import dev.riftverse.energy.EnergyPortBlock;
import dev.riftverse.energy.KreativQuelleBlock;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.block.KnospenderFluxkristallBlock;
import dev.riftverse.block.MeteoritenkernBlock;
import dev.riftverse.block.RiftAnkerBlock;
import dev.riftverse.block.SaeureBlock;
import dev.riftverse.tech.MachineKind;
import dev.riftverse.tech.ProcessingBlock;
import dev.riftverse.tech.TechBlock;
import dev.riftverse.tech.WerkbankBlock;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.ToIntFunction;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Riftverse.MODID);

    /** Leuchtender Baustein für Notfall-Plattformen beim Sprung (und später Boss-Arenen). */
    public static final DeferredBlock<Block> RIFT_STONE = BLOCKS.register("rift_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(2.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 11)
                    .sound(SoundType.AMETHYST)));

    // =====================================================================
    // Meilenstein 1 – Fundament
    // =====================================================================

    // --- Erze und Metallblöcke ---
    public static final DeferredBlock<Block> TIN_ORE = BLOCKS.register("tin_ore",
            () -> new DropExperienceBlock(ConstantInt.of(0), oreProps(MapColor.STONE, 3.0f, SoundType.STONE)));
    public static final DeferredBlock<Block> DEEPSLATE_TIN_ORE = BLOCKS.register("deepslate_tin_ore",
            () -> new DropExperienceBlock(ConstantInt.of(0), oreProps(MapColor.DEEPSLATE, 4.5f, SoundType.DEEPSLATE)));
    public static final DeferredBlock<Block> TIN_BLOCK = BLOCKS.register("tin_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));

    /** Xenit-Erz: rund um Meteoritenkrater im Eisen-Band, in der Schrottwelt in Wrackfeldern. */
    public static final DeferredBlock<Block> XENIT_ORE = BLOCKS.register("xenit_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3), oreProps(MapColor.COLOR_CYAN, 3.0f, SoundType.STONE)
                    .lightLevel(state -> 2)));
    public static final DeferredBlock<Block> DEEPSLATE_XENIT_ORE = BLOCKS.register("deepslate_xenit_ore",
            () -> new DropExperienceBlock(UniformInt.of(1, 3), oreProps(MapColor.DEEPSLATE, 4.5f, SoundType.DEEPSLATE)
                    .lightLevel(state -> 2)));
    public static final DeferredBlock<Block> XENIT_BLOCK = BLOCKS.register("xenit_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(5.0f, 6.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(state -> 3)));

    // --- Gehäuse (Grundzutat jeder Maschine) ---
    public static final DeferredBlock<Block> GARAGEN_GEHAEUSE = BLOCKS.register("garagen_gehaeuse",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD)));
    public static final DeferredBlock<Block> XENIT_GEHAEUSE = BLOCKS.register("xenit_gehaeuse",
            () -> new Block(techProps()));

    // --- T0 Garage ---
    public static final DeferredBlock<WerkbankBlock> GARAGEN_WERKBANK = BLOCKS.register("garagen_werkbank",
            () -> new WerkbankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD)));
    public static final DeferredBlock<ProcessingBlock> GARAGENPRESSE = BLOCKS.register("garagenpresse",
            () -> new ProcessingBlock(garageProps(), MachineKind.GARAGENPRESSE));
    public static final DeferredBlock<TechBlock> KURBEL_DYNAMO = BLOCKS.register("kurbel_dynamo",
            () -> new TechBlock(garageProps(), ModBlockEntities.KURBEL_DYNAMO, true));

    // --- T1 Bastlerwerkstatt ---
    public static final DeferredBlock<ProcessingBlock> ERZMUEHLE = BLOCKS.register("erzmuehle",
            () -> new ProcessingBlock(techProps().lightLevel(activeLight(6)), MachineKind.ERZMUEHLE));
    public static final DeferredBlock<ProcessingBlock> WALZBANK = BLOCKS.register("walzbank",
            () -> new ProcessingBlock(techProps().lightLevel(activeLight(6)), MachineKind.WALZBANK));
    public static final DeferredBlock<ProcessingBlock> LEGIERUNGSOFEN = BLOCKS.register("legierungsofen",
            () -> new ProcessingBlock(techProps().lightLevel(activeLight(12)), MachineKind.LEGIERUNGSOFEN));
    public static final DeferredBlock<TechBlock> TONNENREAKTOR = BLOCKS.register("tonnenreaktor",
            () -> new TechBlock(techProps().lightLevel(activeLight(9)), ModBlockEntities.TONNENREAKTOR, true));

    // --- Energienetz Stufe I ---
    public static final DeferredBlock<CableBlock> KUPFERKABEL = BLOCKS.register("kupferkabel",
            () -> new CableBlock(cableProps(SoundType.COPPER), VoltageTier.I, 0.5, 4, false));
    public static final DeferredBlock<InsulatedCableBlock> KUPFERKABEL_ISOLIERT = BLOCKS.register("kupferkabel_isoliert",
            () -> new InsulatedCableBlock(cableProps(SoundType.WOOL), VoltageTier.I, 0.5, 6));
    public static final DeferredBlock<EnergyPortBlock> EINSPEISER = BLOCKS.register("einspeiser",
            () -> new EnergyPortBlock(techProps(), true));
    public static final DeferredBlock<EnergyPortBlock> AUSSPEISER = BLOCKS.register("ausspeiser",
            () -> new EnergyPortBlock(techProps(), false));
    /** Nur Kreativmodus: unendliche Quelle mit wählbarer Spannungsstufe. */
    public static final DeferredBlock<KreativQuelleBlock> KREATIV_QUELLE = BLOCKS.register("kreativ_quelle",
            () -> new KreativQuelleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.AMETHYST)));

    // =====================================================================
    // Meilenstein 2 – Spielbarer Prototyp
    // =====================================================================

    // --- Krater, Wracks, Kristalle ---
    public static final DeferredBlock<MeteoritenkernBlock> METEORITENKERN = BLOCKS.register("meteoritenkern",
            () -> new MeteoritenkernBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(8.0f, 9.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.ANCIENT_DEBRIS).lightLevel(state -> 5)));
    public static final DeferredBlock<Block> SCHROTT_HAUFEN = BLOCKS.register("schrott_haufen",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.8f)
                    .sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<Block> FLUXKRISTALL_ORE = BLOCKS.register("fluxkristall_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), oreProps(MapColor.DEEPSLATE, 4.5f, SoundType.DEEPSLATE)
                    .lightLevel(state -> 4)));
    public static final DeferredBlock<Block> FLUXKRISTALL_BLOCK = BLOCKS.register("fluxkristall_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(1.5f)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(state -> 12)));
    public static final DeferredBlock<AmethystClusterBlock> FLUXKRISTALL_CLUSTER = BLOCKS.register("fluxkristall_cluster",
            () -> new AmethystClusterBlock(7.0f, 3.0f, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .forceSolidOn().noOcclusion().strength(1.5f).sound(SoundType.AMETHYST_CLUSTER).lightLevel(state -> 6)
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<KnospenderFluxkristallBlock> KNOSPENDER_FLUXKRISTALL = BLOCKS.register("knospender_fluxkristall",
            () -> new KnospenderFluxkristallBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .randomTicks().strength(1.5f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(state -> 8)
                    .pushReaction(PushReaction.DESTROY)));

    // --- Säure (toxische Welt) ---
    public static final DeferredBlock<SaeureBlock> SAEURE = BLOCKS.register("saeure",
            () -> new SaeureBlock(ModFluids.SAEURE.get(), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .replaceable().noCollission().strength(100.0f).pushReaction(PushReaction.DESTROY).noLootTable()
                    .liquid().sound(SoundType.EMPTY).lightLevel(state -> 3)));

    // --- Restliche T1-Maschinen ---
    public static final DeferredBlock<ProcessingBlock> XENIT_SCHMELZE = BLOCKS.register("xenit_schmelze",
            () -> new ProcessingBlock(techProps().lightLevel(activeLight(12)), MachineKind.XENIT_SCHMELZE));
    public static final DeferredBlock<ProcessingBlock> PLATINEN_AETZER = BLOCKS.register("platinen_aetzer",
            () -> new ProcessingBlock(techProps().lightLevel(activeLight(8)), MachineKind.PLATINEN_AETZER));
    public static final DeferredBlock<ProcessingBlock> KRISTALL_SCHLEIFER = BLOCKS.register("kristall_schleifer",
            () -> new ProcessingBlock(techProps().lightLevel(activeLight(6)), MachineKind.KRISTALL_SCHLEIFER));
    public static final DeferredBlock<ProcessingBlock> RECYCLER = BLOCKS.register("recycler",
            () -> new ProcessingBlock(techProps().lightLevel(activeLight(4)), MachineKind.RECYCLER));
    public static final DeferredBlock<TechBlock> FLUID_MIXER = BLOCKS.register("fluid_mixer",
            () -> new TechBlock(techProps().lightLevel(activeLight(9)), ModBlockEntities.FLUID_MIXER, true));
    public static final DeferredBlock<TechBlock> XENIT_ZELLE = BLOCKS.register("xenit_zelle",
            () -> new TechBlock(techProps(), ModBlockEntities.XENIT_ZELLE, true));
    public static final DeferredBlock<RiftAnkerBlock> RIFT_ANKER = BLOCKS.register("rift_anker",
            () -> new RiftAnkerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion().lightLevel(state -> 7)
                    .pushReaction(PushReaction.BLOCK)));

    // ---------------------------------------------------------------------

    private static BlockBehaviour.Properties oreProps(MapColor color, float hardness, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(hardness, 3.0f)
                .requiresCorrectToolForDrops().sound(sound);
    }

    /** T1-Technik: Xenit-Petrol, braucht eine Spitzhacke. */
    private static BlockBehaviour.Properties techProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.5f, 6.0f)
                .requiresCorrectToolForDrops().sound(SoundType.METAL);
    }

    /** T0-Garage: Holz, Rost und Klebeband, geht mit der Hand ab. */
    private static BlockBehaviour.Properties garageProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f, 4.0f).sound(SoundType.WOOD);
    }

    private static BlockBehaviour.Properties cableProps(SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.4f).sound(sound)
                .noOcclusion().pushReaction(PushReaction.NORMAL);
    }

    private static ToIntFunction<BlockState> activeLight(int level) {
        return state -> state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) ? level : 0;
    }

    private ModBlocks() {}
}
