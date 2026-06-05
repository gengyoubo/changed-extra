package github.com.gengyoubo.CE.LP.compat;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.util.AECableType;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.SpaceTowerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;

public class Ae2SpaceTowerBridge {
    private static final double AE_TRANSFER_PER_TICK = 1_000.0D;

    private final BlockEntity owner;
    private final SpaceTowerAccess tower;
    private final IManagedGridNode node;

    public Ae2SpaceTowerBridge(BlockEntity owner, SpaceTowerAccess tower) {
        this.owner = owner;
        this.tower = tower;
        this.node = GridHelper.createManagedNode((IGridConnectedBlockEntity)owner, BlockEntityNodeListener.INSTANCE)
                .setInWorldNode(true)
                .setIdlePowerUsage(0.0D)
                .setTagName("Ae2Node")
                .setExposedOnSides(EnumSet.allOf(Direction.class));
    }

    public void onLoad(Level level, BlockPos pos) {
        if (level != null && !level.isClientSide) {
            GridHelper.onFirstTick(owner, blockEntity -> {
                Level currentLevel = blockEntity.getLevel();
                if (currentLevel != null && !currentLevel.isClientSide) {
                    node.create(currentLevel, blockEntity.getBlockPos());
                }
            });
        }
    }

    public void tick() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide || !node.isReady()) {
            return;
        }

        node.ifPresent((grid, gridNode) -> {
            IEnergyService energy = grid.getEnergyService();
            if (tower.getMode(SpaceTowerEnergyType.AE) == IOType.INPUT) {
                pullAeFromNetwork(energy);
            } else if (tower.getMode(SpaceTowerEnergyType.AE) == IOType.OUTPUT) {
                pushAeToNetwork(energy);
            }
        });
    }

    public void load(CompoundTag tag) {
        node.loadFromNBT(tag);
    }

    public void save(CompoundTag tag) {
        node.saveToNBT(tag);
    }

    public void destroy() {
        node.destroy();
    }

    public IGridNode getGridNode(Direction side) {
        return node.getNode();
    }

    public IManagedGridNode getMainNode() {
        return node;
    }

    public AECableType getCableConnectionType(Direction side) {
        return AECableType.SMART;
    }

    private void pullAeFromNetwork(IEnergyService energy) {
        int freeLp = Math.max(0, tower.getMaxEnergyStored() - tower.getEnergyStored());
        if (freeLp == 0) {
            return;
        }

        double maxAeForStorage = freeLp * SpaceTowerEnergyType.LP.joulesPerUnit() / SpaceTowerEnergyType.AE.joulesPerUnit();
        double requestedAe = Math.min(AE_TRANSFER_PER_TICK, maxAeForStorage);
        if (requestedAe < 1.0D) {
            return;
        }

        double extractedAe = energy.extractAEPower(requestedAe, Actionable.MODULATE, PowerMultiplier.ONE);
        if (extractedAe > 0.0D) {
            tower.receiveEnergyAsType(SpaceTowerEnergyType.AE, extractedAe);
        }
    }

    private void pushAeToNetwork(IEnergyService energy) {
        double demand = energy.getEnergyDemand(AE_TRANSFER_PER_TICK);
        if (demand <= 0.0D) {
            return;
        }

        double extractedAe = tower.extractEnergyAsType(SpaceTowerEnergyType.AE, demand);
        if (extractedAe <= 0.0D) {
            return;
        }

        double rejectedAe = energy.injectPower(extractedAe, Actionable.MODULATE);
        if (rejectedAe > 0.0D) {
            tower.refundEnergyAsType(SpaceTowerEnergyType.AE, rejectedAe);
        }
    }
}
