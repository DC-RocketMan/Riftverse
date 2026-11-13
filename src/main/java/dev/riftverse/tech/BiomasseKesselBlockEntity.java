package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Biomasse-Kessel (T1): 80 FE/t, Stufe I. Verbrennt Biomasse-Pulpe aus der organischen Welt oder Pflanzen.
 * Aus Pulpe fällt als Nebenprodukt Nährserum für den späteren Klon-Tank ab.
 */
public class BiomasseKesselBlockEntity extends GeneratorBlockEntity implements BrennstoffGenerator {
    public static final int OUTPUT = 80;
    /** Brenndauer von Biomasse-Pulpe; Pflanzen brennen ein Viertel so lange. */
    public static final int PULPE_BURN = 400;
    public static final int PFLANZE_BURN = 100;
    /** Chance auf Nährserum pro verbrannter Pulpe (1 zu n). */
    public static final int SERUM_CHANCE = 4;

    private int burnTime;
    private int maxBurnTime = 1;

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

    public BiomasseKesselBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIOMASSE_KESSEL.get(), pos, state, 20_000, 3);
    }

    public IItemHandler automationHandler() {
        return automation;
    }

    public static int burnDuration(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(ModItems.BIOMASSE_PULPE.get())) return PULPE_BURN;
        return stack.is(ModTags.Items.BIOMASSE) ? PFLANZE_BURN : 0;
    }

    @Override
    protected boolean generate() {
        if (level == null) return false;
        if (burnTime <= 0) {
            if (energy.isFull()) return false;
            ItemStack fuel = items.getStackInSlot(SLOT_FUEL);
            int duration = burnDuration(fuel);
            if (duration <= 0) return false;
            boolean pulpe = fuel.is(ModItems.BIOMASSE_PULPE.get());
            items.extractItem(SLOT_FUEL, 1, false);
            burnTime = maxBurnTime = duration;
            if (pulpe && level.random.nextInt(SERUM_CHANCE) == 0) addSerum();
        }
        if (energy.isFull()) return false;
        burnTime--;
        energy.add(OUTPUT);
        return true;
    }

    private void addSerum() {
        ItemStack out = items.getStackInSlot(SLOT_OUT);
        if (out.isEmpty()) {
            items.setStackInSlot(SLOT_OUT, new ItemStack(ModItems.NAEHRSERUM.get()));
        } else if (out.is(ModItems.NAEHRSERUM.get()) && out.getCount() < out.getMaxStackSize()) {
            items.setStackInSlot(SLOT_OUT, out.copyWithCount(out.getCount() + 1));
        }
    }

    @Override
    public boolean isFuel(ItemStack stack) {
        return burnDuration(stack) > 0;
    }

    @Override
    public String fuelTooltipKey() {
        return "tooltip.riftverse.kessel_fuel";
    }

    @Override
    public String outputLabelKey() {
        return "gui.riftverse.serum";
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
