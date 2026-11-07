package dev.riftverse.registry;

import com.mojang.serialization.MapCodec;
import dev.riftverse.Riftverse;
import dev.riftverse.worldgen.ConfiguredFeaturesModifier;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModBiomeModifiers {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Riftverse.MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ConfiguredFeaturesModifier>> ADD_FEATURES_IF =
            SERIALIZERS.register("add_features_if", () -> ConfiguredFeaturesModifier.CODEC);

    private ModBiomeModifiers() {}
}
