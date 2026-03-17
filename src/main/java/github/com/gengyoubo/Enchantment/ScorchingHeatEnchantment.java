package github.com.gengyoubo.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;

public class ScorchingHeatEnchantment extends Enchantment{
    public ScorchingHeatEnchantment() {
        super(
            Rarity.UNCOMMON,
            EnchantmentCategory.DIGGER,
            new EquipmentSlot[]{EquipmentSlot.MAINHAND}
        );
    }
    @Override
public boolean checkCompatibility(@NotNull Enchantment other) {
    return super.checkCompatibility(other) &&
           other != Enchantments.SILK_TOUCH;
}
}
