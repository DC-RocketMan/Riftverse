package dev.riftverse.tech;

import dev.riftverse.Riftverse;
import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.RiftEnergySource;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Mikrokosmos-Zelle (T3, 2.000–8.000 FE/t, Stufe III): in einer Autobatterie lebt eine winzige Zivilisation, die den
 * Strom erzeugt. Sie will Nahrung und Baumaterial.
 * <ul>
 *   <li>Alle 10 Sekunden isst sie {@code stufe} Nahrung, alle 20 Sekunden verbaut sie {@code stufe} Material.</li>
 *   <li>Versorgung hebt die Zufriedenheit, Mangel senkt sie. Die Abzugs-Drossel (25–100 %) bestimmt, wie viel Strom
 *       man abzweigt: 100 % kostet Zufriedenheit, 25 % lässt sie wachsen.</li>
 *   <li>Bei 90 % Zufriedenheit über eine knappe Minute steigt die Zivilisationsstufe (1–4, je 2.000 FE/t); bei 10 % sinkt sie.</li>
 *   <li>Fällt die Zufriedenheit auf 0, streiken die Bewohner, bis sie wieder bei 40 % ist.</li>
 * </ul>
 * Slots: 0 Nahrung, 1 Material, 2 Akku.
 */
public class MikrokosmosZelleBlockEntity extends IndustrieBlockEntity implements RiftEnergySource {
    public static final int SLOT_NAHRUNG = 0, SLOT_MATERIAL = 1, SLOT_BATTERY = 2;
    public static final int PRO_STUFE = 2_000, MAX_STUFE = 4;
    public static final int TAKT = 200;
    public static final int BUTTON_DROSSEL = 0;
    public static final TagKey<Item> MATERIAL = TagKey.create(Registries.ITEM, Riftverse.id("mikrokosmos_material"));

    private int stufe = 1;
    private int zufriedenheit = 60;
    private int drossel = 2;          // 1–4 = 25 / 50 / 75 / 100 %
    private boolean streik;
    private int takt;
    private int hochTakte, tiefTakte;
    private boolean nahrungOk = true, materialOk = true;

    public MikrokosmosZelleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIKROKOSMOS_ZELLE.get(), pos, state, PRO_STUFE * MAX_STUFE * 40, 3);
    }

    public int stufe() {
        return stufe;
    }

    public int zufriedenheit() {
        return zufriedenheit;
    }

    public boolean streik() {
        return streik;
    }

    /** Leistung gerade: Stufe × 2.000 FE/t × Drossel, im Streik 0. */
    public int leistung() {
        return streik ? 0 : PRO_STUFE * stufe * drossel / 4;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        if (++takt >= TAKT) {
            takt = 0;
            versorgen();
        }
        int out = leistung();
        boolean running = out > 0 && !energy.isFull();
        if (running) {
            energy.add(out);
            setChanged();
        }
        ItemEnergy.charge(items.getStackInSlot(SLOT_BATTERY), energy, VoltageTier.III.maxPacket());
        if (level instanceof ServerLevel sl && level.getGameTime() % 40 == 0 && zufriedenheit < 30) {
            // Die Lichter der Stadt flackern bei Unzufriedenheit
            sl.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                    2, 0.2, 0.02, 0.2, 0.0);
        }
        setActive(running || (out > 0 && energy.isFull()));
    }

    /** Alle 10 Sekunden: essen, bauen, Stimmung. */
    private void versorgen() {
        nahrungOk = verbrauche(SLOT_NAHRUNG, stufe);
        boolean materialTakt = level != null && (level.getGameTime() / TAKT) % 2 == 0;
        if (materialTakt) materialOk = verbrauche(SLOT_MATERIAL, stufe);

        int delta = (nahrungOk ? 3 : -6) + (materialOk ? 2 : -4) - (drossel - 2) * 2;
        zufriedenheit = Math.max(0, Math.min(100, zufriedenheit + delta));

        if (zufriedenheit == 0 && !streik) {
            streik = true;
            melden("message.riftverse.mikrokosmos.streik", ChatFormatting.RED);
        } else if (streik && zufriedenheit >= 40) {
            streik = false;
            melden("message.riftverse.mikrokosmos.streikende", ChatFormatting.GREEN);
        }
        hochTakte = zufriedenheit >= 90 ? hochTakte + 1 : 0;
        tiefTakte = zufriedenheit <= 10 ? tiefTakte + 1 : 0;
        if (hochTakte >= 5 && stufe < MAX_STUFE) {
            stufe++;
            zufriedenheit -= 30;   // Wachstumsschmerzen
            hochTakte = 0;
            melden("message.riftverse.mikrokosmos.aufstieg", ChatFormatting.GREEN);
            if (level != null) level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5f, 1.8f);
        } else if (tiefTakte >= 5 && stufe > 1) {
            stufe--;
            tiefTakte = 0;
            melden("message.riftverse.mikrokosmos.abstieg", ChatFormatting.GOLD);
        }
        setChanged();
    }

    private boolean verbrauche(int slot, int amount) {
        ItemStack s = items.getStackInSlot(slot);
        if (s.getCount() < amount) return false;
        items.extractItem(slot, amount, false);
        return true;
    }

    /** Nachricht an Spieler in der Nähe (die Zelle hat keinen Besitzer). */
    private void melden(String key, ChatFormatting color) {
        if (!(level instanceof ServerLevel sl)) return;
        Component msg = Component.translatable(key, stufe).withStyle(color);
        for (Player p : sl.players()) {
            if (p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 32 * 32) {
                p.displayClientMessage(msg, true);
            }
        }
    }

    @Override
    public void onButton(int id, Player player) {
        if (id == BUTTON_DROSSEL) {
            drossel = drossel % 4 + 1;
            setChanged();
        }
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getOfferedEnergy() {
        return Math.min(energy.stored(), PRO_STUFE * MAX_STUFE);
    }

    @Override
    public VoltageTier getSourceTier() {
        return VoltageTier.III;
    }

    @Override
    public void drawEnergy(int amount) {
        energy.take(amount);
    }

    // ------------------------------------------------------------------ GUI

    @Override
    public List<GuiSlot> guiSlots() {
        return List.of(GuiSlot.in(SLOT_NAHRUNG, 36, 22), GuiSlot.in(SLOT_MATERIAL, 36, 42));
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
    public Progress progress() {
        return Progress.BAR;
    }

    @Override
    public int progressX() {
        return 58;
    }

    @Override
    public int progressY() {
        return 22;
    }

    @Override
    protected int work() {
        return zufriedenheit;
    }

    @Override
    protected int workMax() {
        return 100;
    }

    @Override
    protected int extra(int index) {
        return switch (index) {
            case 0 -> stufe;
            case 1 -> drossel;
            case 2 -> streik ? 1 : 0;
            case 3 -> leistung();
            case 4 -> (nahrungOk ? 1 : 0) | (materialOk ? 2 : 0);
            default -> 0;
        };
    }

    @Override
    public List<GuiButton> guiButtons() {
        return List.of(new GuiButton(BUTTON_DROSSEL, 58, 46, 96));
    }

    @Override
    public Component buttonLabel(int id, int[] extra) {
        return Component.translatable("gui.riftverse.mikrokosmos.drossel", extra[1] * 25);
    }

    @Override
    public int infoY() {
        return 32;
    }

    @Override
    public int infoX() {
        return 58;
    }

    @Override
    public List<Component> infoLines(int[] extra) {
        Component line = extra[2] == 1
                ? Component.translatable("gui.riftverse.mikrokosmos.streik").withStyle(ChatFormatting.RED)
                : Component.translatable("gui.riftverse.mikrokosmos.stufe", extra[0], extra[3]);
        return List.of(line);
    }

    @Override
    public List<Component> progressTooltip(int work, int workMax, int[] extra) {
        return List.of(Component.translatable("tooltip.riftverse.mikrokosmos.zufriedenheit", work),
                Component.translatable((extra[4] & 1) != 0 ? "tooltip.riftverse.mikrokosmos.satt" : "tooltip.riftverse.mikrokosmos.hunger"),
                Component.translatable((extra[4] & 2) != 0 ? "tooltip.riftverse.mikrokosmos.baut" : "tooltip.riftverse.mikrokosmos.material"),
                Component.translatable("tooltip.riftverse.mikrokosmos.regel"));
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_NAHRUNG -> stack.has(DataComponents.FOOD);
            case SLOT_MATERIAL -> stack.is(MATERIAL);
            case SLOT_BATTERY -> ItemEnergy.of(stack) != null;
            default -> false;
        };
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Stufe", stufe);
        tag.putInt("Zufriedenheit", zufriedenheit);
        tag.putInt("Drossel", drossel);
        tag.putBoolean("Streik", streik);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stufe = Math.max(1, Math.min(MAX_STUFE, tag.contains("Stufe") ? tag.getInt("Stufe") : 1));
        zufriedenheit = tag.contains("Zufriedenheit") ? tag.getInt("Zufriedenheit") : 60;
        drossel = Math.max(1, Math.min(4, tag.contains("Drossel") ? tag.getInt("Drossel") : 2));
        streik = tag.getBoolean("Streik");
    }
}
