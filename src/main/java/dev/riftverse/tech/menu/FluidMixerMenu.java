package dev.riftverse.tech.menu;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModMenus;
import dev.riftverse.tech.FluidMixerBlockEntity;
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

/** GUI des Fluid-Mixers: Wassertank links, Ausgangstank rechts, Kartuschen ganz rechts. */
public class FluidMixerMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 54, INPUT_Y = 17;
    public static final int BUCKET_IN_X = 54, BUCKET_IN_Y = 45, BUCKET_OUT_X = 74, BUCKET_OUT_Y = 45;
    public static final int CART_IN_X = 130, CART_IN_Y = 17, CART_OUT_X = 130, CART_OUT_Y = 45;
    public static final int BATTERY_X = 9, BATTERY_Y = 62;
    public static final int WATER_X = 34, OUTPUT_X = 108, TANK_Y = 16, TANK_W = 12, TANK_H = 42;
    public static final int ARROW_X = 77, ARROW_Y = 18;

    @Nullable
    private final FluidMixerBlockEntity mixer;
    private final ContainerData data;
    private final int machineSlots;

    public FluidMixerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof FluidMixerBlockEntity be ? be : null,
                new SimpleContainerData(FluidMixerBlockEntity.DATA_COUNT));
    }

    public FluidMixerMenu(int containerId, Inventory inventory, @Nullable FluidMixerBlockEntity mixer, ContainerData data) {
        super(ModMenus.FLUID_MIXER.get(), containerId);
        this.mixer = mixer;
        this.data = data;
        IItemHandler h = mixer != null ? mixer.items() : new ItemStackHandler(FluidMixerBlockEntity.SLOTS);
        addSlot(new SlotItemHandler(h, FluidMixerBlockEntity.SLOT_INPUT, INPUT_X, INPUT_Y));
        addSlot(new SlotItemHandler(h, FluidMixerBlockEntity.SLOT_BUCKET_IN, BUCKET_IN_X, BUCKET_IN_Y));
        addSlot(new SlotItemHandler(h, FluidMixerBlockEntity.SLOT_BUCKET_OUT, BUCKET_OUT_X, BUCKET_OUT_Y));
        addSlot(new SlotItemHandler(h, FluidMixerBlockEntity.SLOT_CART_IN, CART_IN_X, CART_IN_Y));
        addSlot(new SlotItemHandler(h, FluidMixerBlockEntity.SLOT_CART_OUT, CART_OUT_X, CART_OUT_Y));
        addSlot(new SlotItemHandler(h, FluidMixerBlockEntity.SLOT_BATTERY, BATTERY_X, BATTERY_Y));
        machineSlots = slots.size();
        MenuHelper.addPlayerInventory(inventory, this::addSlot);
        addDataSlots(data);
    }

    public int progress() {
        return data.get(FluidMixerBlockEntity.DATA_PROGRESS);
    }

    public int maxProgress() {
        return Math.max(1, data.get(FluidMixerBlockEntity.DATA_MAX));
    }

    public int energy() {
        return MenuHelper.joined(data, FluidMixerBlockEntity.DATA_ENERGY_LO, FluidMixerBlockEntity.DATA_ENERGY_HI);
    }

    public int capacity() {
        return Math.max(1, MenuHelper.joined(data, FluidMixerBlockEntity.DATA_CAP_LO, FluidMixerBlockEntity.DATA_CAP_HI));
    }

    public int water() {
        return data.get(FluidMixerBlockEntity.DATA_WATER);
    }

    public int output() {
        return data.get(FluidMixerBlockEntity.DATA_OUTPUT);
    }

    /** Sekunden bis zum Zerfall, -1 = nichts im Tank. */
    public int restSeconds() {
        return data.get(FluidMixerBlockEntity.DATA_REST);
    }

    @Override
    public boolean stillValid(Player player) {
        return mixer != null && !mixer.isRemoved() && player.distanceToSqr(Vec3.atCenterOf(mixer.getBlockPos())) <= 64.0;
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
            if (ItemEnergy.of(stack) != null) moved = moveItemStackTo(stack, 5, 6, false);
            else if (stack.is(Items.WATER_BUCKET)) moved = moveItemStackTo(stack, 1, 2, false);
            else if (stack.is(ModItems.FLUID_KARTUSCHE.get()) || stack.is(ModItems.FLUID_KARTUSCHE_VOLL.get())) moved = moveItemStackTo(stack, 3, 4, false);
            else moved = moveItemStackTo(stack, 0, 1, false);
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
