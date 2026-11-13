package dev.riftverse.universe;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Die Physik der Zeitdilatationswelt.
 * <ul>
 *   <li>Pflanzen und alle Zufallsticks laufen 4× so schnell (drei zusätzliche Runden rund um jeden Spieler).</li>
 *   <li>Zeitverzerrung: Ab und zu springen Spieler und Kreaturen in der Nähe auf ihre Position von vor
 *       fünf Sekunden zurück.</li>
 *   <li>Chronoglas-Sand fällt überall in Zeitlupe.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class Zeitverzerrung {
    /** Zusätzliche Runden Zufallsticks: 3 → insgesamt 4× so schnell. */
    public static final int EXTRA_RANDOM_TICKS = 3;
    public static final int CHUNK_RADIUS = 3;
    /** Alle 5 Sekunden wird die Position gemerkt, zu der eine Verzerrung zurückführt. */
    public static final int SNAPSHOT_TICKS = 100;
    /** Chance auf eine Verzerrung pro Spieler und Sekunde (1 zu n). */
    public static final int DISTORTION_CHANCE = 90;

    private record Snapshot(Vec3 pos, long time) {}

    private static final Map<UUID, Snapshot> SNAPSHOTS = new HashMap<>();

    private static boolean dilated(ServerLevel level) {
        return UniverseType.of(level.dimension()) == UniverseType.ZEITDILATATION;
    }

    // ------------------------------------------------------------------ 4× Zufallsticks

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !dilated(level)) return;
        int speed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        if (speed <= 0 || level.players().isEmpty()) return;
        Set<Long> done = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    int cx = center.x + dx, cz = center.z + dz;
                    if (!done.add(ChunkPos.asLong(cx, cz))) continue;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk != null) randomTicks(level, chunk, speed * EXTRA_RANDOM_TICKS);
                }
            }
        }
    }

    private static void randomTicks(ServerLevel level, LevelChunk chunk, int perSection) {
        LevelChunkSection[] sections = chunk.getSections();
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (!section.isRandomlyTicking()) continue;
            int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            for (int k = 0; k < perSection; k++) {
                BlockPos pos = level.getBlockRandomPos(minX, minY, minZ, 15);
                BlockState state = section.getBlockState(pos.getX() - minX, pos.getY() - minY, pos.getZ() - minZ);
                if (state.isRandomlyTicking()) state.randomTick(level, pos, level.random);
            }
        }
    }

    // ------------------------------------------------------------------ Zeitverzerrung

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        if (!dilated(level) || player.isSpectator()) return;
        long now = level.getGameTime();
        if (player.tickCount % SNAPSHOT_TICKS == 0) {
            remember(player, now);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24), e -> e != player)) {
                remember(e, now);
            }
        }
        if (player.tickCount % 20 == 0 && !player.isCreative() && level.random.nextInt(DISTORTION_CHANCE) == 0) {
            distort(level, player, now);
        }
    }

    private static void remember(Entity e, long now) {
        if (SNAPSHOTS.size() > 4096) SNAPSHOTS.clear();
        SNAPSHOTS.put(e.getUUID(), new Snapshot(e.position(), now));
    }

    /** Spieler und Kreaturen im Umkreis springen auf ihren letzten Schnappschuss zurück. */
    private static void distort(ServerLevel level, ServerPlayer player, long now) {
        int moved = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24))) {
            Snapshot s = SNAPSHOTS.get(e.getUUID());
            if (s == null || now - s.time() > SNAPSHOT_TICKS * 2 || s.pos().distanceToSqr(e.position()) < 1.0) continue;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            e.teleportTo(s.pos().x, s.pos().y, s.pos().z);
            e.resetFallDistance();
            moved++;
        }
        if (moved > 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.AMBIENT, 1.0f, 0.6f);
            player.displayClientMessage(Component.translatable("message.riftverse.zeitverzerrung").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    // ------------------------------------------------------------------ Chronoglas-Sand

    /** Chronoglas-Sand fällt in Zeitlupe (auf Server und Client, damit es flüssig aussieht). */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof FallingBlockEntity falling && falling.getBlockState().is(ModBlocks.CHRONOGLAS_SAND.get())) {
            Vec3 v = falling.getDeltaMovement();
            if (v.y < -0.08) falling.setDeltaMovement(v.x, -0.08, v.z);
        }
    }

    private Zeitverzerrung() {}
}
