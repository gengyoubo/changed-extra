package github.com.gengyoubo;

import github.com.gengyoubo.enchantment.PrettyStrongEnchantment;
import github.com.gengyoubo.enchantment.PrettyWeekEnchantment;
import github.com.gengyoubo.enchantment.SalvageEnchantment;
import github.com.gengyoubo.enchantment.ScorchingHeatEnchantment;
import github.com.gengyoubo.enchantment.SoStrongEnchantment;
import github.com.gengyoubo.enchantment.SoWeakEnchantment;
import github.com.gengyoubo.enchantment.XPBoostEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {

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

}
