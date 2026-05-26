package github.com.gengyoubo.CE.mixins;

import net.ltxprogrammer.changed.ChangedConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChangedConfig.Common.class)
public abstract class ChangedConfigCommonMixin {
    @ModifyArg(
            method = "<init>",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/common/ForgeConfigSpec$Builder;define(Ljava/lang/String;Z)Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;",
                    ordinal = 0
            ),
            index = 1
    )
    private boolean changede$disablePatreonByDefault(boolean originalValue) {
        return false;
    }
}
