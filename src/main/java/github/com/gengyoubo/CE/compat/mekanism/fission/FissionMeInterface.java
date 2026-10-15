package github.com.gengyoubo.CE.compat.mekanism.fission;

import java.util.EnumSet;
import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.storage.StorageHelper;
import appeng.api.util.AECableType;
import appeng.capabilities.Capabilities;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.providers.IBlockProvider;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;

/** One channel; powered, permission-aware extraction directly into the reactor's shared water tank. */
public final class FissionMeInterface extends TileEntityFissionReactorCasing implements IGridConnectedBlockEntity, ReactorWaterSource {
    private final IManagedGridNode node = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .setInWorldNode(true).setIdlePowerUsage(1).setFlags(GridFlags.REQUIRE_CHANNEL)
            .setTagName("FissionMeNode").setVisualRepresentation(FissionCompat.UPGRADES.get(FissionUpgrade.ME_PORT).asItem())
            .setExposedOnSides(EnumSet.allOf(Direction.class));
    private LazyOptional<IGridConnectedBlockEntity> host = LazyOptional.of(() -> this);
    private int pendingWater;

    public FissionMeInterface(IBlockProvider provider, BlockPos pos, BlockState state) { super(provider, pos, state); }

    @Override public IManagedGridNode getMainNode() { return node; }
    @Override public AECableType getCableConnectionType(Direction side) { return AECableType.SMART; }
    @Override public void saveChanges() { markForSave(); }
    @Override public void assignOwner(Player player) { node.setOwningPlayer(player); }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) GridHelper.onFirstTick(this, tile -> {
            if (!tile.isRemoved()) tile.node.create(tile.level, tile.worldPosition);
        });
    }

    @Override public void onChunkUnloaded() { node.destroy(); super.onChunkUnloaded(); }
    @Override public void setRemoved() { node.destroy(); super.setRemoved(); }
    @Override public void invalidateCaps() { host.invalidate(); super.invalidateCaps(); }
    @Override public void reviveCaps() { super.reviveCaps(); host = LazyOptional.of(() -> this); }

    @Override public void supplyWater(FissionReactorMultiblockData reactor) {
        if (!reactor.isFormed() || !reactor.gasCoolantTank.isEmpty()) return;
        flushPending(reactor);
        if (pendingWater > 0 || !node.isActive()) return;
        var grid = node.getGrid();
        if (grid == null) return;
        int needed = reactor.fluidCoolantTank.getNeeded();
        if (needed <= 0) return;
        int rejected = reactor.fluidCoolantTank.insert(new FluidStack(Fluids.WATER, needed), Action.SIMULATE, AutomationType.EXTERNAL).getAmount();
        int requested = needed - rejected;
        if (requested <= 0) return;
        long extracted = StorageHelper.poweredExtraction(grid.getEnergyService(), grid.getStorageService().getInventory(),
                AEFluidKey.of(Fluids.WATER), requested, IActionSource.ofMachine(this), Actionable.MODULATE);
        if (extracted > 0) {
            // Persist unexpected reentrant rejection rather than deleting water or duplicating a refund.
            pendingWater = (int) extracted;
            flushPending(reactor);
            markForSave();
        }
    }

    private void flushPending(FissionReactorMultiblockData reactor) {
        if (pendingWater <= 0) return;
        int remainder = reactor.fluidCoolantTank.insert(new FluidStack(Fluids.WATER, pendingWater), Action.EXECUTE, AutomationType.EXTERNAL).getAmount();
        if (pendingWater != remainder) { pendingWater = remainder; markForSave(); }
    }

    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        node.saveToNBT(tag);
        tag.putInt("FissionPendingWater", pendingWater);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        node.loadFromNBT(tag);
        pendingWater = Math.max(0, tag.getInt("FissionPendingWater"));
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == Capabilities.IN_WORLD_GRID_NODE_HOST) return host.cast();
        return super.getCapability(capability, side);
    }
}
