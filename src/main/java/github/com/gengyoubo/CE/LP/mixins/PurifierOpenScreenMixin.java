package github.com.gengyoubo.CE.LP.mixins;

import net.ltxprogrammer.changed.block.Purifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Purifier.class, remap = false)
public abstract class PurifierOpenScreenMixin {
    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true)
    private void changede$openWithBlockPos(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = state.getMenuProvider(level, pos);
            if (provider != null) {
                NetworkHooks.openScreen(serverPlayer, provider, buffer -> buffer.writeBlockPos(pos));
            }
        }
        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
