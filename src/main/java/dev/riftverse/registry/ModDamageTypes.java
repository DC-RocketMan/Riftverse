package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/** Schadenstypen liegen als JSON unter data/riftverse/damage_type/. */
public final class ModDamageTypes {
    /** Schlag an einer blanken Leitung unter Strom. */
    public static final ResourceKey<DamageType> STROMSCHLAG = ResourceKey.create(Registries.DAMAGE_TYPE, Riftverse.id("stromschlag"));

    private ModDamageTypes() {}
}
