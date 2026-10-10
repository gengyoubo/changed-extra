package github.com.gengyoubo.CE.verification;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** A negative-dependency test with no references to Create or any burner implementation class. */
public final class LatexBurnerOptionalChecks {
    private LatexBurnerOptionalChecks() {}
    public static void started(ServerStartedEvent event) {
        try {
            boolean absent = !ModList.get().isLoaded("create")
                    && !ForgeRegistries.BLOCKS.containsKey(ResourceLocation.parse("changede:latex_burner"))
                    && !ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse("changede:latex_burner"))
                    && !ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse("changede:empty_latex_burner"))
                    && !ForgeRegistries.BLOCK_ENTITY_TYPES.containsKey(ResourceLocation.parse("changede:latex_burner"));
            if (!absent) throw new AssertionError("Create must be absent and burner content must not register");
            java.nio.file.Files.writeString(java.nio.file.Path.of("latex-burner-optional-checks.txt"), "PASS: dedicated server without Create\n");
            github.com.gengyoubo.CE.changede.LOGGER.info("LATEX BURNER OPTIONAL CHECKS PASSED");
        } catch (Exception error) { throw new RuntimeException(error); }
        finally { event.getServer().execute(() -> event.getServer().halt(false)); }
    }
}
