package github.com.gengyoubo.CE.compat.synergy.camp;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraftforge.common.ForgeHooks;

import java.util.*;

/** Recipe selection and actual withdrawal. Workstation categories are the compatibility boundary. */
public final class CampKitchen {
    public enum StationKind { CRAFTING, SMELTING, COOKING_POT, CUTTING }
    static final int FOOD_LIMIT = 64;
    record Source(BlockPos pos, int slot, ItemStack prototype, int available) {}
    record Choice(int gridSlot, Source source, int count) {}
    record Plan(ResourceLocation recipe, StationKind kind, BlockPos station, List<Choice> choices,
                Choice fuel, ItemStack result, int duration) {}
    record Search(Plan plan, LatexSettlementData.WorkState waiting) {}
    private record Ranked(Recipe<?> recipe, float score) {}
    private CampKitchen() {}
    static float score(ItemStack food, ChangedEntity mob) {
        var properties = food.getFoodProperties(mob);
        return properties == null ? -1 : properties.getNutrition() * (1 + properties.getSaturationModifier() * 2);
    }
    static int stock(ServerLevel level, LatexSettlementData.Settlement camp, ItemStack result) {
        int count = 0;
        for (var handler : CampWarehouse.inventories(level, camp)) for (int slot = 0; slot < handler.getSlots(); slot++)
            if (ItemStack.isSameItemSameTags(handler.getStackInSlot(slot), result)) count += handler.getStackInSlot(slot).getCount();
        for (ItemStack stack : camp.pending) if (ItemStack.isSameItemSameTags(stack, result)) count += stack.getCount();
        for (var resident : camp.residents.values()) {
            ChangedEntity mob = LatexSettlementService.anywhere(level.getServer(), resident.id);
            if (mob != null) {
                count += CampWorkBuffer.countCargo(mob, result);
                CompoundTag batch = CampWorkBuffer.kitchen(mob);
                if (batch.getString("ResultItem").equals(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(result.getItem()).toString())) count += batch.getInt("ResultCount");
            }
        }
        return count;
    }
    static List<Source> sources(ServerLevel level, LatexSettlementData.Settlement camp) {
        List<Source> result = new ArrayList<>(); Set<BlockPos> seen = new HashSet<>();
        for (BlockPos link : camp.storages) {
            if (!level.hasChunkAt(link) || !CampWorkSites.inside(camp, link)) continue;
            BlockPos pos = CampWarehouse.canonical(level, link); if (!seen.add(pos)) continue;
            var inventory = CampWarehouse.inventory(level, pos); if (inventory == null) continue;
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.extractItem(i, Math.min(64, inventory.getStackInSlot(i).getCount()), true);
                if (!stack.isEmpty()) result.add(new Source(pos, i, stack.copyWithCount(1), stack.getCount()));
            }
        }
        return result;
    }
    static Search find(ServerLevel level, LatexSettlementData.Settlement camp, ChangedEntity mob) {
        if (CampWarehouse.inventories(level, camp).isEmpty()) return new Search(null, LatexSettlementData.WorkState.WAITING_STORAGE);
        Set<BlockPos> reachable = new HashSet<>();
        for (BlockPos pos : camp.storages) if (level.hasChunkAt(pos) && CampWorkSites.approach(mob, pos) != null)
            reachable.add(CampWarehouse.canonical(level, pos));
        List<Source> sources = sources(level, camp).stream().filter(source -> reachable.contains(source.pos)).toList();
        if (sources.isEmpty()) return new Search(null, LatexSettlementData.WorkState.WAITING_MATERIALS);
        List<BlockPos> stations = CampWorkSites.kitchens(level, camp).stream()
                .filter(pos -> level.hasChunkAt(pos) && CampWorkSites.available(level, pos, mob.getUUID()))
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(mob.blockPosition()))).toList();
        if (stations.isEmpty()) return new Search(null, LatexSettlementData.WorkState.WAITING_STATION);
        List<Ranked> recipes = new ArrayList<>();
        for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
            if (!(recipe instanceof CraftingRecipe) && recipe.getType() != RecipeType.SMELTING && recipe.getType() != RecipeType.SMOKING) continue;
            ItemStack result = recipe.getResultItem(level.registryAccess()); float score = score(result, mob);
            if (score >= 0 && !recipe.getIngredients().isEmpty()) recipes.add(new Ranked(recipe, score));
        }
        recipes.sort(Comparator.comparingDouble(Ranked::score).reversed().thenComparing(entry -> entry.recipe.getId().toString()));
        Plan best = null; float bestScore = -1; boolean enoughFood = false;
        for (Ranked ranked : recipes) {
            Recipe<?> recipe = ranked.recipe;
            List<Choice> choices = allocate(recipe, sources); if (choices == null) continue;
            ItemStack assembled;
            if (recipe instanceof CraftingRecipe crafting) {
                var grid = grid(preview(choices)); if (!crafting.matches(grid, level)) continue;
                assembled = crafting.assemble(grid, level.registryAccess());
            } else {
                AbstractCookingRecipe cooking = (AbstractCookingRecipe) recipe;
                var input = new SimpleContainer(choices.get(0).source.prototype.copyWithCount(1));
                if (!cooking.matches(input, level)) continue;
                assembled = cooking.assemble(input, level.registryAccess());
            }
            float actualScore = score(assembled, mob);
            if (actualScore < 0 || actualScore <= bestScore) continue;
            Plan candidate = atStation(level, mob, recipe, assembled, choices, sources, stations);
            if (candidate != null && stock(level, camp, assembled) + assembled.getCount() > FOOD_LIMIT) { enoughFood = true; continue; }
            if (candidate != null) { best = candidate; bestScore = actualScore; }
        }
        return new Search(best, enoughFood ? LatexSettlementData.WorkState.WAITING_STOCK : LatexSettlementData.WorkState.WAITING_MATERIALS);
    }
    private static Plan atStation(ServerLevel level, ChangedEntity mob, Recipe<?> recipe, ItemStack assembled,
                                  List<Choice> choices, List<Source> sources, List<BlockPos> stations) {
        for (BlockPos pos : stations) {
            var state = level.getBlockState(pos);
            if (CampWorkSites.approach(mob, pos) == null) continue;
            if (recipe instanceof CraftingRecipe && state.is(CampWorkSites.CRAFTING))
                return new Plan(recipe.getId(), StationKind.CRAFTING, pos, choices, null, assembled, 60);
            boolean smoker = recipe.getType() == RecipeType.SMOKING;
            if (!(recipe instanceof AbstractCookingRecipe cooking) || !state.is(smoker ? Blocks.SMOKER : Blocks.FURNACE)
                    || !(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace)
                    || !furnace.getItem(0).isEmpty() || !furnace.getItem(2).isEmpty()) continue;
            ItemStack storedFuel = furnace.getItem(1);
            int required = cooking.getCookingTime();
            if (ForgeHooks.getBurnTime(storedFuel, cooking.getType()) * storedFuel.getCount() >= required)
                return new Plan(recipe.getId(), StationKind.SMELTING, pos, choices, null, assembled, required);
            if (!storedFuel.isEmpty()) continue;
            for (Source fuel : sources) {
                int burn = ForgeHooks.getBurnTime(fuel.prototype, cooking.getType()); if (burn <= 0) continue;
                int needed = Math.max(1, (required + burn - 1) / burn);
                int used = choices.stream().filter(choice -> choice.source == fuel).mapToInt(Choice::count).sum();
                if (fuel.available - used >= needed) return new Plan(recipe.getId(), StationKind.SMELTING, pos, choices,
                        new Choice(-1, fuel, needed), assembled, required);
            }
        }
        return null;
    }
    static List<Choice> allocate(Recipe<?> recipe, List<Source> sources) {
        List<Ingredient> ingredients = new ArrayList<>(); List<Integer> positions = new ArrayList<>();
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        if (width > 3 || recipe.getIngredients().size() > 9) return null;
        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            Ingredient ingredient = recipe.getIngredients().get(i); if (ingredient.isEmpty()) continue;
            ingredients.add(ingredient); positions.add(recipe instanceof ShapedRecipe ? i % width + i / width * 3 : i);
        }
        if (ingredients.isEmpty()) return null;
        boolean[][] accepts = new boolean[ingredients.size()][sources.size()]; int[] counts = new int[sources.size()];
        for (int source = 0; source < sources.size(); source++) {
            counts[source] = sources.get(source).available;
            for (int row = 0; row < ingredients.size(); row++) accepts[row][source] = ingredients.get(row).test(sources.get(source).prototype);
        }
        int[] chosen = CampIngredientMatcher.match(accepts, counts); if (chosen == null) return null;
        List<Choice> result = new ArrayList<>();
        for (int i = 0; i < chosen.length; i++) result.add(new Choice(positions.get(i), sources.get(chosen[i]), 1));
        return result;
    }
    static NonNullList<ItemStack> preview(List<Choice> choices) {
        NonNullList<ItemStack> result = NonNullList.withSize(9, ItemStack.EMPTY);
        choices.forEach(choice -> result.set(choice.gridSlot, choice.source.prototype.copyWithCount(choice.count))); return result;
    }
    static CompoundTag withdraw(ServerLevel level, LatexSettlementData.Settlement camp, ChangedEntity mob, Plan plan) {
        List<Choice> all = new ArrayList<>(plan.choices); if (plan.fuel != null) all.add(plan.fuel);
        Map<Source, Integer> totals = new LinkedHashMap<>(); all.forEach(choice -> totals.merge(choice.source, choice.count, Integer::sum));
        for (var entry : totals.entrySet()) {
            Source source = entry.getKey();
            if (!level.hasChunkAt(source.pos) || camp.storages.stream().noneMatch(link -> CampWarehouse.canonical(level, link).equals(source.pos))) return null;
            var inventory = CampWarehouse.inventory(level, source.pos); if (inventory == null) return null;
            ItemStack simulated = inventory.extractItem(source.slot, entry.getValue(), true);
            if (simulated.getCount() != entry.getValue() || !ItemStack.isSameItemSameTags(simulated, source.prototype)) return null;
        }
        Map<Source, ItemStack> paid = new LinkedHashMap<>();
        for (var entry : totals.entrySet()) {
            var inventory = CampWarehouse.inventory(level, entry.getKey().pos);
            ItemStack actual = inventory == null ? ItemStack.EMPTY : inventory.extractItem(entry.getKey().slot, entry.getValue(), false);
            if (!actual.isEmpty()) paid.put(entry.getKey(), actual);
            if (actual.getCount() != entry.getValue() || !ItemStack.isSameItemSameTags(actual, entry.getKey().prototype)) {
                CampWorkBuffer.addCargo(mob, paid.values()); return null;
            }
        }
        NonNullList<ItemStack> grid = NonNullList.withSize(9, ItemStack.EMPTY);
        for (Choice choice : plan.choices) grid.set(choice.gridSlot, paid.get(choice.source).split(choice.count));
        CompoundTag batch = new CompoundTag(); ContainerHelper.saveAllItems(batch, grid);
        if (plan.fuel != null) batch.put("Fuel", paid.get(plan.fuel.source).split(plan.fuel.count).save(new CompoundTag()));
        batch.putString("Recipe", plan.recipe.toString()); batch.putString("Kind", plan.kind.name());
        batch.putLong("Station", plan.station.asLong()); batch.putString("Dimension", level.dimension().location().toString());
        batch.putInt("Duration", plan.duration); batch.putInt("Ticks", 0); batch.putString("Phase", "WALKING");
        batch.putString("ResultItem", net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(plan.result.getItem()).toString()); batch.putInt("ResultCount", plan.result.getCount());
        CampWorkBuffer.kitchen(mob, batch); return batch;
    }
    static CraftingContainer grid(NonNullList<ItemStack> items) {
        var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return false; }
        }, 3, 3);
        for (int i = 0; i < 9; i++) grid.setItem(i, items.get(i)); return grid;
    }
}
