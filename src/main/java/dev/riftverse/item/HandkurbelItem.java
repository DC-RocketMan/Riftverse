package dev.riftverse.item;

import dev.riftverse.tech.Crankable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Handkurbel aus Bronze: Rechtsklick gedrückt halten treibt Garagenpresse und Kurbel-Dynamo an.
 * Funktioniert wie der Pinsel: solange der Blick auf dem Block bleibt, wird gekurbelt.
 */
public class HandkurbelItem extends Item {
    /** Reichweite beim Kurbeln (Blöcke). */
    private static final double REACH = 5.0;

    public HandkurbelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player != null && ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof Crankable) {
            player.startUsingItem(ctx.getHand());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) {
            entity.releaseUsingItem();
            return;
        }
        HitResult hit = player.pick(REACH, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK
                || !(level.getBlockEntity(((BlockHitResult) hit).getBlockPos()) instanceof Crankable crankable)) {
            entity.releaseUsingItem();
            return;
        }
        if (!level.isClientSide) {
            crankable.crank(player);
            if (remainingUseDuration % 8 == 0) {
                level.playSound(null, ((BlockHitResult) hit).getBlockPos(), SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.4f, 1.6f);
            }
        }
    }
}
