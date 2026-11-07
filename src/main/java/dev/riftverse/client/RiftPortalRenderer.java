package dev.riftverse.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.riftverse.Riftverse;
import dev.riftverse.portal.RiftPortalEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Zeichnet das Portal als zwei gegenläufig rotierende, leuchtende Strudel. */
public class RiftPortalRenderer extends EntityRenderer<RiftPortalEntity> {
    private static final ResourceLocation TEXTURE = Riftverse.id("textures/entity/rift_portal.png");

    public RiftPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public void render(RiftPortalEntity portal, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        float open = portal.openFactor(partialTick);
        if (open <= 0.001f) return;
        float time = portal.tickCount + partialTick;
        float[] c = portal.color();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));

        pose.pushPose();
        pose.translate(0, 1.2, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-portal.getYRot()));

        // äußerer Strudel
        pose.pushPose();
        pose.scale(1.0f * open, 1.35f * open, 1f);
        pose.mulPose(Axis.ZP.rotationDegrees(time * 6f));
        quad(pose, vc, c, 0.95f, 1.0f);
        pose.popPose();

        // innerer, gegenläufiger Strudel (leicht versetzt gegen Z-Fighting)
        pose.pushPose();
        pose.translate(0, 0, 0.01);
        pose.scale(0.75f * open, 1.05f * open, 1f);
        pose.mulPose(Axis.ZP.rotationDegrees(-time * 9f));
        quad(pose, vc, new float[]{Math.min(1f, c[0] + 0.35f), Math.min(1f, c[1] + 0.35f), Math.min(1f, c[2] + 0.35f)}, 0.85f, 1.0f);
        pose.popPose();

        pose.popPose();
        super.render(portal, entityYaw, partialTick, pose, buffers, packedLight);
    }

    private static void quad(PoseStack pose, VertexConsumer vc, float[] c, float alpha, float size) {
        PoseStack.Pose p = pose.last();
        // Vorderseite
        vertex(vc, p, -size, -size, 0, 1, c, alpha, 1);
        vertex(vc, p, size, -size, 1, 1, c, alpha, 1);
        vertex(vc, p, size, size, 1, 0, c, alpha, 1);
        vertex(vc, p, -size, size, 0, 0, c, alpha, 1);
        // Rückseite
        vertex(vc, p, -size, size, 0, 0, c, alpha, -1);
        vertex(vc, p, size, size, 1, 0, c, alpha, -1);
        vertex(vc, p, size, -size, 1, 1, c, alpha, -1);
        vertex(vc, p, -size, -size, 0, 1, c, alpha, -1);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float u, float v,
                               float[] c, float alpha, float normalZ) {
        vc.addVertex(p, x, y, 0f)
                .setColor(c[0], c[1], c[2], alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(p, 0f, 0f, normalZ);
    }

    @Override
    public ResourceLocation getTextureLocation(RiftPortalEntity portal) {
        return TEXTURE;
    }
}
