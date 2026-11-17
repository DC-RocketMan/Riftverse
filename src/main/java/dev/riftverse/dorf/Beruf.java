package dev.riftverse.dorf;

import net.minecraft.util.StringRepresentable;

/** Berufe der Gleebs (Meilenstein 10). Die Reihenfolge ist gespeichert, neue Berufe nur hinten anhängen. */
public enum Beruf implements StringRepresentable {
    BUERGER("buerger"), BAECKERIN("baeckerin"), KRAEUTERHEXE("kraeuterhexe"), ALCHEMIST("alchemist"),
    RUNENSCHMIED("runenschmied"), STERNENKUNDLERIN("sternenkundlerin");

    public final String id;

    Beruf(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public boolean handelt() {
        return this != BUERGER;
    }

    public static Beruf von(int i) {
        Beruf[] v = values();
        return v[Math.floorMod(i, v.length)];
    }

    public static Beruf von(String id) {
        for (Beruf b : values()) if (b.id.equals(id)) return b;
        return BUERGER;
    }
}
