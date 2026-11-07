package dev.riftverse.tech;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Inventar beim Abbauen (oder Explodieren) fallen lassen. */
public interface DropsContents {
    void dropContents(Level level, BlockPos pos);
}
