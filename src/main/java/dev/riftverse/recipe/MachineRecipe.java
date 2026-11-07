package dev.riftverse.recipe;

import dev.riftverse.tech.MachineKind;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Rezept einer verarbeitenden Maschine. Die Zutaten sind formlos und dürfen in beliebigen
 * Eingangsslots liegen.
 *
 * <pre>{@code
 * {
 *   "type": "riftverse:legierungsofen",
 *   "ingredients": [
 *     {"ingredient": {"tag": "c:ingots/copper"}, "count": 3},
 *     {"ingredient": {"tag": "c:ingots/tin"}}
 *   ],
 *   "result": {"id": "riftverse:bronze_ingot", "count": 4},
 *   "time": 200
 * }
 * }</pre>
 * Die Walzbank liest zusätzlich {@code "mode": "walzen" | "schneiden" | "ziehen"}.
 */
public class MachineRecipe implements Recipe<MachineRecipeInput> {
    private final MachineKind kind;
    private final List<CountedIngredient> ingredients;
    private final ItemStack result;
    private final int time;
    private final String mode;

    public MachineRecipe(MachineKind kind, List<CountedIngredient> ingredients, ItemStack result, int time, String mode) {
        this.kind = kind;
        this.ingredients = List.copyOf(ingredients);
        this.result = result;
        this.time = time;
        this.mode = mode;
    }

    public MachineKind kind() {
        return kind;
    }

    public List<CountedIngredient> ingredients() {
        return ingredients;
    }

    public ItemStack result() {
        return result;
    }

    /** Dauer in Ticks (bei der Garagenpresse: Kurbel-Ticks). */
    public int time() {
        return time;
    }

    public String mode() {
        return mode;
    }

    /** Slot je Zutat, oder null. */
    public int[] assignment(MachineRecipeInput input) {
        return IngredientMatcher.match(ingredients, input.stacks(), CountedIngredient::test);
    }

    @Override
    public boolean matches(MachineRecipeInput input, Level level) {
        if (!mode.isEmpty() && !mode.equals(input.mode())) return false;
        return assignment(input) != null;
    }

    @Override
    public ItemStack assemble(MachineRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        ingredients.forEach(i -> list.add(i.ingredient()));
        return list;
    }

    /** Nicht im Rezeptbuch anzeigen (eigene JEI/EMI-Kategorien kommen in Meilenstein 7). */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return kind.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return kind.recipeType();
    }
}
