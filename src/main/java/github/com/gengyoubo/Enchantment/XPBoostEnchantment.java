package github.com.gengyoubo.Enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class XPBoostEnchantment extends Enchantment {

    public XPBoostEnchantment() {
        super(
            Rarity.UNCOMMON,
            EnchantmentCategory.WEAPON,
            new EquipmentSlot[]{EquipmentSlot.MAINHAND}
        );
    }

    @Override
    public int getMaxLevel() {
        return 10;
    }
}
