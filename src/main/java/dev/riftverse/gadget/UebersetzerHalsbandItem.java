package dev.riftverse.gadget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Übersetzer-Halsband (T2, 10.000 FE): Rechtsklick auf ein eigenes gezähmtes Tier legt es an. Das Tier bekommt eine
 * Stimme (Sprechblasen), sammelt Gegenstände für dich ein, warnt vor Feinden und kommentiert, was du tust.
 * Schleichen + Rechtsklick mit leerer Hand nimmt das Halsband wieder ab (mit der restlichen Ladung).
 */
public class UebersetzerHalsbandItem extends EnergieGadget {
    public UebersetzerHalsbandItem(Properties properties) {
        super(properties, 10_000, 200);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof TamableAnimal pet) || !pet.isTame() || !pet.isOwnedBy(player)) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.riftverse.halsband.nicht_zahm").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.FAIL;
        }
        if (Halsband.hat(pet)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        Halsband.anlegen(pet, energy(stack));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        Halsband.sagen(pet, Component.translatable("halsband.riftverse.hallo", player.getDisplayName()));
        return InteractionResult.SUCCESS;
    }
}
