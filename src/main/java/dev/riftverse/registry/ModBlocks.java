package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.machine.MachineBlock;
import dev.riftverse.machine.MachineType;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Riftverse.MODID);

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

    private static BlockBehaviour.Properties machineProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5f, 6.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 12 : 0);
    }

    private ModBlocks() {}
}
