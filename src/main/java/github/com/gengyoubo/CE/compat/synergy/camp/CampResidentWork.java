package github.com.gengyoubo.CE.compat.synergy.camp;

import github.com.gengyoubo.CE.compat.synergy.CreatureInventoryAccess;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.parkabird.changedsynergy.ai.*;

import java.util.*;

import static github.com.gengyoubo.CE.compat.synergy.camp.LatexSettlementData.*;

/** A single real action under a resident's persistent occupation. No abstract rewards. */
final class CampResidentWork {
    private final ChangedEntity mob;
    private CampRole role;
    private long nextSearch, walkingSince;
    private Vec3 destination;
    private BlockPos crop;
    private CreatureSettlementService.FishingSite fish;
    private CampKitchen.Plan plan;
    private int actionTicks, fetchIndex;
    private BlockPos approachPos;
    private Vec3 approachVec;
    private long approachUntil;
    CampResidentWork(ChangedEntity mob) { this.mob = mob; }
    static void cancel(ChangedEntity mob) { cancel(mob, CreatureInventoryAccess.resolve(mob)); }
    static void cancel(ChangedEntity mob, Container inventory) {
        for (var goal : mob.goalSelector.getAvailableGoals()) if (goal.getGoal() instanceof LatexCampGoal camp) camp.resetWork();
        if (mob.level() instanceof ServerLevel level) FishingVisualEffects.cancel(level, mob);
        FishingVisualEffects.setRodCastModel(mob, false);
        CampWorkBuffer.restoreTool(mob, inventory); CampWorkBuffer.cancelKitchen(mob); CampWorkSites.release(mob.getUUID());
    }
    void reset() {
        role = null; crop = null; fish = null; plan = null; destination = null; actionTicks = 0; fetchIndex = 0;
        walkingSince = 0; nextSearch = 0; approachPos = null; approachVec = null;
    }
    void tick(ServerLevel level, Settlement camp, Resident resident) {
        if (role != resident.role) {
            if (role != null) cancel(mob);
            role = resident.role;
            if (role != CampRole.COOK) CampWorkBuffer.cancelKitchen(mob);
            if (role != CampRole.FISHER) CampWorkBuffer.restoreTool(mob, CreatureInventoryAccess.resolve(mob));
        }
        if (CampWorkBuffer.hasCargo(mob)) { deliver(level, camp, resident); return; }
        if (role == CampRole.NONE || role == CampRole.GUARD) return;
        switch (role) {
            case FISHER -> fishing(level, camp, resident);
            case FARMER -> farming(level, camp, resident);
            case COOK -> cooking(level, camp, resident);
            default -> {}
        }
    }
    private boolean searchReady(ServerLevel level) {
        if (level.getGameTime() < nextSearch) return false;
        nextSearch = level.getGameTime() + 80 + Math.floorMod(mob.getId(), 20); return true;
    }
    private void status(Resident resident, WorkState state, BlockPos target) { resident.workState = state; resident.workTarget = target; }
    private Vec3 approach(ServerLevel level, BlockPos pos) {
        if (!pos.equals(approachPos) || level.getGameTime() >= approachUntil || approachVec != null
                && !level.noCollision(mob, mob.getBoundingBox().move(approachVec.subtract(mob.position())))) {
            approachPos = pos; approachUntil = level.getGameTime() + 60; approachVec = CampWorkSites.approach(mob, pos);
        }
        return approachVec;
    }
    private boolean walk(ServerLevel level, Vec3 target, Resident resident, WorkState moving) {
        if (target == null) {
            cancel(mob); status(resident, WorkState.SEARCHING, null); nextSearch = level.getGameTime() + 100; return false;
        }
        if (!target.equals(destination)) { destination = target; walkingSince = level.getGameTime(); }
        if (mob.distanceToSqr(target) > 2.25 && level.getGameTime() - walkingSince > 600 || !level.hasChunkAt(BlockPos.containing(target))) {
            cancel(mob); status(resident, WorkState.SEARCHING, null); nextSearch = level.getGameTime() + 100; return false;
        }
        if (mob.distanceToSqr(target) > 2.25) {
            status(resident, moving, BlockPos.containing(target));
            if (mob.tickCount % 20 == 0 || mob.getNavigation().isDone()) mob.getNavigation().moveTo(target.x, target.y, target.z, 1);
            return false;
        }
        walkingSince = level.getGameTime(); mob.getNavigation().stop(); return true;
    }
    private void deliver(ServerLevel level, Settlement camp, Resident resident) {
        BlockPos storage = camp.storages.stream().filter(pos -> CampWarehouse.inventory(level, pos) != null)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(mob.blockPosition()))).orElse(null);
        if (storage == null) {
            camp.pending.addAll(CampWorkBuffer.takeCargo(mob)); status(resident, WorkState.WAITING_STORAGE, null);
            LatexSettlementData.get(level.getServer()).setDirty(); return;
        }
        Vec3 approach = approach(level, storage);
        if (approach == null) { status(resident, WorkState.WAITING_STORAGE, storage); return; }
        if (!walk(level, approach, resident, WorkState.DELIVERING)) return;
        camp.pending.addAll(CampWorkBuffer.takeCargo(mob)); CampWarehouse.deposit(level, camp);
        status(resident, camp.pending.isEmpty() ? WorkState.IDLE : WorkState.WAITING_STORAGE, storage);
        destination = null; LatexSettlementData.get(level.getServer()).setDirty();
    }
    private void fishing(ServerLevel level, Settlement camp, Resident resident) {
        if (!camp.pending.isEmpty() || CampWarehouse.inventories(level, camp).isEmpty()) { status(resident, WorkState.WAITING_STORAGE, null); return; }
        var inventory = CreatureInventoryAccess.resolve(mob);
        InteractionHand hand = CampWorkBuffer.rodHand(mob, inventory);
        if (hand == null) { status(resident, WorkState.WAITING_ROD, null); return; }
        if (fish == null) {
            if (!searchReady(level)) return;
            status(resident, WorkState.SEARCHING, null);
            fish = CampWorkSites.fishing(mob, camp);
            if (fish == null || !CampWorkSites.claim(level, fish.water(), mob.getUUID())) {
                fish = null; CampWorkBuffer.restoreTool(mob, inventory); status(resident, WorkState.WAITING_WATER, null); return;
            }
            actionTicks = 0;
        }
        if (!level.hasChunkAt(fish.water()) || !level.getBlockState(fish.water()).is(Blocks.WATER)
                || !level.getFluidState(fish.water().above()).isEmpty()) { cancel(mob); return; }
        CampWorkSites.claim(level, fish.water(), mob.getUUID());
        if (!walk(level, CreatureSettlementService.fishingApproach(mob, fish), resident, WorkState.WALKING)) return;
        mob.getLookControl().setLookAt(Vec3.atCenterOf(fish.water())); status(resident, WorkState.FISHING, fish.water());
        if (actionTicks++ == 0) {
            mob.swing(hand); FishingVisualEffects.cast(level, mob, fish.water()); FishingVisualEffects.setRodCastModel(mob, true);
            level.playSound(null, mob.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.55F, 1);
        }
        FishingVisualEffects.tick(level, mob, fish.water(), actionTicks);
        ItemStack rod = mob.getItemInHand(hand);
        int duration = Math.max(80, 160 - EnchantmentHelper.getFishingSpeedBonus(rod) * 20);
        if (actionTicks < duration) return;
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(fish.water()))
                .withParameter(LootContextParams.TOOL, rod).withParameter(LootContextParams.THIS_ENTITY, mob)
                .withLuck(EnchantmentHelper.getFishingLuckBonus(rod)).create(LootContextParamSets.FISHING);
        List<ItemStack> caught = level.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING).getRandomItems(params);
        FishingVisualEffects.retrieve(level, mob, fish.water(), caught.stream().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY));
        rod.hurtAndBreak(1, mob, entity -> entity.broadcastBreakEvent(hand));
        mob.swing(hand); level.playSound(null, mob.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 0.8F, 1);
        CampWorkBuffer.addCargo(mob, caught); cancel(mob); status(resident, WorkState.DELIVERING, null);
    }
    private void farming(ServerLevel level, Settlement camp, Resident resident) {
        if (!ForgeEventFactory.getMobGriefingEvent(level, mob)) { status(resident, WorkState.PAUSED, null); return; }
        if (!camp.pending.isEmpty() || CampWarehouse.inventories(level, camp).isEmpty()) { status(resident, WorkState.WAITING_STORAGE, null); return; }
        if (crop == null) {
            if (!searchReady(level)) return;
            for (BlockPos pos : CampWorkSites.crops(level, camp).stream().sorted(Comparator.comparingDouble(pos -> pos.distSqr(mob.blockPosition()))).toList()) {
                if (!legalCrop(level, pos) || !CampWorkSites.available(level, pos, mob.getUUID())) continue;
                Vec3 approach = CampWorkSites.approach(mob, pos);
                if (approach != null && CampWorkSites.claim(level, pos, mob.getUUID())) { crop = pos; destination = null; actionTicks = 0; break; }
            }
            if (crop == null) { status(resident, WorkState.WAITING_FIELD, null); return; }
        }
        if (!legalCrop(level, crop)) { cancel(mob); return; }
        CampWorkSites.claim(level, crop, mob.getUUID()); Vec3 approach = approach(level, crop);
        if (!walk(level, approach, resident, WorkState.WALKING)) return;
        status(resident, WorkState.HARVESTING, crop); mob.getLookControl().setLookAt(Vec3.atCenterOf(crop));
        if (actionTicks++ % 10 == 0) mob.swing(InteractionHand.MAIN_HAND);
        if (actionTicks < 30) return;
        var state = level.getBlockState(crop); CropBlock block = (CropBlock) state.getBlock();
        if (!ForgeEventFactory.onEntityDestroyBlock(mob, crop, state)) { cancel(mob); return; }
        ItemStack seed = block.getCloneItemStack(level, crop, state);
        List<ItemStack> drops = Block.getDrops(state, level, crop, level.getBlockEntity(crop), mob, mob.getMainHandItem());
        boolean reserved = false;
        for (ItemStack stack : drops) if (ItemStack.isSameItemSameTags(stack, seed) && !stack.isEmpty()) { stack.shrink(1); reserved = true; break; }
        ItemStack warehouseSeed = ItemStack.EMPTY;
        if (!reserved) for (CampKitchen.Source source : CampKitchen.sources(level, camp)) {
            if (!ItemStack.isSameItemSameTags(seed, source.prototype())) continue;
            var handler = CampWarehouse.inventory(level, source.pos()); if (handler == null) continue;
            warehouseSeed = handler.extractItem(source.slot(), 1, false);
            if (!warehouseSeed.isEmpty() && (!ItemStack.isSameItemSameTags(warehouseSeed, seed) || warehouseSeed.getCount() != 1)) {
                CampWorkBuffer.addCargo(mob, List.of(warehouseSeed)); cancel(mob); return;
            }
            if (!warehouseSeed.isEmpty()) break;
        }
        if (seed.isEmpty() || !reserved && warehouseSeed.isEmpty()) {
            cancel(mob); status(resident, WorkState.WAITING_SEEDS, crop); return;
        }
        if (!legalCrop(level, crop) || !level.setBlock(crop, block.getStateForAge(0), Block.UPDATE_ALL)) {
            if (!warehouseSeed.isEmpty()) CampWorkBuffer.addCargo(mob, List.of(warehouseSeed)); cancel(mob); return;
        }
        level.levelEvent(2001, crop, Block.getId(state)); CampWorkBuffer.addCargo(mob, drops);
        camp.prosperity++; cancel(mob); status(resident, WorkState.DELIVERING, null); LatexSettlementData.get(level.getServer()).setDirty();
    }
    private boolean legalCrop(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state) && level.getBlockState(pos.below()).is(CampWorkSites.FARMLAND)
                && crop.canSurvive(state, level, pos);
    }
    private void cooking(ServerLevel level, Settlement camp, Resident resident) {
        CompoundTag batch = CampWorkBuffer.kitchen(mob);
        if (batch.isEmpty()) {
            if (plan == null) {
                if (!searchReady(level)) return;
                status(resident, WorkState.SEARCHING, null);
                var search = CampKitchen.find(level, camp, mob); plan = search.plan(); fetchIndex = 0;
                if (plan == null) { status(resident, search.waiting(), null); return; }
                if (!CampWorkSites.claim(level, plan.station(), mob.getUUID())) { plan = null; return; }
            }
            if (!level.hasChunkAt(plan.station()) || !CampWorkSites.claim(level, plan.station(), mob.getUUID())) { cancel(mob); return; }
            List<BlockPos> sources = new ArrayList<>();
            plan.choices().forEach(choice -> { if (!sources.contains(choice.source().pos())) sources.add(choice.source().pos()); });
            if (plan.fuel() != null && !sources.contains(plan.fuel().source().pos())) sources.add(plan.fuel().source().pos());
            Vec3 source = approach(level, sources.get(fetchIndex));
            if (!walk(level, source, resident, WorkState.FETCHING)) return;
            if (++fetchIndex < sources.size()) { destination = null; return; }
            batch = CampKitchen.withdraw(level, camp, mob, plan); plan = null; destination = null;
            if (batch == null) { cancel(mob); status(resident, WorkState.WAITING_MATERIALS, null); return; }
        }
        BlockPos station = BlockPos.of(batch.getLong("Station"));
        if (!batch.getString("Dimension").equals(level.dimension().location().toString()) || !CampWorkSites.inside(camp, station)
                || !level.hasChunkAt(station) || !CampWorkSites.claim(level, station, mob.getUUID())) { cancel(mob); return; }
        Vec3 approach = approach(level, station);
        if (!walk(level, approach, resident, WorkState.WALKING)) return;
        status(resident, WorkState.COOKING, station); mob.getLookControl().setLookAt(Vec3.atCenterOf(station));
        ResourceLocation id = ResourceLocation.tryParse(batch.getString("Recipe"));
        Recipe<?> recipe = id == null ? null : level.getRecipeManager().byKey(id).orElse(null);
        if (batch.getString("Kind").equals(CampKitchen.StationKind.SMELTING.name())) { furnace(level, camp, resident, station, batch, recipe); return; }
        if (!(recipe instanceof CraftingRecipe crafting) || !level.getBlockState(station).is(CampWorkSites.CRAFTING)) { cancel(mob); return; }
        var grid = CampKitchen.grid(CampWorkBuffer.ingredients(batch));
        if (!crafting.matches(grid, level)) { cancel(mob); return; }
        int ticks = batch.getInt("Ticks") + 1; batch.putInt("Ticks", ticks);
        if (ticks % 15 == 0) { mob.swing(InteractionHand.MAIN_HAND); level.playSound(null, station, SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 0.3F, 1); }
        if (ticks < batch.getInt("Duration")) return;
        ItemStack result = crafting.assemble(grid, level.registryAccess());
        // stock includes this paid batch's reservation; remove it for the final capacity check.
        int reserved = batch.getString("ResultItem").equals(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(result.getItem()).toString())
                ? batch.getInt("ResultCount") : 0;
        if (CampKitchen.score(result, mob) < 0 || CampKitchen.stock(level, camp, result) - reserved + result.getCount() > CampKitchen.FOOD_LIMIT) { cancel(mob); return; }
        List<ItemStack> products = new ArrayList<>(crafting.getRemainingItems(grid)); products.add(result);
        CampWorkBuffer.clearKitchen(mob); CampWorkBuffer.addCargo(mob, products); camp.prosperity++;
        cancel(mob); status(resident, WorkState.DELIVERING, null); LatexSettlementData.get(level.getServer()).setDirty();
    }
    private void furnace(ServerLevel level, Settlement camp, Resident resident, BlockPos pos, CompoundTag batch, Recipe<?> recipe) {
        boolean smoker = recipe != null && recipe.getType() == RecipeType.SMOKING;
        if (!(recipe instanceof AbstractCookingRecipe cooking) || !level.getBlockState(pos).is(smoker ? Blocks.SMOKER : Blocks.FURNACE)
                || !(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace)) { cancel(mob); return; }
        if (!batch.getString("Phase").equals("FURNACE")) {
            var items = CampWorkBuffer.ingredients(batch); ItemStack input = items.get(0), fuel = ItemStack.of(batch.getCompound("Fuel"));
            if (!furnace.getItem(0).isEmpty() || !furnace.getItem(2).isEmpty() || !cooking.matches(new SimpleContainer(input), level)
                    || !fuel.isEmpty() && !furnace.getItem(1).isEmpty()) { cancel(mob); return; }
            furnace.setItem(0, input); if (!fuel.isEmpty()) furnace.setItem(1, fuel); furnace.setChanged();
            batch.remove("Items"); batch.remove("Fuel"); batch.putString("Phase", "FURNACE"); batch.putLong("Started", level.getGameTime());
        }
        ItemStack expected = cooking.getResultItem(level.registryAccess()), actual = furnace.getItem(2);
        if (!actual.isEmpty() && ItemStack.isSameItemSameTags(actual, expected) && actual.getCount() >= batch.getInt("ResultCount")) {
            ItemStack food = furnace.removeItem(2, batch.getInt("ResultCount")); furnace.setChanged();
            CampWorkBuffer.clearKitchen(mob); CampWorkBuffer.addCargo(mob, List.of(food)); camp.prosperity++;
            cancel(mob); status(resident, WorkState.DELIVERING, null); LatexSettlementData.get(level.getServer()).setDirty();
        } else if (furnace.getItem(0).isEmpty() && actual.isEmpty() || level.getGameTime() - batch.getLong("Started") > 2400) {
            // Inputs/fuel already belong to the real furnace. Cancellation never recreates them.
            cancel(mob); status(resident, WorkState.WAITING_MATERIALS, pos);
        }
    }
}
