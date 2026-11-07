package dev.riftverse.client;

import dev.riftverse.Riftverse;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;

/** Schrott-Golem: Modell des Eisengolems, Textur aus Rost, Blech und einem grünen Auge. Die Größe kommt vom Scale-Attribut. */
public class SchrottGolemRenderer extends IronGolemRenderer {
    private static final ResourceLocation TEXTURE = Riftverse.id("textures/entity/schrott_golem.png");

    public SchrottGolemRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(IronGolem entity) {
        return TEXTURE;
    }
}
