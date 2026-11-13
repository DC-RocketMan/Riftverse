package dev.riftverse.gadget;

import dev.riftverse.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tarn-Detektor (T2, 1.000 FE pro Sekunde, solange er an ist und in einer Hand liegt): Rechtsklick schaltet ihn ein.
 * Unsichtbare und getarnte Wesen im Umkreis von 24 Blöcken leuchten auf. Dasselbe kann das Helm-Modul.
 */
public class TarnDetektorItem extends EnergieGadget {
    public static final int PRO_SEKUNDE = 1_000, RADIUS = 24;

    public TarnDetektorItem(Properties properties) {
        super(properties, 100_000, 2_000);
    }

    public static boolean an(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MODUS.get(), 0) == 1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        boolean an = !an(stack);
        stack.set(ModDataComponents.MODUS.get(), an ? 1 : 0);
        level.playSound(null, player.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1f, an ? 1.4f : 0.8f);
        player.displayClientMessage(Component.translatable(an ? "message.riftverse.tarn.an" : "message.riftverse.tarn.aus")
                .withStyle(ChatFormatting.AQUA), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel sl) || !(entity instanceof Player player) || !an(stack)) return;
        if (level.getGameTime() % 20 != 0) return;
        boolean inHand = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!inHand) return;
        if (!ziehenLeise(player, stack, PRO_SEKUNDE)) {
            stack.set(ModDataComponents.MODUS.get(), 0);
            return;
        }
        aufdecken(sl, player);
    }

    /** Lässt Unsichtbares im Umkreis aufleuchten. @return Anzahl der aufgedeckten Wesen */
    public static int aufdecken(ServerLevel level, Player player) {
        int n = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS),
                e -> e != player && e.isAlive() && (e.isInvisible() || e.getPersistentData().getBoolean("riftverse:getarnt")))) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false));
            n++;
        }
        return n;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return an(stack);
    }
}
