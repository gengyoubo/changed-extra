package github.com.gengyoubo.CE.compat.tinkers;

import net.ltxprogrammer.changed.entity.TransfurContext;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.adding.ModifierRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.recipe.ModifierRemovalRecipe;

import java.nio.file.Files;
import java.nio.file.Path;

/** Opt-in integration checks in a disposable, player-free server. */
public final class LatexTinkersRegressionChecks {
    private static int assertions;
    private LatexTinkersRegressionChecks() {}
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        assertions++;
        github.com.gengyoubo.CE.changede.LOGGER.info("TINKERS PASS: {}", message);
    }
    public static void started(ServerStartedEvent event) {
        try {
            check(event.getServer().getPlayerCount() == 0, "Disposable server has no real players");
            ServerLevel level = event.getServer().overworld();
            recipes(level); effects(level);
            github.com.gengyoubo.CE.compat.tinkers.smeltery.LatexSmelteryRegressionChecks.run(level, LatexTinkersRegressionChecks::check);
            Files.writeString(Path.of("latex-tinkers-checks.txt"), "PASS: " + assertions + " assertions\n");
        } catch (Throwable error) {
            github.com.gengyoubo.CE.changede.LOGGER.error("LATEX TINKERS CHECKS FAILED", error);
            try { Files.writeString(Path.of("latex-tinkers-checks.txt"), "FAIL: " + error + "\n"); } catch (Exception ignored) {}
        } finally { event.getServer().execute(() -> event.getServer().halt(false)); }
    }
    private static ToolStack tool() {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("tconstruct:pickaxe"));
        ToolStack tool = ToolStack.from(new ItemStack(item)); tool.ensureHasData();
        var materials = tool.getMaterials();
        for (int i = 0; i < materials.size(); i++) materials = materials.replaceMaterial(i, MaterialVariantId.parse("tconstruct:flint"));
        tool.setMaterials(materials);
        tool.getPersistentData().setSlots(SlotType.UPGRADE, 20);
        return tool;
    }
    private record Station(ItemStack stack, ItemStack input) implements ITinkerStationContainer {
        @Override public ItemStack getTinkerableStack() { return stack; }
        @Override public ItemStack getInput(int index) { return input; }
        @Override public int getInputCount() { return 1; }
        @Override public MaterialRecipe getInputMaterial(int index) { return null; }
    }
    private static void recipes(ServerLevel level) {
        for (String path : new String[]{"latex_plasticity", "latex_resilience"}) {
            var id = ResourceLocation.parse("changede:tools/modifiers/" + path);
            var recipe = (ModifierRecipe) level.getRecipeManager().byKey(id).orElseThrow();
            ItemStack input = new ItemStack(path.equals("latex_plasticity") ? ChangedItems.WHITE_LATEX_GOO.get() : ChangedItems.DARK_LATEX_GOO.get());
            check(recipe.getSlots() == null, "Modifier recipe has no upgrade-slot cost");
            check(!recipe.matches(new Station(new ItemStack(Items.IRON_PICKAXE), input), level), "Vanilla equipment is not treated as a Tinkers tool");
            check(level.getRecipeManager().byKey(ResourceLocation.parse("changede:tools/modifiers/salvage/" + path)).isEmpty(), "No slot-refund salvage recipe is registered");
            ToolStack tool = tool();
            tool.getPersistentData().setSlots(SlotType.UPGRADE, -tool.getVolatileData().getSlots(SlotType.UPGRADE));
            check(tool.getFreeSlots(SlotType.UPGRADE) == 0, "Test equipment has zero free upgrade slots");
            float base = tool.getStats().get(ToolStats.DURABILITY);
            check(base > 20, "Native tool has real material durability");
            int free = tool.getFreeSlots(SlotType.UPGRADE);
            for (int stage = 1; stage <= 5; stage++) {
                var station = new Station(tool.createStack(), input);
                check(recipe.matches(station, level), "Native modifier recipe matches level " + stage + " of " + path);
                var result = recipe.getValidatedResult(station, level.registryAccess());
                check(result.isSuccess(), "Crafting succeeds without a player or latex-form condition");
                check(tool.getUpgrades().getLevel(new ModifierId("changede", path)) == stage - 1, "Recipe preview leaves the input tool unchanged");
                tool = result.getResult().getTool();
                check(tool.getFreeSlots(SlotType.UPGRADE) == free, "Upgrading succeeds with zero slots and consumes no slots");
                if (path.equals("latex_resilience"))
                    check(Math.abs(tool.getStats().get(ToolStats.DURABILITY) - base * (1 + .1 * stage)) < .01,
                            "Durability increases by ten percent per level: base=" + base + ", level=" + stage + ", actual=" + tool.getStats().get(ToolStats.DURABILITY));
            }
            check(recipe.getValidatedResult(new Station(tool.createStack(), input), level.registryAccess()).hasError(), "Sixth level rejected by the modifier level limit");
            ToolStack loaded = ToolStack.from(tool.createStack());
            float saved = loaded.getStats().get(ToolStats.DURABILITY); loaded.rebuildStats(); loaded.rebuildStats();
            check(Math.abs(loaded.getStats().get(ToolStats.DURABILITY) - saved) < .01, "Save and repeated rebuild do not compound durability");
            check(!recipe.matches(new Station(tool.createStack(), new ItemStack(Items.DIAMOND)), level), "Unrelated ingredient rejected");
            var modifierId = new ModifierId("changede", path);
            check(ModifierRecipeLookup.getSalvage(tool.createStack(), tool, modifierId, 5) == null, "Native salvage lookup cannot refund slots");
            var removal = level.getRecipeManager().getRecipes().stream().filter(ModifierRemovalRecipe.class::isInstance)
                    .map(ModifierRemovalRecipe.class::cast).findFirst().orElseThrow();
            var removed = removal.getResult(new Station(tool.createStack(), input), new ModifierEntry(modifierId, 5));
            check(removed.isSuccess(), "Native modifier removal succeeds");
            check(removed.getResult().getTool().getUpgrades().getLevel(modifierId) == 4, "Native removal removes one modifier level");
            check(removed.getResult().getTool().getFreeSlots(SlotType.UPGRADE) == free, "Native removal does not grant upgrade slots");
        }
    }
    private static void effects(ServerLevel level) {
        var timeData = (net.minecraft.world.level.storage.ServerLevelData) level.getLevelData();
        var player = FakePlayerFactory.getMinecraft(level);
        player.setPos(32, 96, 32);
        ProcessTransfur.setPlayerTransfurVariant(player, null, (TransfurContext) null);
        ToolStack tool = tool(); tool.addModifier(LatexTinkersCompat.PLASTICITY.getId(), 3); tool.setDamage(20);
        var white = new ModifierEntry(LatexTinkersCompat.PLASTICITY.get(), 3);
        timeData.setGameTime(60);
        white.getHook(slimeknights.tconstruct.library.modifiers.ModifierHooks.INVENTORY_TICK)
                .onInventoryTick(tool, white, level, player, 0, true, true, tool.createStack());
        check(tool.getDamage() == 17, "Human gets one repair point per level every three seconds");
        LatexTinkersCompat.PLASTICITY.get().onInventoryTick(tool, white, level, player, 0, true, true, tool.createStack());
        check(tool.getDamage() == 17, "Duplicate inventory hooks in one tick cannot repair twice");
        timeData.setGameTime(119);
        LatexTinkersCompat.PLASTICITY.get().onInventoryTick(tool, white, level, player, 0, true, true, tool.createStack());
        check(tool.getDamage() == 17, "No repair between sixty-tick intervals");
        ProcessTransfur.setPlayerTransfurVariant(player, ChangedTransfurVariants.PURE_WHITE_LATEX_WOLF.get(), (TransfurContext) null);
        timeData.setGameTime(120);
        LatexTinkersCompat.PLASTICITY.get().onInventoryTick(tool, white, level, player, 0, true, true, tool.createStack());
        check(tool.getDamage() == 11, "Actual white player form doubles repair");
        tool.setDamage(1); timeData.setGameTime(180);
        LatexTinkersCompat.PLASTICITY.get().onInventoryTick(tool, white, level, player, 0, true, true, tool.createStack());
        check(tool.getDamage() == 0, "Repair clamps at full durability");
        tool.setDamage(tool.getStats().getInt(ToolStats.DURABILITY)); timeData.setGameTime(240);
        check(tool.isBroken(), "Native broken-tool state is set");
        LatexTinkersCompat.PLASTICITY.get().onInventoryTick(tool, white, level, player, 0, false, false, tool.createStack());
        check(!tool.isBroken(), "Repair restores a broken tool even in an unselected inventory slot");
        var black = new ModifierEntry(LatexTinkersCompat.RESILIENCE.get(), 5);
        check(LatexTinkersCompat.RESILIENCE.get().onDamageTool(tool, black, 100, player, tool.createStack()) == 100, "White form receives no black Unbreaking bonus");
        ProcessTransfur.setPlayerTransfurVariant(player, ChangedTransfurVariants.DARK_LATEX_WOLF_MALE.get(), (TransfurContext) null);
        ToolStack damageTool = tool(); damageTool.addModifier(LatexTinkersCompat.RESILIENCE.getId(), 5);
        player.getRandom().setSeed(42);
        int expectedDamage = 0;
        for (int i = 0; i < 20; i++) if (!DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(damageTool.createStack(), 5, player.getRandom())) expectedDamage++;
        player.getRandom().setSeed(42);
        ToolDamageUtil.damage(damageTool, 20, player, damageTool.createStack());
        check(damageTool.getDamage() == expectedDamage, "Native damage pipeline invokes the black modifier hook");
        for (ItemStack stack : new ItemStack[]{tool.createStack(), new ItemStack(Items.IRON_CHESTPLATE)}) {
            player.getRandom().setSeed(42);
            int expected = 0;
            for (int i = 0; i < 1000; i++) if (!DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(stack, 5, player.getRandom())) expected++;
            player.getRandom().setSeed(42);
            check(LatexTinkersCompat.RESILIENCE.get().onDamageTool(tool, black, 1000, player, stack) == expected,
                    "Actual black form matches vanilla Unbreaking including armor behavior");
        }
        check(LatexTinkersCompat.RESILIENCE.get().beforeDamageTool(tool, black, 20, player, tool.createStack(), new ModifierId("tconstruct", "glowing")) == 20,
                "Tinkers damage causes that bypass reinforcement retain their cost");
        ProcessTransfur.setPlayerTransfurVariant(player, null, (TransfurContext) null);
        check(LatexTinkersCompat.RESILIENCE.get().onDamageTool(tool, black, 100, player, tool.createStack()) == 100, "Human form has no black Unbreaking bonus");
    }
}
