package dev.riftverse.gadget;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.List;

/**
 * Mutagen-Spritze (T3, Verbrauchsitem): Rechtsklick auf einen Mob löst eine zufällige Mutation aus – Größe, Tempo oder
 * eine neue Fähigkeit. Mit 10 % Risiko wird er zur Fehlmutation: größer, dreimal so zäh, doppelt so stark, leuchtend,
 * mit Sonderbeute.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public class MutagenSpritzeItem extends Item implements Gadget {
    public static final String FEHLMUTATION = "riftverse:fehlmutation";
    public static final float FEHL_RISIKO = 0.1f;

    public MutagenSpritzeItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob) || !mob.isAlive()) return InteractionResult.PASS;
        if (!(player.level() instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        Component was = mutieren(sl, mob, sl.random);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(was.copy().withStyle(mob.getPersistentData().getBoolean(FEHLMUTATION)
                ? ChatFormatting.DARK_RED : ChatFormatting.LIGHT_PURPLE), true);
        return InteractionResult.SUCCESS;
    }

    /** Mutiert den Mob. @return Beschreibung, was passiert ist */
    public static Component mutieren(ServerLevel level, Mob mob, RandomSource r) {
        level.playSound(null, mob.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.NEUTRAL, 1f, 0.6f);
        level.sendParticles(ParticleTypes.WITCH, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 30, 0.4, 0.5, 0.4, 0.05);
        if (r.nextFloat() < FEHL_RISIKO) return fehlmutation(level, mob);
        int n = mob.getPersistentData().getInt("riftverse:mutationen") + 1;
        mob.getPersistentData().putInt("riftverse:mutationen", n);
        ResourceLocation id = Riftverse.id("mutation_" + n);
        switch (r.nextInt(3)) {
            case 0 -> {
                double f = r.nextBoolean() ? 0.5 : 1.6;
                add(mob, Attributes.SCALE, id, f - 1.0);
                return Component.translatable(f < 1 ? "message.riftverse.mutation.klein" : "message.riftverse.mutation.gross");
            }
            case 1 -> {
                add(mob, Attributes.MOVEMENT_SPEED, id, 0.5);
                return Component.translatable("message.riftverse.mutation.tempo");
            }
            default -> {
                List<Holder<MobEffect>> faehigkeiten = List.of(MobEffects.FIRE_RESISTANCE, MobEffects.REGENERATION,
                        MobEffects.DAMAGE_RESISTANCE, MobEffects.DAMAGE_BOOST, MobEffects.JUMP, MobEffects.WATER_BREATHING);
                Holder<MobEffect> e = faehigkeiten.get(r.nextInt(faehigkeiten.size()));
                mob.addEffect(new MobEffectInstance(e, MobEffectInstance.INFINITE_DURATION, 0, false, true));
                mob.setPersistenceRequired();
                return Component.translatable("message.riftverse.mutation.faehigkeit", e.value().getDisplayName());
            }
        }
    }

    private static Component fehlmutation(ServerLevel level, Mob mob) {
        ResourceLocation id = Riftverse.id("fehlmutation");
        add(mob, Attributes.SCALE, id, 0.6);
        add(mob, Attributes.MAX_HEALTH, id, 2.0);
        add(mob, Attributes.ATTACK_DAMAGE, id, 1.0);
        mob.setHealth(mob.getMaxHealth());
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        mob.setCustomName(Component.translatable("entity.riftverse.fehlmutation", mob.getType().getDescription()).withStyle(ChatFormatting.DARK_RED));
        mob.getPersistentData().putBoolean(FEHLMUTATION, true);
        mob.setPersistenceRequired();
        level.playSound(null, mob.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 0.8f, 1.4f);
        return Component.translatable("message.riftverse.mutation.fehl");
    }

    private static void add(Mob mob, Holder<Attribute> attr, ResourceLocation id, double amount) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** Sonderbeute der Fehlmutation. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (!e.getPersistentData().getBoolean(FEHLMUTATION) || !(e.level() instanceof ServerLevel sl)) return;
        RandomSource r = sl.random;
        List<ItemStack> beute = List.of(new ItemStack(ModItems.BIOMASSE_PULPE.get(), 4 + r.nextInt(5)),
                new ItemStack(ModItems.NAEHRSERUM.get(), 1 + r.nextInt(2)),
                r.nextBoolean() ? new ItemStack(ModItems.INVERSIONSSPLITTER.get()) : new ItemStack(ModItems.GRAVITIT.get()));
        for (ItemStack s : beute) {
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(sl, e.getX(), e.getY() + 0.5, e.getZ(), s));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Gadget.beschreibung(this, tooltip);
    }
}
