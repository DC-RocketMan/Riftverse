package dev.riftverse.gadget;

import dev.riftverse.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Holo-Köder (T1, 2.000 FE pro Einsatz): projiziert ein Hologramm von dir, Mobs greifen es 10 s lang an. */
public class HoloKoederItem extends EnergieGadget {
    public static final int KOSTEN = 2_000;

    public HoloKoederItem(Properties properties) {
        super(properties, 20_000, 500);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) return InteractionResultHolder.success(stack);
        if (!ziehen(player, stack, KOSTEN)) return InteractionResultHolder.fail(stack);
        Vec3 spot = ziel(sl, player);
        HoloKoederEntity holo = ModEntities.HOLO_KOEDER.get().create(sl);
        if (holo == null) return InteractionResultHolder.fail(stack);
        holo.moveTo(spot.x, spot.y, spot.z, player.getYRot(), 0f);
        sl.addFreshEntity(holo);
        sl.playSound(null, holo.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.8f);
        player.displayClientMessage(Component.translatable("message.riftverse.holo").withStyle(ChatFormatting.GREEN), true);
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.success(stack);
    }

    /** Wo man hinschaut (höchstens 12 Blöcke), sonst vor die Füße. */
    public static Vec3 ziel(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(12));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection()));
        }
        Vec3 flat = player.getLookAngle().multiply(1, 0, 1);
        if (flat.lengthSqr() < 1.0e-4) return player.position();
        return player.position().add(flat.normalize().scale(3));
    }
}
