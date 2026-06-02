package github.com.gengyoubo.CE.init;

import github.com.gengyoubo.CE.enchantment.*;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CEEnchantment {
    public static final EnchantmentCategory MELEE =
            EnchantmentCategory.create(
                    "melee",
                    item -> item instanceof SwordItem || item instanceof AxeItem
            );

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, "changede");
    public static final RegistryObject<Enchantment> SALVAGE =
            ENCHANTMENTS.register("salvage", SalvageEnchantment::new);
    public static final RegistryObject<Enchantment> SCORCHINGHEAT =
            ENCHANTMENTS.register("scorchingheat", ScorchingHeatEnchantment::new);
    public static final RegistryObject<Enchantment> XPBOOST =
            ENCHANTMENTS.register("xpboost", XPBoostEnchantment::new);
    public static final RegistryObject<Enchantment> SOSTRONG =
            ENCHANTMENTS.register("sostrong", SoStrongEnchantment::new);
    public static final RegistryObject<Enchantment> SOWEEK =
            ENCHANTMENTS.register("soweak", SoWeakEnchantment::new);
    public static final RegistryObject<Enchantment> PRETTYWEEK =
            ENCHANTMENTS.register("prettyweek", PrettyWeekEnchantment::new);
    public static final RegistryObject<Enchantment> PREETTYSTRONG =
            ENCHANTMENTS.register("prettystrong", PrettyStrongEnchantment::new);
    public static final RegistryObject<Enchantment> LATEX_WALK =
            ENCHANTMENTS.register("latex_walk", LatexWalkEnchantment::new);
}
