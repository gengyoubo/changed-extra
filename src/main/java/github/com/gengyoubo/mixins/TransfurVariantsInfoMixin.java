package github.com.gengyoubo.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.foxyas.changedaddon.variant.TransfurVariantsInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@Mixin(value = TransfurVariantsInfo.class, remap = false)
public class TransfurVariantsInfoMixin {

    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
        ),
        remap = false
    )
    private static MutableComponent changede$replaceLiteral(String text) {

        if (text.equals("Not free for use, Unknown owner")) {
            return Component.translatable("changed_addon.owner.unknown");
        }

        if (text.equals("Free For Use, No Owner")) {
            return Component.translatable("changed_addon.owner.free");
        }
        if (text.equals("Free for use but made By @Foxyas")) {
            return Component.translatable("changed_addon.owner.foxyas");
        }
        if (text.equals("No form linked, please link one with §e<Shift+Click>")) {
            return Component.translatable("changed_addon.words1");
        }
        return Component.literal(text);
    }
}