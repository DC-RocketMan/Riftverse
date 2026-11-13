package dev.riftverse.gadget;

import dev.riftverse.Riftverse;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Eingefrorene Mobs und Geschosse des Zeitstoppers. Mobs verlieren ihre KI, Geschosse ihre Schwerkraft und ihr Tempo;
 * nach {@link #DAUER} Ticks geht es weiter, als wäre nichts gewesen.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class Zeitstopp {
    public static final int DAUER = 120;
    public static final double RADIUS = 12.0;

    private record Gefroren(long bis, Vec3 tempo, boolean ki, boolean schwer) {}

    private static final Map<Entity, Gefroren> GEFROREN = new HashMap<>();

    public static boolean gefroren(Entity e) {
        return GEFROREN.containsKey(e);
    }

    /** Friert alles um {@code center} ein (Spieler und der Auslöser ausgenommen). @return Anzahl */
    public static int anhalten(ServerLevel level, Vec3 center, Entity ausloeser) {
        long bis = level.getGameTime() + DAUER;
        int n = 0;
        for (Entity e : level.getEntities(ausloeser, new AABB(center, center).inflate(RADIUS),
                e -> (e instanceof Mob || e instanceof Projectile) && e.position().distanceToSqr(center) <= RADIUS * RADIUS)) {
            Gefroren alt = GEFROREN.get(e);
            if (alt != null) {
                GEFROREN.put(e, new Gefroren(bis, alt.tempo(), alt.ki(), alt.schwer()));
                continue;
            }
            boolean ki = e instanceof Mob m && m.isNoAi();
            GEFROREN.put(e, new Gefroren(bis, e.getDeltaMovement(), ki, e.isNoGravity()));
            if (e instanceof Mob m) m.setNoAi(true);
            e.setNoGravity(true);
            e.setDeltaMovement(Vec3.ZERO);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 6, 0.2, 0.3, 0.2, 0.01);
            n++;
        }
        return n;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || GEFROREN.isEmpty()) return;
        long now = sl.getGameTime();
        Iterator<Map.Entry<Entity, Gefroren>> it = GEFROREN.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Entity, Gefroren> en = it.next();
            Entity e = en.getKey();
            if (e.level() != sl) {
                if (e.isRemoved()) it.remove();
                continue;
            }
            Gefroren g = en.getValue();
            if (e.isRemoved()) {
                it.remove();
            } else if (now >= g.bis()) {
                if (e instanceof Mob m) m.setNoAi(g.ki());
                e.setNoGravity(g.schwer());
                e.setDeltaMovement(g.tempo());
                e.hurtMarked = true;
                it.remove();
            } else {
                e.setDeltaMovement(Vec3.ZERO);
                if (e instanceof Projectile) e.setPos(e.xo, e.yo, e.zo);
                if (now % 10 == 0) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 0.5, e.getZ(), 1, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    /** Für Rauchtests: alles sofort freigeben. */
    public static void allesFrei() {
        GEFROREN.clear();
    }


    private Zeitstopp() {}
}
