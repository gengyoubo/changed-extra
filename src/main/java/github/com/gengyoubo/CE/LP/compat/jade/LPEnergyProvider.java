package github.com.gengyoubo.CE.LP.compat.jade;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum LPEnergyProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String STORED_KEY = "StoredLP";
    private static final String CAPACITY_KEY = "CapacityLP";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof ILatexEnergyHandler energy) {
            data.putInt(STORED_KEY, energy.getEnergyStored());
            data.putInt(CAPACITY_KEY, energy.getMaxEnergyStored());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(STORED_KEY) && data.contains(CAPACITY_KEY)) {
            tooltip.add(Component.literal("LP: " + data.getInt(STORED_KEY) + " / " + data.getInt(CAPACITY_KEY)));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.parse("changede:lp_energy");
    }
}
