package github.com.gengyoubo.mixins;

import net.foxyas.changedaddon.variant.TransfurVariantsInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TransfurVariantsInfo.class, remap = false)
public class TransfurVariantsInfoMixin {
    //可能会有漏洞
    @SuppressWarnings("MixinAnnotationTarget")
    @Redirect(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            remap = false,
            require = 0
    )
    private static MutableComponent changede$replaceLiteral(String text) {

        return switch (text) {
            case "Not free for use, Unknown owner" -> Component.translatable("changed_addon.owner.unknown");
            case "Free For Use, No Owner" -> Component.translatable("changed_addon.owner.free");
            case "Free for use but made By @Foxyas" -> Component.translatable("changed_addon.owner.foxyas");
            case "No form linked, please link one with §e<Shift+Click>" ->
                    Component.translatable("changed_addon.words1");
            default -> Component.literal(text);
        };
    }
}