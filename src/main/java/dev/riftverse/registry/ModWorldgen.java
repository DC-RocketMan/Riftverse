package dev.riftverse.registry;

import com.mojang.serialization.MapCodec;
import dev.riftverse.Riftverse;
import dev.riftverse.universe.MultiverseChunkGenerator;
import dev.riftverse.universe.UniverseBiomeSource;
import dev.riftverse.worldgen.AlienWrackFeature;
import dev.riftverse.worldgen.KraterPiece;
import dev.riftverse.worldgen.KraterZonePlacement;
import dev.riftverse.worldgen.MeteoritenkraterStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Riftverse.MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Riftverse.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Riftverse.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Riftverse.MODID);
    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Riftverse.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Riftverse.MODID);

    // --- Universen ---
    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<MultiverseChunkGenerator>> MULTIVERSE =
            CHUNK_GENERATORS.register("multiverse", () -> MultiverseChunkGenerator.CODEC);
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<UniverseBiomeSource>> UNIVERSE =
            BIOME_SOURCES.register("universe", () -> UniverseBiomeSource.CODEC);

    // --- Meteoritenkrater und Xenit-Zone ---
    public static final DeferredHolder<StructureType<?>, StructureType<MeteoritenkraterStructure>> METEORITENKRATER =
            STRUCTURE_TYPES.register("meteoritenkrater", () -> () -> MeteoritenkraterStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> KRATER_PIECE =
            STRUCTURE_PIECES.register("krater", () -> (StructurePieceType.ContextlessType) KraterPiece::new);
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<KraterZonePlacement>> KRATER_ZONE =
            PLACEMENT_MODIFIERS.register("krater_zone", () -> () -> KraterZonePlacement.CODEC);

    // --- Alien-Wracks ---
    public static final DeferredHolder<Feature<?>, AlienWrackFeature> ALIEN_WRACK =
            FEATURES.register("alien_wrack", AlienWrackFeature::new);

    private ModWorldgen() {}
}
