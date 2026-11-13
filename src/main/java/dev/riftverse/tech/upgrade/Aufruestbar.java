package dev.riftverse.tech.upgrade;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

/** Block-Entity, die Upgrade-Chips aufnimmt (Rechtsklick mit dem Chip, Ausbau mit dem Multitool-Schlüssel). */
public interface Aufruestbar {
    Set<UpgradeChip> chips();

    /** @return false, wenn der Chip schon drin ist oder nicht passt */
    boolean einbauen(UpgradeChip chip);

    /** Baut alle Chips aus und gibt sie als Items zurück. */
    List<ItemStack> ausbauen();
}
