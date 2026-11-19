package dev.riftverse.devtest;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.universe.Einzigartig;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseProfile;
import dev.riftverse.universe.UniverseType;
import dev.riftverse.universe.dichte.DichteBefehl;
import dev.riftverse.universe.dichte.Dichtefeld;
import dev.riftverse.universe.dichte.Regionskarte;
import dev.riftverse.universe.dichte.TestGelaende;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static dev.riftverse.devtest.RauchTest.check;

/**
 * Rauchtest für Meilenstein 11 der neuen Universen (3D-Dichte und Regionssystem): Weltenhöhe pro Typ, die
 * Dichte-Testwelt mit schwebendem Testberg und Überhängen, der Lochtest (keine eingeschlossenen Luftblöcke, keine
 * schwebenden Ein-Block-Fetzen), dieselbe Höhe in {@code getBaseHeight} und im Chunk, geflutete Senken, die
 * Regionskarte mit Saum und die Leistung gegen Vanilla.
 */
final class RauchTestM11 {
    private RauchTestM11() {
    }

    static void aufbauen(MinecraftServer server) {
        // --- Weltenhöhe pro Typ (M11-08) ------------------------------------------------------------
        boolean hoehenOk = true;
        StringBuilder falsch = new StringBuilder();
        for (UniverseType t : UniverseType.values()) {
            ServerLevel u = server.getLevel(t.dimension());
            if (u == null) continue;
            boolean ok = u.getMinBuildHeight() == t.minY() && u.getHeight() == t.height() && t.height() <= UniverseType.MAX_HOEHE
                    && u.getChunkSource().getGenerator().getGenDepth() == t.height()
                    && u.getChunkSource().getGenerator().getMinY() == t.minY();
            if (!ok) falsch.append(' ').append(t.id()).append('(').append(u.getMinBuildHeight()).append('/').append(u.getHeight()).append(')');
            hoehenOk &= ok;
        }
        check(hoehenOk, "Weltenhöhe jedes Typs passt zum dimension_type und zum Generator" + falsch);

        UniverseType typ = UniverseType.TEST_DICHTE;
        check(typ.einzigartig() && typ.entwicklung() && !typ.randomTarget() && Einzigartig.of(typ) != null,
                "Dichteprobe ist einzigartig, nur für Entwickler und kein Zufallsziel");
        check(typ.minY() == -64 && typ.height() == 640, "Dichteprobe ist 640 Blöcke hoch (Y −64 bis 575)");
        ServerLevel welt = server.getLevel(typ.dimension());
        check(welt != null, "Dichteprobe ist im Entwicklungsmodus geladen");
        if (welt == null) return;

        UniverseProfile p = UniverseProfile.get(Einzigartig.id(typ));
        check(p.gelaende instanceof TestGelaende && p.regionen != null && p.terrain == UniverseProfile.Terrain.DICHTETEST,
                "Dichteprobe nutzt das 3D-Dichte-Gelände mit Regionskarte");
        if (!(p.gelaende instanceof TestGelaende g)) return;
        Dichtefeld feld = p.dichtefeld();

        // --- Chunks rund um Testberg und Bogen erzeugen -------------------------------------------
        int c = Multiverse.cellCenter(0);
        int xA = c + TestGelaende.BOGEN_DX - 40, xB = c + TestGelaende.BERG_DX + TestGelaende.BERG_R + 24;
        int zA = c - TestGelaende.BERG_R - 24, zB = c + TestGelaende.BERG_R + 24;
        int cxA = xA >> 4, cxB = xB >> 4, czA = zA >> 4, czB = zB >> 4;
        for (int cx = cxA; cx <= cxB; cx++) {
            for (int cz = czA; cz <= czB; cz++) welt.getChunk(cx, cz);
        }
        check(welt.getMaxBuildHeight() == 576, "Höchster Block der Dichteprobe ist Y 575");

        // --- Schwebender Testberg (Fertig-Kriterium) -----------------------------------------------
        int bx = c + TestGelaende.BERG_DX, bz = c + TestGelaende.BERG_DZ;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int bergOben = Integer.MIN_VALUE, bergUnten = Integer.MIN_VALUE;
        for (int y = 400; y > 120; y--) {
            boolean f = fest(welt.getBlockState(pos.set(bx, y, bz)));
            if (f && bergOben == Integer.MIN_VALUE) bergOben = y;
            if (!f && bergOben != Integer.MIN_VALUE) {
                bergUnten = y + 1;
                break;
            }
        }
        int luft = 0;
        for (int y = bergUnten - 1; y > 0 && !fest(welt.getBlockState(pos.set(bx, y, bz))); y--) luft++;
        check(bergOben > 200 && bergUnten > 0 && bergOben - bergUnten > 60 && luft > 30,
                "Schwebender Testberg: Y " + bergUnten + " bis " + bergOben + ", " + luft + " Blöcke Luft darunter");
        check(bergUnten > 0 && welt.getBlockState(pos.set(bx, bergUnten, bz)).is(ModBlocks.SCHWEBSTEIN.get()),
                "Unterseite des Testbergs ist aus Schwebstein");
        BlockState bergKrone = welt.getBlockState(pos.set(bx, bergOben, bz));
        check(bergKrone.is(BlockTags.CANDLES) || bergKrone.getBlock().getDescriptionId().endsWith("_concrete"),
                "Oberseite des Testbergs trägt die Regionsfarbe (" + bergKrone.getBlock().getName().getString() + ")");

        // --- Lochtest (M11-15): innere Chunks, deren Nachbarn alle erzeugt sind ------------------
        int loecher = 0, fetzen = 0, ueberhaenge = 0, hoehenFehler = 0, hoehenGeprueft = 0, nichtGeflutet = 0;
        String ersterFehler = "";
        Set<String> farben = new HashSet<>();
        for (int cx = cxA + 1; cx <= cxB - 1; cx++) {
            for (int cz = czA + 1; cz <= czB - 1; cz++) {
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        int x = (cx << 4) + lx, z = (cz << 4) + lz;
                        int oben = -1000;
                        boolean ueberhang = false;
                        for (int y = 330; y >= 0; y--) {
                            BlockState s = welt.getBlockState(pos.set(x, y, z));
                            if (s.isAir()) {
                                if (alleFest(welt, x, y, z)) {
                                    loecher++;
                                    if (ersterFehler.isEmpty()) ersterFehler = "Loch bei " + x + " " + y + " " + z;
                                }
                                if (oben != -1000 && y > 40 && y < TestGelaende.SCHWEBE_Y && fest(welt.getBlockState(pos.set(x, y + 1, z)))) {
                                    ueberhang = true;
                                }
                                continue;
                            }
                            if (!fest(s)) continue;
                            if (oben == -1000) {
                                oben = y;
                                farben.add(s.getBlock().getDescriptionId());
                            }
                            if (keinNachbar(welt, x, y, z)) {
                                fetzen++;
                                if (ersterFehler.isEmpty()) ersterFehler = "Fetzen bei " + x + " " + y + " " + z;
                            }
                        }
                        if (ueberhang) ueberhaenge++;
                        // Höhe aus getBaseHeight gegen den echten Chunk (M11-07), jede 4. Spalte
                        if ((lx & 3) == 0 && (lz & 3) == 0) {
                            int roh = feld.oberste(x, z);
                            int echt = hoechsterFels(welt, x, z);
                            hoehenGeprueft++;
                            if (roh != echt) hoehenFehler++;
                        }
                        // Senken unter dem Wasserspiegel sind geflutet (M11-05)
                        int boden = hoechsterFels(welt, x, z);
                        if (boden < p.fluidLevel && !welt.getBlockState(pos.set(x, p.fluidLevel, z)).is(Blocks.WATER)) nichtGeflutet++;
                    }
                }
            }
        }
        check(loecher == 0 && fetzen == 0, "Lochtest: " + loecher + " eingeschlossene Luftblöcke, " + fetzen
                + " schwebende Ein-Block-Fetzen " + ersterFehler);
        check(ueberhaenge > 20, "Überhänge und Bögen: " + ueberhaenge + " Spalten mit Fels über Luft");
        check(hoehenFehler * 50 <= hoehenGeprueft, "getBaseHeight passt zum Chunk: " + hoehenFehler + " von " + hoehenGeprueft + " Spalten abweichend");
        check(nichtGeflutet == 0, "Senken unter dem Wasserspiegel sind geflutet (" + nichtGeflutet + " trocken)");
        check(farben.size() >= 2, "Regionsfarben an der Oberfläche: " + farben.size());

        // --- Regionskarte (M11-10, M11-11) --------------------------------------------------------
        Regionskarte karte = p.regionen;
        Set<Integer> arten = new HashSet<>();
        int saum = 0, grenzeOk = 0, proben = 0;
        for (int i = 0; i < 1024; i++) {
            int x = c - 2048 + i * 4, z = c + 300;
            Regionskarte.Region r = karte.bei(x, z);
            arten.add(r.art());
            proben++;
            if (r.saum() > 0) saum++;
            if (r.saum() >= 0 && r.saum() <= 0.5 && (r.saum() == 0 || r.nachbar() != r.art())) grenzeOk++;
        }
        check(arten.size() >= 3, "Regionskarte: " + arten.size() + " Regionsarten auf 4096 Blöcken");
        check(saum > 0 && saum < proben / 2, "Übergangssaum: " + saum + " von " + proben + " Proben im Saum");
        check(grenzeOk == proben, "Saumgewicht liegt zwischen 0 und 0,5 und gehört zu einer anderen Region");
        Regionskarte.Region r1 = karte.bei(c + 123, c - 77), r2 = karte.bei(c + 123, c - 77);
        check(r1.equals(r2), "Regionskarte ist deterministisch");

        // --- Leistung (M11-06, Fertig-Kriterium) ---------------------------------------------------
        DichteBefehl.Messung m = DichteBefehl.messen(server, 48);
        if (m != null) {
            String text = String.format(Locale.ROOT, "Dichteprobe %.2f ms/Chunk, Vanilla-Oberwelt %.2f ms/Chunk", m.dichteMs(), m.vanillaMs());
            Riftverse.LOGGER.info("RIFTVERSE-RAUCHTEST: Leistung {}", text);
            check(m.schneller(), "Leistung: " + text);
        } else {
            check(false, "Leistungsmessung lief");
        }
        check(g.regionen() == karte, "Testgelände nutzt die Regionskarte des Universums");
    }

    private static boolean fest(BlockState s) {
        return !s.isAir() && s.getFluidState().isEmpty() && !s.is(BlockTags.CANDLES);
    }

    private static boolean alleFest(ServerLevel w, int x, int y, int z) {
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        return fest(w.getBlockState(q.set(x + 1, y, z))) && fest(w.getBlockState(q.set(x - 1, y, z)))
                && fest(w.getBlockState(q.set(x, y + 1, z))) && fest(w.getBlockState(q.set(x, y - 1, z)))
                && fest(w.getBlockState(q.set(x, y, z + 1))) && fest(w.getBlockState(q.set(x, y, z - 1)));
    }

    private static boolean keinNachbar(ServerLevel w, int x, int y, int z) {
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        return !fest(w.getBlockState(q.set(x + 1, y, z))) && !fest(w.getBlockState(q.set(x - 1, y, z)))
                && !fest(w.getBlockState(q.set(x, y + 1, z))) && !fest(w.getBlockState(q.set(x, y - 1, z)))
                && !fest(w.getBlockState(q.set(x, y, z + 1))) && !fest(w.getBlockState(q.set(x, y, z - 1)));
    }

    private static int hoechsterFels(ServerLevel w, int x, int z) {
        int y = w.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        while (y > w.getMinBuildHeight() && !fest(w.getBlockState(q.set(x, y, z)))) y--;
        return y;
    }
}
