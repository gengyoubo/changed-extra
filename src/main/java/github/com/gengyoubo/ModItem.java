package github.com.gengyoubo;

import github.com.gengyoubo.items.InactiveDarkLatex;
import github.com.gengyoubo.items.InactiveWhiteLatex;
import github.com.gengyoubo.items.LatexIngot;
import github.com.gengyoubo.items.UnbakedLatexIngot;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModItem {
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
                    InactiveWhiteLatex::new);

    public static final RegistryObject<Item> LATEX_INGOT =
            ITEMS.register("latex_ingot",
                    LatexIngot::new);

    public static final RegistryObject<Item> UNBAKED_LATEX_INGOT =
            ITEMS.register("unbaked_latex_ingot",
                    UnbakedLatexIngot::new);
}
