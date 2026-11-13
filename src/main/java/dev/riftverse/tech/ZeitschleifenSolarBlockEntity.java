package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Zeitschleifen-Solarpanel (T2): 120 FE/t, Stufe II. Liefert auch nachts, „leiht“ sich das Licht aber vom
 * nächsten Tag: Jeder Nacht-Tick wird zur Lichtschuld, die tagsüber mit halber Leistung abgezahlt wird.
 * Braucht freien Himmel; Regen halbiert die Leistung.
 */
public class ZeitschleifenSolarBlockEntity extends GeneratorBlockEntity {
    public static final int OUTPUT = 120;
    /** Höchstens eine ganze Nacht Schuld. */
    public static final int MAX_DEBT = 12_000;
    public static final int STATUS_OK = 0, STATUS_NO_SKY = 1;

    private int debt;
    private int current;
    private int status;

    public ZeitschleifenSolarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZEITSCHLEIFEN_SOLAR.get(), pos, state, 24_000, 1);
    }

    @Override
    protected boolean generate() {
        if (level == null) return false;
        current = 0;
        if (!level.canSeeSky(worldPosition.above())) {
            status = STATUS_NO_SKY;
            return false;
        }
        status = STATUS_OK;
        if (energy.isFull()) return false;
        int out;
        if (level.getSkyDarken() < 4) {   // Tag (funktioniert auch in Dimensionen mit fester Tageszeit)
            if (debt > 0) {
                debt--;
                out = OUTPUT / 2;   // die geliehene Nacht wird zurückgezahlt
            } else {
                out = OUTPUT;
            }
        } else {
            if (debt >= MAX_DEBT) return false;
            debt++;
            out = OUTPUT;
        }
        if (level.isRaining()) out /= 2;
        current = out;
        energy.add(out);
        setChanged();
        return true;
    }

    @Override
    public int outputPerTick() {
        return OUTPUT;
    }

    @Override
    protected int currentOutput() {
        return current;
    }

    @Override
    protected int status() {
        return status;
    }

    @Override
    public VoltageTier tier() {
        return VoltageTier.II;
    }

    @Override
    public int batterySlot() {
        return 0;
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return ItemEnergy.of(stack) != null;
    }

    /** Balken = Lichtschuld. */
    @Override
    protected int work() {
        return debt;
    }

    @Override
    protected int workMax() {
        return MAX_DEBT;
    }

    @Override
    public String barLabelKey() {
        return "gui.riftverse.lichtschuld";
    }

    @Override
    public List<Component> barTooltip(int work, int workMax, int output, int status) {
        if (status == STATUS_NO_SKY) return List.of(Component.translatable("tooltip.riftverse.solar_kein_himmel"));
        return List.of(Component.translatable("tooltip.riftverse.solar_schuld", work * 100 / Math.max(1, workMax)),
                Component.translatable("tooltip.riftverse.solar_regel"),
                Component.translatable("tooltip.riftverse.output", output));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Debt", debt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        debt = tag.getInt("Debt");
    }
}
