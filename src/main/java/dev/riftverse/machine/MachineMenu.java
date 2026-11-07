package dev.riftverse.machine;

import dev.riftverse.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = MachineBlockEntity.SLOTS;
    private static final int INV_START = MACHINE_SLOTS;          // 4
    private static final int HOTBAR_START = INV_START + 27;      // 31
    private static final int END = HOTBAR_START + 9;             // 40

    private final MachineBlockEntity machine;
    private final ContainerData data;
    private final MachineType type;

    /** Client-Konstruktor (wird über das Netzwerk mit der Blockposition geöffnet). */
    public MachineMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory,
                (MachineBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(4));
    }

    public MachineMenu(int containerId, Inventory inventory, MachineBlockEntity machine, ContainerData data) {
        super(ModMenus.MACHINE.get(), containerId);
        this.machine = machine;
        this.data = data;
        this.type = machine != null ? machine.machineType() : MachineType.REFINER;

        IItemHandler handler = machine != null ? machine.items() : new ItemStackHandler(MACHINE_SLOTS);
        addSlot(new SlotItemHandler(handler, MachineBlockEntity.SLOT_A, 38, 17));
        addSlot(new SlotItemHandler(handler, MachineBlockEntity.SLOT_B, 62, 17));
        addSlot(new SlotItemHandler(handler, MachineBlockEntity.SLOT_FUEL, 50, 53));
        addSlot(new SlotItemHandler(handler, MachineBlockEntity.SLOT_OUT, 116, 35));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public MachineType machineType() {
        return type;
    }

    public boolean isBurning() {
        return data.get(2) > 0;
    }

    public int progressScaled(int pixels) {
        int max = Math.max(1, data.get(1));
        return data.get(0) * pixels / max;
    }

    public int burnScaled(int pixels) {
        int max = Math.max(1, data.get(3));
        return data.get(2) * pixels / max;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine != null && !machine.isRemoved()
                && player.distanceToSqr(Vec3.atCenterOf(machine.getBlockPos())) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < MACHINE_SLOTS) {
            // Maschine → Spielerinventar
            if (!moveItemStackTo(stack, INV_START, END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else {
            boolean ingredient = MachineRecipes.isIngredient(type, stack);
            boolean fuel = stack.getBurnTime(RecipeType.SMELTING) > 0;
            boolean moved = false;
            if (ingredient) moved = moveItemStackTo(stack, MachineBlockEntity.SLOT_A, MachineBlockEntity.SLOT_FUEL, false);
            if (!moved && fuel) moved = moveItemStackTo(stack, MachineBlockEntity.SLOT_FUEL, MachineBlockEntity.SLOT_OUT, false);
            if (!moved) {
                if (index < HOTBAR_START) {
                    if (!moveItemStackTo(stack, HOTBAR_START, END, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, INV_START, HOTBAR_START, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
