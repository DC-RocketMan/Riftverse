package dev.riftverse.client;

import dev.riftverse.Riftverse;
import dev.riftverse.item.PortalKanoneItem;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModDataComponents.KanonenTank;
import dev.riftverse.ruestung.Modul;
import dev.riftverse.ruestung.MultiversalArmorItem;
import dev.riftverse.ruestung.Ruestung;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * HUD des Analyse-Visiers (Grundeigenschaft des Helms): Universumstyp, Physik-Modifikatoren und Fluidstand der
 * Kanone. Mit dem Analyse-HUD-Modul dazu Erze im Umkreis von 8 Blöcken und Lebenspunkte und Werte des Mobs im
 * Fadenkreuz.
 */
@EventBusSubscriber(modid = Riftverse.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RuestungHud {
    public static final int ERZ_RADIUS = 8;

    private static long scanTick = -1;
    private static List<Component> erze = List.of();

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Riftverse.id("analyse_visier"), RuestungHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.level == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        ItemStack helm = Ruestung.teil(p, EquipmentSlot.HEAD);
        if (helm.isEmpty() || (!p.getAbilities().instabuild && Ruestung.energie(p, EquipmentSlot.HEAD) <= 0)) return;

        List<Component> lines = new ArrayList<>();
        UniverseId id = Multiverse.at(mc.level, p.blockPosition());
        if (id == null) {
            lines.add(Component.translatable("hud.riftverse.heimat").withStyle(ChatFormatting.AQUA));
        } else {
            UniverseProfile prof = UniverseProfile.get(id);
            lines.add(Component.literal(prof.title() + " „" + prof.name + "“").withStyle(ChatFormatting.GREEN));
            lines.add(Component.translatable("hud.riftverse.physik", prof.gravityText(), prof.modifierText()).withStyle(ChatFormatting.GRAY));
        }
        ItemStack gun = PortalKanoneItem.find(p);
        KanonenTank chest = Ruestung.teil(p, EquipmentSlot.CHEST).get(ModDataComponents.KANONEN_TANK.get());
        if (gun != null || chest != null) {
            int mb = gun == null ? 0 : PortalKanoneItem.tank(gun).mb();
            lines.add(Component.translatable("hud.riftverse.kanone", mb, PortalKanoneItem.TANK, chest == null ? 0 : chest.mb(), Ruestung.BRUSTTANK)
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        lines.add(Component.translatable("hud.riftverse.energie", kilo(Ruestung.energie(p, EquipmentSlot.HEAD)))
                .withStyle(ChatFormatting.DARK_GREEN));
        int legs = Ruestung.zustand(p, EquipmentSlot.LEGS);
        if ((legs & 3) != 0 && Ruestung.hat(p, Modul.GRAVITATIONSREGLER)) {
            lines.add(Component.translatable("message.riftverse.gravitationsregler." + (legs & 3)).withStyle(ChatFormatting.AQUA));
        }
        if ((legs & 4) != 0 && Ruestung.hat(p, Modul.SCHRUMPF_MODUL)) lines.add(Component.translatable("message.riftverse.modul.schrumpf.an").withStyle(ChatFormatting.AQUA));
        if ((Ruestung.zustand(p, EquipmentSlot.HEAD) & 1) != 0 && Ruestung.hat(p, Modul.TARN_DETEKTOR)) {
            lines.add(Component.translatable("message.riftverse.modul.tarn.an").withStyle(ChatFormatting.AQUA));
        }
        if (Ruestung.setBonus(p)) lines.add(Component.translatable("hud.riftverse.resonanz").withStyle(ChatFormatting.LIGHT_PURPLE));

        if (MultiversalArmorItem.has(helm, Modul.ANALYSE_HUD)) {
            long t = mc.level.getGameTime();
            if (t / 20 != scanTick) {
                scanTick = t / 20;
                erze = scanne(mc, p);
            }
            lines.addAll(erze);
            if (mc.crosshairPickEntity instanceof LivingEntity e) {
                lines.add(Component.empty().append(e.getDisplayName()).append(String.format(Locale.GERMAN, ": %.0f / %.0f ♥, %d Rüstung",
                        e.getHealth(), e.getMaxHealth(), e.getArmorValue())).withStyle(ChatFormatting.YELLOW));
            }
        }

        int y = 4;
        for (Component line : lines) {
            g.drawString(mc.font, line, 4, y, 0xFFFFFF, true);
            y += 10;
        }
    }

    /** Die drei nächsten Erzsorten im Umkreis, mit Anzahl und Entfernung. */
    private static List<Component> scanne(Minecraft mc, Player p) {
        Map<Block, int[]> found = new LinkedHashMap<>();
        BlockPos c = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-ERZ_RADIUS, -ERZ_RADIUS, -ERZ_RADIUS), c.offset(ERZ_RADIUS, ERZ_RADIUS, ERZ_RADIUS))) {
            var state = mc.level.getBlockState(pos);
            if (!state.is(Tags.Blocks.ORES)) continue;
            int d = (int) Math.round(Math.sqrt(pos.distSqr(c)));
            int[] e = found.computeIfAbsent(state.getBlock(), b -> new int[]{0, Integer.MAX_VALUE});
            e[0]++;
            e[1] = Math.min(e[1], d);
        }
        List<Map.Entry<Block, int[]>> list = new ArrayList<>(found.entrySet());
        list.sort((a, b) -> Integer.compare(a.getValue()[1], b.getValue()[1]));
        List<Component> out = new ArrayList<>();
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            var en = list.get(i);
            out.add(Component.translatable("hud.riftverse.erz", en.getKey().getName(), en.getValue()[0], en.getValue()[1])
                    .withStyle(ChatFormatting.GOLD));
        }
        if (out.isEmpty()) out.add(Component.translatable("hud.riftverse.keine_erze").withStyle(ChatFormatting.DARK_GRAY));
        return out;
    }

    private static String kilo(int fe) {
        return fe >= 1_000_000 ? String.format(Locale.GERMAN, "%.1f Mio.", fe / 1_000_000.0)
                : String.format(Locale.GERMAN, "%,d", fe);
    }

    private RuestungHud() {}
}
