package github.com.gengyoubo.CE.compat.synergy.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.parkabird.changedsynergy.ai.PlayerOutpostData;

import java.util.*;

@SuppressWarnings("deprecation")
final class CampWarehouse {
    private CampWarehouse() {}
    static BlockPos canonical(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return pos.immutable();
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            if (!level.hasChunkAt(other)) return pos.immutable();
            return pos.compareTo(other) < 0 ? pos.immutable() : other.immutable();
        }
        return pos.immutable();
    }
    static IItemHandler inventory(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        var be = level.getBlockEntity(pos);
        if (be == null) return null;
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                && !level.hasChunkAt(pos.relative(ChestBlock.getConnectedDirection(state)))) return null;
        Container vanilla = PlayerOutpostData.container(level, pos);
        if (vanilla != null) return new InvWrapper(vanilla);
        return be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
    }
    static List<IItemHandler> inventories(ServerLevel level, LatexSettlementData.Settlement camp) {
        List<IItemHandler> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (BlockPos pos : camp.storages) {
            if (!level.hasChunkAt(pos) || camp.core.distSqr(pos) > 32 * 32) continue;
            if (!seen.add(canonical(level, pos))) continue;
            IItemHandler handler = inventory(level, pos);
            if (handler != null) result.add(handler);
        }
        return result;
    }
    static List<BlockPos> nearby(ServerLevel level, LatexSettlementData.Settlement camp) {
        Set<BlockPos> result = new HashSet<>();
        int cx = camp.core.getX() >> 4, cz = camp.core.getZ() >> 4;
        for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) {
            var chunk = level.getChunkSource().getChunkNow(x, z);
            if (chunk == null) continue;
            for (BlockPos pos : chunk.getBlockEntities().keySet()) {
                if (pos.distSqr(camp.core) > 32 * 32 || inventory(level, pos) == null) continue;
                BlockPos canonical = canonical(level, pos);
                boolean claimed = LatexSettlementData.get(level.getServer()).settlements.values().stream()
                        .filter(other -> other.active && other.dimension.equals(camp.dimension))
                        .anyMatch(other -> other.storages.stream().anyMatch(link -> canonical(level, link).equals(canonical)));
                if (!claimed) result.add(canonical);
            }
        }
        return result.stream().sorted(Comparator.comparingDouble(pos -> pos.distSqr(camp.core))).limit(32).toList();
    }
    static void deposit(ServerLevel level, LatexSettlementData.Settlement camp) {
        List<IItemHandler> handlers = inventories(level, camp);
        for (int index = camp.pending.size() - 1; index >= 0; index--) {
            ItemStack stack = camp.pending.get(index);
            for (IItemHandler handler : handlers) {
                stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);
                if (stack.isEmpty()) break;
            }
            if (stack.isEmpty()) camp.pending.remove(index);
            else camp.pending.set(index, stack);
        }
    }
    static ListTag summary(ServerLevel level, LatexSettlementData.Settlement camp) {
        Map<String, CompoundTag> counts = new LinkedHashMap<>();
        for (IItemHandler handler : inventories(level, camp)) for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            String name = stack.getHoverName().getString();
            CompoundTag entry = counts.computeIfAbsent(name, k -> { CompoundTag value = new CompoundTag(); value.putString("Name", name); return value; });
            entry.putInt("Count", entry.getInt("Count") + stack.getCount());
        }
        ListTag list = new ListTag(); counts.values().stream().limit(256).forEach(list::add); return list;
    }
}
