package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Säure (Seen der toxischen Welt, mit Eimer und Block) und Roh-Portalfluid (nur in Tanks und Kartuschen;
 * zerfällt nach 10 Minuten, siehe {@link dev.riftverse.fluid.Zerfall}).
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Riftverse.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Riftverse.MODID);

    // --- Säure -----------------------------------------------------------------
    public static final DeferredHolder<FluidType, FluidType> SAEURE_TYPE = FLUID_TYPES.register("saeure",
            () -> new FluidType(FluidType.Properties.create()
                    .canSwim(true).canDrown(true).supportsBoating(true).canExtinguish(true)
                    .density(1300).viscosity(1600).lightLevel(3)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SAEURE = FLUIDS.register("saeure",
            () -> new BaseFlowingFluid.Source(saeureProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> SAEURE_FLOWING = FLUIDS.register("saeure_flowing",
            () -> new BaseFlowingFluid.Flowing(saeureProperties()));

    // --- Roh-Portalfluid -----------------------------------------------------------
    public static final DeferredHolder<FluidType, FluidType> ROH_PORTALFLUID_TYPE = FLUID_TYPES.register("roh_portalfluid",
            () -> new FluidType(FluidType.Properties.create().density(900).viscosity(1200).lightLevel(10)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> ROH_PORTALFLUID = FLUIDS.register("roh_portalfluid",
            () -> new BaseFlowingFluid.Source(rohProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> ROH_PORTALFLUID_FLOWING = FLUIDS.register("roh_portalfluid_flowing",
            () -> new BaseFlowingFluid.Flowing(rohProperties()));

    // --- Stabilisiertes Portalfluid (Raffinerie, zerfällt nicht) --------------------------
    public static final DeferredHolder<FluidType, FluidType> PORTALFLUID_TYPE = FLUID_TYPES.register("portalfluid",
            () -> new FluidType(FluidType.Properties.create().density(1000).viscosity(1500).lightLevel(12)));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> PORTALFLUID = FLUIDS.register("portalfluid",
            () -> new BaseFlowingFluid.Source(portalProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> PORTALFLUID_FLOWING = FLUIDS.register("portalfluid_flowing",
            () -> new BaseFlowingFluid.Flowing(portalProperties()));

    private static BaseFlowingFluid.Properties portalProperties() {
        return new BaseFlowingFluid.Properties(PORTALFLUID_TYPE, PORTALFLUID, PORTALFLUID_FLOWING);
    }

    private static BaseFlowingFluid.Properties saeureProperties() {
        return new BaseFlowingFluid.Properties(SAEURE_TYPE, SAEURE, SAEURE_FLOWING)
                .block(ModBlocks.SAEURE).bucket(ModItems.SAEURE_EIMER)
                .tickRate(10).slopeFindDistance(3).levelDecreasePerBlock(2);
    }

    private static BaseFlowingFluid.Properties rohProperties() {
        return new BaseFlowingFluid.Properties(ROH_PORTALFLUID_TYPE, ROH_PORTALFLUID, ROH_PORTALFLUID_FLOWING);
    }

    private ModFluids() {}
}
