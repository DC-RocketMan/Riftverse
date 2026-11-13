package dev.riftverse.gadget;

import dev.riftverse.portal.RiftTravel;
import dev.riftverse.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Erinnerungsphiole (T2, Verbrauchsitem): Eine leere Phiole speichert mit Schleichen + Rechtsklick den Ort, an dem man
 * steht (Dimension und Position). Trinken bringt einen dorthin zurück; gefüllte Phiolen lassen sich weitergeben.
 */
public class ErinnerungsphioleItem extends Item implements Gadget {
    public ErinnerungsphioleItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GlobalPos ort = stack.get(ModDataComponents.ORT.get());
        if (ort == null) {
            if (!player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.riftverse.phiole.leer").withStyle(ChatFormatting.GRAY), true);
                }
                return InteractionResultHolder.pass(stack);
            }
            if (!level.isClientSide) {
                ItemStack filled = stack.copyWithCount(1);
                filled.set(ModDataComponents.ORT.get(), GlobalPos.of(level.dimension(), player.blockPosition()));
                stack.consume(1, player);
                if (!player.getInventory().add(filled)) player.drop(filled, false);
                level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL_DRAGONBREATH, SoundSource.PLAYERS, 1f, 1.2f);
                player.displayClientMessage(Component.translatable("message.riftverse.phiole.gespeichert").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        GlobalPos ort = stack.get(ModDataComponents.ORT.get());
        if (ort == null || !(entity instanceof ServerPlayer player)) return stack;
        if (erinnern(player, ort)) stack.consume(1, player);
        return stack;
    }

    /** Bringt den Spieler an den gespeicherten Ort. */
    public static boolean erinnern(ServerPlayer player, GlobalPos ort) {
        ServerLevel target = player.server.getLevel(ort.dimension());
        if (target == null) return false;
        BlockPos safe = RiftTravel.findSafe(target, ort.pos(), 8, true);
        player.serverLevel().sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        Vec3 to = Vec3.atBottomCenterOf(safe);
        RiftTravel.teleport(player, target, to, player.getYRot());
        if (player.level() == target && player.position().distanceToSqr(to) > 0.01) player.setPos(to.x, to.y, to.z);   // FakePlayer
        target.playSound(null, safe, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.4f);
        target.sendParticles(ParticleTypes.WITCH, safe.getX() + 0.5, safe.getY() + 1, safe.getZ() + 0.5, 30, 0.4, 0.8, 0.4, 0.05);
        return true;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.ORT.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos ort = stack.get(ModDataComponents.ORT.get());
        if (ort != null) {
            tooltip.add(Component.translatable("tooltip.riftverse.phiole.ort", ort.dimension().location().toString(),
                    ort.pos().getX(), ort.pos().getY(), ort.pos().getZ()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        Gadget.beschreibung(this, tooltip);
    }
}
