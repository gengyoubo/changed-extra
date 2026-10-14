package github.com.gengyoubo.CE.verification;

import github.com.gengyoubo.CE.compat.jer.JerOreClientState;
import github.com.gengyoubo.CE.compat.jer.JerOreNetwork;
import jeresources.api.util.BiomeHelper;
import jeresources.jei.JEIConfig;
import jeresources.platform.Services;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
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

/** Real Forge/JEI checks in an opt-in disposable world; never touches a user's save. */
@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT)
public final class JerCompatibilityClientChecks {
    private static int stage;
    private static boolean finished;
    private static long initialCount;
    private static int initialSnapshotCount;
    private static java.util.List<github.com.gengyoubo.CE.compat.jer.OreGenerationData> beforeRepeat;
    private static final Path RESULT = Path.of("jer-compat-checks.txt");
    private JerCompatibilityClientChecks() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("changede.verifyJerCompat") || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        try {
            if (stage == 0 && minecraft.getOverlay() == null && minecraft.screen != null
                    && (minecraft.screen instanceof TitleScreen || minecraft.screen.getClass().getSimpleName().equals("ContentWarningScreen"))) {
                stage = 1;
                if (!ModList.get().isLoaded("jeresources")) {
                    finish(minecraft, "PASS: client starts without JER or JER Integration");
                    return;
                }
                String world = "jer-check-" + System.currentTimeMillis();
                var pack = minecraft.getLevelSource().getBaseDir().resolve(world).resolve("datapacks/jer-regression");
                Files.createDirectories(pack.resolve("data/changede/worldgen/configured_feature"));
                Files.createDirectories(pack.resolve("data/changede/worldgen/placed_feature"));
                Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":15,\"description\":\"JER dormant-feature regression\"}}");
                Files.writeString(pack.resolve("data/changede/worldgen/configured_feature/jer_check_dormant.json"),
                        "{\"type\":\"minecraft:ore\",\"config\":{\"size\":32,\"discard_chance_on_air_exposure\":0,\"targets\":[{\"target\":{\"predicate_type\":\"minecraft:tag_match\",\"tag\":\"minecraft:stone_ore_replaceables\"},\"state\":{\"Name\":\"changede:dark_latex_morphic_crystal_ore\"}}]}}");
                Files.writeString(pack.resolve("data/changede/worldgen/placed_feature/jer_check_dormant.json"),
                        "{\"feature\":\"changede:jer_check_dormant\",\"placement\":[{\"type\":\"minecraft:count\",\"count\":99},{\"type\":\"minecraft:in_square\"},{\"type\":\"minecraft:height_range\",\"height\":{\"type\":\"minecraft:uniform\",\"min_inclusive\":{\"absolute\":40},\"max_inclusive\":{\"absolute\":40}}},{\"type\":\"minecraft:biome\"}]}");
                var settings = new LevelSettings("JER compatibility check", GameType.CREATIVE, false,
                        Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                minecraft.options.renderDistance().set(2);
                minecraft.options.simulationDistance().set(5);
                minecraft.createWorldOpenFlows().createFreshLevel(world, settings,
                        new WorldOptions(0L, false, false), WorldPresets::createNormalWorldDimensions);
                return;
            }
            if (stage == 0 || minecraft.level == null || minecraft.player == null || minecraft.getOverlay() != null) return;
            var field = JEIConfig.class.getDeclaredField("jeiRuntime");
            field.setAccessible(true);
            var runtime = (IJeiRuntime) field.get(null);
            if (runtime == null || JerOreClientState.snapshot().isEmpty()) return;
            if (stage == 1) {
                var biomes = minecraft.getConnection().registryAccess().registryOrThrow(Registries.BIOME);
                check(BiomeHelper.getBiome(Biomes.PLAINS) == biomes.getOrThrow(Biomes.PLAINS), "JER must use live biome instances");
                check(BiomeHelper.getAllBiomes().size() == biomes.size(), "JER all-biomes query");
                check(BiomeHelper.getBiomes(Biomes.PLAINS).contains(biomes.getOrThrow(Biomes.PLAINS)), "JER biome-list query");
                long categories = runtime.getRecipeManager().createRecipeCategoryLookup().includeHidden().get()
                        .filter(category -> category.getRecipeType().getUid().getNamespace().equals("jeresources")).count();
                check(categories == 6, "Expected all six JER categories, got " + categories);
                for (String block : new String[]{"changed_addon:deepslate_iridium_ore", "changed_addon:deepslate_painite_ore",
                        "changede:dark_latex_morphic_crystal_ore", "changede:white_latex_morphic_crystal_ore",
                        "create:zinc_ore", "create:deepslate_zinc_ore"}) check(hasChart(runtime, block), "Missing JEI ore chart: " + block);
                check(JerOreClientState.snapshot().stream().anyMatch(data -> data.block().getNamespace().equals("tconstruct")),
                        "Standard ore features from another mod must be discovered");
                check(hasChart(runtime, "tconstruct:cobalt_ore"), "Tinkers cobalt ore must be queryable in JEI");
                if (ModList.get().isLoaded("mekanism")) {
                    for (String ore : new String[]{"osmium", "tin", "lead", "uranium", "fluorite"}) {
                        check(hasChart(runtime, "mekanism:" + ore + "_ore"), "Missing Mekanism ore chart: " + ore);
                        check(hasChart(runtime, "mekanism:deepslate_" + ore + "_ore"), "Missing deepslate Mekanism ore chart: " + ore);
                    }
                    check(JerOreClientState.snapshot().stream().filter(data -> data.block().getNamespace().equals("mekanism"))
                            .allMatch(data -> data.dimension().toString().equals("minecraft:overworld")), "Mekanism ores must have their actual overworld restriction");
                }
                check(jeresources.util.TranslationHelper.tryDimensionTranslate("changede:latex_space")
                        .equals(net.minecraft.network.chat.Component.translatable("dimension.changede.latex_space").getString()), "JER latex dimension title must translate");
                check(!jeresources.util.TranslationHelper.tryDimensionTranslate("twilightforest:twilight_forest")
                        .equals("twilightforest:twilight_forest"), "JER Twilight Forest title must translate");
                check(JerOreClientState.snapshot().stream().noneMatch(data -> data.block().getNamespace().equals("changede")
                        && data.dimension().toString().equals("minecraft:overworld")), "Dormant/unattached feature must not expose CE ores in the overworld");
                check(minecraft.getSingleplayerServer().registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
                        .containsKey(ResourceLocation.parse("changede:jer_check_dormant")), "Dormant regression fixture must actually be loaded");
                initialCount = recipeCount(runtime);
                initialSnapshotCount = JerOreClientState.snapshot().size();
                stage = 2;
                minecraft.getSingleplayerServer().execute(() -> {
                    var server = minecraft.getSingleplayerServer();
                    server.overworld().getGameRules().getRule(net.foxyas.changedaddon.init.ChangedAddonGameRules.PAINITE_GENERATION).set(false, server);
                    com.simibubi.create.infrastructure.config.AllConfigs.common().worldGen.disable.set(true);
                    if (ModList.get().isLoaded("mekanism")) setOsmiumGeneration(false);
                });
                return;
            }
            if (stage == 2) {
                if (JerOreClientState.snapshot().stream().anyMatch(data -> data.block().toString().equals("changed_addon:deepslate_painite_ore")
                        && data.dimension().toString().equals("minecraft:overworld"))) return;
                check(recipeCount(runtime) < initialCount, "Disabled Painite charts must be hidden in JEI");
                check(JerOreClientState.snapshot().stream().noneMatch(data -> data.block().getNamespace().equals("create")), "Disabled Create worldgen must not expose zinc charts");
                check(!hasChart(runtime, "create:zinc_ore"), "Disabled zinc charts must disappear from JEI");
                if (ModList.get().isLoaded("mekanism")) {
                    check(!hasChart(runtime, "mekanism:osmium_ore"), "Disabled osmium charts must disappear from JEI");
                    check(hasChart(runtime, "mekanism:tin_ore"), "Disabling osmium must preserve other Mekanism ores");
                }
                stage = 3;
                minecraft.getSingleplayerServer().execute(() -> {
                    var server = minecraft.getSingleplayerServer();
                    server.overworld().getGameRules().getRule(net.foxyas.changedaddon.init.ChangedAddonGameRules.PAINITE_GENERATION).set(true, server);
                    com.simibubi.create.infrastructure.config.AllConfigs.common().worldGen.disable.set(false);
                    if (ModList.get().isLoaded("mekanism")) setOsmiumGeneration(true);
                });
                return;
            }
            if (stage == 3) {
                if (JerOreClientState.snapshot().size() != initialSnapshotCount) return;
                check(recipeCount(runtime) == initialCount, "Re-enabling generation must restore charts without duplicates");
                for (int reload = 0; reload < 2; reload++) Services.PLATFORM.getProxy().initCompatibility();
                stage = 4;
                beforeRepeat = JerOreClientState.snapshot();
                JerOreNetwork.CHANNEL.sendToServer(new JerOreNetwork.Request());
                return;
            }
            if (JerOreClientState.snapshot() == beforeRepeat) return;
            check(recipeCount(runtime) == initialCount, "Repeated sync must not double-count ore charts");
            finish(minecraft, "PASS: live JER biome queries; six categories; Addon/CE/Create/other-mod ores; "
                    + initialSnapshotCount + " verified charts; translated dimension titles; dormant feature omitted; Painite/Create/Mekanism disable/re-enable; reload and repeated sync; Integration="
                    + ModList.get().isLoaded("jerintegration") + "; Mekanism=" + ModList.get().isLoaded("mekanism"));
        } catch (Throwable error) {
            finished = true;
            LogManager.getLogger("changede-jer-checks").error("JER COMPATIBILITY CHECKS FAILED", error);
            try { Files.writeString(RESULT, "FAIL: " + error + "\n"); } catch (Exception ignored) {}
            minecraft.stop();
        }
    }

    private static boolean hasChart(IJeiRuntime runtime, String block) {
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(block));
        return runtime.getRecipeManager().createRecipeLookup(JEIConfig.WORLD_GEN_TYPE).get()
                .anyMatch(recipe -> recipe.getBlocks().stream().anyMatch(stack -> stack.getItem() == item));
    }
    private static long recipeCount(IJeiRuntime runtime) { return runtime.getRecipeManager().createRecipeLookup(JEIConfig.WORLD_GEN_TYPE).get().count(); }
    private static void setOsmiumGeneration(boolean enabled) {
        // Regression-only access to Mekanism's private per-ore switch; production uses its public suppliers.
        try {
            var world = mekanism.common.config.MekanismConfig.world;
            var field = world.getClass().getDeclaredField("ores");
            field.setAccessible(true);
            var ore = ((java.util.Map<?, ?>) field.get(world)).get(mekanism.common.resource.ore.OreType.OSMIUM);
            var switchField = ore.getClass().getDeclaredField("shouldGenerate");
            switchField.setAccessible(true);
            ((mekanism.common.config.value.CachedBooleanValue) switchField.get(ore)).set(enabled);
        } catch (ReflectiveOperationException error) { throw new AssertionError("Cannot toggle Mekanism ore fixture", error); }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void finish(Minecraft minecraft, String result) throws Exception {
        finished = true;
        Files.writeString(RESULT, result + "\n");
        LogManager.getLogger("changede-jer-checks").info(result);
        minecraft.stop();
    }
}
