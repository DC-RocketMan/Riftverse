package dev.riftverse.storyline;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stand einer Storyline: aktueller Schritt, Flags, Notizen, Zähler. Liegt im {@code MultiverseData.Record} des
 * einzigartigen Universums – für die ganze Welt oder pro Spieler (siehe {@link Storyline.Geltung}). Wird die Welt
 * zurückgesetzt, ist auch der Stand weg.
 */
public final class StoryStand {
    /** Aktueller Schritt; null = noch nicht begonnen (dann gilt der Start-Schritt). */
    @Nullable
    public String schritt;
    public boolean fertig;
    public final Set<String> flags = new LinkedHashSet<>();
    /** Notiz-Ids in der Reihenfolge, in der sie gefunden wurden. */
    public final Set<String> notizen = new LinkedHashSet<>();
    public final Set<String> gelesen = new LinkedHashSet<>();
    /** Zähler für „besiegt“-Auslöser im aktuellen Schritt (Mob-Id → Anzahl). */
    public final Map<String, Integer> zaehler = new HashMap<>();
    /** Nur im Welt-Stand: Figuren, die gerade in der Welt stehen (Figur-Id → Entity-UUID). */
    public final Map<String, UUID> figuren = new HashMap<>();
    /** Nur im Welt-Stand: Die Figuren mit „sofort“ wurden schon gesetzt. */
    public boolean figurenGesetzt;

    public boolean begonnen() {
        return schritt != null || fertig;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        if (schritt != null) t.putString("Schritt", schritt);
        t.putBoolean("Fertig", fertig);
        t.put("Flags", strings(flags));
        t.put("Notizen", strings(notizen));
        t.put("Gelesen", strings(gelesen));
        CompoundTag z = new CompoundTag();
        zaehler.forEach(z::putInt);
        t.put("Zaehler", z);
        CompoundTag f = new CompoundTag();
        figuren.forEach(f::putUUID);
        t.put("Figuren", f);
        t.putBoolean("FigurenGesetzt", figurenGesetzt);
        return t;
    }

    public static StoryStand load(CompoundTag t) {
        StoryStand s = new StoryStand();
        if (t.contains("Schritt")) s.schritt = t.getString("Schritt");
        s.fertig = t.getBoolean("Fertig");
        read(t, "Flags", s.flags);
        read(t, "Notizen", s.notizen);
        read(t, "Gelesen", s.gelesen);
        CompoundTag z = t.getCompound("Zaehler");
        for (String k : z.getAllKeys()) s.zaehler.put(k, z.getInt(k));
        CompoundTag f = t.getCompound("Figuren");
        for (String k : f.getAllKeys()) {
            if (f.hasUUID(k)) s.figuren.put(k, f.getUUID(k));
        }
        s.figurenGesetzt = t.getBoolean("FigurenGesetzt");
        return s;
    }

    private static ListTag strings(Set<String> set) {
        ListTag l = new ListTag();
        for (String s : set) l.add(StringTag.valueOf(s));
        return l;
    }

    private static void read(CompoundTag t, String key, Set<String> into) {
        for (Tag e : t.getList(key, Tag.TAG_STRING)) into.add(e.getAsString());
    }

    // ------------------------------------------------------------------ Sammlung pro Universum

    /** Alle Stände eines Universums: für die Welt und pro Spieler, je Storyline-Id. */
    public static final class Sammlung {
        public final Map<String, StoryStand> welt = new HashMap<>();
        public final Map<UUID, Map<String, StoryStand>> spieler = new HashMap<>();

        public StoryStand welt(String storyline) {
            return welt.computeIfAbsent(storyline, k -> new StoryStand());
        }

        public StoryStand spieler(UUID id, String storyline) {
            return spieler.computeIfAbsent(id, k -> new HashMap<>()).computeIfAbsent(storyline, k -> new StoryStand());
        }

        public boolean leer() {
            return welt.isEmpty() && spieler.isEmpty();
        }

        public void leeren() {
            welt.clear();
            spieler.clear();
        }

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            CompoundTag w = new CompoundTag();
            welt.forEach((k, v) -> w.put(k, v.save()));
            t.put("Welt", w);
            CompoundTag sp = new CompoundTag();
            spieler.forEach((uuid, map) -> {
                CompoundTag m = new CompoundTag();
                map.forEach((k, v) -> m.put(k, v.save()));
                sp.put(uuid.toString(), m);
            });
            t.put("Spieler", sp);
            return t;
        }

        public void load(CompoundTag t) {
            leeren();
            CompoundTag w = t.getCompound("Welt");
            for (String k : w.getAllKeys()) welt.put(k, StoryStand.load(w.getCompound(k)));
            CompoundTag sp = t.getCompound("Spieler");
            for (String u : sp.getAllKeys()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(u);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                CompoundTag m = sp.getCompound(u);
                Map<String, StoryStand> map = new HashMap<>();
                for (String k : m.getAllKeys()) map.put(k, StoryStand.load(m.getCompound(k)));
                spieler.put(uuid, map);
            }
        }
    }
}
