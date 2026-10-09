package github.com.gengyoubo.CE.LP.compat.jade;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.ILatexTypedEnergyHandler;
import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.DimensionSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.I.ItemPipeBlockEntity;
import github.com.gengyoubo.CE.util.AmountFormat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

public enum LPEnergyProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String STORED_KEY = "StoredLP";
    private static final String CAPACITY_KEY = "CapacityLP";
    private static final String TOWER_ENERGY_KEY = "DimensionTowerEnergy";
    private static final String ITEM_NAME_KEY = "PipeItemName";
    private static final String ITEM_COUNT_KEY = "PipeItemCount";
    private static final String TYPED_STORED_KEY = "StoredTypedEnergy";
    private static final String TYPED_CAPACITY_KEY = "CapacityTypedEnergy";
    private static final String TYPED_TYPE_KEY = "TypedEnergyType";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        collectEnergyData(data, blockEntity);
        if (blockEntity instanceof ItemPipeBlockEntity itemPipe) {
            var contents = itemPipe.getNetworkContents();
            if (!contents.isEmpty()) {
                data.putString(ITEM_NAME_KEY, contents.getHoverName().getString());
                data.putInt(ITEM_COUNT_KEY, contents.getCount());
            }
        }
    }

    public static void collectEnergyData(CompoundTag data, BlockEntity blockEntity) {
        if (blockEntity instanceof DimensionSpaceTowerBlockEntity tower) {
            CompoundTag buffers = new CompoundTag();
            for (LatexEnergyType type : LatexEnergyType.values()) {
                buffers.putInt(type.name(), tower.getStoredEnergy(type));
            }
            data.put(TOWER_ENERGY_KEY, buffers);
            return;
        }
        if (blockEntity instanceof ILatexEnergyHandler energy && energy.getMaxEnergyStored() > 0) {
            data.putInt(STORED_KEY, energy.getEnergyStored());
            data.putInt(CAPACITY_KEY, energy.getMaxEnergyStored());
        }
        // Typed LP is already shown above. Never interpret LP as DLP.
        if (blockEntity instanceof ILatexTypedEnergyHandler energy && energy.getEnergyType() != LatexEnergyType.LP) {
            data.putInt(TYPED_STORED_KEY, energy.getTypedEnergyStored());
            data.putInt(TYPED_CAPACITY_KEY, energy.getTypedEnergyCapacity());
            data.putString(TYPED_TYPE_KEY, energy.getEnergyType().name());
        }
    }

    public static List<Component> energyLines(CompoundTag data) {
        List<Component> lines = new ArrayList<>();
        if (data.contains(TOWER_ENERGY_KEY)) {
            CompoundTag buffers = data.getCompound(TOWER_ENERGY_KEY);
            for (LatexEnergyType type : LatexEnergyType.values()) {
                lines.add(energyLine(type.name(), buffers.getInt(type.name()), DimensionSpaceTowerBlockEntity.CAPACITY));
            }
            return lines;
        }
        if (data.contains(STORED_KEY) && data.contains(CAPACITY_KEY)) {
            lines.add(energyLine("LP", data.getInt(STORED_KEY), data.getInt(CAPACITY_KEY)));
        }
        String type = data.getString(TYPED_TYPE_KEY);
        if ((type.equals("WLP") || type.equals("DLP"))
                && data.contains(TYPED_STORED_KEY) && data.contains(TYPED_CAPACITY_KEY)) {
            lines.add(energyLine(type, data.getInt(TYPED_STORED_KEY), data.getInt(TYPED_CAPACITY_KEY)));
        }
        return lines;
    }

    private static Component energyLine(String type, int stored, int capacity) {
        return Component.literal(type + ": " + AmountFormat.format(stored, capacity));
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        // Each buffer occupies its own text row, including empty buffers.
        energyLines(data).forEach(tooltip::add);
        if (data.contains(ITEM_NAME_KEY) && data.contains(ITEM_COUNT_KEY)) {
            tooltip.add(Component.translatable("tooltip.changede.pipe_items",
                    data.getString(ITEM_NAME_KEY), data.getInt(ITEM_COUNT_KEY)));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.parse("changede:lp_energy");
    }
}
