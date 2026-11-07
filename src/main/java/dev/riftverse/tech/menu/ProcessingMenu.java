package dev.riftverse.tech.menu;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.registry.ModMenus;
import dev.riftverse.tech.MachineKind;
import dev.riftverse.tech.ProcessingBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/** GUI der Erzmühle, Walzbank, des Legierungsofens und der Garagenpresse. */
public class ProcessingMenu extends AbstractContainerMenu {
    public static final int IN_Y = 35, OUT_X = 122, OUT_Y = 35, ARROW_X = 88, ARROW_Y = 35;
    public static final int BATTERY_X = 9, BATTERY_Y = 62;
    public static final int BUTTON_MODE = 0;

    @Nullable
    private final ProcessingBlockEntity machine;
    private final ContainerData data;
    private final MachineKind kind;
    private final int machineSlots;

    /** Client: wird mit der Blockposition geöffnet. */
    public ProcessingMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, machineAt(inventory, buf), new SimpleContainerData(ProcessingBlockEntity.DATA_COUNT));
    }

    @Nullable
    private static ProcessingBlockEntity machineAt(Inventory inventory, RegistryFriendlyByteBuf buf) {
        return inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof ProcessingBlockEntity be ? be : null;
    }

    public ProcessingMenu(int containerId, Inventory inventory, @Nullable ProcessingBlockEntity machine, ContainerData data) {
        super(ModMenus.PROCESSING.get(), containerId);
        this.machine = machine;
        this.data = data;
        this.kind = machine != null ? machine.kind() : MachineKind.ERZMUEHLE;
        IItemHandler handler = machine != null ? machine.items() : new ItemStackHandler(kind.inputs() + 2);

        int n = kind.inputs();
        for (int i = 0; i < n; i++) addSlot(new SlotItemHandler(handler, i, inputX(n, i), IN_Y));
        addSlot(new SlotItemHandler(handler, n, OUT_X, OUT_Y));
        if (kind.electric()) addSlot(new SlotItemHandler(handler, n + 1, BATTERY_X, BATTERY_Y));
        machineSlots = slots.size();

        MenuHelper.addPlayerInventory(inventory, this::addSlot);
        addDataSlots(data);
    }

    /** x-Position des i-ten Eingangs bei n Eingängen. */
    public static int inputX(int n, int i) {
        return switch (n) {
            case 1 -> 56;
            case 2 -> 38 + i * 18;
            default -> 30 + i * 18;
        };
    }

    public MachineKind kind() {
        return kind;
    }

    public int progress() {
        return data.get(ProcessingBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        return Math.max(1, data.get(ProcessingBlockEntity.DATA_MAX));
    }

    public int energy() {
        return MenuHelper.joined(data, ProcessingBlockEntity.DATA_ENERGY_LO, ProcessingBlockEntity.DATA_ENERGY_HI);
    }

    public int capacity() {
        return Math.max(1, MenuHelper.joined(data, ProcessingBlockEntity.DATA_CAP_LO, ProcessingBlockEntity.DATA_CAP_HI));
    }

    public int mode() {
        return data.get(ProcessingBlockEntity.DATA_MODE);
    }

    public boolean cranking() {
        return data.get(ProcessingBlockEntity.DATA_CRANK) > 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_MODE && machine != null && kind.hasModes()) {
            machine.cycleMode();
            return true;
        }
        return false;
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
        int n = kind.inputs();

        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, machineSlots + 36, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else {
            boolean moved = false;
            if (kind.electric() && ItemEnergy.of(stack) != null) moved = moveItemStackTo(stack, n + 1, n + 2, false);
            if (!moved) moved = moveItemStackTo(stack, 0, n, false);
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
