package dev.riftverse.client.dorf;

import dev.riftverse.Riftverse;
import dev.riftverse.dorf.Glimmhase;
import dev.riftverse.dorf.Pusteling;
import dev.riftverse.dorf.Schimmerfalter;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Modelle der Tiere von Bridge's Inn: Glimmhase, Pusteling und Schimmerfalter. */
public final class TierModelle {
    public static final ModelLayerLocation GLIMMHASE = new ModelLayerLocation(Riftverse.id("glimmhase"), "main");
    public static final ModelLayerLocation PUSTELING = new ModelLayerLocation(Riftverse.id("pusteling"), "main");
    public static final ModelLayerLocation SCHIMMERFALTER = new ModelLayerLocation(Riftverse.id("schimmerfalter"), "main");

    private TierModelle() {
    }

    // ------------------------------------------------------------------ Glimmhase

    public static class GlimmhaseModel extends HierarchicalModel<Glimmhase> {
        private final ModelPart root, kopf, ohrL, ohrR, beinVL, beinVR, beinHL, beinHR, schwanz;

        public GlimmhaseModel(ModelPart root) {
            this.root = root;
            this.kopf = root.getChild("kopf");
            this.ohrL = kopf.getChild("ohr_l");
            this.ohrR = kopf.getChild("ohr_r");
            this.beinVL = root.getChild("bein_vl");
            this.beinVR = root.getChild("bein_vr");
            this.beinHL = root.getChild("bein_hl");
            this.beinHR = root.getChild("bein_hr");
            this.schwanz = root.getChild("schwanz");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition r = mesh.getRoot();
            r.addOrReplaceChild("koerper", CubeListBuilder.create().texOffs(0, 0).addBox(-3f, -2.5f, -4f, 6, 5, 8), PartPose.offset(0f, 18.5f, 0f));
            PartDefinition kopf = r.addOrReplaceChild("kopf", CubeListBuilder.create().texOffs(28, 0).addBox(-2.5f, -4f, -4.5f, 5, 5, 5),
                    PartPose.offset(0f, 17.5f, -3.5f));
            kopf.addOrReplaceChild("ohr_l", CubeListBuilder.create().texOffs(48, 0).addBox(-0.5f, -6f, -1f, 1, 6, 2),
                    PartPose.offsetAndRotation(-1.5f, -4f, -2f, -0.15f, 0f, -0.15f));
            kopf.addOrReplaceChild("ohr_r", CubeListBuilder.create().texOffs(54, 0).addBox(-0.5f, -6f, -1f, 1, 6, 2),
                    PartPose.offsetAndRotation(1.5f, -4f, -2f, -0.15f, 0f, 0.15f));
            CubeListBuilder bein = CubeListBuilder.create().texOffs(0, 13).addBox(-1f, 0f, -1f, 2, 3, 2);
            r.addOrReplaceChild("bein_vl", bein, PartPose.offset(-2f, 21f, -2.5f));
            r.addOrReplaceChild("bein_vr", bein, PartPose.offset(2f, 21f, -2.5f));
            r.addOrReplaceChild("bein_hl", bein, PartPose.offset(-2f, 21f, 2.5f));
            r.addOrReplaceChild("bein_hr", bein, PartPose.offset(2f, 21f, 2.5f));
            r.addOrReplaceChild("schwanz", CubeListBuilder.create().texOffs(8, 13).addBox(-1.5f, -1.5f, 0f, 3, 3, 3), PartPose.offset(0f, 17.5f, 3.5f));
            return LayerDefinition.create(mesh, 64, 32);
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(Glimmhase e, float limbSwing, float amount, float age, float yaw, float pitch) {
            kopf.yRot = yaw * Mth.DEG_TO_RAD;
            kopf.xRot = pitch * Mth.DEG_TO_RAD;
            float s = Mth.cos(limbSwing * 0.8f) * 1.2f * amount;
            beinVL.xRot = s;
            beinVR.xRot = s;
            beinHL.xRot = -s;
            beinHR.xRot = -s;
            float w = Mth.sin(age * 0.15f) * 0.08f;
            ohrL.zRot = -0.15f + w;
            ohrR.zRot = 0.15f - w;
            schwanz.yRot = Mth.sin(age * 0.3f) * 0.2f;
        }
    }

    // ------------------------------------------------------------------ Pusteling

    public static class PustelingModel extends HierarchicalModel<Pusteling> {
        private final ModelPart root, koerper;

        public PustelingModel(ModelPart root) {
            this.root = root;
            this.koerper = root.getChild("koerper");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition k = mesh.getRoot().addOrReplaceChild("koerper",
                    CubeListBuilder.create().texOffs(0, 0).addBox(-4f, -8f, -4f, 8, 8, 8), PartPose.offset(0f, 24f, 0f));
            k.addOrReplaceChild("tuff", CubeListBuilder.create().texOffs(32, 0).addBox(-2f, -11f, -2f, 4, 3, 4),
                    PartPose.rotation(0.1f, 0f, 0.08f));
            return LayerDefinition.create(mesh, 64, 32);
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(Pusteling e, float limbSwing, float amount, float age, float yaw, float pitch) {
            koerper.yRot = yaw * Mth.DEG_TO_RAD * 0.5f;
            float q = e.onGround() ? Mth.sin(limbSwing * 0.7f) * 0.12f * amount + Mth.sin(age * 0.1f) * 0.03f : -0.1f;
            koerper.yScale = 1f + q;
            koerper.xScale = 1f - q * 0.6f;
            koerper.zScale = 1f - q * 0.6f;
        }
    }

    // ------------------------------------------------------------------ Schimmerfalter

    public static class SchimmerfalterModel extends HierarchicalModel<Schimmerfalter> {
        private final ModelPart root, links, rechts;

        public SchimmerfalterModel(ModelPart root) {
            this.root = root;
            ModelPart k = root.getChild("koerper");
            this.links = k.getChild("fluegel_l");
            this.rechts = k.getChild("fluegel_r");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition k = mesh.getRoot().addOrReplaceChild("koerper",
                    CubeListBuilder.create().texOffs(0, 0).addBox(-0.5f, -0.5f, -2.5f, 1, 1, 5), PartPose.offset(0f, 20f, 0f));
            k.addOrReplaceChild("fluegel_l", CubeListBuilder.create().texOffs(0, 8).addBox(0f, 0f, -3f, 7, 0, 6), PartPose.offset(0.5f, -0.5f, 0f));
            k.addOrReplaceChild("fluegel_r", CubeListBuilder.create().texOffs(0, 16).addBox(-7f, 0f, -3f, 7, 0, 6), PartPose.offset(-0.5f, -0.5f, 0f));
            return LayerDefinition.create(mesh, 32, 32);
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(Schimmerfalter e, float limbSwing, float amount, float age, float yaw, float pitch) {
            float f = Mth.sin(age * 1.1f) * 0.9f + 0.2f;
            links.zRot = -f;
            rechts.zRot = f;
        }
    }
}
