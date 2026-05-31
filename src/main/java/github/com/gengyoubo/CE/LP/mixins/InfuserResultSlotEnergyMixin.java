package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyRules;
import net.ltxprogrammer.changed.world.inventory.InfuserMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.ltxprogrammer.changed.world.inventory.InfuserMenu$1", remap = false)
public abstract class InfuserResultSlotEnergyMixin {
    @Inject(method = {"onTake", "m_6654_"}, at = @At("HEAD"))
    private void changede$consumeEnergyOnCraft(Player player, ItemStack stack, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            InfuserMenu menu = changede$getMenu();
            if (menu != null) {
                WorkbenchEnergyRules.consumeInfuserCraft(menu.world, new BlockPos(menu.x, menu.y, menu.z), false);
            }
        }
    }

    private InfuserMenu changede$getMenu() {
        try {
            var field = this.getClass().getDeclaredField("this$0");
            field.setAccessible(true);
            Object value = field.get(this);
            return value instanceof InfuserMenu menu ? menu : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
