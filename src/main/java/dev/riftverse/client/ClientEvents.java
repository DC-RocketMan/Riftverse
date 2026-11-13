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
