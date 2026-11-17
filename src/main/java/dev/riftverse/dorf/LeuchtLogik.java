package dev.riftverse.dorf;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * Gemeinsames Verhalten aller Leuchtpflanzen: nachts leuchten sie ({@link #LEUCHTET}), und wer hindurchläuft,
 * regt sie an ({@link #ANGEREGT}): dann strahlen sie mit voller Kraft und sprühen Funken, bis niemand mehr darin steht.
 * Lumora nutzt das später für seine Pflanzen weiter.
 */
public final class LeuchtLogik {
    public static final BooleanProperty LEUCHTET = BooleanProperty.create("leuchtet");
    public static final BooleanProperty ANGEREGT = BooleanProperty.create("angeregt");
    /** So lange (Ticks) bleibt eine Pflanze nach dem Durchlaufen angeregt. */
    public static final int ANGEREGT_TICKS = 40;

    private LeuchtLogik() {
    }

    /** Lichtstufe: angeregt 15, nachts {@code nacht}, sonst {@code grund}. */
    public static int licht(BlockState s, int nacht, int grund) {
        if (s.getValue(ANGEREGT)) return 15;
        return s.getValue(LEUCHTET) ? nacht : grund;
    }

    /** Zufallstick: an die Tageszeit anpassen. */
    public static void tageszeit(BlockState state, ServerLevel level, BlockPos pos) {
        boolean nacht = level.isNight();
        if (state.getValue(LEUCHTET) != nacht) level.setBlock(pos, state.setValue(LEUCHTET, nacht), Block.UPDATE_ALL);
    }

    /** Jemand läuft hindurch: anregen und das Abklingen planen. */
    public static void durchlaufen(BlockState state, Level level, BlockPos pos, Entity entity, Block block) {
        if (level.isClientSide || state.getValue(ANGEREGT) || !(entity instanceof LivingEntity)) return;
        level.setBlock(pos, state.setValue(ANGEREGT, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, block, ANGEREGT_TICKS);
    }

    /** Geplanter Tick: abklingen, außer es steht noch jemand darin. */
    public static void abklingen(BlockState state, ServerLevel level, BlockPos pos, Block block) {
        if (!state.getValue(ANGEREGT)) return;
        if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) {
            level.scheduleTick(pos, block, ANGEREGT_TICKS);
            return;
        }
        level.setBlock(pos, state.setValue(ANGEREGT, false), Block.UPDATE_ALL);
    }

    /** Zauberei der Gleebs: alle Leuchtpflanzen im Umkreis aufleuchten lassen. */
    public static int anregen(ServerLevel level, BlockPos mitte, int radius) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(mitte.offset(-radius, -2, -radius), mitte.offset(radius, 2, radius))) {
            BlockState s = level.getBlockState(p);
            if (s.hasProperty(ANGEREGT) && !s.getValue(ANGEREGT)) {
                level.setBlock(p, s.setValue(ANGEREGT, true), Block.UPDATE_ALL);
                level.scheduleTick(p.immutable(), s.getBlock(), ANGEREGT_TICKS + level.random.nextInt(40));
                n++;
            }
        }
        return n;
    }

    /** Leuchtpflanzen-Partikel in der Farbe der Pflanze; angeregt deutlich mehr. */
    public static void partikel(BlockState state, Level level, BlockPos pos, RandomSource random, int farbe, double hoehe) {
        boolean an = state.getValue(ANGEREGT);
        if (!an && !state.getValue(LEUCHTET)) return;
        int n = an ? 3 : (random.nextInt(4) == 0 ? 1 : 0);
        Vector3f c = new Vector3f(((farbe >> 16) & 0xFF) / 255f, ((farbe >> 8) & 0xFF) / 255f, (farbe & 0xFF) / 255f);
        for (int i = 0; i < n; i++) {
            level.addParticle(new DustParticleOptions(c, an ? 1.0f : 0.6f), pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + hoehe * random.nextDouble() + 0.1, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
        }
        if (an && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + hoehe, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.04, 0.05, (random.nextDouble() - 0.5) * 0.04);
        }
    }
}
