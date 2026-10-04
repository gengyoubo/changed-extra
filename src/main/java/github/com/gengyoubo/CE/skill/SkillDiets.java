package github.com.gengyoubo.CE.skill;

import github.com.gengyoubo.CE.changede;
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
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Rebuild only after recipe loading and tag binding, never while a player is eating. */
@Mod.EventBusSubscriber(modid = "changede")
public final class SkillDiets {
    public enum Diet {
        FISH("fish"), DARK_LATEX("dark_latex"), WHITE_LATEX("white_latex"), ORANGE("orange"), MEAT("meat"), VEGETARIAN("vegetarian");
        private final String id;
        Diet(String id) { this.id = id; }
        public String effect() { return id + "_diet"; }
        private TagKey<Item> foodsTag() { return tag("skill_" + id + "_foods"); }
        private TagKey<Item> exclusionsTag() { return tag("skill_" + id + "_diet_exclusions"); }
    }
    private static final TagKey<Item> EXCLUDED = tag("skill_diet_exclusions"), NEUTRAL = tag("skill_diet_neutral_ingredients");
    private static final Set<ResourceLocation> RECIPE_TYPES = Set.of(
            ResourceLocation.parse("minecraft:crafting"), ResourceLocation.parse("minecraft:smelting"),
            ResourceLocation.parse("minecraft:smoking"), ResourceLocation.parse("minecraft:campfire_cooking"),
            ResourceLocation.parse("farmersdelight:cooking"), ResourceLocation.parse("farmersdelight:cutting"));
    private static volatile Map<Diet, Set<Item>> foods = Map.of();
    private static MinecraftServer owner;
    private SkillDiets() { }
    private static TagKey<Item> tag(String path) { return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("changede", path)); }
    public static boolean matches(Diet diet, ItemStack stack) { return !stack.isEmpty() && foods.getOrDefault(diet, Set.of()).contains(stack.getItem()); }
    public static boolean matches(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        return Arrays.stream(Diet.values()).anyMatch(diet -> SkillMechanics.value(player, diet.effect()) > 0 && matches(diet, stack));
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) { rebuild(event.getServer()); }
    @SubscribeEvent public static void sync(OnDatapackSyncEvent event) {
        MinecraftServer server = event.getPlayerList().getServer();
        if (event.getPlayer() == null || owner != server) rebuild(server);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        if (owner == event.getServer()) { owner = null; foods = Map.of(); }
    }
    private static Set<Item> tagged(TagKey<Item> tag) {
        Set<Item> result = new HashSet<>();
        var tags = ForgeRegistries.ITEMS.tags();
        if (tags != null) tags.getTag(tag).forEach(result::add);
        return result;
    }
    private static void rebuild(MinecraftServer server) {
        List<SkillDietGraph.Recipe<Item>> recipes = new ArrayList<>();
        List<SkillDietGraph.VegetarianRecipe<Item>> vegetarianRecipes = new ArrayList<>();
        for (var recipe : server.getRecipeManager().getRecipes()) {
            ResourceLocation type = ForgeRegistries.RECIPE_TYPES.getKey(recipe.getType());
            ItemStack output = ItemStack.EMPTY;
            try {
                output = recipe.getResultItem(server.registryAccess());
                if (output.isEmpty()) continue;
                if (type == null || !RECIPE_TYPES.contains(type)) {
                    vegetarianRecipes.add(new SkillDietGraph.VegetarianRecipe<>(output.getItem(), List.of()));
                    continue;
                }
                Set<Item> inputs = new HashSet<>();
                List<Set<Item>> slots = new ArrayList<>();
                boolean unresolved = false;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient == Ingredient.EMPTY) continue;
                    ingredient.checkInvalidation();
                    ingredient.getStackingIds();
                    var candidates = Arrays.stream(ingredient.getItems()).filter(stack -> !stack.isEmpty()).toList();
                    if (candidates.isEmpty()) { unresolved = true; break; }
                    candidates.forEach(stack -> inputs.add(stack.getItem()));
                    Set<Item> alternatives = new HashSet<>();
                    candidates.forEach(stack -> alternatives.add(stack.getItem()));
                    slots.add(alternatives);
                }
                if (!unresolved && !inputs.isEmpty()) recipes.add(new SkillDietGraph.Recipe<>(output.getItem(), inputs));
                vegetarianRecipes.add(new SkillDietGraph.VegetarianRecipe<>(output.getItem(), unresolved ? List.of() : slots));
            } catch (RuntimeException ex) {
                if (!output.isEmpty()) vegetarianRecipes.add(new SkillDietGraph.VegetarianRecipe<>(output.getItem(), List.of()));
                changede.LOGGER.debug("Cannot infer skill diets from recipe {}", recipe.getId(), ex);
            }
        }
        Set<Item> excluded = tagged(EXCLUDED), neutral = tagged(NEUTRAL);
        Map<Diet, Set<Item>> next = new EnumMap<>(Diet.class);
        for (Diet diet : Diet.values()) {
            if (diet == Diet.VEGETARIAN) continue;
            Set<Item> blocked = new HashSet<>(excluded);
            blocked.addAll(tagged(diet.exclusionsTag()));
            blocked.addAll(neutral);
            Set<Item> seeds = tagged(diet.foodsTag());
            if (diet == Diet.MEAT) for (Item item : ForgeRegistries.ITEMS) {
                try {
                    var properties = item.getDefaultInstance().getFoodProperties(null);
                    if (properties != null && properties.isMeat()) seeds.add(item);
                } catch (RuntimeException ex) {
                    changede.LOGGER.debug("Cannot inspect food properties of {}", ForgeRegistries.ITEMS.getKey(item), ex);
                }
            }
            next.put(diet, SkillDietGraph.infer(seeds, recipes, blocked));
        }
        Set<Item> animalFoods = new HashSet<>(next.get(Diet.MEAT));
        animalFoods.addAll(next.get(Diet.FISH));
        Set<Item> vegetarianExcluded = new HashSet<>(excluded);
        vegetarianExcluded.addAll(tagged(Diet.VEGETARIAN.exclusionsTag()));
        next.put(Diet.VEGETARIAN, SkillDietGraph.inferVegetarian(tagged(Diet.VEGETARIAN.foodsTag()),
                vegetarianRecipes, vegetarianExcluded, animalFoods, neutral));
        // Meat-eating accepts fish/seafood too, matching the reference carnivore diet.
        animalFoods.removeAll(excluded);
        animalFoods.removeAll(tagged(Diet.MEAT.exclusionsTag()));
        next.put(Diet.MEAT, Set.copyOf(animalFoods));
        foods = Map.copyOf(next);
        owner = server;
        changede.LOGGER.info("Skill diets: {} categories across {} supported recipes", foods.size(), recipes.size());
    }
}
