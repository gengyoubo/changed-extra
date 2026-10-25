package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.curios.CuriosAccessorySlots;
import java.util.Optional;
import net.ltxprogrammer.changed.data.AccessorySlots;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AccessorySlots.class, priority = 900, remap = false)
public abstract class AccessorySlotsCuriosMixin {
    @Inject(method = "getForEntity", at = @At("RETURN"), cancellable = true)
    private static void changede$useCuriosForPlayers(LivingEntity entity, CallbackInfoReturnable<Optional<AccessorySlots>> cir) {
        CuriosAccessorySlots.forEntity(entity, false).ifPresent(slots -> cir.setReturnValue(Optional.of(slots)));
    }
}
