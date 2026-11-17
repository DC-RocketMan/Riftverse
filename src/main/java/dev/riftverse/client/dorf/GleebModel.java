package dev.riftverse.client.dorf;

import dev.riftverse.Riftverse;
import dev.riftverse.dorf.Gleeb;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Gleeb: kleiner, gedrungener Zwergen-Elf mit großem Kopf, Knollennase, Spitzohren und Bart (je nach Beruf, über die
 * Textur), dazu ein weicher Hut mit Schnallenband, dessen Zipfel sich nach hinten einrollt – halb Kobold, halb
 * Maschinenelf. Gleiche Teile für alle Berufe; die Kleidung steckt in der Textur (64 × 64).
 */
public class GleebModel<T extends Mob> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Riftverse.id("gleeb"), "main");

    private final ModelPart root, kopf, koerper, armR, armL, beinR, beinL, besen, zipfel;

    public GleebModel(ModelPart root) {
        this.root = root;
        this.kopf = root.getChild("kopf");
        this.koerper = root.getChild("koerper");
        this.armR = root.getChild("arm_r");
        this.armL = root.getChild("arm_l");
        this.beinR = root.getChild("bein_r");
        this.beinL = root.getChild("bein_l");
        this.besen = koerper.getChild("besen");
        this.zipfel = kopf.getChild("krone").getChild("zipfel1");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        PartDefinition kopf = r.addOrReplaceChild("kopf", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4f, -8f, -3.5f, 8, 8, 7)
                .texOffs(30, 0).addBox(-1f, -4f, -4.5f, 2, 2, 1)       // Knollennase
                .texOffs(30, 5).addBox(-3f, -3f, -4.3f, 6, 3, 1),      // Bart (durchsichtig, wenn keiner)
                PartPose.offset(0f, 13f, 0f));
        kopf.addOrReplaceChild("ohr_r", CubeListBuilder.create().texOffs(36, 0).addBox(-1f, -2f, -1f, 1, 3, 2),
                PartPose.offsetAndRotation(-4f, -4f, 0f, 0f, -0.35f, 0.45f));
        kopf.addOrReplaceChild("ohr_l", CubeListBuilder.create().texOffs(42, 0).addBox(0f, -2f, -1f, 1, 3, 2),
                PartPose.offsetAndRotation(4f, -4f, 0f, 0f, 0.35f, -0.45f));
        // Hut: breite Krempe, niedrige Krone mit Schnallenband, weicher Zipfel, der sich nach hinten einrollt
        kopf.addOrReplaceChild("krempe", CubeListBuilder.create().texOffs(0, 27).addBox(-5f, -9f, -4.5f, 10, 1, 9), PartPose.ZERO);
        PartDefinition krone = kopf.addOrReplaceChild("krone", CubeListBuilder.create().texOffs(0, 38).addBox(-3.5f, -4f, -3.5f, 7, 4, 7),
                PartPose.offset(0f, -9f, 0f));
        PartDefinition z1 = krone.addOrReplaceChild("zipfel1", CubeListBuilder.create().texOffs(28, 38).addBox(-2.5f, -2f, -2.5f, 5, 2, 5),
                PartPose.offsetAndRotation(0f, -4f, 0.8f, -0.7f, 0f, 0f));
        PartDefinition z2 = z1.addOrReplaceChild("zipfel2", CubeListBuilder.create().texOffs(48, 38).addBox(-1.5f, -2f, -1.5f, 3, 2, 3),
                PartPose.offsetAndRotation(0f, -1.5f, 1.2f, -0.95f, 0f, 0f));
        z2.addOrReplaceChild("bommel", CubeListBuilder.create().texOffs(48, 44).addBox(-1f, -1f, -1f, 2, 2, 2),
                PartPose.offsetAndRotation(0f, -1.2f, 1.6f, -0.8f, 0f, 0f));

        PartDefinition koerper = r.addOrReplaceChild("koerper", CubeListBuilder.create().texOffs(0, 16).addBox(-3.5f, 0f, -2f, 7, 6, 4),
                PartPose.offset(0f, 13f, 0f));
        koerper.addOrReplaceChild("besen", CubeListBuilder.create()
                .texOffs(0, 50).addBox(-0.5f, 5.5f, -7f, 1, 1, 12)
                .texOffs(28, 50).addBox(-1.5f, 4.5f, 5f, 3, 3, 4), PartPose.ZERO);
        r.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(22, 16).addBox(-1f, 0f, -1f, 2, 6, 2),
                PartPose.offset(-4.5f, 13.5f, 0f));
        r.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(30, 16).addBox(-1f, 0f, -1f, 2, 6, 2),
                PartPose.offset(4.5f, 13.5f, 0f));
        r.addOrReplaceChild("bein_r", CubeListBuilder.create().texOffs(38, 16).addBox(-1.5f, 0f, -1.5f, 3, 5, 3),
                PartPose.offset(-1.6f, 19f, 0f));
        r.addOrReplaceChild("bein_l", CubeListBuilder.create().texOffs(50, 16).addBox(-1.5f, 0f, -1.5f, 3, 5, 3),
                PartPose.offset(1.6f, 19f, 0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T e, float limbSwing, float amount, float age, float netHeadYaw, float headPitch) {
        root().getAllParts().forEach(ModelPart::resetPose);
        boolean aufBesen = e instanceof Gleeb g && g.besen();
        boolean zaubert = e instanceof Gleeb g && g.zaubert();
        kopf.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        kopf.xRot = headPitch * Mth.DEG_TO_RAD;
        // Watschelgang: kurze Beine, die Arme schwingen mit, der Hutzipfel wippt
        float s = Mth.cos(limbSwing * 0.9f) * 1.3f * amount;
        beinR.xRot = s;
        beinL.xRot = -s;
        armR.xRot = -s * 0.8f;
        armL.xRot = s * 0.8f;
        armR.zRot = 0.08f;
        armL.zRot = -0.08f;
        koerper.zRot = Mth.sin(limbSwing * 0.45f) * 0.06f * amount;
        zipfel.xRot += Mth.sin(age * 0.08f) * 0.06f + Mth.cos(limbSwing * 0.9f) * 0.15f * amount;
        besen.visible = aufBesen;
        if (aufBesen) {
            beinR.xRot = -1.35f;
            beinL.xRot = -1.35f;
            beinR.yRot = 0.25f;
            beinL.yRot = -0.25f;
            armR.xRot = -0.9f;
            armL.xRot = -0.9f;
            koerper.xRot = 0.12f + Mth.sin(age * 0.1f) * 0.05f;
            zipfel.xRot = -1.0f + Mth.sin(age * 0.3f) * 0.15f;   // Fahrtwind
        } else if (zaubert) {
            float w = Mth.sin(age * 0.6f) * 0.25f;
            armR.xRot = -2.6f + w;
            armL.xRot = -2.6f - w;
            armR.zRot = 0.35f;
            armL.zRot = -0.35f;
        }
    }
}
