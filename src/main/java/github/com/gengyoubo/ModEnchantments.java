package github.com.gengyoubo;

import github.com.gengyoubo.Enchantment.SalvageEnchantment;
import github.com.gengyoubo.Enchantment.ScorchingHeatEnchantment;
import github.com.gengyoubo.Enchantment.XPBoostEnchantment;
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
}
