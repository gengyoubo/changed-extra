package github.com.gengyoubo.fix.SpecialLatexFix;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ltxprogrammer.changed.Changed;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SpecialLatexModel extends AdvancedHumanoidModel<SpecialLatex> {
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart head;
    private final ModelPart torso;
    private final HumanoidAnimator<SpecialLatex, SpecialLatexModel> animator;

    private static ModelPart getChildIfPresent(ModelPart parent, String childName) {
        try {
            return parent.getChild(childName);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ModelPart getFirstPresentChild(ModelPart parent, String... names) {
        for (String name : names) {
            ModelPart child = getChildIfPresent(parent, name);
            if (child != null) return child;
        }
        return null;
    }

    private static boolean hasAnyChild(ModelPart parent, String... names) {
        for (String name : names) {
            if (parent.hasChild(name)) return true;
        }
        return false;
    }

    private static ModelPart unwrapWingContainer(ModelPart group, boolean left) {
        if (group == null) return null;
        ModelPart nested = left
                ? getFirstPresentChild(group, "LeftWing", "leftWing", "leftwing", "WingL", "wingL")
                : getFirstPresentChild(group, "RightWing", "rightWing", "rightwing", "RightWing3", "WingR", "wingR");
        return nested != null ? nested : group;
    }

    public SpecialLatexModel(ModelPart root, PatreonBenefits.ModelData form) {
        super(root);
        this.rightLeg = root.getChild("RightLeg");
        this.leftLeg = root.getChild("LeftLeg");
        this.head = root.getChild("Head");
        this.torso = root.getChild("Torso");
        ModelPart tail = form.animationData().hasTail() ? torso.getChild("Tail") : null;
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
        boolean hasWingsFlag = form.animationData().hasWings();
        boolean hasWingsV2Flag = form.animationData().hasWingsV2();

        ModelPart leftWingGroup = getFirstPresentChild(torso,
                "LeftWing", "leftWing", "leftwing",
                "LeftWings", "leftWings", "leftwings");
        ModelPart rightWingGroup = getFirstPresentChild(torso,
                "RightWing", "rightWing", "rightwing",
                "RightWings", "rightWings", "rightwings");
        leftWingGroup = unwrapWingContainer(leftWingGroup, true);
        rightWingGroup = unwrapWingContainer(rightWingGroup, false);
        boolean wingsDetected = leftWingGroup != null && rightWingGroup != null;
        boolean wingAnimationAdded = false;
        String wingAnimationMode = "none";

        if (wingsDetected) {
            boolean preferV2 = hasWingsV2Flag || !hasWingsFlag;

            if (preferV2) {
                ModelPart leftWingRoot = getFirstPresentChild(leftWingGroup, "WingRoot", "leftWingRoot", "WingRoot2");
                ModelPart rightWingRoot = getFirstPresentChild(rightWingGroup, "WingRoot2", "rightWingRoot", "WingRoot");
                if (leftWingRoot == null && hasAnyChild(leftWingGroup, "bone3", "leftSecondaries", "bone")) {
                    leftWingRoot = leftWingGroup;
                }
                if (rightWingRoot == null && hasAnyChild(rightWingGroup, "bone", "rightSecondaries", "bone3")) {
                    rightWingRoot = rightWingGroup;
                }

                if (leftWingRoot != null && rightWingRoot != null) {
                    ModelPart leftSecondary = getFirstPresentChild(leftWingRoot, "bone3", "leftSecondaries", "bone");
                    ModelPart leftTertiary = leftSecondary != null
                            ? getFirstPresentChild(leftSecondary, "bone4", "leftTertiaries", "bone2")
                            : null;

                    ModelPart rightSecondary = getFirstPresentChild(rightWingRoot, "bone", "rightSecondaries", "bone3");
                    ModelPart rightTertiary = rightSecondary != null
                            ? getFirstPresentChild(rightSecondary, "bone2", "rightTertiaries", "bone4")
                            : null;

                    if (leftSecondary != null && leftTertiary != null && rightSecondary != null && rightTertiary != null) {
                        this.animator.addPreset(AnimatorPresets.wingedV2(
                                leftWingRoot, leftSecondary, leftTertiary,
                                rightWingRoot, rightSecondary, rightTertiary));
                        wingAnimationAdded = true;
                        wingAnimationMode = "v2";
                    } else if (leftSecondary != null && rightSecondary != null) {
                        // Legacy two-stage wing rigs can animate more naturally with legacyWinged.
                        ModelPart leftThird = leftTertiary != null ? leftTertiary : leftSecondary;
                        ModelPart rightThird = rightTertiary != null ? rightTertiary : rightSecondary;
                        this.animator.addPreset(AnimatorPresets.legacyWinged(
                                leftWingRoot, leftSecondary, leftThird,
                                rightWingRoot, rightSecondary, rightThird
                        ));
                        wingAnimationAdded = true;
                        wingAnimationMode = "legacy";
                    }
                }
            }

            if (!wingAnimationAdded) {
                this.animator.addPreset(AnimatorPresets.wingedOld(leftWingGroup, rightWingGroup));
                wingAnimationAdded = true;
                wingAnimationMode = "old";
            }
        }

        if ((hasWingsFlag || hasWingsV2Flag) && !wingAnimationAdded) {
            Changed.LOGGER.warn("SpecialLatexModel wings enabled in animation config but no usable wing nodes were found.");
        } else if (!hasWingsFlag && !hasWingsV2Flag && wingAnimationAdded) {
            Changed.LOGGER.info("SpecialLatexModel auto-enabled wing animation from detected wing nodes.");
        }
        if (wingAnimationAdded) {
            Changed.LOGGER.info("SpecialLatexModel wing animation mode={}", wingAnimationMode);
        }

        this.animator.hipOffset = form.hipOffset();
        this.animator.torsoWidth = form.torsoWidth();
        this.animator.forwardOffset = form.forwardOffset();
        this.animator.torsoLength = form.torsoLength();
        this.animator.armLength = form.armLength();
        this.animator.legLength = form.legLength();

        Changed.LOGGER.debug(
                "SpecialLatexModel rig: hipOffset={} torsoLength={} armLength={} legLength={} pivots(headY={},torsoY={},rightArmY={},leftArmY={},rightLegY={},leftLegY={})",
                this.animator.hipOffset,
                this.animator.torsoLength,
                this.animator.armLength,
                this.animator.legLength,
                this.head.y,
                this.torso.y,
                this.rightArm.y,
                this.leftArm.y,
                this.rightLeg.y,
                this.leftLeg.y
        );
    }

    @Override
    public void setupAnim(@NotNull SpecialLatex entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.animator.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public @NotNull ModelPart getArm(HumanoidArm arm) {
        return arm == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
    }

    @Override
    public ModelPart getLeg(HumanoidArm arm) {
        return arm == HumanoidArm.LEFT ? this.leftLeg : this.rightLeg;
    }

    @Override
    public @NotNull ModelPart getHead() {
        return this.head;
    }

    @Override
    public ModelPart getTorso() {
        return this.torso;
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
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
