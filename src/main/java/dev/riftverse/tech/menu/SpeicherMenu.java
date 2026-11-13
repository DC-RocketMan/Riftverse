package dev.riftverse.tech.menu;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.registry.ModMenus;
import dev.riftverse.tech.XenitZelleBlockEntity;
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

/** GUI der Xenit-Zelle: Füllstand, ein Slot zum Laden und einer zum Entladen von Items. */
public class SpeicherMenu extends AbstractContainerMenu {
    public static final int CHARGE_X = 62, DISCHARGE_X = 116, SLOT_Y = 48;

    @Nullable
    private final XenitZelleBlockEntity cell;
    private final ContainerData data;

    public SpeicherMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof XenitZelleBlockEntity be ? be : null,
                new SimpleContainerData(XenitZelleBlockEntity.DATA_COUNT));
    }

    public SpeicherMenu(int containerId, Inventory inventory, @Nullable XenitZelleBlockEntity cell, ContainerData data) {
        super(ModMenus.SPEICHER.get(), containerId);
        this.cell = cell;
        this.data = data;
        IItemHandler h = cell != null ? cell.items() : new ItemStackHandler(2);
        addSlot(new SlotItemHandler(h, XenitZelleBlockEntity.SLOT_CHARGE, CHARGE_X, SLOT_Y));
        addSlot(new SlotItemHandler(h, XenitZelleBlockEntity.SLOT_DISCHARGE, DISCHARGE_X, SLOT_Y));
        MenuHelper.addPlayerInventory(inventory, this::addSlot);
        addDataSlots(data);
    }

    /** Spannungsstufe des Speichers (Xenit-Zelle I, Flux-Zelle II). */
    public dev.riftverse.energy.VoltageTier tier() {
        return cell != null ? cell.tier() : dev.riftverse.energy.VoltageTier.I;
    }

    public int energy() {
        return MenuHelper.joined(data, XenitZelleBlockEntity.DATA_ENERGY_LO, XenitZelleBlockEntity.DATA_ENERGY_HI);
    }

    public int capacity() {
        return Math.max(1, MenuHelper.joined(data, XenitZelleBlockEntity.DATA_CAP_LO, XenitZelleBlockEntity.DATA_CAP_HI));
    }

    @Override
    public boolean stillValid(Player player) {
        return cell != null && !cell.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(cell.getBlockPos())) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = ItemEnergy.of(stack) != null && moveItemStackTo(stack, 0, 2, false);
            if (!moved && !MenuHelper.moveWithinPlayer(this, index, 2, (s, e, r) -> moveItemStackTo(stack, s, e, r))) {
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
