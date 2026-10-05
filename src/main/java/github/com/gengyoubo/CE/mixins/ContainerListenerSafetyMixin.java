package github.com.gengyoubo.CE.mixins;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Listener callbacks may register or remove listeners while a menu broadcasts changes. */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerListenerSafetyMixin {
    @Shadow @Final @Mutable
    private List<ContainerListener> containerListeners;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void changede$useStableListenerSnapshots(CallbackInfo ci) {
        // Writes are rare; slot and data broadcasts can iterate without copying or locking.
        // The current notification finishes its snapshot; changes apply to later notifications.
        containerListeners = new CopyOnWriteArrayList<>(containerListeners);
    }
}
