package github.com.gengyoubo.CE.verification;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.DimensionSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.ElectricFurnaceBlockEntity;
import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.energy.DimensionTowerChannels;
import github.com.gengyoubo.CE.LP.energy.DimensionTowerManager;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Opt-in real server checks. Run only with the disposable runDimensionTowerServer world. */
@Mod.EventBusSubscriber(modid = "changede")
@SuppressWarnings("deprecation")

public final class DimensionTowerRegressionChecks {
    private static final BlockPos SOURCE = new BlockPos(2048, 160, 2048);
    private static final BlockPos TARGET = new BlockPos(4095, 160, 4095);
    private static final BlockPos MACHINE = TARGET.east();
    private static final int OBSERVATION_TICKS = 600;
    private static final Path BASELINE = Path.of("dimension-tower-baseline.txt");
    private static MinecraftServer server;
    private static ServerLevel remote;
    private static int elapsed;
    private static int initialOutput;
    private static boolean restart;

    private DimensionTowerRegressionChecks() { }

    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        LogManager.getLogger("changede-dimension-check").info("PASS: {}", message);
    }

    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("changede.verifyDimensionTower")) return;
        server = event.getServer();
        elapsed = 0;
        restart = Boolean.getBoolean("changede.verifyDimensionTowerRestart");
        remote = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("changede", "latex_space")));
        check(remote != null, "Latex dimension exists");
        check(server.getPlayerCount() == 0, "Test runs with no players");
        if (restart) {
            // Do not fetch the tower chunk: startup must restore it solely from the Forge ticket.
            try {
                initialOutput = Integer.parseInt(Files.readString(BASELINE).trim());
            } catch (IOException e) {
                throw new UncheckedIOException("Run the prepare phase first to save the restart baseline", e);
            }
            return;
        }
        initialOutput = 0;
        ServerLevel overworld = server.overworld();
        placeTower(overworld, SOURCE, "INPUT", "LP", 40_000);
        placeTower(remote, TARGET, "OUTPUT", "LP", 0);
        remote.setBlockAndUpdate(MACHINE, Blocks.AIR.defaultBlockState());
        remote.setBlockAndUpdate(MACHINE, CELPBlock.ELECTRIC_FURNACE.get().defaultBlockState());
        furnace().getItemHandler().setStackInSlot(0, new ItemStack(Items.IRON_ORE, 64));
        check(tower(remote, TARGET).getLoadedChunkCount() == 4, "Corner tower owns four ticking tickets");
        check(ticketCount(remote) == 4, "Forge saved ticking tickets match the footprint");

        // Actual same-chunk owners must remain independent on release.
        BlockPos second = TARGET.west();
        var sibling = placeTower(remote, second, "INPUT", "LP", 0);
        check(ticketCount(remote) == 6, "Overlapping towers own separate tickets");
        remote.setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
        check(ticketCount(remote) == 4 && tower(remote, TARGET).active(), "Removing one owner retains other tower tickets");
        check(sibling.isRemoved(), "Removed block entity is invalidated");

        // WLP and DLP are real typed block entities, not only fake channel endpoints.
        for (String type : new String[]{"WLP", "DLP"}) {
            BlockPos a = SOURCE.above(type.equals("WLP") ? 3 : 6);
            BlockPos b = TARGET.above(type.equals("WLP") ? 3 : 6);
            var sender = placeTower(overworld, a, "INPUT", type, 123);
            var receiver = placeTower(remote, b, "OUTPUT", type, 0);
            DimensionTowerManager.network(server).tick();
            check(sender.stored() == 0 && receiver.stored() == 123, type + " transfers its entire buffer between real dimensions");
            check(receiver.receiveEnergy(50, null) == 0, type + " endpoint rejects LP");
            LatexEnergyType energyType = LatexEnergyType.valueOf(type);
            check(receiver.extractTypedEnergy(energyType, Integer.MAX_VALUE) == 123, type + " output is bounded by stored energy");
            check(sender.receiveTypedEnergy(energyType, 20_000) == 20_000
                    && sender.receiveTypedEnergy(energyType, 30_000) == 30_000
                    && sender.receiveTypedEnergy(energyType, 1) == 0,
                    type + " repeated same-tick input fills only to capacity without a shared rate limit");
            DimensionTowerManager.network(server).tick();
            check(sender.stored() == 0 && receiver.stored() == 50_000,
                    type + " full buffer crosses dimensions in one tick");
            check(receiver.extractTypedEnergy(energyType, 20_000) == 20_000
                    && receiver.extractTypedEnergy(energyType, 30_000) == 30_000
                    && receiver.extractTypedEnergy(energyType, 1) == 0,
                    type + " repeated same-tick output drains only stored energy without a shared rate limit");
            sender.receiveTypedEnergy(energyType, 123);
            CompoundTag saved = sender.saveWithoutMetadata();
            var restored = new DimensionSpaceTowerBlockEntity(a, sender.getBlockState());
            restored.load(saved);
            check(restored.stored() == 123 && restored.getChannel() == 35 && restored.isSwitchedOn(), type + " persists configuration and buffer");
            overworld.setBlockAndUpdate(a, Blocks.AIR.defaultBlockState());
            remote.setBlockAndUpdate(b, Blocks.AIR.defaultBlockState());
        }
        // Off/channel-zero/disabled-mode/redstone must all release the owner immediately.
        var receiver = tower(remote, TARGET);
        receiver.toggleEnabled();
        check(ticketCount(remote) == 0 && !receiver.active(), "Disabling releases anchor and network");
        receiver.toggleEnabled();
        receiver.setChannel(0);
        check(ticketCount(remote) == 0, "Clearing channel releases anchor");
        receiver.setChannel(35);
        receiver.cycleMode(); // OUTPUT -> NONE
        check(ticketCount(remote) == 0, "Disabled transfer mode releases anchor");
        receiver.cycleMode(); // NONE -> INPUT
        receiver.cycleMode(); // INPUT -> OUTPUT
        receiver.cycleRedstoneMode(); // ALWAYS_ON -> ALWAYS_OFF
        check(ticketCount(remote) == 0, "Redstone pause releases anchor");
        receiver.cycleRedstoneMode(); // ON_WITH_REDSTONE
        receiver.cycleRedstoneMode(); // OFF_WITH_REDSTONE
        receiver.cycleRedstoneMode(); // ALWAYS_ON
        check(ticketCount(remote) == 4 && receiver.active(), "Re-enabling restores ticking tickets");
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (server == null || event.getServer() != server || event.phase != TickEvent.Phase.END) return;
        elapsed++;
        if (elapsed < OBSERVATION_TICKS) return;
        try {
            check(server.getPlayerCount() == 0, "No players entered either dimension");
            check(remote.hasChunkAt(TARGET) && remote.hasChunkAt(MACHINE), "Tower and cross-boundary machine remain loaded after " + OBSERVATION_TICKS + " ticks");
            var receiver = tower(remote, TARGET);
            check(receiver.active() && receiver.getPeerCount() == 1, "Cross-dimension network remains live");
            int output = furnace().getItemHandler().getStackInSlot(1).getCount();
            check(output >= initialOutput + 4, "Remote furnace keeps smelting with nobody in latex space (before=" + initialOutput + ", after=" + output + ")");
            check(ticketCount(remote) == 4, "Runtime owns exactly the expected four tickets");
            if (restart) {
                remote.setBlockAndUpdate(TARGET, Blocks.AIR.defaultBlockState());
                check(ticketCount(remote) == 0, "Removal after restart releases restored tickets");
                server.overworld().setBlockAndUpdate(SOURCE, Blocks.AIR.defaultBlockState());
                check(ticketCount(server.overworld()) == 0, "Source removal releases all tickets");
                LogManager.getLogger("changede-dimension-check").info("ALL DIMENSION TOWER RESTART CHECKS PASSED");
            } else {
                // Reset the source so the second run tests fresh transfer after ticket restoration.
                CompoundTag tag = tower(server.overworld(), SOURCE).saveWithoutMetadata();
                tag.putInt("LP", 40_000);
                tower(server.overworld(), SOURCE).load(tag);
                tower(server.overworld(), SOURCE).setChanged();
                try {
                    Files.writeString(BASELINE, Integer.toString(output));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                LogManager.getLogger("changede-dimension-check").info("ALL DIMENSION TOWER SERVER CHECKS PASSED; RESTART FIXTURE SAVED");
            }
        } finally {
            MinecraftServer finished = server;
            server = null;
            remote = null;
            finished.halt(false);
        }
    }

    private static DimensionSpaceTowerBlockEntity placeTower(ServerLevel level, BlockPos pos, String mode, String type, int energy) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, CELPBlock.DIMENSION_SPACE_TOWER.get().defaultBlockState());
        var tower = tower(level, pos);
        CompoundTag tag = new CompoundTag();
        tag.putInt("Channel", 35);
        tag.putBoolean("Enabled", true);
        tag.putString("Mode", mode);
        tag.putString("EnergyType", type);
        tag.putInt(type, energy);
        tower.load(tag);
        tower.setChanged();
        tower.refreshNetwork();
        return tower;
    }

    private static DimensionSpaceTowerBlockEntity tower(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DimensionSpaceTowerBlockEntity tower) return tower;
        throw new AssertionError("Missing tower at " + pos);
    }
    private static ElectricFurnaceBlockEntity furnace() {
        if (remote.getBlockEntity(MACHINE) instanceof ElectricFurnaceBlockEntity furnace) return furnace;
        throw new AssertionError("Missing remote furnace");
    }
    private static int ticketCount(ServerLevel level) {
        ForcedChunksSavedData data = level.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
        return data == null ? 0 : data.getBlockForcedChunks().getTickingChunks().values().stream().mapToInt(java.util.Set::size).sum();
    }
}
