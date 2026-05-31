package github.com.gengyoubo.CE.LP.mixins;

import net.ltxprogrammer.changed.block.Infuser;
import net.ltxprogrammer.changed.world.inventory.InfuserMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Infuser.class, remap = false)
public abstract class InfuserOpenScreenMixin {
    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true)
    private void changede$openWithBlockPos(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            MenuConstructor constructor = (id, inventory, ignored) -> {
                InfuserMenu menu = new InfuserMenu(id, inventory, null);
                menu.x = pos.getX();
                menu.y = pos.getY();
                menu.z = pos.getZ();
                return menu;
            };
            NetworkHooks.openScreen(
                    serverPlayer,
                    new SimpleMenuProvider(constructor, Component.translatable("container.changed.infuser")),
                    buffer -> buffer.writeBlockPos(pos)
            );
        }
        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
