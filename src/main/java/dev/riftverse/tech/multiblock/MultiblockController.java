package dev.riftverse.tech.multiblock;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** Steuerblock eines Multiblocks: meldet beim Rechtsklick, welches Teil noch fehlt. */
public interface MultiblockController {
    /** Prüft den Aufbau sofort. @return Meldung über das fehlende Teil, oder null, wenn alles steht */
    @Nullable
    Component checkStructure();
}
