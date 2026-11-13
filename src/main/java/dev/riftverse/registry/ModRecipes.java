package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.recipe.FluidMixerRecipe;
import dev.riftverse.recipe.MachineRecipe;
import dev.riftverse.recipe.MachineRecipeSerializer;
import dev.riftverse.recipe.WerkbankRecipe;
import dev.riftverse.tech.MachineKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Jede Maschine hat einen eigenen Rezepttyp (später je eine JEI/EMI-Kategorie). */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Riftverse.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Riftverse.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> ERZMUEHLE_TYPE = type("erzmuehle");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> WALZBANK_TYPE = type("walzbank");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> LEGIERUNGSOFEN_TYPE = type("legierungsofen");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> GARAGENPRESSE_TYPE = type("garagenpresse");
    public static final DeferredHolder<RecipeType<?>, RecipeType<WerkbankRecipe>> WERKBANK_TYPE = type("garagen_werkbank");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> XENIT_SCHMELZE_TYPE = type("xenit_schmelze");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> PLATINEN_AETZER_TYPE = type("platinen_aetzer");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> KRISTALL_SCHLEIFER_TYPE = type("kristall_schleifer");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> RECYCLER_TYPE = type("recycler");
    public static final DeferredHolder<RecipeType<?>, RecipeType<FluidMixerRecipe>> FLUID_MIXER_TYPE = type("fluid_mixer");
    // Meilenstein 3 – T2 Rift-Labor
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> PRAEZISIONS_MONTIERER_TYPE = type("praezisions_montierer");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> QUANTENZENTRIFUGE_TYPE = type("quantenzentrifuge");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> GRAVITATIONSPRESSE_TYPE = type("gravitationspresse");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> DIMENSIONS_SCANNER_TYPE = type("dimensions_scanner");
    public static final DeferredHolder<RecipeType<?>, RecipeType<FluidMixerRecipe>> RAFFINERIE_TYPE = type("raffinerie");
    // Meilenstein 4 – T3 Multiversum-Industrie
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> QUANTEN_LITHOGRAF_TYPE = type("quanten_lithograf");
    // Meilenstein 5 – T4 Weltenformer
    public static final DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>> RIFT_SCHMIEDE_TYPE = type("rift_schmiede");

    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> ERZMUEHLE =
            SERIALIZERS.register("erzmuehle", () -> new MachineRecipeSerializer(MachineKind.ERZMUEHLE));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> WALZBANK =
            SERIALIZERS.register("walzbank", () -> new MachineRecipeSerializer(MachineKind.WALZBANK));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> LEGIERUNGSOFEN =
            SERIALIZERS.register("legierungsofen", () -> new MachineRecipeSerializer(MachineKind.LEGIERUNGSOFEN));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> GARAGENPRESSE =
            SERIALIZERS.register("garagenpresse", () -> new MachineRecipeSerializer(MachineKind.GARAGENPRESSE));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> XENIT_SCHMELZE =
            SERIALIZERS.register("xenit_schmelze", () -> new MachineRecipeSerializer(MachineKind.XENIT_SCHMELZE));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> PLATINEN_AETZER =
            SERIALIZERS.register("platinen_aetzer", () -> new MachineRecipeSerializer(MachineKind.PLATINEN_AETZER));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> KRISTALL_SCHLEIFER =
            SERIALIZERS.register("kristall_schleifer", () -> new MachineRecipeSerializer(MachineKind.KRISTALL_SCHLEIFER));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> RECYCLER =
            SERIALIZERS.register("recycler", () -> new MachineRecipeSerializer(MachineKind.RECYCLER));
    public static final DeferredHolder<RecipeSerializer<?>, FluidMixerRecipe.Serializer> FLUID_MIXER =
            SERIALIZERS.register("fluid_mixer", () -> new FluidMixerRecipe.Serializer(FluidMixerRecipe.Machine.FLUID_MIXER));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> PRAEZISIONS_MONTIERER =
            SERIALIZERS.register("praezisions_montierer", () -> new MachineRecipeSerializer(MachineKind.PRAEZISIONS_MONTIERER));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> QUANTENZENTRIFUGE =
            SERIALIZERS.register("quantenzentrifuge", () -> new MachineRecipeSerializer(MachineKind.QUANTENZENTRIFUGE));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> GRAVITATIONSPRESSE =
            SERIALIZERS.register("gravitationspresse", () -> new MachineRecipeSerializer(MachineKind.GRAVITATIONSPRESSE));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> DIMENSIONS_SCANNER =
            SERIALIZERS.register("dimensions_scanner", () -> new MachineRecipeSerializer(MachineKind.DIMENSIONS_SCANNER));
    /** Portalfluid-Raffinerie: Roh-Portalfluid + Säuregel + Fluxstaub → stabilisiertes Portalfluid (gleiches Format wie der Fluid-Mixer). */
    public static final DeferredHolder<RecipeSerializer<?>, FluidMixerRecipe.Serializer> RAFFINERIE =
            SERIALIZERS.register("raffinerie", () -> new FluidMixerRecipe.Serializer(FluidMixerRecipe.Machine.RAFFINERIE));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> QUANTEN_LITHOGRAF =
            SERIALIZERS.register("quanten_lithograf", () -> new MachineRecipeSerializer(MachineKind.QUANTEN_LITHOGRAF));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> RIFT_SCHMIEDE =
            SERIALIZERS.register("rift_schmiede", () -> new MachineRecipeSerializer(MachineKind.RIFT_SCHMIEDE));
    public static final DeferredHolder<RecipeSerializer<?>, WerkbankRecipe.ShapedSerializer> WERKBANK_SHAPED =
            SERIALIZERS.register("werkbank_shaped", WerkbankRecipe.ShapedSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, WerkbankRecipe.ShapelessSerializer> WERKBANK_SHAPELESS =
            SERIALIZERS.register("werkbank_shapeless", WerkbankRecipe.ShapelessSerializer::new);

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> type(String name) {
        return TYPES.register(name, () -> RecipeType.simple(Riftverse.id(name)));
    }

    private ModRecipes() {}
}
