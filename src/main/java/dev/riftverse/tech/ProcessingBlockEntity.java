package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.Overvoltage;
import dev.riftverse.energy.RiftEnergyBlockEntity;
import dev.riftverse.energy.RiftEnergySink;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.recipe.MachineRecipe;
import dev.riftverse.recipe.MachineRecipeInput;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.tech.menu.ProcessingMenu;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemeinsame Block-Entity der verarbeitenden Maschinen. Was die Maschine kann, steht in {@link MachineKind}.
 * <p>
 * Slots: Eingänge 0..n-1, Ausgänge n..n+o-1, Akku-Slot n+o (nur elektrische Maschinen).
 */
public class ProcessingBlockEntity extends RiftEnergyBlockEntity
        implements MenuProvider, RiftEnergySink, Crankable, ServerTickable, DropsContents {

    public static final int DATA_PROGRESS = 0, DATA_MAX = 1, DATA_ENERGY_LO = 2, DATA_ENERGY_HI = 3,
            DATA_CAP_LO = 4, DATA_CAP_HI = 5, DATA_MODE = 6, DATA_CRANK = 7, DATA_COUNT = 8;

    private final MachineKind kind;
    private final ItemStackHandler items;
    private final IItemHandler automation;
    private final ContainerData data;

    private int progress;
    private int maxProgress = 1;
    private int mode;
    private int crankTicks;
    private boolean recipeDirty = true;
    @Nullable
    private RecipeHolder<MachineRecipe> cached;

    public ProcessingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROCESSING.get(), pos, state, kindOf(state).capacity());
        this.kind = kindOf(state);
        final int inputs = kind.inputs();
        final int outputs = kind.outputs();
        this.items = new ItemStackHandler(inputs + outputs + 1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (slot < inputs) recipeDirty = true;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot >= inputs && slot < inputs + outputs) return false;
                if (slot == inputs + outputs) return ItemEnergy.of(stack) != null;
                return true;
            }
        };
        this.automation = new Automation();
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_PROGRESS -> progress;
                    case DATA_MAX -> maxProgress;
                    case DATA_ENERGY_LO -> energy.stored() & 0xFFFF;
                    case DATA_ENERGY_HI -> energy.stored() >>> 16;
                    case DATA_CAP_LO -> energy.capacity() & 0xFFFF;
                    case DATA_CAP_HI -> energy.capacity() >>> 16;
                    case DATA_MODE -> mode;
                    case DATA_CRANK -> crankTicks;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Nur der Client schreibt hier hinein; die Werte kommen vom Server.
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static MachineKind kindOf(BlockState state) {
        return state.getBlock() instanceof ProcessingBlock block ? block.kind() : MachineKind.ERZMUEHLE;
    }

    public MachineKind kind() {
        return kind;
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automationHandler() {
        return automation;
    }

    /** Erster (Haupt-)Ausgang. */
    public int outputSlot() {
        return kind.inputs();
    }

    public boolean isOutputSlot(int slot) {
        return slot >= kind.inputs() && slot < kind.inputs() + kind.outputs();
    }

    public int batterySlot() {
        return kind.inputs() + kind.outputs();
    }

    @Override
    protected boolean joinsNetwork() {
        return kind.electric();
    }

    // ------------------------------------------------------------------ Tick

    @Override
    public void serverTick() {
        if (level == null) return;
        if (kind.electric()) ItemEnergy.discharge(items.getStackInSlot(batterySlot()), energy, VoltageTier.I.maxPacket());

        RecipeHolder<MachineRecipe> holder = currentRecipe();
        boolean working = false;
        if (holder != null && canOutput(holder.value().allOutputs())) {
            MachineRecipe recipe = holder.value();
            maxProgress = recipe.time();
            if (kind.electric()) {
                if (energy.stored() >= kind.energyPerTick()) {
                    energy.take(kind.energyPerTick());
                    progress++;
                    working = true;
                }
            } else if (crankTicks > 0) {
                progress++;
                working = true;
            }
            if (progress >= maxProgress) {
                craft(recipe);
                progress = 0;
            }
            if (working) setChanged();
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
        if (crankTicks > 0) crankTicks--;
        setActive(working);
    }

    private MachineRecipeInput input() {
        List<ItemStack> stacks = new ArrayList<>(kind.inputs());
        for (int i = 0; i < kind.inputs(); i++) stacks.add(items.getStackInSlot(i));
        String m = kind.hasModes() ? kind.modes().get(Math.floorMod(mode, kind.modes().size())) : "";
        return new MachineRecipeInput(stacks, m);
    }

    @Nullable
    private RecipeHolder<MachineRecipe> currentRecipe() {
        if (recipeDirty && level != null) {
            cached = level.getRecipeManager().getRecipeFor(kind.recipeType(), input(), level).orElse(null);
            recipeDirty = false;
        }
        return cached;
    }

    /** Passen alle möglichen Produkte gleichzeitig in die Ausgänge? */
    private boolean canOutput(List<ItemStack> products) {
        List<ItemStack> outs = new ArrayList<>();
        for (int i = 0; i < kind.outputs(); i++) outs.add(items.getStackInSlot(outputSlot() + i).copy());
        for (ItemStack product : products) {
            if (!insertInto(outs, product.copy())) return false;
        }
        return true;
    }

    /** Legt {@code stack} in die Liste (erst zusammenführen, dann freier Platz). */
    private static boolean insertInto(List<ItemStack> outs, ItemStack stack) {
        for (ItemStack o : outs) {
            if (!o.isEmpty() && ItemStack.isSameItemSameComponents(o, stack)) {
                int move = Math.min(stack.getCount(), o.getMaxStackSize() - o.getCount());
                o.grow(move);
                stack.shrink(move);
                if (stack.isEmpty()) return true;
            }
        }
        for (int i = 0; i < outs.size(); i++) {
            if (outs.get(i).isEmpty()) {
                outs.set(i, stack);
                return true;
            }
        }
        return false;
    }

    private void craft(MachineRecipe recipe) {
        MachineRecipeInput in = input();
        int[] slots = recipe.assignment(in);
        if (slots == null) {
            recipeDirty = true;
            return;
        }
        for (int i = 0; i < slots.length; i++) {
            items.extractItem(slots[i], recipe.ingredients().get(i).count(), false);
        }
        List<ItemStack> outs = new ArrayList<>();
        for (int i = 0; i < kind.outputs(); i++) outs.add(items.getStackInSlot(outputSlot() + i).copy());
        for (ItemStack product : recipe.roll(level.random)) insertInto(outs, product);
        for (int i = 0; i < kind.outputs(); i++) {
            if (!ItemStack.matches(outs.get(i), items.getStackInSlot(outputSlot() + i))) items.setStackInSlot(outputSlot() + i, outs.get(i));
        }
    }

    private void setActive(boolean active) {
        if (level == null) return;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(TechBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ Handbetrieb

    @Override
    public void crank(Player player) {
        if (!kind.electric()) crankTicks = 3;
    }

    // ------------------------------------------------------------------ Betriebsart (Walzbank)

    public void cycleMode() {
        if (!kind.hasModes()) return;
        mode = (mode + 1) % kind.modes().size();
        progress = 0;
        recipeDirty = true;
        setChanged();
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getDemandedEnergy() {
        return kind.electric() ? energy.space() : 0;
    }

    @Override
    public VoltageTier getSinkTier() {
        return kind.tier();
    }

    @Override
    public int injectEnergy(int amount, VoltageTier tier) {
        return energy.add(amount);
    }

    @Override
    public void onOvervoltage(VoltageTier tier) {
        if (level != null) Overvoltage.blowUp(level, worldPosition, tier);
    }

    // ------------------------------------------------------------------ Inventar

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, items.getStackInSlot(i));
        }
    }

    /** Für Trichter und Rohre: rein nur in die Eingänge, raus nur aus dem Ausgang. */
    private class Automation implements IItemHandler {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot < kind.inputs() ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isOutputSlot(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < kind.inputs();
        }
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("Mode", mode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
        mode = tag.getInt("Mode");
        recipeDirty = true;
    }

    // ------------------------------------------------------------------ GUI

    @Override
    public Component getDisplayName() {
        return Component.translatable(kind.translationKey());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ProcessingMenu(containerId, inventory, this, data);
    }
}
