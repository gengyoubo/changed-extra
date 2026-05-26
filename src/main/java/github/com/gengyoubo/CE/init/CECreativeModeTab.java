package github.com.gengyoubo.CE.init;

import github.com.gengyoubo.CE.LP.init.CELPItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CECreativeModeTab {
    private static final String PROJECT_E_MODID = "projecte";
    private static final String PTOTEM_CLASS = "github.com.gengyoubo.CE.projectextended.PTotemOfUndying";
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS;
    public static final RegistryObject<CreativeModeTab> BASIC;
    public static final RegistryObject<CreativeModeTab> EXTRA;
    public static final RegistryObject<CreativeModeTab> EE;

    static {
        CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "changede");
                BASIC = CREATIVE_MODE_TABS.register("basic", () ->
                                CreativeModeTab.builder()
                                        .title(Component.translatable("creativetab.changede1"))
                                        .icon(() -> new ItemStack(CEItem.LATEX_INGOT.get()))
                                        .displayItems((parameters, output) -> {
                                            output.accept(CEItem.INACTIVE_DARK_LATEX.get());
                                            output.accept(CEItem.INACTIVE_WHITE_LATEX.get());
                                            output.accept(CEItem.LATEX_GRAY.get());
                                            output.accept(CEItem.LATEX_INGOT.get());
                                            output.accept(CEItem.UNBAKED_LATEX_INGOT.get());
                                        })
                                        .build()
                        );
                EXTRA = CREATIVE_MODE_TABS.register("extra", () ->
                                CreativeModeTab.builder()
                                        .title(Component.translatable("creativetab.changede3"))
                                        .icon(() -> new ItemStack(CELPItem.ELECTRIC_FURNACE_ITEM.get()))
                                        .displayItems((parameters, output) -> {
                                            output.accept(CELPItem.BASIC_WIRE_ITEM.get());
                                            output.accept(CELPItem.BASIC_GENERATOR_ITEM.get());
                                            output.accept(CELPItem.ELECTRIC_FURNACE_ITEM.get());
                                            output.accept(CELPItem.LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK_ITEM.get());
                                            output.accept(CELPItem.SPACE_TOWER_ITEM.get());
                                            output.accept(CELPItem.MIMIC_YUFENG_WINGS.get());
                                        })
                                        .build()
                        );
                EE = CREATIVE_MODE_TABS.register("easter_egg", () ->
                                CreativeModeTab.builder()
                                        .title(Component.translatable("creativetab.changede2"))
                                        .icon(() -> new ItemStack(Items.AIR))
                                        .displayItems((parameters, output) -> {
                                            safeAccept(output, resolveProjecteTotem("DARK_MATTER_TOTEM_OF_UNDYING"));
                                            safeAccept(output, resolveProjecteTotem("RED_MATTER_TOTEM_OF_UNDYING"));
                                            safeAccept(output, resolveProjecteTotem("MATTER_TOTEM_OF_UNDYING_TRUE"));
                                        })
                                        .build()
                        );
            }

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

    private static void safeAccept(CreativeModeTab.Output output, Item item) {
        if (item != Items.AIR) {
            output.accept(item);
        }
    }
}
