package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.items.LatexSpearItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Comparator;

@Mixin(Gui.class)
public abstract class LatexSpearAttackIndicatorMixin {
    @Unique private boolean changede$spearIndicator;
    @Unique private Entity changede$spearTarget;
    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void changede$findSpearTarget(GuiGraphics graphics, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        changede$spearIndicator = player != null && !player.isSpectator()
                && player.getMainHandItem().getItem() instanceof LatexSpearItem;
        changede$spearTarget = changede$spearIndicator ? LatexSpearItem.targets(player).stream()
                .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null) : null;
    }
    @Redirect(method = "renderCrosshair", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;crosshairPickEntity:Lnet/minecraft/world/entity/Entity;"))
    private Entity changede$spearIndicatorRange(Minecraft minecraft) {
        // MC-302677: only the attack indicator uses this ray; entity interactions retain native reach.
        return changede$spearIndicator ? changede$spearTarget : minecraft.crosshairPickEntity;
    }
}
