package dev.riftverse.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftverse.tech.MachineKind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
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
 * Maschinen mit mehreren Ausgängen (Kristall-Schleifer, Recycler) lesen
 * {@code "byproducts": [{"result": {...}, "chance": 0.4}]}; ohne {@code chance} kommt das Nebenprodukt immer.
 */
public class MachineRecipe implements Recipe<MachineRecipeInput> {
    private final MachineKind kind;
    private final List<CountedIngredient> ingredients;
    private final ItemStack result;
    private final int time;
    private final String mode;
    private final List<Byproduct> byproducts;

    public MachineRecipe(MachineKind kind, List<CountedIngredient> ingredients, ItemStack result, int time, String mode,
                         List<Byproduct> byproducts) {
        this.kind = kind;
        this.ingredients = List.copyOf(ingredients);
        this.result = result;
        this.time = time;
        this.mode = mode;
        this.byproducts = List.copyOf(byproducts);
        if (1 + byproducts.size() > kind.outputs()) {
            throw new IllegalArgumentException("Rezept für " + kind.id() + " hat " + (1 + byproducts.size())
                    + " Produkte, die Maschine aber nur " + kind.outputs() + " Ausgänge");
        }
    }

    /** Nebenprodukt mit Wahrscheinlichkeit (0..1). */
    public record Byproduct(ItemStack result, float chance) {
        public static final Codec<Byproduct> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("result").forGetter(Byproduct::result),
                Codec.floatRange(0.0f, 1.0f).optionalFieldOf("chance", 1.0f).forGetter(Byproduct::chance)
        ).apply(i, Byproduct::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Byproduct> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Byproduct::result,
                ByteBufCodecs.FLOAT, Byproduct::chance,
                Byproduct::new);
    }

    public List<Byproduct> byproducts() {
        return byproducts;
    }

    /** Alles, was bei diesem Durchlauf herauskommt: Hauptprodukt plus gewürfelte Nebenprodukte. */
    public List<ItemStack> roll(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        out.add(result.copy());
        for (Byproduct b : byproducts) {
            if (b.chance() >= 1.0f || random.nextFloat() < b.chance()) out.add(b.result().copy());
        }
        return out;
    }

    /** Alle möglichen Produkte (für die Platzprüfung im Ausgang). */
    public List<ItemStack> allOutputs() {
        List<ItemStack> out = new ArrayList<>();
        out.add(result);
        byproducts.forEach(b -> out.add(b.result()));
        return out;
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
