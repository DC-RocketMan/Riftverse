package dev.riftverse.client.dorf;

import dev.riftverse.Riftverse;
import dev.riftverse.dorf.Gleeb;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Gleeb: kleiner Humanoide mit großem, schiefem Zaubererhut; auf Wunsch mit Besen unter sich. */
public class GleebModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Riftverse.id("gleeb"), "main");

    private final ModelPart besen;

    public GleebModel(ModelPart root) {
        super(root);
        this.besen = root.getChild("body").getChild("besen");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("krempe", CubeListBuilder.create().texOffs(0, 32).addBox(-6f, -9f, -6f, 12, 1, 12), PartPose.ZERO);
        head.addOrReplaceChild("hut1", CubeListBuilder.create().texOffs(0, 45).addBox(-3.5f, -13f, -3.5f, 7, 4, 7), PartPose.ZERO);
        head.addOrReplaceChild("hut2", CubeListBuilder.create().texOffs(28, 45).addBox(-2.5f, -16f, -2f, 5, 3, 5),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.12f, 0f, 0.05f));
        head.addOrReplaceChild("hut3", CubeListBuilder.create().texOffs(48, 45).addBox(-1.5f, -18.5f, -0.5f, 3, 3, 3),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.28f, 0f, 0.1f));
        head.addOrReplaceChild("spitze", CubeListBuilder.create().texOffs(48, 52).addBox(-0.5f, -20f, 1.5f, 1, 2, 1),
                PartPose.offsetAndRotation(0f, 0f, 0f, -0.4f, 0f, 0.12f));
        root.getChild("body").addOrReplaceChild("besen", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-0.5f, 11.5f, -8f, 1, 1, 14)
                .texOffs(48, 32).addBox(-1.5f, 10.5f, 6f, 3, 3, 4), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        boolean aufBesen = entity instanceof Gleeb g && g.besen();
        boolean zaubert = entity instanceof Gleeb g && g.zaubert();
        riding = aufBesen;
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        hat.visible = false;
        besen.visible = aufBesen;
        if (aufBesen) {
            rightArm.xRot = -0.9f;
            leftArm.xRot = -0.9f;
            rightArm.zRot = 0.1f;
            leftArm.zRot = -0.1f;
            body.xRot = 0.15f + Mth.sin(ageInTicks * 0.1f) * 0.05f;
        } else if (zaubert) {
            float w = Mth.sin(ageInTicks * 0.6f) * 0.25f;
            rightArm.xRot = -2.7f + w;
            leftArm.xRot = -2.7f - w;
            rightArm.zRot = 0.35f;
            leftArm.zRot = -0.35f;
        }
    }
}
