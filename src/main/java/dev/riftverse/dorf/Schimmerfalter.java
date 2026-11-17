package dev.riftverse.dorf;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Schimmerfalter: großer Nachtfalter mit leuchtenden Flügeln. Flattert ziellos über die Wiesen, nachts gern zu den
 * Leuchtpflanzen. Lässt Falterstaub fallen.
 */
public class Schimmerfalter extends PathfinderMob {
    @Nullable
    private BlockPos ziel;

    public Schimmerfalter(EntityType<? extends Schimmerfalter> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 3).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (ziel == null || random.nextInt(40) == 0 || ziel.closerToCenterThan(position(), 1.5)) {
            int x = getBlockX() + random.nextInt(15) - 7, z = getBlockZ() + random.nextInt(15) - 7;
            int boden = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            ziel = new BlockPos(x, boden + 1 + random.nextInt(5), z);
        }
        Vec3 d = Vec3.atCenterOf(ziel).subtract(position());
        Vec3 v = getDeltaMovement();
        Vec3 neu = v.add((Math.signum(d.x) * 0.4 - v.x) * 0.08, (Math.signum(d.y) * 0.5 - v.y) * 0.08,
                (Math.signum(d.z) * 0.4 - v.z) * 0.08);
        setDeltaMovement(neu);
        float yaw = (float) (Mth.atan2(neu.z, neu.x) * (180F / Math.PI)) - 90F;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.3f);
        yBodyRot = getYRot();
        zza = 0.4f;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isFlapping() {
        return true;
    }
}
