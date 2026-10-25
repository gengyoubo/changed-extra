package github.com.gengyoubo.CE.mixins;

import java.util.Map;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.common.data.CuriosEntityManager;

@Mixin(value = CuriosEntityManager.class, remap = false)
public interface CuriosEntityManagerAccessor {
    @Accessor("entitySlots") Map<EntityType<?>, Map<String, ISlotType>> changede$getEntitySlots();
    @Accessor("entitySlots") void changede$setEntitySlots(Map<EntityType<?>, Map<String, ISlotType>> slots);
}
