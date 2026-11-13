package dev.riftverse.gadget;

import dev.riftverse.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Einzweck-Drohne (T1, 5.000 FE): eine winzige Drohne mit genau einer Aufgabe. Schleichen + Rechtsklick in die Luft
 * wechselt die Aufgabe, Schleichen + Rechtsklick auf eine Kiste führt sie dort aus:
 * <ul>
 *   <li><b>Aufsammeln:</b> Rechtsklick schaltet sie ein; sie holt Gegenstände im Umkreis von 8 Blöcken (2 FE/t).</li>
 *   <li><b>Reichen:</b> Rechtsklick auf einen Spieler (oder Schleichen + Rechtsklick auf eine Kiste) reicht den Stapel aus der Zweithand (10 FE).</li>
 *   <li><b>Sortieren:</b> Schleichen + Rechtsklick auf eine Kiste fasst Stapel zusammen und sortiert sie (50 FE).</li>
 * </ul>
 * Im Leerlauf fragt sie nach ihrem Lebenssinn.
 */
public class EinzweckDrohneItem extends EnergieGadget {
    public static final int AUFSAMMELN = 0, REICHEN = 1, SORTIEREN = 2;
    public static final int AN = 4;
    public static final int RADIUS = 8;
    public static final int SORT_KOSTEN = 50, REICH_KOSTEN = 10;

    public EinzweckDrohneItem(Properties properties) {
        super(properties, 5_000, 100);
    }

    public static int aufgabe(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MODUS.get(), 0) & 3;
    }

    public static boolean an(ItemStack stack) {
        return (stack.getOrDefault(ModDataComponents.MODUS.get(), 0) & AN) != 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        int modus = stack.getOrDefault(ModDataComponents.MODUS.get(), 0);
        if (player.isShiftKeyDown()) {
            int next = ((modus & 3) + 1) % 3;
            stack.set(ModDataComponents.MODUS.get(), next);
            player.displayClientMessage(Component.translatable("message.riftverse.drohne.aufgabe",
                    Component.translatable("gui.riftverse.drohne." + next)).withStyle(ChatFormatting.AQUA), true);
        } else if ((modus & 3) == AUFSAMMELN) {
            boolean an = (modus & AN) == 0;
            stack.set(ModDataComponents.MODUS.get(), an ? AUFSAMMELN | AN : AUFSAMMELN);
            player.displayClientMessage(Component.translatable(an ? "message.riftverse.drohne.an" : "message.riftverse.drohne.aus")
                    .withStyle(ChatFormatting.AQUA), true);
        } else {
            player.displayClientMessage(Component.translatable("message.riftverse.drohne.ziel_fehlt").withStyle(ChatFormatting.GRAY), true);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BEEHIVE_WORK, SoundSource.PLAYERS, 0.6f, 1.8f);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        // Kisten öffnen sonst ihre GUI: die Drohne arbeitet mit Schleichen + Rechtsklick
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, ctx.getClickedPos(), ctx.getClickedFace());
        if (handler == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int a = aufgabe(stack);
        if (a == SORTIEREN) {
            if (!ziehen(player, stack, SORT_KOSTEN)) return InteractionResult.FAIL;
            int n = sortieren(handler, player);
            player.displayClientMessage(Component.translatable("message.riftverse.drohne.sortiert", n).withStyle(ChatFormatting.AQUA), true);
        } else if (a == REICHEN) {
            ItemStack off = player.getOffhandItem();
            if (off.isEmpty() || !ziehen(player, stack, REICH_KOSTEN)) return InteractionResult.FAIL;
            ItemStack rest = ItemHandlerHelper.insertItemStacked(handler, off.copy(), false);
            player.setItemInHand(InteractionHand.OFF_HAND, rest);
        } else {
            return InteractionResult.PASS;
        }
        level.playSound(null, ctx.getClickedPos(), SoundEvents.BEEHIVE_WORK, SoundSource.PLAYERS, 0.8f, 1.5f);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (aufgabe(stack) != REICHEN || !(target instanceof Player other)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        ItemStack off = player.getOffhandItem();
        if (off.isEmpty() || !ziehen(player, stack, REICH_KOSTEN)) return InteractionResult.FAIL;
        ItemStack give = off.copy();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        if (!other.getInventory().add(give)) other.drop(give, false);
        other.displayClientMessage(Component.translatable("message.riftverse.drohne.gereicht", player.getDisplayName())
                .withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.SUCCESS;
    }

    /** Fasst gleiche Stapel zusammen und sortiert nach Item-Name. Was nicht zurückpasst, bekommt der Spieler. */
    public static int sortieren(IItemHandler handler, Player player) {
        List<ItemStack> all = new ArrayList<>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.extractItem(i, handler.getSlotLimit(i), false);
            if (s.isEmpty()) continue;
            boolean merged = false;
            for (ItemStack o : all) {
                if (ItemStack.isSameItemSameComponents(o, s)) {
                    o.grow(s.getCount());
                    merged = true;
                    break;
                }
            }
            if (!merged) all.add(s.copy());
        }
        all.sort(Comparator.comparing((ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));

        for (ItemStack s : all) {
            ItemStack left = s;
            while (!left.isEmpty()) {
                ItemStack part = left.split(Math.min(left.getCount(), left.getMaxStackSize()));
                ItemStack rest = ItemHandlerHelper.insertItemStacked(handler, part, false);
                if (!rest.isEmpty()) {
                    if (!player.getInventory().add(rest)) player.drop(rest, false);
                }

            }
        }
        return all.size();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel sl) || !(entity instanceof Player player) || !an(stack)) return;
        if (level.getGameTime() % 5 != 0) return;
        if (!ziehenLeise(player, stack, 10)) {
            stack.set(ModDataComponents.MODUS.get(), AUFSAMMELN);
            player.displayClientMessage(Component.translatable("message.riftverse.drohne.leer").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        List<ItemEntity> items = sl.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(RADIUS),
                i -> i.isAlive() && !i.hasPickUpDelay());
        for (ItemEntity item : items) {
            item.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            item.setNoPickUpDelay();
        }
        if (items.isEmpty() && level.getGameTime() % 600 == 0 && level.random.nextInt(3) == 0) {
            player.displayClientMessage(Component.translatable("message.riftverse.drohne.sinn." + level.random.nextInt(4))
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("message.riftverse.drohne.aufgabe", Component.translatable("gui.riftverse.drohne." + aufgabe(stack)))
                .append(an(stack) ? Component.literal(" ●").withStyle(ChatFormatting.GREEN) : Component.empty())
                .withStyle(ChatFormatting.AQUA));
    }
}
