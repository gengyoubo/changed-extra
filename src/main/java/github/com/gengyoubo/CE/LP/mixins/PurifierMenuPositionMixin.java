package github.com.gengyoubo.CE.LP.mixins;

import net.ltxprogrammer.changed.world.inventory.PurifierMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PurifierMenu.class, remap = false)
public abstract class PurifierMenuPositionMixin {
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
    private void changede$readBlockPos(int id, Inventory inventory, FriendlyByteBuf buffer, CallbackInfo ci) {
        if (buffer == null || buffer.readableBytes() < Long.BYTES) {
            return;
        }

        BlockPos pos = buffer.readBlockPos();
        PurifierMenu menu = (PurifierMenu) (Object) this;
        menu.x = pos.getX();
        menu.y = pos.getY();
        menu.z = pos.getZ();
    }
}
