package github.com.gengyoubo;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
        
        public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "changede");

        public static final RegistryObject<CreativeModeTab> BASIC =
        CREATIVE_MODE_TABS.register("basic", () ->
        CreativeModeTab.builder()
        .title(Component.translatable("creativetab.changede1"))
        .icon(() -> new ItemStack(Items.AIR))
        .displayItems((parameters, output) -> {
        output.accept(ModItem.INACTIVE_DARK_LATEX.get());
        output.accept(ModItem.INACTIVE_WHITE_LATEX.get());
        output.accept(ModItem.LATEX_GRAY.get());
        output.accept(ModItem.LATEX_INGOT.get());
        output.accept(ModItem.UNBAKED_LATEX_INGOT.get());
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
}