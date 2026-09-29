package github.com.gengyoubo.CE.mixins;

import net.ltxprogrammer.changed.fluid.DarkLatexFluid;
import net.ltxprogrammer.changed.fluid.SkunkGas;
import net.ltxprogrammer.changed.fluid.TigerGas;
import net.ltxprogrammer.changed.fluid.WhiteLatexFluid;
import net.ltxprogrammer.changed.fluid.WolfGas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Qualifies Changed's FluidType description IDs so its locale entries are used. */
@Mixin(value = {
        DarkLatexFluid.class,
        WhiteLatexFluid.class,
        SkunkGas.class,
        TigerGas.class,
        WolfGas.class
}, remap = false)
public abstract class ChangedFluidDescriptionIdMixin {
    @ModifyArg(
            method = "createFluidType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/FluidType$Properties;descriptionId(Ljava/lang/String;)Lnet/minecraftforge/fluids/FluidType$Properties;"
            ),
            index = 0,
            remap = false
    )
    private static String changede$qualifyFluidDescriptionId(String descriptionId) {
        return switch (descriptionId) {
            case "skunk_transfur_gas" -> "fluid.changed.skunk_gas";
            case "tiger_transfur_gas" -> "fluid.changed.tiger_gas";
            case "wolf_transfur_gas" -> "fluid.changed.wolf_gas";
            default -> "fluid.changed." + descriptionId;
        };
    }
}
