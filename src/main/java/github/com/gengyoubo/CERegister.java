package github.com.gengyoubo;

import github.com.gengyoubo.LP.CELPRegister;
import github.com.gengyoubo.enchantment.PrettyStrongEnchantment;
import github.com.gengyoubo.enchantment.PrettyWeekEnchantment;
import github.com.gengyoubo.enchantment.SalvageEnchantment;
import github.com.gengyoubo.enchantment.ScorchingHeatEnchantment;
import github.com.gengyoubo.enchantment.SoStrongEnchantment;
import github.com.gengyoubo.enchantment.SoWeakEnchantment;
import github.com.gengyoubo.enchantment.XPBoostEnchantment;
import github.com.gengyoubo.items.InactiveDarkLatex;
import github.com.gengyoubo.items.InactiveWhiteLatex;
import github.com.gengyoubo.items.LatexGray;
import github.com.gengyoubo.items.LatexIngot;
import github.com.gengyoubo.items.UnbakedLatexIngot;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CERegister {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "changede");
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "changede");
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, "changede");
    private static final String PROJECT_E_MODID = "projecte";
    private static final String PTOTEM_CLASS = "github.com.gengyoubo.projectextended.PTotemOfUndying";
    public static final RegistryObject<Item> INACTIVE_DARK_LATEX =
            ITEMS.register("inactive_dark_latex", InactiveDarkLatex::new);
    public static final RegistryObject<Item> INACTIVE_WHITE_LATEX =
            ITEMS.register("inactive_white_latex", InactiveWhiteLatex::new);
    public static final RegistryObject<Item> LATEX_GRAY =
            ITEMS.register("latex_gray", LatexGray::new);
    public static final RegistryObject<Item> LATEX_INGOT =
            ITEMS.register("latex_ingot", LatexIngot::new);
    public static final RegistryObject<Item> UNBAKED_LATEX_INGOT =
            ITEMS.register("unbaked_latex_ingot", UnbakedLatexIngot::new);
    public static final EnchantmentCategory MELEE =
            EnchantmentCategory.create(
                    "melee",
                    item -> item instanceof SwordItem || item instanceof AxeItem
            );
    public static final RegistryObject<CreativeModeTab> EE =
            CREATIVE_MODE_TABS.register("easter_egg", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.changede2"))
                            .icon(() -> new ItemStack(resolveProjecteTotem("MATTER_TOTEM_OF_UNDYING_TRUE")))
                            .displayItems((parameters, output) -> {
                                output.accept(resolveProjecteTotem("DARK_MATTER_TOTEM_OF_UNDYING"));
                                output.accept(resolveProjecteTotem("RED_MATTER_TOTEM_OF_UNDYING"));
                                output.accept(resolveProjecteTotem("MATTER_TOTEM_OF_UNDYING_TRUE"));
                            })
                            .build()
            );
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



    public static final RegistryObject<CreativeModeTab> BASIC =
            CREATIVE_MODE_TABS.register("basic", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.changede1"))
                            .icon(() -> new ItemStack(CERegister.LATEX_INGOT.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(CERegister.INACTIVE_DARK_LATEX.get());
                                output.accept(CERegister.INACTIVE_WHITE_LATEX.get());
                                output.accept(CERegister.LATEX_GRAY.get());
                                output.accept(CERegister.LATEX_INGOT.get());
                                output.accept(CERegister.UNBAKED_LATEX_INGOT.get());
                            })
                            .build()
            );

    public static final RegistryObject<CreativeModeTab> EXTRA =
            CREATIVE_MODE_TABS.register("extra", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.changede3"))
                            .icon(() -> new ItemStack(CELPRegister.ELECTRIC_FURNACE_ITEM.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(CELPRegister.BASIC_WIRE_ITEM.get());
                                output.accept(CELPRegister.BASIC_GENERATOR_ITEM.get());
                                output.accept(CELPRegister.ELECTRIC_FURNACE_ITEM.get());
                            })
                            .build()
            );

    private static boolean hasProjectE() {
        return ModList.get().isLoaded(PROJECT_E_MODID);
    }

    private static Item resolveProjecteTotem(String fieldName) {
        if (!hasProjectE()) {
            return Items.AIR;
        }

        try {
            Class<?> pTotemClass = Class.forName(PTOTEM_CLASS);
            Object registryObject = pTotemClass.getField(fieldName).get(null);
            Object item = registryObject.getClass().getMethod("get").invoke(registryObject);
            if (item instanceof Item resolvedItem) {
                return resolvedItem;
            }
        } catch (Throwable ignored) {
            // Optional dependency unavailable or class init failed; use safe fallback.
        }

        return Items.TOTEM_OF_UNDYING;
    }

    public static GameRules.Key<GameRules.BooleanValue> LATEX_START;

    public static void register() {
        LATEX_START = GameRules.register(
                "latex_start",
                GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false)
        );
    }
}
