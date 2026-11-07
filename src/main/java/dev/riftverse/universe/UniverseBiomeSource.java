package dev.riftverse.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Jedes Universum bekommt genau ein Biom (Himmels-, Nebel-, Wasser- und Grasfarben)
 * aus der Liste der Dimension – passend zum Universums-Seed.
 */
public class UniverseBiomeSource extends BiomeSource {
    public static final MapCodec<UniverseBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(src -> src.biomes),
            Codec.INT.fieldOf("variant").forGetter(src -> src.variant)
    ).apply(instance, UniverseBiomeSource::new));

    private final HolderSet<Biome> biomes;
    private final int variant;

    public UniverseBiomeSource(HolderSet<Biome> biomes, int variant) {
        this.biomes = biomes;
        this.variant = variant;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int x = QuartPos.toBlock(quartX);
        int z = QuartPos.toBlock(quartZ);
        UniverseId id = new UniverseId(variant, Multiverse.cellOf(x), Multiverse.cellOf(z));
        int index = Math.floorMod(UniverseProfile.get(id).biomeRoll, biomes.size());
        return biomes.get(index);
    }
}
