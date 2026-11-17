package dev.riftverse.dorf;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Irrlicht (M10-15): erscheint nachts in der Nähe eines Spielers, wartet, bis er näher kommt, und schwebt dann
 * vor ihm her zu einem Versteck. Dort taucht ein Fass mit kleinen Schätzen auf, und das Irrlicht verlischt.
 */
public class Irrlicht extends Entity {
    @Nullable
    private BlockPos ziel;
    private int leben, allein;

    public Irrlicht(EntityType<? extends Irrlicht> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void zielSetzen(BlockPos ziel) {
        this.ziel = ziel.immutable();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        double schweben = Math.sin(tickCount * 0.12) * 0.25;
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 0.2,
                        getZ() + (random.nextDouble() - 0.5) * 0.3, 0, -0.01, 0);
            }
            if (random.nextInt(5) == 0) level().addParticle(ParticleTypes.GLOW, getX(), getY() + 0.2, getZ(), 0, 0, 0);
            return;
        }
        leben++;
        if (ziel == null || leben > 6000 || !level().isNight()) {
            verloeschen();
            return;
        }
        Player p = level().getNearestPlayer(this, 40);
        allein = p == null ? allein + 1 : 0;
        if (allein > 600) {
            verloeschen();
            return;
        }
        Vec3 zu = Vec3.atBottomCenterOf(ziel).subtract(position());
        double weg = zu.horizontalDistance();
        if (weg < 1.5) {
            if (p != null && p.distanceToSqr(this) < 144) versteckOeffnen();
            return;
        }
        double nx = getX(), nz = getZ();
        if (p != null && p.distanceToSqr(this) < 49) {
            nx += zu.x / weg * 0.2;
            nz += zu.z / weg * 0.2;
        }
        int boden = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(nx), Mth.floor(nz));
        double ny = Mth.lerp(0.15, getY(), boden + 1.4 + schweben);
        setPos(nx, ny, nz);
    }

    private void verloeschen() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.2, getZ(), 15, 0.2, 0.2, 0.2, 0.05);
        }
        discard();
    }

    private void versteckOeffnen() {
        if (!(level() instanceof ServerLevel sl) || ziel == null) return;
        BlockPos pos = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ziel);
        if (sl.getBlockState(pos).canBeReplaced() && sl.getBlockState(pos.below()).isFaceSturdy(sl, pos.below(), Direction.UP)) {
            sl.setBlock(pos, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), Block.UPDATE_ALL);
            RandomizableContainer.setBlockEntityLootTable(sl, random, pos, DorfBau.KISTE_IRRLICHT);
            sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 30, 0.4, 0.6, 0.4, 0.05);
            sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.5f, 0.8f);
            sl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.5f, 1.6f);
        }
        verloeschen();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
