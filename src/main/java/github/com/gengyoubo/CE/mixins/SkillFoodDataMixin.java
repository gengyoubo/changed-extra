package github.com.gengyoubo.CE.mixins;

import github.com.gengyoubo.CE.skill.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
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
    @Unique private Player changede$owner;
    @Unique private double changede$regenRemainder;
    @Unique private double changede$previousCapacity;
    @Override public void changede$setOwner(Player player) { changede$owner = player; }
    @Unique private double changede$attribute(Attribute attribute, double fallback) {
        if (changede$owner == null || changede$owner.level().isClientSide) return fallback;
        var instance = changede$owner.getAttribute(attribute);
        return instance == null ? fallback : instance.getValue();
    }
    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float changede$slowExhaustion(float amount) {
        return SkillNutrition.exhaustion(amount, changede$attribute(SkillAttributes.EXHAUSTION_REDUCTION.get(), 0));
    }
    @Redirect(method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private void changede$diet(FoodData data, int nutrition, float saturation,
                                  Item item, ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !SkillDiets.matches(player, stack)) {
            data.eat(nutrition, saturation);
            return;
        }
        changede$setOwner(player);
        var meal = SkillNutrition.dietMeal(nutrition, saturation);
        data.eat(meal.nutrition(), meal.saturationModifier());
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
    }
    @ModifyVariable(method = "eat(IF)V", at = @At("HEAD"), argsOnly = true)
    private float changede$mealSaturation(float modifier) {
        return SkillNutrition.foodSaturation(modifier, changede$attribute(SkillAttributes.FOOD_SATURATION.get(), 1));
    }
    @Redirect(method = "eat(IF)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F"))
    private float changede$mealCapacity(float saturation, float vanillaCap) {
        return Math.min(saturation, SkillNutrition.saturationCap(vanillaCap,
                changede$attribute(SkillAttributes.SATURATION_CAPACITY.get(), 0)));
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void changede$naturalGrowth(Player player, CallbackInfo ci) {
        changede$setOwner(player);
        if (player.level().isClientSide) return;
        double capacity = changede$attribute(SkillAttributes.SATURATION_CAPACITY.get(), 0);
        if (capacity > 0 || changede$previousCapacity > 0 || saturationLevel > 20)
            saturationLevel = Math.min(saturationLevel, SkillNutrition.saturationCap(foodLevel, capacity));
        changede$previousCapacity = capacity;
        boolean regeneration = player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        if (SkillNutrition.canRegenerate(regeneration, player.isHurt(), foodLevel)) {
            changede$regenRemainder += SkillNutrition.regenerationBonus(changede$attribute(SkillAttributes.REGENERATION_SPEED.get(), 1));
            int extraTicks = SkillNutrition.wholeBonusTicks(changede$regenRemainder);
            tickTimer += extraTicks;
            changede$regenRemainder = Math.max(0, changede$regenRemainder - extraTicks);
        } else changede$regenRemainder = 0;
    }
}
