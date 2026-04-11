package github.com.gengyoubo.projectextended.mixins;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import github.com.gengyoubo.projectextended.items.CEShield;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public class PlayerShieldDisableMixin {

    @Redirect(
            method = "blockUsingShield",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;disableShield(Z)V"
            )
    )
    private void changede$skipDisableShieldForRedMatter(Player player, boolean strongAttack) {
        ItemStack shieldStack = player.getUseItem();
        if (shieldStack.getItem() instanceof CEShield shield && shield.isRedMatter()) {
            if (player instanceof ServerPlayer serverPlayer) {
                changed_extra$awardAdvancement(serverPlayer, ResourceLocation.fromNamespaceAndPath("changede", "shield_disable_fail"));
            }
            return;
        }
        player.disableShield(strongAttack);
    }
    @Unique
    private void changed_extra$awardAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(advancementId);
        if (advancement == null) {
            return;
        }

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
