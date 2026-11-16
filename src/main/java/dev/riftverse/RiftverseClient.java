package dev.riftverse;

import dev.riftverse.client.handbuch.HandbuchClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Client-Einstieg von Riftverse: alles, was nur auf dem Client gebaut wird, bevor die Ressourcen laden.
 * Die übrigen Client-Registrierungen laufen über {@code client/ClientSetup}.
 */
@Mod(value = Riftverse.MODID, dist = Dist.CLIENT)
public final class RiftverseClient {
    public RiftverseClient(IEventBus modBus, ModContainer container) {
        HandbuchClient.erstellen();
    }
}
