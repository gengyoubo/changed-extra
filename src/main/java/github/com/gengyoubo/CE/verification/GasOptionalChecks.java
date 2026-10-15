package github.com.gengyoubo.CE.verification;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Safe to load with no Mek classes on the runtime classpath. */
public final class GasOptionalChecks {
    public static void started(ServerStartedEvent event) {
        try {
            for (String id : new String[]{"reverser", "basic_gas_pipe"}) {
                var key = ResourceLocation.parse("changede:" + id);
                if (ForgeRegistries.BLOCKS.containsKey(key) || ForgeRegistries.ITEMS.containsKey(key)) throw new AssertionError("Unexpected optional block: " + id);
            }
            if (event.getServer().getPackRepository().getSelectedIds().contains("changede:gas_technology")) throw new AssertionError("Optional gas pack selected without Mek");
            if (event.getServer().getRecipeManager().byKey(ResourceLocation.parse("changede:reversing/spent_nuclear_waste_to_fissile_fuel")).isPresent()) throw new AssertionError("Optional gas recipe loaded without Mek");
            Files.writeString(Path.of("gas-checks.txt"), "PASS: dedicated server without Mek; gas blocks, items and recipes absent\n");
        } catch (Throwable error) {
            github.com.gengyoubo.CE.changede.LOGGER.error("GAS OPTIONAL CHECKS FAILED", error);
            try { Files.writeString(Path.of("gas-checks.txt"), "FAIL: " + error + "\n"); } catch (Exception ignored) {}
        } finally { event.getServer().execute(() -> event.getServer().halt(false)); }
    }
    private GasOptionalChecks() {}
}
