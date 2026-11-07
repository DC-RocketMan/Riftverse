package dev.riftverse.client.screen;

import dev.riftverse.energy.VoltageTier;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

final class EnergyTooltips {
    static List<Component> energy(int stored, int capacity, VoltageTier tier) {
        return List.of(
                Component.translatable("tooltip.riftverse.energy", fmt(stored), fmt(capacity)),
                tier.describe());
    }

    static String fmt(int value) {
        return String.format(Locale.GERMANY, "%,d", value);
    }

    private EnergyTooltips() {}
}
