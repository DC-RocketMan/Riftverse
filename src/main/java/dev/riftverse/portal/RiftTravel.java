package dev.riftverse.portal;

import dev.riftverse.registry.ModBlocks;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/** Die eigentliche Reise durch ein Rift: sicheren Landeplatz finden, teleportieren, Rückportal öffnen. */
public final class RiftTravel {
    public static final String COOLDOWN_KEY = "riftverse:rift_cooldown";

    private record Destination(ResourceKey<Level> dim, @Nullable Vec3 exactPos, @Nullable UniverseId universe,
                               boolean spawnReturn, Vec3 returnPos) {}

    /** Reise für den Ende des Server-Ticks einplanen (nicht mitten im Entity-Tick teleportieren). */
    public static void queue(Entity entity, RiftPortalEntity portal) {
        MinecraftServer server = entity.getServer();
        if (server == null || portal.targetDim() == null) return;
        entity.getPersistentData().putLong(COOLDOWN_KEY, entity.level().getGameTime() + 60);
        Destination dest = new Destination(portal.targetDim(), portal.targetPos(), portal.targetUniverse(),
                portal.spawnReturn(), portal.frontPosition(2.0));
        server.tell(new TickTask(server.getTickCount(), () -> travel(entity, dest)));
    }

    private static void travel(Entity entity, Destination dest) {
        if (entity.isRemoved()) return;
        MinecraftServer server = entity.getServer();
        if (server == null) return;
        ServerLevel target = server.getLevel(dest.dim());
        if (target == null) return;

        BlockPos landing;
        if (dest.exactPos() != null) {
            landing = findSafe(target, BlockPos.containing(dest.exactPos()), 8, true);
        } else if (dest.universe() != null) {
            RandomSource r = target.getRandom();
            int x = Multiverse.cellCenter(dest.universe().cellX()) + r.nextInt(301) - 150;
            int z = Multiverse.cellCenter(dest.universe().cellZ()) + r.nextInt(301) - 150;
            landing = findSafe(target, new BlockPos(x, 100, z), 32, false);
        } else {
            return;
        }

        ServerLevel origin = (ServerLevel) entity.level();
        Vec3 arrive = Vec3.atBottomCenterOf(landing);
        float yaw = entity.getYRot();
        ResourceKey<Level> originDim = origin.dimension();

        effects(origin, entity.position());
        Entity moved = teleport(entity, target, arrive, yaw);
        if (moved == null) return;
        moved.getPersistentData().putLong(COOLDOWN_KEY, target.getGameTime() + 60);
        effects(target, arrive);

        if (dest.spawnReturn() && moved instanceof ServerPlayer) {
            Vec3 behind = arrive.add(Vec3.directionFromRotation(0f, yaw).scale(-2.5));
            RiftPortalEntity back = RiftPortalEntity.create(target, behind, yaw, RiftPortalEntity.STYLE_RETURN, 400);
            back.setExactTarget(originDim, dest.returnPos());
            back.setSpawnReturn(false);
            target.addFreshEntity(back);
        }
    }

    @Nullable
    public static Entity teleport(Entity entity, ServerLevel target, Vec3 pos, float yaw) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(target, pos.x, pos.y, pos.z, yaw, player.getXRot());
            player.fallDistance = 0;
            return player;
        }
        // Bei Dimensionswechsel erzeugt Minecraft eine Kopie des Mobs; der Cooldown
        // steckt in den persistenten Daten und wird automatisch mitkopiert.
        boolean ok = entity.teleportTo(target, pos.x, pos.y, pos.z, Set.of(), yaw, entity.getXRot());
        return ok ? entity : null;
    }

    private static void effects(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.7f);
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1, pos.z, 40, 0.5, 1.0, 0.5, 0.3);
    }

    // ------------------------------------------------------------------ Landeplatz

    /** Sucht spiralförmig einen sicheren Platz; baut notfalls eine Plattform aus Rift-Stein. */
    public static BlockPos findSafe(ServerLevel level, BlockPos around, int radius, boolean tryExactFirst) {
        if (tryExactFirst) {
            level.getChunk(around.getX() >> 4, around.getZ() >> 4);
            if (isSafe(level, around)) return around;
        }
        int step = 4;
        for (int r = 0; r <= radius; r += step) {
            for (int dx = -r; dx <= r; dx += step) {
                for (int dz = -r; dz <= r; dz += step) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = around.getX() + dx, z = around.getZ() + dz;
                    level.getChunk(x >> 4, z >> 4);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    if (y <= level.getMinBuildHeight() + 1) continue;
                    BlockPos p = new BlockPos(x, y, z);
                    if (isSafe(level, p)) return p;
                }
            }
        }
        // Kein sicherer Boden (Ozean, Lava, Leere …): Notfall-Plattform
        int x = around.getX(), z = around.getZ();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() + 1) y = Math.max(around.getY(), 100);
        BlockPos center = new BlockPos(x, y + 1, z);
        buildPlatform(level, center);
        return center;
    }

    public static boolean isSafe(ServerLevel level, BlockPos p) {
        BlockPos below = p.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP)) return false;
        if (ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.CACTUS) || ground.is(Blocks.CAMPFIRE)) return false;
        if (!level.getFluidState(p).isEmpty() || !level.getFluidState(p.above()).isEmpty()) return false;
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty();
    }

    public static void buildPlatform(ServerLevel level, BlockPos center) {
        BlockState stone = ModBlocks.RIFT_STONE.get().defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(center.offset(dx, -1, dz), stone, 3);
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private RiftTravel() {}
}
