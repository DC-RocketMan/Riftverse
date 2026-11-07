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
 * Meilenstein 2 bringt die drei T1-Typen; Niedriggravitation, Zeitdilatation usw. folgen mit Kanone Mk II.
 */
public enum UniverseType implements StringRepresentable {
    /** Dauerhafter Nebel, Müllberge; Alien-Schrott, Xenit in Wrackfeldern; Schrott-Golems in den Halden. */
    SCHROTT("schrottwelt", 1, 1.0),
    /** Immer hell, Kristalle wachsen nach; Fluxkristall-Cluster; Lärm lässt Kristalle explodieren. */
    KRISTALL("kristallwelt", 1, 1.0),
    /** Luft vergiftet ohne Helm; Säureseen mit Säuregel; Säureregen beschädigt Rüstung. */
    TOXISCH("toxische_welt", 1, 1.0);

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

    /** Gravitation in g; die T1-Typen haben normale Schwerkraft. */
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

    /** Alle Typen, die eine Kanone dieser Stufe erreicht. */
    public static List<UniverseType> reachableWith(int tier) {
        return Arrays.stream(values()).filter(t -> t.gunTier <= tier).toList();
    }
}
