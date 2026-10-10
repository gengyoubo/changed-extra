package github.com.gengyoubo.CE.verification;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;

/** Verifies optional compatibility without linking any Tinkers class. */
public final class LatexTinkersOptionalChecks {
    private LatexTinkersOptionalChecks() {}
    public static void started(ServerStartedEvent event) {
        try {
            if (ModList.get().isLoaded("tconstruct")) throw new AssertionError("Tinkers must be absent in this check");
            if (event.getServer().getPackRepository().getSelectedIds().contains("changede:latex_smeltery"))
                throw new AssertionError("Optional smeltery loot pack must be absent");
            for (String name : new String[]{"latex_combustion_core", "latex_fuel_tank"}) {
                if (net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(ResourceLocation.parse("changede:" + name))
                        || net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse("changede:" + name)))
                    throw new AssertionError("Optional smeltery block must not register: " + name);
            }
            for (String path : new String[]{"smeltery/latex_combustion_core", "smeltery/latex_fuel_tank", "smeltery/fuel/dark_latex", "smeltery/fuel/white_latex"})
                if (event.getServer().getRecipeManager().byKey(ResourceLocation.parse("changede:" + path)).isPresent())
                    throw new AssertionError("Optional smeltery recipe must not load: " + path);
            for (String name : new String[]{"latex_plasticity", "latex_resilience"}) {
                for (String prefix : new String[]{"tools/modifiers/", "tools/modifiers/salvage/"}) {
                    if (event.getServer().getRecipeManager().byKey(ResourceLocation.parse("changede:" + prefix + name)).isPresent())
                        throw new AssertionError("Conditional Tinkers recipe must be absent: " + prefix + name);
                }
            }
            java.nio.file.Files.writeString(java.nio.file.Path.of("latex-tinkers-optional-checks.txt"), "PASS: dedicated server without Tinkers; latex modifier and salvage recipes absent\n");
            github.com.gengyoubo.CE.changede.LOGGER.info("LATEX TINKERS OPTIONAL CHECKS PASSED");
        } catch (Throwable error) {
            github.com.gengyoubo.CE.changede.LOGGER.error("LATEX TINKERS OPTIONAL CHECKS FAILED", error);
            try { java.nio.file.Files.writeString(java.nio.file.Path.of("latex-tinkers-optional-checks.txt"), "FAIL: " + error + "\n"); } catch (Exception ignored) {}
        } finally { event.getServer().execute(() -> event.getServer().halt(false)); }
    }
}
