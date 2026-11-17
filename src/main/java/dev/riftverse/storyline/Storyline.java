package dev.riftverse.storyline;

import dev.riftverse.universe.UniverseType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Eine Storyline aus dem Datapack ({@code data/<ns>/riftverse_story/<welt>/<name>.json}, siehe {@code docs/story.md}).
 * Sie besteht aus Schritten; der aktuelle Schritt wartet auf einen seiner Auslöser und führt dann seine Aktionen aus.
 * Verzweigungen laufen über Flags und über Antworten in Dialogen.
 *
 * @param id          z. B. {@code riftverse:test_einzigartig/kartograf}
 * @param welt        das einzigartige Universum, in dem die Storyline spielt
 * @param geltung     wo der Stand liegt: für die ganze Welt (Standard) oder pro Spieler
 * @param fortschritt Advancement, das beim Abschluss vergeben wird (optional)
 * @param start       Id des ersten Schritts
 */
public record Storyline(ResourceLocation id, UniverseType welt, String titel, String beschreibung, Geltung geltung,
                        @Nullable ResourceLocation fortschritt, Map<String, Figur> figuren, Map<String, Schritt> schritte,
                        String start) {

    /** Wo ein Stand, ein Flag oder eine Notiz gespeichert wird. */
    public enum Geltung {
        WELT, SPIELER;

        public static Geltung of(String s) {
            return switch (s) {
                case "welt" -> WELT;
                case "spieler" -> SPIELER;
                default -> throw new IllegalArgumentException("geltung muss \"welt\" oder \"spieler\" sein, nicht \"" + s + "\"");
            };
        }
    }

    /** Ein Ort in der Welt; ohne y steht er auf dem Boden. */
    public record Ort(int x, @Nullable Integer y, int z) {
        public BlockPos aufloesen(ServerLevel level) {
            if (y != null) return new BlockPos(x, y, z);
            level.getChunk(x >> 4, z >> 4);
            return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        }

        public double abstandXZ(double px, double pz) {
            double dx = px - (x + 0.5), dz = pz - (z + 0.5);
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    /**
     * Eine Figur, mit der man reden kann.
     *
     * @param aussehen {@code steve}, {@code alex} oder ein Texturpfad wie {@code riftverse:textures/entity/figur/mira.png}
     * @param sofort   erscheint, sobald die Welt die Storyline zum ersten Mal lädt (sonst erst per Aktion „figur“)
     * @param standard Satz, den die Figur sagt, wenn gerade kein Auslöser passt (optional)
     */
    public record Figur(String id, String name, String aussehen, Ort ort, boolean laufen, int radius, boolean sofort,
                        @Nullable String standard) {}

    /**
     * Ein Schritt der Storyline.
     *
     * @param ziel      Text in der Ziel-Anzeige, solange dieser Schritt aktiv ist (leer = keine Anzeige)
     * @param weiter    nächster Schritt nach den Aktionen (optional)
     * @param abschluss die Storyline endet nach diesem Schritt
     */
    public record Schritt(String id, String ziel, List<Ausloeser> ausloeser, Bedingung bedingung, List<Aktion> aktionen,
                          @Nullable String weiter, boolean abschluss) {}

    /** Flags, die gesetzt bzw. nicht gesetzt sein müssen, damit ein Auslöser zählt. */
    public record Bedingung(Set<String> flags, Set<String> nicht) {
        public static final Bedingung IMMER = new Bedingung(Set.of(), Set.of());

        public boolean erfuellt(Set<String> gesetzt) {
            return gesetzt.containsAll(flags) && nicht.stream().noneMatch(gesetzt::contains);
        }
    }

    // ------------------------------------------------------------------ Auslöser

    public sealed interface Ausloeser {
        /** Ankunft in der Welt (jede Ankunft). */
        record Ankunft() implements Ausloeser {}

        /** Spieler ist höchstens {@code radius} Blöcke (waagerecht) von {@code pos} entfernt. */
        record Bereich(Ort pos, double radius) implements Ausloeser {}

        /** Spieler hat mindestens {@code anzahl} dieses Items im Inventar. */
        record Item(ResourceLocation item, int anzahl) implements Ausloeser {}

        /** Spieler gibt der Figur das Item (Rechtsklick mit dem Item in der Hand); es wird verbraucht. */
        record Abgeben(String figur, ResourceLocation item, int anzahl) implements Ausloeser {}

        /** Spieler hat in diesem Schritt so viele dieser Mobs besiegt. */
        record Besiegt(ResourceLocation mob, int anzahl) implements Ausloeser {}

        /** Rechtsklick auf die Figur. */
        record Reden(String figur) implements Ausloeser {}

        /** Rechtsklick auf den Block an dieser Stelle. */
        record Block(BlockPos pos) implements Ausloeser {}

        /** Die Notiz wurde im Rift-Tagebuch gelesen. */
        record NotizGelesen(String notiz) implements Ausloeser {}

        /** Es ist Nacht (oder Tag). */
        record Zeit(boolean nacht) implements Ausloeser {}

        /** Das Flag ist gesetzt. */
        record Flag(String flag) implements Ausloeser {}

        /** Im Dialog wurde die Antwort mit dieser Id gewählt. */
        record Antwort(String antwort) implements Ausloeser {}
    }

    // ------------------------------------------------------------------ Aktionen

    public sealed interface Aktion {
        /** Neue Notiz im Rift-Tagebuch. */
        record Notiz(String id, String titel, String text, @Nullable Geltung geltung) implements Aktion {}

        /** Dialog über der Figur. Die folgenden Aktionen laufen erst, wenn der Dialog zu Ende ist. */
        record Dialog(String figur, List<Zeile> zeilen) implements Aktion {}

        record Titel(String titel, String untertitel) implements Aktion {}

        record Nachricht(String text) implements Aktion {}

        record ItemGeben(ResourceLocation item, int anzahl, @Nullable String name) implements Aktion {}

        record Xp(int menge) implements Aktion {}

        record Sound(ResourceLocation sound, float lautstaerke, float tonhoehe) implements Aktion {}

        record FlagSetzen(String flag, boolean wert, @Nullable Geltung geltung) implements Aktion {}

        /** Figur erscheinen lassen (optional an einem anderen Ort) oder verschwinden lassen. */
        record FigurAktion(String figur, boolean erscheinen, @Nullable Ort ort) implements Aktion {}

        record Mob(ResourceLocation mob, Ort ort, int anzahl) implements Aktion {}

        /** Blöcke setzen; die Welt verändert sich dauerhaft. */
        record Bloecke(List<BlockSetzen> bloecke) implements Aktion {}

        /** Zu einem anderen Schritt springen. */
        record Schritt(String schritt) implements Aktion {}

        /** Storyline abschließen. */
        record Abschluss() implements Aktion {}

        /**
         * Intern: Ende des Schritts {@code von} ({@code weiter}, {@code abschluss} oder der nächste Schritt der Liste).
         * Läuft nach allen Aktionen und nur, wenn die Storyline noch auf diesem Schritt steht (eine Antwort mit eigenem
         * Sprung hat Vorrang).
         */
        record SchrittEnde(String von, @Nullable String weiter, boolean abschluss) implements Aktion {}
    }

    /**
     * Eine Zeile im Dialog.
     *
     * @param wenn     Zeile nur zeigen, wenn dieses Flag gesetzt ist (optional; mit „!“ davor: wenn es nicht gesetzt ist)
     * @param sprecher anderer Sprecher als die Figur (optional)
     */
    public record Zeile(String text, @Nullable String wenn, @Nullable String sprecher, List<Antwort> antworten) {}

    /**
     * Eine Antwortmöglichkeit. Führt ihre Aktionen aus; mit {@code schritt} endet der Dialog und die Storyline springt
     * dorthin, ohne {@code schritt} geht der Dialog mit der nächsten Zeile weiter.
     */
    public record Antwort(String text, @Nullable String id, List<Aktion> aktionen, boolean verzweigt) {}

    /**
     * Ein Block (oder ein Quader von {@code von} bis {@code bis}).
     *
     * @param ueberBoden nur bei Orten ohne y: so viele Blöcke über dem Boden
     */
    public record BlockSetzen(@Nullable Ort pos, int ueberBoden, @Nullable BlockPos von, @Nullable BlockPos bis,
                              ResourceLocation block) {}

    @Nullable
    public Schritt schritt(@Nullable String id) {
        return id == null ? null : schritte.get(id);
    }

    /** Der Schritt nach {@code id} in der Reihenfolge der Datei, oder null nach dem letzten. */
    @Nullable
    public String naechster(String id) {
        boolean gefunden = false;
        for (String k : schritte.keySet()) {
            if (gefunden) return k;
            if (k.equals(id)) gefunden = true;
        }
        return null;
    }

    /** Alle Notizen der Storyline (auch in Antworten), nach Id. */
    public Map<String, Aktion.Notiz> notizen() {
        Map<String, Aktion.Notiz> out = new java.util.LinkedHashMap<>();
        for (Schritt s : schritte.values()) sammle(s.aktionen(), out);
        return out;
    }

    private static void sammle(List<Aktion> aktionen, Map<String, Aktion.Notiz> out) {
        for (Aktion a : aktionen) {
            if (a instanceof Aktion.Notiz n) out.putIfAbsent(n.id(), n);
            if (a instanceof Aktion.Dialog d) {
                for (Zeile z : d.zeilen()) for (Antwort an : z.antworten()) sammle(an.aktionen(), out);
            }
        }
    }
}
