package dev.riftverse.boss;

/** Fähigkeiten, aus denen jeder Boss zwei (später drei) zieht. */
public enum BossAbility {
    GRAVITY_PULSE("Gravitationspuls", 200, 12),
    PLASMA_BEAM("Plasmastrahl", 120, 24),
    SUMMON_MINIONS("Beschwörung", 400, 30),
    BLINK("Raumsprung", 160, 20),
    TIME_WARP("Zeitverzerrung", 300, 14),
    GROUND_SLAM("Bodenschlag", 220, 8);

    public final String label;
    public final int cooldown;
    public final double maxRange;

    BossAbility(String label, int cooldown, double maxRange) {
        this.label = label;
        this.cooldown = cooldown;
        this.maxRange = maxRange;
    }
}
