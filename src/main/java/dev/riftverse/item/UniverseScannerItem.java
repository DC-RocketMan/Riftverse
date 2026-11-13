package dev.riftverse.item;

import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.MultiverseData;
import dev.riftverse.universe.UniverseCleanup;
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

/** Zeigt Typ, Naturgesetze und Modifikatoren des aktuellen Universums – und ob es verankert ist. */
public class UniverseScannerItem extends Item {
    public UniverseScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel sl)) return InteractionResultHolder.success(stack);

        UniverseId id = Multiverse.at(level, player.blockPosition());
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
        if (id == null) {
            player.sendSystemMessage(Component.translatable("message.riftverse.scanner.home").withStyle(ChatFormatting.AQUA));
            return InteractionResultHolder.success(stack);
        }
        UniverseProfile p = UniverseProfile.get(id);
        MultiverseData.Record rec = MultiverseData.get(sl.getServer()).record(id);

        player.sendSystemMessage(Component.literal("━━ " + p.title() + " „" + p.name + "“ ━━").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        line(player, "message.riftverse.scanner.type", p.zucht != null
                ? p.type.displayName().copy().append(" (").append(p.zucht.describe()).append(")") : p.type.displayName());
        line(player, "message.riftverse.scanner.modifiers", p.modifierText());
        line(player, "message.riftverse.scanner.gravity", Component.literal(p.gravityText()));
        line(player, "message.riftverse.scanner.air", Component.literal(p.atmosphere.label));
        line(player, "message.riftverse.scanner.terrain", Component.literal(p.terrain.label + " · " + p.palette.name()));

        long now = sl.getServer().overworld().getGameTime();
        Component status;
        if (rec.isAnchored()) {
            status = Component.translatable("message.riftverse.scanner.anchored").withStyle(ChatFormatting.GREEN);
        } else {
            int days = UniverseCleanup.daysLeft(rec, now);
            status = days < 0
                    ? Component.translatable("message.riftverse.scanner.built").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("message.riftverse.scanner.reset", days).withStyle(ChatFormatting.YELLOW);
        }
        line(player, "message.riftverse.scanner.status", status);
        return InteractionResultHolder.success(stack);
    }

    private static void line(Player player, String key, Component value) {
        player.sendSystemMessage(Component.literal(" ").append(Component.translatable(key).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(value));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.riftverse.universe_scanner.lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
