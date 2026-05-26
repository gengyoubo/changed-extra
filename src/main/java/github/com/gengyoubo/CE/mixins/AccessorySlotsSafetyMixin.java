package github.com.gengyoubo.CE.mixins;

import net.ltxprogrammer.changed.data.AccessorySlots;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = AccessorySlots.class, remap = false)
public abstract class AccessorySlotsSafetyMixin {
    @Inject(method = "getForEntity", at = @At("RETURN"), cancellable = true, remap = false)
    private static void changede$neverReturnEmptyForAddonCompatibility(
            LivingEntity livingEntity,
            CallbackInfoReturnable<Optional<AccessorySlots>> cir
    ) {
        if (cir.getReturnValue().isEmpty()) {
            cir.setReturnValue(Optional.of(AccessorySlots.DUMMY));
        }
    }
}

