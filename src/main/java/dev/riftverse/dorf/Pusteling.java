package dev.riftverse.dorf;

import dev.riftverse.registry.ModDorf;
import dev.riftverse.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Pusteling: hüpfender Flauschball. Mit der Schere lässt er sich alle fünf Minuten scheren (Flausch), er frisst
 * Leuchtglocken.
 */
public class Pusteling extends Animal {
    private int geschoren;

    public Pusteling(EntityType<? extends Pusteling> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 8).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    private static final Ingredient FUTTER = Ingredient.of(ModDorf.LEUCHTGLOCKE_BLAU.get(), ModDorf.LEUCHTGLOCKE_VIOLETT.get(),
            ModDorf.LEUCHTGLOCKE_TUERKIS.get(), ModDorf.LEUCHTGLOCKE_ROSA.get());

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.1, FUTTER, false));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (geschoren > 0) geschoren--;
        // Hüpfen statt Laufen
        if (!level().isClientSide && onGround() && !getNavigation().isDone() && random.nextInt(8) == 0) {
            setDeltaMovement(getDeltaMovement().add(0, 0.32, 0));
            hasImpulse = true;
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.SHEARS) && geschoren == 0 && !isBaby()) {
            if (!level().isClientSide) {
                geschoren = 6000;
                spawnAtLocation(new ItemStack(ModDorf.FLAUSCH.get(), 1 + random.nextInt(2)), 0.6f);
                level().playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0f, 1.4f);
                stack.hurtAndBreak(1, player, getSlotForHand(hand));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return FUTTER.test(stack);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.PUSTELING.get().create(level);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Geschoren", geschoren);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        geschoren = tag.getInt("Geschoren");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SLIME_SQUISH_SMALL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    public float getVoicePitch() {
        return 1.5f;
    }
}
