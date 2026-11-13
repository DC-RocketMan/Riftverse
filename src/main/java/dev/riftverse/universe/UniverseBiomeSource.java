package dev.riftverse.universe;

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
 * Jedes Universum bekommt genau ein Biom (Himmels-, Nebel-, Wasser- und Grasfarben, Spawns)
 * aus der Liste seines Typs – passend zum Universums-Seed. Gezüchtete Universen nehmen ein Biom ihrer Landschaft.
 */
public class UniverseBiomeSource extends BiomeSource {
    public static final MapCodec<UniverseBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(src -> src.biomes),
            UniverseType.CODEC.fieldOf("universe").forGetter(src -> src.type)
    ).apply(instance, UniverseBiomeSource::new));

    private final HolderSet<Biome> biomes;
    private final UniverseType type;

    public UniverseBiomeSource(HolderSet<Biome> biomes, UniverseType type) {
        this.biomes = biomes;
        this.type = type;
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
        UniverseId id = new UniverseId(type, Multiverse.cellOf(x), Multiverse.cellOf(z));
        UniverseProfile p = UniverseProfile.get(id);
        int index = Math.floorMod(p.biomeRoll, biomes.size());
        // Gezüchtete Universen: zwei Biome je Grundtyp, in der Reihenfolge von Zucht.BASIS
        if (p.zucht != null && biomes.size() >= Zucht.BASIS.size() * 2) {
            index = Zucht.BASIS.indexOf(p.look) * 2 + Math.floorMod(p.biomeRoll, 2);
        }
        return biomes.get(index);
    }
}
