package dev.riftverse.tech.menu;

import dev.riftverse.recipe.WerkbankRecipe;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModMenus;
import dev.riftverse.registry.ModRecipes;
import dev.riftverse.tech.WerkbankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Garagen-Werkbank: 3×3-Raster, Ergebnis, Werkzeugslots für Hammer, Lötkolben und Lötzinn.
 * Riftverse-Rezepte haben Vorrang; normale Crafting-Rezepte gehen auch.
 */
public class WerkbankMenu extends AbstractContainerMenu {
    public static final int RESULT = 0, GRID_START = 1, TOOL_START = 10, INV_START = 13;
    public static final int GRID_X = 30, GRID_Y = 17, RESULT_X = 124, RESULT_Y = 35, TOOL_X = 152, TOOL_Y = 17;

    private final CraftingContainer grid = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    private final ItemStackHandler tools;
    @Nullable
    private RecipeHolder<?> current;

    public WerkbankMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, bench(inventory, buf.readBlockPos()), ContainerLevelAccess.NULL);
    }

    @Nullable
    private static WerkbankBlockEntity bench(Inventory inventory, BlockPos pos) {
        return inventory.player.level().getBlockEntity(pos) instanceof WerkbankBlockEntity be ? be : null;
    }

    public WerkbankMenu(int containerId, Inventory inventory, @Nullable WerkbankBlockEntity bench, ContainerLevelAccess access) {
        super(ModMenus.WERKBANK.get(), containerId);
        this.access = access;
        this.player = inventory.player;
        this.tools = bench != null ? bench.tools() : new ItemStackHandler(3);

        addSlot(new ResultSlot(result, 0, RESULT_X, RESULT_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(grid, col + row * 3, GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        for (int i = 0; i < 3; i++) addSlot(new ToolSlot(tools, i, TOOL_X, TOOL_Y + i * 18));
        MenuHelper.addPlayerInventory(inventory, this::addSlot);
    }

    // ------------------------------------------------------------------ Rezept suchen

    @Override
    public void slotsChanged(Container container) {
        if (container == grid) access.execute((level, pos) -> updateResult(level));
    }

    private CraftingInput input() {
        return CraftingInput.of(3, 3, grid.getItems());
    }

    private void updateResult(Level level) {
        if (level.isClientSide) return;
        CraftingInput input = input();
        ItemStack out = ItemStack.EMPTY;
        current = null;

        Optional<RecipeHolder<WerkbankRecipe>> own = level.getRecipeManager().getRecipeFor(ModRecipes.WERKBANK_TYPE.get(), input, level);
        if (own.isPresent()) {
            if (toolsReady(own.get().value())) {
                out = own.get().value().assemble(input, level.registryAccess());
                current = own.get();
            }
        } else {
            Optional<RecipeHolder<CraftingRecipe>> vanilla = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (vanilla.isPresent()) {
                ItemStack assembled = vanilla.get().value().assemble(input, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    out = assembled;
                    current = vanilla.get();
                }
            }
        }
        result.setItem(0, out);
    }

    /** Liegen die nötigen Werkzeuge bereit? */
    private boolean toolsReady(WerkbankRecipe recipe) {
        if (recipe.hammer() > 0 && tools.getStackInSlot(WerkbankBlockEntity.SLOT_HAMMER).isEmpty()) return false;
        if (recipe.loetzinn() > 0) {
            if (tools.getStackInSlot(WerkbankBlockEntity.SLOT_LOETKOLBEN).isEmpty()) return false;
            return tools.getStackInSlot(WerkbankBlockEntity.SLOT_LOETZINN).getCount() >= recipe.loetzinn();
        }
        return true;
    }

    // ------------------------------------------------------------------ Herstellen

    private void onCraft(Player player, ItemStack crafted) {
        RecipeHolder<?> used = current;
        crafted.onCraftedBy(player.level(), player, crafted.getCount());

        if (used != null && used.value() instanceof WerkbankRecipe recipe) {
            if (recipe.hammer() > 0) wear(WerkbankBlockEntity.SLOT_HAMMER, recipe.hammer());
            if (recipe.loetzinn() > 0) {
                wear(WerkbankBlockEntity.SLOT_LOETKOLBEN, 1);
                ItemStack solder = tools.getStackInSlot(WerkbankBlockEntity.SLOT_LOETZINN).copy();
                solder.shrink(recipe.loetzinn());
                tools.setStackInSlot(WerkbankBlockEntity.SLOT_LOETZINN, solder);
            }
        }

        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack in = grid.getItem(i);
            if (in.isEmpty()) continue;
            ItemStack remainder = in.hasCraftingRemainingItem() ? in.getCraftingRemainingItem() : ItemStack.EMPTY;
            grid.removeItem(i, 1);
            if (remainder.isEmpty()) continue;
            if (grid.getItem(i).isEmpty()) {
                grid.setItem(i, remainder);
            } else if (!player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
        }
        slotsChanged(grid);
    }

    /** Werkzeug abnutzen; bei 0 Haltbarkeit zerbricht es. */
    private void wear(int slot, int amount) {
        ItemStack tool = tools.getStackInSlot(slot).copy();
        if (tool.isEmpty() || !tool.isDamageableItem()) return;
        int damage = tool.getDamageValue() + amount;
        if (damage >= tool.getMaxDamage()) {
            tools.setStackInSlot(slot, ItemStack.EMPTY);
            access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.8f, 1.0f));
        } else {
            tool.setDamageValue(damage);
            tools.setStackInSlot(slot, tool);
        }
    }

    // ------------------------------------------------------------------ Slots

    private class ResultSlot extends Slot {
        ResultSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            onCraft(player, stack);
            super.onTake(player, stack);
        }
    }

    private class ToolSlot extends SlotItemHandler {
        ToolSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(grid);
        }
    }

    // ------------------------------------------------------------------ Allgemein

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, grid));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.GARAGEN_WERKBANK.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int end = INV_START + 36;

        if (index == RESULT) {
            if (!moveItemStackTo(stack, INV_START, end, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else if (index < INV_START) {
            if (!moveItemStackTo(stack, INV_START, end, false)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int t = 0; t < 3 && !moved; t++) {
                if (WerkbankBlockEntity.isTool(t, stack)) moved = moveItemStackTo(stack, TOOL_START + t, TOOL_START + t + 1, false);
            }
            if (!moved) moved = moveItemStackTo(stack, GRID_START, TOOL_START, false);
            if (!moved && !MenuHelper.moveWithinPlayer(this, index, INV_START, (s, e, r) -> moveItemStackTo(stack, s, e, r))) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        if (index == RESULT) player.drop(stack, false);
        return original;
    }
}
