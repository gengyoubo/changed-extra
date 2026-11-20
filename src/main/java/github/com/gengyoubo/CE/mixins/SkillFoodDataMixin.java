package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.skill.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Changes natural metabolism while retaining vanilla healing eligibility, food and starvation. */
@Mixin(FoodData.class)
public abstract class SkillFoodDataMixin implements SkillFoodData {
    @Shadow private int tickTimer;
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Unique private Player changed_extra$owner;
    @Unique private double changed_extra$regenRemainder;
    @Unique private double changed_extra$previousCapacity;
    @Override public void changed_extra$setOwner(Player player) { changed_extra$owner = player; }
    @Unique private double changed_extra$attribute(Attribute attribute, double fallback) {
        if (changed_extra$owner == null || changed_extra$owner.level().isClientSide) return fallback;
        var instance = changed_extra$owner.getAttribute(attribute);
        return instance == null ? fallback : instance.getValue();
    }
    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float changed_extra$slowExhaustion(float amount) {
        return SkillNutrition.exhaustion(amount, changed_extra$attribute(SkillAttributes.EXHAUSTION_REDUCTION.get(), 0));
    }
    // Keep the eat(IF) invocation intact for SSC/Apoli redirects. FoodData only reads
    // nutrition and saturation from this local; normal eating effects still come from the item.
    // This three-argument overload is added by Forge and has no vanilla obfuscation mapping.
    @ModifyVariable(method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("STORE"), ordinal = 0, remap = false)
    private FoodProperties changed_extra$diet(FoodProperties original, Item item, ItemStack stack, LivingEntity entity) {
        if (original == null || !(entity instanceof ServerPlayer player) || !SkillDiets.matches(player, stack))
            return original;
        changed_extra$setOwner(player);
        var meal = SkillNutrition.dietMeal(original.getNutrition(), original.getSaturationModifier());
        var builder = new FoodProperties.Builder().nutrition(meal.nutrition()).saturationMod(meal.saturationModifier());
        if (original.isMeat()) builder.meat();
        if (original.canAlwaysEat()) builder.alwaysEat();
        if (original.isFastFood()) builder.fast();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
        return builder.build();
    }
    @ModifyVariable(method = "eat(IF)V", at = @At("HEAD"), argsOnly = true)
    private float changed_extra$mealSaturation(float modifier) {
        return SkillNutrition.foodSaturation(modifier, changed_extra$attribute(SkillAttributes.FOOD_SATURATION.get(), 1));
    }
    @Redirect(method = "eat(IF)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F"))
    private float changed_extra$mealCapacity(float saturation, float vanillaCap) {
        return Math.min(saturation, SkillNutrition.saturationCap(vanillaCap,
                changed_extra$attribute(SkillAttributes.SATURATION_CAPACITY.get(), 0)));
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void changed_extra$naturalGrowth(Player player, CallbackInfo ci) {
        changed_extra$setOwner(player);
        if (player.level().isClientSide) return;
        double capacity = changed_extra$attribute(SkillAttributes.SATURATION_CAPACITY.get(), 0);
        if (capacity > 0 || changed_extra$previousCapacity > 0 || saturationLevel > 20)
            saturationLevel = Math.min(saturationLevel, SkillNutrition.saturationCap(foodLevel, capacity));
        changed_extra$previousCapacity = capacity;
        boolean regeneration = player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        if (SkillNutrition.canRegenerate(regeneration, player.isHurt(), foodLevel)) {
            changed_extra$regenRemainder += SkillNutrition.regenerationBonus(changed_extra$attribute(SkillAttributes.REGENERATION_SPEED.get(), 1));
            int extraTicks = SkillNutrition.wholeBonusTicks(changed_extra$regenRemainder);
            tickTimer += extraTicks;
            changed_extra$regenRemainder = Math.max(0, changed_extra$regenRemainder - extraTicks);
        } else changed_extra$regenRemainder = 0;
    }
}
