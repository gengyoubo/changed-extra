package github.com.gengyoubo.CE.verification;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** No links to Mekanism or AE2: validates boot and absence when Generators is missing. */
public final class FissionOptionalChecks {
    public static void started(ServerStartedEvent event) {
        try {
            for (String id : new String[]{"advanced_fission_cooling", "advanced_uranium_processor_x", "advanced_uranium_processor_y", "fission_emergency_scram", "fission_me_interface"}) {
                var key = ResourceLocation.parse("changede:" + id);
                if (ForgeRegistries.BLOCKS.containsKey(key) || ForgeRegistries.ITEMS.containsKey(key)) throw new AssertionError("Optional block registered: " + id);
                if (event.getServer().getRecipeManager().byKey(ResourceLocation.parse("changede:fission/" + id)).isPresent()) throw new AssertionError("Optional recipe loaded: " + id);
            }
            if (event.getServer().getPackRepository().getSelectedIds().contains("changede:fission_upgrades")) throw new AssertionError("Optional pack loaded");
            Files.writeString(Path.of("fission-checks.txt"), "PASS: dedicated server without Mekanism Generators or AE2; no fission blocks, items, recipes or pack\n");
        } catch (Throwable error) {
            github.com.gengyoubo.CE.changede.LOGGER.error("FISSION OPTIONAL CHECKS FAILED", error);
            try { Files.writeString(Path.of("fission-checks.txt"), "FAIL: " + error + "\n"); } catch (Exception ignored) { }
        } finally { event.getServer().execute(() -> event.getServer().halt(false)); }
    }
    private FissionOptionalChecks() {}
}
