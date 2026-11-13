package dev.riftverse.tech;

import dev.riftverse.Riftverse;
import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.Overvoltage;
import dev.riftverse.energy.RiftEnergySink;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Materie-Rekonfigurator (T3, 2.000 FE/t + 50 mB Portalfluid pro Vorgang, Stufe III): wandelt ein Material in ein anderes
 * derselben Wertstufe. Das Musterstück zeigt, was herauskommen soll, und wird nicht verbraucht.
 * <p>
 * Wertstufen sind Item-Tags {@code riftverse:wertstufe/*} (Erdmetalle, Rohstoffe der T1-, T2- und T3-Universen); im
 * Pack können weitere dazukommen. Spart Reisen, kostet aber viel.
 * <p>
 * Slots: 0 Material, 1 Muster, 2 Ergebnis, 3 volle Kartusche rein, 4 leere Kartusche raus, 5 Akku.
 */
public class MaterieRekonfiguratorBlockEntity extends IndustrieBlockEntity implements RiftEnergySink {
    public static final int SLOT_IN = 0, SLOT_MUSTER = 1, SLOT_OUT = 2, SLOT_CART_IN = 3, SLOT_CART_OUT = 4, SLOT_BATTERY = 5;
    public static final int CONSUMPTION = 2_000;
    public static final int FLUID_PER_OP = 50;
    public static final int TIME = 200;
    public static final int TANK = 4_000;

    /** Die Wertstufen; ein Material lässt sich nur innerhalb seiner Stufe umbauen. */
    public static final List<TagKey<Item>> WERTSTUFEN = List.of(
            tag("erdmetalle"), tag("universen_t1"), tag("universen_t2"), tag("universen_t3"));

    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, Riftverse.id("wertstufe/" + name));
    }

    private final PortalfluidSpeicher tank = new PortalfluidSpeicher(TANK, this::setChanged);
    private int progress;

    public MaterieRekonfiguratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATERIE_REKONFIGURATOR.get(), pos, state, CONSUMPTION * 400, 6);
    }

    public PortalfluidSpeicher tank() {
        return tank;
    }

    public IFluidHandler fluidHandler() {
        return tank.handler();
    }

    /** Gemeinsame Wertstufe von Material und Muster, oder null. */
    @Nullable
    public static TagKey<Item> gemeinsameStufe(ItemStack material, ItemStack muster) {
        if (material.isEmpty() || muster.isEmpty() || material.is(muster.getItem())) return null;
        for (TagKey<Item> t : WERTSTUFEN) {
            if (material.is(t) && muster.is(t)) return t;
        }
        return null;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        ItemEnergy.discharge(items.getStackInSlot(SLOT_BATTERY), energy, VoltageTier.III.maxPacket());
        tank.emptyCartridge(items, SLOT_CART_IN, SLOT_CART_OUT);

        ItemStack in = items.getStackInSlot(SLOT_IN);
        ItemStack muster = items.getStackInSlot(SLOT_MUSTER);
        boolean working = false;
        if (gemeinsameStufe(in, muster) != null && insertOutput(SLOT_OUT, new ItemStack(muster.getItem()), true)
                && tank.mb() >= FLUID_PER_OP) {
            if (energy.stored() >= CONSUMPTION) {
                energy.take(CONSUMPTION);
                progress++;
                working = true;
                if (progress >= TIME) {
                    progress = 0;
                    if (tank.take(FLUID_PER_OP)) {
                        items.extractItem(SLOT_IN, 1, false);
                        insertOutput(SLOT_OUT, new ItemStack(muster.getItem()), false);
                    }
                }
                setChanged();
            }
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
        setActive(working);
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getDemandedEnergy() {
        return energy.space();
    }

    @Override
    public VoltageTier getSinkTier() {
        return VoltageTier.III;
    }

    @Override
    public int injectEnergy(int amount, VoltageTier tier) {
        return energy.add(amount);
    }

    @Override
    public void onOvervoltage(VoltageTier tier) {
        if (level != null) Overvoltage.blowUp(level, worldPosition, tier);
    }

    // ------------------------------------------------------------------ GUI

    @Override
    public List<GuiSlot> guiSlots() {
        return List.of(GuiSlot.in(SLOT_IN, 36, 26), GuiSlot.in(SLOT_MUSTER, 36, 46), GuiSlot.bigOut(SLOT_OUT, 98, 35),
                GuiSlot.in(SLOT_CART_IN, 132, 18), GuiSlot.out(SLOT_CART_OUT, 132, 52));
    }

    @Override
    public int batterySlot() {
        return SLOT_BATTERY;
    }

    @Override
    public VoltageTier guiTier() {
        return VoltageTier.III;
    }

    @Override
    public int progressX() {
        return 64;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public int tankColor() {
        return 0xFF9A6BFF;
    }

    @Override
    protected int work() {
        return progress;
    }

    @Override
    protected int workMax() {
        return TIME;
    }

    @Override
    protected int fluidAmount() {
        return tank.mb();
    }

    @Override
    protected int fluidCapacity() {
        return tank.capacity();
    }

    @Override
    public int infoY() {
        return 70;
    }

    @Override
    public List<Component> progressTooltip(int work, int workMax, int[] extra) {
        return List.of(Component.translatable("tooltip.riftverse.progress", work * 100 / Math.max(1, workMax)),
                Component.translatable("tooltip.riftverse.consumption", CONSUMPTION),
                Component.translatable("tooltip.riftverse.rekonfigurator_regel", FLUID_PER_OP));
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_OUT, SLOT_CART_OUT -> false;
            case SLOT_CART_IN -> PortalfluidSpeicher.valueOf(stack) > 0;
            case SLOT_BATTERY -> ItemEnergy.of(stack) != null;
            default -> true;
        };
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.save(tag);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.load(tag);
        progress = tag.getInt("Progress");
    }
}
