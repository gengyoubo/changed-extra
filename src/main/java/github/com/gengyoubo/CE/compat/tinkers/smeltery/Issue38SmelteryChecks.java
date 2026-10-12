package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipe;
import slimeknights.tconstruct.library.recipe.entitymelting.EntityMeltingRecipeCache;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Opt-in integration checks for real fuel consumption and native recipe/entity paths. */
public final class Issue38SmelteryChecks {
    private Issue38SmelteryChecks() {}
    private static Block block(String id) { return ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(id)); }
    public static void run(ServerLevel level, BiConsumer<Boolean, String> check) {
        var action = IFluidHandler.FluidAction.EXECUTE;
        for (var entry : List.of(MoltenLatexFluids.DARK, MoltenLatexFluids.WHITE)) {
            check.accept(entry.source.get() != net.ltxprogrammer.changed.init.ChangedFluids.DARK_LATEX.get()
                    && entry.source.get() != net.ltxprogrammer.changed.init.ChangedFluids.WHITE_LATEX.get(), "Molten latex is a distinct fluid");
            check.accept(entry.type.get().getTemperature() == 2273, "Fluid temperature stores 2000 C as 2273 K");
            var fuelRecipe = slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup.findFuel(entry.source.get());
            check.accept(fuelRecipe != null && fuelRecipe.getTemperature() == 2000 && fuelRecipe.getAmount(entry.source.get()) == 50,
                    "Molten latex fuel recipe loads at 2000 C / 50 mB");
        }
        var original = new ArrayList<>(level.getRecipeManager().getRecipes());
        var recipes = new ArrayList<>(original);
        recipes.add(new MeltingRecipe(ResourceLocation.parse("changede:verify/blaze"), "", Ingredient.of(Items.STICK),
                FluidOutput.fromFluid(Fluids.WATER, 90), 1400, 1, List.of()));
        recipes.add(new MeltingRecipe(ResourceLocation.parse("changede:verify/molten"), "", Ingredient.of(Items.PAPER),
                FluidOutput.fromFluid(Fluids.WATER, 90), 1800, 1, List.of()));
        level.getRecipeManager().replaceRecipes(recipes);
        try {
            for (boolean foundry : new boolean[]{false, true}) {
                BlockPos min = new BlockPos(foundry ? 144 : 128, 96, 48);
                var brick = block(foundry ? "tconstruct:scorched_bricks" : "tconstruct:seared_bricks");
                for (BlockPos pos : BlockPos.betweenClosed(min, min.offset(4, 2, 2))) {
                    boolean air = pos.getY() == min.getY() + 2 || pos.getY() == min.getY() + 1
                            && pos.getZ() == min.getZ() + 1 && pos.getX() > min.getX() && pos.getX() < min.getX() + 4;
                    level.setBlockAndUpdate(pos, air ? Blocks.AIR.defaultBlockState() : brick.defaultBlockState());
                }
                var tankBlock = block(foundry ? "tconstruct:scorched_fuel_tank" : "tconstruct:seared_fuel_tank");
                List<BlockPos> positions = List.of(min.offset(4, 1, 1), min.offset(2, 1, 0), min.offset(3, 1, 0));
                for (var pos : positions) level.setBlockAndUpdate(pos, tankBlock.defaultBlockState());
                BlockPos core = min.offset(0, 1, 1), controllerPos = min.offset(1, 1, 0);
                level.setBlockAndUpdate(core, LatexSmelteryCompat.DYNAMIC_CORE.get().defaultBlockState());
                level.setBlockAndUpdate(controllerPos, block(foundry ? "tconstruct:foundry_controller" : "tconstruct:smeltery_controller")
                        .defaultBlockState().setValue(ControllerBlock.FACING, Direction.NORTH));
                var controller = (HeatingStructureBlockEntity) level.getBlockEntity(controllerPos);
                controller.updateStructure(); HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controllerPos, controller.getBlockState(), controller);
                check.accept(controller.getStructure() != null && LatexSmelteryRules.dynamicCoreReady(controller), "Dynamic core forms in " + (foundry ? "foundry" : "smeltery"));
                var tanks = positions.stream().map(pos -> ((TankBlockEntity) level.getBlockEntity(pos)).getTank()).toList();
                Fluid blaze = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse("tconstruct:blazing_blood"));
                // Hottest tank is inserted first to expose the native tank-order bug.
                tanks.get(0).fill(new FluidStack(MoltenLatexFluids.DARK.source.get(), 1000), action);
                tanks.get(1).fill(new FluidStack(blaze, 1000), action);
                tanks.get(2).fill(new FluidStack(Fluids.LAVA, 1000), action);
                var inventory = controller.getMeltingInventory(); var fuel = controller.getFuelModule();
                inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
                inventory.setStackInSlot(1, new ItemStack(Items.STICK));
                inventory.setStackInSlot(2, new ItemStack(Items.PAPER));
                check.accept(fuel.findFuel(false) == 1000 && tanks.stream().allMatch(t -> t.getFluidAmount() == 1000), "Mixed recipe preview selects lava without consuming fuel");
                check.accept(fuel.findFuel(true) == 1000 && tanks.get(2).getFluidAmount() == 950 && tanks.get(1).getFluidAmount() == 1000
                        && tanks.get(0).getFluidAmount() == 1000, "Lava is consumed before rare fuel for mixed temperatures");
                inventory.setStackInSlot(0, ItemStack.EMPTY); fuel.decreaseFuel(Integer.MAX_VALUE);
                check.accept(fuel.findFuel(true) == 1500 && tanks.get(1).getFluidAmount() == 950 && tanks.get(0).getFluidAmount() == 1000,
                        "After low-temperature work completes, native cached lava is bypassed for blaze blood");
                inventory.setStackInSlot(1, ItemStack.EMPTY); fuel.decreaseFuel(Integer.MAX_VALUE);
                check.accept(fuel.findFuel(true) == 2000 && tanks.get(0).getFluidAmount() == 950, "1800 C work switches to molten latex");
                var saved = fuel.writeToTag(new net.minecraft.nbt.CompoundTag());
                var reloaded = new slimeknights.tconstruct.smeltery.block.entity.module.MultitankFuelModule(controller, () -> controller.getStructure().getTanks());
                reloaded.readFromTag(saved);
                check.accept(reloaded.getTemperature() == 2000 && ((LatexFuelAccess) reloaded).changede$latexBatch(), "Molten fuel batch survives NBT reload");
                inventory.setStackInSlot(0, new ItemStack(net.ltxprogrammer.changed.init.ChangedItems.DARK_LATEX_GOO.get()));
                var goo = inventory.getModule(0); var gooRecipe = level.getRecipeManager().getRecipeFor(
                        slimeknights.tconstruct.library.recipe.TinkerRecipeTypes.MELTING.get(), goo, level).orElseThrow();
                check.accept(gooRecipe.getOutput(goo).getAmount() == 250 && gooRecipe.getOutput(goo).getFluid() == MoltenLatexFluids.DARK.source.get(), "Black goo melts to 250 mB molten black latex");
                level.setBlockAndUpdate(core, brick.defaultBlockState());
                check.accept(!reloaded.hasFuel() && reloaded.getFuel() == 0, "Removing dynamic core stops reloaded molten fuel");
                check.accept(Block.getDrops(LatexSmelteryCompat.DYNAMIC_CORE.get().defaultBlockState(), level, core, null).stream()
                        .anyMatch(s -> s.is(LatexSmelteryCompat.DYNAMIC_CORE_ITEM.get())), "Dynamic core has native block loot");
            }
        } finally { level.getRecipeManager().replaceRecipes(original); }
        EntityMeltingRecipe recipe = (EntityMeltingRecipe) level.getRecipeManager().byKey(ResourceLocation.parse("changede:smeltery/entity_melting/latex")).orElseThrow();
        for (String id : new String[]{"changed:dark_latex_wolf_male", "changed:pure_white_latex_wolf", "changed_addon:experiment_009"}) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse(id));
            var entity = (LivingEntity) type.create(level);
            check.accept(recipe.matches(type) && EntityMeltingRecipeCache.findRecipe(level.getRecipeManager(), type) != null, "Native latex entity recipe matches " + id);
            int dark = 0, white = 0; boolean amountCorrect = true;
            for (int i = 0; i < 64; i++) {
                var output = recipe.getOutput(entity);
                amountCorrect &= output.getAmount() == 250;
                if (output.getFluid() == MoltenLatexFluids.DARK.source.get()) dark++;
                if (output.getFluid() == MoltenLatexFluids.WHITE.source.get()) white++;
            }
            check.accept(amountCorrect, "Entity output is exactly 250 mB for " + id);
            check.accept(id.contains("dark_latex") ? dark == 64 : id.contains("white_latex") ? white == 64 : dark > 0 && white > 0,
                    "Latex faction controls output; neutral creatures can produce either color: " + id);
            BlockPos pos = level.getSharedSpawnPos().above(32).offset(id.contains("dark_latex") ? 0 : id.contains("white_latex") ? 8 : 16, 0, 0);
            level.getChunkAt(pos); entity.moveTo(pos, 0, 0); ((Mob) entity).setNoAi(true); level.addFreshEntity(entity);
            entity.tickCount = 40; // Changed ignores all damage for the first 30 ticks after spawning.
            var outputTank = new net.minecraftforge.fluids.capability.templates.FluidTank(10000);
            var parent = new LatexSmelteryCompat.CoreBlockEntity(pos, LatexSmelteryCompat.CORE.get().defaultBlockState()); parent.setLevel(level);
            var module = new slimeknights.tconstruct.smeltery.block.entity.module.EntityMeltingModule(parent, outputTank, () -> true,
                    stack -> stack, () -> new net.minecraft.world.phys.AABB(pos).inflate(2));
            boolean interacted = module.interactWithEntities();
            check.accept(interacted && outputTank.getFluidAmount() == 250 && entity.getHealth() < entity.getMaxHealth(),
                    "Native smeltery damages the creature and deposits 250 mB: " + id + " (interacted=" + interacted
                            + ", fluid=" + outputTank.getFluidAmount() + ", health=" + entity.getHealth() + "/" + entity.getMaxHealth() + ")");
            entity.discard();
        }
    }
}
