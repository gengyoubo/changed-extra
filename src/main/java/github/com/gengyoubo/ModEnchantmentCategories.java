package github.com.gengyoubo;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class ModEnchantmentCategories {
    public static final EnchantmentCategory MELEE =
            EnchantmentCategory.create(
                    "melee",
                    item -> item instanceof SwordItem || item instanceof AxeItem
            );
}
