package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.Overvoltage;
import dev.riftverse.energy.RiftEnergySink;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModFluids;
import dev.riftverse.registry.ModItems;
import dev.riftverse.tech.multiblock.Multiblock;
import dev.riftverse.tech.multiblock.MultiblockController;
import dev.riftverse.universe.MultiverseData;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import dev.riftverse.universe.UniverseType;
import dev.riftverse.universe.Zucht;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * Weltenformer (T4, Ring 7 × 7 × 3 aus Null-Hülle): 40.000 FE/t auf Stufe IV und Portalfluid-Konzentrat. Zwei Modi:
 * <ul>
 *   <li><b>Essenz:</b> leere Kartusche + Inversionssplitter + 2.000 mB Konzentrat → Kartusche Weltenformer-Essenz
 *       (Treibstoff der Kanone Mk IV; die Mk III springt damit zufällig in die Leere Welt).</li>
 *   <li><b>Universum:</b> Zwei Universumsproben geben Landschaft und Rohstoffe vor, Schwerkraft und Gefahren stellt
 *       man an den Knöpfen ein. Papier + 4.000 mB Konzentrat + eine Minute → Koordinatenkarte eines neu gezüchteten
 *       Universums. Die Proben bleiben liegen.</li>
 * </ul>
 * Slots: 0 Konzentrat-Kartusche rein, 1 leere Kartusche raus, 2 Probe Landschaft, 3 Probe Rohstoffe,
 * 4 leere Kartusche oder Papier, 5 Inversionssplitter, 6 Ausgang, 7 Akku.
 */
public class WeltenformerBlockEntity extends IndustrieBlockEntity implements RiftEnergySink, MultiblockController {
    public static final int SLOT_KONZ_IN = 0, SLOT_KONZ_OUT = 1, SLOT_PROBE_A = 2, SLOT_PROBE_B = 3, SLOT_ROH = 4,
            SLOT_ZUTAT = 5, SLOT_OUT = 6, SLOT_BATTERY = 7;
    public static final int CONSUMPTION = 40_000;
    public static final int TANK = 16_000;
    public static final int ESSENZ_KONZENTRAT = 2_000, UNIVERSUM_KONZENTRAT = 4_000;
    public static final int ESSENZ_TIME = 300, UNIVERSUM_TIME = 1_200;
    public static final int MODE_ESSENZ = 0, MODE_UNIVERSUM = 1;
    public static final int BUTTON_MODUS = 0, BUTTON_SCHWERKRAFT = 1, BUTTON_GEFAHREN = 2;
    public static final int STATUS_LAEUFT = 0, STATUS_UNVOLLSTAENDIG = 1, STATUS_KONZENTRAT = 2, STATUS_ZUTATEN = 3,
            STATUS_PROBEN = 4, STATUS_VOLL = 5, STATUS_ENERGIE = 6;

    private int konzentrat;
    private int mode = MODE_ESSENZ;
    private int schwerkraft = 1;
    private boolean gefahren = true;
    private int progress;
    private boolean formed;
    private int checkIn;
    private int status = STATUS_UNVOLLSTAENDIG;
    /** Zuletzt erschaffenes Universum (für die Meldung und den Rauchtest). */
    @Nullable
    private UniverseId letztes;

    public WeltenformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WELTENFORMER.get(), pos, state, CONSUMPTION * 100, 8);
    }

    public int konzentrat() {
        return konzentrat;
    }

    public int mode() {
        return mode;
    }

    public boolean formed() {
        return formed;
    }

    @Nullable
    public UniverseId letztes() {
        return letztes;
    }

    public void setParameter(int mode, int schwerkraft, boolean gefahren) {
        this.mode = mode;
        this.schwerkraft = Math.floorMod(schwerkraft, Zucht.GRAVITATION.length);
        this.gefahren = gefahren;
        progress = 0;
        setChanged();
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(TechBlock.FACING) ? state.getValue(TechBlock.FACING) : Direction.NORTH;
    }

    @Nullable
    @Override
    public Component checkStructure() {
        if (level == null) return null;
        Component missing = Multiblock.WELTENFORMER.missingMessage(level, worldPosition, facing());
        formed = missing == null;
        checkIn = 20;
        return missing;
    }

    // ------------------------------------------------------------------ Bauplan aus den Proben

    /** Grundtyp, den eine Probe beisteuert (gezüchtete Proben geben ihre eigene Wahl weiter), oder null. */
    @Nullable
    private static UniverseType grundtyp(ItemStack probe, boolean landschaft) {
        if (!probe.is(ModItems.UNIVERSUMSPROBE.get())) return null;
        UniverseId id = probe.get(ModDataComponents.ZIEL.get());
        if (id == null) return null;
        if (id.type() == UniverseType.GEZUECHTET) {
            Zucht z = Zucht.of(id);
            return landschaft ? z.landschaft() : z.rohstoff();
        }
        return Zucht.BASIS.contains(id.type()) ? id.type() : null;
    }

    /** Bauplan aus Proben und Knöpfen, oder null, wenn eine Probe fehlt oder nicht taugt. */
    @Nullable
    public Zucht zucht() {
        UniverseType a = grundtyp(items.getStackInSlot(SLOT_PROBE_A), true);
        UniverseType b = grundtyp(items.getStackInSlot(SLOT_PROBE_B), false);
        if (a == null || b == null) return null;
        return new Zucht(a, b, schwerkraft, gefahren);
    }

    // ------------------------------------------------------------------ Tick

    @Override
    public void serverTick() {
        if (level == null) return;
        ItemEnergy.discharge(items.getStackInSlot(SLOT_BATTERY), energy, Integer.MAX_VALUE);
        if (--checkIn <= 0) checkStructure();
        fillFromCartridge();

        boolean working = false;
        int need = mode == MODE_ESSENZ ? ESSENZ_KONZENTRAT : UNIVERSUM_KONZENTRAT;
        int time = mode == MODE_ESSENZ ? ESSENZ_TIME : UNIVERSUM_TIME;
        ItemStack product = product();
        if (!formed) {
            status = STATUS_UNVOLLSTAENDIG;
        } else if (mode == MODE_UNIVERSUM && zucht() == null) {
            status = STATUS_PROBEN;
        } else if (!hasIngredients()) {
            status = STATUS_ZUTATEN;
        } else if (konzentrat < need) {
            status = STATUS_KONZENTRAT;
        } else if (!insertOutput(SLOT_OUT, product, true)) {
            status = STATUS_VOLL;
        } else if (energy.stored() < CONSUMPTION) {
            status = STATUS_ENERGIE;
        } else {
            status = STATUS_LAEUFT;
            energy.take(CONSUMPTION);
            working = true;
            if (++progress >= time) {
                progress = 0;
                finish(need);
            }
            effekte();
            setChanged();
        }
        if (!working && progress != 0 && status != STATUS_ENERGIE) {
            progress = 0;
            setChanged();
        }
        setActive(working);
    }

    private boolean hasIngredients() {
        ItemStack roh = items.getStackInSlot(SLOT_ROH);
        if (mode == MODE_ESSENZ) {
            return roh.is(ModItems.FLUID_KARTUSCHE.get()) && items.getStackInSlot(SLOT_ZUTAT).is(ModItems.INVERSIONSSPLITTER.get());
        }
        return roh.is(Items.PAPER);
    }

    /** Was beim nächsten Abschluss herauskommt (für den Platz-Test; die Karte bekommt ihr Ziel erst am Ende). */
    private ItemStack product() {
        return mode == MODE_ESSENZ ? new ItemStack(ModItems.FLUID_KARTUSCHE_ESSENZ.get()) : new ItemStack(ModItems.KOORDINATENKARTE.get());
    }

    private void finish(int need) {
        if (!(level instanceof ServerLevel sl)) return;
        konzentrat -= need;
        items.extractItem(SLOT_ROH, 1, false);
        if (mode == MODE_ESSENZ) {
            items.extractItem(SLOT_ZUTAT, 1, false);
            insertOutput(SLOT_OUT, new ItemStack(ModItems.FLUID_KARTUSCHE_ESSENZ.get()), false);
            sl.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 0.5f);
            return;
        }
        Zucht z = zucht();
        if (z == null) return;
        UniverseId id = erschaffen(sl, z);
        ItemStack karte = new ItemStack(ModItems.KOORDINATENKARTE.get());
        karte.set(ModDataComponents.ZIEL.get(), id);
        insertOutput(SLOT_OUT, karte, false);
        letztes = id;

        Vec3 c = mitte();
        sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 80, 1.2, 1.2, 1.2, 0.15);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 120, 1.5, 1.5, 1.5, 0.2);
        sl.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.6f);
        Component msg = Component.translatable("message.riftverse.weltenformer.erschaffen", "Universum " + id.code(),
                UniverseProfile.get(id).name).withStyle(ChatFormatting.LIGHT_PURPLE);
        for (Player p : sl.players()) {
            if (p.distanceToSqr(c) < 32 * 32) p.displayClientMessage(msg, false);
        }
    }

    /** Vergibt die nächste Nummer dieser Bauart und gibt das neue Universum zurück. */
    public static UniverseId erschaffen(ServerLevel level, Zucht zucht) {
        return zucht.id(MultiverseData.get(level.getServer()).nextZucht(zucht));
    }

    private Vec3 mitte() {
        return Vec3.atCenterOf(Multiblock.at(worldPosition, facing(), Multiblock.WELTENFORMER_MITTE));
    }

    /** Hologramm-Planet: eine Partikelkugel in der Mitte des Rings, die sich nach den Parametern verformt. */
    private void effekte() {
        if (!(level instanceof ServerLevel sl) || level.getGameTime() % 4 != 0) return;
        Vec3 c = mitte();
        double t = level.getGameTime() * 0.05;
        Zucht z = mode == MODE_UNIVERSUM ? zucht() : null;
        double flat = z == null ? 1.0 : z.schwerkraft() == 2 ? 0.55 : z.schwerkraft() == 0 ? 1.35 : 1.0;
        Vector3f color = z == null ? new Vector3f(0.8f, 0.4f, 1.0f) : switch (z.landschaft()) {
            case SCHROTT -> new Vector3f(0.6f, 0.35f, 0.2f);
            case KRISTALL -> new Vector3f(0.4f, 1.0f, 0.6f);
            case TOXISCH -> new Vector3f(0.6f, 0.9f, 0.1f);
            case NIEDRIGGRAVITATION -> new Vector3f(0.7f, 0.85f, 1.0f);
            case ZEITDILATATION -> new Vector3f(0.95f, 0.7f, 0.2f);
            case ORGANISCH -> new Vector3f(0.95f, 0.4f, 0.6f);
            case HOCHGRAVITATION -> new Vector3f(0.3f, 0.4f, 0.8f);
            default -> new Vector3f(0.9f, 0.95f, 1.0f);
        };
        DustParticleOptions dust = new DustParticleOptions(color, 1.2f);
        for (int i = 0; i < 10; i++) {
            double phi = t + i * 0.628, theta = Math.acos(1 - 2 * ((i * 0.618 + t * 0.1) % 1.0));
            double x = Math.sin(theta) * Math.cos(phi), y = Math.cos(theta) * flat, zz = Math.sin(theta) * Math.sin(phi);
            sl.sendParticles(dust, c.x + x * 1.4, c.y + y * 1.4, c.z + zz * 1.4, 1, 0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ Konzentrat

    private void fillFromCartridge() {
        ItemStack cart = items.getStackInSlot(SLOT_KONZ_IN);
        if (!cart.is(ModItems.FLUID_KARTUSCHE_KONZENTRAT.get())) return;
        int value = dev.riftverse.item.FluidKartuscheItem.INHALT;
        if (TANK - konzentrat < value) return;
        ItemStack o = items.getStackInSlot(SLOT_KONZ_OUT);
        if (!o.isEmpty() && (!o.is(ModItems.FLUID_KARTUSCHE.get()) || o.getCount() >= o.getMaxStackSize())) return;
        items.extractItem(SLOT_KONZ_IN, 1, false);
        if (o.isEmpty()) items.setStackInSlot(SLOT_KONZ_OUT, new ItemStack(ModItems.FLUID_KARTUSCHE.get()));
        else items.setStackInSlot(SLOT_KONZ_OUT, o.copyWithCount(o.getCount() + 1));
        konzentrat += value;
        setChanged();
    }

    /** Rohre: nur Portalfluid-Konzentrat hinein. */
    public IFluidHandler fluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return konzentrat > 0 ? new FluidStack(ModFluids.PORTALFLUID_KONZENTRAT.get(), konzentrat) : FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return TANK;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return stack.is(ModFluids.PORTALFLUID_KONZENTRAT.get());
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
                int accepted = Math.min(resource.getAmount(), TANK - konzentrat);
                if (accepted > 0 && action.execute()) {
                    konzentrat += accepted;
                    setChanged();
                }
                return accepted;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getDemandedEnergy() {
        return energy.space();
    }

    @Override
    public VoltageTier getSinkTier() {
        return VoltageTier.IV;
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
    public void onButton(int id, Player player) {
        switch (id) {
            case BUTTON_MODUS -> mode = mode == MODE_ESSENZ ? MODE_UNIVERSUM : MODE_ESSENZ;
            case BUTTON_SCHWERKRAFT -> schwerkraft = (schwerkraft + 1) % Zucht.GRAVITATION.length;
            case BUTTON_GEFAHREN -> gefahren = !gefahren;
            default -> { return; }
        }
        progress = 0;
        setChanged();
    }

    @Override
    public List<GuiSlot> guiSlots() {
        return List.of(GuiSlot.in(SLOT_PROBE_A, 30, 17), GuiSlot.in(SLOT_PROBE_B, 48, 17), GuiSlot.in(SLOT_ROH, 70, 17),
                GuiSlot.in(SLOT_ZUTAT, 88, 17), GuiSlot.out(SLOT_OUT, 110, 17), GuiSlot.in(SLOT_KONZ_IN, 132, 17),
                GuiSlot.out(SLOT_KONZ_OUT, 132, 37));
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
        return 38;
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public int tankColor() {
        return 0xFF2AA8C0;
    }

    @Override
    public String tankFluidKey() {
        return "fluid_type.riftverse.portalfluid_konzentrat";
    }

    @Override
    protected int work() {
        return progress;
    }

    @Override
    protected int workMax() {
        return mode == MODE_ESSENZ ? ESSENZ_TIME : UNIVERSUM_TIME;
    }

    @Override
    protected int fluidAmount() {
        return konzentrat;
    }

    @Override
    protected int fluidCapacity() {
        return TANK;
    }

    @Override
    protected int extra(int index) {
        return switch (index) {
            case 0 -> status;
            case 1 -> mode;
            case 2 -> schwerkraft;
            case 3 -> gefahren ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    public List<GuiButton> guiButtons() {
        return List.of(new GuiButton(BUTTON_MODUS, 30, 57, 40), new GuiButton(BUTTON_SCHWERKRAFT, 72, 57, 36),
                new GuiButton(BUTTON_GEFAHREN, 110, 57, 42));
    }

    @Override
    public Component buttonLabel(int id, int[] extra) {
        return switch (id) {
            case BUTTON_MODUS -> Component.translatable(extra[1] == MODE_ESSENZ ? "gui.riftverse.weltenformer.essenz"
                    : "gui.riftverse.weltenformer.universum");
            case BUTTON_SCHWERKRAFT -> Component.translatable("gui.riftverse.weltenformer.g" + Math.floorMod(extra[2], 3));
            default -> Component.translatable(extra[3] == 1 ? "gui.riftverse.weltenformer.gefahr_an" : "gui.riftverse.weltenformer.gefahr_aus");
        };
    }

    @Override
    public int infoY() {
        return 46;
    }

    @Override
    public List<Component> infoLines(int[] extra) {
        ChatFormatting color = extra[0] == STATUS_LAEUFT ? ChatFormatting.DARK_GREEN : ChatFormatting.RED;
        return List.of(Component.translatable("gui.riftverse.weltenformer.status." + extra[0]).withStyle(color));
    }

    @Override
    public List<Component> progressTooltip(int work, int workMax, int[] extra) {
        boolean essenz = extra[1] == MODE_ESSENZ;
        return List.of(Component.translatable("tooltip.riftverse.progress", work * 100 / Math.max(1, workMax)),
                Component.translatable(essenz ? "tooltip.riftverse.weltenformer.essenz" : "tooltip.riftverse.weltenformer.universum",
                        essenz ? ESSENZ_KONZENTRAT : UNIVERSUM_KONZENTRAT),
                Component.translatable("tooltip.riftverse.consumption", CONSUMPTION));
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_KONZ_IN -> stack.is(ModItems.FLUID_KARTUSCHE_KONZENTRAT.get());
            case SLOT_PROBE_A, SLOT_PROBE_B -> stack.is(ModItems.UNIVERSUMSPROBE.get());
            case SLOT_ROH -> stack.is(ModItems.FLUID_KARTUSCHE.get()) || stack.is(Items.PAPER);
            case SLOT_ZUTAT -> stack.is(ModItems.INVERSIONSSPLITTER.get());
            case SLOT_BATTERY -> ItemEnergy.of(stack) != null;
            default -> false;
        };
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Konzentrat", konzentrat);
        tag.putInt("Modus", mode);
        tag.putInt("Schwerkraft", schwerkraft);
        tag.putBoolean("Gefahren", gefahren);
        tag.putInt("Progress", progress);
        if (letztes != null) tag.put("Letztes", letztes.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        konzentrat = Math.max(0, Math.min(TANK, tag.getInt("Konzentrat")));
        mode = tag.getInt("Modus") == MODE_UNIVERSUM ? MODE_UNIVERSUM : MODE_ESSENZ;
        schwerkraft = tag.contains("Schwerkraft") ? Math.floorMod(tag.getInt("Schwerkraft"), 3) : 1;
        gefahren = !tag.contains("Gefahren") || tag.getBoolean("Gefahren");
        progress = tag.getInt("Progress");
        letztes = tag.contains("Letztes") ? UniverseId.load(tag.getCompound("Letztes")) : null;
    }
}
