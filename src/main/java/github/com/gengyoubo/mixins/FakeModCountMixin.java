package github.com.gengyoubo.mixins;

import net.minecraftforge.internal.BrandingControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BrandingControl.class, remap = false)
public class FakeModCountMixin {
    @Redirect(
            method = "computeBranding",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/common/ForgeI18n;parseMessage(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"
            )
    )
    private static String redirect(String key, Object... args) {
        if (false){
            if ("fml.menu.loadingmods".equals(key)) {
                return "∞个Mod已加载";
            }
        }
        return net.minecraftforge.common.ForgeI18n.parseMessage(key, args);
    }
}
