package github.com.gengyoubo.projectextended.items;

import gg.galaxygaming.projectextended.common.items.PETrident;
import moze_intel.projecte.gameObjs.EnumMatterType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import org.jetbrains.annotations.NotNull;

public class CETrident extends PETrident {
    public CETrident(EnumMatterType matterType, int numCharges, float damage, Properties props) {
        super(matterType, numCharges, damage, props);
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
        return enchantment.category == EnchantmentCategory.TRIDENT || enchantment.category == EnchantmentCategory.BREAKABLE;

    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }
}
