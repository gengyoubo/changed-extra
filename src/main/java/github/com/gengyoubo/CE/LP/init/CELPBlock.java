package github.com.gengyoubo.CE.LP.init;

import github.com.gengyoubo.CE.LP.Block.BasicEnergyPipeBlock;
import github.com.gengyoubo.CE.LP.Block.TypedEnergyPipeBlock;
import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.Block.BasicItemPipeBlock;
import github.com.gengyoubo.CE.LP.Block.BasicFluidPipeBlock;
import github.com.gengyoubo.CE.LP.Block.BasicPumpBlock;
import github.com.gengyoubo.CE.LP.Block.BasicLatexFluidGeneratorBlock;
import github.com.gengyoubo.CE.LP.Block.BasicGeneratorBlock;
import github.com.gengyoubo.CE.LP.Block.LatexEnergyConverterBlock;
import github.com.gengyoubo.CE.LP.Block.OrangeProducerBlock;
import github.com.gengyoubo.CE.LP.Block.BasicCrystalGeneratorBlock;
import github.com.gengyoubo.CE.LP.Block.ElectricFurnaceBlock;
import github.com.gengyoubo.CE.LP.Block.BasicAlloyFurnaceBlock;
import github.com.gengyoubo.CE.LP.Block.BasicLatexPurifierBlock;
import github.com.gengyoubo.CE.LP.Block.LatexCreativeExtranalbodyCraftTableBlock;
import github.com.gengyoubo.CE.LP.compat.SpaceTowerCompat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CELPBlock {
    public static final DeferredRegister<Block> BLOCKS;
    public static final DeferredRegister<Block> WIRE_BLOCKS;
    public static final RegistryObject<Block> BASIC_WIRE;
    public static final RegistryObject<Block> WLP_PIPE;
    public static final RegistryObject<Block> DLP_PIPE;
    public static final RegistryObject<Block> BASIC_ITEM_PIPE;
    public static final RegistryObject<Block> BASIC_FLUID_PIPE;
    public static final RegistryObject<Block> BASIC_PUMP;
    public static final RegistryObject<Block> BASIC_LATEX_FLUID_GENERATOR;
    public static final RegistryObject<Block> BASIC_GENERATOR;
    public static final RegistryObject<Block> BASIC_CRYSTAL_GENERATOR;
    public static final RegistryObject<Block> ELECTRIC_FURNACE;
    public static final RegistryObject<Block> BASIC_ALLOY_FURNACE;
    public static final RegistryObject<Block> BASIC_LATEX_PURIFIER;
    public static final RegistryObject<Block> LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK;
    public static final RegistryObject<Block> SPACE_TOWER;
    public static final RegistryObject<Block> WHITE_LATEX_POWER_CONVERTER;
    public static final RegistryObject<Block> DARK_LATEX_POWER_CONVERTER;
    public static final RegistryObject<Block> ORANGE_PRODUCER;

    static {
        BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "changede");

        WIRE_BLOCKS = BLOCKS;
        BASIC_WIRE = BLOCKS.register("basic_wire",
                () -> new BasicEnergyPipeBlock(BlockBehaviour.Properties.of()));
        WLP_PIPE = BLOCKS.register("wlp_pipe",()->new TypedEnergyPipeBlock(BlockBehaviour.Properties.of().strength(0.5F).noOcclusion(),LatexEnergyType.WLP));
        DLP_PIPE = BLOCKS.register("dlp_pipe",()->new TypedEnergyPipeBlock(BlockBehaviour.Properties.of().strength(0.5F).noOcclusion(),LatexEnergyType.DLP));
        BASIC_ITEM_PIPE = BLOCKS.register("basic_item_pipe",
                () -> new BasicItemPipeBlock(BlockBehaviour.Properties.of()));
        BASIC_FLUID_PIPE = BLOCKS.register("basic_fluid_pipe",
                () -> new BasicFluidPipeBlock(BlockBehaviour.Properties.of()));
        BASIC_PUMP = BLOCKS.register("basic_pump",
                () -> new BasicPumpBlock(BlockBehaviour.Properties.of()));
        BASIC_LATEX_FLUID_GENERATOR = BLOCKS.register("basic_latex_fluid_generator",
                () -> new BasicLatexFluidGeneratorBlock(BlockBehaviour.Properties.of()));
        BASIC_GENERATOR = BLOCKS.register("basic_generator",
                () -> new BasicGeneratorBlock(BlockBehaviour.Properties.of()));
        BASIC_CRYSTAL_GENERATOR = BLOCKS.register("basic_crystal_generator",
                () -> new BasicCrystalGeneratorBlock(BlockBehaviour.Properties.of()));
        ELECTRIC_FURNACE = BLOCKS.register("electric_furnace",
                () -> new ElectricFurnaceBlock(BlockBehaviour.Properties.of()));
        BASIC_ALLOY_FURNACE = BLOCKS.register("basic_alloy_furnace",
                () -> new BasicAlloyFurnaceBlock(BlockBehaviour.Properties.of()));
        BASIC_LATEX_PURIFIER = BLOCKS.register("basic_latex_purifier",
                () -> new BasicLatexPurifierBlock(BlockBehaviour.Properties.of()));
        LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK = BLOCKS.register("latexcreative_extranalbody_craft_table_block",
                () -> new LatexCreativeExtranalbodyCraftTableBlock(BlockBehaviour.Properties.of()));
        SPACE_TOWER = BLOCKS.register("space_tower",
                () -> SpaceTowerCompat.createBlock(BlockBehaviour.Properties.of()));
        WHITE_LATEX_POWER_CONVERTER = BLOCKS.register("white_latex_power_converter",
                () -> new LatexEnergyConverterBlock(BlockBehaviour.Properties.of(), true));
        DARK_LATEX_POWER_CONVERTER = BLOCKS.register("dark_latex_power_converter",
                () -> new LatexEnergyConverterBlock(BlockBehaviour.Properties.of(), false));
        ORANGE_PRODUCER = BLOCKS.register("orange_producer",
                () -> new OrangeProducerBlock(BlockBehaviour.Properties.of()));
    }
}
