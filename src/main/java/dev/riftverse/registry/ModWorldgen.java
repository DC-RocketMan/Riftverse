package dev.riftverse.registry;

import com.mojang.serialization.MapCodec;
import dev.riftverse.Riftverse;
import dev.riftverse.universe.MultiverseChunkGenerator;
import dev.riftverse.universe.UniverseBiomeSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Riftverse.MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Riftverse.MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<MultiverseChunkGenerator>> MULTIVERSE =
            CHUNK_GENERATORS.register("multiverse", () -> MultiverseChunkGenerator.CODEC);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<UniverseBiomeSource>> UNIVERSE =
            BIOME_SOURCES.register("universe", () -> UniverseBiomeSource.CODEC);

    private ModWorldgen() {}
}
