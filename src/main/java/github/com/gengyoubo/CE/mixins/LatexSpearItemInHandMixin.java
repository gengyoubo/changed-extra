package github.com.gengyoubo.CE.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.CE.client.LatexSpearAnimations;
import github.com.gengyoubo.CE.items.LatexSpearItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class LatexSpearItemInHandMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    private float changede$keepSpearRaised(LocalPlayer player, float partialTick) {
        // Attack strength still drives gameplay/HUD; it must not lower the item during STAB.
        return player.getMainHandItem().getItem() instanceof LatexSpearItem ? 1 : player.getAttackStrengthScale(partialTick);
    }

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void changede$stabItem(LivingEntity entity, ItemStack stack, ItemDisplayContext display,
                                  boolean leftHand, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        LatexSpearAnimations.thirdPersonItem(entity, stack, display, pose, Minecraft.getInstance().getFrameTime());
    }
}
