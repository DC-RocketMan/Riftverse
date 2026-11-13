package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.energy.EnergyItems;
import dev.riftverse.energy.EnergyPortBlockEntity;
import dev.riftverse.energy.KreativQuelleBlockEntity;
import dev.riftverse.energy.UmspannerBlockEntity;
import dev.riftverse.tech.BiomasseKesselBlockEntity;
import dev.riftverse.tech.ChronoSpeicherBlockEntity;
import dev.riftverse.tech.KlonTankBlockEntity;
import dev.riftverse.tech.MaterieRekonfiguratorBlockEntity;
import dev.riftverse.tech.MikrokosmosZelleBlockEntity;
import dev.riftverse.tech.ParallelzapferBlockEntity;
import dev.riftverse.tech.RiftKabelknotenBlockEntity;
import dev.riftverse.tech.UniversumsDestillatorBlockEntity;
import dev.riftverse.tech.ChronoBeschleunigerBlockEntity;
import dev.riftverse.tech.FluxZelleBlockEntity;
import dev.riftverse.tech.FluxkristallResonatorBlockEntity;
import dev.riftverse.tech.GravitationsPendelBlockEntity;
import dev.riftverse.tech.RaffinerieBlockEntity;
import dev.riftverse.tech.ZeitschleifenSolarBlockEntity;
import dev.riftverse.tech.FluidMixerBlockEntity;
import dev.riftverse.tech.KurbelDynamoBlockEntity;
import dev.riftverse.tech.ProcessingBlockEntity;
import dev.riftverse.tech.TonnenreaktorBlockEntity;
import dev.riftverse.tech.WerkbankBlockEntity;
import dev.riftverse.tech.XenitZelleBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Riftverse.MODID);

    /** Alle verarbeitenden Maschinen; was die Maschine kann, steht in MachineKind. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingBlockEntity>> PROCESSING =
            BLOCK_ENTITIES.register("processing", () -> BlockEntityType.Builder.of(ProcessingBlockEntity::new,
                    ModBlocks.ERZMUEHLE.get(),
                    ModBlocks.WALZBANK.get(),
                    ModBlocks.LEGIERUNGSOFEN.get(),
                    ModBlocks.GARAGENPRESSE.get(),
                    ModBlocks.XENIT_SCHMELZE.get(),
                    ModBlocks.PLATINEN_AETZER.get(),
                    ModBlocks.KRISTALL_SCHLEIFER.get(),
                    ModBlocks.RECYCLER.get(),
                    ModBlocks.PRAEZISIONS_MONTIERER.get(),
                    ModBlocks.QUANTENZENTRIFUGE.get(),
                    ModBlocks.GRAVITATIONSPRESSE.get(),
                    ModBlocks.DIMENSIONS_SCANNER.get(),
                    ModBlocks.QUANTEN_LITHOGRAF.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidMixerBlockEntity>> FLUID_MIXER =
            BLOCK_ENTITIES.register("fluid_mixer", () -> BlockEntityType.Builder.of(FluidMixerBlockEntity::new,
                    ModBlocks.FLUID_MIXER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<XenitZelleBlockEntity>> XENIT_ZELLE =
            BLOCK_ENTITIES.register("xenit_zelle", () -> BlockEntityType.Builder.of(XenitZelleBlockEntity::new,
                    ModBlocks.XENIT_ZELLE.get()).build(null));

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

    // --- Meilenstein 3 ---
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UmspannerBlockEntity>> UMSPANNER =
            BLOCK_ENTITIES.register("umspanner", () -> BlockEntityType.Builder.of(UmspannerBlockEntity::new,
                    ModBlocks.UMSPANNER.get(), ModBlocks.UMSPANNER_II.get(), ModBlocks.UMSPANNER_III.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluxZelleBlockEntity>> FLUX_ZELLE =
            BLOCK_ENTITIES.register("flux_zelle", () -> BlockEntityType.Builder.of(FluxZelleBlockEntity::new,
                    ModBlocks.FLUX_ZELLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChronoBeschleunigerBlockEntity>> CHRONO_BESCHLEUNIGER =
            BLOCK_ENTITIES.register("chrono_beschleuniger", () -> BlockEntityType.Builder.of(ChronoBeschleunigerBlockEntity::new,
                    ModBlocks.CHRONO_BESCHLEUNIGER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RaffinerieBlockEntity>> RAFFINERIE =
            BLOCK_ENTITIES.register("portalfluid_raffinerie", () -> BlockEntityType.Builder.of(RaffinerieBlockEntity::new,
                    ModBlocks.PORTALFLUID_RAFFINERIE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BiomasseKesselBlockEntity>> BIOMASSE_KESSEL =
            BLOCK_ENTITIES.register("biomasse_kessel", () -> BlockEntityType.Builder.of(BiomasseKesselBlockEntity::new,
                    ModBlocks.BIOMASSE_KESSEL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZeitschleifenSolarBlockEntity>> ZEITSCHLEIFEN_SOLAR =
            BLOCK_ENTITIES.register("zeitschleifen_solar", () -> BlockEntityType.Builder.of(ZeitschleifenSolarBlockEntity::new,
                    ModBlocks.ZEITSCHLEIFEN_SOLAR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluxkristallResonatorBlockEntity>> FLUXKRISTALL_RESONATOR =
            BLOCK_ENTITIES.register("fluxkristall_resonator", () -> BlockEntityType.Builder.of(FluxkristallResonatorBlockEntity::new,
                    ModBlocks.FLUXKRISTALL_RESONATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GravitationsPendelBlockEntity>> GRAVITATIONS_PENDEL =
            BLOCK_ENTITIES.register("gravitations_pendel", () -> BlockEntityType.Builder.of(GravitationsPendelBlockEntity::new,
                    ModBlocks.GRAVITATIONS_PENDEL.get()).build(null));

    // --- Meilenstein 4 ---
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChronoSpeicherBlockEntity>> CHRONO_SPEICHER =
            BLOCK_ENTITIES.register("chrono_speicher", () -> BlockEntityType.Builder.of(ChronoSpeicherBlockEntity::new,
                    ModBlocks.CHRONO_SPEICHER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaterieRekonfiguratorBlockEntity>> MATERIE_REKONFIGURATOR =
            BLOCK_ENTITIES.register("materie_rekonfigurator", () -> BlockEntityType.Builder.of(MaterieRekonfiguratorBlockEntity::new,
                    ModBlocks.MATERIE_REKONFIGURATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KlonTankBlockEntity>> KLON_TANK =
            BLOCK_ENTITIES.register("klon_tank", () -> BlockEntityType.Builder.of(KlonTankBlockEntity::new,
                    ModBlocks.KLON_TANK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UniversumsDestillatorBlockEntity>> UNIVERSUMS_DESTILLATOR =
            BLOCK_ENTITIES.register("universums_destillator", () -> BlockEntityType.Builder.of(UniversumsDestillatorBlockEntity::new,
                    ModBlocks.UNIVERSUMS_DESTILLATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RiftKabelknotenBlockEntity>> RIFT_KABELKNOTEN =
            BLOCK_ENTITIES.register("rift_kabelknoten", () -> BlockEntityType.Builder.of(RiftKabelknotenBlockEntity::new,
                    ModBlocks.RIFT_KABELKNOTEN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MikrokosmosZelleBlockEntity>> MIKROKOSMOS_ZELLE =
            BLOCK_ENTITIES.register("mikrokosmos_zelle", () -> BlockEntityType.Builder.of(MikrokosmosZelleBlockEntity::new,
                    ModBlocks.MIKROKOSMOS_ZELLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ParallelzapferBlockEntity>> PARALLELZAPFER =
            BLOCK_ENTITIES.register("parallelzapfer", () -> BlockEntityType.Builder.of(ParallelzapferBlockEntity::new,
                    ModBlocks.PARALLELZAPFER.get()).build(null));

    /**
     * Trichter und Rohre befüllen die Maschinen; fremde FE-Leitungen docken nur am Einspeiser und Ausspeiser an.
     * Maschinen und Generatoren zeigen ihren Füllstand nur lesbar an (für Jade, Integrated Dynamics).
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
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

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FLUID_MIXER.get(),
                (mixer, side) -> mixer.automationHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUID_MIXER.get(),
                (mixer, side) -> mixer.fluidHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FLUID_MIXER.get(),
                (mixer, side) -> mixer.energy().readOnly());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, XENIT_ZELLE.get(),
                (cell, side) -> cell.energy().readOnly());

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENERGY_PORT.get(),
                (port, side) -> port.externalStorage(side));

        // Meilenstein 3
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, UMSPANNER.get(), (u, side) -> u.energy().readOnly());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FLUX_ZELLE.get(), (cell, side) -> cell.energy().readOnly());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CHRONO_BESCHLEUNIGER.get(), (c, side) -> c.energy().readOnly());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RAFFINERIE.get(), (r, side) -> r.automationHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RAFFINERIE.get(), (r, side) -> r.fluidHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, RAFFINERIE.get(), (r, side) -> r.energy().readOnly());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BIOMASSE_KESSEL.get(), (k, side) -> k.automationHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BIOMASSE_KESSEL.get(), (k, side) -> k.energy().readOnly());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ZEITSCHLEIFEN_SOLAR.get(), (s, side) -> s.energy().readOnly());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, FLUXKRISTALL_RESONATOR.get(), (r, side) -> r.automationHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUXKRISTALL_RESONATOR.get(), (r, side) -> r.fluidHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, FLUXKRISTALL_RESONATOR.get(), (r, side) -> r.energy().readOnly());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, GRAVITATIONS_PENDEL.get(), (p, side) -> p.energy().readOnly());

        // Meilenstein 4
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CHRONO_SPEICHER.get(), (c, side) -> c.energy().readOnly());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MATERIE_REKONFIGURATOR.get(), (m, side) -> m.automationHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, MATERIE_REKONFIGURATOR.get(), (m, side) -> m.fluidHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MATERIE_REKONFIGURATOR.get(), (m, side) -> m.energy().readOnly());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, KLON_TANK.get(), (k, side) -> k.automationHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, KLON_TANK.get(), (k, side) -> k.energy().readOnly());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, UNIVERSUMS_DESTILLATOR.get(), (d, side) -> d.automationHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, UNIVERSUMS_DESTILLATOR.get(), (d, side) -> d.energy().readOnly());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RIFT_KABELKNOTEN.get(), (k, side) -> k.itemHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RIFT_KABELKNOTEN.get(), (k, side) -> k.fluidHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, RIFT_KABELKNOTEN.get(), (k, side) -> k.energy().readOnly());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MIKROKOSMOS_ZELLE.get(), (z, side) -> z.automationHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MIKROKOSMOS_ZELLE.get(), (z, side) -> z.energy().readOnly());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PARALLELZAPFER.get(), (p, side) -> p.automationHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PARALLELZAPFER.get(), (p, side) -> p.fluidHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PARALLELZAPFER.get(), (p, side) -> p.energy().readOnly());

        EnergyItems.register(event);
    }

    private ModBlockEntities() {}
}
