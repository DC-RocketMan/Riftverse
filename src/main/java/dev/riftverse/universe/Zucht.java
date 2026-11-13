package dev.riftverse.universe;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Bauplan eines gezüchteten Universums aus dem Weltenformer. Die Parameter stecken vollständig in der Zelle des
 * Universums in der Dimension {@link UniverseType#GEZUECHTET}, deshalb kann der Weltengenerator sie ohne Speicherdaten
 * lesen:
 * <ul>
 *   <li>Zelle X (−32 … 31): Landschaft × 8 + Rohstoffe (je einer der acht Grundtypen).</li>
 *   <li>Zelle Z (−150 … 149): (Schwerkraft × 2 + Gefahren) × 50 + laufende Nummer (0 … 49).</li>
 * </ul>
 * Ab der 51. Zucht derselben Kombination beginnt die Nummer wieder bei 0 – man landet dann in einem schon
 * erschaffenen Universum gleicher Bauart.
 *
 * @param landschaft Gelände, Gestein, Deko, Biom und Physik-Eigenheiten
 * @param rohstoff   welche exklusiven Erze vorkommen
 * @param schwerkraft 0 = niedrig (0,3 g), 1 = normal, 2 = hoch (2,5 g)
 * @param gefahren   Umweltgefahren der Landschaft (Giftluft, Resonanz, Doppelgänger …) an oder aus
 */
public record Zucht(UniverseType landschaft, UniverseType rohstoff, int schwerkraft, boolean gefahren) {

    /** Die acht Grundtypen, aus denen sich ein Universum züchten lässt. */
    public static final List<UniverseType> BASIS = List.of(UniverseType.SCHROTT, UniverseType.KRISTALL, UniverseType.TOXISCH,
            UniverseType.NIEDRIGGRAVITATION, UniverseType.ZEITDILATATION, UniverseType.ORGANISCH, UniverseType.HOCHGRAVITATION,
            UniverseType.SPIEGEL);
    public static final double[] GRAVITATION = {0.3, 1.0, 2.5};
    public static final int PRO_KOMBINATION = 50;

    public Zucht {
        if (!BASIS.contains(landschaft)) landschaft = UniverseType.SCHROTT;
        if (!BASIS.contains(rohstoff)) rohstoff = UniverseType.SCHROTT;
        schwerkraft = Math.floorMod(schwerkraft, GRAVITATION.length);
    }

    public double gravity() {
        return GRAVITATION[schwerkraft];
    }

    /** Schlüssel einer Kombination (für den Zähler im {@link MultiverseData}). */
    public String key() {
        return landschaft.id() + "/" + rohstoff.id() + "/" + schwerkraft + "/" + (gefahren ? 1 : 0);
    }

    /** Das {@code nummer}-te Universum dieser Bauart. */
    public UniverseId id(int nummer) {
        int x = BASIS.indexOf(landschaft) * 8 + BASIS.indexOf(rohstoff) - 32;
        int z = (schwerkraft * 2 + (gefahren ? 1 : 0)) * PRO_KOMBINATION + Math.floorMod(nummer, PRO_KOMBINATION) - 150;
        return new UniverseId(UniverseType.GEZUECHTET, x, z);
    }

    /** Bauplan eines gezüchteten Universums; Zellen außerhalb des Zuchtbereichs werden in ihn gefaltet. */
    public static Zucht of(UniverseId id) {
        int x = Math.floorMod(id.cellX() + 32, 64);
        int z = Math.floorMod(id.cellZ() + 150, 300);
        int group = z / PRO_KOMBINATION;
        return new Zucht(BASIS.get(x / 8), BASIS.get(x % 8), group / 2, group % 2 == 1);
    }

    /** „Kristallwelt · Gravitit · 2,50 g · Gefahren an“ */
    public Component describe() {
        return Component.translatable("tooltip.riftverse.zucht", landschaft.displayName(),
                Component.translatable("zucht.riftverse.rohstoff." + rohstoff.id()),
                String.format(Locale.GERMAN, "%.2f g", gravity()),
                Component.translatable(gefahren ? "zucht.riftverse.gefahren_an" : "zucht.riftverse.gefahren_aus"));
    }
}
