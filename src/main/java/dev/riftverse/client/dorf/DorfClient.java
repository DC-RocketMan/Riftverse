package dev.riftverse.client.dorf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.riftverse.Riftverse;
import dev.riftverse.dorf.Glimmhase;
import dev.riftverse.dorf.Irrlicht;
import dev.riftverse.dorf.Pusteling;
import dev.riftverse.dorf.Schimmerfalter;
import dev.riftverse.registry.ModEntities;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;

/** Client-Seite von Bridge's Inn: Modell-Ebenen und Renderer für Gleebs, Tiere und Irrlichter. */
@EventBusSubscriber(modid = Riftverse.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DorfClient {
    private DorfClient() {
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GleebModel.LAYER, GleebModel::createLayer);
        event.registerLayerDefinition(TierModelle.GLIMMHASE, TierModelle.GlimmhaseModel::createLayer);
        event.registerLayerDefinition(TierModelle.PUSTELING, TierModelle.PustelingModel::createLayer);
        event.registerLayerDefinition(TierModelle.SCHIMMERFALTER, TierModelle.SchimmerfalterModel::createLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GLEEB.get(), GleebRenderer::gleebs);
        event.registerEntityRenderer(ModEntities.GLIMMHASE.get(), GlimmhaseRenderer::new);
        event.registerEntityRenderer(ModEntities.PUSTELING.get(), PustelingRenderer::new);
        event.registerEntityRenderer(ModEntities.SCHIMMERFALTER.get(), SchimmerfalterRenderer::new);
        event.registerEntityRenderer(ModEntities.IRRLICHT.get(), IrrlichtRenderer::new);
    }

    private static ResourceLocation tier(String name) {
        return Riftverse.id("textures/entity/tiere/" + name + ".png");
    }

    /** Leuchtende Teile (Ohrspitzen, Flügelmuster) – voll hell wie Augen. */
    private static <T extends Entity, M extends EntityModel<T>> EyesLayer<T, M> leuchten(RenderLayerParent<T, M> parent, String textur) {
        RenderType typ = RenderType.eyes(tier(textur));
        return new EyesLayer<>(parent) {
            @Override
            public RenderType renderType() {
                return typ;
            }
        };
    }

    public static class GlimmhaseRenderer extends MobRenderer<Glimmhase, TierModelle.GlimmhaseModel> {
        public GlimmhaseRenderer(EntityRendererProvider.Context c) {
            super(c, new TierModelle.GlimmhaseModel(c.bakeLayer(TierModelle.GLIMMHASE)), 0.3f);
            addLayer(leuchten(this, "glimmhase_leuchten"));
        }

        @Override
        public ResourceLocation getTextureLocation(Glimmhase e) {
            return tier("glimmhase");
        }

        @Override
        protected void scale(Glimmhase e, PoseStack pose, float partial) {
            if (e.isBaby()) pose.scale(0.6f, 0.6f, 0.6f);
        }
    }

    public static class PustelingRenderer extends MobRenderer<Pusteling, TierModelle.PustelingModel> {
        public PustelingRenderer(EntityRendererProvider.Context c) {
            super(c, new TierModelle.PustelingModel(c.bakeLayer(TierModelle.PUSTELING)), 0.35f);
        }

        @Override
        public ResourceLocation getTextureLocation(Pusteling e) {
            return tier("pusteling");
        }

        @Override
        protected void scale(Pusteling e, PoseStack pose, float partial) {
            if (e.isBaby()) pose.scale(0.55f, 0.55f, 0.55f);
        }
    }

    public static class SchimmerfalterRenderer extends MobRenderer<Schimmerfalter, TierModelle.SchimmerfalterModel> {
        public SchimmerfalterRenderer(EntityRendererProvider.Context c) {
            super(c, new TierModelle.SchimmerfalterModel(c.bakeLayer(TierModelle.SCHIMMERFALTER)), 0.15f);
            addLayer(leuchten(this, "schimmerfalter_leuchten"));
        }

        @Override
        public ResourceLocation getTextureLocation(Schimmerfalter e) {
            return tier("schimmerfalter");
        }
    }

    /** Irrlicht: pulsierende, immer helle Lichtkugel, die zur Kamera zeigt. */
    public static class IrrlichtRenderer extends EntityRenderer<Irrlicht> {
        private static final ResourceLocation TEXTUR = tier("irrlicht");
        private static final RenderType TYP = RenderType.entityTranslucentEmissive(TEXTUR);

        public IrrlichtRenderer(EntityRendererProvider.Context c) {
            super(c);
        }

        @Override
        public void render(Irrlicht e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            pose.pushPose();
            pose.translate(0, 0.2, 0);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            pose.mulPose(Axis.ZP.rotationDegrees((e.tickCount + partial) * 4f));
            float s = 0.7f + Mth.sin((e.tickCount + partial) * 0.2f) * 0.12f;
            pose.scale(s, s, s);
            Matrix4f m = pose.last().pose();
            VertexConsumer vc = buffers.getBuffer(TYP);
            punkt(vc, m, pose, -0.5f, -0.5f, 0, 1);
            punkt(vc, m, pose, 0.5f, -0.5f, 1, 1);
            punkt(vc, m, pose, 0.5f, 0.5f, 1, 0);
            punkt(vc, m, pose, -0.5f, 0.5f, 0, 0);
            pose.popPose();
            super.render(e, yaw, partial, pose, buffers, light);
        }

        private static void punkt(VertexConsumer vc, Matrix4f m, PoseStack pose, float x, float y, float u, float v) {
            vc.addVertex(m, x, y, 0).setColor(255, 255, 255, 230).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 1, 0);
        }

        @Override
        public ResourceLocation getTextureLocation(Irrlicht e) {
            return TEXTUR;
        }
    }
}
