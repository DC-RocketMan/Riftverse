package dev.riftverse.universe;

import dev.riftverse.Riftverse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Wendet die physikalischen Gesetze des aktuellen Universums an.
 *
 * Gravitation: über das Gravitations-Attribut. Weil die Sprungkraft gleich bleibt,
 * springt man bei niedriger Schwerkraft automatisch höher (wie auf dem Mond).
 * Fallschaden skaliert mit g (Aufprallenergie ∝ g·h), sichere Fallhöhe mit 1/g.
 * Kreaturen wachsen bei niedriger Schwerkraft (Quadrat-Kubik-Gesetz) und
 * schrumpfen bei hoher.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class PhysicsHandler {
    private static final ResourceLocation GRAVITY = Riftverse.id("universe_gravity");
    private static final ResourceLocation SAFE_FALL = Riftverse.id("universe_safe_fall");
    private static final ResourceLocation FALL_DAMAGE = Riftverse.id("universe_fall_damage");
    private static final ResourceLocation DRAG = Riftverse.id("universe_air_drag");
    private static final ResourceLocation SCALE = Riftverse.id("universe_scale");

    @Nullable
    public static UniverseProfile profileOf(LivingEntity entity) {
        UniverseId id = Multiverse.at(entity.level(), entity.blockPosition());
        return id == null ? null : UniverseProfile.get(id);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        UniverseProfile p = Multiverse.isMultiverse(player.level()) ? profileOf(player) : null;
        if (player.tickCount % 10 == 0) apply(player, p, false);
        if (p == null || player.isCreative() || player.isSpectator()) return;

        // Kälte: läuft jeden Tick, weil Minecraft Frost sonst wieder abbaut
        if (p.temperature == UniverseProfile.Temperature.COLD && player.canFreeze()) {
            int max = player.getTicksRequiredToFreeze() + 20;
            player.setTicksFrozen(Math.min(max, player.getTicksFrozen() + 3));
        }

        if (player.tickCount % 20 != 0) return;
        switch (p.atmosphere) {
            case TOXIC -> {
                if (player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                    player.addEffect(new MobEffectInstance(MobEffects.POISON, 50, 0, false, true));
                    if (player.tickCount % 200 == 0)
                        player.displayClientMessage(Component.literal("☣ Giftige Atmosphäre – setz einen Helm auf!")
                                .withStyle(ChatFormatting.DARK_GREEN), true);
                }
            }
            case THIN -> {
                if (player.isSprinting()) player.causeFoodExhaustion(0.6f);
            }
            default -> { }
        }
        if (p.temperature == UniverseProfile.Temperature.HOT) {
            player.causeFoodExhaustion(0.25f);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            apply(living, profileOf(living), true);
        }
    }

    public static void apply(LivingEntity e, @Nullable UniverseProfile p, boolean scaleCreature) {
        double g = p == null ? 1.0 : p.gravity;
        set(e, Attributes.GRAVITY, GRAVITY, g - 1.0);
        set(e, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL, 1.0 / g - 1.0);
        set(e, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE, g - 1.0);

        double drag = 0;
        if (p != null) {
            if (p.atmosphere == UniverseProfile.Atmosphere.DENSE) drag = -0.12;
            else if (p.atmosphere == UniverseProfile.Atmosphere.THIN) drag = 0.06;
        }
        set(e, Attributes.MOVEMENT_SPEED, DRAG, drag);

        if (scaleCreature) {
            double scale = p == null ? 1.0 : Math.max(0.7, Math.min(1.5, 1.0 / Math.sqrt(g)));
            set(e, Attributes.SCALE, SCALE, scale - 1.0);
        }
    }

    private static void set(LivingEntity e, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance inst = e.getAttribute(attribute);
        if (inst == null) return;
        if (Math.abs(amount) < 1.0e-4) {
            if (inst.getModifier(id) != null) inst.removeModifier(id);
            return;
        }
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < 1.0e-4) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private PhysicsHandler() {}
}
