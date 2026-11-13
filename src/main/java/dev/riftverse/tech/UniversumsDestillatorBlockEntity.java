package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.Overvoltage;
import dev.riftverse.energy.RiftEnergySink;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModItems;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Universums-Destillator (T3, 3.500 FE/t, Stufe III): destilliert aus einer gefüllten Universumsprobe die exklusive
 * Ressource ihres Universumstyps, ohne dass man hinreisen muss. Die Probe verbraucht sich langsam
 * ({@link #DURCHLAEUFE} Durchläufe). Bewusst ein Einzelblock: eine Graphit-Säule mit einer Kugel, in der eine Miniwelt schwebt.
 * <p>
 * Slots: 0 Probe, 1–4 Ausgänge, 5 Akku.
 */
public class UniversumsDestillatorBlockEntity extends IndustrieBlockEntity implements RiftEnergySink {
    public static final int SLOT_PROBE = 0, SLOT_OUT = 1, OUTPUTS = 4, SLOT_BATTERY = 5;
    public static final int CONSUMPTION = 3_500;
    public static final int TIME = 300;
    /** So viele Durchläufe hält eine Probe. */
    public static final int DURCHLAEUFE = 16;

    private int progress;

    public UniversumsDestillatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UNIVERSUMS_DESTILLATOR.get(), pos, state, CONSUMPTION * 400, 6);
    }

    /** Was ein Durchlauf aus einer Probe dieses Typs gewinnt; ohne Zufall (null) alles, was möglich ist. */
    public static List<ItemStack> ausbeute(UniverseType type, @Nullable RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        switch (type) {
            case SCHROTT -> {
                out.add(new ItemStack(ModItems.ALIEN_SCHROTT.get(), 4));
                if (random == null || random.nextFloat() < 0.25f) out.add(new ItemStack(ModItems.RAW_XENIT.get()));
            }
            case KRISTALL -> out.add(new ItemStack(ModItems.FLUXKRISTALL.get(), 2));
            case TOXISCH -> out.add(new ItemStack(ModItems.SAEUREGEL.get(), 2));
            case NIEDRIGGRAVITATION -> out.add(new ItemStack(ModItems.SCHWEBSTEIN_BROCKEN.get(), 4));
            case ZEITDILATATION -> out.add(new ItemStack(ModItems.CHRONOGLAS_SAND.get(), 2));
            case ORGANISCH -> out.add(new ItemStack(ModItems.BIOMASSE_PULPE.get(), 4));
            case HOCHGRAVITATION -> out.add(new ItemStack(ModItems.GRAVITIT.get()));
            case SPIEGEL -> out.add(new ItemStack(ModItems.INVERSIONSSPLITTER.get()));
            // Nullmaterie lässt sich nie automatisieren: aus der Leere kommt nur ihr Gestein
            case LEERE -> {
                out.add(new ItemStack(ModItems.LEERSTEIN.get(), 4));
                if (random == null || random.nextFloat() < 0.3f) out.add(new ItemStack(net.minecraft.world.item.Items.OBSIDIAN));
            }
            case GEZUECHTET -> { }
        }
        return out;
    }

    /** Ausbeute einer Probe: gezüchtete Universen liefern die Rohstoffe, die man für sie gewählt hat. */
    public static List<ItemStack> ausbeute(UniverseId id, @Nullable RandomSource random) {
        UniverseType type = id.type() == UniverseType.GEZUECHTET ? dev.riftverse.universe.Zucht.of(id).rohstoff() : id.type();
        return ausbeute(type, random);
    }

    @Nullable
    private UniverseId probe() {
        ItemStack p = items.getStackInSlot(SLOT_PROBE);
        return p.is(ModItems.UNIVERSUMSPROBE.get()) ? p.get(ModDataComponents.ZIEL.get()) : null;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        ItemEnergy.discharge(items.getStackInSlot(SLOT_BATTERY), energy, VoltageTier.III.maxPacket());
        UniverseId id = probe();
        boolean working = false;
        if (id != null && fits(ausbeute(id, null))) {
            if (energy.stored() >= CONSUMPTION) {
                energy.take(CONSUMPTION);
                progress++;
                working = true;
                if (progress >= TIME) {
                    progress = 0;
                    finish(id);
                }
                setChanged();
            }
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
        setActive(working);
    }

    /** Passt die größtmögliche Ausbeute in die Ausgänge? */
    private boolean fits(List<ItemStack> products) {
        List<ItemStack> outs = new ArrayList<>();
        for (int i = 0; i < OUTPUTS; i++) outs.add(items.getStackInSlot(SLOT_OUT + i).copy());
        for (ItemStack product : products) {
            ItemStack rest = product.copy();
            for (int i = 0; i < outs.size() && !rest.isEmpty(); i++) {
                ItemStack o = outs.get(i);
                if (o.isEmpty()) {
                    outs.set(i, rest);
                    rest = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(o, rest)) {
                    int move = Math.min(rest.getCount(), o.getMaxStackSize() - o.getCount());
                    o.grow(move);
                    rest.shrink(move);
                }
            }
            if (!rest.isEmpty()) return false;
        }
        return true;
    }

    private void finish(UniverseId id) {
        if (level == null) return;
        for (ItemStack product : ausbeute(id, level.random)) {
            ItemStack rest = product.copy();
            for (int i = 0; i < OUTPUTS && !rest.isEmpty(); i++) {
                ItemStack o = items.getStackInSlot(SLOT_OUT + i);
                if (o.isEmpty()) {
                    items.setStackInSlot(SLOT_OUT + i, rest);
                    rest = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(o, rest)) {
                    int move = Math.min(rest.getCount(), o.getMaxStackSize() - o.getCount());
                    if (move > 0) {
                        items.setStackInSlot(SLOT_OUT + i, o.copyWithCount(o.getCount() + move));
                        rest.shrink(move);
                    }
                }
            }
        }
        // Die Probe nutzt sich ab; nach 16 Durchläufen ist sie aufgebraucht
        ItemStack probe = items.getStackInSlot(SLOT_PROBE).copy();
        int used = probe.getOrDefault(ModDataComponents.VERSCHLEISS.get(), 0) + 1;
        if (used >= DURCHLAEUFE) {
            items.extractItem(SLOT_PROBE, 1, false);
        } else if (probe.getCount() == 1) {
            probe.set(ModDataComponents.VERSCHLEISS.get(), used);
            items.setStackInSlot(SLOT_PROBE, probe);
        } else {
            // Mehrere Proben im Slot: eine abgenutzte geht in einen freien Ausgang, sonst zählt der Stapel als eine
            ItemStack worn = probe.copyWithCount(1);
            worn.set(ModDataComponents.VERSCHLEISS.get(), used);
            for (int i = 0; i < OUTPUTS; i++) {
                if (items.getStackInSlot(SLOT_OUT + i).isEmpty()) {
                    items.extractItem(SLOT_PROBE, 1, false);
                    items.setStackInSlot(SLOT_OUT + i, worn);
                    break;
                }
            }
        }
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
        return List.of(GuiSlot.in(SLOT_PROBE, 48, 35), GuiSlot.out(SLOT_OUT, 112, 26), GuiSlot.out(SLOT_OUT + 1, 130, 26),
                GuiSlot.out(SLOT_OUT + 2, 112, 44), GuiSlot.out(SLOT_OUT + 3, 130, 44));
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
        return 76;
    }

    @Override
    protected int work() {
        return progress;
    }

    @Override
    protected int workMax() {
        return TIME;
    }

    /** Extra 0 = verbleibende Durchläufe der Probe (oder -1 ohne Probe). */
    @Override
    protected int extra(int index) {
        if (index != 0) return 0;
        ItemStack p = items.getStackInSlot(SLOT_PROBE);
        if (probe() == null) return -1;
        return DURCHLAEUFE - p.getOrDefault(ModDataComponents.VERSCHLEISS.get(), 0);
    }

    @Override
    public List<Component> infoLines(int[] extra) {
        if (extra[0] < 0) return List.of(Component.translatable("gui.riftverse.destillator_probe"));
        return List.of(Component.translatable("gui.riftverse.destillator_rest", extra[0], DURCHLAEUFE));
    }

    @Override
    public int infoY() {
        return 64;
    }

    @Override
    public List<Component> progressTooltip(int work, int workMax, int[] extra) {
        return List.of(Component.translatable("tooltip.riftverse.progress", work * 100 / Math.max(1, workMax)),
                Component.translatable("tooltip.riftverse.consumption", CONSUMPTION));
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        if (slot == SLOT_PROBE) return stack.is(ModItems.UNIVERSUMSPROBE.get()) && stack.has(ModDataComponents.ZIEL.get());
        if (slot == SLOT_BATTERY) return ItemEnergy.of(stack) != null;
        return false;
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getInt("Progress");
    }
}
