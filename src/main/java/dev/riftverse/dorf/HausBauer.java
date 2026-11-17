package dev.riftverse.dorf;

import dev.riftverse.registry.ModDorf;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.List;

/**
 * Baut die Gebäude der Gleeb-Dörfer prozedural (Meilenstein 10, Entscheidung M10-09 B). Gebaut wird in lokalen
 * Koordinaten – Ursprung an der Ecke des Grundrisses, Tür vorne in der Mitte (lokal +z) – und über {@link Ziel} an
 * die richtige Stelle in der Welt gedreht. Der Bauer ist deterministisch: Er wird pro Chunk erneut aufgerufen und
 * das Ziel setzt nur die Blöcke, die in diesem Chunk liegen.
 */
public final class HausBauer {
    /** Empfängt Blöcke in Weltkoordinaten. */
    @FunctionalInterface
    public interface Ziel {
        void set(int x, int y, int z, BlockState state);
    }

    private HausBauer() {
    }

    // ------------------------------------------------------------------ Materialien

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    private static BlockState stamm(Direction.Axis achse) {
        return ModDorf.FUNKELHOLZ_STAMM.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, achse);
    }

    private static BlockState treppe(Block b, Direction f, Half half) {
        return b.defaultBlockState().setValue(StairBlock.FACING, f).setValue(StairBlock.HALF, half);
    }

    private static BlockState dachTreppe(Direction f) {
        return treppe(ModDorf.DACHZIEGEL_TREPPE.get(), f, Half.BOTTOM);
    }

    private static final BlockState LUFT = Blocks.AIR.defaultBlockState();

    private static BlockState laterne(boolean haengend) {
        return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, haengend);
    }

    // ------------------------------------------------------------------ Lokales Koordinatensystem

    /** Schreibt in lokalen Koordinaten und dreht in die Welt. */
    private static final class L {
        final Ziel ziel;
        final int ox, oy, oz, w, d;
        final Rotation rot;
        final RandomSource r;

        L(Ziel ziel, DorfPlan.Haus h) {
            this.ziel = ziel;
            this.ox = h.x();
            this.oy = h.y();
            this.oz = h.z();
            this.w = h.typ().w;
            this.d = h.typ().d;
            this.rot = h.rot();
            this.r = RandomSource.create(h.seed());
        }

        void set(int x, int y, int z, BlockState state) {
            int rx = x - w / 2, rz = z - d / 2;
            int wx, wz;
            switch (rot) {
                case CLOCKWISE_90 -> { wx = -rz; wz = rx; }
                case CLOCKWISE_180 -> { wx = -rx; wz = -rz; }
                case COUNTERCLOCKWISE_90 -> { wx = rz; wz = -rx; }
                default -> { wx = rx; wz = rz; }
            }
            ziel.set(ox + wx, oy + y, oz + wz, state.rotate(rot));
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
            for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) set(x, y, z, state);
        }

        /** Zweiteiliges: Tür. */
        void tuer(int x, int y, int z, Block door, Direction f) {
            BlockState t = door.defaultBlockState().setValue(DoorBlock.FACING, f).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
            set(x, y, z, t.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            set(x, y + 1, z, t.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }

        /** Bett: Fußende auf (x, z), Kopf in Richtung f. */
        void bett(int x, int y, int z, Block bed, Direction f) {
            BlockState b = bed.defaultBlockState().setValue(BedBlock.FACING, f);
            set(x, y, z, b.setValue(BedBlock.PART, BedPart.FOOT));
            set(x + f.getStepX(), y, z + f.getStepZ(), b.setValue(BedBlock.PART, BedPart.HEAD));
        }
    }

    // ------------------------------------------------------------------ Einstieg

    public static void bauen(DorfPlan.Haus h, Ziel ziel) {
        L b = new L(ziel, h);
        switch (h.typ()) {
            case WOHNHAUS_KLEIN -> wohnhaus(b, 1);
            case WOHNHAUS_GROSS -> wohnhaus(b, 2);
            case BAECKEREI -> baeckerei(b);
            case KRAEUTERHUETTE -> kraeuterhuette(b);
            case ALCHEMIE -> alchemie(b);
            case RUNENSCHMIEDE -> runenschmiede(b);
            case STERNWARTE -> sternwarte(b);
            case GEWAECHSHAUS -> gewaechshaus(b);
            case BIBLIOTHEK -> bibliothek(b);
            case TAVERNE -> taverne(b);
            case MAGIERTURM -> magierturm(b);
            case BRUNNEN -> brunnen(b, false);
            case MARKTBRUNNEN -> brunnen(b, true);
        }
    }

    // ------------------------------------------------------------------ Grundgerüst

    private static final int STOCK = 4;

    private enum Wand { FACHWERK, STEIN_UNTEN, MOOS_UNTEN, STEIN }

    /**
     * Fundament, Boden, Wände mit Fachwerk und Fenstern, Zwischendecken, Tür und Leiter. Gibt die Höhe der
     * Wandoberkante zurück.
     */
    private static int schale(L b, int stock, Wand wand, Block tuer) {
        int w = b.w, d = b.d, top = stock * STOCK;
        int luftBis = top + Math.max(w, d) / 2 + 3;
        b.fill(0, 1, 0, w - 1, luftBis, d - 1, LUFT);
        b.fill(-1, 1, -1, w, luftBis, -1, LUFT);
        b.fill(-1, 1, d, w, luftBis, d, LUFT);
        b.fill(-1, 1, 0, -1, luftBis, d - 1, LUFT);
        b.fill(w, 1, 0, w, luftBis, d - 1, LUFT);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                boolean rand = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                b.set(x, 0, z, rand ? s(Blocks.STONE_BRICKS) : s(ModDorf.FUNKELHOLZ_BRETTER.get()));
                b.set(x, -1, z, s(Blocks.STONE_BRICKS));
                for (int y = -2; y >= -4; y--) b.set(x, y, z, s(Blocks.COBBLESTONE));
            }
        }
        for (int y = 1; y <= top; y++) {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    boolean xr = x == 0 || x == w - 1, zr = z == 0 || z == d - 1;
                    if (!xr && !zr) continue;
                    BlockState st;
                    if (xr && zr) {
                        st = stamm(Direction.Axis.Y);
                    } else if (y % STOCK == 0) {
                        st = stamm(xr ? Direction.Axis.Z : Direction.Axis.X);
                    } else if (wand == Wand.STEIN || (y < STOCK && wand != Wand.FACHWERK)) {
                        st = wand == Wand.MOOS_UNTEN && b.r.nextInt(3) == 0 ? s(Blocks.MOSSY_STONE_BRICKS)
                                : wand == Wand.MOOS_UNTEN ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.STONE_BRICKS);
                    } else {
                        int i = xr ? z : x;
                        st = (i / 2) % 2 == 0 ? s(ModDorf.FACHWERK.get()) : s(ModDorf.FACHWERK_KREUZ.get());
                    }
                    // Fenster
                    int i = xr ? z : x, len = xr ? d : w;
                    int yy = y % STOCK;
                    if (!(xr && zr) && (yy == 2 || yy == 3) && i % 3 == 1 && i != len - 1) st = s(ModDorf.STERNGLAS.get());
                    b.set(x, y, z, st);
                }
            }
        }
        // Zwischendecken (auch unter dem Dach, damit Lampen und Kräuter hängen können) und Leiter
        for (int k = 1; k <= stock; k++) b.fill(1, k * STOCK, 1, w - 2, k * STOCK, d - 2, s(ModDorf.FUNKELHOLZ_BRETTER.get()));
        if (stock > 1) {
            for (int y = 1; y <= (stock - 1) * STOCK; y++) {
                b.set(1, y, 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
            }
        }
        // Lampen
        for (int k = 1; k <= stock; k++) b.set(w / 2, k * STOCK - 1, d / 2, laterne(true));
        // Tür vorne mittig, mit Stufe und Blumenkästen
        b.set(w / 2, 1, d - 1, LUFT);
        b.set(w / 2, 2, d - 1, LUFT);
        b.tuer(w / 2, 1, d - 1, tuer, Direction.NORTH);
        b.set(w / 2, 0, d, s(ModDorf.BUNTES_KOPFSTEINPFLASTER.get()));
        for (int x : new int[]{w / 2 - 2, w / 2 + 2}) {
            if (x > 0 && x < w - 1) {
                b.set(x, 1, d, pflanze(b.r));
                b.set(x, 0, d, s(Blocks.ROOTED_DIRT));
            }
        }
        return top;
    }

    /** Eine zufällige Leuchtpflanze (ohne Pilze). */
    private static BlockState pflanze(RandomSource r) {
        List<net.neoforged.neoforge.registries.DeferredBlock<LeuchtpflanzeBlock>> alle = List.copyOf(ModDorf.PFLANZEN.values());
        BlockState s = alle.get(r.nextInt(alle.size() - 2)).get().defaultBlockState();
        return s;
    }

    /** Satteldach aus violetten Ziegeln, First entlang der längeren Seite, mit Fachwerk-Giebeln. */
    private static int dach(L b, int top) {
        int w = b.w, d = b.d;
        boolean entlangZ = d > w;
        int quer = entlangZ ? w : d, lang = entlangZ ? d : w;
        int k = 0;
        for (; ; k++) {
            int a = -1 + k, e = quer - k;
            int y = top + k;
            if (a > e) break;
            for (int l = -1; l <= lang; l++) {
                if (a == e) {
                    setDach(b, entlangZ, a, y, l, s(ModDorf.DACHZIEGEL.get()));
                    continue;
                }
                setDach(b, entlangZ, a, y, l, dachTreppe(entlangZ ? Direction.EAST : Direction.SOUTH));
                setDach(b, entlangZ, e, y, l, dachTreppe(entlangZ ? Direction.WEST : Direction.NORTH));
                if (e == a + 1 && l >= 0 && l < lang) {
                    // Firstreihe
                    setDach(b, entlangZ, a, y + 1, l, s(ModDorf.DACHZIEGEL_STUFE.get()).setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                    setDach(b, entlangZ, e, y + 1, l, s(ModDorf.DACHZIEGEL_STUFE.get()).setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                }
            }
            // Giebel
            for (int q = a + 1; q < e; q++) {
                if (k == 0) continue;
                BlockState g = (q % 2 == 0) ? s(ModDorf.FACHWERK.get()) : s(ModDorf.FACHWERK_KREUZ.get());
                if (q == quer / 2 && k >= 2) g = s(ModDorf.STERNGLAS.get());
                setDach(b, entlangZ, q, y, 0, g);
                setDach(b, entlangZ, q, y, lang - 1, g);
            }
        }
        return top + k;
    }

    private static void setDach(L b, boolean entlangZ, int q, int y, int l, BlockState s) {
        if (entlangZ) b.set(q, y, l, s);
        else b.set(l, y, q, s);
    }

    /** Schornstein mit Rauch (Lagerfeuer oben). */
    private static void schornstein(L b, int x, int z, int bisY) {
        for (int y = 1; y <= bisY; y++) b.set(x, y, z, s(Blocks.BRICKS));
        b.set(x, bisY + 1, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
    }

    private static BlockState kiste() {
        return s(Blocks.CHEST);
    }

    private static BlockState fass() {
        return s(Blocks.BARREL).setValue(net.minecraft.world.level.block.BarrelBlock.FACING, Direction.UP);
    }

    private static void tisch(L b, int x, int y, int z) {
        b.set(x, y, z, s(Blocks.CHERRY_FENCE));
        b.set(x, y + 1, z, s(Blocks.CHERRY_PRESSURE_PLATE));
    }

    // ------------------------------------------------------------------ Häuser

    private static void wohnhaus(L b, int stock) {
        int top = schale(b, stock, stock == 1 ? Wand.FACHWERK : Wand.STEIN_UNTEN, Blocks.CHERRY_DOOR);
        int w = b.w, d = b.d;
        Block[] betten = {Blocks.PINK_BED, Blocks.PURPLE_BED, Blocks.LIGHT_BLUE_BED, Blocks.LIME_BED, Blocks.YELLOW_BED};
        int yb = stock == 1 ? 1 : STOCK + 1;
        b.bett(w - 2, yb, 2, betten[b.r.nextInt(betten.length)], Direction.NORTH);
        if (stock > 1 || w >= 9) b.bett(w - 3, yb, 2, betten[b.r.nextInt(betten.length)], Direction.NORTH);
        b.set(1, 1, d - 2, s(Blocks.CRAFTING_TABLE));
        b.set(w - 2, 1, d - 2, kiste());
        b.set(2, 1, 1, fass());
        b.set(w / 2, 1, d / 2 - 1, s(Blocks.MAGENTA_CARPET));
        b.set(w / 2 - 1, 1, d / 2 - 1, s(Blocks.PINK_CARPET));
        if (stock > 1) {
            tisch(b, w / 2, 1, 2);
            b.set(w / 2 - 1, 1, 2, treppe(Blocks.CHERRY_STAIRS, Direction.EAST, Half.BOTTOM));
            b.set(w - 2, STOCK + 1, d - 2, s(Blocks.BOOKSHELF));
            b.set(2, STOCK + 1, d - 2, s(ModDorf.FEENLICHT.get()));
        }
        dach(b, top);
    }

    private static void baeckerei(L b) {
        int top = schale(b, 1, Wand.STEIN_UNTEN, Blocks.CHERRY_DOOR);
        int w = b.w, d = b.d;
        b.set(1, 1, 2, s(Blocks.SMOKER).setValue(net.minecraft.world.level.block.SmokerBlock.FACING, Direction.EAST));
        b.set(1, 1, 3, s(Blocks.FURNACE).setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, Direction.EAST));
        b.set(1, 1, 4, s(Blocks.SMOKER).setValue(net.minecraft.world.level.block.SmokerBlock.FACING, Direction.EAST));
        for (int x = 3; x <= w - 3; x++) b.set(x, 1, 3, s(Blocks.SMOOTH_STONE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
        b.set(4, 2, 3, s(Blocks.CAKE));
        b.set(w - 2, 1, 1, fass());
        b.set(w - 2, 1, 2, fass());
        b.set(w - 2, 1, d - 2, s(Blocks.HAY_BLOCK));
        b.set(w - 2, 2, d - 2, s(Blocks.HAY_BLOCK));
        int first = dach(b, top);
        schornstein(b, 1, 1, first + 1);
    }

    private static void kraeuterhuette(L b) {
        int top = schale(b, 1, Wand.MOOS_UNTEN, Blocks.SPRUCE_DOOR);
        int w = b.w, d = b.d;
        b.set(w / 2, 1, 2, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        b.set(1, 1, d - 2, s(Blocks.COMPOSTER));
        b.set(w - 2, 1, 1, fass());
        b.set(1, 1, 1, s(Blocks.POTTED_FERN));
        b.set(w - 2, 1, d - 2, s(Blocks.POTTED_RED_MUSHROOM));
        for (int x = 1; x < w - 1; x++) {
            for (int z = 1; z < d - 1; z++) {
                if ((x + z) % 3 == 0 && b.r.nextInt(3) > 0) b.set(x, top - 1, z, s(ModDorf.KRAEUTERBUENDEL.get()));
            }
        }
        b.set(w / 2, top - 1, d / 2, laterne(true));
        // Moos auf dem Dachrand und Lichtranken
        dach(b, top);
        b.set(-1, top - 1, 1, s(ModDorf.LICHTRANKE.get()));
        b.set(w, top - 1, d - 2, s(ModDorf.LICHTRANKE.get()));
    }

    private static void alchemie(L b) {
        int top = schale(b, 1, Wand.STEIN_UNTEN, Blocks.CHERRY_DOOR);
        int w = b.w, d = b.d;
        for (int x = 2; x <= w - 3; x++) b.set(x, 1, 1, s(Blocks.POLISHED_DEEPSLATE_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
        b.set(3, 2, 1, s(Blocks.BREWING_STAND));
        b.set(w - 4, 2, 1, s(Blocks.BREWING_STAND));
        b.set(w / 2, 1, d / 2, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2));
        b.set(1, 1, 3, s(Blocks.BOOKSHELF));
        b.set(1, 2, 3, s(Blocks.BOOKSHELF));
        b.set(w - 2, 1, 3, s(Blocks.BOOKSHELF));
        b.set(w - 2, 2, 3, s(ModDorf.FEENLICHT.get()));
        b.set(1, 1, d - 2, kiste());
        b.set(w - 2, 1, d - 2, s(Blocks.AMETHYST_BLOCK));
        b.set(w - 2, 2, d - 2, s(Blocks.AMETHYST_CLUSTER));
        dach(b, top);
    }

    private static void runenschmiede(L b) {
        int top = schale(b, 1, Wand.STEIN, Blocks.SPRUCE_DOOR);
        int w = b.w, d = b.d;
        b.set(1, 1, 1, s(Blocks.BLAST_FURNACE).setValue(net.minecraft.world.level.block.BlastFurnaceBlock.FACING, Direction.SOUTH));
        b.set(2, 1, 1, s(Blocks.SMITHING_TABLE));
        b.set(w / 2, 1, 3, s(Blocks.ANVIL));
        b.set(w - 2, 1, 1, s(Blocks.GRINDSTONE));
        b.set(w - 3, 1, 1, s(Blocks.STONECUTTER));
        b.set(1, 1, d - 2, kiste());
        b.set(w - 2, 1, d - 2, s(Blocks.CHISELED_STONE_BRICKS));
        b.set(w - 2, 2, d - 2, s(Blocks.AMETHYST_CLUSTER));
        b.set(1, 1, d / 2, s(Blocks.CHISELED_STONE_BRICKS));
        int first = dach(b, top);
        schornstein(b, w - 2, 2, first);
    }

    private static void bibliothek(L b) {
        int top = schale(b, 2, Wand.STEIN_UNTEN, Blocks.CHERRY_DOOR);
        int w = b.w, d = b.d;
        for (int z = 2; z < d - 2; z++) {
            for (int y = 1; y <= 3; y++) {
                b.set(w - 2, y, z, s(Blocks.BOOKSHELF));
                if (z > 2) b.set(1, y, z, s(Blocks.BOOKSHELF));
                b.set(w - 2, STOCK + y, z, y == 2 && z % 3 == 0 ? s(Blocks.CHISELED_BOOKSHELF) : s(Blocks.BOOKSHELF));
            }
        }
        b.set(w / 2, 1, 3, s(Blocks.LECTERN));
        b.set(w / 2, STOCK + 1, d / 2, s(Blocks.ENCHANTING_TABLE));
        tisch(b, w / 2, 1, d / 2 + 1);
        b.set(w / 2, STOCK + 1, 2, s(ModDorf.FEENLICHT.get()));
        dach(b, top);
    }

    private static void gewaechshaus(L b) {
        int w = b.w, d = b.d, top = 5;
        b.fill(-1, 1, -1, w, top + w / 2 + 2, d, LUFT);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                b.set(x, -1, z, s(Blocks.STONE_BRICKS));
                boolean rand = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                b.set(x, 0, z, rand ? s(Blocks.STONE_BRICKS) : (x == w / 2 ? s(ModDorf.FUNKELHOLZ_BRETTER.get()) : s(Blocks.DIRT)));
                if (rand) {
                    for (int y = 1; y <= top; y++) {
                        boolean pfosten = (x == 0 || x == w - 1) && (z == 0 || z == d - 1 || z % 3 == 0)
                                || (z == 0 || z == d - 1) && x % 4 == 0;
                        b.set(x, y, z, pfosten ? stamm(Direction.Axis.Y) : s(Blocks.GLASS));
                    }
                }
            }
        }
        // Beete mit Leuchtpflanzen, Wasser in der Mitte
        for (int z = 1; z < d - 1; z++) {
            for (int x = 1; x < w - 1; x++) {
                if (x == w / 2) continue;
                if ((x == 2 || x == w - 3) && z % 4 == 2) {
                    b.set(x, 0, z, s(Blocks.WATER));
                    continue;
                }
                b.set(x, 1, z, pflanze(b.r));
            }
        }
        // Glasdach, gestuft
        for (int k = 0; k <= w / 2; k++) {
            for (int z = 0; z < d; z++) {
                b.set(k, top + 1 + k, z, s(ModDorf.STERNGLAS.get()));
                b.set(w - 1 - k, top + 1 + k, z, s(ModDorf.STERNGLAS.get()));
            }
            for (int x = k + 1; x < w - 1 - k; x++) {
                b.set(x, top + 1 + k, 0, s(Blocks.GLASS));
                b.set(x, top + 1 + k, d - 1, s(Blocks.GLASS));
            }
        }
        b.set(w / 2, 1, d - 1, LUFT);
        b.tuer(w / 2, 1, d - 1, Blocks.CHERRY_DOOR, Direction.NORTH);
        b.set(w / 2, top, d / 2, laterne(true));
    }

    // ------------------------------------------------------------------ Türme

    private static boolean ring(double dx, double dz, double r) {
        double q = dx * dx + dz * dz;
        return q <= r * r + r && q > (r - 1) * (r - 1) + (r - 1);
    }

    private static boolean innen(double dx, double dz, double r) {
        return dx * dx + dz * dz <= (r - 1) * (r - 1) + (r - 1);
    }

    /** Runder Turm um die Grundrissmitte. Gibt die Wandoberkante zurück. */
    private static int turm(L b, int r, int hoehe, int ebenen) {
        int mx = b.w / 2, mz = b.d / 2;
        for (int x = mx - r - 1; x <= mx + r + 1; x++) {
            for (int z = mz - r - 1; z <= mz + r + 1; z++) {
                int dx = x - mx, dz = z - mz;
                boolean in = innen(dx, dz, r), wand = ring(dx, dz, r);
                if (!in && !wand) continue;
                b.set(x, 0, z, wand ? s(Blocks.STONE_BRICKS) : s(ModDorf.FUNKELHOLZ_BRETTER.get()));
                for (int y = -4; y <= -1; y++) b.set(x, y, z, s(Blocks.COBBLESTONE));
                for (int y = 1; y <= hoehe + r * 2 + 3; y++) b.set(x, y, z, LUFT);
                if (!wand) continue;
                for (int y = 1; y <= hoehe; y++) {
                    BlockState st;
                    if (y % 6 == 0) st = s(Blocks.CHISELED_STONE_BRICKS);
                    else if (y < 4) st = b.r.nextInt(4) == 0 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
                    else st = (y / 3 + Math.abs(dx + dz)) % 5 == 0 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
                    boolean fenster = (y % 6 == 3 || y % 6 == 4) && (dx == 0 || dz == 0) && y > 2;
                    b.set(x, y, z, fenster ? s(ModDorf.STERNGLAS.get()) : st);
                }
            }
        }
        // Ebenen mit Leiter an der Nordwand (lokal -z)
        for (int e = 1; e < ebenen; e++) {
            int y = e * 6;
            for (int x = mx - r; x <= mx + r; x++) {
                for (int z = mz - r; z <= mz + r; z++) {
                    if (innen(x - mx, z - mz, r)) b.set(x, y, z, s(ModDorf.FUNKELHOLZ_BRETTER.get()));
                }
            }
            b.set(mx, y - 1, mz, laterne(true));
        }
        for (int y = 1; y <= (ebenen - 1) * 6; y++) {
            b.set(mx, y, mz - r + 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        // Tür
        b.set(mx, 1, mz + r, LUFT);
        b.set(mx, 2, mz + r, LUFT);
        b.tuer(mx, 1, mz + r, Blocks.CHERRY_DOOR, Direction.NORTH);
        b.set(mx, 0, mz + r + 1, s(ModDorf.BUNTES_KOPFSTEINPFLASTER.get()));
        return hoehe;
    }

    /** Spitzes Kegeldach aus violetten Ziegeln mit Laterne über der Spitze. */
    private static void kegeldach(L b, int r, int top) {
        int mx = b.w / 2, mz = b.d / 2;
        int hoehe = (r + 1) * 2;
        for (int k = 0; k <= hoehe; k++) {
            double rr = (r + 1.5) * (1.0 - (double) k / (hoehe + 1));
            for (int x = mx - r - 2; x <= mx + r + 2; x++) {
                for (int z = mz - r - 2; z <= mz + r + 2; z++) {
                    double q = Math.sqrt((x - mx) * (x - mx) + (z - mz) * (z - mz));
                    if (q <= rr && q > rr - 1.3) b.set(x, top + 1 + k, z, s(ModDorf.DACHZIEGEL.get()));
                }
            }
        }
        b.set(mx, top + hoehe + 2, mz, stamm(Direction.Axis.Y));
        b.set(mx, top + hoehe + 4, mz, s(ModDorf.SCHWEBELATERNE.get()));
    }

    private static void magierturm(L b) {
        int r = 5;
        int top = turm(b, r, 24, 4);
        int mx = b.w / 2, mz = b.d / 2;
        // Oben: Studierzimmer
        b.set(mx, 19, mz + 2, s(Blocks.ENCHANTING_TABLE));
        b.set(mx - 2, 19, mz - 2, s(Blocks.BOOKSHELF));
        b.set(mx + 2, 19, mz - 2, s(Blocks.BOOKSHELF));
        b.set(mx - 3, 19, mz + 1, s(Blocks.BREWING_STAND));
        b.set(mx + 3, 19, mz + 1, s(ModDorf.FEENLICHT.get()));
        b.set(mx + 3, 7, mz, kiste());
        b.set(mx - 3, 7, mz, s(Blocks.LECTERN));
        b.bett(mx + 2, 13, mz + 2, Blocks.PURPLE_BED, Direction.NORTH);
        b.set(mx - 3, 1, mz, fass());
        // Zinnen-Balkon
        for (int x = mx - r - 1; x <= mx + r + 1; x++) {
            for (int z = mz - r - 1; z <= mz + r + 1; z++) {
                if (ring(x - mx, z - mz, r + 1)) b.set(x, top - 6, z, s(Blocks.STONE_BRICK_SLAB).setValue(SlabBlock.TYPE, SlabType.TOP));
            }
        }
        kegeldach(b, r, top);
    }

    private static void sternwarte(L b) {
        int r = 4;
        int top = turm(b, r, 12, 2);
        int mx = b.w / 2, mz = b.d / 2;
        // Glaskuppel
        for (int x = mx - r; x <= mx + r; x++) {
            for (int z = mz - r; z <= mz + r; z++) {
                for (int y = 0; y <= r; y++) {
                    double q = (x - mx) * (x - mx) + (z - mz) * (z - mz) + y * y;
                    if (q <= r * r + r && q > (r - 1) * (r - 1) + (r - 1)) b.set(x, top + 1 + y, z, s(ModDorf.STERNGLAS.get()));
                }
            }
        }
        b.set(mx, 7, mz, s(Blocks.LECTERN));
        b.set(mx + 2, 7, mz, s(Blocks.AMETHYST_BLOCK));
        b.set(mx + 2, 8, mz, s(Blocks.AMETHYST_CLUSTER));
        b.set(mx - 2, 7, mz + 1, kiste());
        b.set(mx, 1, mz - 2, s(Blocks.CARTOGRAPHY_TABLE));
        b.set(mx + 2, 1, mz, s(Blocks.BOOKSHELF));
    }

    // ------------------------------------------------------------------ Brunnen und Bridge's Inn

    private static void brunnen(L b, boolean markt) {
        int w = b.w, d = b.d, mx = w / 2, mz = d / 2;
        b.fill(0, 1, 0, w - 1, 6, d - 1, LUFT);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                boolean rand = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                for (int y = -4; y <= -1; y++) b.set(x, y, z, s(Blocks.STONE_BRICKS));
                b.set(x, 0, z, rand ? s(Blocks.STONE_BRICKS) : s(Blocks.WATER));
                if (!rand) {
                    b.set(x, -1, z, s(Blocks.WATER));
                    b.set(x, -2, z, s(Blocks.WATER));
                }
                if (rand) b.set(x, 1, z, s(Blocks.STONE_BRICK_SLAB));
            }
        }
        if (markt) {
            for (int y = -2; y <= 3; y++) b.set(mx, y, mz, y == 3 ? s(ModDorf.STERNGLAS.get()) : s(Blocks.CHISELED_STONE_BRICKS));
            b.set(mx, 4, mz, s(ModDorf.FEENLICHT.get()));
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                b.set(mx + dir.getStepX() * 2, 0, mz + dir.getStepZ() * 2, s(Blocks.SEA_LANTERN));
            }
            b.set(mx, 7, mz, s(ModDorf.SCHWEBELATERNE.get()));
        } else {
            for (int[] e : new int[][]{{0, 0}, {w - 1, 0}, {0, d - 1}, {w - 1, d - 1}}) {
                for (int y = 1; y <= 3; y++) b.set(e[0], y, e[1], stamm(Direction.Axis.Y));
            }
            for (int x = 0; x < w; x++) for (int z = 0; z < d; z++) {
                boolean rand = x == 0 || z == 0 || x == w - 1 || z == d - 1;
                b.set(x, 4, z, rand ? s(ModDorf.DACHZIEGEL_STUFE.get()) : s(ModDorf.DACHZIEGEL.get()));
            }
            b.set(mx, 5, mz, s(ModDorf.DACHZIEGEL_STUFE.get()));
            b.set(mx, 3, mz, laterne(true));
        }
    }

    /**
     * Bridge's Inn: ein zweistöckiges Wirtshaus mitten auf einer Steinbrücke über den Fluss. Die Brücke reicht
     * {@link DorfPlan#BRUECKE_HL} Blöcke zu beiden Seiten der Mitte; der Weg führt durch beide Türen des Wirtshauses.
     */
    private static void taverne(L b) {
        int w = b.w, d = b.d, mz = d / 2;
        int z0 = mz - DorfPlan.BRUECKE_HL, z1 = mz + DorfPlan.BRUECKE_HL;
        // Brücke
        for (int z = z0; z <= z1; z++) {
            for (int x = -1; x <= w; x++) {
                boolean kante = x == -1 || x == w;
                b.set(x, 0, z, kante ? s(Blocks.STONE_BRICKS) : (x >= w / 2 - 1 && x <= w / 2 + 1)
                        ? s(ModDorf.BUNTES_KOPFSTEINPFLASTER.get()) : s(Blocks.STONE_BRICKS));
                b.set(x, -1, z, s(Blocks.STONE_BRICKS));
                // Bogen unter der Brücke: in der Mitte flacher
                int tiefe = 2 + (int) Math.round(Math.pow(Math.abs(z - mz) / (double) DorfPlan.BRUECKE_HL, 2) * 6);
                for (int y = -2; y >= -tiefe; y--) b.set(x, y, z, s(Blocks.STONE_BRICKS));
                if (Math.floorMod(z - z0, 9) == 0) {
                    for (int y = -2; y >= -14; y--) b.set(x, y, z, s(Blocks.STONE_BRICKS));
                }
                if (z < 0 || z >= d) {
                    for (int y = 1; y <= 5; y++) b.set(x, y, z, LUFT);
                    if (kante) {
                        b.set(x, 1, z, Math.floorMod(z - z0, 6) == 0 ? s(Blocks.STONE_BRICKS) : s(Blocks.STONE_BRICK_WALL));
                        if (Math.floorMod(z - z0, 6) == 0) b.set(x, 3, z, s(ModDorf.SCHWEBELATERNE.get()));
                    }
                }
            }
        }
        // Wirtshaus (Vorderseite +z, hinten eine zweite Tür)
        int top = schale(b, 2, Wand.STEIN_UNTEN, Blocks.CHERRY_DOOR);
        b.set(w / 2, 1, 0, LUFT);
        b.set(w / 2, 2, 0, LUFT);
        b.tuer(w / 2, 1, 0, Blocks.CHERRY_DOOR, Direction.SOUTH);
        // Erdgeschoss: Theke, Tische, Kamin
        for (int z = 2; z <= 6; z++) b.set(w - 3, 1, z, s(Blocks.STRIPPED_CHERRY_LOG));
        b.set(w - 2, 1, 2, fass());
        b.set(w - 2, 1, 3, fass());
        b.set(w - 2, 2, 2, fass());
        b.set(w - 2, 1, 5, s(Blocks.SMOKER).setValue(net.minecraft.world.level.block.SmokerBlock.FACING, Direction.WEST));
        b.set(w - 3, 2, 4, s(Blocks.CAKE));
        for (int[] t : new int[][]{{2, 3}, {2, 8}, {5, 10}, {2, 12}}) {
            tisch(b, t[0], 1, t[1]);
            b.set(t[0], 1, t[1] - 1, treppe(Blocks.CHERRY_STAIRS, Direction.NORTH, Half.BOTTOM));
            b.set(t[0], 1, t[1] + 1, treppe(Blocks.CHERRY_STAIRS, Direction.SOUTH, Half.BOTTOM));
        }
        b.set(w - 2, 1, d - 3, s(Blocks.BRICKS));
        b.set(w - 2, 1, d - 4, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        // Obergeschoss: Gästezimmer
        Block[] betten = {Blocks.PINK_BED, Blocks.PURPLE_BED, Blocks.LIGHT_BLUE_BED, Blocks.YELLOW_BED};
        for (int i = 0; i < 3; i++) {
            int z = 2 + i * 3;
            b.bett(w - 2, STOCK + 1, z, betten[i], Direction.WEST);
            b.set(w - 2, STOCK + 1, z + 1, kiste());
        }
        b.set(2, STOCK + 1, d - 2, s(ModDorf.FEENLICHT.get()));
        b.set(2, STOCK + 1, 2, s(Blocks.BOOKSHELF));
        int first = dach(b, top);
        schornstein(b, w - 2, d - 3, first);
        // Schild: hängende Laternen am Eingang
        b.set(w / 2 - 1, 3, d, s(ModDorf.SCHWEBELATERNE.get()));
        b.set(w / 2 + 1, 3, d, s(ModDorf.SCHWEBELATERNE.get()));
        b.set(w / 2 - 1, 3, -1, s(ModDorf.SCHWEBELATERNE.get()));
        b.set(w / 2 + 1, 3, -1, s(ModDorf.SCHWEBELATERNE.get()));
    }
}
