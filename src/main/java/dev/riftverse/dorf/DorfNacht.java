package dev.riftverse.dorf;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModDorf;
import dev.riftverse.registry.ModEntities;
import dev.riftverse.universe.UniverseType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Nächte in Bridge's Inn: Sternschnuppen (M10-16) lassen glitzernden Sternenstaub in der Nähe landen, Irrlichter
 * (M10-15) führen zu kleinen Verstecken.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class DorfNacht {
    /** Chance pro Sekunde und Spieler. */
    public static final int STERNSCHNUPPE = 80, IRRLICHT = 200;

    private DorfNacht() {
    }

    public static boolean imDorf(ServerLevel level) {
        return level.dimension().equals(UniverseType.MAGISCHES_DORF.dimension());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7 || p.isSpectator()) return;
        ServerLevel level = p.serverLevel();
        if (!imDorf(level) || !level.isNight()) return;
        RandomSource r = p.getRandom();
        if (r.nextInt(STERNSCHNUPPE) == 0) sternschnuppe(level, p, r);
        if (r.nextInt(IRRLICHT) == 0 && level.getEntitiesOfClass(Irrlicht.class, p.getBoundingBox().inflate(64)).isEmpty()) {
            irrlicht(level, p, r);
        }
    }

    private static BlockPos boden(ServerLevel level, double x, double z) {
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
    }

    public static void sternschnuppe(ServerLevel level, ServerPlayer p, RandomSource r) {
        double a = r.nextDouble() * Math.PI * 2, d = 15 + r.nextDouble() * 25;
        double x = p.getX() + Math.cos(a) * d, z = p.getZ() + Math.sin(a) * d;
        BlockPos ziel = boden(level, x, z);
        // Leuchtspur vom Himmel
        double sx = x - Math.cos(a + 0.6) * 40, sz = z - Math.sin(a + 0.6) * 40, sy = ziel.getY() + 60;
        for (int i = 0; i <= 30; i++) {
            double t = i / 30.0;
            level.sendParticles(ParticleTypes.FIREWORK, sx + (x - sx) * t, sy + (ziel.getY() + 1 - sy) * t, sz + (z - sz) * t,
                    1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.END_ROD, x, ziel.getY() + 0.5, z, 20, 0.3, 0.3, 0.3, 0.08);
        level.playSound(null, x, ziel.getY(), z, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, SoundSource.AMBIENT, 3.0f, 1.2f);
        ItemEntity staub = new ItemEntity(level, x, ziel.getY() + 0.5, z, new ItemStack(ModDorf.STERNENSTAUB.get(), 1 + r.nextInt(2)));
        staub.setGlowingTag(true);
        staub.setExtendedLifetime();
        level.addFreshEntity(staub);
    }

    public static void irrlicht(ServerLevel level, ServerPlayer p, RandomSource r) {
        double a = r.nextDouble() * Math.PI * 2;
        double d = 40 + r.nextDouble() * 30;
        BlockPos ziel = boden(level, p.getX() + Math.cos(a) * d, p.getZ() + Math.sin(a) * d);
        BlockPos start = boden(level, p.getX() + Math.cos(a) * 9, p.getZ() + Math.sin(a) * 9);
        Irrlicht i = ModEntities.IRRLICHT.get().create(level);
        if (i == null) return;
        i.setPos(start.getX() + 0.5, start.getY() + 1.4, start.getZ() + 0.5);
        i.zielSetzen(ziel);
        level.addFreshEntity(i);
        level.playSound(null, start, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 1.0f, 1.6f);
    }
}
