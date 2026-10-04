package github.com.gengyoubo.CE.skill;

import github.com.gengyoubo.CE.changede;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Rebuild only after recipe loading and tag binding, never while a player is eating. */
@Mod.EventBusSubscriber(modid = "changede")
public final class SkillFishDiet {
    private static final TagKey<Item> FOODS = TagKey.create(Registries.ITEM, ResourceLocation.parse("changede:skill_fish_foods"));
    private static final TagKey<Item> EXCLUDED = TagKey.create(Registries.ITEM, ResourceLocation.parse("changede:skill_fish_diet_exclusions"));
    private static final Set<ResourceLocation> RECIPE_TYPES = Set.of(
            ResourceLocation.parse("minecraft:crafting"), ResourceLocation.parse("minecraft:smelting"),
            ResourceLocation.parse("minecraft:smoking"), ResourceLocation.parse("minecraft:campfire_cooking"),
            ResourceLocation.parse("farmersdelight:cooking"), ResourceLocation.parse("farmersdelight:cutting"));
    private static volatile Set<Item> foods = Set.of();
    private static MinecraftServer owner;
    private SkillFishDiet() { }
    public static boolean matches(ItemStack stack) { return !stack.isEmpty() && foods.contains(stack.getItem()); }
    @SubscribeEvent public static void started(ServerStartedEvent event) { rebuild(event.getServer()); }
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event) {
        MinecraftServer server = event.getPlayerList().getServer();
        if (event.getPlayer() == null || owner != server) rebuild(server);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        if (owner == event.getServer()) { owner = null; foods = Set.of(); }
    }
    private static Set<Item> tagged(TagKey<Item> tag) {
        Set<Item> result = new HashSet<>();
        BuiltInRegistries.ITEM.getTag(tag).ifPresent(entries -> entries.forEach(entry -> result.add(entry.value())));
        return result;
    }
    private static void rebuild(MinecraftServer server) {
        List<SkillFishDietGraph.Recipe<Item>> recipes = new ArrayList<>();
        for (var recipe : server.getRecipeManager().getRecipes()) {
            if (!RECIPE_TYPES.contains(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()))) continue;
            try {
                ItemStack output = recipe.getResultItem(server.registryAccess());
                if (output.isEmpty()) continue;
                Set<Item> inputs = new HashSet<>();
                boolean unresolved = false;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient == Ingredient.EMPTY) continue;
                    ingredient.checkInvalidation();
                    ingredient.getStackingIds();
                    var candidates = Arrays.stream(ingredient.getItems()).filter(stack -> !stack.isEmpty()).toList();
                    if (candidates.isEmpty()) { unresolved = true; break; }
                    candidates.forEach(stack -> inputs.add(stack.getItem()));
                }
                if (!unresolved && !inputs.isEmpty()) recipes.add(new SkillFishDietGraph.Recipe<>(output.getItem(), inputs));
            } catch (RuntimeException ex) {
                changede.LOGGER.debug("Cannot infer fish diet from recipe {}", recipe.getId(), ex);
            }
        }
        foods = SkillFishDietGraph.infer(tagged(FOODS), recipes, tagged(EXCLUDED));
        owner = server;
        changede.LOGGER.info("Skill fish diet: {} ingredient/food items across {} supported recipes", foods.size(), recipes.size());
    }
}
