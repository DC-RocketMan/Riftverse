package dev.riftverse.client;

import dev.riftverse.Riftverse;
import dev.riftverse.boss.RiftBossEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Humanoides Modell mit vier prozedural erzeugten Alien-Skins. */
public class RiftBossRenderer extends HumanoidMobRenderer<RiftBossEntity, HumanoidModel<RiftBossEntity>> {
    private static final ResourceLocation[] SKINS = {
            Riftverse.id("textures/entity/boss/boss_0.png"),
            Riftverse.id("textures/entity/boss/boss_1.png"),
            Riftverse.id("textures/entity/boss/boss_2.png"),
            Riftverse.id("textures/entity/boss/boss_3.png")
    };

    public RiftBossRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.6f);
    }

    @Override
    public ResourceLocation getTextureLocation(RiftBossEntity boss) {
        return SKINS[Math.floorMod(boss.getSkin(), SKINS.length)];
    }
}
