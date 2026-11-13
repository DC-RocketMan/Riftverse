package dev.riftverse.tech;

import dev.riftverse.Config;
import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.RiftEnergyBlockEntity;
import dev.riftverse.energy.RiftEnergySource;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModItems;
import dev.riftverse.tech.menu.ResonatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Fluxkristall-Resonator (T2, wachsender Multiblock): ein Raster aus Fluxkristall-Stäben und Kühlkörpern wie ein
 * IndustrialCraft-Reaktor. Der Kern hat 3 Spalten, jede angrenzende Resonator-Kammer (bis zu 6) eine mehr.
 * <ul>
 *   <li>Jeder Stab schwingt mit seinen Nachbarstäben mit: 20 FE/t pro Puls, 1 Puls plus einer je Nachbarstab.</li>
 *   <li>Jeder Puls erzeugt 4 Hitze. Kühlkörper führen je 12 Hitze/t ab, die Hülle selbst 1.</li>
 *   <li>Mit Wasser im Tank ist der Resonator gekühlt: 1,5-fache Leistung, bis zu 20 Hitze/t mehr Abfuhr (1 mB pro Hitze).</li>
 *   <li>Bei 10.000 Hitze explodiert der Kristall. Ein Redstone-Signal schaltet den Resonator ab.</li>
 * </ul>
 * Rund 300 FE/t ungekühlt und 450 FE/t gekühlt sind mit einem guten Aufbau drin; Stufe II.
 */
public class FluxkristallResonatorBlockEntity extends RiftEnergyBlockEntity
        implements RiftEnergySource, MenuProvider, ServerTickable, DropsContents {

    public static final int ROWS = 3, BASE_COLUMNS = 3, MAX_COLUMNS = 9, GRID = ROWS * MAX_COLUMNS;
    public static final int SLOT_BUCKET_IN = GRID, SLOT_BUCKET_OUT = GRID + 1, SLOT_BATTERY = GRID + 2, SLOTS = GRID + 3;
    public static final int FE_PER_PULSE = 20, HEAT_PER_PULSE = 4, SINK_COOLING = 12, HULL_COOLING = 1;
    public static final int WATER_COOLING = 20, MAX_HEAT = 10_000, TANK = 8_000;
    public static final int MAX_OUTPUT = VoltageTier.II.maxPacket();

    public static final int DATA_ENERGY_LO = 0, DATA_ENERGY_HI = 1, DATA_CAP_LO = 2, DATA_CAP_HI = 3, DATA_HEAT = 4,
            DATA_OUTPUT = 5, DATA_WATER = 6, DATA_COLUMNS = 7, DATA_HEAT_DELTA = 8, DATA_COUNT = 9;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < GRID) return isComponent(stack);
            return switch (slot) {
                case SLOT_BUCKET_IN -> stack.is(Items.WATER_BUCKET);
                case SLOT_BATTERY -> ItemEnergy.of(stack) != null;
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < GRID ? 1 : 64;
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_ENERGY_LO -> energy.stored() & 0xFFFF;
                case DATA_ENERGY_HI -> energy.stored() >>> 16;
                case DATA_CAP_LO -> energy.capacity() & 0xFFFF;
                case DATA_CAP_HI -> energy.capacity() >>> 16;
                case DATA_HEAT -> heat;
                case DATA_OUTPUT -> output;
                case DATA_WATER -> water.getAmount();
                case DATA_COLUMNS -> columns;
                case DATA_HEAT_DELTA -> heatDelta;
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

    private int heat;
    private int output;
    private int heatDelta;
    private int columns = BASE_COLUMNS;
    private int checkIn;
    private FluidStack water = FluidStack.EMPTY;

    public FluxkristallResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUXKRISTALL_RESONATOR.get(), pos, state, 100_000);
    }

    public static boolean isComponent(ItemStack stack) {
        return stack.is(ModItems.FLUXKRISTALL_STAB.get()) || stack.is(ModItems.KUEHLKOERPER.get());
    }

    public ItemStackHandler items() {
        return items;
    }

    public int heat() {
        return heat;
    }

    public int columns() {
        return columns;
    }

    /** Zählt die angrenzenden Resonator-Kammern. */
    public void recountChambers() {
        if (level == null) return;
        int chambers = 0;
        for (Direction d : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(d)).is(ModBlocks.RESONATOR_KAMMER.get())) chambers++;
        }
        columns = Math.min(MAX_COLUMNS, BASE_COLUMNS + chambers);
        checkIn = 20;
    }

    private ItemStack at(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= columns) return ItemStack.EMPTY;
        return items.getStackInSlot(row * MAX_COLUMNS + col);
    }

    private boolean rodAt(int row, int col) {
        return at(row, col).is(ModItems.FLUXKRISTALL_STAB.get());
    }

    // ------------------------------------------------------------------ Tick

    @Override
    public void serverTick() {
        if (level == null) return;
        if (--checkIn <= 0) recountChambers();
        emptyBucket();
        ItemEnergy.charge(items.getStackInSlot(SLOT_BATTERY), energy, VoltageTier.II.maxPacket());

        boolean off = level.hasNeighborSignal(worldPosition);
        int fe = 0, produced = 0, sinks = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < columns; col++) {
                ItemStack stack = at(row, col);
                if (stack.is(ModItems.KUEHLKOERPER.get())) {
                    sinks++;
                } else if (!off && stack.is(ModItems.FLUXKRISTALL_STAB.get())) {
                    int pulses = 1;
                    if (rodAt(row - 1, col)) pulses++;
                    if (rodAt(row + 1, col)) pulses++;
                    if (rodAt(row, col - 1)) pulses++;
                    if (rodAt(row, col + 1)) pulses++;
                    fe += FE_PER_PULSE * pulses;
                    produced += HEAT_PER_PULSE * pulses;
                    wear(row * MAX_COLUMNS + col, stack);
                }
            }
        }
        boolean cooled = !water.isEmpty();
        if (cooled) fe = fe * 3 / 2;
        output = Math.min(MAX_OUTPUT, fe);
        if (output > 0) energy.add(output);

        int removed = sinks * SINK_COOLING + HULL_COOLING;
        int before = heat;
        heat = Math.max(0, heat + produced - removed);
        if (cooled && heat > 0) {
            int w = Math.min(Math.min(WATER_COOLING, heat), water.getAmount());
            heat -= w;
            water.shrink(w);
            if (water.isEmpty()) water = FluidStack.EMPTY;
        }
        heatDelta = heat - before;
        if (output > 0 || heat != before) setChanged();

        if (heat >= MAX_HEAT) {
            meltdown();
            return;
        }
        if (heat > MAX_HEAT * 3 / 4 && level.getGameTime() % 10 == 0 && level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1,
                    worldPosition.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.01);
            sl.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, 0.5f);
        }
        BlockState state = level.getBlockState(worldPosition);
        boolean active = output > 0;
        if (state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(TechBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    /** Stäbe nutzen sich ab; ein verbrauchter Stab zerfällt zu Fluxstaub. */
    private void wear(int slot, ItemStack rod) {
        int damage = rod.getDamageValue() + 1;
        if (damage >= rod.getMaxDamage()) {
            items.setStackInSlot(slot, new ItemStack(ModItems.FLUXSTAUB.get(), 2));
        } else {
            ItemStack copy = rod.copy();
            copy.setDamageValue(damage);
            items.setStackInSlot(slot, copy);
        }
    }

    private void meltdown() {
        if (level == null) return;
        heat = 0;
        BlockPos pos = worldPosition;
        if (Config.OVERVOLTAGE.get() == Config.OvervoltageMode.OFF) {
            for (int i = 0; i < GRID; i++) {
                if (items.getStackInSlot(i).is(ModItems.FLUXKRISTALL_STAB.get())) items.setStackInSlot(i, ItemStack.EMPTY);
            }
            return;
        }
        int rods = 0;
        for (int i = 0; i < GRID; i++) {
            if (items.getStackInSlot(i).is(ModItems.FLUXKRISTALL_STAB.get())) rods++;
            items.setStackInSlot(i, ItemStack.EMPTY);   // der Kristall reißt das Raster mit
        }
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0f + Math.min(5, rods * 0.5f),
                Config.OVERVOLTAGE.get() == Config.OvervoltageMode.EXPLODE ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

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

    // ------------------------------------------------------------------ Netz

    @Override
    public int getOfferedEnergy() {
        return Math.min(energy.stored(), MAX_OUTPUT);
    }

    @Override
    public VoltageTier getSourceTier() {
        return VoltageTier.II;
    }

    @Override
    public void drawEnergy(int amount) {
        energy.take(amount);
    }

    // ------------------------------------------------------------------ Automatisierung

    /** Wasser rein über Rohre. */
    public IFluidHandler fluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return water;
            }

            @Override
            public int getTankCapacity(int tank) {
                return TANK;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return stack.is(FluidTags.WATER);
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
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    /** Trichter: Stäbe und Kühlkörper in freie Rasterplätze, Wassereimer rein, leere Eimer raus. */
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
                if (slot < GRID && slot % MAX_COLUMNS >= columns) return stack;
                if (slot == SLOT_BUCKET_OUT) return stack;
                return items.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot == SLOT_BUCKET_OUT) return items.extractItem(slot, amount, simulate);
                // Verbrauchte Stäbe (Fluxstaub) dürfen raus
                if (slot < GRID && items.getStackInSlot(slot).is(ModItems.FLUXSTAUB.get())) return items.extractItem(slot, amount, simulate);
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot != SLOT_BUCKET_OUT && items.isItemValid(slot, stack);
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
        tag.putInt("Heat", heat);
        if (!water.isEmpty()) tag.put("Water", water.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Inventory"));
        heat = tag.getInt("Heat");
        water = FluidStack.parseOptional(registries, tag.getCompound("Water"));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        recountChambers();
        return new ResonatorMenu(containerId, inventory, this, data);
    }
}
