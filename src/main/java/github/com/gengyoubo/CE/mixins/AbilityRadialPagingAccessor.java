package github.com.gengyoubo.CE.mixins;

import net.ltxprogrammer.changed.client.gui.AbstractRadialScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractRadialScreen.class, remap = false)
public interface AbilityRadialPagingAccessor {
    @Accessor("viewOffset") int changede$getViewOffset();
    @Accessor("viewOffset") void changede$setViewOffset(int offset);
    @Accessor("viewOffsetO") void changede$setPreviousOffset(int offset);
    @Accessor("viewTransitionTicksLeft") void changede$setTransitionTicks(int ticks);
}
