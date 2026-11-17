package dev.riftverse.dorf;

import org.jetbrains.annotations.Nullable;

/**
 * Gebäudearten der Gleeb-Dörfer. Die Maße gelten in Blöcken, die Tür liegt immer mittig an der Vorderseite
 * (lokal +z). Solange David noch keine eigenen Bauten geliefert hat, baut {@link HausBauer} sie prozedural.
 *
 * @param bewohner Beruf des Gleebs, der hier wohnt (null = Bürger)
 */
public enum HausTyp {
    WOHNHAUS_KLEIN(7, 7, null, 1),
    WOHNHAUS_GROSS(9, 11, null, 2),
    BAECKEREI(9, 9, Beruf.BAECKERIN, 0),
    KRAEUTERHUETTE(7, 9, Beruf.KRAEUTERHEXE, 0),
    ALCHEMIE(9, 9, Beruf.ALCHEMIST, 0),
    RUNENSCHMIEDE(9, 9, Beruf.RUNENSCHMIED, 0),
    STERNWARTE(9, 9, Beruf.STERNENKUNDLERIN, 0),
    GEWAECHSHAUS(9, 11, null, 1),
    BIBLIOTHEK(9, 11, null, 1),
    TAVERNE(11, 15, Beruf.BAECKERIN, 3),
    MAGIERTURM(11, 11, null, 1),
    BRUNNEN(5, 5, null, 0),
    MARKTBRUNNEN(9, 9, null, 0);

    public final int w, d;
    @Nullable
    public final Beruf beruf;
    /** Zusätzliche Bürger. */
    public final int buerger;

    HausTyp(int w, int d, @Nullable Beruf beruf, int buerger) {
        this.w = w;
        this.d = d;
        this.beruf = beruf;
        this.buerger = buerger;
    }

    public boolean rund() {
        return this == STERNWARTE || this == MAGIERTURM;
    }

    public boolean brunnen() {
        return this == BRUNNEN || this == MARKTBRUNNEN;
    }
}
