package github.com.gengyoubo.Enchantment;

import github.com.gengyoubo.ModEnchantmentCategories;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;

public class XPBoostEnchantment extends Enchantment {

    public XPBoostEnchantment() {
        super(
            Rarity.UNCOMMON,
            ModEnchantmentCategories.MELEE,
            new EquipmentSlot[]{EquipmentSlot.MAINHAND}
        );
    }

    @Override
    public int getMaxLevel() {
        return 10;
    }
}
