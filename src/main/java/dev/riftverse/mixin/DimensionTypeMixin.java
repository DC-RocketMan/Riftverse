package dev.riftverse.mixin;

import dev.riftverse.universe.SpiegelZeit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Spiegelwelt: Tag und Nacht sind vertauscht. Alle Dimensionen teilen sich die Tageszeit der Overworld, deshalb
 * verschiebt Riftverse die Zeit für den Dimensionstyp der Spiegelwelt um einen halben Tag – auf Server und Client,
 * damit Himmel, Licht und Mob-Spawns zusammenpassen.
 */
@Mixin(DimensionType.class)
public abstract class DimensionTypeMixin {
    @Shadow
    public abstract ResourceLocation effectsLocation();

    @ModifyVariable(method = "timeOfDay", at = @At("HEAD"), argsOnly = true)
    private long riftverse$spiegelzeit(long dayTime) {
        return SpiegelZeit.verschieben(effectsLocation(), dayTime);
    }
}
