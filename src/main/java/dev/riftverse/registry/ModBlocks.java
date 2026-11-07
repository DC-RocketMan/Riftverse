package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.cable.CableBlock;
import dev.riftverse.cable.InsulatedCableBlock;
import dev.riftverse.energy.EnergyPortBlock;
import dev.riftverse.energy.KreativQuelleBlock;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.machine.MachineBlock;
import dev.riftverse.machine.MachineType;
import dev.riftverse.tech.MachineKind;
import dev.riftverse.tech.ProcessingBlock;
import dev.riftverse.tech.TechBlock;
import dev.riftverse.tech.WerkbankBlock;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
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

    // =====================================================================
    // Vorhandener Prototyp (Quanten-Kette) – wird in Meilenstein 2 durch Xenit und Kanone Mk I ersetzt
    // =====================================================================

    /** Erz für Quanten-Splitter. Kommt in der Oberwelt tief im Stein und in Rift-Universen vor. */
    public static final DeferredBlock<Block> QUANTUM_ORE = BLOCKS.register("quantum_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 5)
                    .sound(SoundType.STONE)));

    /** Leuchtender Baustein – wird auch für Notfall-Plattformen und Boss-Arenen verwendet. */
    public static final DeferredBlock<Block> RIFT_STONE = BLOCKS.register("rift_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(2.0f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 11)
                    .sound(SoundType.AMETHYST)));

    public static final DeferredBlock<MachineBlock> QUANTUM_REFINER = BLOCKS.register("quantum_refiner",
            () -> new MachineBlock(machineProps(), MachineType.REFINER));
    public static final DeferredBlock<MachineBlock> FLUID_SYNTHESIZER = BLOCKS.register("fluid_synthesizer",
            () -> new MachineBlock(machineProps(), MachineType.SYNTHESIZER));
    public static final DeferredBlock<MachineBlock> RIFT_ASSEMBLER = BLOCKS.register("rift_assembler",
            () -> new MachineBlock(machineProps(), MachineType.ASSEMBLER));

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

    /** Xenit-Erz ist schon registriert; die Krater-Generierung folgt in Meilenstein 2. */
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

    // ---------------------------------------------------------------------

    private static BlockBehaviour.Properties machineProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5f, 6.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 12 : 0);
    }

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
