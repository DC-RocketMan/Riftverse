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
import dev.riftverse.tech.menu.RaffinerieMenu;
import dev.riftverse.tech.multiblock.Multiblock;
import dev.riftverse.tech.multiblock.MultiblockController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Portalfluid-Raffinerie (T2, Multiblock 3 × 3 × 4, 250 FE/t): Roh-Portalfluid + Säuregel + Fluxstaub →
 * stabilisiertes Portalfluid, das nicht mehr zerfällt. Füllt leere Fluid-Kartuschen für die Kanone.
 * Ab Meilenstein 4 verdichtet sie Roh-Portalfluid mit Chronoglas und Flux-Stabilisator auch zu Portalfluid-Konzentrat
 * (2:1), dem Treibstoff der Kanone Mk III; der Ausgangstank nimmt immer nur eine Sorte auf einmal.
 * <p>
 * Das Roh-Portalfluid zerfällt auch hier im Eingangstank: Mixer und Raffinerie müssen durchlaufen.
 * Es kommt aus vollen Kartuschen oder über Rohre direkt aus dem Fluid-Mixer.
 * <p>
 * Slots: 0–1 Zutaten, 2 volle Roh-Kartusche rein, 3 leere Kartusche raus, 4 leere Kartusche rein,
 * 5 stabile Kartusche raus, 6 Akku.
 */
public class RaffinerieBlockEntity extends RiftEnergyBlockEntity
        implements MenuProvider, RiftEnergySink, ServerTickable, DropsContents, MultiblockController {

    public static final int SLOT_IN_A = 0, SLOT_IN_B = 1, SLOT_RAW_IN = 2, SLOT_RAW_OUT = 3, SLOT_CART_IN = 4,
            SLOT_CART_OUT = 5, SLOT_BATTERY = 6, SLOTS = 7;
    public static final int CONSUMPTION = 250;
    public static final int TANK = 8_000;
    public static final int KARTUSCHE = FluidKartuscheItem.INHALT;

    public static final int DATA_PROGRESS = 0, DATA_MAX = 1, DATA_ENERGY_LO = 2, DATA_ENERGY_HI = 3, DATA_CAP_LO = 4,
            DATA_CAP_HI = 5, DATA_RAW = 6, DATA_STABIL = 7, DATA_REST = 8, DATA_FORMED = 9, DATA_COUNT = 10;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_RAW_IN -> stack.is(ModItems.FLUID_KARTUSCHE_VOLL.get());
                case SLOT_CART_IN -> stack.is(ModItems.FLUID_KARTUSCHE.get());
                case SLOT_BATTERY -> ItemEnergy.of(stack) != null;
                case SLOT_RAW_OUT, SLOT_CART_OUT -> false;
                default -> true;
            };
        }
    };

    private FluidStack raw = FluidStack.EMPTY;
    private long rawZerfall;
    private FluidStack stabil = FluidStack.EMPTY;
    private int progress;
    private int maxProgress = 1;
    private boolean formed;
    private int checkIn;

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
                case DATA_RAW -> raw.getAmount();
                case DATA_STABIL -> stabil.getAmount();
                case DATA_REST -> restSeconds();
                case DATA_FORMED -> formed ? 1 : 0;
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

    public RaffinerieBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RAFFINERIE.get(), pos, state, CONSUMPTION * 400);
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidStack raw() {
        return raw;
    }

    public FluidStack stabil() {
        return stabil;
    }

    public boolean formed() {
        return formed;
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(TechBlock.FACING) ? state.getValue(TechBlock.FACING) : Direction.NORTH;
    }

    @Nullable
    @Override
    public Component checkStructure() {
        if (level == null) return null;
        Component missing = Multiblock.RAFFINERIE.missingMessage(level, worldPosition, facing());
        formed = missing == null;
        checkIn = 20;
        return missing;
    }

    private int restSeconds() {
        if (raw.isEmpty() || rawZerfall <= 0 || level == null) return -1;
        return (int) Math.max(0, (rawZerfall - Zerfall.now(level)) / 20);
    }

    // ------------------------------------------------------------------ Tick

    @Override
    public void serverTick() {
        if (level == null) return;
        if (--checkIn <= 0) checkStructure();
        ItemEnergy.discharge(items.getStackInSlot(SLOT_BATTERY), energy, VoltageTier.II.maxPacket());
        long now = Zerfall.now(level);
        if (!raw.isEmpty() && Zerfall.zerfallen(rawZerfall, now)) decay();
        emptyRawCartridge(now);

        boolean working = false;
        RecipeHolder<FluidMixerRecipe> holder = formed ? level.getRecipeManager()
                .getRecipeFor(ModRecipes.RAFFINERIE_TYPE.get(), input(), level).orElse(null) : null;
        if (holder != null && canStore(holder.value().result())) {
            FluidMixerRecipe recipe = holder.value();
            maxProgress = recipe.time();
            if (energy.stored() >= CONSUMPTION) {
                energy.take(CONSUMPTION);
                progress++;
                working = true;
                if (progress >= maxProgress) {
                    finish(recipe);
                    progress = 0;
                }
                setChanged();
            }
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
        fillCartridge();
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) != working) {
            level.setBlock(worldPosition, state.setValue(TechBlock.ACTIVE, working), Block.UPDATE_ALL);
        }
    }

    private FluidMixerRecipe.Input input() {
        return new FluidMixerRecipe.Input(List.of(items.getStackInSlot(SLOT_IN_A), items.getStackInSlot(SLOT_IN_B)), raw);
    }

    private boolean canStore(FluidStack result) {
        if (stabil.isEmpty()) return result.getAmount() <= TANK;
        return FluidStack.isSameFluid(stabil, result) && stabil.getAmount() + result.getAmount() <= TANK;
    }

    private void finish(FluidMixerRecipe recipe) {
        int[] slots = recipe.assignment(input());
        if (slots == null) return;
        for (int i = 0; i < slots.length; i++) items.extractItem(SLOT_IN_A + slots[i], recipe.ingredients().get(i).count(), false);
        raw.shrink(recipe.fluid().amount());
        if (raw.isEmpty()) {
            raw = FluidStack.EMPTY;
            rawZerfall = 0;
        }
        if (stabil.isEmpty()) stabil = recipe.result().copy();
        else stabil.grow(recipe.result().getAmount());
    }

    private void decay() {
        raw = FluidStack.EMPTY;
        rawZerfall = 0;
        progress = 0;
        setChanged();
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, 8, 0.25, 0.1, 0.25, 0.01);
            sl.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.4f);
        }
    }

    /** Volle Roh-Kartusche in den Eingangstank leeren; die leere Kartusche kommt in Slot 3. */
    private void emptyRawCartridge(long now) {
        ItemStack in = items.getStackInSlot(SLOT_RAW_IN);
        if (!in.is(ModItems.FLUID_KARTUSCHE_VOLL.get()) || raw.getAmount() + KARTUSCHE > TANK) return;
        ItemStack out = items.getStackInSlot(SLOT_RAW_OUT);
        if (!out.isEmpty() && (!out.is(ModItems.FLUID_KARTUSCHE.get()) || out.getCount() >= out.getMaxStackSize())) return;
        boolean decayed = FluidKartuscheItem.zerfallen(in, now);
        long z = FluidKartuscheItem.zerfall(in);
        items.extractItem(SLOT_RAW_IN, 1, false);
        if (out.isEmpty()) items.setStackInSlot(SLOT_RAW_OUT, new ItemStack(ModItems.FLUID_KARTUSCHE.get()));
        else items.setStackInSlot(SLOT_RAW_OUT, out.copyWithCount(out.getCount() + 1));
        if (decayed) return;
        addRaw(KARTUSCHE, z);
    }

    private void addRaw(int amount, long zerfall) {
        if (raw.isEmpty()) {
            raw = new FluidStack(ModFluids.ROH_PORTALFLUID.get(), amount);
            rawZerfall = zerfall;
        } else {
            raw.grow(amount);
            rawZerfall = Zerfall.frueher(rawZerfall, zerfall);
        }
        setChanged();
    }

    /** Leere Kartusche mit 1.000 mB stabilisiertem Portalfluid (oder Konzentrat) füllen. */
    private void fillCartridge() {
        if (stabil.getAmount() < KARTUSCHE) return;
        ItemStack in = items.getStackInSlot(SLOT_CART_IN);
        if (!in.is(ModItems.FLUID_KARTUSCHE.get())) return;
        net.minecraft.world.item.Item full = stabil.is(ModFluids.PORTALFLUID_KONZENTRAT.get())
                ? ModItems.FLUID_KARTUSCHE_KONZENTRAT.get() : ModItems.FLUID_KARTUSCHE_STABIL.get();
        ItemStack out = items.getStackInSlot(SLOT_CART_OUT);
        if (!out.isEmpty() && (!out.is(full) || out.getCount() >= out.getMaxStackSize())) return;
        items.extractItem(SLOT_CART_IN, 1, false);
        if (out.isEmpty()) items.setStackInSlot(SLOT_CART_OUT, new ItemStack(full));
        else items.setStackInSlot(SLOT_CART_OUT, out.copyWithCount(out.getCount() + 1));
        stabil.shrink(KARTUSCHE);
        if (stabil.isEmpty()) stabil = FluidStack.EMPTY;
        setChanged();
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getDemandedEnergy() {
        return energy.space();
    }

    @Override
    public VoltageTier getSinkTier() {
        return VoltageTier.II;
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
                boolean in = slot == SLOT_IN_A || slot == SLOT_IN_B || slot == SLOT_RAW_IN || slot == SLOT_CART_IN;
                return in ? items.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                boolean out = slot == SLOT_RAW_OUT || slot == SLOT_CART_OUT;
                return out ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                boolean in = slot == SLOT_IN_A || slot == SLOT_IN_B || slot == SLOT_RAW_IN || slot == SLOT_CART_IN;
                return in && items.isItemValid(slot, stack);
            }
        };
    }

    /** Roh-Portalfluid rein (Tank 0, mit Zerfallszeitpunkt), stabilisiertes Portalfluid raus (Tank 1). */
    public IFluidHandler fluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 2;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tank == 0 ? raw : stabil;
            }

            @Override
            public int getTankCapacity(int tank) {
                return TANK;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return tank == 0 && stack.is(ModFluids.ROH_PORTALFLUID.get());
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !resource.is(ModFluids.ROH_PORTALFLUID.get()) || level == null) return 0;
                int accepted = Math.min(resource.getAmount(), TANK - raw.getAmount());
                if (accepted > 0 && action.execute()) {
                    long z = resource.getOrDefault(ModDataComponents.ZERFALL.get(), 0L);
                    if (z <= 0) z = Zerfall.frisch(level);
                    if (Zerfall.zerfallen(z, Zerfall.now(level))) return accepted;   // schon zerfallen: verpufft
                    addRaw(accepted, z);
                }
                return accepted;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !FluidStack.isSameFluid(resource, stabil)) return FluidStack.EMPTY;
                return drain(resource.getAmount(), action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                int amount = Math.min(maxDrain, stabil.getAmount());
                if (amount <= 0) return FluidStack.EMPTY;
                FluidStack drained = stabil.copyWithAmount(amount);
                if (action.execute()) {
                    stabil.shrink(amount);
                    if (stabil.isEmpty()) stabil = FluidStack.EMPTY;
                    setChanged();
                }
                return drained;
            }
        };
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
        if (!raw.isEmpty()) tag.put("Raw", raw.save(registries));
        if (!stabil.isEmpty()) tag.put("Stabil", stabil.save(registries));
        tag.putLong("RawZerfall", rawZerfall);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Inventory"));
        raw = FluidStack.parseOptional(registries, tag.getCompound("Raw"));
        stabil = FluidStack.parseOptional(registries, tag.getCompound("Stabil"));
        rawZerfall = tag.getLong("RawZerfall");
        progress = tag.getInt("Progress");
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RaffinerieMenu(containerId, inventory, this, data);
    }
}
