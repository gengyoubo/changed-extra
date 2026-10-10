package github.com.gengyoubo.CE.LP.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Honor stack-specific mining tiers, including materials on Tinkers' tools. */
@Mixin(targets = {"net.foxyas.changedaddon.block.IridiumOreBlock", "net.foxyas.changedaddon.block.IridiumBlock"}, remap = false)
public abstract class AddonIridiumHarvestMixin {
    @Inject(method = "canHarvestBlock", at = @At("HEAD"), cancellable = true)
    private void changede$useStackHarvestTier(BlockState state, BlockGetter level, BlockPos pos, Player player,
                                            CallbackInfoReturnable<Boolean> cir) {
        // Addon's extra TieredItem.getTier() check reads the item's fixed base tier rather
        // than its stack's actual tier. Forge already checks the tool, tier and harvest event.
        cir.setReturnValue(ForgeHooks.isCorrectToolForDrops(state, player));
    }
}
