package dev.riftverse.gadget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Zeitstopper (T3, 200.000 FE, 40.000 FE pro Einsatz): friert Mobs und Geschosse im Radius 12 für 6 s ein. */
public class ZeitstopperItem extends EnergieGadget {
    public static final int KOSTEN = 40_000;

    public ZeitstopperItem(Properties properties) {
        super(properties, 200_000, 4_000);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) return InteractionResultHolder.success(stack);
        if (!ziehen(player, stack, KOSTEN)) return InteractionResultHolder.fail(stack);
        int n = Zeitstopp.anhalten(sl, player.position(), player);
        sl.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.5f, 0.5f);
        player.displayClientMessage(Component.translatable("message.riftverse.zeitstopp", n).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        player.getCooldowns().addCooldown(this, Zeitstopp.DAUER);
        return InteractionResultHolder.success(stack);
    }
}
