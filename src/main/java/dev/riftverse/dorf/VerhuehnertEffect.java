package dev.riftverse.dorf;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Verhühnert (Meilenstein 10): Wer einen Gleeb schlägt, wird für fünf Minuten zum Huhn. Man gackert, flattert
 * sanft zu Boden, legt ab und zu ein Ei und kann weder Blöcke abbauen noch setzen (siehe {@link Huhn}). Das Aussehen
 * übernimmt der Client ({@code client/dorf/HuhnRender}).
 */
public class VerhuehnertEffect extends MobEffect {
    public static final int DAUER = 5 * 60 * 20;

    public VerhuehnertEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFF4E0);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Flattern: Man fällt wie ein Huhn, ganz langsam
        Vec3 v = entity.getDeltaMovement();
        if (!entity.onGround() && v.y < -0.08) entity.setDeltaMovement(v.x, v.y * 0.6, v.z);
        entity.fallDistance = 0;
        if (entity.level().isClientSide) return true;
        var random = entity.getRandom();
        if (random.nextInt(140) == 0) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CHICKEN_AMBIENT,
                    SoundSource.PLAYERS, 1.0f, 0.9f + random.nextFloat() * 0.3f);
        }
        if (entity instanceof Player && random.nextInt(1800) == 0) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CHICKEN_EGG,
                    SoundSource.PLAYERS, 1.0f, 1.0f);
            entity.spawnAtLocation(Items.EGG);
        }
        return true;
    }
}
