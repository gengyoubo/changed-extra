package github.com.gengyoubo.CE.LP.mixins;

import net.ltxprogrammer.changed.block.entity.PurifierBlockEntity;
import net.ltxprogrammer.changed.world.inventory.PurifierMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PurifierBlockEntity.class, remap = false)
public abstract class PurifierMenuServerPositionMixin {
    @Inject(method = "createMenu", at = @At("RETURN"))
    private void changede$setMenuBlockPos(int id, Inventory inventory, CallbackInfoReturnable<AbstractContainerMenu> cir) {
        if (cir.getReturnValue() instanceof PurifierMenu menu) {
            PurifierBlockEntity blockEntity = (PurifierBlockEntity) (Object) this;
            menu.x = blockEntity.getBlockPos().getX();
            menu.y = blockEntity.getBlockPos().getY();
            menu.z = blockEntity.getBlockPos().getZ();
        }
    }
}
