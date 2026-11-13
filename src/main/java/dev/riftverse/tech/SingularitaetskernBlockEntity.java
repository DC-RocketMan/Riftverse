package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.RiftEnergySource;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.tech.multiblock.Multiblock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Singularitätskern (T4): ein kleines schwarzes Loch im Keller. Gezündet liefert er 100.000 FE/t auf Stufe IV und
 * frisst dafür {@link #HUNGER} beliebige Items pro Sekunde aus seinen neun Futterslots (Trichter und Rohre
 * willkommen). Zu wenig Futter heißt anteilig weniger Strom.
 * <ul>
 *   <li>Hungert er {@link #GEDULD} Sekunden, beginnt er zu wachsen: alle {@link #WACHSTUM_TAKT} Sekunden eine Stufe
 *       (bis {@link #MAX_WACHSTUM}). Gewachsen saugt er Gegenstände, Mobs und Spieler an und frisst Blöcke im Umkreis
 *       – keine Maschinen und nichts Unzerstörbares. Gefressene Blöcke und Gegenstände zählen als Futter.</li>
 *   <li>Notabschaltung (Knopf oder Redstone-Signal) hält ihn sofort an; danach braucht er eine Minute, bis er sich
 *       wieder zünden lässt.</li>
 *   <li>Mit dem optionalen Eindämmungsring (5 × 5 × 5) wächst er nie: ohne Futter schaltet er sich sicher ab.</li>
 * </ul>
 * Slots: 0–8 Futter, 9 Akku (wird geladen).
 */
public class SingularitaetskernBlockEntity extends IndustrieBlockEntity implements RiftEnergySource {
    public static final int FUTTER = 9, SLOT_BATTERY = 9;
    public static final int OUTPUT = 100_000;
    public static final int HUNGER = 64;
    public static final int GEDULD = 5, WACHSTUM_TAKT = 10, MAX_WACHSTUM = 6;
    public static final int ABKUEHLEN = 1_200;
    public static final int BUTTON_ZUENDEN = 0;
    public static final int STATUS_AUS = 0, STATUS_LAEUFT = 1, STATUS_HUNGRIG = 2, STATUS_WAECHST = 3, STATUS_ABKUEHLEN = 4,
            STATUS_SICHER_AUS = 5, STATUS_VOLL = 6;

    private boolean gezuendet;
    /** Futter in dieser Sekunde (0–64), daraus folgt die Leistung der nächsten 20 Ticks. */
    private int satt;
    private int hunger;
    private int wachstum;
    private int abkuehlen;
    private boolean ring;
    private int status;
    private int leistung;

    public SingularitaetskernBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SINGULARITAETSKERN.get(), pos, state, OUTPUT * 20, FUTTER + 1);
    }

    public boolean gezuendet() {
        return gezuendet;
    }

    public int wachstum() {
        return wachstum;
    }

    public int hunger() {
        return hunger;
    }

    public boolean ring() {
        return ring;
    }

    public int leistung() {
        return leistung;
    }

    /** Zünden (wenn abgekühlt) oder Notabschaltung. */
    public void schalten() {
        if (gezuendet) notabschaltung();
        else if (abkuehlen == 0) {
            gezuendet = true;
            hunger = 0;
            satt = HUNGER;
            if (level != null) level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.2f, 0.5f);
            setChanged();
        }
    }

    public void notabschaltung() {
        if (!gezuendet) return;
        gezuendet = false;
        wachstum = 0;
        hunger = 0;
        abkuehlen = ABKUEHLEN;
        if (level != null) level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 1.5f, 0.6f);
        melden("message.riftverse.singularitaet.notaus", ChatFormatting.GOLD);
        setChanged();
    }

    // ------------------------------------------------------------------ Tick

    @Override
    public void serverTick() {
        if (level == null) return;
        long t = level.getGameTime();
        if (abkuehlen > 0) abkuehlen--;
        if (t % 20 == 0) ring = Multiblock.ringMissing(level, worldPosition) == null;
        if (gezuendet && level.hasNeighborSignal(worldPosition)) notabschaltung();

        if (!gezuendet) {
            leistung = 0;
            status = abkuehlen > 0 ? STATUS_ABKUEHLEN : status == STATUS_SICHER_AUS ? STATUS_SICHER_AUS : STATUS_AUS;
        } else {
            if (t % 20 == 0) sekunde();
            if (!gezuendet) {
                leistung = 0;
            } else {
                leistung = (int) ((long) OUTPUT * satt / HUNGER);
                if (energy.space() < leistung) status = STATUS_VOLL;
                energy.add(leistung);
                if (wachstum > 0 && t % 5 == 0) ansaugen();
                if (t % 10 == 0) effekte();
            }
            setChanged();
        }
        ItemEnergy.charge(items.getStackInSlot(SLOT_BATTERY), energy, Integer.MAX_VALUE);
        setActive(gezuendet);
    }

    /** Einmal pro Sekunde: 64 Items fressen, sonst hungern und irgendwann wachsen. */
    private void sekunde() {
        int gegessen = 0;
        for (int i = 0; i < FUTTER && gegessen < HUNGER; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.isEmpty()) continue;
            int n = Math.min(s.getCount(), HUNGER - gegessen);
            items.extractItem(i, n, false);
            gegessen += n;
        }
        if (wachstum > 0) gegessen += blockeFressen(HUNGER - gegessen);
        satt = Math.min(HUNGER, gegessen);

        if (satt >= HUNGER) {
            hunger = 0;
            status = wachstum > 0 ? STATUS_WAECHST : STATUS_LAEUFT;
            return;
        }
        hunger++;
        status = wachstum > 0 ? STATUS_WAECHST : STATUS_HUNGRIG;
        if (hunger < GEDULD) return;
        if (ring) {
            // Der Eindämmungsring fängt ihn ab: sichere Abschaltung statt Wachstum
            gezuendet = false;
            hunger = 0;
            status = STATUS_SICHER_AUS;
            melden("message.riftverse.singularitaet.sicher", ChatFormatting.GREEN);
            return;
        }
        if ((hunger - GEDULD) % WACHSTUM_TAKT == 0 && wachstum < MAX_WACHSTUM) {
            wachstum++;
            status = STATUS_WAECHST;
            if (level != null) level.playSound(null, worldPosition, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 2.0f, 0.5f);
            melden("message.riftverse.singularitaet.waechst", ChatFormatting.RED);
        }
    }

    /** Reichweite des Sogs in Blöcken. */
    public int radius() {
        return wachstum == 0 ? 0 : 1 + wachstum;
    }

    /** Frisst bis zu {@code max} Blöcke (pro Wachstumsstufe zwei pro Sekunde) in Reichweite. @return gefressene Blöcke */
    private int blockeFressen(int max) {
        if (!(level instanceof ServerLevel sl) || max <= 0) return 0;
        int r = radius();
        int versuche = wachstum * 12, gefressen = 0, ziel = Math.min(max, wachstum * 2);
        for (int i = 0; i < versuche && gefressen < ziel; i++) {
            BlockPos p = worldPosition.offset(sl.random.nextInt(2 * r + 1) - r, sl.random.nextInt(2 * r + 1) - r, sl.random.nextInt(2 * r + 1) - r);
            if (p.equals(worldPosition) || p.distSqr(worldPosition) > r * r) continue;
            if (!frissbar(sl, p)) continue;
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
            sl.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            gefressen++;
        }
        return gefressen;
    }

    /** Frisst der Kern diesen Block? Nichts Unzerstörbares, keine Block-Entities (Maschinen, Kisten), nicht seinen Ring. */
    public static boolean frissbar(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.isAir() || s.getDestroySpeed(level, p) < 0 || s.hasBlockEntity()) return false;
        if (s.is(ModBlocks.EINDAEMMUNGSRING.get())) return false;
        return s.getFluidState().isEmpty() || s.getFluidState().isSource();
    }

    /** Sog: Gegenstände werden verschluckt (zählen als Futter), Lebewesen gezogen und am Ereignishorizont verletzt. */
    private void ansaugen() {
        if (!(level instanceof ServerLevel sl)) return;
        Vec3 c = Vec3.atCenterOf(worldPosition);
        double reach = radius() * 2.0 + 2.0;
        List<Entity> list = sl.getEntities((Entity) null, new AABB(worldPosition).inflate(reach), e -> !e.isSpectator());
        for (Entity e : list) {
            if (e instanceof Player p && p.isCreative()) continue;
            Vec3 d = c.subtract(e.position());
            double dist = d.length();
            if (dist > reach || dist < 1.0e-3) continue;
            if (e instanceof ItemEntity item && dist < 1.6) {
                ItemStack s = item.getItem();
                satt = Math.min(HUNGER, satt + s.getCount());
                item.discard();
                continue;
            }
            double pull = 0.04 + 0.02 * wachstum * (1.0 - dist / reach);
            e.setDeltaMovement(e.getDeltaMovement().add(d.normalize().scale(pull)));
            e.hurtMarked = true;
            if (dist < 1.8 && e instanceof net.minecraft.world.entity.LivingEntity living) {
                living.hurt(sl.damageSources().magic(), 1.0f + wachstum);
            }
        }
    }

    private void effekte() {
        if (!(level instanceof ServerLevel sl)) return;
        Vec3 c = Vec3.atCenterOf(worldPosition);
        double r = Math.max(1.5, radius());
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 8 + wachstum * 4, r * 0.6, r * 0.6, r * 0.6, 0.02);
        if (wachstum > 0) sl.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, wachstum * 2, r * 0.4, r * 0.4, r * 0.4, 0.01);
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
        if (id == BUTTON_ZUENDEN) schalten();
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
        List<GuiSlot> list = new java.util.ArrayList<>();
        for (int i = 0; i < FUTTER; i++) list.add(GuiSlot.in(i, 30 + (i % 3) * 18, 17 + (i / 3) * 18));
        return list;
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
        return Progress.NONE;
    }

    @Override
    protected int extra(int index) {
        return switch (index) {
            case 0 -> status;
            case 1 -> leistung / 100;
            case 2 -> wachstum;
            case 3 -> ring ? 1 : 0;
            case 4 -> abkuehlen / 20;
            case 5 -> gezuendet ? 1 : 0;
            default -> 0;
        };
    }

    @Override
    public List<GuiButton> guiButtons() {
        return List.of(new GuiButton(BUTTON_ZUENDEN, 88, 17, 80));
    }

    @Override
    public Component buttonLabel(int id, int[] extra) {
        if (extra[5] == 1) return Component.translatable("gui.riftverse.singularitaet.notaus");
        return extra[4] > 0 ? Component.translatable("gui.riftverse.singularitaet.abkuehlen", extra[4])
                : Component.translatable("gui.riftverse.singularitaet.zuenden");
    }

    @Override
    public int infoX() {
        return 88;
    }

    @Override
    public int infoY() {
        return 35;
    }

    @Override
    public List<Component> infoLines(int[] extra) {
        ChatFormatting color = switch (extra[0]) {
            case STATUS_LAEUFT, STATUS_VOLL, STATUS_SICHER_AUS -> ChatFormatting.DARK_GREEN;
            case STATUS_HUNGRIG -> ChatFormatting.GOLD;
            case STATUS_WAECHST -> ChatFormatting.RED;
            default -> ChatFormatting.DARK_GRAY;
        };
        return List.of(Component.translatable("gui.riftverse.singularitaet.status." + extra[0]).withStyle(color),
                Component.translatable("gui.riftverse.singularitaet.leistung", extra[1] * 100),
                Component.translatable(extra[3] == 1 ? "gui.riftverse.singularitaet.ring" : "gui.riftverse.singularitaet.kein_ring")
                        .withStyle(extra[3] == 1 ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_GRAY));
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return ItemEnergy.of(stack) != null;
        return slot < FUTTER;
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Gezuendet", gezuendet);
        tag.putInt("Satt", satt);
        tag.putInt("Hunger", hunger);
        tag.putInt("Wachstum", wachstum);
        tag.putInt("Abkuehlen", abkuehlen);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        gezuendet = tag.getBoolean("Gezuendet");
        satt = tag.getInt("Satt");
        hunger = tag.getInt("Hunger");
        wachstum = Math.min(MAX_WACHSTUM, tag.getInt("Wachstum"));
        abkuehlen = tag.getInt("Abkuehlen");
    }

    /** Für den Rauchtest: Hunger und Wachstum direkt setzen. */
    public void setZustand(boolean gezuendet, int hunger, int wachstum) {
        this.gezuendet = gezuendet;
        this.hunger = hunger;
        this.wachstum = wachstum;
        this.abkuehlen = 0;
        setChanged();
    }

    @Nullable
    public BlockPos ringFehlt() {
        return level == null ? null : Multiblock.ringMissing(level, worldPosition);
    }
}
