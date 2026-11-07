package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** Schadenstypen liegen als JSON unter data/riftverse/damage_type/. */
public final class ModDamageTypes {
    /** Schlag an einer blanken Leitung unter Strom. */
    public static final ResourceKey<DamageType> STROMSCHLAG = ResourceKey.create(Registries.DAMAGE_TYPE, Riftverse.id("stromschlag"));
    /** Säureseen und Säureregen der toxischen Welt. */
    public static final ResourceKey<DamageType> SAEURE = ResourceKey.create(Registries.DAMAGE_TYPE, Riftverse.id("saeure"));

    public static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }

    private ModDamageTypes() {}
}
