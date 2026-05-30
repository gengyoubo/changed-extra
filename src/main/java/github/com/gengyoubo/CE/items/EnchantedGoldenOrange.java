package github.com.gengyoubo.CE.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;

public class EnchantedGoldenOrange extends Item {
    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(4)
            .saturationMod(1.2F)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 400, 1), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0), 1.0F)
            .alwaysEat()
            .build();

    public EnchantedGoldenOrange() {
        super(new Item.Properties().rarity(Rarity.EPIC).food(FOOD));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }
}
