package dev.riftverse.universe;

import com.mojang.serialization.Codec;
import dev.riftverse.Riftverse;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Universumstypen. Jeder Typ ist eine eigene Dimension, die in ein Raster aus Universen aufgeteilt ist
 * (siehe {@link Multiverse}). Welcher Typ erreichbar ist, hängt von der Kanonenstufe ab.
 * <p>
 * Meilenstein 2 bringt die drei T1-Typen, Meilenstein 3 die drei T2-Typen (Niedriggravitation, Zeitdilatation,
 * Organisch), Meilenstein 4 die T3-Typen Hochgravitation und Spiegelwelt, Meilenstein 5 die Leere Welt und die
 * gezüchteten Universen des Weltenformers. Neue Typen kommen immer hinten dazu, weil der Seed eines Universums die
 * Ordnungszahl enthält.
 */
public enum UniverseType implements StringRepresentable {
    /** Dauerhafter Nebel, Müllberge; Alien-Schrott, Xenit in Wrackfeldern; Schrott-Golems in den Halden. */
    SCHROTT("schrottwelt", 1, 1.0),
    /** Immer hell, Kristalle wachsen nach; Fluxkristall-Cluster; Lärm lässt Kristalle explodieren. */
    KRISTALL("kristallwelt", 1, 1.0),
    /** Luft vergiftet ohne Helm; Säureseen mit Säuregel; Säureregen beschädigt Rüstung. */
    TOXISCH("toxische_welt", 1, 1.0),

    // --- T2, ab Kanone Mk II (Mk I mit stabilisiertem Portalfluid: zufällig) ---------------
    /** Gravitation 30 %, schwebende Inseln über dem Nichts; Schwebstein; Abstürze zwischen den Inseln. */
    NIEDRIGGRAVITATION("niedriggravitation", 2, 0.3),
    /** Pflanzen und Zufallsticks 4× schneller; Chronoglas-Sand fällt in Zeitlupe; Zeitverzerrungen setzen zurück. */
    ZEITDILATATION("zeitdilatation", 2, 1.0),
    /** Abgebaute Blöcke wachsen nach, der Boden atmet; Biomasse-Pulpe; fressende Flora. */
    ORGANISCH("organische_welt", 2, 1.0),

    // --- T3, ab Kanone Mk III (Mk II mit Portalfluid-Konzentrat: zufällig) ------------------
    /** Gravitation 250 %, kaum Sprünge, dreifacher Fallschaden; Gravitit, extrem dichtes Erz. */
    HOCHGRAVITATION("hochgravitation", 3, 2.5),
    /** Tag und Nacht vertauscht, gespiegeltes Gelände; Inversionssplitter; Doppelgänger kopieren die Ausrüstung. */
    SPIEGEL("spiegelwelt", 3, 1.0),

    // --- T4, ab Kanone Mk IV (Mk III mit Weltenformer-Essenz: zufällig in die Leere Welt) -------
    /** Fast keine Blöcke, Sternenhimmel, schwebende Kerne; Nullmaterie (selten, bewacht); die Leere frisst Licht. */
    LEERE("leere_welt", 4, 1.0),
    /**
     * Gezüchtete Universen aus dem Weltenformer: Landschaft, Rohstoffe, Schwerkraft und Gefahren sind frei gewählt
     * (siehe {@link Zucht}). Nie ein zufälliges Ziel – man kommt nur mit der Koordinatenkarte des Weltenformers hin.
     */
    GEZUECHTET("gezuechtet", 4, 1.0);

    public static final Codec<UniverseType> CODEC = StringRepresentable.fromEnum(UniverseType::values);

    private final String id;
    private final int gunTier;
    private final double gravity;
    private final ResourceKey<Level> dimension;

    UniverseType(String id, int gunTier, double gravity) {
        this.id = id;
        this.gunTier = gunTier;
        this.gravity = gravity;
        this.dimension = ResourceKey.create(Registries.DIMENSION, Riftverse.id(id));
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public String id() {
        return id;
    }

    /** Ab welcher Kanonenstufe dieser Typ erreichbar ist. */
    public int gunTier() {
        return gunTier;
    }

    /** Gravitation in g; außer der Niedriggravitationswelt haben alle bisherigen Typen normale Schwerkraft. */
    public double gravity() {
        return gravity;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public String translationKey() {
        return "universe.riftverse." + id;
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }

    @Nullable
    public static UniverseType of(ResourceKey<Level> dimension) {
        for (UniverseType t : values()) {
            if (t.dimension.equals(dimension)) return t;
        }
        return null;
    }

    @Nullable
    public static UniverseType byId(String id) {
        for (UniverseType t : values()) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }

    /** Kann eine Kanone zufällig hier landen? Gezüchtete Universen erreicht man nur gezielt. */
    public boolean randomTarget() {
        return this != GEZUECHTET;
    }

    /** Zufallsziele einer Kanone dieser Stufe (ohne gezüchtete Universen). */
    public static List<UniverseType> randomWith(int tier) {
        return Arrays.stream(values()).filter(t -> t.gunTier <= tier && t.randomTarget()).toList();
    }

    /** Alle Typen, die eine Kanone dieser Stufe erreicht. */
    public static List<UniverseType> reachableWith(int tier) {
        return Arrays.stream(values()).filter(t -> t.gunTier <= tier).toList();
    }
}
