package github.com.gengyoubo.LP.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.ltxprogrammer.changed.entity.ChangedEntity;

public class MimicYufengWingsModel extends EntityModel<LivingEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("changede", "mimic_yufeng_wings"),
            "main"
    );
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("changede", "textures/item/mimic_yufeng_wings.png");

    private final ModelPart torso;
    private final ModelPart leftWingRoot;
    private final ModelPart leftSecondaries;
    private final ModelPart leftTertiaries;
    private final ModelPart rightWingRoot;
    private final ModelPart rightSecondaries;
    private final ModelPart rightTertiaries;

    public MimicYufengWingsModel(ModelPart root) {
        this.torso = root.getChild("Torso");

        ModelPart leftWing = this.torso.getChild("LeftWing");
        this.leftWingRoot = leftWing.getChild("leftWingRoot");
        this.leftSecondaries = this.leftWingRoot.getChild("leftSecondaries");
        this.leftTertiaries = this.leftSecondaries.getChild("leftTertiaries");

        ModelPart rightWing = this.torso.getChild("RightWing");
        this.rightWingRoot = rightWing.getChild("rightWingRoot");
        this.rightSecondaries = this.rightWingRoot.getChild("rightSecondaries");
        this.rightTertiaries = this.rightSecondaries.getChild("rightTertiaries");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition torso = root.addOrReplaceChild("Torso", CubeListBuilder.create(), PartPose.offset(0.0F, -0.5F, 0.0F));

        PartDefinition leftWing = torso.addOrReplaceChild("LeftWing", CubeListBuilder.create(), PartPose.offsetAndRotation(2.0F, 5.0F, 2.0F, 0.0F, -0.48F, 0.0F));
        PartDefinition leftWingRoot = leftWing.addOrReplaceChild("leftWingRoot", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        leftWingRoot.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(37, 0).addBox(18.975F, -4.475F, 1.65F, 7.0F, 5.0F, 0.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(-2.0F, 20.0F, -2.0F, 0.0F, 0.0F, -1.2654F));
        leftWingRoot.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(60, 47).addBox(19.075F, -12.7F, 1.2F, 6.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(-2.0F, 20.0F, -2.0F, 0.0F, 0.0F, -0.7854F));
        leftWingRoot.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(64, 43).addBox(7.775F, -19.75F, 1.2F, 5.0F, 2.0F, 1.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(-2.0F, 20.0F, -2.0F, 0.0F, 0.0F, -0.3491F));
        PartDefinition leftSecondaries = leftWingRoot.addOrReplaceChild("leftSecondaries", CubeListBuilder.create().texOffs(52, 67).addBox(-0.8F, -0.475F, -0.3F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(7.3F, -7.0F, -0.5F, 0.0F, 0.0F, -0.5236F));
        leftSecondaries.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(12, 70).addBox(-2.025F, -22.55F, 1.2F, 1.0F, 6.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(-9.3F, 27.0F, -1.5F, 0.0F, 0.0F, 0.48F));
        leftSecondaries.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(60, 0).addBox(15.525F, -13.85F, 1.648F, 9.0F, 6.0F, 0.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(-9.3F, 27.0F, -1.5F, 0.0F, 0.0F, -0.7418F));
        leftSecondaries.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(36, 57).addBox(13.4F, 10.625F, 1.651F, 9.0F, 5.0F, 0.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(-9.3F, 27.0F, -1.5F, 0.0F, 0.0F, -1.8326F));
        PartDefinition leftTertiaries = leftSecondaries.addOrReplaceChild("leftTertiaries", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.3F, 0.0F, 0.0F, 0.0F, 0.0F, -0.9599F));
        leftTertiaries.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(48, 67).addBox(-3.3F, -22.5F, 1.2F, 1.0F, 7.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(-9.0F, 27.0F, -1.5F, 0.0F, 0.0F, 0.5236F));
        leftTertiaries.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(56, 16).addBox(16.125F, -10.525F, 1.64F, 9.0F, 5.0F, 0.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(-9.0F, 27.0F, -1.5F, 0.0F, 0.0F, -0.8727F));
        leftTertiaries.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(8, 68).addBox(9.15F, -26.2F, 1.2F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.005F)), PartPose.offsetAndRotation(-9.0F, 27.0F, -1.5F, 0.0F, 0.0F, -0.0436F));

        PartDefinition rightWing = torso.addOrReplaceChild("RightWing", CubeListBuilder.create(), PartPose.offsetAndRotation(-2.0F, 5.0F, 2.0F, 0.0F, 0.48F, 0.0F));
        PartDefinition rightWingRoot = rightWing.addOrReplaceChild("rightWingRoot", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        rightWingRoot.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(60, 21).addBox(-25.975F, -4.475F, 1.65F, 7.0F, 5.0F, 0.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(2.0F, 20.0F, -2.0F, 0.0F, 0.0F, 1.2654F));
        rightWingRoot.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(32, 24).addBox(-25.075F, -12.7F, 1.2F, 6.0F, 2.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(2.0F, 20.0F, -2.0F, 0.0F, 0.0F, 0.7854F));
        rightWingRoot.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(64, 50).addBox(-12.775F, -19.75F, 1.2F, 5.0F, 2.0F, 1.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(2.0F, 20.0F, -2.0F, 0.0F, 0.0F, 0.3491F));
        PartDefinition rightSecondaries = rightWingRoot.addOrReplaceChild("rightSecondaries", CubeListBuilder.create().texOffs(0, 68).addBox(-0.2F, -0.475F, -0.3F, 1.0F, 7.0F, 1.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(-7.3F, -7.0F, -0.5F, 0.0F, 0.0F, 0.5236F));
        rightSecondaries.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(20, 70).addBox(1.025F, -22.55F, 1.2F, 1.0F, 6.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(9.3F, 27.0F, -1.5F, 0.0F, 0.0F, -0.48F));
        rightSecondaries.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(17, 56).addBox(-22.4F, 10.625F, 1.651F, 9.0F, 5.0F, 0.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(9.3F, 27.0F, -1.5F, 0.0F, 0.0F, 1.8326F));
        rightSecondaries.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(58, 37).addBox(-24.525F, -13.85F, 1.648F, 9.0F, 6.0F, 0.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(9.3F, 27.0F, -1.5F, 0.0F, 0.0F, 0.7418F));
        PartDefinition rightTertiaries = rightSecondaries.addOrReplaceChild("rightTertiaries", CubeListBuilder.create(), PartPose.offsetAndRotation(0.3F, 0.0F, 0.0F, 0.0F, 0.0F, 0.9599F));
        rightTertiaries.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(4, 68).addBox(2.3F, -22.5F, 1.2F, 1.0F, 7.0F, 1.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(9.0F, 27.0F, -1.5F, 0.0F, 0.0F, -0.5236F));
        rightTertiaries.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(16, 70).addBox(-10.15F, -26.2F, 1.2F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.005F)), PartPose.offsetAndRotation(9.0F, 27.0F, -1.5F, 0.0F, 0.0F, 0.0436F));
        rightTertiaries.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(52, 32).addBox(-25.125F, -10.525F, 1.64F, 9.0F, 5.0F, 0.0F, CubeDeformation.NONE), PartPose.offsetAndRotation(9.0F, 27.0F, -1.5F, 0.0F, 0.0F, 0.8727F));

        return LayerDefinition.create(meshDefinition, 96, 96);
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.torso.getAllParts().forEach(ModelPart::resetPose);

        boolean flying = isActivelyFlying(entity);
        applyWingPose(flying ? Mth.cos(ageInTicks * 0.7F) * 0.18F : 0.0F, flying ? 0.18F : 0.0F);
    }

    public void setupItemPose(float ageInTicks) {
        this.torso.getAllParts().forEach(ModelPart::resetPose);
        applyWingPose(Mth.cos(ageInTicks * 0.35F) * 0.08F, 0.14F);
    }

    private void applyWingPose(float flap, float spread) {
        this.leftWingRoot.yRot -= spread;
        this.leftSecondaries.zRot -= spread * 0.7F;
        this.leftTertiaries.zRot -= spread * 1.1F;
        this.rightWingRoot.yRot += spread;
        this.rightSecondaries.zRot += spread * 0.7F;
        this.rightTertiaries.zRot += spread * 1.1F;

        this.leftWingRoot.zRot -= flap;
        this.leftSecondaries.zRot -= flap * 0.85F;
        this.leftTertiaries.zRot -= flap * 0.65F;
        this.rightWingRoot.zRot += flap;
        this.rightSecondaries.zRot += flap * 0.85F;
        this.rightTertiaries.zRot += flap * 0.65F;
    }

    private static boolean isActivelyFlying(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getAbilities().flying;
        }
        if (entity instanceof ChangedEntity changedEntity) {
            Player player = changedEntity.getUnderlyingPlayer();
            return player != null && player.getAbilities().flying;
        }
        return false;
    }

    public void copyBodyPoseFrom(EntityModel<?> parentModel) {
        if (parentModel instanceof HumanoidModel<?> humanoidModel) {
            this.torso.copyFrom(humanoidModel.body);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.torso.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
