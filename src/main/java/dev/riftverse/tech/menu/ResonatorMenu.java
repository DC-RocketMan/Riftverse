package dev.riftverse.tech.menu;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.registry.ModMenus;
import dev.riftverse.tech.FluxkristallResonatorBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/** GUI des Fluxkristall-Resonators: Raster aus 3 Zeilen und 3–9 Spalten, darunter Hitze, Wasser und Akku. */
public class ResonatorMenu extends AbstractContainerMenu {
    public static final int GRID_X = 8, GRID_Y = 18;
    public static final int BUCKET_IN_X = 116, BUCKET_OUT_X = 134, BATTERY_X = 152, LOWER_Y = 80;
    /** Spielerinventar liegt 34 Pixel tiefer als in den Standard-GUIs. */
    public static final int INV_SHIFT = 34;

    @Nullable
    private final FluxkristallResonatorBlockEntity resonator;
    private final ContainerData data;
    private final int machineSlots;

    public ResonatorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof FluxkristallResonatorBlockEntity be ? be : null,
                new SimpleContainerData(FluxkristallResonatorBlockEntity.DATA_COUNT));
    }

    public ResonatorMenu(int containerId, Inventory inventory, @Nullable FluxkristallResonatorBlockEntity resonator, ContainerData data) {
        super(ModMenus.RESONATOR.get(), containerId);
        this.resonator = resonator;
        this.data = data;
        IItemHandler h = resonator != null ? resonator.items() : new ItemStackHandler(FluxkristallResonatorBlockEntity.SLOTS);
        for (int row = 0; row < FluxkristallResonatorBlockEntity.ROWS; row++) {
            for (int col = 0; col < FluxkristallResonatorBlockEntity.MAX_COLUMNS; col++) {
                final int c = col;
                addSlot(new SlotItemHandler(h, row * FluxkristallResonatorBlockEntity.MAX_COLUMNS + col, GRID_X + col * 18, GRID_Y + row * 18) {
                    @Override
                    public boolean isActive() {
                        return c < columns();
                    }
                });
            }
        }
        addSlot(new SlotItemHandler(h, FluxkristallResonatorBlockEntity.SLOT_BUCKET_IN, BUCKET_IN_X, LOWER_Y));
        addSlot(new SlotItemHandler(h, FluxkristallResonatorBlockEntity.SLOT_BUCKET_OUT, BUCKET_OUT_X, LOWER_Y));
        addSlot(new SlotItemHandler(h, FluxkristallResonatorBlockEntity.SLOT_BATTERY, BATTERY_X, LOWER_Y));
        machineSlots = slots.size();
        MenuHelper.addPlayerInventory(inventory, this::addSlot, INV_SHIFT);
        addDataSlots(data);
    }

    public int energy() {
        return MenuHelper.joined(data, FluxkristallResonatorBlockEntity.DATA_ENERGY_LO, FluxkristallResonatorBlockEntity.DATA_ENERGY_HI);
    }

    public int capacity() {
        return Math.max(1, MenuHelper.joined(data, FluxkristallResonatorBlockEntity.DATA_CAP_LO, FluxkristallResonatorBlockEntity.DATA_CAP_HI));
    }

    public int heat() {
        return data.get(FluxkristallResonatorBlockEntity.DATA_HEAT);
    }

    /** Hitzeänderung im letzten Tick (kann negativ sein; Datenslots sind shorts). */
    public int heatDelta() {
        return (short) data.get(FluxkristallResonatorBlockEntity.DATA_HEAT_DELTA);
    }

    public int output() {
        return data.get(FluxkristallResonatorBlockEntity.DATA_OUTPUT);
    }

    public int water() {
        return data.get(FluxkristallResonatorBlockEntity.DATA_WATER);
    }

    public int columns() {
        int c = data.get(FluxkristallResonatorBlockEntity.DATA_COLUMNS);
        return c <= 0 ? FluxkristallResonatorBlockEntity.BASE_COLUMNS : c;
    }

    @Override
    public boolean stillValid(Player player) {
        return resonator != null && !resonator.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(resonator.getBlockPos())) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int grid = FluxkristallResonatorBlockEntity.GRID;

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else {
            boolean moved = false;
            if (ItemEnergy.of(stack) != null) moved = moveItemStackTo(stack, grid + 2, grid + 3, false);
            else if (stack.is(Items.WATER_BUCKET)) moved = moveItemStackTo(stack, grid, grid + 1, false);
            else if (FluxkristallResonatorBlockEntity.isComponent(stack)) {
                for (int i = 0; i < grid && !stack.isEmpty(); i++) {
                    Slot target = slots.get(i);
                    if (target.isActive() && !target.hasItem()) {
                        target.setByPlayer(stack.split(1));
                        moved = true;
                    }
                }
            }
            if (!moved && !MenuHelper.moveWithinPlayer(this, index, machineSlots, (s, e, r) -> moveItemStackTo(stack, s, e, r))) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
