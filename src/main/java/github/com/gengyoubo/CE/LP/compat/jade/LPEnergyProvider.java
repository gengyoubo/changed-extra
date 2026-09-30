package github.com.gengyoubo.CE.LP.compat.jade;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.I.ItemPipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.F.FluidPipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicPumpBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.BasicLatexFluidGeneratorBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.LatexEnergyConverterBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.OrangeProducerBlockEntity;
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
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

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
    private static final String CONVERTER_OUTPUT_KEY = "ConverterOutput";
    private static final String CONVERTER_TYPE_KEY = "ConverterType";
    private static final String CONVERTER_FLUID_KEY = "ConverterFluid";
    private static final String CONVERTER_FLUID_ID_KEY = "ConverterFluidId";
    private static final String ORANGE_COUNT_KEY = "OrangeProducerCount";

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
        if (blockEntity instanceof LatexEnergyConverterBlockEntity converter) {
            data.putInt(CONVERTER_OUTPUT_KEY, converter.getOutputStored());
            data.putString(CONVERTER_TYPE_KEY, converter.getOutputType().name());
            data.putInt(CONVERTER_FLUID_KEY, converter.getFluidStored());
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(converter.getRequiredFluid());
            if (fluidId != null) data.putString(CONVERTER_FLUID_ID_KEY, fluidId.toString());
        }
        if (blockEntity instanceof OrangeProducerBlockEntity producer) {
            data.putInt(ORANGE_COUNT_KEY, producer.getOrangeCount());
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
        if (data.contains(CONVERTER_OUTPUT_KEY)) {
            boolean white = "WLP".equals(data.getString(CONVERTER_TYPE_KEY));
            String energyKey = white ? "energy.changede.wlp" : "energy.changede.dlp";
            int stored = data.getInt(CONVERTER_OUTPUT_KEY);
            int capacity = LatexEnergyConverterBlockEntity.OUTPUT_CAPACITY;
            float ratio = capacity <= 0 ? 0 : (float) stored / capacity;
            IElementHelper elements = tooltip.getElementHelper();
            int fillColor = white ? 0xFFFFFFFF : 0xFF202020;
            int backgroundColor = white ? 0xFF666666 : 0xFFAAAAAA;
            tooltip.add(elements.progress(ratio,
                    Component.translatable("tooltip.changede.converter_energy", Component.translatable(energyKey), stored, capacity),
                    elements.progressStyle().color(fillColor, backgroundColor).textColor(0xFFFFFFFF),
                    BoxStyle.DEFAULT, true));
            tooltip.add(Component.translatable("tooltip.changede.converter_fluid", fluidName(data.getString(CONVERTER_FLUID_ID_KEY)), data.getInt(CONVERTER_FLUID_KEY), LatexEnergyConverterBlockEntity.TANK_CAPACITY));
        }
        if (data.contains(ORANGE_COUNT_KEY)) {
            tooltip.add(Component.translatable("tooltip.changede.orange_producer_output", data.getInt(ORANGE_COUNT_KEY)));
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
