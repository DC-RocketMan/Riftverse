package dev.riftverse.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftverse.Config;
import dev.riftverse.registry.ModBiomeModifiers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Wie {@code neoforge:add_features}, aber per Config abschaltbar.
 * <pre>{@code
 * {"type": "riftverse:add_features_if", "biomes": "#minecraft:is_overworld",
 *  "features": "riftverse:ore_tin", "step": "underground_ores", "config": "tin_ore"}
 * }</pre>
 */
public record ConfiguredFeaturesModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features,
                                         GenerationStep.Decoration step, String config) implements BiomeModifier {

    public static final MapCodec<ConfiguredFeaturesModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfiguredFeaturesModifier::biomes),
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(ConfiguredFeaturesModifier::features),
            GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(ConfiguredFeaturesModifier::step),
            Codec.STRING.fieldOf("config").forGetter(ConfiguredFeaturesModifier::config)
    ).apply(i, ConfiguredFeaturesModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !biomes.contains(biome) || !Config.isEnabled(config)) return;
        features.forEach(feature -> builder.getGenerationSettings().addFeature(step, feature));
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return ModBiomeModifiers.ADD_FEATURES_IF.get();
    }
}
