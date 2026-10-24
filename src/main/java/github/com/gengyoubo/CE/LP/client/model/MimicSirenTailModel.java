package github.com.gengyoubo.CE.LP.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.client.renderer.model.LeglessModel;
import net.ltxprogrammer.changed.client.renderer.model.TorsoedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MimicSirenTailModel extends EntityModel<LivingEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("changede", "mimic_siren_tail"), "main");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("changed", "textures/latex_siren.png");
    private final ModelPart waist;
    private final ModelPart abdomen;
    private final ModelPart lowerAbdomen;
    private final ModelPart tail;
    private final List<ModelPart> joints;

    public MimicSirenTailModel(ModelPart root) {
        waist = root.getChild("Waist");
        abdomen = waist.getChild("Abdomen");
        lowerAbdomen = abdomen.getChild("LowerAbdomen");
        tail = lowerAbdomen.getChild("Tail");
        ModelPart primary = tail.getChild("TailPrimary");
        ModelPart secondary = primary.getChild("TailSecondary");
        ModelPart tertiary = secondary.getChild("TailTertiary");
        ModelPart quaternary = tertiary.getChild("TailQuaternary");
        joints = List.of(primary, secondary, tertiary, quaternary, quaternary.getChild("TailQuintary"));
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("Waist", CubeListBuilder.create(), PartPose.ZERO);
        // Original LatexSirenModel lower body, with the waist fitted to a player.
      PartDefinition Abdomen = root.addOrReplaceChild(
         "Abdomen",
         CubeListBuilder.create().texOffs(48, 10).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.2F)),
         PartPose.offset(0.0F, 11.8F, 0.0F)
      );
      PartDefinition LowerAbdomen = Abdomen.addOrReplaceChild(
         "LowerAbdomen",
         CubeListBuilder.create().texOffs(27, 32).addBox(-4.5F, -1.25F, -2.5F, 9.0F, 7.0F, 5.0F, new CubeDeformation(0.06F)),
         PartPose.offset(0.0F, 4.25F, 0.0F)
      );
      PartDefinition Tail = LowerAbdomen.addOrReplaceChild(
         "Tail",
         CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -0.75F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.25F)),
         PartPose.offset(0.0F, 5.5F, 0.0F)
      );
      PartDefinition TailPrimary = Tail.addOrReplaceChild(
         "TailPrimary",
         CubeListBuilder.create().texOffs(50, 27).addBox(-3.5F, -0.25F, -2.0F, 7.0F, 5.0F, 4.0F, CubeDeformation.NONE),
         PartPose.offset(0.0F, 4.5F, 0.0F)
      );
      PartDefinition TailSecondary = TailPrimary.addOrReplaceChild(
         "TailSecondary",
         CubeListBuilder.create().texOffs(16, 53).addBox(-3.0F, -0.25F, -2.0F, 6.0F, 5.0F, 4.0F, new CubeDeformation(-0.25F)),
         PartPose.offset(0.0F, 4.25F, 0.0F)
      );
      PartDefinition TailTertiary = TailSecondary.addOrReplaceChild(
         "TailTertiary",
         CubeListBuilder.create().texOffs(55, 36).addBox(-2.0F, -0.25F, -1.5F, 4.0F, 3.0F, 3.0F, CubeDeformation.NONE),
         PartPose.offset(0.0F, 4.5F, 0.0F)
      );
      PartDefinition TailQuaternary = TailTertiary.addOrReplaceChild(
         "TailQuaternary",
         CubeListBuilder.create().texOffs(56, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F, new CubeDeformation(-0.05F)),
         PartPose.offset(0.0F, 2.55F, 0.0F)
      );
      PartDefinition TailQuintary = TailQuaternary.addOrReplaceChild("TailQuintary", CubeListBuilder.create(), PartPose.offset(0.0F, 2.5F, -0.5F));
      PartDefinition Base_r1 = TailQuintary.addOrReplaceChild(
         "Base_r1",
         CubeListBuilder.create()
            .texOffs(32, 0)
            .addBox(-0.5F, 9.4311F, 0.1673F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.05F))
            .texOffs(57, 57)
            .addBox(-0.5F, 1.5311F, 0.1673F, 1.0F, 8.0F, 3.0F, new CubeDeformation(-0.05F)),
         PartPose.offsetAndRotation(0.0F, 0.5834F, -0.2961F, -0.3491F, 0.0F, 0.0F)
      );
      PartDefinition Base_r2 = TailQuintary.addOrReplaceChild(
         "Base_r2",
         CubeListBuilder.create()
            .texOffs(16, 47)
            .addBox(-0.5F, -10.3457F, 2.9744F, 1.0F, 7.0F, 1.0F, CubeDeformation.NONE)
            .texOffs(36, 53)
            .addBox(-0.5F, -12.3457F, -0.0256F, 1.0F, 10.0F, 3.0F, CubeDeformation.NONE),
         PartPose.offsetAndRotation(0.0F, 0.5834F, -0.2961F, -2.3562F, 0.0F, 0.0F)
      );
      PartDefinition Base_r3 = TailQuintary.addOrReplaceChild(
         "Base_r3",
         CubeListBuilder.create().texOffs(0, 19).addBox(-1.0F, 0.1701F, -0.3405F, 2.0F, 5.0F, 2.0F, CubeDeformation.NONE),
         PartPose.offsetAndRotation(0.0F, 0.5834F, -0.2961F, 0.0F, 0.0F, 0.0F)
      );
        return LayerDefinition.create(mesh, 96, 96);
    }

    @Override
    public void setupAnim(@NotNull LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
        float swimming = entity.getSwimAmount(partialTick);
        if (entity.isInWater()) {
            swimming = Math.max(swimming, entity.isVisuallySwimming() ? 1.0F : 0.65F);
        }
        setupPose(swimming, limbSwing, limbSwingAmount, ageInTicks);
    }

    public void setupItemPose() {
        setupPose(0.0F, 0.0F, 0.0F, 0.0F);
        waist.y = -12.0F;
    }

    public void setupPose(float swimming, float limbSwing, float limbSwingAmount, float ageInTicks) {
        waist.getAllParts().forEach(ModelPart::resetPose);
        swimming = Mth.clamp(swimming, 0.0F, 1.0F);
        // Siren's LeglessInitAnimatorV2 / LeglessSwimAnimatorV2 joint poses.
        abdomen.xRot = Mth.lerp(swimming, -0.218166F, 0.0F);
        lowerAbdomen.xRot = Mth.lerp(swimming, 0.741765F, 0.0F);
        tail.xRot = Mth.lerp(swimming, 1.047198F, 0.0F);
        float phase = limbSwing * 0.25F + ageInTicks * 0.08F;
        float movement = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float sway = Mth.lerp(swimming, 0.05F + movement * 0.10F, 0.20F);
        abdomen.zRot = -sway * Mth.cos(phase);
        lowerAbdomen.zRot = -sway * Mth.cos(phase - Mth.PI / 6.0F);
        tail.zRot = -sway * Mth.cos(phase - Mth.PI / 3.0F);
        for (int i = 0; i < joints.size(); i++) {
            joints.get(i).zRot = -sway * Mth.cos(phase - Mth.PI / 3.0F * (1.0F + i * 0.5F));
        }
    }

    public void copyBodyPoseFrom(EntityModel<?> parent) {
        ModelPart body = parent instanceof TorsoedModel torsoed ? torsoed.getTorso()
                : parent instanceof HumanoidModel<?> humanoid ? humanoid.body : null;
        if (body == null) {
            return;
        }
        waist.copyFrom(body);
        float hipY = body.y + 12.0F;
        if (parent instanceof LeglessModel legless) {
            hipY = legless.getAbdomen().y;
        } else if (parent instanceof AdvancedHumanoidModel<?> advanced) {
            hipY = advanced.getLeg(HumanoidArm.LEFT).y;
        } else if (parent instanceof HumanoidModel<?> humanoid) {
            hipY = humanoid.leftLeg.y;
        }
        abdomen.y = hipY - body.y - 0.2F;
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
        waist.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
    }
}
