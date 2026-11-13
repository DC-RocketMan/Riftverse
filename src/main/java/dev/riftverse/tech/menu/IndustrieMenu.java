package dev.riftverse.tech.menu;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.registry.ModMenus;
import dev.riftverse.tech.IndustrieBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Gemeinsame GUI der T3-Geräte (Materie-Rekonfigurator, Klon-Tank, Universums-Destillator, Mikrokosmos-Zelle,
 * Parallelzapfer). Welche Slots, Balken und Knöpfe es gibt, bestimmt die Block-Entity.
 */
public class IndustrieMenu extends AbstractContainerMenu {
    @Nullable
    private final IndustrieBlockEntity machine;
    private final ContainerData data;
    private final int machineSlots;
    private int batteryMenuSlot = -1;

    public IndustrieMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, machineAt(inventory, buf), new SimpleContainerData(IndustrieBlockEntity.DATA_COUNT));
    }

    @Nullable
    private static IndustrieBlockEntity machineAt(Inventory inventory, RegistryFriendlyByteBuf buf) {
        return inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof IndustrieBlockEntity be ? be : null;
    }

    public IndustrieMenu(int containerId, Inventory inventory, @Nullable IndustrieBlockEntity machine, ContainerData data) {
        super(ModMenus.INDUSTRIE.get(), containerId);
        this.machine = machine;
        this.data = data;
        if (machine != null) {
            ItemStackHandler handler = machine.items();
            for (IndustrieBlockEntity.GuiSlot s : machine.guiSlots()) {
                if (s.index() == machine.batterySlot()) batteryMenuSlot = slots.size();
                addSlot(new SlotItemHandler(handler, s.index(), s.x(), s.y()));
            }
            if (machine.batterySlot() >= 0 && batteryMenuSlot < 0) {
                batteryMenuSlot = slots.size();
                addSlot(new SlotItemHandler(handler, machine.batterySlot(), IndustrieBlockEntity.BATTERY_X, IndustrieBlockEntity.BATTERY_Y));
            }
        }
        machineSlots = slots.size();
        MenuHelper.addPlayerInventory(inventory, this::addSlot);
        addDataSlots(data);
    }

    @Nullable
    public IndustrieBlockEntity machine() {
        return machine;
    }

    public int machineSlots() {
        return machineSlots;
    }

    public int energy() {
        return MenuHelper.joined(data, IndustrieBlockEntity.DATA_ENERGY_LO, IndustrieBlockEntity.DATA_ENERGY_HI);
    }

    public int capacity() {
        return Math.max(1, MenuHelper.joined(data, IndustrieBlockEntity.DATA_CAP_LO, IndustrieBlockEntity.DATA_CAP_HI));
    }

    public int work() {
        return data.get(IndustrieBlockEntity.DATA_WORK);
    }

    public int workMax() {
        return Math.max(1, data.get(IndustrieBlockEntity.DATA_WORK_MAX));
    }

    public int fluid() {
        return data.get(IndustrieBlockEntity.DATA_FLUID);
    }

    public int fluidCapacity() {
        return Math.max(1, data.get(IndustrieBlockEntity.DATA_FLUID_CAP));
    }

    public int[] extras() {
        int[] e = new int[IndustrieBlockEntity.EXTRAS];
        for (int i = 0; i < e.length; i++) e[i] = data.get(IndustrieBlockEntity.DATA_EXTRA + i);
        return e;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (machine == null) return false;
        machine.onButton(id, player);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine != null && !machine.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(machine.getBlockPos())) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else {
            boolean moved = false;
            if (batteryMenuSlot >= 0 && ItemEnergy.of(stack) != null) moved = moveItemStackTo(stack, batteryMenuSlot, batteryMenuSlot + 1, false);
            for (int i = 0; i < machineSlots && !moved; i++) {
                if (i != batteryMenuSlot && slots.get(i).mayPlace(stack)) moved = moveItemStackTo(stack, i, i + 1, false);
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
