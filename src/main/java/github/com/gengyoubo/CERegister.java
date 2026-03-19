package github.com.gengyoubo;

import github.com.gengyoubo.enchantment.*;
import github.com.gengyoubo.items.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CERegister {
    //物品
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "changede");
    public static final RegistryObject<Item> INACTIVE_DARK_LATEX =
            ITEMS.register("inactive_dark_latex",
                    InactiveDarkLatex::new);
    public static final RegistryObject<Item> INACTIVE_WHITE_LATEX =
            ITEMS.register("inactive_white_latex",
                    InactiveWhiteLatex::new);

    public static final RegistryObject<Item> LATEX_GRAY =
            ITEMS.register("latex_gray",
                    LatexGray::new);

    public static final RegistryObject<Item> LATEX_INGOT =
            ITEMS.register("latex_ingot",
                    LatexIngot::new);

    public static final RegistryObject<Item> UNBAKED_LATEX_INGOT =
            ITEMS.register("unbaked_latex_ingot",
                    UnbakedLatexIngot::new);
    //附魔
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
    //创造模式物品栏tab
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "changede");

    public static final RegistryObject<CreativeModeTab> BASIC =
            CREATIVE_MODE_TABS.register("basic", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.changede1"))
                            .icon(() -> new ItemStack(Items.AIR))
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
                            .icon(() -> new ItemStack(Items.AIR))
                            .displayItems((parameters, output) -> {

                            })
                            .build()
            );
    public static final RegistryObject<CreativeModeTab> EE =
            CREATIVE_MODE_TABS.register("easter_egg", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("creativetab.changede2"))
                            .icon(() -> new ItemStack(Items.AIR))
                            .displayItems((parameters, output) -> {

                            })
                            .build()
            );
    //游戏规则
    public static void register() {
        LATEX_START = GameRules.register(
                "latex_start", // 规则ID（/gamerule 用）
                GameRules.Category.PLAYER, // 分类
                GameRules.BooleanValue.create(false) // 默认值
        );
    }
    public static GameRules.Key<GameRules.BooleanValue> LATEX_START;

    //其他
    public static final EnchantmentCategory MELEE =
            EnchantmentCategory.create(
                    "melee",
                    item -> item instanceof SwordItem || item instanceof AxeItem
            );}
