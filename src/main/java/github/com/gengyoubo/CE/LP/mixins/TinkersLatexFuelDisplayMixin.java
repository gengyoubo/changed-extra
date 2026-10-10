package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.compat.tinkers.smeltery.LatexSmelteryRules;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import slimeknights.tconstruct.smeltery.block.entity.module.FuelModule;
import slimeknights.tconstruct.smeltery.client.screen.module.GuiFuelModule;

import java.util.List;

@Mixin(value = GuiFuelModule.class, remap = false)
public abstract class TinkersLatexFuelDisplayMixin {
    @Shadow @Final private FuelModule fuelModule;
    @Shadow private FuelModule.FuelInfo fuelInfo;
    @ModifyArg(method = "addTooltip", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V", remap = true), index = 1)
    private List<Component> changede$latexFuelTooltip(List<Component> tooltip) {
        return LatexSmelteryRules.tooltip(fuelModule, fuelInfo, tooltip);
    }
}
