package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.function.Supplier;

/**
 * Tonnenreaktor (T1): 40 FE/t, Stufe I. Verbrennt jedes Item, Schrott zählt doppelt.
 * Wirft gelegentlich ein zufälliges nützliches Kleinteil aus.
 */
public class TonnenreaktorBlockEntity extends GeneratorBlockEntity {
    public static final int OUTPUT = 40;
    public static final int SLOT_FUEL = 0, SLOT_OUT = 1, SLOT_BATTERY = 2;
    /** Brenndauer eines beliebigen Items in Ticks; Brennstoffe halten länger. */
    public static final int BASE_BURN = 50;
    /** Chance auf ein Kleinteil pro verbranntem Item (1 zu n). */
    public static final int PART_CHANCE = 12;

    private static final List<Supplier<? extends ItemLike>> PARTS = List.of(
            ModItems.KUPFERDRAHT, ModItems.FEDER, ModItems.MINI_MOTOR, ModItems.POLYMERFETZEN,
            () -> Items.IRON_NUGGET, () -> Items.REDSTONE);

    private int burnTime;
    private int maxBurnTime = 1;

    /** Für Trichter und Rohre: rein in den Brennstoff-Slot, raus aus dem Kleinteile-Slot. */
    private final IItemHandler automation = new IItemHandler() {
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
            return slot == SLOT_FUEL ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_FUEL && burnDuration(stack) > 0;
        }
    };

    public TonnenreaktorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TONNENREAKTOR.get(), pos, state, 10_000, 3);
    }

    public IItemHandler automationHandler() {
        return automation;
    }

    /** Brenndauer eines Items im Tonnenreaktor (0 = gar nicht). */
    public static int burnDuration(ItemStack stack) {
        if (stack.isEmpty() || ItemEnergy.of(stack) != null) return 0;   // Akkus nicht verheizen
        int fuel = stack.getBurnTime(RecipeType.SMELTING);
        int base = stack.is(ModTags.Items.SCHROTT) ? BASE_BURN * 2 : BASE_BURN;
        return Math.max(base, fuel / 2);
    }

    @Override
    protected boolean generate() {
        if (level == null) return false;
        if (burnTime <= 0) {
            if (energy.isFull()) return false;
            ItemStack fuel = items.getStackInSlot(SLOT_FUEL);
            int duration = burnDuration(fuel);
            if (duration <= 0) return false;
            if (fuel.hasCraftingRemainingItem() && fuel.getCount() == 1) {
                items.setStackInSlot(SLOT_FUEL, fuel.getCraftingRemainingItem());
            } else {
                items.extractItem(SLOT_FUEL, 1, false);
            }
            burnTime = maxBurnTime = duration;
            if (level.random.nextInt(PART_CHANCE) == 0) ejectPart();
        }
        if (energy.isFull()) return false;   // pausiert, bis wieder Platz im Puffer ist
        burnTime--;
        energy.add(OUTPUT);
        return true;
    }

    private void ejectPart() {
        ItemStack part = new ItemStack(PARTS.get(level.random.nextInt(PARTS.size())).get());
        ItemStack out = items.getStackInSlot(SLOT_OUT);
        if (out.isEmpty()) {
            items.setStackInSlot(SLOT_OUT, part);
        } else if (ItemStack.isSameItemSameComponents(out, part) && out.getCount() < out.getMaxStackSize()) {
            ItemStack grown = out.copy();
            grown.grow(1);
            items.setStackInSlot(SLOT_OUT, grown);
        }
    }

    @Override
    public int outputPerTick() {
        return OUTPUT;
    }

    @Override
    public int batterySlot() {
        return SLOT_BATTERY;
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FUEL -> burnDuration(stack) > 0;
            case SLOT_BATTERY -> ItemEnergy.of(stack) != null;
            default -> false;
        };
    }

    @Override
    protected int work() {
        return burnTime;
    }

    @Override
    protected int workMax() {
        return maxBurnTime;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("MaxBurnTime", maxBurnTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTime = tag.getInt("BurnTime");
        maxBurnTime = Math.max(1, tag.getInt("MaxBurnTime"));
    }
}
