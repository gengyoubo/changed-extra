package github.com.gengyoubo.CE.LP.compat.jade;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.I.ItemPipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.F.FluidPipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicPumpBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.BasicLatexFluidGeneratorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum LPEnergyProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String STORED_KEY = "StoredLP";
    private static final String CAPACITY_KEY = "CapacityLP";
    private static final String ITEM_NAME_KEY = "PipeItemName";
    private static final String ITEM_COUNT_KEY = "PipeItemCount";
    private static final String FLUID_NAME_KEY = "PipeFluidName";
    private static final String FLUID_AMOUNT_KEY = "PipeFluidAmount";
    private static final String PUMP_FLUID_NAME_KEY = "PumpFluidName";
    private static final String PUMP_FLUID_AMOUNT_KEY = "PumpFluidAmount";
    private static final String GENERATOR_FLUID_NAME_KEY = "GeneratorFluidName";
    private static final String GENERATOR_FLUID_AMOUNT_KEY = "GeneratorFluidAmount";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity instanceof ILatexEnergyHandler energy) {
            data.putInt(STORED_KEY, energy.getEnergyStored());
            data.putInt(CAPACITY_KEY, energy.getMaxEnergyStored());
        }
        if (blockEntity instanceof ItemPipeBlockEntity itemPipe) {
            var contents = itemPipe.getNetworkContents();
            if (!contents.isEmpty()) {
                data.putString(ITEM_NAME_KEY, contents.getHoverName().getString());
                data.putInt(ITEM_COUNT_KEY, contents.getCount());
            }
        }
        if (blockEntity instanceof FluidPipeBlockEntity fluidPipe) {
            var contents = fluidPipe.getNetworkContents();
            if (!contents.isEmpty()) {
                ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(contents.getFluid());
                if (fluidId != null) data.putString(FLUID_NAME_KEY, fluidId.toString());
                data.putInt(FLUID_AMOUNT_KEY, contents.getAmount());
            }
        }
        if (blockEntity instanceof BasicPumpBlockEntity pump) {
            var contents = pump.getStoredFluid();
            if (!contents.isEmpty()) {
                ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(contents.getFluid());
                if (fluidId != null) data.putString(PUMP_FLUID_NAME_KEY, fluidId.toString());
                data.putInt(PUMP_FLUID_AMOUNT_KEY, contents.getAmount());
            }
        }
        if (blockEntity instanceof BasicLatexFluidGeneratorBlockEntity generator) {
            var contents = generator.getStoredFluid();
            if (!contents.isEmpty()) {
                ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(contents.getFluid());
                if (fluidId != null) data.putString(GENERATOR_FLUID_NAME_KEY, fluidId.toString());
                data.putInt(GENERATOR_FLUID_AMOUNT_KEY, contents.getAmount());
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(STORED_KEY) && data.contains(CAPACITY_KEY)) {
            tooltip.add(Component.literal("LP: " + data.getInt(STORED_KEY) + " / " + data.getInt(CAPACITY_KEY)));
        }
        if (data.contains(ITEM_NAME_KEY) && data.contains(ITEM_COUNT_KEY)) {
            tooltip.add(Component.translatable("tooltip.changede.pipe_items", data.getString(ITEM_NAME_KEY), data.getInt(ITEM_COUNT_KEY)));
        }
        if (data.contains(FLUID_NAME_KEY) && data.contains(FLUID_AMOUNT_KEY)) {
            tooltip.add(Component.translatable("tooltip.changede.pipe_fluid", fluidName(data.getString(FLUID_NAME_KEY)), data.getInt(FLUID_AMOUNT_KEY)));
        }
        if (data.contains(PUMP_FLUID_NAME_KEY) && data.contains(PUMP_FLUID_AMOUNT_KEY)) {
            tooltip.add(Component.translatable("tooltip.changede.pump_fluid", fluidName(data.getString(PUMP_FLUID_NAME_KEY)), data.getInt(PUMP_FLUID_AMOUNT_KEY)));
        }
        if (data.contains(GENERATOR_FLUID_NAME_KEY) && data.contains(GENERATOR_FLUID_AMOUNT_KEY)) {
            tooltip.add(Component.translatable("tooltip.changede.generator_fluid", fluidName(data.getString(GENERATOR_FLUID_NAME_KEY)), data.getInt(GENERATOR_FLUID_AMOUNT_KEY)));
        }
    }

    private static Component fluidName(String fluidId) {
        ResourceLocation id = ResourceLocation.tryParse(fluidId);
        if (id == null) return Component.literal(fluidId);
        var fluid = ForgeRegistries.FLUIDS.getValue(id);
        if (fluid == null) return Component.literal(fluidId);
        return new FluidStack(fluid, 1).getDisplayName();
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.parse("changede:lp_energy");
    }
}
