package dev.riftverse.gadget;

import dev.riftverse.registry.ModEntities;
import dev.riftverse.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Geworfene Anti-Grav-Granate: Alle Mobs im Radius 6 schweben 8 Sekunden lang hilflos. */
public class AntiGravGranateEntity extends ThrowableItemProjectile {
    public static final double RADIUS = 6.0;
    public static final int DAUER = 160;

    public AntiGravGranateEntity(EntityType<? extends AntiGravGranateEntity> type, Level level) {
        super(type, level);
    }

    public AntiGravGranateEntity(Level level, LivingEntity thrower) {
        super(ModEntities.ANTI_GRAV_GRANATE.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ANTI_GRAV_GRANATE.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel sl) {
            zuenden(sl, this);
            discard();
        }
    }

    /** Lässt alle Mobs um {@code center} schweben. @return Anzahl der getroffenen Mobs */
    public static int zuenden(ServerLevel level, net.minecraft.world.entity.Entity center) {
        int n = 0;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, center.getBoundingBox().inflate(RADIUS), m -> m.isAlive()
                && m.distanceToSqr(center) <= RADIUS * RADIUS)) {
            mob.addEffect(new MobEffectInstance(MobEffects.LEVITATION, DAUER, 0));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DAUER, 4));
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DAUER, 2));
            n++;
        }
        level.sendParticles(ParticleTypes.END_ROD, center.getX(), center.getY(), center.getZ(), 60, RADIUS * 0.4, 1.0, RADIUS * 0.4, 0.05);
        level.playSound(null, center.blockPosition(), SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 1.2f, 0.6f);
        return n;
    }
}
