package dev.riftverse.registry;

import dev.riftverse.Riftverse;
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

    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> ERZMUEHLE =
            SERIALIZERS.register("erzmuehle", () -> new MachineRecipeSerializer(MachineKind.ERZMUEHLE));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> WALZBANK =
            SERIALIZERS.register("walzbank", () -> new MachineRecipeSerializer(MachineKind.WALZBANK));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> LEGIERUNGSOFEN =
            SERIALIZERS.register("legierungsofen", () -> new MachineRecipeSerializer(MachineKind.LEGIERUNGSOFEN));
    public static final DeferredHolder<RecipeSerializer<?>, MachineRecipeSerializer> GARAGENPRESSE =
            SERIALIZERS.register("garagenpresse", () -> new MachineRecipeSerializer(MachineKind.GARAGENPRESSE));
    public static final DeferredHolder<RecipeSerializer<?>, WerkbankRecipe.ShapedSerializer> WERKBANK_SHAPED =
            SERIALIZERS.register("werkbank_shaped", WerkbankRecipe.ShapedSerializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, WerkbankRecipe.ShapelessSerializer> WERKBANK_SHAPELESS =
            SERIALIZERS.register("werkbank_shapeless", WerkbankRecipe.ShapelessSerializer::new);

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> type(String name) {
        return TYPES.register(name, () -> RecipeType.simple(Riftverse.id(name)));
    }

    private ModRecipes() {}
}
