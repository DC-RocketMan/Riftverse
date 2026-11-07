package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.Overvoltage;
import dev.riftverse.energy.RiftEnergyBlockEntity;
import dev.riftverse.energy.RiftEnergySink;
import dev.riftverse.energy.RiftEnergySource;
import dev.riftverse.energy.RiftEnergyStorage;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.tech.menu.SpeicherMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Xenit-Zelle (T1): einfacher Akku mit 1 Mio. FE, Stufe I. Nimmt Pakete aus dem Netz an und gibt sie mit
 * Stufe I (128 FE/t) wieder ab. Abgebaut behält sie ihre Ladung.
 * Slot 0 lädt ein Item (Energiezelle), Slot 1 entlädt eins.
 */
public class XenitZelleBlockEntity extends RiftEnergyBlockEntity
        implements RiftEnergySource, RiftEnergySink, RiftEnergyStorage, MenuProvider, ServerTickable, DropsContents {

    public static final int CAPACITY = 1_000_000;
    public static final VoltageTier TIER = VoltageTier.I;
    public static final int SLOT_CHARGE = 0, SLOT_DISCHARGE = 1;
    public static final int DATA_ENERGY_LO = 0, DATA_ENERGY_HI = 1, DATA_CAP_LO = 2, DATA_CAP_HI = 3, DATA_COUNT = 4;

    private final ItemStackHandler items = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ItemEnergy.of(stack) != null;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
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

    private int lastStored = -1;
    private int flowTicks;

    public XenitZelleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.XENIT_ZELLE.get(), pos, state, CAPACITY);
    }

    public ItemStackHandler items() {
        return items;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        ItemEnergy.charge(items.getStackInSlot(SLOT_CHARGE), energy, TIER.maxPacket());
        ItemEnergy.discharge(items.getStackInSlot(SLOT_DISCHARGE), energy, TIER.maxPacket());
        if (energy.stored() != lastStored) {
            if (lastStored >= 0) flowTicks = 20;
            lastStored = energy.stored();
        }
        if (flowTicks > 0) flowTicks--;
        boolean active = flowTicks > 0;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(TechBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getOfferedEnergy() {
        return Math.min(energy.stored(), TIER.emitPacket());
    }

    @Override
    public VoltageTier getSourceTier() {
        return TIER;
    }

    @Override
    public void drawEnergy(int amount) {
        energy.take(amount);
    }

    @Override
    public int getDemandedEnergy() {
        return energy.space();
    }

    @Override
    public VoltageTier getSinkTier() {
        return TIER;
    }

    @Override
    public int injectEnergy(int amount, VoltageTier tier) {
        return energy.add(amount);
    }

    @Override
    public void onOvervoltage(VoltageTier tier) {
        if (level != null) Overvoltage.blowUp(level, worldPosition, tier);
    }

    // ------------------------------------------------------------------ Ladung im Item behalten

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.ENERGY.get(), energy.stored());
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        energy.set(input.getOrDefault(ModDataComponents.ENERGY.get(), 0));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Energy");
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
        return new SpeicherMenu(containerId, inventory, this, data);
    }
}
