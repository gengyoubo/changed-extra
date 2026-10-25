package github.com.gengyoubo.CE.mixins;

import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.common.data.CuriosSlotManager;

@Mixin(value = CuriosSlotManager.class, remap = false)
public interface CuriosSlotManagerAccessor {
    @Accessor("slots") void changede$setSlots(Map<String, ISlotType> slots);
}
