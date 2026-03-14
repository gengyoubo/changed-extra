package github.com.gengyoubo.mixins;

import net.foxyas.changedaddon.event.ClientEvent;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientEvent.class)
public class ClientEventMixin {

    @Redirect(
        method = "showExtraTransfurInfo",
        remap = false,
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
        )
    )
    private static MutableComponent redirectLiteral(String text) {

        String clean = text.replaceAll("§.", "");

        switch (clean) {

            case "None":
                return Component.translatable("text.changed_addon.none");

            case "True":
                return Component.translatable("text.changed_addon.true");

            case "False":
                return Component.translatable("text.changed_addon.false");

            case "OC Transfur":
                return Component.translatable("text.changed_addon.oc_transfur");

            case "Boss Version":
                return Component.translatable("text.changed_addon.boss_version");
        }

        return Component.literal(text);
    }


    @Redirect(
        method = "showExtraTransfurInfo",
        remap = false,
        require = 0,
        at = @At(
            value = "INVOKE",
            target = "Lnet/foxyas/changedaddon/util/TransfurVariantUtils;getMiningStrength(Lnet/ltxprogrammer/changed/entity/variant/TransfurVariant;)Ljava/lang/String;"
        )
    )
    private static String redirectMiningStrength(TransfurVariant<?> variant) {
    return Component.translatable(
        "text.changed_addon.mining_strength." + variant.miningStrength.getSerializedName()
    ).getString();
}
}