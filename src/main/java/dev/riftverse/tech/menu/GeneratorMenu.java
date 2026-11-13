package dev.riftverse.tech.menu;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.registry.ModMenus;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.tech.BrennstoffGenerator;
import dev.riftverse.tech.GeneratorBlockEntity;
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

/** GUI der einfachen Generatoren: Kurbel-Dynamo, Tonnenreaktor, Biomasse-Kessel, Zeitschleifen-Solarpanel, Gravitations-Pendel. */
public class GeneratorMenu extends AbstractContainerMenu {
    public static final int FUEL_X = 56, FUEL_Y = 53, OUT_X = 116, OUT_Y = 35;
    public static final int BATTERY_X = 9, BATTERY_Y = 62;
    /** Batterie-Slot des Kurbel-Dynamos sitzt mittig unter der Schwung-Anzeige. */
    public static final int DYNAMO_BATTERY_X = 80, DYNAMO_BATTERY_Y = 53;

    @Nullable
    private final GeneratorBlockEntity generator;
    private final ContainerData data;
    private final boolean reactor;
    private final int machineSlots;

    public GeneratorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, generatorAt(inventory, buf), new SimpleContainerData(GeneratorBlockEntity.DATA_COUNT));
    }

    @Nullable
    private static GeneratorBlockEntity generatorAt(Inventory inventory, RegistryFriendlyByteBuf buf) {
        return inventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof GeneratorBlockEntity be ? be : null;
    }

    public GeneratorMenu(int containerId, Inventory inventory, @Nullable GeneratorBlockEntity generator, ContainerData data) {
        super(ModMenus.GENERATOR.get(), containerId);
        this.generator = generator;
        this.data = data;
        this.reactor = generator instanceof BrennstoffGenerator;
        IItemHandler handler = generator != null ? generator.items() : new ItemStackHandler(1);

        if (reactor) {
            addSlot(new SlotItemHandler(handler, BrennstoffGenerator.SLOT_FUEL, FUEL_X, FUEL_Y));
            addSlot(new SlotItemHandler(handler, BrennstoffGenerator.SLOT_OUT, OUT_X, OUT_Y));
            addSlot(new SlotItemHandler(handler, BrennstoffGenerator.SLOT_BATTERY, BATTERY_X, BATTERY_Y));
        } else {
            addSlot(new SlotItemHandler(handler, generator != null ? generator.batterySlot() : 0, DYNAMO_BATTERY_X, DYNAMO_BATTERY_Y));
        }
        machineSlots = slots.size();
        MenuHelper.addPlayerInventory(inventory, this::addSlot);
        addDataSlots(data);
    }

    /** Generator mit Brennstoff-Slot (Tonnenreaktor, Biomasse-Kessel). */
    public boolean isReactor() {
        return reactor;
    }

    /** Block-Entity auf dem Client: liefert Beschriftungen, aber keine Werte (die kommen über die Daten-Slots). */
    @Nullable
    public GeneratorBlockEntity generator() {
        return generator;
    }

    public int output() {
        return data.get(GeneratorBlockEntity.DATA_OUTPUT);
    }

    public int status() {
        return data.get(GeneratorBlockEntity.DATA_STATUS);
    }

    public VoltageTier tier() {
        return generator != null ? generator.tier() : VoltageTier.I;
    }

    public int energy() {
        return MenuHelper.joined(data, GeneratorBlockEntity.DATA_ENERGY_LO, GeneratorBlockEntity.DATA_ENERGY_HI);
    }

    public int capacity() {
        return Math.max(1, MenuHelper.joined(data, GeneratorBlockEntity.DATA_CAP_LO, GeneratorBlockEntity.DATA_CAP_HI));
    }

    public int work() {
        return data.get(GeneratorBlockEntity.DATA_WORK);
    }

    public int workMax() {
        return Math.max(1, data.get(GeneratorBlockEntity.DATA_WORK_MAX));
    }

    @Override
    public boolean stillValid(Player player) {
        return generator != null && !generator.isRemoved()
                && player.distanceToSqr(Vec3.atCenterOf(generator.getBlockPos())) <= 64.0;
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
            if (ItemEnergy.of(stack) != null) moved = moveItemStackTo(stack, machineSlots - 1, machineSlots, false);
            if (!moved && reactor && generator instanceof BrennstoffGenerator fuel && fuel.isFuel(stack)) {
                moved = moveItemStackTo(stack, BrennstoffGenerator.SLOT_FUEL, BrennstoffGenerator.SLOT_FUEL + 1, false);
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
