package github.com.gengyoubo.CE.mixins;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ChangedCannedFoodItemMixin {
    @Inject(method = "isEdible", at = @At("HEAD"), cancellable = true)
    private void changed_extra$makeChangedCansEdible(CallbackInfoReturnable<Boolean> cir) {
        if (changed_extra$isChangedCannedFood()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getFoodProperties", at = @At("HEAD"), cancellable = true)
    private void changed_extra$useBreadFoodValueForChangedCans(CallbackInfoReturnable<FoodProperties> cir) {
        if (changed_extra$isChangedCannedFood()) {
            cir.setReturnValue(Foods.BREAD);
        }
    }

    private boolean changed_extra$isChangedCannedFood() {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey((Item) (Object) this);
        return id != null && "changed".equals(id.getNamespace())
                && ("canned_peaches".equals(id.getPath()) || "canned_soup".equals(id.getPath()));
    }
}
