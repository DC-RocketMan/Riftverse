package dev.riftverse.tech.menu;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModMenus;
import dev.riftverse.tech.RaffinerieBlockEntity;
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

/** GUI der Portalfluid-Raffinerie: Roh-Tank links, Zutaten in der Mitte, stabiler Tank und Kartuschen rechts. */
public class RaffinerieMenu extends AbstractContainerMenu {
    public static final int RAW_IN_X = 30, RAW_IN_Y = 17, RAW_OUT_X = 30, RAW_OUT_Y = 45;
    public static final int IN_A_X = 66, IN_B_X = 84, IN_Y = 17;
    public static final int CART_IN_X = 148, CART_IN_Y = 17, CART_OUT_X = 148, CART_OUT_Y = 45;
    public static final int BATTERY_X = 9, BATTERY_Y = 62;
    public static final int RAW_TANK_X = 50, STABIL_TANK_X = 128, TANK_Y = 16, TANK_W = 12, TANK_H = 42;
    public static final int ARROW_X = 104, ARROW_Y = 26;

    @Nullable
    private final RaffinerieBlockEntity raffinerie;
    private final ContainerData data;
    private final int machineSlots;

    public RaffinerieMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof RaffinerieBlockEntity be ? be : null,
                new SimpleContainerData(RaffinerieBlockEntity.DATA_COUNT));
    }

    public RaffinerieMenu(int containerId, Inventory inventory, @Nullable RaffinerieBlockEntity raffinerie, ContainerData data) {
        super(ModMenus.RAFFINERIE.get(), containerId);
        this.raffinerie = raffinerie;
        this.data = data;
        IItemHandler h = raffinerie != null ? raffinerie.items() : new ItemStackHandler(RaffinerieBlockEntity.SLOTS);
        addSlot(new SlotItemHandler(h, RaffinerieBlockEntity.SLOT_IN_A, IN_A_X, IN_Y));
        addSlot(new SlotItemHandler(h, RaffinerieBlockEntity.SLOT_IN_B, IN_B_X, IN_Y));
        addSlot(new SlotItemHandler(h, RaffinerieBlockEntity.SLOT_RAW_IN, RAW_IN_X, RAW_IN_Y));
        addSlot(new SlotItemHandler(h, RaffinerieBlockEntity.SLOT_RAW_OUT, RAW_OUT_X, RAW_OUT_Y));
        addSlot(new SlotItemHandler(h, RaffinerieBlockEntity.SLOT_CART_IN, CART_IN_X, CART_IN_Y));
        addSlot(new SlotItemHandler(h, RaffinerieBlockEntity.SLOT_CART_OUT, CART_OUT_X, CART_OUT_Y));
        addSlot(new SlotItemHandler(h, RaffinerieBlockEntity.SLOT_BATTERY, BATTERY_X, BATTERY_Y));
        machineSlots = slots.size();
        MenuHelper.addPlayerInventory(inventory, this::addSlot);
        addDataSlots(data);
    }

    public int progress() {
        return data.get(RaffinerieBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        return Math.max(1, data.get(RaffinerieBlockEntity.DATA_MAX));
    }

    public int energy() {
        return MenuHelper.joined(data, RaffinerieBlockEntity.DATA_ENERGY_LO, RaffinerieBlockEntity.DATA_ENERGY_HI);
    }

    public int capacity() {
        return Math.max(1, MenuHelper.joined(data, RaffinerieBlockEntity.DATA_CAP_LO, RaffinerieBlockEntity.DATA_CAP_HI));
    }

    public int raw() {
        return data.get(RaffinerieBlockEntity.DATA_RAW);
    }

    public int stabil() {
        return data.get(RaffinerieBlockEntity.DATA_STABIL);
    }

    public int restSeconds() {
        return (short) data.get(RaffinerieBlockEntity.DATA_REST);
    }

    public boolean formed() {
        return data.get(RaffinerieBlockEntity.DATA_FORMED) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return raffinerie != null && !raffinerie.isRemoved()
                && player.distanceToSqr(Vec3.atCenterOf(raffinerie.getBlockPos())) <= 64.0;
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
            boolean moved;
            if (ItemEnergy.of(stack) != null) moved = moveItemStackTo(stack, 6, 7, false);
            else if (stack.is(ModItems.FLUID_KARTUSCHE_VOLL.get())) moved = moveItemStackTo(stack, 2, 3, false);
            else if (stack.is(ModItems.FLUID_KARTUSCHE.get())) moved = moveItemStackTo(stack, 4, 5, false);
            else moved = moveItemStackTo(stack, 0, 2, false);
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
