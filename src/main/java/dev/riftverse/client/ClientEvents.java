package dev.riftverse.client;

import dev.riftverse.Riftverse;
import dev.riftverse.network.RueckkehrPayload;
import dev.riftverse.network.ZielPayload;
import dev.riftverse.universe.UniverseType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Clientseitige Ereignisse im Spiel: Rückkehr-Taste und der dauerhafte Nebel der Schrottwelt. */
@EventBusSubscriber(modid = Riftverse.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ClientSetup.RUECKKEHR.consumeClick()) {
            if (mc.player != null && mc.screen == null) PacketDistributor.sendToServer(new RueckkehrPayload());
        }
        while (ClientSetup.ZIEL.consumeClick()) {
            if (mc.player != null && mc.screen == null) PacketDistributor.sendToServer(new ZielPayload(mc.player.isShiftKeyDown()));
        }
        // Multiversal-Rüstung
        ruestungTaste(mc, ClientSetup.TARN, dev.riftverse.ruestung.Ruestung.AKTION_TARN);
        ruestungTaste(mc, ClientSetup.KANONE, dev.riftverse.ruestung.Ruestung.AKTION_KANONE);
        ruestungTaste(mc, ClientSetup.GADGET_1, dev.riftverse.ruestung.Ruestung.AKTION_GADGET_1);
        ruestungTaste(mc, ClientSetup.GADGET_2, dev.riftverse.ruestung.Ruestung.AKTION_GADGET_2);
        ruestungTaste(mc, ClientSetup.GRAVITATION, dev.riftverse.ruestung.Ruestung.AKTION_GRAVITATION);
        ruestungTaste(mc, ClientSetup.SCHRUMPF, dev.riftverse.ruestung.Ruestung.AKTION_SCHRUMPF);
        phasenschritt(mc);
    }

    private static void ruestungTaste(Minecraft mc, net.minecraft.client.KeyMapping key, int aktion) {
        while (key.consumeClick()) {
            if (mc.player != null && mc.screen == null) PacketDistributor.sendToServer(new dev.riftverse.network.RuestungPayload(aktion));
        }
    }

    private static boolean sprungGedrueckt;
    private static int luftTicks;

    /** Phasenschritt: In der Luft noch einmal Springen drücken. */
    private static void phasenschritt(Minecraft mc) {
        if (mc.player == null || mc.screen != null) return;
        boolean down = mc.options.keyJump.isDown();
        luftTicks = mc.player.onGround() ? 0 : luftTicks + 1;
        if (down && !sprungGedrueckt && luftTicks > 2 && !mc.player.isInWater() && !mc.player.getAbilities().flying
                && dev.riftverse.ruestung.Ruestung.hat(mc.player, dev.riftverse.ruestung.Modul.PHASENSCHRITT)) {
            PacketDistributor.sendToServer(new dev.riftverse.network.RuestungPayload(dev.riftverse.ruestung.Ruestung.AKTION_PHASE));
        }
        sprungGedrueckt = down;
    }

    /** Schrottwelt: dauerhafter Nebel. Toxische und organische Welt: leichter Dunst. */
    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || event.getType() != FogType.NONE) return;
        UniverseType type = UniverseType.of(mc.level.dimension());
        float far;
        if (type == UniverseType.SCHROTT) far = 48.0f;
        else if (type == UniverseType.TOXISCH) far = 96.0f;
        else if (type == UniverseType.ORGANISCH) far = 80.0f;
        else return;
        if (event.getFarPlaneDistance() <= far) return;
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * 0.1f));
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }

    private ClientEvents() {}
}
