package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.RiftEnergyBlockEntity;
import dev.riftverse.energy.RiftEnergySource;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.tech.menu.GeneratorMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Basis der Generatoren: Puffer, Pakete ins Netz, Akku-Slot zum Laden einer Energiezelle.
 */
public abstract class GeneratorBlockEntity extends RiftEnergyBlockEntity
        implements RiftEnergySource, MenuProvider, ServerTickable, DropsContents {

    public static final int DATA_ENERGY_LO = 0, DATA_ENERGY_HI = 1, DATA_CAP_LO = 2, DATA_CAP_HI = 3,
            DATA_WORK = 4, DATA_WORK_MAX = 5, DATA_COUNT = 6;

    protected final ItemStackHandler items;
    protected final ContainerData data;

    protected GeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int slots) {
        super(type, pos, state, capacity);
        this.items = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return GeneratorBlockEntity.this.isItemValid(slot, stack);
            }
        };
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_ENERGY_LO -> energy.stored() & 0xFFFF;
                    case DATA_ENERGY_HI -> energy.stored() >>> 16;
                    case DATA_CAP_LO -> energy.capacity() & 0xFFFF;
                    case DATA_CAP_HI -> energy.capacity() >>> 16;
                    case DATA_WORK -> work();
                    case DATA_WORK_MAX -> workMax();
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
    }

    /** Erzeugung in FE/t (= Paketgröße). */
    public abstract int outputPerTick();

    public abstract int batterySlot();

    protected abstract boolean isItemValid(int slot, ItemStack stack);

    /** Fortschrittsanzeige der GUI (Brenndauer, Schwung). */
    protected abstract int work();

    protected abstract int workMax();

    /** Erzeugt Strom in diesem Tick. @return ob der Generator läuft */
    protected abstract boolean generate();

    public ItemStackHandler items() {
        return items;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        boolean running = generate();
        ItemEnergy.charge(items.getStackInSlot(batterySlot()), energy, outputPerTick());
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) != running) {
            level.setBlock(worldPosition, state.setValue(TechBlock.ACTIVE, running), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getOfferedEnergy() {
        return Math.min(energy.stored(), outputPerTick());
    }

    @Override
    public VoltageTier getSourceTier() {
        return VoltageTier.forPacket(outputPerTick());
    }

    @Override
    public void drawEnergy(int amount) {
        energy.take(amount);
    }

    // ------------------------------------------------------------------ Inventar, Speichern, GUI

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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Inventory"));
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new GeneratorMenu(containerId, inventory, this, data);
    }
}
