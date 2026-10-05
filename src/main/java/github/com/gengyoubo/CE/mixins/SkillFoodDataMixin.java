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
    // Keep the eat(IF) invocation intact for SSC/Apoli redirects. FoodData only reads
    // nutrition and saturation from this local; normal eating effects still come from the item.
    // This three-argument overload is added by Forge and has no vanilla obfuscation mapping.
    @ModifyVariable(method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("STORE"), ordinal = 0, remap = false)
    private FoodProperties changede$diet(FoodProperties original, Item item, ItemStack stack, LivingEntity entity) {
        if (original == null || !(entity instanceof ServerPlayer player) || !SkillDiets.matches(player, stack))
            return original;
        changede$setOwner(player);
        var meal = SkillNutrition.dietMeal(original.getNutrition(), original.getSaturationModifier());
        var builder = new FoodProperties.Builder().nutrition(meal.nutrition()).saturationMod(meal.saturationModifier());
        if (original.isMeat()) builder.meat();
        if (original.canAlwaysEat()) builder.alwaysEat();
        if (original.isFastFood()) builder.fast();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
        return builder.build();
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
