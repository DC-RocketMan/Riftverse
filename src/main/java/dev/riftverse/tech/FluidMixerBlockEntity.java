package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.Overvoltage;
import dev.riftverse.energy.RiftEnergyBlockEntity;
import dev.riftverse.energy.RiftEnergySink;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.fluid.Zerfall;
import dev.riftverse.item.FluidKartuscheItem;
import dev.riftverse.recipe.FluidMixerRecipe;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModFluids;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModRecipes;
import dev.riftverse.tech.menu.FluidMixerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fluid-Mixer (T1, 40 FE/t): Fluxstaub + Wasser → Roh-Portalfluid. Füllt leere Fluid-Kartuschen.
 * <p>
 * Roh-Portalfluid zerfällt 10 Minuten nach der Herstellung, auch im Tank; mischt sich frisches Fluid zu
 * älterem, gilt der frühere Zeitpunkt. Die Kette muss also durchlaufen statt zu lagern.
 * <p>
 * Slots: 0 Zutat, 1 Wassereimer rein, 2 leerer Eimer raus, 3 leere Kartusche rein, 4 volle Kartusche raus, 5 Akku.
 */
public class FluidMixerBlockEntity extends RiftEnergyBlockEntity
        implements MenuProvider, RiftEnergySink, ServerTickable, DropsContents {

    public static final int SLOT_INPUT = 0, SLOT_BUCKET_IN = 1, SLOT_BUCKET_OUT = 2, SLOT_CART_IN = 3, SLOT_CART_OUT = 4,
            SLOT_BATTERY = 5, SLOTS = 6;
    public static final int CONSUMPTION = 40;
    public static final int TANK = 8_000;
    public static final int KARTUSCHE = FluidKartuscheItem.INHALT;

    public static final int DATA_PROGRESS = 0, DATA_MAX = 1, DATA_ENERGY_LO = 2, DATA_ENERGY_HI = 3, DATA_CAP_LO = 4,
            DATA_CAP_HI = 5, DATA_WATER = 6, DATA_OUTPUT = 7, DATA_REST = 8, DATA_COUNT = 9;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_BUCKET_IN -> stack.is(Items.WATER_BUCKET);
                case SLOT_CART_IN -> stack.is(ModItems.FLUID_KARTUSCHE.get()) || stack.is(ModItems.FLUID_KARTUSCHE_VOLL.get());
                case SLOT_BATTERY -> ItemEnergy.of(stack) != null;
                case SLOT_BUCKET_OUT, SLOT_CART_OUT -> false;
                default -> true;
            };
        }
    };

    private FluidStack water = FluidStack.EMPTY;
    private FluidStack output = FluidStack.EMPTY;
    private long outputZerfall;
    private int progress;
    private int maxProgress = 1;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX -> maxProgress;
                case DATA_ENERGY_LO -> energy.stored() & 0xFFFF;
                case DATA_ENERGY_HI -> energy.stored() >>> 16;
                case DATA_CAP_LO -> energy.capacity() & 0xFFFF;
                case DATA_CAP_HI -> energy.capacity() >>> 16;
                case DATA_WATER -> water.getAmount();
                case DATA_OUTPUT -> output.getAmount();
                case DATA_REST -> restSeconds();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FluidMixerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_MIXER.get(), pos, state, CONSUMPTION * 400);
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidStack water() {
        return water;
    }

    public FluidStack output() {
        return output;
    }

    public long outputZerfall() {
        return outputZerfall;
    }

    /** Sekunden bis zum Zerfall des Tankinhalts, -1 = leer oder stabil. */
    private int restSeconds() {
        if (output.isEmpty() || outputZerfall <= 0 || level == null) return -1;
        return (int) Math.max(0, (outputZerfall - Zerfall.now(level)) / 20);
    }

    // ------------------------------------------------------------------ Tick

    @Override
    public void serverTick() {
        if (level == null) return;
        ItemEnergy.discharge(items.getStackInSlot(SLOT_BATTERY), energy, VoltageTier.I.maxPacket());
        long now = Zerfall.now(level);

        if (!output.isEmpty() && Zerfall.zerfallen(outputZerfall, now)) {
            decay();
        }
        emptyBucket();

        boolean working = false;
        RecipeHolder<FluidMixerRecipe> holder = level.getRecipeManager()
                .getRecipeFor(ModRecipes.FLUID_MIXER_TYPE.get(), input(), level).orElse(null);
        if (holder != null && canStore(holder.value().result())) {
            FluidMixerRecipe recipe = holder.value();
            maxProgress = recipe.time();
            if (energy.stored() >= CONSUMPTION) {
                energy.take(CONSUMPTION);
                progress++;
                working = true;
                if (progress >= maxProgress) {
                    finish(recipe, now);
                    progress = 0;
                }
                setChanged();
            }
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
        fillCartridge();
        setActive(working);
    }

    private FluidMixerRecipe.Input input() {
        return new FluidMixerRecipe.Input(List.of(items.getStackInSlot(SLOT_INPUT)), water);
    }

    private boolean canStore(FluidStack result) {
        if (output.isEmpty()) return result.getAmount() <= TANK;
        return FluidStack.isSameFluid(output, result) && output.getAmount() + result.getAmount() <= TANK;
    }

    private void finish(FluidMixerRecipe recipe, long now) {
        int[] slots = recipe.assignment(input());
        if (slots == null) return;
        for (int i = 0; i < slots.length; i++) items.extractItem(SLOT_INPUT + slots[i], recipe.ingredients().get(i).count(), false);
        water.shrink(recipe.fluid().amount());
        if (water.isEmpty()) water = FluidStack.EMPTY;

        FluidStack result = recipe.result();
        long fresh = result.is(ModFluids.ROH_PORTALFLUID.get()) ? now + Zerfall.DAUER : 0;
        if (output.isEmpty()) {
            output = result.copy();
            outputZerfall = fresh;
        } else {
            output.grow(result.getAmount());
            outputZerfall = Zerfall.frueher(outputZerfall, fresh);
        }
    }

    private void decay() {
        output = FluidStack.EMPTY;
        outputZerfall = 0;
        progress = 0;
        setChanged();
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, 8, 0.25, 0.1, 0.25, 0.01);
            sl.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.4f);
        }
    }

    /** Wassereimer in den Tank leeren. */
    private void emptyBucket() {
        ItemStack in = items.getStackInSlot(SLOT_BUCKET_IN);
        if (!in.is(Items.WATER_BUCKET) || water.getAmount() + 1000 > TANK) return;
        ItemStack out = items.getStackInSlot(SLOT_BUCKET_OUT);
        if (!out.isEmpty() && (!out.is(Items.BUCKET) || out.getCount() >= out.getMaxStackSize())) return;
        items.extractItem(SLOT_BUCKET_IN, 1, false);
        if (out.isEmpty()) items.setStackInSlot(SLOT_BUCKET_OUT, new ItemStack(Items.BUCKET));
        else items.setStackInSlot(SLOT_BUCKET_OUT, out.copyWithCount(out.getCount() + 1));
        if (water.isEmpty()) water = new FluidStack(Fluids.WATER, 1000);
        else water.grow(1000);
        setChanged();
    }

    /** Leere (oder zerfallene) Kartusche mit 1.000 mB Roh-Portalfluid füllen. */
    private void fillCartridge() {
        if (level == null || output.getAmount() < KARTUSCHE || !output.is(ModFluids.ROH_PORTALFLUID.get())) return;
        ItemStack in = items.getStackInSlot(SLOT_CART_IN);
        boolean usable = in.is(ModItems.FLUID_KARTUSCHE.get())
                || (in.is(ModItems.FLUID_KARTUSCHE_VOLL.get()) && FluidKartuscheItem.zerfallen(in, Zerfall.now(level)));
        if (!usable) return;
        ItemStack full = FluidKartuscheItem.gefuellt(outputZerfall);
        ItemStack out = items.getStackInSlot(SLOT_CART_OUT);
        if (!out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, full) || out.getCount() >= out.getMaxStackSize())) return;
        items.extractItem(SLOT_CART_IN, 1, false);
        if (out.isEmpty()) items.setStackInSlot(SLOT_CART_OUT, full);
        else items.setStackInSlot(SLOT_CART_OUT, out.copyWithCount(out.getCount() + 1));
        output.shrink(KARTUSCHE);
        if (output.isEmpty()) {
            output = FluidStack.EMPTY;
            outputZerfall = 0;
        }
        setChanged();
    }

    private void setActive(boolean active) {
        if (level == null) return;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(TechBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getDemandedEnergy() {
        return energy.space();
    }

    @Override
    public VoltageTier getSinkTier() {
        return VoltageTier.I;
    }

    @Override
    public int injectEnergy(int amount, VoltageTier tier) {
        return energy.add(amount);
    }

    @Override
    public void onOvervoltage(VoltageTier tier) {
        if (level != null) Overvoltage.blowUp(level, worldPosition, tier);
    }

    // ------------------------------------------------------------------ Automatisierung

    /** Für Trichter und Rohre: rein in Zutat, Eimer- und Kartuschen-Eingang; raus aus den beiden Ausgängen. */
    public IItemHandler automationHandler() {
        return new IItemHandler() {
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
                boolean in = slot == SLOT_INPUT || slot == SLOT_BUCKET_IN || slot == SLOT_CART_IN;
                return in ? items.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                boolean out = slot == SLOT_BUCKET_OUT || slot == SLOT_CART_OUT;
                return out ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return (slot == SLOT_INPUT || slot == SLOT_BUCKET_IN || slot == SLOT_CART_IN) && items.isItemValid(slot, stack);
            }
        };
    }

    /** Wasser rein (Tank 0), Roh-Portalfluid mit Zerfallszeitpunkt raus (Tank 1). */
    public IFluidHandler fluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 2;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tank == 0 ? water : stamped(output);
            }

            @Override
            public int getTankCapacity(int tank) {
                return TANK;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank == 0 && stack.is(FluidTags.WATER);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !resource.is(FluidTags.WATER)) return 0;
                int accepted = Math.min(resource.getAmount(), TANK - water.getAmount());
                if (accepted > 0 && action.execute()) {
                    if (water.isEmpty()) water = new FluidStack(Fluids.WATER, accepted);
                    else water.grow(accepted);
                    setChanged();
                }
                return accepted;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !FluidStack.isSameFluid(resource, output)) return FluidStack.EMPTY;
                return drain(resource.getAmount(), action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                int amount = Math.min(maxDrain, output.getAmount());
                if (amount <= 0) return FluidStack.EMPTY;
                FluidStack drained = stamped(output.copyWithAmount(amount));
                if (action.execute()) {
                    output.shrink(amount);
                    if (output.isEmpty()) {
                        output = FluidStack.EMPTY;
                        outputZerfall = 0;
                    }
                    setChanged();
                }
                return drained;
            }
        };
    }

    private FluidStack stamped(FluidStack stack) {
        if (stack.isEmpty() || outputZerfall <= 0) return stack;
        FluidStack copy = stack.copy();
        copy.set(ModDataComponents.ZERFALL.get(), outputZerfall);
        return copy;
    }

    // ------------------------------------------------------------------ Speichern, GUI

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, items.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", items.serializeNBT(registries));
        if (!water.isEmpty()) tag.put("Water", water.save(registries));
        if (!output.isEmpty()) tag.put("Output", output.save(registries));
        tag.putLong("OutputZerfall", outputZerfall);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Inventory"));
        water = FluidStack.parseOptional(registries, tag.getCompound("Water"));
        output = FluidStack.parseOptional(registries, tag.getCompound("Output"));
        outputZerfall = tag.getLong("OutputZerfall");
        progress = tag.getInt("Progress");
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FluidMixerMenu(containerId, inventory, this, data);
    }
}
