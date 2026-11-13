package dev.riftverse.gadget;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Hologramm des Holo-Köders: ein grünes Gitter in Spielergestalt. Zehn Sekunden lang lenkt es alle feindlichen Mobs
 * im Umkreis von 16 Blöcken auf sich; Schaden nimmt es keinen. Wird nicht gespeichert.
 */
public class HoloKoederEntity extends Mob {
    public static final int DAUER = 200, RADIUS = 16;
    /** Wie oft ein Mob schon auf ein Hologramm umgelenkt wurde (für den Rauchtest). */
    public static int abgelenkt;
    private static final DustParticleOptions GITTER = new DustParticleOptions(new Vector3f(0.22f, 1.0f, 0.42f), 0.8f);

    public HoloKoederEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setNoAi(true);
        setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (tickCount >= DAUER) {
            discard();
            return;
        }
        if (tickCount % 10 == 0) {
            for (Mob mob : sl.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(RADIUS), m -> m instanceof Enemy && m.isAlive())) {
                if (mob.getTarget() != this) {
                    mob.setTarget(this);
                    abgelenkt++;
                }
            }
        }
        if (tickCount % 3 == 0 && random.nextInt(8) != 0) {   // flackert
            Vec3 p = position();
            for (int i = 0; i < 14; i++) {
                double y = random.nextDouble() * 1.8;
                double w = y > 1.4 ? 0.25 : y > 0.75 ? 0.4 : 0.2;
                double a = random.nextDouble() * Math.PI * 2;
                sl.sendParticles(GITTER, p.x + Math.cos(a) * w, p.y + y, p.z + Math.sin(a) * w * 0.6, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
