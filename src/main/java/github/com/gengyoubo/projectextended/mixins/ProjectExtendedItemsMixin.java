package github.com.gengyoubo.projectextended.mixins;

import gg.galaxygaming.projectextended.ProjectExtended;
import github.com.gengyoubo.projectextended.PERegister;
import github.com.gengyoubo.projectextended.PTotemOfUndying;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ProjectExtended.class, remap = false)
public class ProjectExtendedItemsMixin {
    @Redirect(
            method = "<init>",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lmoze_intel/projecte/gameObjs/registration/impl/ItemDeferredRegister;register(Lnet/minecraftforge/eventbus/api/IEventBus;)V"
            )
    )
    private void redirectItemsRegister(
            moze_intel.projecte.gameObjs.registration.impl.ItemDeferredRegister instance,
            net.minecraftforge.eventbus.api.IEventBus bus
    ) {
        PERegister.ITEMS.register(bus);
        PTotemOfUndying.ITEMS.register(bus);
    }
}
