package dev.riftverse.gadget;

import dev.riftverse.Riftverse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Schrumpfstrahler (T2, 50.000 FE, 2.000 FE pro Schuss): Jeder Treffer halbiert die Größe (1 → 0,5 → 0,25), der
 * nächste setzt sie wieder auf 1. Wer nichts anvisiert, trifft sich selbst; Schleichen setzt das Ziel sofort zurück.
 * Geschrumpfte Mobs richten anteilig weniger Schaden an.
 */
public class SchrumpfstrahlerItem extends EnergieGadget {
    public static final int KOSTEN = 2_000;
    public static final double REICHWEITE = 24.0;
    public static final ResourceLocation ID = Riftverse.id("schrumpfstrahler");

    public SchrumpfstrahlerItem(Properties properties) {
        super(properties, 50_000, 1_000);
    }

    /** Aktuelle Größe durch den Strahler (1, 0,5 oder 0,25). */
    public static double faktor(LivingEntity e) {
        AttributeInstance inst = e.getAttribute(Attributes.SCALE);
        AttributeModifier m = inst == null ? null : inst.getModifier(ID);
        return m == null ? 1.0 : 1.0 + m.amount();
    }

    public static void setFaktor(LivingEntity e, double f) {
        set(e, Attributes.SCALE, f);
        set(e, Attributes.ATTACK_DAMAGE, f);
    }

    private static void set(LivingEntity e, Holder<Attribute> attr, double f) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(ID);
        if (f < 0.999) inst.addPermanentModifier(new AttributeModifier(ID, f - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) return InteractionResultHolder.success(stack);
        if (!ziehen(player, stack, KOSTEN)) return InteractionResultHolder.fail(stack);
        LivingEntity target = anvisiert(player);
        if (target == null) target = player;
        double f = faktor(target);
        double next = player.isShiftKeyDown() ? 1.0 : f > 0.75 ? 0.5 : f > 0.375 ? 0.25 : 1.0;
        setFaktor(target, next);
        Vec3 from = player.getEyePosition(), to = target.position().add(0, target.getBbHeight() / 2, 0);
        for (int i = 0; i <= 16; i++) {
            Vec3 p = from.lerp(to, i / 16.0);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, next < 1 ? 1.8f : 0.8f);
        player.displayClientMessage(Component.translatable("message.riftverse.schrumpf", target.getDisplayName(),
                String.format(java.util.Locale.GERMAN, "%.2f", next)).withStyle(ChatFormatting.AQUA), true);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.success(stack);
    }

    @Nullable
    public static LivingEntity anvisiert(Player player) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(REICHWEITE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(REICHWEITE)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator(), REICHWEITE * REICHWEITE);
        Entity e = hit == null ? null : hit.getEntity();
        return e instanceof LivingEntity l ? l : null;
    }
}
