package github.com.gengyoubo.CE.init;

import github.com.gengyoubo.CE.Block.LatexPaintingPortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CEBlock {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "changede");
    public static final RegistryObject<Block> LATEX_SKILL_RESEARCH_TABLE = BLOCKS.register("latex_skill_research_table",
            github.com.gengyoubo.CE.Block.LatexSkillResearchTableBlock::new);
    public static final RegistryObject<Block> DARK_LATEX_MORPHIC_CRYSTAL_ORE=BLOCKS.register("dark_latex_morphic_crystal_ore",
            ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE).strength(3.0F,3.0F),UniformInt.of(3,7)));
    public static final RegistryObject<Block> WHITE_LATEX_MORPHIC_CRYSTAL_ORE=BLOCKS.register("white_latex_morphic_crystal_ore",
            ()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE).strength(3.0F,3.0F),UniformInt.of(3,7)));

    public static final RegistryObject<Block> DARK_LATEX_LOG = BLOCKS.register("dark_latex_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)));
    public static final RegistryObject<Block> DARK_LATEX_PLANKS = BLOCKS.register("dark_latex_planks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));
    public static final RegistryObject<Block> DARK_LATEX_LEAVES = BLOCKS.register("dark_latex_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)));
    public static final RegistryObject<Block> DARK_LATEX_STONE = BLOCKS.register("dark_latex_stone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)));
    public static final RegistryObject<Block> DARK_LATEX_COBBLESTONE = BLOCKS.register("dark_latex_cobblestone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)));

    public static final RegistryObject<Block> WHITE_LATEX_LOG = BLOCKS.register("white_latex_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.BIRCH_LOG)));
    public static final RegistryObject<Block> WHITE_LATEX_PLANKS = BLOCKS.register("white_latex_planks",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.BIRCH_PLANKS)));
    public static final RegistryObject<Block> WHITE_LATEX_LEAVES = BLOCKS.register("white_latex_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.BIRCH_LEAVES)));
    public static final RegistryObject<Block> WHITE_LATEX_STONE = BLOCKS.register("white_latex_stone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)));
    public static final RegistryObject<Block> WHITE_LATEX_COBBLESTONE = BLOCKS.register("white_latex_cobblestone",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COBBLESTONE)));
    public static final RegistryObject<Block> LUMINARA_GRASS_BLOCK = BLOCKS.register("luminara_grass_block",
            () -> new GrassBlock(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)));
    public static final RegistryObject<Block> LATEX_PAINTING_PORTAL = BLOCKS.register("latex_painting_portal",
            LatexPaintingPortalBlock::new);
}
