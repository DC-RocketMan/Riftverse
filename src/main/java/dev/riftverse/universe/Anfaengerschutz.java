package dev.riftverse.universe;

import dev.riftverse.Config;
import dev.riftverse.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Anfängerschutz (Config {@code universen.anfaengerschutz}, standardmäßig aus): Die ersten drei Zufallssprünge mit
 * der Kanone Mk I führen in die Schrott-, die Kristall- und die Toxische Welt, jede genau einmal und in zufälliger
 * Reihenfolge. So kommt jeder Spieler früh an Alien-Schrott, Fluxkristall und Säuregel.
 */
public final class Anfaengerschutz {
    public static final List<UniverseType> WELTEN = List.of(UniverseType.SCHROTT, UniverseType.KRISTALL, UniverseType.TOXISCH);

    private Anfaengerschutz() {
    }

    /** Das geschützte Ziel für den nächsten Sprung, oder null, wenn der Schutz aus oder schon verbraucht ist. */
    @Nullable
    public static UniverseId naechstes(ServerPlayer player, RandomSource random) {
        if (!Config.ANFAENGERSCHUTZ.get()) return null;
        int maske = player.getData(ModAttachments.ANFAENGERSCHUTZ);
        UniverseType typ = waehle(maske, random);
        if (typ == null) return null;
        player.setData(ModAttachments.ANFAENGERSCHUTZ, maske | bit(typ));
        return Multiverse.random(random, List.of(typ));
    }

    /** Ein noch nicht besuchter Typ aus {@link #WELTEN}, oder null, wenn alle drei durch sind. */
    @Nullable
    public static UniverseType waehle(int maske, RandomSource random) {
        List<UniverseType> offen = new ArrayList<>();
        for (UniverseType t : WELTEN) {
            if ((maske & bit(t)) == 0) offen.add(t);
        }
        return offen.isEmpty() ? null : offen.get(random.nextInt(offen.size()));
    }

    public static int bit(UniverseType typ) {
        return 1 << WELTEN.indexOf(typ);
    }
}
