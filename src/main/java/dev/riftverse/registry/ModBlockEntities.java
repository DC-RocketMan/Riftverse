package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.machine.MachineBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Riftverse.MODID);

    /** Eine gemeinsame BlockEntity für alle drei Maschinen; der Maschinentyp kommt vom Block. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> MACHINE =
            BLOCK_ENTITIES.register("machine", () -> BlockEntityType.Builder.of(MachineBlockEntity::new,
                    ModBlocks.QUANTUM_REFINER.get(),
                    ModBlocks.FLUID_SYNTHESIZER.get(),
                    ModBlocks.RIFT_ASSEMBLER.get()).build(null));

    /** Trichter, Rohre usw. aus anderen Mods können die Maschinen befüllen und leeren. */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACHINE.get(),
                (machine, side) -> machine.getItemHandlerForSide(side));
    }

    private ModBlockEntities() {}
}
