package dev.riftverse.universe;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Jedes Universum würfelt zusätzlich 1–3 Modifikatoren, damit keine zwei gleich sind.
 * Aus jeder Gruppe kommt höchstens einer (z. B. nicht Riesen- und Mini-Mobs zugleich).
 */
public enum UniverseModifier {
    /** Mobs 1,6× so groß (Scale-Attribut). */
    RIESEN_MOBS(Group.SIZE),
    /** Mobs halb so groß. */
    MINI_MOBS(Group.SIZE),
    /** Doppelt so viele Erzadern. */
    DOPPELTE_ERZE(null),
    /** Es spawnen keine Tiere. */
    KEINE_TIERE(null),
    /** Ständig schlagen in der Nähe Blitze ein. */
    GEWITTER(null),
    /** Supraleitend: Riftverse-Leitungen ohne Verlust. */
    SUPRALEITEND(Group.CABLE),
    /** Hochohmig: doppelter Leitungsverlust. */
    HOCHOHMIG(Group.CABLE),
    /** Kuriosität: Alle Kreaturen hüpfen vor Freude. */
    HUEPFEN(Group.CURIOSITY),
    /** Kuriosität: Alle Kreaturen leuchten wie auf einer Disco. */
    LEUCHTEN(Group.CURIOSITY);

    private enum Group { SIZE, CABLE, CURIOSITY }

    @Nullable
    private final Group group;

    UniverseModifier(@Nullable Group group) {
        this.group = group;
    }

    public String translationKey() {
        return "modifier.riftverse." + name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }

    /** Würfelt 1–3 verträgliche Modifikatoren. */
    public static List<UniverseModifier> roll(RandomSource r) {
        int count = 1 + r.nextInt(3);
        List<UniverseModifier> pool = new ArrayList<>(List.of(values()));
        List<UniverseModifier> result = new ArrayList<>();
        Set<Group> used = EnumSet.noneOf(Group.class);
        while (result.size() < count && !pool.isEmpty()) {
            UniverseModifier m = pool.remove(r.nextInt(pool.size()));
            if (m.group != null && !used.add(m.group)) continue;
            result.add(m);
        }
        result.sort(null);
        return List.copyOf(result);
    }
}
