package github.com.gengyoubo.mixins;

import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.jetbrains.annotations.Nullable;

@Mixin(value = net.ltxprogrammer.changed.util.UniversalDist.ServerDist.class, remap = false)
public class UniversalDistServerDistMixin {

    /**
     * @author gengyoubo
     * @reason Avoid NPE before dedicated server is fully available
     */
    @Overwrite
    @Nullable
    public static Level getLevel() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.overworld() : null;
    }
}
