package github.com.gengyoubo.CE.init;

import github.com.gengyoubo.CE.LP.init.CELPItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CECreativeModeTab {
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
                                            output.accept(CEItem.WHITE_LATEX_INGOT.get());
                                            output.accept(CEItem.DARK_LATEX_INGOT.get());
                                            output.accept(CEItem.LATEX_SPEAR.get());
                                            output.accept(CEItem.IRIDIUM_INGOT.get());
                                            output.accept(CEItem.PAINITE_INGOT.get());
                                            output.accept(CEItem.DARK_LATEX_MORPHIC_CRYSTAL_ORE.get());
                                            output.accept(CEItem.WHITE_LATEX_MORPHIC_CRYSTAL_ORE.get());
                                            output.accept(CEItem.MORPHIC_CRYSTAL.get());
                                            output.accept(CEItem.MORPHIC_CRYSTAL_ALLOY.get());
                                            output.accept(CEItem.UNBAKED_LATEX_INGOT.get());
                                            output.accept(CEItem.DARK_LATEX_LOG.get());
                                            output.accept(CEItem.DARK_LATEX_PLANKS.get());
                                            output.accept(CEItem.DARK_LATEX_LEAVES.get());
                                            output.accept(CEItem.DARK_LATEX_STONE.get());
                                            output.accept(CEItem.DARK_LATEX_COBBLESTONE.get());
                                            output.accept(CEItem.WHITE_LATEX_LOG.get());
                                            output.accept(CEItem.WHITE_LATEX_PLANKS.get());
                                            output.accept(CEItem.WHITE_LATEX_LEAVES.get());
                                            output.accept(CEItem.WHITE_LATEX_STONE.get());
                                            output.accept(CEItem.WHITE_LATEX_COBBLESTONE.get());
                                            CEWoodFamilies.ALL.forEach(family -> family.items.forEach(item -> output.accept(item.get())));
                                            output.accept(CEItem.LUMINARA_GRASS_BLOCK.get());
                                            output.accept(CEItem.LATEX_PAINTING_PORTAL.get());
                                            output.accept(CEItem.PEACH.get());
                                            output.accept(CEItem.ENCHANTED_GOLDEN_ORANGE.get());
                                            output.accept(CEItem.CHAIN_INGOT.get());
                                        })
                                        .build()
                        );
                EXTRA = CREATIVE_MODE_TABS.register("extra", () ->
                                CreativeModeTab.builder()
                                        .title(Component.translatable("creativetab.changede3"))
                                        .icon(() -> new ItemStack(CELPItem.ELECTRIC_FURNACE_ITEM.get()))
                                        .displayItems((parameters, output) -> {
                                            output.accept(CELPItem.BASIC_WIRE_ITEM.get());
                                            output.accept(CELPItem.INGOT_FILLER_ITEM.get());
                                            output.accept(CELPItem.WLP_PIPE_ITEM.get());
                                            output.accept(CELPItem.DLP_PIPE_ITEM.get());
                                            output.accept(CEItem.PAINITE_WORKBENCH_CORE.get());
                                            output.accept(CEItem.MORPHIC_CRYSTAL_CORE.get());
                                            output.accept(CEItem.LATEX_SKILL_RESEARCH_TABLE.get());
                                            output.accept(CELPItem.BASIC_ITEM_PIPE_ITEM.get());
                                            output.accept(CELPItem.BASIC_FLUID_PIPE_ITEM.get());
                                            output.accept(CELPItem.BASIC_PUMP_ITEM.get());
                                            output.accept(CELPItem.BASIC_LATEX_FLUID_GENERATOR_ITEM.get());
                                            output.accept(CELPItem.PIPE_WRENCH.get());
                                            output.accept(CELPItem.BASIC_GENERATOR_ITEM.get());
                                            output.accept(CELPItem.BASIC_CRYSTAL_GENERATOR_ITEM.get());
                                            output.accept(CELPItem.ELECTRIC_FURNACE_ITEM.get());
                                            output.accept(CELPItem.BASIC_ALLOY_FURNACE_ITEM.get());
                                            output.accept(CELPItem.BASIC_LATEX_PURIFIER_ITEM.get());
                                            output.accept(CELPItem.LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK_ITEM.get());
                                            output.accept(CELPItem.SPACE_TOWER_ITEM.get());
                                            output.accept(CELPItem.DIMENSION_SPACE_TOWER_ITEM.get());
                                            output.accept(CELPItem.WHITE_LATEX_POWER_CONVERTER_ITEM.get());
                                            output.accept(CELPItem.DARK_LATEX_POWER_CONVERTER_ITEM.get());
                                            output.accept(CELPItem.ORANGE_PRODUCER_ITEM.get());
                                            output.accept(CELPItem.MIMIC_YUFENG_WINGS.get());
                                        })
                                        .build()
                        );
                EE = CREATIVE_MODE_TABS.register("easter_egg", () ->
                                CreativeModeTab.builder()
                                        .title(Component.translatable("creativetab.changede2"))
                                        .icon(() -> new ItemStack(CEItem.PLATE.get()))
                                        .displayItems((parameters, output) -> {
                                            safeAccept(output, OptionalProjectEItems.resolveTotem("DARK_MATTER_TOTEM_OF_UNDYING"));
                                            safeAccept(output, OptionalProjectEItems.resolveTotem("RED_MATTER_TOTEM_OF_UNDYING"));
                                            safeAccept(output, OptionalProjectEItems.resolveTotem("MATTER_TOTEM_OF_UNDYING_TRUE"));
                                            output.accept(CEItem.PLATE.get());
                                            output.accept(CEItem.PLATE_HELMET.get());
                                            output.accept(CEItem.PLATE_CHESTPLATE.get());
                                            output.accept(CEItem.PLATE_LEGGINGS.get());
                                            output.accept(CEItem.PLATE_BOOTS.get());
                                            output.accept(CEItem.RIDING_STICK.get());
                                            output.accept(CEItem.RIDDEN_STICK.get());
                                            output.accept(CEItem.REMOTE_RIDING_STICK.get());
                                        })
                                        .build()
                        );
            }

    private static void safeAccept(CreativeModeTab.Output output, Item item) {
        if (item != Items.AIR) {
            output.accept(item);
        }
    }
}
