package dev.riftverse.gadget;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Aufgaben-Box (T3, Verbrauchsitem): Der große Knopf beschwört ein Helfer-Konstrukt für genau eine Aufgabe.
 * Rechtsklick auf ein Erz: es baut die ganze Ader ab. Auf ein Feld: es erntet reife Pflanzen und sät neu.
 * In die Luft: es kämpft für dich. Nach 5 Minuten wird es instabil und zerfällt explosiv.
 */
public class AufgabenBoxItem extends Item implements Gadget {
    public AufgabenBoxItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockState state = ctx.getLevel().getBlockState(ctx.getClickedPos());
        Helfer.Aufgabe a = Helfer.erzAn(state) ? Helfer.Aufgabe.ERZ : Helfer.feldAn(state) ? Helfer.Aufgabe.ERNTE : Helfer.Aufgabe.KAMPF;
        if (!(ctx.getLevel() instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        BlockPos spawn = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (Helfer.beschwoeren(sl, player, a, ctx.getClickedPos(), spawn) != null) ctx.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) return InteractionResultHolder.success(stack);
        BlockPos at = BlockPos.containing(player.position().add(player.getLookAngle().multiply(2, 0, 2)));
        if (Helfer.beschwoeren(sl, player, Helfer.Aufgabe.KAMPF, at, at) != null) stack.consume(1, player);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Gadget.beschreibung(this, tooltip);
    }
}
