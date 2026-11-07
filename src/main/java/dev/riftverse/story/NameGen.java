package dev.riftverse.story;

import net.minecraft.util.RandomSource;

/** Silbenbasierter Namensgenerator für Aliens, Universen, Orte und Artefakte. */
public final class NameGen {
    private static final String[] ONSETS = {
            "Gl", "Zr", "Bl", "Kr", "Pl", "Sn", "Vr", "Qu", "Fl", "Gr", "Sk", "Br", "Zl", "X", "M", "N",
            "D", "Th", "Sch", "Pf", "Gn", "Tr", "V", "Z", "R", "L", "K", "Bw", "Sp", "Gw"};
    private static final String[] VOWELS = {
            "o", "u", "a", "ee", "oo", "i", "ai", "oi", "y", "ü", "ö", "au", "e", "ä"};
    private static final String[] CODAS = {
            "rp", "b", "g", "x", "nk", "zz", "rb", "lon", "mph", "st", "tch", "rk", "ng", "m", "p",
            "sch", "ff", "l", "r", "z", "th", "bus", "dar", "nax", "lip", "gorp"};
    private static final String[] MIDDLES = {"", "", "", "b", "l", "r", "z", "m", "pl", "gl"};

    private static final String[] UNIVERSE_PATTERNS = {
            "%s Prime", "%s-Nebel", "%s VII", "Neu-%s", "%sia", "Groß-%s", "%s-Gürtel", "Die %s-Weiten",
            "%s Minor", "%s-Zentralwelt", "Kolonie %s", "%s-Ödnis", "Das %s-Kontinuum"};

    private static final String[] ARTIFACTS = {
            "Entropie-Amulett", "Paradox-Ei", "Quanten-Zepter", "Gravitations-Herz", "Kalibrierungs-Modul",
            "Ur-Rezept", "Kausalitäts-Siegel", "Multiversal-Register", "Leerstellen-Juwel",
            "Zeitkristall-Fragment", "Schwerkraft-Ventil", "Antimaterie-Fondue-Set", "Unendlichkeits-Formular",
            "Entropie-Thermometer", "Realitäts-Lineal", "Dimensions-Kompass", "Singularitäts-Glas",
            "Plasma-Orakel", "Vakuum-Horn", "Zufalls-Würfel"};

    private static final String[] SENDERS = {
            "eine verzweifelte Physikerin namens %s",
            "der Bürgermeister von %s",
            "ein sprechender Kaktus, der sich als %s vorstellt",
            "eine Stimme, die verdächtig nach dir selbst klingt",
            "das Kopfgeld-Netzwerk von %s",
            "ein Roboter-Hausmeister namens %s",
            "die letzte Bibliothekarin von %s",
            "ein Kollektiv intelligenter Pilze aus %s"};

    private static final String[] MINIONS = {
            "Handlanger", "Formular-Drohnen", "Vasallen", "Bio-Klone", "Praktikanten", "Gravitons",
            "Leibwächter", "Quanten-Lakaien", "Schatten-Kopien", "Erfüllungsgehilfen"};

    public static String alienName(RandomSource r) {
        StringBuilder sb = new StringBuilder();
        int syllables = 2 + r.nextInt(2);
        for (int i = 0; i < syllables; i++) {
            if (i == 0) {
                sb.append(pick(r, ONSETS));
            } else {
                String middle = pick(r, MIDDLES);
                sb.append(middle.isEmpty() ? pick(r, ONSETS).toLowerCase() : middle);
            }
            sb.append(pick(r, VOWELS));
            if (i == syllables - 1 || r.nextInt(3) == 0) sb.append(pick(r, CODAS));
        }
        String s = sb.toString();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String universeName(RandomSource r) {
        return String.format(pick(r, UNIVERSE_PATTERNS), alienName(r));
    }

    /** Artefakt-Nomen (alle sächlich: „das …“, „dem …“). */
    public static String artifact(RandomSource r) {
        return pick(r, ARTIFACTS);
    }

    public static String sender(RandomSource r) {
        return String.format(pick(r, SENDERS), alienName(r));
    }

    public static String minions(RandomSource r) {
        return pick(r, MINIONS);
    }

    public static String pick(RandomSource r, String[] options) {
        return options[r.nextInt(options.length)];
    }

    private NameGen() {}
}
