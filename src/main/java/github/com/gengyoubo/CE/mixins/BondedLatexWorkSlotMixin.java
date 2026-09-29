package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.MaidWorkSwitchPacket;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.util.SingleRunnable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.parkabird.changedsynergy.client.BondedLatexScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Uses the eighth radial sector when Synergy has no suit action for this creature. */
@Mixin(value = BondedLatexScreen.class, remap = false)
public abstract class BondedLatexWorkSlotMixin {
    private static final ResourceLocation CHANGEDE$WORK_ICON =
            ResourceLocation.fromNamespaceAndPath("changede", "textures/gui/maid_work.png");

    @Shadow @Final private List<?> interactions;

    private boolean changede$hasEmptySlot() {
        return interactions.size() == 7;
    }

    @Inject(method = "getCount", at = @At("RETURN"), cancellable = true, remap = false)
    private void changede$includeWorkSlot(CallbackInfoReturnable<Integer> cir) {
        if (changede$hasEmptySlot()) cir.setReturnValue(8);
    }

    @Inject(method = "tooltipsFor", at = @At("HEAD"), cancellable = true, remap = false)
    private void changede$workTooltip(int section, CallbackInfoReturnable<List<Component>> cir) {
        if (section != 7 || !changede$hasEmptySlot()) return;
        cir.setReturnValue(List.of(
                Component.translatable("screen.changede.maid_work.title"),
                Component.translatable("screen.changede.maid_work.unlock")
        ));
    }

    @Inject(method = "renderSectionForeground", at = @At("HEAD"), cancellable = true, remap = false)
    private void changede$renderWorkIcon(GuiGraphics graphics, int section, double x, double y,
                                        float scale, int mouseX, int mouseY,
                                        float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (section != 7 || !changede$hasEmptySlot()) return;
        BondedLatexScreen screen = (BondedLatexScreen) (Object) this;
        int drawX = screen.getGuiLeft() + (int) x - 24;
        int drawY = screen.getGuiTop() + (int) y - 24;
        graphics.setColor(0.0F, 0.0F, 0.0F, 0.5F * alpha);
        graphics.blit(CHANGEDE$WORK_ICON, drawX + 3, drawY + 3, 0, 0, 48, 48, 48, 48);
        graphics.setColor(red, green, blue, alpha);
        graphics.blit(CHANGEDE$WORK_ICON, drawX, drawY, 0, 0, 48, 48, 48, 48);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        ci.cancel();
    }

    @Inject(method = "handleClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void changede$openWorkScreen(int section, SingleRunnable onClose, CallbackInfoReturnable<Boolean> cir) {
        if (section != 7 || !changede$hasEmptySlot()) return;
        ChangedEntity creature = ((BondedLatexScreen) (Object) this).getMenu().getPet();
        if (creature != null) {
            CENetwork.sendToServer(new MaidWorkSwitchPacket(creature.getId(), true));
        }
        cir.setReturnValue(false);
    }
}
