package github.com.gengyoubo.CE.compat.mekanism.fission.jei;

import github.com.gengyoubo.CE.compat.mekanism.fission.FissionCompat;
import github.com.gengyoubo.CE.compat.mekanism.fission.FissionConfig;
import mekanism.client.jei.CatalystRegistryHelper;
import mekanism.generators.client.jei.GeneratorsJEIRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;

/** Optional client bridge; CE's common JEI plugin only calls this when Generators is installed. */
public final class FissionJei {
    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        FissionCompat.UPGRADES.values().forEach(block ->
                CatalystRegistryHelper.register(registration, GeneratorsJEIRecipeType.FISSION, block));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        FissionCompat.UPGRADES.forEach((type, block) -> {
            var details = new java.util.ArrayList<Component>();
            details.add(Component.translatable("jei.changede.fission.install"));
            switch (type) {
                case COOLING -> details.add(Component.translatable("jei.changede.fission.cooling_info", percent(FissionConfig.COOLING_EFFICIENCY.get())));
                case PROCESSOR_X -> details.add(Component.translatable("jei.changede.fission.x_info", percent(FissionConfig.FUEL_EFFICIENCY.get()), percent(FissionConfig.OUTPUT_BONUS.get())));
                case PROCESSOR_Y -> details.add(Component.translatable("jei.changede.fission.y_info", percent(FissionConfig.RETURN_CHANCE.get())));
                case SCRAM -> details.add(Component.translatable("jei.changede.fission.scram_info"));
                case ME_PORT -> details.add(Component.translatable("jei.changede.fission.me_info", FissionConfig.ME_IDLE_POWER.get()));
                case THERMOELECTRIC -> {
                    details.add(Component.translatable("jei.changede.fission.thermo_info",
                            FissionConfig.THERMO_MAX_HEAT.get(), percent(FissionConfig.THERMO_PASSIVE.get()), FissionConfig.THERMO_OUTPUT.get()));
                    details.add(Component.translatable("jei.changede.fission.thermo_fuse"));
                }
            }
            registration.addItemStackInfo(block.getItemStack(), details.toArray(Component[]::new));
        });
        registration.addItemStackInfo(new net.minecraft.world.item.ItemStack(FissionCompat.HEAT_EXCHANGE_CORE.get()), Component.translatable("jei.changede.fission.thermo_core"));
    }

    static String percent(double value) { return String.format(java.util.Locale.ROOT, "%.2f%%", value * 100); }
    private FissionJei() {}
}
