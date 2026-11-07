package dev.riftverse.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Schrott-Golem: lebt in den Halden der Schrottwelt. Ein kleiner, rostiger Verwandter des Eisengolems,
 * der Eindringlinge angreift und beim Tod Alien-Schrott fallen lässt.
 */
public class SchrottGolem extends IronGolem {
    public SchrottGolem(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.SCALE, 0.7);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Überall in den Halden, bei Tag wie bei Nacht – nur nicht auf Friedlich. */
    public static boolean checkSpawnRules(EntityType<SchrottGolem> type, ServerLevelAccessor level, MobSpawnType reason,
                                          BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }
}
