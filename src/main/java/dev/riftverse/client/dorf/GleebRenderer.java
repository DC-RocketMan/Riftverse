package dev.riftverse.client.dorf;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftverse.Riftverse;
import dev.riftverse.dorf.Beruf;
import dev.riftverse.dorf.Gleeb;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/** Zeichnet Gleebs – und Story-Figuren mit dem Aussehen {@code gleeb:<beruf>} (siehe {@code StoryFigurRenderer}). */
public class GleebRenderer<T extends Mob> extends MobRenderer<T, GleebModel<T>> {
    private static final Map<Beruf, ResourceLocation> TEXTUREN = new EnumMap<>(Beruf.class);

    static {
        for (Beruf b : Beruf.values()) TEXTUREN.put(b, Riftverse.id("textures/entity/gleeb/" + b.id + ".png"));
    }

    private final Function<T, Beruf> beruf;

    public GleebRenderer(EntityRendererProvider.Context context, Function<T, Beruf> beruf) {
        super(context, new GleebModel<>(context.bakeLayer(GleebModel.LAYER)), 0.3f);
        this.beruf = beruf;
    }

    public static GleebRenderer<Gleeb> gleebs(EntityRendererProvider.Context context) {
        return new GleebRenderer<>(context, Gleeb::beruf);
    }

    public static ResourceLocation textur(Beruf b) {
        return TEXTUREN.get(b);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTUREN.get(beruf.apply(entity));
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        pose.scale(0.62f, 0.62f, 0.62f);
    }
}
