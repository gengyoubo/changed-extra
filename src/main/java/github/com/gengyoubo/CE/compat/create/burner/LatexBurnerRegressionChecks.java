package github.com.gengyoubo.CE.compat.create.burner;

import com.google.gson.JsonParser;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.ltxprogrammer.changed.init.ChangedFluids;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Opt-in checks; run only in the disposable runLatexBurnerServer directory. */
public final class LatexBurnerRegressionChecks {
    private static final BlockPos POS = new BlockPos(32, 96, 32);
    private static int assertions;
    private LatexBurnerRegressionChecks() {}
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
        github.com.gengyoubo.CE.changede.LOGGER.info("BURNER PASS: {}", message);
    }
    public static void started(ServerStartedEvent event) {
        try {
            check(event.getServer().getPlayerCount() == 0, "Disposable test server has no players");
            rules(); captureAndFuel(event.getServer().overworld()); captureDelivery(event.getServer().overworld()); mixers(event.getServer().overworld());
            Files.writeString(Path.of("latex-burner-checks.txt"), "PASS: " + assertions + " assertions\n");
            github.com.gengyoubo.CE.changede.LOGGER.info("LATEX BURNER CHECKS PASSED: {} assertions", assertions);
        } catch (Throwable error) {
            github.com.gengyoubo.CE.changede.LOGGER.error("LATEX BURNER CHECKS FAILED", error);
            try { Files.writeString(Path.of("latex-burner-checks.txt"), "FAIL: " + error + "\n"); }
            catch (Exception ignored) { }
        } finally { event.getServer().execute(() -> event.getServer().halt(false)); }
    }
    private static void rules() {
        check(LatexBurnerRules.heat(false, 100, 100) == 0, "An empty cage cannot heat from stale timers");
        check(LatexBurnerRules.heat(true, 0, 0) == 1 && LatexBurnerRules.heat(true, 1, 0) == 2
                && LatexBurnerRules.heat(true, 0, 1) == 2 && LatexBurnerRules.heat(true, 1, 1) == 3, "All four occupied heat combinations");
        for (int units : new int[]{3, 4, 6, 8}) {
            for (int duration : new int[]{1, 2, 7, 100}) {
                LatexBurnerRules.Progress progress = new LatexBurnerRules.Progress();
                int remaining = duration, ticks = 0;
                while (remaining > 0 && ticks < 1000) { remaining = progress.advance(remaining, units); ticks++; }
                check(ticks == (duration * 4 + units - 1) / units && remaining == 0, "Fixed-point speed " + units + "/4, duration " + duration);
            }
        }
        for (String id : new String[]{"changed:phage_latex_wolf_male", "changed:dark_latex_wolf_partial", "changed_addon:puro_kind_male",
                "changed:white_latex_wolf_male", "changed_addon:wolfy"}) check(LatexBurnerProfiles.find(id, "none").speedUnits() == 4, "Explicit 1x species " + id);
        for (String id : new String[]{"changed:dark_latex_double_yufeng", "changed:white_latex_knight"}) check(LatexBurnerProfiles.find(id, "none").speedUnits() == 6, "Explicit 1.5x species " + id);
        for (String id : new String[]{"changed:white_latex_knight_fusion", "changed:white_latex_centaur"}) check(LatexBurnerProfiles.find(id, "none").speedUnits() == 8, "Explicit 2x species " + id);
        check(LatexBurnerProfiles.find("changed:beifeng", "none") == null, "Unknown NONE species is rejected");
        var listener = new LatexBurnerProfiles();
        listener.apply(Map.of(ResourceLocation.parse("probe:profiles"), JsonParser.parseString("{\"entities\":{\"changed:white_latex_knight\":{\"latex\":\"white\",\"speed\":1.1}}}")), null, null);
        check(LatexBurnerProfiles.find("changed:white_latex_knight", "none").speedUnits() == 6, "Invalid data override falls back to bundled species");
        listener.apply(Map.of(ResourceLocation.parse("probe:profiles"), JsonParser.parseString("{\"entities\":{\"changed:white_latex_knight\":{\"latex\":\"white\",\"fuel_ticks\":1.5}}}")), null, null);
        check(LatexBurnerProfiles.find("changed:white_latex_knight", "none").fuelTicks() == 3600, "Fractional fuel durations are rejected");
        listener.apply(Map.of(ResourceLocation.parse("zprobe:defaults"), JsonParser.parseString("{\"defaults\":{\"white\":{\"fuel_ticks\":4000}}}"),
                ResourceLocation.parse("aprobe:species"), JsonParser.parseString("{\"entities\":{\"probe:wolf\":{\"latex\":\"white\",\"speed\":1}}}")), null, null);
        check(LatexBurnerProfiles.find("probe:wolf", "none").fuelTicks() == 4000
                && LatexBurnerProfiles.find("changed:white_latex_knight", "none").fuelTicks() == 4000, "Data-pack species inherit final defaults regardless of file order");
        listener.apply(Map.of(), null, null);
    }
    private static CompoundTag core(String id) {
        CompoundTag tag = new CompoundTag(), creature = new CompoundTag();
        creature.putString("id", id); tag.put("Creature", creature); tag.putString("RuntimeLatex", "none"); return tag;
    }
    private static LatexBurnerBlockEntity place(ServerLevel level, String id) {
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(POS, LatexBurnerCompat.BLOCK.get().defaultBlockState());
        var burner = (LatexBurnerBlockEntity) level.getBlockEntity(POS);
        if (!id.isEmpty()) burner.load(core(id));
        burner.updateHeat(); return burner;
    }
    private static void captureAndFuel(ServerLevel level) {
        var fake = FakePlayerFactory.getMinecraft(level);
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        fake.getInventory().clearContent();
        fake.setPos(POS.getX(), POS.getY(), POS.getZ());
        var type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse("changed:dark_latex_wolf_male"));
        var creature = (net.minecraft.world.entity.LivingEntity) type.create(level);
        creature.setPos(POS.getX(), POS.getY(), POS.getZ()); level.addFreshEntity(creature);
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(LatexBurnerCompat.EMPTY.get()));
        creature.getPersistentData().putUUID("changede_maid_work_owner", fake.getUUID());
        check(LatexBurnerCapture.capture(fake.getMainHandItem(), fake, creature, InteractionHand.MAIN_HAND) == InteractionResult.FAIL
                && !creature.isRemoved(), "Working creature rejected without losing entity or cage");
        creature.getPersistentData().remove("changede_maid_work_owner");
        check(fake.interactOn(creature, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS
                && creature.isRemoved() && fake.getMainHandItem().is(LatexBurnerCompat.FILLED.get()), "Survival right-click retains the filled cage after vanilla interaction cleanup");
        CompoundTag captured = fake.getMainHandItem().getTagElement("BlockEntityTag").copy();
        check(captured.getCompound("Creature").getUUID("UUID").equals(creature.getUUID()), "Capture preserves creature UUID in server snapshot");
        var empty = place(level, "");
        check(!empty.hasCreature() && !empty.items().insertItem(0, new ItemStack(ChangedItems.DARK_LATEX_GOO.get()), true).isEmpty(), "Empty cage rejects item fuel");
        check(empty.fluids().fill(new FluidStack(ChangedFluids.DARK_LATEX.get(), 250), IFluidHandler.FluidAction.EXECUTE) == 0, "Empty cage rejects fluid fuel");
        empty.load(captured); empty.updateHeat();
        check(empty.heat() == com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.SMOULDERING, "Captured cage provides passive heat");
        check(empty.items().insertItem(0, new ItemStack(Items.COAL), false).getCount() == 1, "Coal is not a latex fuel");
        empty.items().insertItem(0, new ItemStack(ChangedItems.DARK_LATEX_GOO.get(), 3), true);
        check(empty.items().getStackInSlot(0).isEmpty(), "Item simulation cannot pay or consume fuel");
        empty.items().insertItem(0, new ItemStack(ChangedItems.DARK_LATEX_GOO.get(), 3), false);
        check(empty.fluids().fill(new FluidStack(ChangedFluids.WHITE_LATEX.get(), 249), IFluidHandler.FluidAction.EXECUTE) == 249, "Cross-color fluid input is accepted");
        empty.tick();
        check(empty.itemTicks() == 2400 && empty.fluidTicks() == 0 && empty.items().getStackInSlot(0).getCount() == 2, "One item payment starts one ordinary fuel lane; 249 mB waits");
        check(BoilerHeater.findHeat(level, POS, empty.getBlockState()) == 1, "Ordinary heat is registered with Create boiler API");
        empty.fluids().fill(new FluidStack(ChangedFluids.WHITE_LATEX.get(), 1), IFluidHandler.FluidAction.EXECUTE); empty.tick();
        check(empty.itemTicks() == 2399 && empty.fluidTicks() == 2400 && empty.fluids().getFluidInTank(0).isEmpty(), "250 mB starts independent liquid payment without resetting item time");
        check(BoilerHeater.findHeat(level, POS, empty.getBlockState()) == 2, "Dual active lanes provide superheat to boiler");
        var arm = ArmInteractionPointType.getPrimaryType(level, POS, empty.getBlockState());
        check(arm != null && arm.createPoint(level, POS, empty.getBlockState()).insert(new ItemStack(ChangedItems.DARK_LATEX_GOO.get()), true).isEmpty(), "Create arm discovers and simulates the fuel input");
        int itemTime = empty.itemTicks(), fluidTime = empty.fluidTicks();
        CompoundTag packet = empty.writeClient(new CompoundTag());
        check(!packet.contains("Creature") && packet.getInt("SpeedUnits") == 4, "Client receives a summary, never a full entity snapshot");
        CompoundTag carry = empty.carriedStack().getTagElement("BlockEntityTag");
        var restored = new LatexBurnerBlockEntity(POS, empty.getBlockState()); restored.setLevel(level); restored.load(carry);
        check(restored.itemTicks() == itemTime && restored.fluidTicks() == fluidTime && restored.items().getStackInSlot(0).getCount() == 2, "Save/load preserves paid time and remaining item stock");
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ChangedItems.WHITE_LATEX_BUCKET.get()));
        ((LatexBurnerBlock) LatexBurnerCompat.BLOCK.get()).use(empty.getBlockState(), level, POS, fake, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(POS), Direction.UP, POS, false));
        check(empty.fluids().getFluidInTank(0).getAmount() == 1000 && fake.getMainHandItem().is(Items.BUCKET)
                && empty.itemTicks() == itemTime && empty.fluidTicks() == fluidTime, "Bucket returns its container and fuels only the fluid inventory");
        empty.fluids().fill(new FluidStack(ChangedFluids.WHITE_LATEX.get(), 2501), IFluidHandler.FluidAction.EXECUTE);
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ChangedItems.WHITE_LATEX_BUCKET.get()));
        check(((LatexBurnerBlock) LatexBurnerCompat.BLOCK.get()).use(empty.getBlockState(), level, POS, fake, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(POS), Direction.UP, POS, false)) == InteractionResult.FAIL
                && fake.getMainHandItem().is(ChangedItems.WHITE_LATEX_BUCKET.get()) && empty.fluids().getFluidInTank(0).getAmount() == 3501,
                "Bucket insertion is atomic when a full bucket cannot fit");
        var drops = Block.getDrops(empty.getBlockState(), level, POS, empty, fake, ItemStack.EMPTY);
        check(drops.size() == 1 && drops.get(0).is(LatexBurnerCompat.FILLED.get())
                && drops.get(0).getTagElement("BlockEntityTag").getInt("ItemBurnTicks") == itemTime, "Normal drops contain exactly one filled cage with paid time");
        var retainedItems = empty.items(); var retainedFluids = empty.fluids();
        var cap = empty.getCapability(ForgeCapabilities.ITEM_HANDLER);
        ((LatexBurnerBlock) LatexBurnerCompat.BLOCK.get()).onSneakWrenched(empty.getBlockState(),
                new UseOnContext(fake, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(POS), Direction.UP, POS, false)));
        check(level.isEmptyBlock(POS) && !cap.isPresent() && retainedItems.extractItem(0, 64, false).isEmpty()
                && retainedFluids.drain(4000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Wrench removal invalidates capabilities and retained transfer handles");
    }
    private static Block createBlock(String path) { return ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("create:" + path)); }
    private static void captureDelivery(ServerLevel level) {
        var fake = FakePlayerFactory.getMinecraft(level);
        var type = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.parse("changed:dark_latex_wolf_male"));
        for (boolean creative : new boolean[]{false, true}) {
            for (InteractionHand hand : InteractionHand.values()) {
                for (int count : new int[]{1, 3}) {
                    for (boolean full : new boolean[]{false, true}) {
                        fake.getInventory().clearContent();
                        fake.setGameMode(creative ? net.minecraft.world.level.GameType.CREATIVE : net.minecraft.world.level.GameType.SURVIVAL);
                        if (full) for (int slot = 0; slot < fake.getInventory().items.size(); slot++)
                            fake.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
                        fake.setItemInHand(hand, new ItemStack(LatexBurnerCompat.EMPTY.get(), count));
                        var creature = (net.minecraft.world.entity.LivingEntity) type.create(level);
                        creature.setPos(POS.getX(), POS.getY(), POS.getZ());
                        level.addFreshEntity(creature);
                        String context = (creative ? "Creative" : "Survival") + " " + hand + " x" + count + (full ? " full inventory" : " free inventory");
                        check(fake.interactOn(creature, hand).consumesAction() && creature.isRemoved(), context + " captures through the real player interaction");
                        int retained = 0;
                        for (var stack : fake.getInventory().items) if (capturedCreature(stack, creature.getUUID())) retained += stack.getCount();
                        if (capturedCreature(fake.getOffhandItem(), creature.getUUID())) retained += fake.getOffhandItem().getCount();
                        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                                new net.minecraft.world.phys.AABB(POS).inflate(4), item -> capturedCreature(item.getItem(), creature.getUUID()));
                        int dropped = drops.stream().mapToInt(item -> item.getItem().getCount()).sum();
                        check(retained + dropped == 1, context + " delivers exactly one captured burner without loss or duplication");
                        if (!creative && count == 1) check(fake.getItemInHand(hand).is(LatexBurnerCompat.FILLED.get()) && dropped == 0,
                                context + " keeps the single replacement in the clicked hand");
                        else check(fake.getItemInHand(hand).is(LatexBurnerCompat.EMPTY.get())
                                        && fake.getItemInHand(hand).getCount() == count - (creative ? 0 : 1),
                                context + " consumes exactly one empty burner only in survival");
                        if (full && (creative || count > 1)) check(dropped == 1, context + " drops the filled burner when inventory cannot accept it");
                        drops.forEach(net.minecraft.world.entity.item.ItemEntity::discard);
                    }
                }
            }
        }
        fake.getInventory().clearContent();
        fake.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    }
    private static boolean capturedCreature(ItemStack stack, java.util.UUID id) {
        if (!stack.is(LatexBurnerCompat.FILLED.get())) return false;
        CompoundTag data = stack.getTagElement("BlockEntityTag");
        return data != null && data.getCompound("Creature").hasUUID("UUID") && id.equals(data.getCompound("Creature").getUUID("UUID"));
    }
    private static void mixers(ServerLevel level) throws Exception {
        var recipeField = BasinOperatingBlockEntity.class.getDeclaredField("currentRecipe"); recipeField.setAccessible(true);
        for (String id : new String[]{"changed:dark_latex_wolf_pup", "changed:dark_latex_wolf_male", "changed:white_latex_knight", "changed:white_latex_knight_fusion"}) {
            level.setBlockAndUpdate(POS.above(4).east(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(POS.above(3).east(), Blocks.AIR.defaultBlockState());
            var burner = place(level, id);
            CompoundTag tag = core(id); tag.putInt("ItemBurnTicks", 1000); burner.load(tag); burner.updateHeat();
            level.setBlockAndUpdate(POS.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(POS.above(3), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(POS.above(), createBlock("basin").defaultBlockState());
            level.setBlockAndUpdate(POS.above(3), createBlock("mechanical_mixer").defaultBlockState());
            level.setBlockAndUpdate(POS.above(3).east(), createBlock("cogwheel").defaultBlockState()
                    .setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
            level.setBlockAndUpdate(POS.above(4).east(), createBlock("creative_motor").defaultBlockState()
                    .setValue(BlockStateProperties.FACING, Direction.DOWN));
            var basin = (BasinBlockEntity) level.getBlockEntity(POS.above());
            var mixer = (MechanicalMixerBlockEntity) level.getBlockEntity(POS.above(3));
            var cog = (KineticBlockEntity) level.getBlockEntity(POS.above(3).east());
            var motor = (CreativeMotorBlockEntity) level.getBlockEntity(POS.above(4).east());
            // A real motor and meshing cog keep Create's source validation active throughout the recipe.
            basin.tick(); mixer.tick(); cog.tick(); motor.tick();
            motor.generatedSpeed.setValue(64);
            for (int warmup = 0; warmup < 20; warmup++) { motor.tick(); cog.tick(); basin.tick(); mixer.tick(); }
            check(Math.abs(mixer.getSpeed()) == 64, "Mixer fixture receives real kinetic power");
            var recipe = new ProcessingRecipeBuilder<>(MixingRecipe::new, ResourceLocation.parse("probe:burner_mix"))
                    .require(Items.COBBLESTONE).output(Items.STONE).requiresHeat(HeatCondition.HEATED).duration(100).build();
            basin.inputInventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
            recipeField.set(mixer, recipe); mixer.running = true; mixer.runningTicks = 20; mixer.processingTicks = 100;
            int elapsed = 0;
            while (!basin.inputInventory.getStackInSlot(0).isEmpty() && elapsed < 200) { motor.tick(); cog.tick(); basin.tick(); mixer.tick(); elapsed++; }
            int expected = (400 + burner.profile().speedUnits() - 1) / burner.profile().speedUnits();
            check(elapsed == expected, "Real Create mixer applies species multiplier: " + id + " in " + elapsed + " ticks"
                    + " (speed=" + mixer.getSpeed() + ", remaining=" + mixer.processingTicks + ", running=" + mixer.running
                    + ", heat=" + burner.heat() + ")");
            var inventory = basin.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
            int output = 0; for (int slot = 0; slot < inventory.getSlots(); slot++) if (inventory.getStackInSlot(slot).is(Items.STONE)) output += inventory.getStackInSlot(slot).getCount();
            check(output == 1, "Accelerated completion emits exactly one batch for " + id);
            // Start another paid recipe and verify a loss of heat cannot advance it.
            basin.inputInventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
            mixer.running = true; mixer.runningTicks = 20; mixer.processingTicks = 10;
            burner.load(core(id)); burner.updateHeat(); basin.tick(); mixer.tick();
            check(mixer.processingTicks == 10 && basin.inputInventory.getStackInSlot(0).getCount() == 1, "Loss of fuel pauses an in-progress heated recipe");
        }
        level.setBlockAndUpdate(POS.above(4).east(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(POS.above(3).east(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(POS.above(3), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(POS.above(), Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
    }
}
