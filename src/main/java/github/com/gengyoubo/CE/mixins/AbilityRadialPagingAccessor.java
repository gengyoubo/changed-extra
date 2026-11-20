package github.com.gengyoubo.CE.mixins;

import net.ltxprogrammer.changed.client.gui.AbstractRadialScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractRadialScreen.class, remap = false)
public interface AbilityRadialPagingAccessor {
    @Accessor("viewOffset") int changed_extra$getViewOffset();
    @Accessor("viewOffset") void changed_extra$setViewOffset(int offset);
    @Accessor("viewOffsetO") void changed_extra$setPreviousOffset(int offset);
    @Accessor("viewTransitionTicksLeft") void changed_extra$setTransitionTicks(int ticks);
}
