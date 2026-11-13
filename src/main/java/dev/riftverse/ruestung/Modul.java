package dev.riftverse.ruestung;

import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Module der Multiversal-Rüstung. Jedes Modul passt in genau ein Teil; wie viele Module ein Teil trägt, steht in
 * {@link MultiversalArmorItem#slots()}. Eingebaut wird an der Werkbank (Rüstungsteil + Modul), ausgebaut ebenso
 * (Rüstungsteil allein: das zuletzt eingebaute Modul kommt zurück).
 *
 * @param proTick FE pro Tick, solange das Modul arbeitet (0 = Kosten pro Ereignis, siehe {@link Ruestung})
 */
public enum Modul {
    // --- Helm "Analyse-Visier" ---
    ANALYSE_HUD("analyse_hud", EquipmentSlot.HEAD, 5),
    NACHTSICHT_LINSE("nachtsicht_linse", EquipmentSlot.HEAD, 10),
    ATEMFILTER("atemfilter", EquipmentSlot.HEAD, 20),
    TARN_DETEKTOR("tarn_detektor_modul", EquipmentSlot.HEAD, 50),
    UNIVERSAL_UEBERSETZER("universal_uebersetzer", EquipmentSlot.HEAD, 0),
    // --- Brustteil "Kernmantel" ---
    ENERGIESCHILD("energieschild", EquipmentSlot.CHEST, 0),
    NOT_PORTAL("not_portal", EquipmentSlot.CHEST, 0),
    KANONEN_INTEGRATION("kanonen_integration", EquipmentSlot.CHEST, 0),
    KLON_LINK("klon_link", EquipmentSlot.CHEST, 0),
    GADGET_HALTERUNG("gadget_halterung", EquipmentSlot.CHEST, 0),
    /** Kein Modul im engeren Sinn: nur im perfektionierten Kernmantel, schaltet den Set-Bonus frei. */
    UNIVERSUMS_KERN("universums_kern", EquipmentSlot.CHEST, 0),
    // --- Hose "Exo-Rahmen" ---
    SERVO_VERSTAERKER("servo_verstaerker", EquipmentSlot.LEGS, 15),
    SPRINT_BOOSTER("sprint_booster", EquipmentSlot.LEGS, 10),
    GRAVITATIONSREGLER("gravitationsregler", EquipmentSlot.LEGS, 30),
    SCHRUMPF_MODUL("schrumpf_modul", EquipmentSlot.LEGS, 100),
    // --- Stiefel "Gravi-Stiefel" ---
    FALL_DAEMPFER("fall_daempfer", EquipmentSlot.FEET, 0),
    SPRUNGFEDER("sprungfeder", EquipmentSlot.FEET, 0),
    PHASENSCHRITT("phasenschritt", EquipmentSlot.FEET, 0),
    HAFTSOHLEN("haftsohlen", EquipmentSlot.FEET, 20);

    private final String id;
    private final EquipmentSlot slot;
    private final int proTick;

    Modul(String id, EquipmentSlot slot, int proTick) {
        this.id = id;
        this.slot = slot;
        this.proTick = proTick;
    }

    public String id() {
        return id;
    }

    public EquipmentSlot slot() {
        return slot;
    }

    public int proTick() {
        return proTick;
    }

    /** Name des Items, mit dem man das Modul einbaut. */
    public String itemName() {
        return this == UNIVERSUMS_KERN ? "universums_kern" : "modul_" + id;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static Modul byKey(String key) {
        for (Modul m : values()) {
            if (m.key().equals(key)) return m;
        }
        return null;
    }
}
