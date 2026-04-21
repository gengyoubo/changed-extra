package github.com.gengyoubo.fix;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ltxprogrammer.changed.client.renderer.animate.AnimatorPresets;
import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.arm.ArmBobAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.arm.ArmRideAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.arm.ArmSwimAnimator;
import net.ltxprogrammer.changed.client.renderer.animate.upperbody.HeadInitAnimator;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;

import java.util.List;

public class SpecialLatexModel extends AdvancedHumanoidModel<SpecialLatex> {
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart tail;
    private final HumanoidAnimator<SpecialLatex, SpecialLatexModel> animator;

    public SpecialLatexModel(ModelPart root, PatreonBenefits.ModelData form) {
        super(root);
        this.rightLeg = root.getChild("RightLeg");
        this.leftLeg = root.getChild("LeftLeg");
        this.head = root.getChild("Head");
        this.torso = root.getChild("Torso");
        this.tail = form.animationData().hasTail() ? torso.getChild("Tail") : null;
        this.rightArm = root.getChild("RightArm");
        this.leftArm = root.getChild("LeftArm");

        this.animator = HumanoidAnimator.of(this);
        this.animator.addPreset(AnimatorPresets.upperBody(head, torso, leftArm, rightArm));
        this.animator.addPreset(AnimatorPresets.bipedal(leftLeg, rightLeg))
                .addAnimator(new HeadInitAnimator<>(head))
                .addAnimator(new ArmBobAnimator<>(leftArm, rightArm))
                .addAnimator(new ArmRideAnimator<>(leftArm, rightArm));

        if (form.animationData().swimTail()) {
            this.animator.addAnimator(new ArmSwimAnimator<>(leftArm, rightArm));
        }
        if (tail != null) {
            this.animator.addPreset(form.animationData().swimTail()
                    ? AnimatorPresets.aquaticTail(tail, List.of())
                    : AnimatorPresets.standardTail(tail, List.of()));
        }
        if (form.animationData().hasWings()) {
            ModelPart rightWing = torso.getChild("RightWing");
            ModelPart leftWing = torso.getChild("LeftWing");
            this.animator.addPreset(AnimatorPresets.wingedOld(leftWing, rightWing));
        } else if (form.animationData().hasWingsV2()) {
            ModelPart rightWing = torso.getChild("RightWing");
            ModelPart leftWing = torso.getChild("LeftWing");
            ModelPart leftWingRoot = leftWing.getChild("WingRoot");
            ModelPart rightWingRoot = rightWing.getChild("WingRoot2");
            this.animator.addPreset(AnimatorPresets.wingedV2(
                    leftWingRoot, leftWingRoot.getChild("bone3"), leftWingRoot.getChild("bone3").getChild("bone4"),
                    rightWingRoot, rightWingRoot.getChild("bone"), rightWingRoot.getChild("bone").getChild("bone2")));
        }

        this.animator.hipOffset = form.hipOffset();
        this.animator.torsoWidth = form.torsoWidth();
        this.animator.forwardOffset = form.forwardOffset();
        this.animator.torsoLength = form.torsoLength();
        this.animator.armLength = form.armLength();
        this.animator.legLength = form.legLength();
    }

    @Override
    public void setupAnim(SpecialLatex entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public ModelPart getArm(HumanoidArm arm) {
        return arm == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
    }

    @Override
    public ModelPart getLeg(HumanoidArm arm) {
        return arm == HumanoidArm.LEFT ? this.leftLeg : this.rightLeg;
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }

    @Override
    public ModelPart getTorso() {
        return this.torso;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.rightLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftLeg.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.head.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.torso.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.rightArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        this.leftArm.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public HumanoidAnimator<SpecialLatex, SpecialLatexModel> getAnimator(SpecialLatex entity) {
        return this.animator;
    }
}
