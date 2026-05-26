package github.com.gengyoubo.CE.enchantment;

import github.com.gengyoubo.CE.init.CEEnchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;

public class XPBoostEnchantment extends Enchantment {

    public XPBoostEnchantment() {
        super(
            Rarity.UNCOMMON,
            CEEnchantment.MELEE,
            new EquipmentSlot[]{EquipmentSlot.MAINHAND}
        );
    }

    @Override
    public int getMaxLevel() {
        return 10;
    }
}
