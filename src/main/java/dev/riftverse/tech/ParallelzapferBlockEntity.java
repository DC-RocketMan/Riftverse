package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.RiftEnergySource;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.tech.multiblock.Multiblock;
import dev.riftverse.tech.multiblock.MultiblockController;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Parallelzapfer (T3, Multiblock 3 × 3 × 3): ein Antennengestell über einem kleinen Riss zapft ein energiereiches
 * Paralleluniversum an. 10.000 FE/t – das ist <b>Stufe IV</b>: ohne Umspanner (III ↔ IV) davor grillt er die eigenen
 * T3-Maschinen. Verbraucht 5 mB Portalfluid pro Tick.
 * <ul>
 *   <li>Jede Quelle ist nach 3–5 Stunden Laufzeit erschöpft; dann muss der Zapfer neu ausgerichtet werden (1 Minute).</li>
 *   <li>Manchmal bemerkt die andere Seite den Diebstahl: Wächter kommen durch den Riss, der Zapfer setzt 30 Sekunden aus.</li>
 * </ul>
 * Slots: 0 volle Kartusche rein, 1 leere Kartusche raus, 2 Akku.
 */
public class ParallelzapferBlockEntity extends IndustrieBlockEntity implements RiftEnergySource, MultiblockController {
    public static final int SLOT_CART_IN = 0, SLOT_CART_OUT = 1, SLOT_BATTERY = 2;
    public static final int OUTPUT = 10_000;
    public static final int FLUID_PER_TICK = 5;
    public static final int TANK = 8_000;
    public static final int AUSRICHTEN = 1_200;
    public static final int PAUSE = 600;
    public static final int BUTTON_AUSRICHTEN = 0;
    public static final int STATUS_LAEUFT = 0, STATUS_UNVOLLSTAENDIG = 1, STATUS_KEIN_FLUID = 2, STATUS_ERSCHOEPFT = 3,
            STATUS_AUSRICHTEN = 4, STATUS_ENTDECKT = 5, STATUS_VOLL = 6;

    private final PortalfluidSpeicher tank = new PortalfluidSpeicher(TANK, this::setChanged);
    private boolean formed;
    private int checkIn;
    private long quelle = -1;
    private long quelleStart = 1;
    private int ausrichten;
    private int pause;
    private int status;

    public ParallelzapferBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARALLELZAPFER.get(), pos, state, OUTPUT * 40, 3);
    }

    public PortalfluidSpeicher tank() {
        return tank;
    }

    public IFluidHandler fluidHandler() {
        return tank.handler();
    }

    public boolean formed() {
        return formed;
    }

    public long quelle() {
        return quelle;
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(TechBlock.FACING) ? state.getValue(TechBlock.FACING) : Direction.NORTH;
    }

    @Nullable
    @Override
    public Component checkStructure() {
        if (level == null) return null;
        Component missing = Multiblock.PARALLELZAPFER.missingMessage(level, worldPosition, facing());
        formed = missing == null;
        checkIn = 20;
        return missing;
    }

    /** Neue Quelle: 3–5 Stunden Laufzeit. */
    private void neueQuelle() {
        if (level == null) return;
        quelle = 3L * 72_000 + level.random.nextInt(2 * 72_000);
        quelleStart = quelle;
        setChanged();
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        if (quelle < 0) neueQuelle();
        if (--checkIn <= 0) checkStructure();
        tank.emptyCartridge(items, SLOT_CART_IN, SLOT_CART_OUT);

        boolean running = false;
        if (!formed) {
            status = STATUS_UNVOLLSTAENDIG;
        } else if (ausrichten > 0) {
            status = STATUS_AUSRICHTEN;
            if (--ausrichten == 0) neueQuelle();
        } else if (quelle <= 0) {
            status = STATUS_ERSCHOEPFT;
        } else if (pause > 0) {
            status = STATUS_ENTDECKT;
            pause--;
        } else if (energy.space() < OUTPUT) {
            status = STATUS_VOLL;
        } else if (!tank.take(FLUID_PER_TICK)) {
            status = STATUS_KEIN_FLUID;
        } else {
            status = STATUS_LAEUFT;
            energy.add(OUTPUT);
            quelle--;
            running = true;
            if (quelle == 0) erschoepft();
            else if (level.getGameTime() % 1_200 == 0 && level.random.nextFloat() < 0.04f) entdeckt();
            effekte();
            setChanged();
        }
        ItemEnergy.charge(items.getStackInSlot(SLOT_BATTERY), energy, VoltageTier.III.maxPacket());
        setActive(running || status == STATUS_VOLL);
    }

    private Vec3 riss() {
        return Vec3.atCenterOf(Multiblock.at(worldPosition, facing(), new Multiblock.Part(0, 1, 1, s -> true, "")));
    }

    private void effekte() {
        if (!(level instanceof ServerLevel sl) || level.getGameTime() % 10 != 0) return;
        Vec3 c = riss();
        sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 3, 0.15, 0.25, 0.15, 0.01);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 1.2, c.z, 6, 0.9, 0.3, 0.9, 0.05);
    }

    private void erschoepft() {
        melden("message.riftverse.zapfer.erschoepft", ChatFormatting.GOLD);
    }

    /** Die andere Seite hat den Diebstahl bemerkt: Wächter kommen durch den Riss. */
    private void entdeckt() {
        if (!(level instanceof ServerLevel sl)) return;
        pause = PAUSE;
        Vec3 c = riss();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.moveTo(c.x, c.y + 1, c.z);
            sl.addFreshEntity(bolt);
        }
        for (int i = 0; i < 3; i++) {
            Vex vex = EntityType.VEX.create(sl);
            if (vex == null) continue;
            vex.moveTo(c.x + (sl.random.nextDouble() - 0.5) * 2, c.y + 1.5, c.z + (sl.random.nextDouble() - 0.5) * 2, 0f, 0f);
            vex.setLimitedLife(20 * 40);
            sl.addFreshEntity(vex);
        }
        sl.playSound(null, worldPosition, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.BLOCKS, 1.0f, 0.6f);
        melden("message.riftverse.zapfer.entdeckt", ChatFormatting.RED);
    }

    private void melden(String key, ChatFormatting color) {
        if (!(level instanceof ServerLevel sl)) return;
        Component msg = Component.translatable(key).withStyle(color);
        for (Player p : sl.players()) {
            if (p.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 48 * 48) p.displayClientMessage(msg, false);
        }
    }

    @Override
    public void onButton(int id, Player player) {
        if (id == BUTTON_AUSRICHTEN && ausrichten == 0) {
            ausrichten = AUSRICHTEN;
            quelle = 0;
            setChanged();
        }
    }

    // ------------------------------------------------------------------ Netz: Stufe IV

    @Override
    public int getOfferedEnergy() {
        return Math.min(energy.stored(), OUTPUT);
    }

    @Override
    public VoltageTier getSourceTier() {
        return VoltageTier.IV;
    }

    @Override
    public void drawEnergy(int amount) {
        energy.take(amount);
    }

    // ------------------------------------------------------------------ GUI

    @Override
    public List<GuiSlot> guiSlots() {
        return List.of(GuiSlot.in(SLOT_CART_IN, 132, 18), GuiSlot.out(SLOT_CART_OUT, 132, 52));
    }

    @Override
    public int batterySlot() {
        return SLOT_BATTERY;
    }

    @Override
    public VoltageTier guiTier() {
        return VoltageTier.IV;
    }

    @Override
    public Progress progress() {
        return Progress.BAR;
    }

    @Override
    public int progressX() {
        return 30;
    }

    @Override
    public int progressY() {
        return 20;
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
        return (int) Math.min(30_000, Math.max(0, quelle) / 20);
    }

    @Override
    protected int workMax() {
        return (int) Math.max(1, Math.min(30_000, quelleStart / 20));
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
    protected int extra(int index) {
        return switch (index) {
            case 0 -> status;
            case 1 -> (int) Math.min(30_000, Math.max(0, quelle) / 1_200);
            case 2 -> ausrichten / 20;
            default -> 0;
        };
    }

    @Override
    public List<GuiButton> guiButtons() {
        return List.of(new GuiButton(BUTTON_AUSRICHTEN, 30, 50, 96));
    }

    @Override
    public Component buttonLabel(int id, int[] extra) {
        return extra[0] == STATUS_AUSRICHTEN
                ? Component.translatable("gui.riftverse.zapfer.richtet_aus", extra[2])
                : Component.translatable("gui.riftverse.zapfer.ausrichten");
    }

    @Override
    public int infoY() {
        return 31;
    }

    @Override
    public List<Component> infoLines(int[] extra) {
        ChatFormatting color = extra[0] == STATUS_LAEUFT || extra[0] == STATUS_VOLL ? ChatFormatting.DARK_GREEN : ChatFormatting.RED;
        return List.of(Component.translatable("gui.riftverse.zapfer.status." + extra[0]).withStyle(color));
    }

    @Override
    public List<Component> progressTooltip(int work, int workMax, int[] extra) {
        return List.of(Component.translatable("tooltip.riftverse.zapfer.quelle", extra[1] / 60, extra[1] % 60),
                Component.translatable("tooltip.riftverse.output", extra[0] == STATUS_LAEUFT ? OUTPUT : 0),
                Component.translatable("tooltip.riftverse.zapfer.regel", FLUID_PER_TICK));
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_CART_IN -> PortalfluidSpeicher.valueOf(stack) > 0;
            case SLOT_BATTERY -> ItemEnergy.of(stack) != null;
            default -> false;
        };
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.save(tag);
        tag.putLong("Quelle", quelle);
        tag.putLong("QuelleStart", quelleStart);
        tag.putInt("Ausrichten", ausrichten);
        tag.putInt("Pause", pause);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.load(tag);
        quelle = tag.contains("Quelle") ? tag.getLong("Quelle") : -1;
        quelleStart = Math.max(1, tag.getLong("QuelleStart"));
        ausrichten = tag.getInt("Ausrichten");
        pause = tag.getInt("Pause");
    }
}
