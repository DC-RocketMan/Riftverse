package dev.riftverse.story;

/** Persönlichkeit eines Bosses – bestimmt Tonfall und ob er um Gnade bettelt. */
public enum Personality {
    MEGALOMANIAC("größenwahnsinnig", 0.10f),
    BUREAUCRAT("bürokratisch", 0.60f),
    NIHILIST("nihilistisch", 0.25f),
    MANIAC("völlig durchgeknallt", 0.30f),
    COWARD("feige", 1.00f),
    AVENGER("rachsüchtig", 0.35f),
    SALESMAN("geschäftstüchtig", 0.70f);

    public final String label;
    public final float begChance;

    Personality(String label, float begChance) {
        this.label = label;
        this.begChance = begChance;
    }
}
