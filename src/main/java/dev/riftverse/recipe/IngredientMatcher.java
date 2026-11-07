package dev.riftverse.recipe;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Formlose Zuordnung von Zutaten zu Slots (jede Zutat braucht einen eigenen Slot).
 * Bei höchstens neun Slots reicht einfaches Backtracking.
 */
public final class IngredientMatcher {

    /**
     * @return assignment[i] = Slotindex für Zutat i, oder null, wenn es nicht passt.
     * Alle nicht leeren Slots müssen verbraucht werden.
     */
    public static <T> int[] match(List<T> ingredients, List<ItemStack> stacks, BiPredicate<T, ItemStack> test) {
        int nonEmpty = 0;
        for (ItemStack s : stacks) if (!s.isEmpty()) nonEmpty++;
        if (nonEmpty != ingredients.size()) return null;
        int[] assignment = new int[ingredients.size()];
        boolean[] used = new boolean[stacks.size()];
        return assign(0, ingredients, stacks, test, assignment, used) ? assignment : null;
    }

    private static <T> boolean assign(int i, List<T> ingredients, List<ItemStack> stacks, BiPredicate<T, ItemStack> test,
                                      int[] assignment, boolean[] used) {
        if (i == ingredients.size()) return true;
        for (int s = 0; s < stacks.size(); s++) {
            if (used[s] || stacks.get(s).isEmpty() || !test.test(ingredients.get(i), stacks.get(s))) continue;
            used[s] = true;
            assignment[i] = s;
            if (assign(i + 1, ingredients, stacks, test, assignment, used)) return true;
            used[s] = false;
        }
        return false;
    }

    private IngredientMatcher() {}
}
