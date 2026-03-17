package github.com.gengyoubo.enchantment;

import github.com.gengyoubo.CERegister;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;

public class XPBoostEnchantment extends Enchantment {

    public XPBoostEnchantment() {
        super(
            Rarity.UNCOMMON,
            CERegister.MELEE,
            new EquipmentSlot[]{EquipmentSlot.MAINHAND}
        );
    }

    @Override
    public int getMaxLevel() {
        return 10;
    }
}
