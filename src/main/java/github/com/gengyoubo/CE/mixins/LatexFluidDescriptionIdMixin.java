package github.com.gengyoubo.CE.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Corrects Changed's bare FluidType description IDs so the existing locale entries are used. */
@Mixin(targets = {
        "net.ltxprogrammer.changed.fluid.DarkLatexFluid",
        "net.ltxprogrammer.changed.fluid.WhiteLatexFluid"
})
public abstract class LatexFluidDescriptionIdMixin {
    @ModifyArg(
            method = "createFluidType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/FluidType$Properties;descriptionId(Ljava/lang/String;)Lnet/minecraftforge/fluids/FluidType$Properties;"
            ),
            index = 0
    )
    private static String changede$qualifyFluidDescriptionId(String descriptionId) {
        return "fluid.changed." + descriptionId;
    }
}
