package dev.riftverse.item;

import dev.riftverse.story.MultiverseData;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Zeigt die Naturgesetze und den Story-Status des aktuellen Universums. */
public class UniverseScannerItem extends Item {
    public UniverseScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        UniverseId id = Multiverse.at(level, player.blockPosition());
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
        if (id == null) {
            player.sendSystemMessage(Component.literal("⌁ Scanner: Heimat-Realität. 1,00 g, atembar, gemäßigt.")
                    .withStyle(ChatFormatting.AQUA));
            return InteractionResultHolder.success(stack);
        }
        UniverseProfile p = UniverseProfile.get(id);
        MultiverseData data = MultiverseData.get(((ServerLevel) level).getServer());
        MultiverseData.Record rec = data.record(id);

        player.sendSystemMessage(Component.literal("━━ " + p.title() + " „" + p.name + "“ ━━").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        line(player, "Gravitation", p.gravityText());
        line(player, "Atmosphäre", p.atmosphere.label);
        line(player, "Temperatur", p.temperature.label);
        line(player, "Gelände", p.terrain.label + " · " + p.palette.name());
        line(player, "Rohstoffe", p.richnessLabel());
        line(player, "Gefahrenstufe", "★".repeat(p.danger) + "☆".repeat(5 - p.danger));
        String status = switch (rec.state) {
            case MultiverseData.STATE_ACTIVE -> "⚠ " + rec.bossName + " herrscht hier noch";
            case MultiverseData.STATE_DEFEATED -> "✔ befreit (" + rec.bossName + " besiegt)";
            case MultiverseData.STATE_SPARED -> "☮ " + rec.bossName + " wurde verschont";
            default -> "unbekannt – noch keine Geschichte";
        };
        line(player, "Geschichte", status);
        return InteractionResultHolder.success(stack);
    }

    private static void line(Player player, String key, String value) {
        player.sendSystemMessage(Component.literal(" " + key + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.riftverse.universe_scanner.lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
