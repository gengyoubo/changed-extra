package github.com.gengyoubo.CE.mixins;

import net.ltxprogrammer.changed.network.packet.MenuUpdatePacket;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** A completed level future must not run menu updates on the receiving Netty thread. */
@Mixin(value = MenuUpdatePacket.class, remap = false)
public abstract class MenuUpdatePacketThreadMixin {
    @Redirect(method = "handle", at = @At(value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;thenAccept(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletableFuture;"),
            remap = false)
    private CompletableFuture<Void> changede$dispatchMenuUpdate(CompletableFuture<Level> future, Consumer<Level> update,
            NetworkEvent.Context context, CompletableFuture<Level> levelFuture, Executor sidedExecutor) {
        return future.thenAcceptAsync(update, sidedExecutor);
    }
}
