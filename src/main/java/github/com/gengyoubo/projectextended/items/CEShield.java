package github.com.gengyoubo.projectextended.items;

import gg.galaxygaming.projectextended.common.items.PEShield;
import moze_intel.projecte.gameObjs.EnumMatterType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.jetbrains.annotations.NotNull;

public class CEShield extends PEShield {
    public CEShield(EnumMatterType matterType, Item.Properties props){
        super(matterType,props);
    }
    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }
    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.category == EnchantmentCategory.BREAKABLE;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }
}
