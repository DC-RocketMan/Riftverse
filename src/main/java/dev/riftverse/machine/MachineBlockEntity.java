package dev.riftverse.machine;

import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Gemeinsame Logik aller Maschinen: 2 Eingänge, 1 Brennstoff, 1 Ausgang.
 * Brennstoff = alles, was auch im Ofen brennt (Kohle, Holz, Lava-Eimer …).
 */
public class MachineBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_A = 0, SLOT_B = 1, SLOT_FUEL = 2, SLOT_OUT = 3, SLOTS = 4;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_OUT) return false;
            if (slot == SLOT_FUEL) return stack.getBurnTime(RecipeType.SMELTING) > 0;
            return true;
        }
    };

    private final IItemHandler topHandler = new RangedWrapper(items, SLOT_A, SLOT_FUEL);
    private final IItemHandler sideHandler = new RangedWrapper(items, SLOT_FUEL, SLOT_OUT);
    private final IItemHandler bottomHandler = new RangedWrapper(items, SLOT_OUT, SLOTS);

    private int progress;
    private int maxProgress = 1;
    private int burnTime;
    private int maxBurnTime = 1;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> burnTime;
                case 3 -> maxBurnTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> burnTime = value;
                case 3 -> maxBurnTime = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE.get(), pos, state);
    }

    public MachineType machineType() {
        return getBlockState().getBlock() instanceof MachineBlock mb ? mb.machineType() : MachineType.REFINER;
    }

    public ItemStackHandler items() {
        return items;
    }

    @Nullable
    public IItemHandler getItemHandlerForSide(@Nullable Direction side) {
        if (side == null) return items;
        return switch (side) {
            case UP -> topHandler;
            case DOWN -> bottomHandler;
            default -> sideHandler;
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        machine.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        boolean wasLit = burnTime > 0;
        boolean dirty = false;

        if (burnTime > 0) {
            burnTime--;
            dirty = true;
        }

        ItemStack a = items.getStackInSlot(SLOT_A);
        ItemStack b = items.getStackInSlot(SLOT_B);
        MachineRecipes.Recipe recipe = MachineRecipes.find(machineType(), a, b);
        boolean canWork = recipe != null && canOutput(recipe.result());

        // Neuen Brennstoff nachlegen, wenn gearbeitet werden kann
        if (canWork && burnTime == 0) {
            ItemStack fuel = items.getStackInSlot(SLOT_FUEL);
            int fuelTime = fuel.getBurnTime(RecipeType.SMELTING);
            if (fuelTime > 0) {
                burnTime = maxBurnTime = fuelTime;
                if (fuel.hasCraftingRemainingItem()) {
                    items.setStackInSlot(SLOT_FUEL, fuel.getCraftingRemainingItem());
                } else {
                    items.extractItem(SLOT_FUEL, 1, false);
                }
                dirty = true;
            }
        }

        if (canWork && burnTime > 0) {
            maxProgress = recipe.time();
            progress++;
            if (progress >= maxProgress) {
                craft(recipe, a, b);
                progress = 0;
            }
            dirty = true;
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2);
            dirty = true;
        }

        boolean lit = burnTime > 0;
        if (wasLit != lit) {
            level.setBlock(pos, state.setValue(MachineBlock.LIT, lit), 3);
        }
        if (dirty) setChanged();
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = items.getStackInSlot(SLOT_OUT);
        if (out.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void craft(MachineRecipes.Recipe recipe, ItemStack a, ItemStack b) {
        int order = recipe.match(a, b);
        if (order < 0) return;
        int slotForA = order == 0 ? SLOT_A : SLOT_B;
        int slotForB = order == 0 ? SLOT_B : SLOT_A;
        items.extractItem(slotForA, recipe.countA(), false);
        if (recipe.inputB() != null) items.extractItem(slotForB, recipe.countB(), false);

        ItemStack out = items.getStackInSlot(SLOT_OUT);
        ItemStack result = recipe.result();
        if (out.isEmpty()) {
            items.setStackInSlot(SLOT_OUT, result);
        } else {
            out.grow(result.getCount());
            items.setStackInSlot(SLOT_OUT, out);
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, items.getStackInSlot(i));
        }
    }

    // ---------------------------------------------------------------- Speichern

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
        maxProgress = Math.max(1, tag.getInt("MaxProgress"));
        burnTime = tag.getInt("BurnTime");
        maxBurnTime = Math.max(1, tag.getInt("MaxBurnTime"));
    }

    // ---------------------------------------------------------------- GUI

    @Override
    public Component getDisplayName() {
        return Component.translatable(machineType().translationKey());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(containerId, inventory, this, data);
    }
}
