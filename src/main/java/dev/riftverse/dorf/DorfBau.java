package dev.riftverse.dorf;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModDorf;
import dev.riftverse.registry.ModEntities;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

/**
 * Baut Bridge's Inn in den Chunk (Meilenstein 10): Blumenwiesen, Funkelbäume, Wege, Laternen, die Häuser der
 * Gleeb-Dörfer und die Orte in der Natur. In {@link #gestalten} (beim Gelände) entstehen alle Blöcke samt Kisten,
 * Fässern, Betten und Öfen, in {@link #beleben} (Spawn-Schritt) die Gleebs und die ersten Tiere.
 */
public final class DorfBau {
    public static final ResourceKey<LootTable> KISTE_HAUS = ResourceKey.create(Registries.LOOT_TABLE, Riftverse.id("chests/gleeb_haus"));
    public static final ResourceKey<LootTable> KISTE_IRRLICHT = ResourceKey.create(Registries.LOOT_TABLE, Riftverse.id("chests/irrlicht"));

    private DorfBau() {
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    // ------------------------------------------------------------------ Schritt 1: Gelände

    public static void gestalten(ChunkAccess chunk, UniverseProfile p, int[][] tops, BlockPos.MutableBlockPos pos) {
        DorfPlan plan = p.dorfPlan();
        ChunkPos cp = chunk.getPos();
        int bx = cp.getMinBlockX(), bz = cp.getMinBlockZ();
        RandomSource rnd = RandomSource.create(UniverseId.mix(p.seed ^ cp.toLong() ^ 0xB10E5L));
        List<DorfPlan.Dorf> doerfer = plan.doerferIn(bx - 16, bz - 16, bx + 31, bz + 31);
        BlockState pflaster = s(ModDorf.BUNTES_KOPFSTEINPFLASTER.get());

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int top = tops[lx][lz];
                int x = bx + lx, z = bz + lz;
                if (top == Integer.MIN_VALUE) continue;
                if (top < DorfPlan.WASSER) {
                    // Seerosen am Rand von See und Fluss
                    if (top >= DorfPlan.WASSER - 3 && rnd.nextFloat() < 0.05f) {
                        chunk.setBlockState(pos.set(x, DorfPlan.WASSER + 1, z), s(Blocks.LILY_PAD), false);
                    }
                    continue;
                }
                DorfPlan.Dorf d = plan.dorfBei(x, z);
                if (d != null) {
                    if (DorfPlan.aufWeg(d, x, z)) {
                        chunk.setBlockState(pos.set(x, top, z), pflaster, false);
                    } else if (rnd.nextFloat() < 0.07f) {
                        chunk.setBlockState(pos.set(x, top + 1, z), rnd.nextInt(3) == 0 ? blume(rnd, 0) : s(Blocks.SHORT_GRASS), false);
                    }
                    continue;
                }
                if (!plan.frei(x, z, 6)) continue;
                boolean innen = lx >= 3 && lx <= 12 && lz >= 3 && lz <= 12;
                double wald = 1 + 7 * Math.max(0, p.feld(x / 300.0, z / 300.0));
                if (innen && rnd.nextFloat() < p.featureDensity * wald
                        && chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) {
                    baum(chunk, pos, x, top + 1, z, rnd);
                    continue;
                }
                if (!chunk.getBlockState(pos.set(x, top + 1, z)).isAir()) continue;
                double nahFluss = Math.abs(z - plan.flussZ(x));
                if (nahFluss < DorfPlan.FLUSS_BREITE + DorfPlan.UFER + 3 && rnd.nextFloat() < 0.12f) {
                    chunk.setBlockState(pos.set(x, top + 1, z), s(ModDorf.MONDKRAUT.get()), false);
                    continue;
                }
                if (rnd.nextFloat() < p.plantDensity) {
                    int thema = Mth.clamp((int) ((p.feld(x / 55.0, z / 55.0) + 1) * 3), 0, 5);
                    pflanzen(chunk, pos, x, top + 1, z, thema, rnd);
                }
            }
        }

        // Häuser, Laternen und Orte in der Natur
        HausBauer.Ziel ziel = chunkZiel(chunk, pos, KISTE_HAUS, rnd);
        for (DorfPlan.Dorf d : doerfer) {
            laternen(d, ziel);
            for (DorfPlan.Haus h : d.haeuser()) {
                if (h.maxX() + 3 < bx || h.minX() - 3 > bx + 15 || h.maxZ() + 3 < bz || h.minZ() - 3 > bz + 15) {
                    if (h.typ() != HausTyp.TAVERNE) continue;
                }
                HausBauer.bauen(h, ziel);
            }
        }
        HausBauer.Ziel natur = chunkZiel(chunk, pos, KISTE_IRRLICHT, rnd);
        for (DorfPlan.Naturort o : plan.orteIn(bx, bz, bx + 15, bz + 15, 16)) {
            Naturorte.bauen(o, plan, natur);
        }
    }

    /**
     * Schreibt nur in diesen Chunk. Blöcke mit Block-Entity (Kisten, Fässer, Betten, Öfen …) bekommen sie gleich
     * mit, Kisten und Fässer dazu ihre Loot-Tabelle – im Spawn-Schritt dürfte man keine Blöcke mehr setzen.
     */
    private static HausBauer.Ziel chunkZiel(ChunkAccess chunk, BlockPos.MutableBlockPos pos, ResourceKey<LootTable> loot,
                                            RandomSource rnd) {
        int bx = chunk.getPos().getMinBlockX(), bz = chunk.getPos().getMinBlockZ();
        return (x, y, z, st) -> {
            if (x < bx || x > bx + 15 || z < bz || z > bz + 15) return;
            if (y <= chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) return;
            pos.set(x, y, z);
            BlockState alt = chunk.setBlockState(pos, st, false);
            if (st.hasBlockEntity() && st.getBlock() instanceof EntityBlock eb) {
                BlockEntity be = eb.newBlockEntity(pos.immutable(), st);
                if (be != null) {
                    if (be instanceof RandomizableContainerBlockEntity k) k.setLootTable(loot, rnd.nextLong());
                    chunk.setBlockEntity(be);
                }
            } else if (alt != null && alt.hasBlockEntity()) {
                chunk.removeBlockEntity(pos);
            }
        };
    }

    /** Schwebelaternen entlang der Wege und rund um die Plätze. */
    private static void laternen(DorfPlan.Dorf d, HausBauer.Ziel ziel) {
        BlockState l = s(ModDorf.SCHWEBELATERNE.get());
        int y = d.hoehe() + 3;
        for (int[] w : d.wege()) {
            double dx = w[2] - w[0], dz = w[3] - w[1], len = Math.sqrt(dx * dx + dz * dz);
            if (len < 6) continue;
            int i = 0;
            for (double t = 4; t < len - 6; t += 9, i++) {
                double px = w[0] + dx * t / len, pz = w[1] + dz * t / len;
                int seite = i % 2 == 0 ? 2 : -2;
                int lx = (int) Math.round(px - dz / len * seite), lz = (int) Math.round(pz + dx / len * seite);
                ziel.set(lx, y, lz, l);
            }
        }
        for (DorfPlan.Platz pl : d.plaetze()) {
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4 + 0.3;
                ziel.set(pl.x() + (int) Math.round(Math.cos(a) * (pl.r() + 1)), y + 1,
                        pl.z() + (int) Math.round(Math.sin(a) * (pl.r() + 1)), l);
            }
        }
    }

    // ------------------------------------------------------------------ Pflanzen und Bäume

    private static final Block[][] THEMEN = {
            {Blocks.POPPY, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.DANDELION},
            {Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.ALLIUM, Blocks.LILY_OF_THE_VALLEY},
            {Blocks.PINK_TULIP, Blocks.ALLIUM, Blocks.WHITE_TULIP, Blocks.OXEYE_DAISY},
            {Blocks.DANDELION, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.POPPY},
            {Blocks.AZURE_BLUET, Blocks.WHITE_TULIP, Blocks.LILY_OF_THE_VALLEY, Blocks.ORANGE_TULIP},
            {Blocks.ALLIUM, Blocks.PINK_TULIP, Blocks.POPPY, Blocks.CORNFLOWER},
    };

    /** Zu jedem Blumenthema passende Leuchtpflanzen. */
    private static final String[][] LEUCHTEND = {
            {"funkenmohn_rot", "funkenmohn_orange", "sternenblume_gold"},
            {"leuchtglocke_blau", "leuchtglocke_tuerkis", "mondkraut"},
            {"leuchtglocke_rosa", "leuchtglocke_violett", "sternenblume_weiss"},
            {"sternenblume_gold", "sternenblume_weiss", "funkenmohn_orange"},
            {"leuchtglocke_tuerkis", "mondkraut", "sternenblume_weiss"},
            {"leuchtglocke_violett", "leuchtglocke_rosa", "funkenmohn_rot"},
    };

    private static BlockState blume(RandomSource rnd, int thema) {
        Block[] t = THEMEN[thema];
        return s(t[rnd.nextInt(t.length)]);
    }

    private static void pflanzen(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z, int thema, RandomSource rnd) {
        float r = rnd.nextFloat();
        if (r < 0.20f) {
            String[] l = LEUCHTEND[thema];
            chunk.setBlockState(pos.set(x, y, z), ModDorf.PFLANZEN.get(l[rnd.nextInt(l.length)]).get().defaultBlockState(), false);
        } else if (r < 0.55f) {
            chunk.setBlockState(pos.set(x, y, z), blume(rnd, thema), false);
        } else if (r < 0.63f) {
            chunk.setBlockState(pos.set(x, y, z), s(Blocks.PINK_PETALS).setValue(PinkPetalsBlock.AMOUNT, 1 + rnd.nextInt(4))
                    .setValue(PinkPetalsBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(rnd)), false);
        } else if (r < 0.70f && chunk.getBlockState(pos.set(x, y + 1, z)).isAir()) {
            Block hoch = switch (thema) {
                case 0, 5 -> Blocks.ROSE_BUSH;
                case 1, 4 -> Blocks.TALL_GRASS;
                case 2 -> Blocks.PEONY;
                default -> Blocks.LILAC;
            };
            chunk.setBlockState(pos.set(x, y, z), s(hoch).setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), false);
            chunk.setBlockState(pos.set(x, y + 1, z), s(hoch).setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), false);
        } else {
            chunk.setBlockState(pos.set(x, y, z), rnd.nextInt(5) == 0 ? s(Blocks.FERN) : s(Blocks.SHORT_GRASS), false);
        }
    }

    private static BlockState laub(Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** Funkelbaum (rosa Laub, oft mit Lichtranken), Kirschbaum oder Birke mit blühendem Azaleenlaub. */
    private static void baum(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z, RandomSource rnd) {
        int art = rnd.nextInt(10);
        BlockState stamm, laub;
        if (art < 5) {
            stamm = ModDorf.FUNKELHOLZ_STAMM.get().defaultBlockState();
            laub = laub(ModDorf.FUNKELLAUB.get());
        } else if (art < 8) {
            stamm = s(Blocks.CHERRY_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            laub = laub(Blocks.CHERRY_LEAVES);
        } else {
            stamm = s(Blocks.BIRCH_LOG);
            laub = laub(Blocks.FLOWERING_AZALEA_LEAVES);
        }
        int h = 4 + rnd.nextInt(3);
        for (int i = 0; i < h; i++) chunk.setBlockState(pos.set(x, y + i, z), stamm, false);
        int ky = y + h - 1;
        double r = 2.2 + rnd.nextDouble() * 0.8;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    double q = dx * dx + dy * dy * 1.6 + dz * dz;
                    if (q > r * r || (dx == 0 && dz == 0 && dy <= 0)) continue;
                    if (q > (r - 0.7) * (r - 0.7) && rnd.nextInt(4) == 0) continue;
                    pos.set(x + dx, ky + dy, z + dz);
                    if (chunk.getBlockState(pos).isAir()) chunk.setBlockState(pos, laub, false);
                }
            }
        }
        chunk.setBlockState(pos.set(x, ky + 1, z), laub, false);
        // Lichtranken unter dem Funkellaub, Zauberpilze am Stamm
        if (art < 5) {
            for (int i = 0; i < 4; i++) {
                int rx = x + rnd.nextInt(5) - 2, rz = z + rnd.nextInt(5) - 2;
                int ry = ky - 2;
                if (!chunk.getBlockState(pos.set(rx, ry + 1, rz)).is(ModDorf.FUNKELLAUB.get())) continue;
                for (int l = 0; l < 1 + rnd.nextInt(3); l++) {
                    if (!chunk.getBlockState(pos.set(rx, ry - l, rz)).isAir()) break;
                    chunk.setBlockState(pos, s(ModDorf.LICHTRANKE.get()), false);
                }
            }
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (rnd.nextInt(3) != 0) continue;
            pos.set(x + dir.getStepX(), y, z + dir.getStepZ());
            if (chunk.getBlockState(pos).canBeReplaced() && chunk.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
                chunk.setBlockState(pos, rnd.nextBoolean() ? s(ModDorf.ZAUBERPILZ_VIOLETT.get()) : s(ModDorf.ZAUBERPILZ_GRUEN.get()), false);
            }
        }
    }

    // ------------------------------------------------------------------ Schritt 2: Leben

    public static void beleben(WorldGenRegion region, UniverseProfile p) {
        DorfPlan plan = p.dorfPlan();
        ChunkPos cp = region.getCenter();
        int bx = cp.getMinBlockX(), bz = cp.getMinBlockZ();
        RandomSource rnd = RandomSource.create(UniverseId.mix(p.seed ^ cp.toLong() ^ 0x1EBE1L));
        for (DorfPlan.Dorf d : plan.doerferIn(bx, bz, bx + 15, bz + 15)) {
            for (DorfPlan.Haus h : d.haeuser()) {
                if (h.x() >= bx && h.x() <= bx + 15 && h.z() >= bz && h.z() <= bz + 15) bewohner(region, d, h, rnd);
            }
            // Besenflieger über dem Platz
            DorfPlan.Platz pl = d.plaetze().get(0);
            if (pl.x() >= bx && pl.x() <= bx + 15 && pl.z() >= bz && pl.z() <= bz + 15) {
                for (int i = 0; i < (d.haupt() ? 2 : 1); i++) {
                    gleeb(region, Beruf.BUERGER, new BlockPos(pl.x(), d.hoehe() + 16 + i * 5, pl.z()), d, rnd, true);
                }
            }
        }
        // Tiere der Wiesen (wie in normalen Welten beim Erzeugen)
        Holder<Biome> biome = region.getBiome(cp.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
        WorldgenRandom wr = new WorldgenRandom(new LegacyRandomSource(UniverseId.mix(p.seed ^ cp.toLong())));
        wr.setDecorationSeed(region.getSeed(), bx, bz);
        NaturalSpawner.spawnMobsForChunkGeneration(region, biome, cp, wr);
    }

    private static void bewohner(WorldGenRegion region, DorfPlan.Dorf d, DorfPlan.Haus h, RandomSource rnd) {
        BlockPos innen = new BlockPos(h.x(), h.y() + 1, h.z());
        if (h.typ().beruf != null) gleeb(region, h.typ().beruf, innen, d, rnd, false);
        for (int i = 0; i < h.typ().buerger; i++) gleeb(region, Beruf.BUERGER, innen, d, rnd, false);
    }

    /** Erst ganz einrichten, dann hinzufügen: der Proto-Chunk speichert das Wesen sofort als NBT. */
    private static Gleeb gleeb(WorldGenRegion region, Beruf beruf, BlockPos pos, DorfPlan.Dorf d, RandomSource rnd, boolean besen) {
        Gleeb g = ModEntities.GLEEB.get().create(region.getLevel());
        if (g == null) return null;
        g.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rnd.nextFloat() * 360, 0);
        DorfPlan.Platz pl = d.plaetze().get(0);
        g.einrichten(beruf, pos, new BlockPos(pl.x(), d.hoehe() + 1, pl.z()), d.radius(), rnd);
        g.setPersistenceRequired();
        if (besen) g.besenSetzen(true);
        region.addFreshEntity(g);
        return g;
    }
}
