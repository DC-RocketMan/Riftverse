package dev.riftverse.tech.upgrade;

import dev.riftverse.Riftverse;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/** Upgrade-Chip: Rechtsklick auf eine Maschine baut ihn ein. */
public class UpgradeChipItem extends Item {
    private final UpgradeChip chip;

    public UpgradeChipItem(Properties properties, UpgradeChip chip) {
        super(properties.stacksTo(16));
        this.chip = chip;
    }

    public UpgradeChip chip() {
        return chip;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockEntity be = ctx.getLevel().getBlockEntity(ctx.getClickedPos());
        if (!(be instanceof Aufruestbar a)) return InteractionResult.PASS;
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        var player = ctx.getPlayer();
        if (!a.einbauen(chip)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.riftverse.chip.schon_drin").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResult.FAIL;
        }
        ctx.getItemInHand().consume(1, player);
        ctx.getLevel().playSound(null, ctx.getClickedPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1f, 1.4f);
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.riftverse.chip.eingebaut", ctx.getItemInHand().getHoverName(),
                    a.chips().size()).withStyle(ChatFormatting.GREEN), true);
            if (chip == UpgradeChip.KI) {
                player.displayClientMessage(Component.translatable("ki.riftverse.hallo." + ctx.getLevel().random.nextInt(3))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip." + Riftverse.MODID + "." + chip.itemName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.riftverse.chip.einbau").withStyle(ChatFormatting.DARK_GRAY));
    }
}
