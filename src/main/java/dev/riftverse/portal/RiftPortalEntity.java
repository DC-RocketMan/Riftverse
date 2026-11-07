package dev.riftverse.portal;

import dev.riftverse.boss.RiftBossEntity;
import dev.riftverse.registry.ModEntities;
import dev.riftverse.universe.UniverseId;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Ein offenes Rift. Alles, was hineinläuft (Spieler, Mobs, Items), wird zum Ziel
 * transportiert. Schließt sich nach Ablauf der Lebensdauer von selbst.
 */
public class RiftPortalEntity extends Entity {
    public static final int STYLE_RANDOM = 0, STYLE_HOME = 1, STYLE_RETURN = 2, STYLE_ESCAPE = 3;

    private static final EntityDataAccessor<Integer> DATA_STYLE =
            SynchedEntityData.defineId(RiftPortalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CLOSING =
            SynchedEntityData.defineId(RiftPortalEntity.class, EntityDataSerializers.BOOLEAN);

    private int life = 600;
    @Nullable private ResourceKey<Level> targetDim;
    @Nullable private Vec3 targetPos;
    @Nullable private UniverseId targetUniverse;
    private boolean spawnReturn = true;
    private boolean travelEnabled = true;

    /** nur clientseitig: Animation beim Schließen */
    private int closingTicks;

    public RiftPortalEntity(EntityType<? extends RiftPortalEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static RiftPortalEntity create(Level level, Vec3 pos, float yaw, int style, int life) {
        RiftPortalEntity portal = new RiftPortalEntity(ModEntities.RIFT_PORTAL.get(), level);
        portal.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        portal.setYRot(yaw);
        portal.entityData.set(DATA_STYLE, style);
        portal.life = life;
        return portal;
    }

    // ------------------------------------------------------------------ Ziel

    public void setUniverseTarget(ResourceKey<Level> dim, UniverseId universe) {
        this.targetDim = dim;
        this.targetUniverse = universe;
        this.targetPos = null;
    }

    public void setExactTarget(ResourceKey<Level> dim, Vec3 pos) {
        this.targetDim = dim;
        this.targetPos = pos;
        this.targetUniverse = null;
    }

    public void setSpawnReturn(boolean spawnReturn) {
        this.spawnReturn = spawnReturn;
    }

    public void setTravelEnabled(boolean enabled) {
        this.travelEnabled = enabled;
    }

    @Nullable public ResourceKey<Level> targetDim() { return targetDim; }
    @Nullable public Vec3 targetPos() { return targetPos; }
    @Nullable public UniverseId targetUniverse() { return targetUniverse; }
    public boolean spawnReturn() { return spawnReturn; }

    public int style() {
        return entityData.get(DATA_STYLE);
    }

    /** Position vor dem Portal (Seite, von der aus es geöffnet wurde). */
    public Vec3 frontPosition(double distance) {
        return position().add(Vec3.directionFromRotation(0f, getYRot()).scale(distance));
    }

    // ------------------------------------------------------------------ Tick

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        life--;
        if (life == 15) entityData.set(DATA_CLOSING, true);
        if (life <= 0) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(dust(), getX(), getY() + 1.2, getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            }
            playSound(SoundEvents.BEACON_DEACTIVATE, 0.6f, 1.8f);
            discard();
            return;
        }
        if (travelEnabled && targetDim != null && tickCount > 8 && life > 10) {
            for (Entity e : level().getEntities(this, getBoundingBox(), this::canTravel)) {
                RiftTravel.queue(e, this);
            }
        }
        if (tickCount % 40 == 0) playSound(SoundEvents.PORTAL_AMBIENT, 0.25f, 1.7f);
    }

    private boolean canTravel(Entity e) {
        if (!e.isAlive() || e instanceof RiftPortalEntity || e instanceof RiftBossEntity) return false;
        if (e.isPassenger() || e.isVehicle()) return false;
        if (e instanceof Player p && p.isSpectator()) return false;
        return e.getPersistentData().getLong(RiftTravel.COOLDOWN_KEY) <= level().getGameTime();
    }

    private void clientTick() {
        if (entityData.get(DATA_CLOSING)) closingTicks++;
        for (int i = 0; i < 2; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double rx = Math.cos(a) * 0.9, ry = Math.sin(a) * 1.25;
            Vec3 side = Vec3.directionFromRotation(0f, getYRot() + 90f);
            level().addParticle(dust(), getX() + side.x * rx, getY() + 1.2 + ry, getZ() + side.z * rx, 0, 0, 0);
        }
    }

    public float[] color() {
        return switch (style()) {
            case STYLE_HOME -> new float[]{0.35f, 0.65f, 1.0f};
            case STYLE_RETURN -> new float[]{1.0f, 0.62f, 0.15f};
            case STYLE_ESCAPE -> new float[]{0.72f, 0.3f, 1.0f};
            default -> new float[]{0.32f, 1.0f, 0.25f};
        };
    }

    private DustParticleOptions dust() {
        float[] c = color();
        return new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 1.2f);
    }

    /** 0..1 – wie weit das Portal geöffnet ist (für den Renderer). */
    public float openFactor(float partialTick) {
        float open = Math.min(1f, (tickCount + partialTick) / 10f);
        if (entityData.get(DATA_CLOSING)) open *= Math.max(0f, 1f - (closingTicks + partialTick) / 15f);
        return open;
    }

    // ------------------------------------------------------------------ Daten

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STYLE, STYLE_RANDOM);
        builder.define(DATA_CLOSING, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        life = tag.getInt("Life");
        entityData.set(DATA_STYLE, tag.getInt("Style"));
        spawnReturn = tag.getBoolean("SpawnReturn");
        travelEnabled = !tag.contains("Travel") || tag.getBoolean("Travel");
        if (tag.contains("TargetDim")) {
            targetDim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("TargetDim")));
        }
        if (tag.contains("TX")) targetPos = new Vec3(tag.getDouble("TX"), tag.getDouble("TY"), tag.getDouble("TZ"));
        targetUniverse = UniverseId.load(tag.getCompound("Universe"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", life);
        tag.putInt("Style", style());
        tag.putBoolean("SpawnReturn", spawnReturn);
        tag.putBoolean("Travel", travelEnabled);
        if (targetDim != null) tag.putString("TargetDim", targetDim.location().toString());
        if (targetPos != null) {
            tag.putDouble("TX", targetPos.x);
            tag.putDouble("TY", targetPos.y);
            tag.putDouble("TZ", targetPos.z);
        }
        if (targetUniverse != null) tag.put("Universe", targetUniverse.save());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
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
}
