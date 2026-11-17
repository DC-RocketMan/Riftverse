package dev.riftverse.client.story;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riftverse.Riftverse;
import dev.riftverse.storyline.StoryNetz;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Client-Seite des Story-Gerüsts: der Dialog, der über einer Figur schwebt (gezeichnet von {@link StoryFigurRenderer}),
 * Antworten mit den Tasten 1–4 (nur solange man die Figur ansieht), die Ziel-Anzeige oben rechts und das Rift-Tagebuch.
 */
public final class ClientStory {
    /** Ziel-Anzeige ein- und ausblenden (Standard J). */
    public static final KeyMapping ZIEL_ANZEIGE = new KeyMapping("key.riftverse.ziel_anzeige", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.riftverse");

    /** Der Dialog, der gerade über einer Figur steht. */
    public record Anzeige(int entity, String sprecher, String text, List<String> antworten, boolean mehr, long bis) {}

    @Nullable
    private static Anzeige anzeige;
    private static String zielStory = "";
    private static String ziel = "";
    private static boolean zielSichtbar = true;

    private ClientStory() {
    }

    // ------------------------------------------------------------------ Pakete

    public static void dialog(StoryNetz.Dialog p) {
        Minecraft mc = Minecraft.getInstance();
        if (p.entity() < 0) {
            anzeige = null;
            return;
        }
        long bis = p.dauer() > 0 && mc.level != null ? mc.level.getGameTime() + p.dauer() : Long.MAX_VALUE;
        anzeige = new Anzeige(p.entity(), p.sprecher(), p.text(), p.antworten(), p.mehr(), bis);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.2f, 0.5f));
    }

    public static void ziel(StoryNetz.Ziel p) {
        boolean neu = !p.ziel().isEmpty() && !p.ziel().equals(ziel);
        zielStory = p.storyline();
        ziel = p.ziel();
        if (neu) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 0.7f));
    }

    public static void tagebuch(StoryNetz.Tagebuch p) {
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(p.seiten())));
    }

    /** Der Dialog über dieser Figur, falls einer läuft. */
    @Nullable
    public static Anzeige fuer(Entity entity) {
        Anzeige a = anzeige;
        return a != null && a.entity() == entity.getId() ? a : null;
    }

    // ------------------------------------------------------------------ Eingabe

    @EventBusSubscriber(modid = Riftverse.MODID, value = Dist.CLIENT)
    public static final class Spiel {
        private Spiel() {
        }

        /** Vor Minecrafts eigener Tastenauswertung: 1–4 beantworten, solange man die Figur ansieht. */
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            Anzeige a = anzeige;
            if (a == null || mc.level == null || mc.player == null) return;
            if (mc.level.getGameTime() > a.bis() || mc.level.getEntity(a.entity()) == null) {
                anzeige = null;
                return;
            }
            if (mc.screen != null || a.antworten().isEmpty() || !siehtAn(mc, a.entity())) return;
            for (int i = 0; i < Math.min(4, a.antworten().size()); i++) {
                boolean gedrueckt = false;
                while (mc.options.keyHotbarSlots[i].consumeClick()) gedrueckt = true;
                if (gedrueckt) {
                    PacketDistributor.sendToServer(new StoryNetz.Antwort(i));
                    return;
                }
            }
        }

        @SubscribeEvent
        public static void onTickPost(ClientTickEvent.Post event) {
            while (ZIEL_ANZEIGE.consumeClick()) zielSichtbar = !zielSichtbar;
        }

        private static boolean siehtAn(Minecraft mc, int entity) {
            HitResult hit = mc.hitResult;
            return hit instanceof EntityHitResult e && e.getEntity().getId() == entity;
        }
    }

    // ------------------------------------------------------------------ Registrierung und Ziel-Anzeige

    @EventBusSubscriber(modid = Riftverse.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Setup {
        private Setup() {
        }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) {
            event.register(ZIEL_ANZEIGE);
        }

        @SubscribeEvent
        public static void layers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(Riftverse.id("story_ziel"), ClientStory::zielZeichnen);
        }
    }

    private static void zielZeichnen(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!zielSichtbar || ziel.isEmpty() || mc.player == null || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) return;
        int breite = 150;
        int rechts = g.guiWidth() - 6;
        List<FormattedCharSequence> zeilen = mc.font.split(Component.literal(ziel), breite);
        Component kopf = Component.literal(zielStory).withStyle(ChatFormatting.GOLD);
        int w = Math.max(mc.font.width(kopf), zeilen.stream().mapToInt(mc.font::width).max().orElse(0));
        int x = rechts - w;
        int y = 6;
        g.fill(x - 4, y - 3, rechts + 3, y + 11 + zeilen.size() * 10, 0x80000000);
        g.drawString(mc.font, kopf, x, y, 0xFFFFFF, true);
        y += 12;
        for (FormattedCharSequence z : zeilen) {
            g.drawString(mc.font, z, x, y, 0xFFFFFF, true);
            y += 10;
        }
    }
}
