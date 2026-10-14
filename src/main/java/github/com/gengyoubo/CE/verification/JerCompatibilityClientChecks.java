package github.com.gengyoubo.CE.verification;

import github.com.gengyoubo.CE.compat.jer.JerCreateWorldGenCompat;
import jeresources.api.util.BiomeHelper;
import jeresources.jei.JEIConfig;
import jeresources.platform.Services;
import jeresources.registry.WorldGenRegistry;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Opt-in Forge/JEI regression check in a disposable flat world, followed by automatic exit. */
@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT)
public final class JerCompatibilityClientChecks {
    private static boolean opened;
    private static boolean finished;
    private static final Path RESULT = Path.of("jer-compat-checks.txt");

    private JerCompatibilityClientChecks() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("changede.verifyJerCompat") || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (!opened && minecraft.screen instanceof TitleScreen && minecraft.getOverlay() == null) {
                opened = true;
                if (!ModList.get().isLoaded("jeresources")) {
                    // With both optional mods omitted, all four optional mixins must remain unloaded.
                    finish(minecraft, "PASS: client starts without JER or JER Integration");
                    return;
                }
                // Establish that the original Changed/vanilla bootstrap failure still exists: the patch
                // must bypass it inside JER, not silence global registry validation.
                boolean reproduced = false;
                try { VanillaRegistries.createLookup(); }
                catch (IllegalStateException expected) {
                    reproduced = java.util.Arrays.stream(expected.getSuppressed())
                            .anyMatch(error -> error.getMessage().contains("changed:white_latex_forest"));
                }
                if (!reproduced) throw new AssertionError("Expected original Changed biome lookup failure");
                if (!JerCreateWorldGenCompat.zincOreValues().equals(
                        Map.of("clusterSize", 12, "frequency", 8, "minHeight", -63, "maxHeight", 70))) {
                    throw new AssertionError("Create's packaged zinc defaults must work before connecting to a server");
                }
                // This run's working directory is isolated under build; never open a user save.
                String world = "jer-check-" + System.currentTimeMillis();
                var settings = new LevelSettings("JER compatibility check", GameType.CREATIVE, false,
                        Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                minecraft.createWorldOpenFlows().createFreshLevel(world, settings,
                        new WorldOptions(0L, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET)
                                .getOrThrow(WorldPresets.FLAT).createWorldDimensions());
                return;
            }
            if (!opened || minecraft.level == null || minecraft.player == null || minecraft.getOverlay() != null) return;
            var field = JEIConfig.class.getDeclaredField("jeiRuntime");
            field.setAccessible(true);
            var runtime = (IJeiRuntime) field.get(null);
            if (runtime == null) return;
            var biomes = minecraft.getConnection().registryAccess().registryOrThrow(Registries.BIOME);
            if (BiomeHelper.getBiome(Biomes.PLAINS) != biomes.getOrThrow(Biomes.PLAINS)) {
                throw new AssertionError("JER must return the actual connected world's biome instance");
            }
            if (BiomeHelper.getAllBiomes().size() != biomes.size()
                    || !BiomeHelper.getBiomes(Biomes.PLAINS).contains(biomes.getOrThrow(Biomes.PLAINS))) {
                throw new AssertionError("JER list queries must use the connected biome registry");
            }
            long categories = runtime.getRecipeManager().createRecipeCategoryLookup().includeHidden().get()
                    .filter(category -> category.getRecipeType().getUid().getNamespace().equals("jeresources")).count();
            if (categories != 6) throw new AssertionError("Expected all six JER categories, got " + categories);
            if (runtime.getRecipeManager().createRecipeLookup(JEIConfig.WORLD_GEN_TYPE).includeHidden().get().count() == 0) {
                throw new AssertionError("JER must expose ore recipes through JEI");
            }
            boolean integration = ModList.get().isLoaded("jerintegration");
            if (integration) {
                var values = JerCreateWorldGenCompat.zincOreValues();
                if (!values.equals(Map.of("clusterSize", 12, "frequency", 8, "minHeight", -63, "maxHeight", 70))) {
                    throw new AssertionError("Unexpected Create zinc feature values: " + values);
                }
            }
            int count = WorldGenRegistry.getInstance().getWorldGen().size();
            for (int reload = 0; reload < 2; reload++) {
                Services.PLATFORM.getProxy().initCompatibility();
                if (WorldGenRegistry.getInstance().getWorldGen().size() != count) {
                    throw new AssertionError("JER registry reload duplicates or loses entries");
                }
                if (integration) {
                    for (String ore : new String[]{"zinc_ore", "deepslate_zinc_ore"}) {
                        long matches = WorldGenRegistry.getInstance().getWorldGen().stream()
                                .filter(entry -> net.minecraftforge.registries.ForgeRegistries.ITEMS
                                        .getKey(entry.getBlock().getItem())
                                        .equals(ResourceLocation.fromNamespaceAndPath("create", ore))).count();
                        if (matches != 1) throw new AssertionError("Expected one Create " + ore + " chart, got " + matches);
                    }
                }
            }
            finish(minecraft, "PASS: original Changed registry failure reproduced; live JER biome queries; six JEI categories; "
                    + count + " ore entries; two registry reloads; JER Integration present=" + integration);
        } catch (Throwable error) {
            finished = true;
            LogManager.getLogger("changede-jer-checks").error("JER COMPATIBILITY CHECKS FAILED", error);
            try { Files.writeString(RESULT, "FAIL: " + error + "\n"); } catch (Exception ignored) {}
            minecraft.stop();
        }
    }

    private static void finish(Minecraft minecraft, String result) throws Exception {
        finished = true;
        Files.writeString(RESULT, result + "\n");
        LogManager.getLogger("changede-jer-checks").info(result);
        minecraft.stop();
    }
}
