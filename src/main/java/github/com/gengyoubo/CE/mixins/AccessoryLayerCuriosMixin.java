package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.compat.curios.CuriosAccessorySlots;
import java.util.Optional;
import net.ltxprogrammer.changed.client.renderer.layers.AccessoryLayer;
import net.ltxprogrammer.changed.data.AccessorySlots;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AccessoryLayer.class, remap = false)
public abstract class AccessoryLayerCuriosMixin {
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/ltxprogrammer/changed/data/AccessorySlots;getForEntity(Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/Optional;"))
    private Optional<AccessorySlots> changede$renderCuriosVisibilityAndCosmetics(LivingEntity entity) {
        var curios = CuriosAccessorySlots.forEntity(entity, true);
        return curios.isPresent() ? curios : AccessorySlots.getForEntity(entity);
    }
}
