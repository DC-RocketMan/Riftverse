package dev.riftverse.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftverse.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Rezept der Garagen-Werkbank: wie ein normales Crafting-Rezept, zusätzlich mit Werkzeugen.
 * <ul>
 *   <li>{@code "hammer": n} – braucht den Garagen-Hammer im Werkzeugslot, kostet n Haltbarkeit</li>
 *   <li>{@code "loetzinn": n} – braucht den Lötkolben (1 Haltbarkeit) und verbraucht n Lötzinn</li>
 * </ul>
 */
public class WerkbankRecipe implements Recipe<CraftingInput> {
    @Nullable
    private final ShapedRecipePattern pattern;
    private final List<Ingredient> shapeless;
    private final ItemStack result;
    private final int hammer;
    private final int loetzinn;

    private WerkbankRecipe(@Nullable ShapedRecipePattern pattern, List<Ingredient> shapeless, ItemStack result, int hammer, int loetzinn) {
        this.pattern = pattern;
        this.shapeless = List.copyOf(shapeless);
        this.result = result;
        this.hammer = hammer;
        this.loetzinn = loetzinn;
    }

    public static WerkbankRecipe shaped(ShapedRecipePattern pattern, ItemStack result, int hammer, int loetzinn) {
        return new WerkbankRecipe(pattern, List.of(), result, hammer, loetzinn);
    }

    public static WerkbankRecipe shapeless(List<Ingredient> ingredients, ItemStack result, int hammer, int loetzinn) {
        return new WerkbankRecipe(null, ingredients, result, hammer, loetzinn);
    }

    public int hammer() {
        return hammer;
    }

    public int loetzinn() {
        return loetzinn;
    }

    public ItemStack result() {
        return result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (pattern != null) return pattern.matches(input);
        return IngredientMatcher.match(shapeless, input.items(), Ingredient::test) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return pattern != null ? width >= pattern.width() && height >= pattern.height() : width * height >= shapeless.size();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        if (pattern != null) return pattern.ingredients();
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(shapeless);
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return pattern != null ? ModRecipes.WERKBANK_SHAPED.get() : ModRecipes.WERKBANK_SHAPELESS.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.WERKBANK_TYPE.get();
    }

    // ------------------------------------------------------------------ Serializer

    public static class ShapedSerializer implements RecipeSerializer<WerkbankRecipe> {
        private static final MapCodec<WerkbankRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("hammer", 0).forGetter(r -> r.hammer),
                Codec.INT.optionalFieldOf("loetzinn", 0).forGetter(r -> r.loetzinn)
        ).apply(i, WerkbankRecipe::shaped));

        private static final StreamCodec<RegistryFriendlyByteBuf, WerkbankRecipe> STREAM_CODEC = StreamCodec.composite(
                ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
                ItemStack.STREAM_CODEC, r -> r.result,
                ByteBufCodecs.VAR_INT, r -> r.hammer,
                ByteBufCodecs.VAR_INT, r -> r.loetzinn,
                WerkbankRecipe::shaped);

        @Override
        public MapCodec<WerkbankRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WerkbankRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class ShapelessSerializer implements RecipeSerializer<WerkbankRecipe> {
        private static final MapCodec<WerkbankRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(r -> r.shapeless),
                ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result),
                Codec.INT.optionalFieldOf("hammer", 0).forGetter(r -> r.hammer),
                Codec.INT.optionalFieldOf("loetzinn", 0).forGetter(r -> r.loetzinn)
        ).apply(i, WerkbankRecipe::shapeless));

        private static final StreamCodec<RegistryFriendlyByteBuf, WerkbankRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.shapeless,
                ItemStack.STREAM_CODEC, r -> r.result,
                ByteBufCodecs.VAR_INT, r -> r.hammer,
                ByteBufCodecs.VAR_INT, r -> r.loetzinn,
                WerkbankRecipe::shapeless);

        @Override
        public MapCodec<WerkbankRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WerkbankRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
