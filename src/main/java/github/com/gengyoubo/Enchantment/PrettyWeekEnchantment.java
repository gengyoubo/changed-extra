package github.com.gengyoubo.Enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class PrettyWeekEnchantment extends Enchantment{
    public PrettyWeekEnchantment() {
        super(Rarity.RARE,
                EnchantmentCategory.BREAKABLE,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }
    @Override
    public int getMaxLevel() {
        return 5;
    }
}
