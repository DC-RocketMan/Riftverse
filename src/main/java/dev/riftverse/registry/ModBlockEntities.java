package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.energy.EnergyItems;
import dev.riftverse.energy.EnergyPortBlockEntity;
import dev.riftverse.energy.KreativQuelleBlockEntity;
import dev.riftverse.machine.MachineBlockEntity;
import dev.riftverse.tech.KurbelDynamoBlockEntity;
import dev.riftverse.tech.ProcessingBlockEntity;
import dev.riftverse.tech.TonnenreaktorBlockEntity;
import dev.riftverse.tech.WerkbankBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Riftverse.MODID);

    /** Eine gemeinsame BlockEntity für alle drei Prototyp-Maschinen; der Maschinentyp kommt vom Block. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> MACHINE =
            BLOCK_ENTITIES.register("machine", () -> BlockEntityType.Builder.of(MachineBlockEntity::new,
                    ModBlocks.QUANTUM_REFINER.get(),
                    ModBlocks.FLUID_SYNTHESIZER.get(),
                    ModBlocks.RIFT_ASSEMBLER.get()).build(null));

    /** Erzmühle, Walzbank, Legierungsofen, Garagenpresse; was die Maschine kann, steht in MachineKind. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingBlockEntity>> PROCESSING =
            BLOCK_ENTITIES.register("processing", () -> BlockEntityType.Builder.of(ProcessingBlockEntity::new,
                    ModBlocks.ERZMUEHLE.get(),
                    ModBlocks.WALZBANK.get(),
                    ModBlocks.LEGIERUNGSOFEN.get(),
                    ModBlocks.GARAGENPRESSE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KurbelDynamoBlockEntity>> KURBEL_DYNAMO =
            BLOCK_ENTITIES.register("kurbel_dynamo", () -> BlockEntityType.Builder.of(KurbelDynamoBlockEntity::new,
                    ModBlocks.KURBEL_DYNAMO.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TonnenreaktorBlockEntity>> TONNENREAKTOR =
            BLOCK_ENTITIES.register("tonnenreaktor", () -> BlockEntityType.Builder.of(TonnenreaktorBlockEntity::new,
                    ModBlocks.TONNENREAKTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyPortBlockEntity>> ENERGY_PORT =
            BLOCK_ENTITIES.register("energy_port", () -> BlockEntityType.Builder.of(EnergyPortBlockEntity::new,
                    ModBlocks.EINSPEISER.get(),
                    ModBlocks.AUSSPEISER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KreativQuelleBlockEntity>> KREATIV_QUELLE =
            BLOCK_ENTITIES.register("kreativ_quelle", () -> BlockEntityType.Builder.of(KreativQuelleBlockEntity::new,
                    ModBlocks.KREATIV_QUELLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WerkbankBlockEntity>> WERKBANK =
            BLOCK_ENTITIES.register("garagen_werkbank", () -> BlockEntityType.Builder.of(WerkbankBlockEntity::new,
                    ModBlocks.GARAGEN_WERKBANK.get()).build(null));

    /**
     * Trichter und Rohre befüllen die Maschinen; fremde FE-Leitungen docken nur am Einspeiser und Ausspeiser an.
     * Maschinen und Generatoren zeigen ihren Füllstand nur lesbar an (für Jade, Integrated Dynamics).
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACHINE.get(),
                (machine, side) -> machine.getItemHandlerForSide(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PROCESSING.get(),
                (machine, side) -> machine.automationHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PROCESSING.get(),
                (machine, side) -> machine.kind().electric() ? machine.energy().readOnly() : null);

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TONNENREAKTOR.get(),
                (reactor, side) -> reactor.automationHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TONNENREAKTOR.get(),
                (reactor, side) -> reactor.energy().readOnly());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, KURBEL_DYNAMO.get(),
                (dynamo, side) -> dynamo.energy().readOnly());

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENERGY_PORT.get(),
                (port, side) -> port.externalStorage(side));

        EnergyItems.register(event);
    }

    private ModBlockEntities() {}
}
