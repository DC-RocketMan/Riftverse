package dev.riftverse.dorf;

import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Der Plan von Bridge's Inn (Meilenstein 10): Fluss, See mit Insel, Aussichtshügel, das Hauptdorf mit dem Wirtshaus
 * auf der Brücke, rund zwanzig kleine Gleeb-Dörfer und Orte in der Natur. Alles folgt deterministisch aus dem
 * Universums-Seed, damit jeder Chunk für sich weiß, was auf ihm steht.
 */
public final class DorfPlan {
    public static final int WASSER = 62;
    /** Aussichtshügel (Ankunft beim ersten Besuch), relativ zur Zellmitte. */
    public static final int HUEGEL_DX = -150, HUEGEL_DZ = -110, HUEGEL_RADIUS = 80;
    public static final int HAUPT_RADIUS = 100, DORF_RADIUS = 46, RAND = 28;
    public static final int SEE_DX = 600, SEE_DZ = 520, SEE_RADIUS = 95, INSEL_RADIUS = 16;
    public static final int FLUSS_BREITE = 6, UFER = 14;
    /** Brücke mit Bridge's Inn: halbe Breite (x) und halbe Länge (z). */
    public static final int BRUECKE_HB = 6, BRUECKE_HL = 26;

    public enum Ortsart { STEINKREIS, FEENRING, GROSSVATERBAUM, ALTE_BRUECKE }

    /** Ein Gebäude: Mitte des Grundrisses (x, z), Fußboden auf y. */
    public record Haus(HausTyp typ, int x, int z, int y, Rotation rot, long seed) {
        public int breiteX() {
            return quer() ? typ.d : typ.w;
        }

        public int breiteZ() {
            return quer() ? typ.w : typ.d;
        }

        private boolean quer() {
            return rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90;
        }

        public int minX() {
            return x - breiteX() / 2;
        }

        public int minZ() {
            return z - breiteZ() / 2;
        }

        public int maxX() {
            return minX() + breiteX() - 1;
        }

        public int maxZ() {
            return minZ() + breiteZ() - 1;
        }

        /** Wohin die Tür zeigt. */
        public Direction front() {
            return rot.rotate(Direction.SOUTH);
        }

        /** Der Punkt zwei Blöcke vor der Tür. */
        public int[] vorDerTuer() {
            Direction f = front();
            int abstand = (f.getAxis() == Direction.Axis.Z ? breiteZ() : breiteX()) / 2 + 2;
            return new int[]{x + f.getStepX() * abstand, z + f.getStepZ() * abstand};
        }
    }

    public record Platz(int x, int z, int r) {}

    public record Dorf(int nr, String name, int x, int z, int radius, int hoehe, boolean haupt, List<Haus> haeuser,
                       List<int[]> wege, List<Platz> plaetze) {
        public int minX() {
            return x - radius - RAND;
        }

        public int maxX() {
            return x + radius + RAND;
        }

        public int minZ() {
            return z - radius - RAND;
        }

        public int maxZ() {
            return z + radius + RAND;
        }
    }

    public record Naturort(Ortsart art, int x, int z, long seed) {}

    private final UniverseProfile p;
    public final int cx, cz;
    private final double phase1, phase2;
    public final int seeX, seeZ, huegelX, huegelZ;
    public final Dorf hauptdorf;
    public final List<Dorf> doerfer = new ArrayList<>();
    public final List<Naturort> orte = new ArrayList<>();

    public DorfPlan(UniverseProfile p) {
        this.p = p;
        this.cx = Multiverse.cellCenter(p.id.cellX());
        this.cz = Multiverse.cellCenter(p.id.cellZ());
        RandomSource r = RandomSource.create(UniverseId.mix(p.seed ^ 0xD0EF1L));
        this.phase1 = r.nextDouble() * Math.PI * 2;
        this.phase2 = r.nextDouble() * Math.PI * 2;
        this.seeX = cx + SEE_DX;
        this.seeZ = cz + SEE_DZ;
        this.huegelX = cx + HUEGEL_DX;
        this.huegelZ = cz + HUEGEL_DZ;

        this.hauptdorf = hauptdorf(r);
        doerfer.add(hauptdorf);
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) continue;
                int vx = cx + i * 640 + r.nextInt(281) - 140;
                int vz = cz + j * 640 + r.nextInt(281) - 140;
                double rz = flussZ(vx);
                double abstand = DORF_RADIUS + RAND + UFER + 16;
                if (Math.abs(vz - rz) < abstand) vz = (int) (vz < rz ? rz - abstand : rz + abstand);
                if (dist(vx, vz, seeX, seeZ) < SEE_RADIUS + DORF_RADIUS + RAND + 50) continue;
                if (dist(vx, vz, huegelX, huegelZ) < HUEGEL_RADIUS + DORF_RADIUS + RAND) continue;
                if (dist(vx, vz, cx, cz) < HAUPT_RADIUS + DORF_RADIUS + 140) continue;
                if (Math.abs(vx - cx) > 1650 || Math.abs(vz - cz) > 1650) continue;
                doerfer.add(kleinesDorf(doerfer.size(), vx, vz, r));
            }
        }

        orte.add(new Naturort(Ortsart.STEINKREIS, seeX, seeZ, r.nextLong()));
        int bx = cx + 900;
        orte.add(new Naturort(Ortsart.ALTE_BRUECKE, bx, (int) Math.round(flussZ(bx)), r.nextLong()));
        platziere(Ortsart.GROSSVATERBAUM, 1, 700, r);
        platziere(Ortsart.FEENRING, 6, 1500, r);
    }

    private void platziere(Ortsart art, int anzahl, int weite, RandomSource r) {
        for (int n = 0, versuch = 0; n < anzahl && versuch < 200; versuch++) {
            int x = cx + r.nextInt(weite * 2 + 1) - weite, z = cz + r.nextInt(weite * 2 + 1) - weite;
            if (!frei(x, z, 30) || Math.abs(z - flussZ(x)) < FLUSS_BREITE + UFER + 14) continue;
            if (dist(x, z, seeX, seeZ) < SEE_RADIUS + 30 || dist(x, z, huegelX, huegelZ) < HUEGEL_RADIUS) continue;
            orte.add(new Naturort(art, x, z, r.nextLong()));
            n++;
        }
    }

    // ------------------------------------------------------------------ Gelände

    private static double dist(double x0, double z0, double x1, double z1) {
        return Math.sqrt((x0 - x1) * (x0 - x1) + (z0 - z1) * (z0 - z1));
    }

    private static double glatt(double a, double b, double v) {
        double t = Mth.clamp((v - a) / (b - a), 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** Mitte des Flusses bei x. Am Hauptdorf läuft er gerade von West nach Ost. */
    public double flussZ(double x) {
        double w = glatt(60, 220, Math.abs(x - cx));
        return cz + w * (24 + 60 * Math.sin(x / 170.0 + phase1) + 25 * Math.sin(x / 63.0 + 1 + phase2));
    }

    /** Grundhöhe ohne Fluss, See und Dörfer. */
    private double grund(int x, int z) {
        double h = 71 + 9 * p.sanft(x, z);
        double dh = dist(x, z, huegelX, huegelZ) / HUEGEL_RADIUS;
        if (dh < 1) h += 24 * (1 - dh * dh) * (1 - dh * dh);
        return h;
    }

    private int platzHoehe(int x, int z) {
        return Mth.clamp((int) Math.round(71 + 9 * p.sanft(x, z)), 66, 80);
    }

    /** Oberster fester Block an (x, z). */
    public int hoehe(int x, int z) {
        double h = grund(x, z);
        for (Dorf d : doerfer) {
            if (x < d.minX() || x > d.maxX() || z < d.minZ() || z > d.maxZ()) continue;
            double t = glatt(d.radius(), d.radius() + RAND, dist(x, z, d.x(), d.z()));
            h = Mth.lerp(t, d.hoehe(), h);
        }
        // See mit Insel und Steinkreis
        double dl = dist(x, z, seeX, seeZ);
        if (dl < SEE_RADIUS + 26) {
            double rand = SEE_RADIUS + p.feld(x / 40.0, z / 40.0) * 8;
            if (dl < rand) {
                double k = dl / rand;
                h = Math.min(h, 52 + 10 * k * k);
                if (dl < INSEL_RADIUS + 6) h = Math.max(h, 66 - Math.max(0, dl - INSEL_RADIUS) * 1.4 + (dl < INSEL_RADIUS ? 0 : 0));
            } else {
                h = Math.min(h, Mth.lerp(glatt(rand, rand + 24, dl), 63, h));
            }
        }
        // Fluss
        double df = Math.abs(z - flussZ(x));
        if (df < FLUSS_BREITE) {
            double k = df / FLUSS_BREITE;
            h = Math.min(h, 57 + 4 * k * k);
        } else if (df < FLUSS_BREITE + UFER) {
            h = Math.min(h, Mth.lerp(glatt(FLUSS_BREITE, FLUSS_BREITE + UFER, df), 63, h));
        }
        return (int) Math.floor(h);
    }

    // ------------------------------------------------------------------ Abfragen

    /** Das Dorf, zu dessen Fläche (x, z) gehört, oder null. */
    @Nullable
    public Dorf dorfBei(int x, int z) {
        for (Dorf d : doerfer) {
            if (dist(x, z, d.x(), d.z()) <= d.radius() + 4) return d;
        }
        return null;
    }

    public List<Dorf> doerferIn(int minX, int minZ, int maxX, int maxZ) {
        List<Dorf> out = new ArrayList<>(2);
        for (Dorf d : doerfer) {
            if (d.maxX() >= minX && d.minX() <= maxX && d.maxZ() >= minZ && d.minZ() <= maxZ) out.add(d);
        }
        return out;
    }

    public List<Naturort> orteIn(int minX, int minZ, int maxX, int maxZ, int rand) {
        List<Naturort> out = new ArrayList<>(1);
        for (Naturort o : orte) {
            if (o.x() + rand >= minX && o.x() - rand <= maxX && o.z() + rand >= minZ && o.z() - rand <= maxZ) out.add(o);
        }
        return out;
    }

    /** Darf hier wilde Deko (Bäume, Blumen) wachsen? Nicht in Dörfern und nicht an Naturorten. */
    public boolean frei(int x, int z, int abstand) {
        for (Dorf d : doerfer) {
            if (dist(x, z, d.x(), d.z()) < d.radius() + abstand) return false;
        }
        for (Naturort o : orte) {
            if (dist(x, z, o.x(), o.z()) < (o.art() == Ortsart.GROSSVATERBAUM ? 14 : 10) + abstand) return false;
        }
        return true;
    }

    /** Liegt (x, z) auf einem Weg oder Platz des Dorfs? */
    public static boolean aufWeg(Dorf d, int x, int z) {
        for (Platz pl : d.plaetze()) {
            if (dist(x, z, pl.x(), pl.z()) <= pl.r() + 0.3) return true;
        }
        for (int[] w : d.wege()) {
            if (abstandZuStrecke(x, z, w) <= 1.2) return true;
        }
        return false;
    }

    static double abstandZuStrecke(double x, double z, int[] w) {
        double dx = w[2] - w[0], dz = w[3] - w[1];
        double l2 = dx * dx + dz * dz;
        double t = l2 == 0 ? 0 : Mth.clamp(((x - w[0]) * dx + (z - w[1]) * dz) / l2, 0, 1);
        return dist(x, z, w[0] + t * dx, w[1] + t * dz);
    }

    // ------------------------------------------------------------------ Dörfer

    private static final String[] VORSILBEN = {"Glimm", "Moos", "Funkel", "Stern", "Mond", "Blüten", "Nebel", "Tau",
            "Kessel", "Pusteblumen", "Laternen", "Hasel", "Zauber", "Kräuter", "Wichtel", "Eulen"};
    private static final String[] NACHSILBEN = {"hain", "bach", "brück", "wiese", "tal", "winkel", "hausen", "grund",
            "au", "heim", "hügel", "dorf"};

    private Dorf hauptdorf(RandomSource r) {
        int y = Math.max(68, platzHoehe(cx, cz - 40));
        List<Haus> haeuser = new ArrayList<>();
        List<int[]> wege = new ArrayList<>();
        int nordX = cx, nordZ = cz - 44, suedX = cx, suedZ = cz + 48;
        List<Platz> plaetze = List.of(new Platz(nordX, nordZ, 11), new Platz(suedX, suedZ, 6));
        Dorf d = new Dorf(0, "Bridge's Inn", cx, cz, HAUPT_RADIUS, y, true, haeuser, wege, plaetze);

        haeuser.add(new Haus(HausTyp.MARKTBRUNNEN, nordX, nordZ, y, Rotation.NONE, r.nextLong()));
        haeuser.add(new Haus(HausTyp.TAVERNE, cx, cz, y, Rotation.CLOCKWISE_180, r.nextLong()));
        // Wege: Platz – Brücke – Südplatz
        wege.add(new int[]{nordX, nordZ, cx, cz - BRUECKE_HL});
        wege.add(new int[]{cx, cz + BRUECKE_HL, suedX, suedZ});

        List<HausTyp> nord = new ArrayList<>(List.of(HausTyp.MAGIERTURM, HausTyp.STERNWARTE, HausTyp.BIBLIOTHEK,
                HausTyp.GEWAECHSHAUS, HausTyp.BAECKEREI, HausTyp.ALCHEMIE, HausTyp.WOHNHAUS_GROSS, HausTyp.KRAEUTERHUETTE,
                HausTyp.WOHNHAUS_KLEIN, HausTyp.WOHNHAUS_GROSS, HausTyp.WOHNHAUS_KLEIN, HausTyp.WOHNHAUS_KLEIN,
                HausTyp.WOHNHAUS_KLEIN));
        List<HausTyp> sued = List.of(HausTyp.RUNENSCHMIEDE, HausTyp.WOHNHAUS_GROSS, HausTyp.WOHNHAUS_KLEIN,
                HausTyp.WOHNHAUS_KLEIN, HausTyp.WOHNHAUS_KLEIN);
        bebauen(d, nord, nordX, nordZ, new int[]{26, 40, 54, 68}, r, false);
        bebauen(d, sued, suedX, suedZ, new int[]{19, 31, 43}, r, true);
        return d;
    }

    private Dorf kleinesDorf(int nr, int vx, int vz, RandomSource r) {
        int y = Math.max(65, platzHoehe(vx, vz));
        String name = VORSILBEN[r.nextInt(VORSILBEN.length)] + NACHSILBEN[r.nextInt(NACHSILBEN.length)];
        List<Haus> haeuser = new ArrayList<>();
        Dorf d = new Dorf(nr, name, vx, vz, DORF_RADIUS, y, false, haeuser, new ArrayList<>(), List.of(new Platz(vx, vz, 7)));
        haeuser.add(new Haus(HausTyp.BRUNNEN, vx, vz, y, Rotation.NONE, r.nextLong()));
        List<HausTyp> berufe = new ArrayList<>(List.of(HausTyp.BAECKEREI, HausTyp.KRAEUTERHUETTE, HausTyp.ALCHEMIE,
                HausTyp.RUNENSCHMIEDE, HausTyp.STERNWARTE, HausTyp.BIBLIOTHEK, HausTyp.GEWAECHSHAUS));
        Collections.shuffle(berufe, new java.util.Random(r.nextLong()));
        List<HausTyp> typen = new ArrayList<>();
        typen.add(HausTyp.WOHNHAUS_GROSS);
        typen.addAll(berufe.subList(0, 2 + r.nextInt(2)));
        int n = 5 + r.nextInt(4);
        while (typen.size() < n) typen.add(HausTyp.WOHNHAUS_KLEIN);
        bebauen(d, typen, vx, vz, new int[]{18, 29, 39}, r, false);
        return d;
    }

    /** Verteilt Häuser auf Ringen um einen Platz; die Türen zeigen zum Platz, Wege führen hin. */
    private void bebauen(Dorf d, List<HausTyp> typen, int px, int pz, int[] ringe, RandomSource r, boolean sued) {
        List<int[]> kandidaten = new ArrayList<>();
        for (int ring : ringe) {
            int n = Math.max(6, (int) (2 * Math.PI * ring / 13));
            double versatz = r.nextDouble() * Math.PI * 2;
            for (int i = 0; i < n; i++) {
                double a = versatz + i * Math.PI * 2 / n;
                kandidaten.add(new int[]{px + (int) Math.round(Math.cos(a) * ring), pz + (int) Math.round(Math.sin(a) * ring)});
            }
        }
        for (HausTyp typ : typen) {
            // erst die inneren Ringe, darin zufällig
            List<int[]> reihe = new ArrayList<>(kandidaten);
            Collections.shuffle(reihe, new java.util.Random(r.nextLong()));
            reihe.sort((a, b) -> Double.compare(dist(a[0], a[1], px, pz), dist(b[0], b[1], px, pz)));
            for (int[] k : reihe) {
                Haus h = new Haus(typ, k[0], k[1], d.hoehe(), richtung(px - k[0], pz - k[1]), r.nextLong());
                if (!passt(d, h, sued, px, pz)) continue;
                d.haeuser().add(h);
                kandidaten.remove(k);
                if (!typ.brunnen()) {
                    int[] tuer = h.vorDerTuer();
                    d.wege().add(new int[]{tuer[0], tuer[1], px, pz});
                }
                break;
            }
        }
    }

    private static Rotation richtung(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
        return dz > 0 ? Rotation.NONE : Rotation.CLOCKWISE_180;
    }

    private boolean passt(Dorf d, Haus h, boolean sued, int px, int pz) {
        int x0 = h.minX(), x1 = h.maxX(), z0 = h.minZ(), z1 = h.maxZ();
        // ganz im flachen Bereich
        for (int[] e : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
            if (dist(e[0], e[1], d.x(), d.z()) > d.radius() - 4) return false;
        }
        for (Haus o : d.haeuser()) {
            if (x0 - 3 <= o.maxX() && x1 + 3 >= o.minX() && z0 - 3 <= o.maxZ() && z1 + 3 >= o.minZ()) return false;
        }
        for (Platz pl : d.plaetze()) {
            double nx = Mth.clamp(pl.x(), x0, x1), nz = Mth.clamp(pl.z(), z0, z1);
            if (dist(nx, nz, pl.x(), pl.z()) < pl.r() + 3) return false;
        }
        for (int[] w : d.wege()) {
            if (abstandZuStrecke((x0 + x1) / 2.0, (z0 + z1) / 2.0, w) < Math.max(x1 - x0, z1 - z0) / 2.0 + 2) return false;
        }
        if (!h.typ().brunnen()) {
            // der neue Weg darf durch kein anderes Haus führen
            int[] tuer = h.vorDerTuer();
            int[] weg = {tuer[0], tuer[1], px, pz};
            for (Haus o : d.haeuser()) {
                if (o.typ().brunnen()) continue;
                double mx = (o.minX() + o.maxX()) / 2.0, mz = (o.minZ() + o.maxZ()) / 2.0;
                if (abstandZuStrecke(mx, mz, weg) < Math.max(o.breiteX(), o.breiteZ()) / 2.0 + 1.5) return false;
            }
        }
        if (d.haupt()) {
            // Abstand zum Fluss und zur Brücke
            int ufer = FLUSS_BREITE + UFER + 4;
            for (int x : new int[]{x0, (x0 + x1) / 2, x1}) {
                double rz = flussZ(x);
                if (sued ? z0 < rz + ufer : z1 > rz - ufer) return false;
            }
            if (x0 <= cx + BRUECKE_HB + 3 && x1 >= cx - BRUECKE_HB - 3 && z0 <= cz + BRUECKE_HL + 2 && z1 >= cz - BRUECKE_HL - 2) {
                return false;
            }
        }
        return true;
    }
}
