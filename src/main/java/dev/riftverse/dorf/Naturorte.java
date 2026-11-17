package dev.riftverse.dorf;

import dev.riftverse.registry.ModDorf;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Orte in der Natur von Bridge's Inn (M10-17): der Steinkreis auf der Seeinsel, Feenringe aus Zauberpilzen, der
 * Großvater-Funkelbaum und die alte Brücke über den Fluss. Spätere Geschichten können hier spielen.
 */
public final class Naturorte {
    private Naturorte() {
    }

    private static BlockState s(Block b) {
        return b.defaultBlockState();
    }

    public static void bauen(DorfPlan.Naturort o, DorfPlan plan, HausBauer.Ziel z) {
        RandomSource r = RandomSource.create(o.seed());
        switch (o.art()) {
            case STEINKREIS -> steinkreis(o, plan, z, r);
            case FEENRING -> feenring(o, plan, z, r);
            case GROSSVATERBAUM -> grossvaterbaum(o, plan, z, r);
            case ALTE_BRUECKE -> alteBruecke(o, plan, z, r);
        }
    }

    private static void steinkreis(DorfPlan.Naturort o, DorfPlan plan, HausBauer.Ziel z, RandomSource r) {
        int y = plan.hoehe(o.x(), o.z());
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int px = o.x() + (int) Math.round(Math.cos(a) * 7), pz = o.z() + (int) Math.round(Math.sin(a) * 7);
            int py = plan.hoehe(px, pz);
            int h = 3 + r.nextInt(3);
            for (int k = 1; k <= h; k++) {
                z.set(px, py + k, pz, k == h ? s(Blocks.CHISELED_STONE_BRICKS)
                        : r.nextInt(3) == 0 ? s(Blocks.CRACKED_STONE_BRICKS) : s(Blocks.MOSSY_STONE_BRICKS));
            }
            if (i % 2 == 0) z.set(px, py + h + 1, pz, s(ModDorf.FEENLICHT.get()));
            else z.set(px, py + h + 1, pz, s(Blocks.MOSS_CARPET));
        }
        // Altar mit Kiste darunter
        z.set(o.x(), y, o.z(), s(Blocks.CHISELED_STONE_BRICKS));
        z.set(o.x(), y + 1, o.z(), s(ModDorf.STERNGLAS.get()));
        z.set(o.x(), y + 4, o.z(), s(ModDorf.SCHWEBELATERNE.get()));
        z.set(o.x(), y - 1, o.z(), s(Blocks.CHEST));
        for (Direction d : Direction.Plane.HORIZONTAL) {
            z.set(o.x() + d.getStepX(), y + 1, o.z() + d.getStepZ(), s(Blocks.STONE_BRICK_SLAB));
            z.set(o.x() + d.getStepX() * 3, y + 1, o.z() + d.getStepZ() * 3, s(ModDorf.STERNENBLUME_WEISS.get()));
        }
    }

    private static void feenring(DorfPlan.Naturort o, DorfPlan plan, HausBauer.Ziel z, RandomSource r) {
        for (int i = 0; i < 14; i++) {
            double a = i * Math.PI * 2 / 14;
            int px = o.x() + (int) Math.round(Math.cos(a) * 4.5), pz = o.z() + (int) Math.round(Math.sin(a) * 4.5);
            int py = plan.hoehe(px, pz);
            z.set(px, py + 1, pz, i % 2 == 0 ? s(ModDorf.ZAUBERPILZ_VIOLETT.get()) : s(ModDorf.ZAUBERPILZ_GRUEN.get()));
        }
        int y = plan.hoehe(o.x(), o.z());
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz <= 9) z.set(o.x() + dx, y + 1, o.z() + dz, s(Blocks.AIR));
            }
        }
        z.set(o.x(), y + 2, o.z(), s(ModDorf.FEENLICHT.get()));
        if (r.nextBoolean()) z.set(o.x(), y + 1, o.z(), s(ModDorf.MONDKRAUT.get()));
    }

    private static BlockState laub() {
        return ModDorf.FUNKELLAUB.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static void grossvaterbaum(DorfPlan.Naturort o, DorfPlan plan, HausBauer.Ziel z, RandomSource r) {
        int y = plan.hoehe(o.x(), o.z());
        BlockState stamm = ModDorf.FUNKELHOLZ_STAMM.get().defaultBlockState();
        int h = 18;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int k = -3; k <= h; k++) z.set(o.x() + dx, y + k, o.z() + dz, stamm);
            }
        }
        // Wurzeln
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int k = 2; k <= 4; k++) {
                int px = o.x() + d.getStepX() * k, pz = o.z() + d.getStepZ() * k;
                z.set(px, plan.hoehe(px, pz) + (k == 2 ? 1 : 0), pz,
                        stamm.setValue(RotatedPillarBlock.AXIS, d.getAxis()));
            }
        }
        // Krone: flaches Ellipsoid mit Lichtranken und Feenlichtern
        int ky = y + h - 2;
        for (int dx = -9; dx <= 9; dx++) {
            for (int dz = -9; dz <= 9; dz++) {
                for (int dy = -3; dy <= 5; dy++) {
                    double q = (dx * dx + dz * dz) / 81.0 + (dy - 1) * (dy - 1) / 20.0;
                    if (q > 1) continue;
                    if (q > 0.8 && r.nextInt(3) == 0) continue;
                    boolean imStamm = Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && dy <= 2;
                    if (imStamm) continue;
                    z.set(o.x() + dx, ky + dy, o.z() + dz, r.nextInt(40) == 0 ? s(ModDorf.FEENLICHT.get()) : laub());
                }
            }
        }
        for (int i = 0; i < 30; i++) {
            int px = o.x() + r.nextInt(17) - 8, pz = o.z() + r.nextInt(17) - 8;
            int dx = px - o.x(), dz = pz - o.z();
            double q = (dx * dx + dz * dz) / 81.0;
            if (q > 0.85 || (Math.abs(dx) <= 1 && Math.abs(dz) <= 1)) continue;
            int unten = ky + 1 - (int) Math.floor(Math.sqrt(Math.max(0, 1 - q) * 20)) + 1;
            for (int l = 1; l <= 1 + r.nextInt(4); l++) z.set(px, unten - l, pz, s(ModDorf.LICHTRANKE.get()));
        }
        // Ein Bänkchen und Pilze am Fuß
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int px = o.x() + d.getStepX() * 3 + d.getClockWise().getStepX() * 2;
            int pz = o.z() + d.getStepZ() * 3 + d.getClockWise().getStepZ() * 2;
            z.set(px, plan.hoehe(px, pz) + 1, pz, r.nextBoolean() ? s(ModDorf.ZAUBERPILZ_VIOLETT.get()) : s(ModDorf.MONDKRAUT.get()));
        }
    }

    private static void alteBruecke(DorfPlan.Naturort o, DorfPlan plan, HausBauer.Ziel z, RandomSource r) {
        int deck = 66;
        for (int dz = -22; dz <= 22; dz++) {
            boolean loch = Math.abs(dz) < 6 && r.nextInt(3) == 0;
            for (int dx = -2; dx <= 2; dx++) {
                int px = o.x() + dx, pz = o.z() + dz;
                int boden = plan.hoehe(px, pz);
                if (boden >= deck) continue;
                if (!loch || Math.abs(dx) == 2) {
                    z.set(px, deck, pz, r.nextInt(4) == 0 ? s(Blocks.CRACKED_STONE_BRICKS)
                            : r.nextInt(3) == 0 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.STONE_BRICKS));
                }
                if (Math.abs(dx) == 2 && Math.floorMod(dz, 3) == 0 && r.nextInt(3) > 0) {
                    z.set(px, deck + 1, pz, s(Blocks.MOSSY_STONE_BRICK_WALL));
                }
                if (Math.floorMod(dz, 11) == 0) {
                    for (int y = deck - 1; y > boden; y--) z.set(px, y, pz, s(Blocks.MOSSY_STONE_BRICKS));
                }
                if (Math.abs(dx) == 2 && r.nextInt(6) == 0) z.set(px, deck - 1, pz, s(ModDorf.LICHTRANKE.get()));
            }
        }
        z.set(o.x(), deck + 1, o.z() - 18, s(Blocks.MOSSY_STONE_BRICK_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        z.set(o.x() + 1, deck + 3, o.z(), s(ModDorf.SCHWEBELATERNE.get()));
    }
}
