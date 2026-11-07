package dev.riftverse.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftverse.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;

/**
 * Rezept des Fluid-Mixers: Items plus Flüssigkeit aus dem Eingangstank ergeben eine neue Flüssigkeit.
 *
 * <pre>{@code
 * {
 *   "type": "riftverse:fluid_mixer",
 *   "ingredients": [{"ingredient": {"item": "riftverse:fluxstaub"}}],
 *   "fluid": {"fluid": "minecraft:water", "amount": 1000},
 *   "result": {"id": "riftverse:roh_portalfluid", "amount": 1000},
 *   "time": 200
 * }
 * }</pre>
 */
public class FluidMixerRecipe implements Recipe<FluidMixerRecipe.Input> {

    /** Eingangsslots und Inhalt des Eingangstanks. */
    public record Input(List<ItemStack> stacks, FluidStack fluid) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return stacks.get(index);
        }

        @Override
        public int size() {
            return stacks.size();
        }
    }

    private final List<CountedIngredient> ingredients;
    private final SizedFluidIngredient fluid;
    private final FluidStack result;
    private final int time;

    public FluidMixerRecipe(List<CountedIngredient> ingredients, SizedFluidIngredient fluid, FluidStack result, int time) {
        this.ingredients = List.copyOf(ingredients);
        this.fluid = fluid;
        this.result = result;
        this.time = time;
    }

    public List<CountedIngredient> ingredients() {
        return ingredients;
    }

    public SizedFluidIngredient fluid() {
        return fluid;
    }

    public FluidStack result() {
        return result;
    }

    public int time() {
        return time;
    }

    public int[] assignment(Input input) {
        return IngredientMatcher.match(ingredients, input.stacks(), CountedIngredient::test);
    }

    @Override
    public boolean matches(Input input, Level level) {
        return fluid.test(input.fluid()) && assignment(input) != null;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        ingredients.forEach(i -> list.add(i.ingredient()));
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FLUID_MIXER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FLUID_MIXER_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<FluidMixerRecipe> {
        private static final MapCodec<FluidMixerRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                CountedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(FluidMixerRecipe::ingredients),
                SizedFluidIngredient.FLAT_CODEC.fieldOf("fluid").forGetter(FluidMixerRecipe::fluid),
                FluidStack.CODEC.fieldOf("result").forGetter(FluidMixerRecipe::result),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("time", 200).forGetter(FluidMixerRecipe::time)
        ).apply(i, FluidMixerRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidMixerRecipe> STREAM_CODEC = StreamCodec.composite(
                CountedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), FluidMixerRecipe::ingredients,
                SizedFluidIngredient.STREAM_CODEC, FluidMixerRecipe::fluid,
                FluidStack.STREAM_CODEC, FluidMixerRecipe::result,
                ByteBufCodecs.VAR_INT, FluidMixerRecipe::time,
                FluidMixerRecipe::new);

        @Override
        public MapCodec<FluidMixerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FluidMixerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
