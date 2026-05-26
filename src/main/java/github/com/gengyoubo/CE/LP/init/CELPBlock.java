package github.com.gengyoubo.CE.LP.init;

import github.com.gengyoubo.CE.LP.Block.BasicEnergyPipeBlock;
import github.com.gengyoubo.CE.LP.Block.BasicGeneratorBlock;
import github.com.gengyoubo.CE.LP.Block.ElectricFurnaceBlock;
import github.com.gengyoubo.CE.LP.Block.LatexCreativeExtranalbodyCraftTableBlock;
import github.com.gengyoubo.CE.LP.Block.SpaceTowerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CELPBlock {
    public static final DeferredRegister<Block> BLOCKS;
    public static final DeferredRegister<Block> WIRE_BLOCKS;
    public static final RegistryObject<Block> BASIC_WIRE;
    public static final RegistryObject<Block> BASIC_GENERATOR;
    public static final RegistryObject<Block> ELECTRIC_FURNACE;
    public static final RegistryObject<Block> LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK;
    public static final RegistryObject<Block> SPACE_TOWER;

    static {
        BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "changede");

        WIRE_BLOCKS = BLOCKS;
        BASIC_WIRE = BLOCKS.register("basic_wire",
                () -> new BasicEnergyPipeBlock(BlockBehaviour.Properties.of()));
        BASIC_GENERATOR = BLOCKS.register("basic_generator",
                () -> new BasicGeneratorBlock(BlockBehaviour.Properties.of()));
        ELECTRIC_FURNACE = BLOCKS.register("electric_furnace",
                () -> new ElectricFurnaceBlock(BlockBehaviour.Properties.of()));
        LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK = BLOCKS.register("latexcreative_extranalbody_craft_table_block",
                () -> new LatexCreativeExtranalbodyCraftTableBlock(BlockBehaviour.Properties.of()));
        SPACE_TOWER = BLOCKS.register("space_tower",
                () -> new SpaceTowerBlock(BlockBehaviour.Properties.of()));
    }
}
