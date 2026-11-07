package dev.riftverse.machine;

import dev.riftverse.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fest im Code definierte Maschinenrezepte (bewusst simpel gehalten).
 * Jedes Rezept hat bis zu zwei Zutaten mit Mengenangabe; die Reihenfolge der
 * Eingabeslots ist egal.
 */
public final class MachineRecipes {

    public record Recipe(MachineType machine,
                         Supplier<? extends ItemLike> inputA, int countA,
                         @Nullable Supplier<? extends ItemLike> inputB, int countB,
                         Supplier<? extends ItemLike> output, int outputCount,
                         int time) {

        public ItemStack result() {
            return new ItemStack(output.get(), outputCount);
        }

        private boolean fits(ItemStack stack, @Nullable Supplier<? extends ItemLike> item, int count) {
            if (item == null) return stack.isEmpty();
            return !stack.isEmpty() && stack.is(item.get().asItem()) && stack.getCount() >= count;
        }

        /** @return 0 = Slot A↔Zutat A, 1 = vertauscht, -1 = passt nicht */
        public int match(ItemStack slotA, ItemStack slotB) {
            if (fits(slotA, inputA, countA) && fits(slotB, inputB, countB)) return 0;
            if (fits(slotB, inputA, countA) && fits(slotA, inputB, countB)) return 1;
            return -1;
        }
    }

    private static List<Recipe> recipes;

    private static List<Recipe> all() {
        if (recipes == null) {
            List<Recipe> list = new ArrayList<>();
            // --- Quanten-Raffinerie ---
            list.add(new Recipe(MachineType.REFINER, ModItems.RAW_QUANTUM, 1, () -> Items.REDSTONE, 1,
                    ModItems.QUANTUM_INGOT, 1, 160));
            list.add(new Recipe(MachineType.REFINER, ModItems.RAW_QUANTUM, 1, () -> Items.GLOWSTONE_DUST, 1,
                    ModItems.QUANTUM_INGOT, 2, 200));
            // --- Fluid-Synthesizer ---
            list.add(new Recipe(MachineType.SYNTHESIZER, ModItems.QUANTUM_INGOT, 1, () -> Items.GLASS, 1,
                    ModItems.RIFT_FLUID_CELL, 2, 140));
            list.add(new Recipe(MachineType.SYNTHESIZER, ModItems.QUANTUM_INGOT, 1, () -> Items.SLIME_BALL, 1,
                    ModItems.RIFT_FLUID_CELL, 4, 180));
            // --- Rift-Assembler ---
            list.add(new Recipe(MachineType.ASSEMBLER, ModItems.QUANTUM_INGOT, 4, () -> Items.ENDER_EYE, 1,
                    ModItems.RIFT_CORE, 1, 300));
            // Alternative ohne Nether: teurer, aber früher erreichbar
            list.add(new Recipe(MachineType.ASSEMBLER, ModItems.QUANTUM_INGOT, 6, () -> Items.DIAMOND, 1,
                    ModItems.RIFT_CORE, 1, 400));
            list.add(new Recipe(MachineType.ASSEMBLER, ModItems.GUN_FRAME, 1, ModItems.RIFT_CORE, 1,
                    ModItems.RIFT_GUN, 1, 400));
            list.add(new Recipe(MachineType.ASSEMBLER, ModItems.RIFT_GUN, 1, ModItems.UNIVERSE_SHARD, 4,
                    ModItems.RIFT_GUN_MK2, 1, 600));
            recipes = List.copyOf(list);
        }
        return recipes;
    }

    @Nullable
    public static Recipe find(MachineType type, ItemStack a, ItemStack b) {
        for (Recipe r : all()) {
            if (r.machine() == type && r.match(a, b) >= 0) return r;
        }
        return null;
    }

    /** Ist das Item irgendwo Zutat dieser Maschine? (für Shift-Klick) */
    public static boolean isIngredient(MachineType type, ItemStack stack) {
        for (Recipe r : all()) {
            if (r.machine() != type) continue;
            if (stack.is(r.inputA().get().asItem())) return true;
            if (r.inputB() != null && stack.is(r.inputB().get().asItem())) return true;
        }
        return false;
    }

    public static List<Recipe> forMachine(MachineType type) {
        return all().stream().filter(r -> r.machine() == type).toList();
    }

    private MachineRecipes() {}
}
