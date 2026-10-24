package github.com.gengyoubo.CE.LP.client.model;

import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.client.renderer.model.LeglessModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class MimicSirenTailVisibility {
    private MimicSirenTailVisibility() {
    }

    public static void renderUpperBody(EntityModel<?> model, Runnable render) {
        Map<ModelPart, Boolean> visibility = new IdentityHashMap<>();
        if (model instanceof HumanoidModel<?> humanoid) {
            hide(visibility, humanoid.leftLeg);
            hide(visibility, humanoid.rightLeg);
        }
        if (model instanceof PlayerModel<?> player) {
            hide(visibility, player.leftPants);
            hide(visibility, player.rightPants);
        }
        if (model instanceof AdvancedHumanoidModel<?> changed) {
            hide(visibility, changed.getLeg(HumanoidArm.LEFT));
            hide(visibility, changed.getLeg(HumanoidArm.RIGHT));
            changed.getAllParts().forEach(stem -> {
                ModelPart part = stem.getLeaf();
                for (String name : List.of("Tail", "Abdomen", "LowerBody", "LeftBackLeg", "RightBackLeg")) {
                    if (part.hasChild(name)) {
                        hide(visibility, part.getChild(name));
                    }
                }
            });
        }
        if (model instanceof LeglessModel legless) {
            hide(visibility, legless.getAbdomen());
        }
        try {
            render.run();
        } finally {
            // Renderer models are shared between entities and inventory previews.
            visibility.forEach((part, visible) -> part.visible = visible);
        }
    }

    private static void hide(Map<ModelPart, Boolean> visibility, ModelPart part) {
        if (part != null) {
            visibility.putIfAbsent(part, part.visible);
            part.visible = false;
        }
    }
}
