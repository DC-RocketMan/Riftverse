package dev.riftverse.tech;

import dev.riftverse.energy.RiftEnergyBlockEntity;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.tech.menu.IndustrieMenu;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Basis der T3-Maschinen und -Generatoren aus Meilenstein 4 (Materie-Rekonfigurator, Klon-Tank, Universums-Destillator,
 * Mikrokosmos-Zelle, Parallelzapfer) und der T4-Geräte aus Meilenstein 5 (Weltenformer, Singularitätskern). Alle teilen eine GUI ({@link IndustrieMenu}): Energiebalken links, Slots nach
 * dem Bauplan der Maschine, optional Fortschritt, Tank und ein paar Textzeilen.
 * <p>
 * Werte für die GUI laufen über {@link ContainerData} und werden nur als 16-Bit-Zahlen übertragen; große Werte
 * (Restlaufzeiten) liefern die Maschinen deshalb skaliert.
 */
public abstract class IndustrieBlockEntity extends RiftEnergyBlockEntity implements MenuProvider, ServerTickable, DropsContents {

    public static final int DATA_ENERGY_LO = 0, DATA_ENERGY_HI = 1, DATA_CAP_LO = 2, DATA_CAP_HI = 3, DATA_WORK = 4,
            DATA_WORK_MAX = 5, DATA_FLUID = 6, DATA_FLUID_CAP = 7, DATA_EXTRA = 8, EXTRAS = 6, DATA_COUNT = DATA_EXTRA + EXTRAS;

    /** Ein Slot in der GUI. @param input darf der Spieler (und ein Trichter) hineinlegen */
    public record GuiSlot(int index, int x, int y, boolean big, boolean input) {
        public static GuiSlot in(int index, int x, int y) {
            return new GuiSlot(index, x, y, false, true);
        }

        public static GuiSlot out(int index, int x, int y) {
            return new GuiSlot(index, x, y, false, false);
        }

        public static GuiSlot bigOut(int index, int x, int y) {
            return new GuiSlot(index, x, y, true, false);
        }
    }

    /** Knopf in der GUI; die Beschriftung kommt aus {@link #buttonLabel}. */
    public record GuiButton(int id, int x, int y, int width) {}

    public enum Progress { NONE, ARROW, BAR }

    /** Akku-Slot liegt bei allen T3-Geräten unter dem Energiebalken. */
    public static final int BATTERY_X = 9, BATTERY_Y = 62;

    protected final ItemStackHandler items;
    protected final ContainerData data;

    protected IndustrieBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int slots) {
        super(type, pos, state, capacity);
        this.items = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                IndustrieBlockEntity.this.onSlotChanged(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return IndustrieBlockEntity.this.isItemValid(slot, stack);
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
                    case DATA_FLUID -> fluidAmount();
                    case DATA_FLUID_CAP -> fluidCapacity();
                    default -> index >= DATA_EXTRA && index < DATA_COUNT ? extra(index - DATA_EXTRA) : 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Werte kommen nur vom Server
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    public ItemStackHandler items() {
        return items;
    }

    // ------------------------------------------------------------------ Bauplan der GUI (Server und Client)

    public abstract List<GuiSlot> guiSlots();

    /** Akku-Slot (Index im Inventar), oder -1. */
    public abstract int batterySlot();

    /** Spannungsstufe für den Tooltip des Energiebalkens. */
    public abstract VoltageTier guiTier();

    public Progress progress() {
        return Progress.ARROW;
    }

    /** Position des Fortschrittspfeils (bei {@link Progress#BAR}: links oben des Balkens, 96 × 6). */
    public int progressX() {
        return 80;
    }

    public int progressY() {
        return 35;
    }

    public boolean hasTank() {
        return false;
    }

    /** Farbe des Tanks in der GUI. */
    public int tankColor() {
        return 0xFF9A6BFF;
    }

    /** Übersetzungsschlüssel des Fluids im Tank (für den Tooltip). */
    public String tankFluidKey() {
        return "fluid_type.riftverse.portalfluid";
    }

    public List<GuiButton> guiButtons() {
        return List.of();
    }

    /** Läuft auf dem Client, nur aus den übertragenen Zusatzwerten. */
    public Component buttonLabel(int id, int[] extra) {
        return Component.empty();
    }

    /** Ein Knopf wurde auf dem Server gedrückt. */
    public void onButton(int id, Player player) {
    }

    /** Textzeilen unter den Slots (y = {@link #infoY()}); läuft auf dem Client. */
    public List<Component> infoLines(int[] extra) {
        return List.of();
    }

    public int infoY() {
        return 60;
    }

    public int infoX() {
        return 30;
    }

    /** Tooltip über Fortschritt oder Balken; läuft auf dem Client. */
    public List<Component> progressTooltip(int work, int workMax, int[] extra) {
        return List.of(Component.translatable("tooltip.riftverse.progress", work * 100 / Math.max(1, workMax)));
    }

    // ------------------------------------------------------------------ Werte für die GUI (Server)

    protected int work() {
        return 0;
    }

    protected int workMax() {
        return 1;
    }

    protected int fluidAmount() {
        return 0;
    }

    protected int fluidCapacity() {
        return 1;
    }

    /** Zusatzwert 0–5 für die GUI (höchstens 32.767). */
    protected int extra(int index) {
        return 0;
    }

    // ------------------------------------------------------------------ Inventar

    protected abstract boolean isItemValid(int slot, ItemStack stack);

    protected void onSlotChanged(int slot) {
    }

    private boolean isInput(int slot) {
        for (GuiSlot s : guiSlots()) {
            if (s.index() == slot) return s.input();
        }
        return false;
    }

    private boolean isOutput(int slot) {
        for (GuiSlot s : guiSlots()) {
            if (s.index() == slot) return !s.input();
        }
        return false;
    }

    /** Trichter und Rohre: rein nur in die Eingänge (nicht in den Akku-Slot), raus nur aus den Ausgängen. */
    public IItemHandler automationHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return items.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return slot != batterySlot() && isInput(slot) ? items.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return isOutput(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot != batterySlot() && isInput(slot) && items.isItemValid(slot, stack);
            }
        };
    }

    /** Legt {@code stack} in einen Ausgangsslot (zusammenführen oder leerer Slot). @return ob es ganz gepasst hat */
    protected boolean insertOutput(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return true;
        ItemStack out = items.getStackInSlot(slot);
        if (out.isEmpty()) {
            if (!simulate) items.setStackInSlot(slot, stack.copy());
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(out, stack) || out.getCount() + stack.getCount() > out.getMaxStackSize()) return false;
        if (!simulate) items.setStackInSlot(slot, out.copyWithCount(out.getCount() + stack.getCount()));
        return true;
    }

    protected void setActive(boolean active) {
        if (level == null) return;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(TechBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, items.getStackInSlot(i));
        }
    }

    // ------------------------------------------------------------------ Speichern, GUI

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

    public ContainerData containerData() {
        return data;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new IndustrieMenu(containerId, inventory, this, data);
    }
}
