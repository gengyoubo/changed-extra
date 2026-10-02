package github.com.gengyoubo.CE.LP.compat.jade;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.ILatexTypedEnergyHandler;
import github.com.gengyoubo.CE.util.AmountFormat;
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

//Jade 能从 Capability 得到的信息，就不要在 CEJadePlugin 里再造一遍。
public enum LPEnergyProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String STORED_KEY = "StoredLP";
    private static final String CAPACITY_KEY = "CapacityLP";

    private static final String ITEM_NAME_KEY = "PipeItemName";
    private static final String ITEM_COUNT_KEY = "PipeItemCount";

    private static final String TYPED_STORED_KEY = "StoredTypedEnergy";
    private static final String TYPED_CAPACITY_KEY = "CapacityTypedEnergy";
    private static final String TYPED_TYPE_KEY = "TypedEnergyType";

    private static final String ORANGE_COUNT_KEY = "OrangeProducerCount";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();

        // LP
        if (blockEntity instanceof ILatexEnergyHandler energy) {
            data.putInt(STORED_KEY, energy.getEnergyStored());
            data.putInt(CAPACITY_KEY, energy.getMaxEnergyStored());
        }

        // 物品管道网络内容
        // Jade 本身无法知道“整个 CE 管网”的物品，因此保留
        if (blockEntity instanceof ItemPipeBlockEntity itemPipe) {
            var contents = itemPipe.getNetworkContents();
            if (!contents.isEmpty()) {
                data.putString(ITEM_NAME_KEY, contents.getHoverName().getString());
                data.putInt(ITEM_COUNT_KEY, contents.getCount());
            }
        }

        // DLP / WLP
        // 这是 CE 自定义能源，Jade 不认识，因此保留
        if (blockEntity instanceof ILatexTypedEnergyHandler energy) {
            data.putInt(TYPED_STORED_KEY, energy.getTypedEnergyStored());
            data.putInt(TYPED_CAPACITY_KEY, energy.getTypedEnergyCapacity());
            data.putString(TYPED_TYPE_KEY, energy.getEnergyType().name());
        }
    }

    @Override
    public void appendTooltip(
            ITooltip tooltip,
            BlockAccessor accessor,
            IPluginConfig config
    ) {
        CompoundTag data = accessor.getServerData();

        // LP
        if (data.contains(STORED_KEY) && data.contains(CAPACITY_KEY)) {
            tooltip.add(Component.literal(
                    "LP: " + AmountFormat.format(data.getInt(STORED_KEY), data.getInt(CAPACITY_KEY))
            ));
        }

        // 物品管道
        if (data.contains(ITEM_NAME_KEY) && data.contains(ITEM_COUNT_KEY)) {
            tooltip.add(Component.translatable(
                    "tooltip.changede.pipe_items",
                    data.getString(ITEM_NAME_KEY),
                    data.getInt(ITEM_COUNT_KEY)
            ));
        }

        // DLP / WLP
        if (data.contains(TYPED_STORED_KEY) && data.contains(TYPED_CAPACITY_KEY)
                && data.contains(TYPED_TYPE_KEY)) {

            boolean white = "WLP".equals(
                    data.getString(TYPED_TYPE_KEY)
            );

            String energyKey = white
                    ? "energy.changede.wlp"
                    : "energy.changede.dlp";

            int stored = data.getInt(TYPED_STORED_KEY);
            int capacity = data.getInt(TYPED_CAPACITY_KEY);

            float ratio = capacity <= 0
                    ? 0
                    : Math.max(0,Math.min(1,(float) stored / capacity));

            IElementHelper elements = tooltip.getElementHelper();

            int fillColor = white
                    ? 0xFFFFFFFF
                    : 0xFF202020;

            int backgroundColor = white
                    ? 0xFF666666
                    : 0xFFAAAAAA;

            tooltip.add(elements.progress(
                    ratio,
                    Component.translatable(
                            "tooltip.changede.converter_energy",
                            Component.translatable(energyKey),
                            AmountFormat.format(stored, capacity)
                    ),
                    elements.progressStyle()
                            .color(fillColor, backgroundColor)
                            .textColor(0xFFFFFFFF),
                    BoxStyle.DEFAULT,
                    true
            ));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.parse("changede:lp_energy");
    }
}
