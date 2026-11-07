package dev.riftverse.energy;

import net.minecraft.network.chat.Component;

/**
 * Spannungsstufen des Riftverse-Netzes. Ein Paket darf höchstens {@link #maxPacket()} FE groß sein,
 * sonst explodiert die Maschine bzw. schmilzt die Leitung.
 */
public enum VoltageTier {
    I(1, 128),
    II(2, 1_024),
    III(3, 8_192),
    IV(4, Integer.MAX_VALUE);

    private final int level;
    private final int maxPacket;

    VoltageTier(int level, int maxPacket) {
        this.level = level;
        this.maxPacket = maxPacket;
    }

    public int level() {
        return level;
    }

    /** Größtes Paket dieser Stufe in FE/t. */
    public int maxPacket() {
        return maxPacket;
    }

    /** Paketgröße, die eine Quelle dieser Stufe höchstens abgibt (Stufe IV: 32.768 FE/t zum Testen). */
    public int emitPacket() {
        return this == IV ? 32_768 : maxPacket;
    }

    public boolean exceeds(VoltageTier other) {
        return level > other.level;
    }

    public VoltageTier next() {
        VoltageTier[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static VoltageTier forPacket(int amount) {
        for (VoltageTier t : values()) {
            if (amount <= t.maxPacket) return t;
        }
        return IV;
    }

    public Component displayName() {
        return Component.translatable("tier.riftverse.voltage." + name().toLowerCase(java.util.Locale.ROOT));
    }

    /** "Stufe I (max. 128 FE/t)" */
    public Component describe() {
        return this == IV
                ? Component.translatable("tooltip.riftverse.voltage_unlimited", displayName())
                : Component.translatable("tooltip.riftverse.voltage", displayName(), String.format(java.util.Locale.GERMANY, "%,d", maxPacket));
    }
}
