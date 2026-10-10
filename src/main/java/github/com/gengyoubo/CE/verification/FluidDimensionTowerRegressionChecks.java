package github.com.gengyoubo.CE.verification;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.FluidDimensionSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.energy.DimensionTowerManager;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.world.Menu.FluidDimensionSpaceTowerMenu;
import net.ltxprogrammer.changed.init.ChangedFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Files;
import java.nio.file.Path;

/** Opt-in verification in a disposable world; never enabled in a normal game. */
@Mod.EventBusSubscriber(modid = "changede")
public final class FluidDimensionTowerRegressionChecks {
    private static final BlockPos SOURCE = new BlockPos(6015, 160, 6015);
    private static final BlockPos TARGET = new BlockPos(8015, 160, 8015);
    private static MinecraftServer server;
    private static ServerLevel remote;
    private static int elapsed;
    private static int assertions;
    private static boolean restart;

    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        assertions++;
        LogManager.getLogger("changede-fluid-dimension-check").info("PASS: {}", message);
    }

    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("changede.verifyFluidDimensionTower")) return;
        server = event.getServer();
        restart = Boolean.getBoolean("changede.verifyFluidDimensionTowerRestart");
        assertions = elapsed = 0;
        try {
            remote = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("changede:latex_space")));
            check(remote != null && server.getPlayerCount() == 0, "Real latex dimension, no players");
            // Restart phase deliberately does not request either tower's chunk.
            if (restart) return;
            var overworld = server.overworld();
            var source = place(overworld, SOURCE, "INPUT", 35, FluidStack.EMPTY);
            var target = place(remote, TARGET, "OUTPUT", 35, FluidStack.EMPTY);
            var input = port(source);
            var output = port(target);
            check(source.getLoadedChunkCount() == 4 && target.getLoadedChunkCount() == 4,
                    "Both towers anchor adjacent cross-boundary devices");
            check(source.getPeerCount() == 1 && target.getPeerCount() == 1, "Fluid peers cross dimensions");
            for (Direction side : Direction.values()) {
                check(source.getCapability(ForgeCapabilities.FLUID_HANDLER, side).isPresent(), "Fluid port on " + side);
            }
            check(!source.getCapability(ForgeCapabilities.ENERGY).isPresent(), "Fluid tower does not expose energy capability");
            check(input.fill(new FluidStack(Fluids.WATER, 60_000), FluidAction.SIMULATE) == 50_000
                    && source.getFluid().isEmpty(), "Simulated fill respects 50B without mutation");
            check(input.fill(new FluidStack(Fluids.WATER, 60_000), FluidAction.EXECUTE) == 50_000
                    && input.fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE) == 0, "Input clamps at 50B");
            check(input.drain(1_000, FluidAction.EXECUTE).isEmpty()
                    && output.fill(new FluidStack(Fluids.WATER, 1_000), FluidAction.EXECUTE) == 0, "Ports enforce direction");
            DimensionTowerManager.fluidNetwork(server).tick();
            check(source.getFluid().isEmpty() && target.getFluid().getAmount() == 50_000, "Full 50B crosses real dimensions without loss");
            check(output.drain(1_000, FluidAction.SIMULATE).getAmount() == 1_000
                    && target.getFluid().getAmount() == 50_000, "Simulated output does not mutate");
            check(output.drain(new FluidStack(Fluids.LAVA, 1_000), FluidAction.EXECUTE).isEmpty(), "Output rejects wrong requested fluid");
            input.fill(new FluidStack(Fluids.LAVA, 1_000), FluidAction.EXECUTE);
            DimensionTowerManager.fluidNetwork(server).tick();
            check(source.getFluid().getAmount() == 1_000 && target.getFluid().getFluid() == Fluids.WATER,
                    "Different fluids stay separate on one channel");
            check(output.drain(Integer.MAX_VALUE, FluidAction.EXECUTE).getAmount() == 50_000, "Output drain bounded by stored fluid");
            DimensionTowerManager.fluidNetwork(server).tick();
            check(target.getFluid().getFluid() == Fluids.LAVA && source.getFluid().isEmpty(), "Empty receiver switches fluid");
            output.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
            var tagged = new FluidStack(Fluids.WATER, 1_234);
            var fluidData = new CompoundTag();
            fluidData.putString("TestData", "A");
            tagged.setTag(fluidData);
            input.fill(tagged, FluidAction.EXECUTE);
            DimensionTowerManager.fluidNetwork(server).tick();
            check(target.getFluid().isFluidEqual(tagged) && target.getFluid().getAmount() == 1_234, "Fluid NBT survives transfer");
            var different = tagged.copy();
            different.getOrCreateTag().putString("TestData", "B");
            input.fill(different, FluidAction.EXECUTE);
            DimensionTowerManager.fluidNetwork(server).tick();
            check(source.getFluid().getAmount() == 1_234 && target.getFluid().getAmount() == 1_234, "Different fluid NBT cannot merge");
            var restored = new FluidDimensionSpaceTowerBlockEntity(TARGET, target.getBlockState());
            restored.load(target.saveWithoutMetadata());
            check(restored.getFluid().isFluidEqual(tagged) && restored.getFluid().getAmount() == 1_234
                    && restored.getChannel() == 35 && restored.isSwitchedOn(), "NBT persists fluid and settings");
            var loot = Block.getDrops(target.getBlockState(), remote, TARGET, target);
            check(loot.size() == 1 && loot.get(0).is(CELPBlock.FLUID_DIMENSION_SPACE_TOWER.get().asItem()), "Mining drops exactly one tower");
            var itemData = BlockItem.getBlockEntityData(loot.get(0));
            check(itemData != null && FluidStack.loadFluidStackFromNBT(itemData.getCompound("Fluid")).isFluidEqual(tagged)
                    && FluidStack.loadFluidStackFromNBT(itemData.getCompound("Fluid")).getAmount() == 1_234
                    && !itemData.contains("Enabled"), "Drop preserves fluid with safe disconnected placement defaults");
            var placementPos = TARGET.above(6);
            clear(remote, placementPos);
            var placementPlayer = FakePlayerFactory.getMinecraft(remote);
            placementPlayer.setItemInHand(InteractionHand.MAIN_HAND, loot.get(0).copy());
            var hit = new BlockHitResult(Vec3.atCenterOf(placementPos), Direction.UP, placementPos, false);
            var placement = new BlockPlaceContext(new UseOnContext(placementPlayer, InteractionHand.MAIN_HAND, hit));
            check(((BlockItem) loot.get(0).getItem()).place(placement).consumesAction(), "Dropped fluid tower can actually be placed");
            var placed = tower(remote, placementPos);
            check(placed.getFluid().isFluidEqual(tagged) && placed.getFluid().getAmount() == 1_234
                    && !placed.isSwitchedOn() && placed.getChannel() == 0, "Actual replacement retains liquid and starts disconnected");
            clear(remote, placementPos);
            var player = FakePlayerFactory.getMinecraft(overworld);
            var menu = new FluidDimensionSpaceTowerMenu(1, player.getInventory(), SOURCE);
            check(menu.getChannel() == 35 && menu.getFluid().getAmount() == 1_234 && menu.hasFluidTag(), "Menu sync includes channel, amount and tag indicator");
            var grid = new TransientCraftingContainer(player.inventoryMenu, 2, 2);
            grid.setItem(3, new ItemStack(CELPBlock.DIMENSION_SPACE_TOWER.get()));
            grid.setItem(0, new ItemStack(Items.BUCKET));
            var recipe = overworld.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, overworld).orElseThrow();
            check(recipe.assemble(grid, overworld.registryAccess()).is(CELPBlock.FLUID_DIMENSION_SPACE_TOWER.get().asItem()),
                    "Unordered dimension tower + bucket recipe");
            source.setChannel(36);
            output.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
            DimensionTowerManager.fluidNetwork(server).tick();
            check(source.getFluid().getAmount() == 1_234 && target.getFluid().isEmpty(), "Other channel cannot receive");
            source.setChannel(35);
            source.toggleEnabled();
            check(source.getLoadedChunkCount() == 0 && !source.active()
                    && input.fill(tagged, FluidAction.EXECUTE) == 0 && target.getPeerCount() == 0, "Disabled tower releases anchor and rejects transfers");
            source.toggleEnabled();
            source.setChannel(0);
            check(source.getLoadedChunkCount() == 0, "Channel zero releases anchor");
            source.setChannel(35);
            source.cycleRedstoneMode();
            check(!source.active() && source.getLoadedChunkCount() == 0, "Redstone pause releases anchor");
            source.cycleRedstoneMode(); // on with signal
            check(!source.active(), "Redstone on mode pauses without power");
            source.cycleRedstoneMode(); // off with signal
            check(source.active(), "Redstone off mode runs without power");
            source.cycleRedstoneMode();
            target.cycleMode(); // output -> none
            check(!target.active() && output.drain(1, FluidAction.EXECUTE).isEmpty(), "Disabled transfer mode blocks ports");
            target.cycleMode();
            target.cycleMode(); // input -> output
            // Test neighboring native mod fluid handlers in both directions.
            clear(overworld, SOURCE); clear(remote, TARGET);
            source = place(overworld, SOURCE, "INPUT", 35, FluidStack.EMPTY);
            target = place(remote, TARGET, "OUTPUT", 35, FluidStack.EMPTY);
            placePump(overworld, SOURCE.east()); placePump(remote, TARGET.east());
            var latex = new FluidStack(ChangedFluids.DARK_LATEX.get(), 4_000);
            pumpPort(overworld, SOURCE.east()).fill(latex, FluidAction.EXECUTE);
            source.tick();
            check(source.getFluid().getAmount() == 4_000 && pumpPort(overworld, SOURCE.east()).getFluidInTank(0).isEmpty(), "Tower pulls adjacent fluid through Forge capability");
            DimensionTowerManager.fluidNetwork(server).tick(); target.tick();
            check(pumpPort(remote, TARGET.east()).getFluidInTank(0).getAmount() == 4_000 && target.getFluid().isEmpty(), "Tower pushes to adjacent device across dimension");
            // Two receivers fill independently; incompatible receiver cannot block another one.
            var second = place(remote, TARGET.above(3), "OUTPUT", 35, new FluidStack(Fluids.LAVA, 100));
            port(source).fill(new FluidStack(Fluids.WATER, 50_000), FluidAction.EXECUTE);
            DimensionTowerManager.fluidNetwork(server).tick();
            check(target.getFluid().getAmount() == 50_000 && second.getFluid().getAmount() == 100
                    && source.getFluid().isEmpty(), "Incompatible receiver does not block compatible receiver");
            clear(remote, TARGET.above(3));
            var stalePort = target.getCapability(ForgeCapabilities.FLUID_HANDLER);
            clear(remote, TARGET);
            check(!stalePort.isPresent() && source.getPeerCount() == 0, "Removal invalidates capability and unregisters endpoint");
            // Repeated observation and restart fixture: no world/player chunk requests after setup.
            clear(overworld, SOURCE); clear(overworld, SOURCE.east()); clear(remote, TARGET.east());
            place(overworld, SOURCE, "INPUT", 35, new FluidStack(Fluids.WATER, 5_000));
            place(remote, TARGET, "OUTPUT", 35, FluidStack.EMPTY);
            placePump(remote, TARGET.east());
        } catch (Throwable failure) {
            finish(false, failure);
            throw failure;
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (server == null || event.getServer() != server || event.phase != TickEvent.Phase.END || ++elapsed < 600) return;
        try {
            check(server.getPlayerCount() == 0 && server.overworld().hasChunkAt(SOURCE) && remote.hasChunkAt(TARGET.east()),
                    "Both dimensions and adjacent device remain loaded with no players for 600 ticks");
            var source = tower(server.overworld(), SOURCE);
            var target = tower(remote, TARGET);
            check(source.active() && target.active() && target.getPeerCount() == 1, "Fluid channel active after observation or restart");
            check(pumpPort(remote, TARGET.east()).getFluidInTank(0).getAmount() == 5_000
                    && target.getFluid().isEmpty() && source.getFluid().isEmpty(), "Remote adjacent device receives exactly 5B without loss or duplication");
            if (restart) {
                clear(server.overworld(), SOURCE); clear(remote, TARGET);
                check(ticketCount(server.overworld()) == 0 && ticketCount(remote) == 0, "Removal releases restored tickets in both dimensions");
            } else {
                pumpPort(remote, TARGET.east()).drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
                port(source).fill(new FluidStack(Fluids.WATER, 5_000), FluidAction.EXECUTE);
            }
            finish(true, null);
        } catch (Throwable failure) { finish(false, failure); throw failure; }
    }

    private static void finish(boolean passed, Throwable failure) {
        try {
            Files.writeString(Path.of("fluid-dimension-tower-checks.txt"), passed
                    ? "PASS: " + assertions + " assertions; " + (restart ? "restart" : "prepare") : "FAIL: " + failure);
        } catch (Exception e) { throw new RuntimeException(e); }
        finally { var finished = server; server = null; if (finished != null) finished.halt(false); }
    }
    private static void clear(ServerLevel level, BlockPos pos) { level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); }
    private static FluidDimensionSpaceTowerBlockEntity tower(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FluidDimensionSpaceTowerBlockEntity tower) return tower;
        throw new AssertionError("Missing fluid tower at " + pos);
    }
    private static FluidDimensionSpaceTowerBlockEntity place(ServerLevel level, BlockPos pos, String mode, int channel, FluidStack fluid) {
        clear(level, pos); level.setBlockAndUpdate(pos, CELPBlock.FLUID_DIMENSION_SPACE_TOWER.get().defaultBlockState());
        var tower = tower(level, pos);
        var tag = new CompoundTag(); tag.putInt("Channel", channel); tag.putBoolean("Enabled", true); tag.putString("Mode", mode);
        tag.put("Fluid", fluid.writeToNBT(new CompoundTag())); tower.load(tag); tower.setChanged(); tower.refreshNetwork();
        return tower;
    }
    private static IFluidHandler port(FluidDimensionSpaceTowerBlockEntity tower) {
        return tower.getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(() -> new AssertionError("Missing fluid capability"));
    }
    private static void placePump(ServerLevel level, BlockPos pos) {
        clear(level, pos); clear(level, pos.below()); level.setBlockAndUpdate(pos, CELPBlock.BASIC_PUMP.get().defaultBlockState());
    }
    private static IFluidHandler pumpPort(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(() -> new AssertionError("Missing device fluid capability"));
    }
    private static int ticketCount(ServerLevel level) {
        ForcedChunksSavedData data = level.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
        return data == null ? 0 : data.getBlockForcedChunks().getTickingChunks().values().stream().mapToInt(java.util.Set::size).sum();
    }
}
