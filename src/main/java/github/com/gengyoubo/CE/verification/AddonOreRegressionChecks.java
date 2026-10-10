package github.com.gengyoubo.CE.verification;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.CE.changede;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TieredItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.tools.TinkerTools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Real survival mining checks, enabled only in a disposable verification server. */
public final class AddonOreRegressionChecks {
    private static final BlockPos POS = new BlockPos(48, 100, 48);
    private static int assertions;
    private AddonOreRegressionChecks() { }

    public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("changede.verifyAddonOre")) return;
        try {
            var level = event.getServer().overworld();
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "AddonOreTest"));
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(POS.getX() + .5, POS.getY() + 1, POS.getZ() + .5);
            compareHarvest(level, player);
            if (!Boolean.getBoolean("changede.verifyAddonOreOriginal")) {
            for (var tool : new net.minecraft.world.item.Item[]{Items.WOODEN_PICKAXE, Items.STONE_PICKAXE,
                    Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE,
                    Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL}) {
                boolean correct = tool == Items.DIAMOND_PICKAXE || tool == Items.NETHERITE_PICKAXE;
                mine(level, player, new ItemStack(tool), correct, false);
            }
            var silk = new ItemStack(Items.DIAMOND_PICKAXE);
            silk.enchant(Enchantments.SILK_TOUCH, 1);
            mine(level, player, silk, true, true);
            var fortune = new ItemStack(Items.DIAMOND_PICKAXE);
            fortune.enchant(Enchantments.BLOCK_FORTUNE, 3);
            mine(level, player, fortune, true, false);
            mine(level, player, tinkersPickaxe("iron"), false, false);
            mine(level, player, tinkersPickaxe("cobalt"), true, false);
            var tinkersNetherite = tinkersPickaxe("cobalt");
            ToolStack.from(tinkersNetherite).addModifier(new ModifierId("tconstruct:netherite"), 1);
            check(ToolStack.from(tinkersNetherite).getStats().get(ToolStats.HARVEST_TIER) == Tiers.NETHERITE,
                    "User's reported case uses an actual netherite-tier Tinkers pickaxe");
            mine(level, player, tinkersNetherite, true, false);
            var tinkersSilk = tinkersPickaxe("cobalt");
            ToolStack.from(tinkersSilk).addModifier(new ModifierId("tconstruct:silky"), 1);
            mine(level, player, tinkersSilk, true, true);
            var tinkersFortune = tinkersPickaxe("cobalt");
            ToolStack.from(tinkersFortune).addModifier(new ModifierId("tconstruct:luck"), 3);
            mine(level, player, tinkersFortune, true, false);
            mine(level, player, new ItemStack(Items.IRON_PICKAXE), false, false, "iridium_block");
            mine(level, player, new ItemStack(Items.DIAMOND_PICKAXE), true, false, "iridium_block");
            mine(level, player, new ItemStack(Items.NETHERITE_PICKAXE), true, false, "iridium_block");
            mine(level, player, tinkersPickaxe("iron"), false, false, "iridium_block");
            mine(level, player, tinkersPickaxe("cobalt"), true, false, "iridium_block");
            mine(level, player, tinkersNetherite.copy(), true, false, "iridium_block");
            }
            Files.writeString(Path.of("addon-ore-checks.txt"), "PASS: " + assertions + " assertions\n");
        } catch (Throwable error) {
            changede.LOGGER.error("ADDON ORE CHECKS FAILED", error);
            try { Files.writeString(Path.of("addon-ore-checks.txt"), "FAIL: " + error + "\n"); }
            catch (Exception ignored) { }
        } finally { event.getServer().execute(() -> event.getServer().halt(false)); }
    }

    private static void mine(ServerLevel level, FakePlayer player, ItemStack tool, boolean correct, boolean silk) {
        mine(level, player, tool, correct, silk, "deepslate_iridium_ore");
    }

    private static void compareHarvest(ServerLevel level, FakePlayer player) throws Exception {
        boolean original = Boolean.getBoolean("changede.verifyAddonOreOriginal");
        var report = new JsonObject();
        report.addProperty("original_addon_harvest", original);
        for (String mod : new String[]{"forge", "changed_addon", "tconstruct"})
            report.addProperty(mod + "_version", ModList.get().getModContainerById(mod).orElseThrow().getModInfo().getVersion().toString());
        var ordering = new JsonArray();
        for (var tier : TierSortingRegistry.getSortedTiers()) {
            var row = new JsonObject();
            row.addProperty("tier", String.valueOf(TierSortingRegistry.getName(tier)));
            row.addProperty("tag", tier.getTag() == null ? null : tier.getTag().location().toString());
            ordering.add(row);
        }
        report.add("sorted_tiers", ordering);
        var results = new JsonArray();
        report.add("results", results);
        var netherite = tinkersPickaxe("cobalt");
        ToolStack.from(netherite).addModifier(new ModifierId("tconstruct:netherite"), 1);
        var tools = new ItemStack[]{new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(Items.NETHERITE_PICKAXE), tinkersPickaxe("iron"), tinkersPickaxe("cobalt"), netherite};
        var names = new String[]{"vanilla_iron", "vanilla_diamond", "vanilla_netherite", "tinkers_iron", "tinkers_diamond", "tinkers_netherite"};
        var failures = new java.util.ArrayList<String>();
        for (int index = 0; index < tools.length; index++) {
            // Both ores are mined with this exact ItemStack instance; only normal tool wear changes.
            ItemStack tool = tools[index];
            var baseTier = ((TieredItem) tool.getItem()).getTier();
            var actualTier = index < 3 ? baseTier : ToolStack.from(tool).getStats().get(ToolStats.HARVEST_TIER);
            for (String block : new String[]{"deepslate_iridium_ore", "deepslate_painite_ore"}) {
                var ore = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("changed_addon:" + block));
                var state = ore.defaultBlockState();
                var area = new AABB(POS).inflate(3);
                level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
                level.getEntitiesOfClass(ExperienceOrb.class, area).forEach(ExperienceOrb::discard);
                level.setBlockAndUpdate(POS, state);
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                var row = new JsonObject();
                row.addProperty("tool", names[index]);
                row.addProperty("block", block);
                row.addProperty("base_tier_class", baseTier.getClass().getName());
                row.addProperty("base_level", baseTier.getLevel());
                row.addProperty("base_registered", TierSortingRegistry.isTierSorted(baseTier));
                row.addProperty("actual_tier", String.valueOf(TierSortingRegistry.getName(actualTier)));
                row.addProperty("actual_level", actualTier.getLevel());
                row.addProperty("needs_diamond_tag", state.is(BlockTags.NEEDS_DIAMOND_TOOL));
                row.addProperty("needs_netherite_tag", state.is(Tags.Blocks.NEEDS_NETHERITE_TOOL));
                boolean baseCorrect = TierSortingRegistry.isCorrectTierForDrops(baseTier, state);
                boolean actualCorrect = TierSortingRegistry.isCorrectTierForDrops(actualTier, state);
                boolean itemCorrect = tool.isCorrectToolForDrops(state);
                boolean playerCorrect = player.hasCorrectToolForDrops(state);
                boolean forgeCorrect = ForgeHooks.isCorrectToolForDrops(state, player);
                boolean blockCorrect = state.canHarvestBlock(level, POS, player);
                row.addProperty("base_tier_check", baseCorrect);
                row.addProperty("actual_tier_check", actualCorrect);
                row.addProperty("item_check", itemCorrect);
                row.addProperty("player_check", playerCorrect);
                row.addProperty("forge_check", forgeCorrect);
                row.addProperty("block_check", blockCorrect);
                player.gameMode.destroyBlock(POS);
                var loot = new JsonArray();
                for (var drop : level.getEntitiesOfClass(ItemEntity.class, area)) {
                    var item = new JsonObject();
                    item.addProperty("item", ForgeRegistries.ITEMS.getKey(drop.getItem().getItem()).toString());
                    item.addProperty("count", drop.getItem().getCount());
                    loot.add(item);
                }
                row.add("drops", loot);
                row.addProperty("xp", level.getEntitiesOfClass(ExperienceOrb.class, area).stream().mapToInt(ExperienceOrb::getValue).sum());
                results.add(row);
                boolean expected = actualTier.getLevel() >= (block.equals("deepslate_iridium_ore") ? 3 : 4);
                boolean expectedBlock = expected && !(original && index >= 3 && block.equals("deepslate_iridium_ore"));
                if (actualCorrect != expected || itemCorrect != expected || playerCorrect != expected || forgeCorrect != expected
                        || blockCorrect != expectedBlock || loot.isEmpty() == expectedBlock || !level.getBlockState(POS).isAir())
                    failures.add(names[index] + " / " + block + " -> " + row);
                else assertions++;
                changede.LOGGER.info("ORE COMPARISON: {}", row);
            }
        }
        String file = original ? "addon-ore-comparison-original.json" : "addon-ore-comparison-fixed.json";
        Files.writeString(Path.of(file), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        check(failures.isEmpty(), "Same-stack ore comparison: " + failures);
    }

    private static ItemStack tinkersPickaxe(String material) {
        var id = new MaterialId("tconstruct:" + material);
        var materials = MaterialNBT.builder().add(id).add(id).add(id).build();
        return ToolBuildHandler.buildItemFromMaterials(TinkerTools.pickaxe.get(), materials);
    }

    private static void mine(ServerLevel level, FakePlayer player, ItemStack tool, boolean correct, boolean silk, String block) {
        var ore = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("changed_addon:" + block));
        var raw = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("changed_addon:raw_iridium"));
        var area = new AABB(POS).inflate(3);
        level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
        level.getEntitiesOfClass(ExperienceOrb.class, area).forEach(ExperienceOrb::discard);
        level.setBlockAndUpdate(POS, ore.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        String name = block + " using " + ForgeRegistries.ITEMS.getKey(tool.getItem()) + " " + tool.getTag();
        boolean playerCorrect = player.hasCorrectToolForDrops(ore.defaultBlockState());
        boolean harvest = ore.defaultBlockState().canHarvestBlock(level, POS, player);
        player.gameMode.destroyBlock(POS);
        check(level.getBlockState(POS).isAir(), name + " breaks the ore, including tools that handle mining themselves");
        var drops = level.getEntitiesOfClass(ItemEntity.class, area);
        int xp = level.getEntitiesOfClass(ExperienceOrb.class, area).stream().mapToInt(ExperienceOrb::getValue).sum();
        changede.LOGGER.info("ORE RESULT: tool={}, playerCorrect={}, harvest={}, drops={}, xp={}",
                name, playerCorrect, harvest, drops.stream().map(item -> item.getItem().toString()).toList(), xp);
        check(playerCorrect == correct && harvest == correct, name + " respects diamond mining requirements");
        if (!correct) check(drops.isEmpty(), name + " cannot harvest the ore");
        else {
            boolean self = silk || block.equals("iridium_block");
            check(drops.size() == 1 && drops.get(0).getItem().is(self ? ore.asItem() : raw), name + " delivers the expected ore loot");
            int count = drops.get(0).getItem().getCount();
            int fortune = tool.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE);
            check(count >= 1 && count <= (self ? 1 : fortune + 1), name + " has the expected drop count");
            check(self ? xp == 0 : xp >= 20 && xp <= 40, name + " preserves ore experience and silk touch rules");
        }
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        assertions++;
    }
}
