package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.LP.client.WorkbenchEnergyOverlayEvents;
import net.ltxprogrammer.changed.client.gui.InfuserScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InfuserScreen.class, remap = false)
public abstract class InfuserScreenEnergyMixin {
    @Inject(method = {"render", "m_88315_"}, at = @At("TAIL"))
    private void changede$renderEnergy(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        WorkbenchEnergyOverlayEvents.renderEnergyOverlay((AbstractContainerScreen<?>) (Object) this, graphics);
    }
}
