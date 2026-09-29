package github.com.gengyoubo.CE.LP.init;

import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.BasicGeneratorBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.BasicCrystalGeneratorBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.GeneratorBlockEntity.BasicLatexFluidGeneratorBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.ElectricFurnaceBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.BasicPumpBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.InfuserPowerBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.LatexEnergyConverterBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.LatexCreativeExtranalbodyCraftTableBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.E.BasicEnergyPipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.I.BasicItemPipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.F.BasicFluidPipeBlockEntity;
import github.com.gengyoubo.CE.LP.compat.SpaceTowerCompat;
import net.ltxprogrammer.changed.init.ChangedBlocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CELPBlockEntity {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "changede");

    // Keep the old names as aliases so existing registration code keeps compiling.
    public static final DeferredRegister<BlockEntityType<?>> WIRE_BLOCK_ENTITIES = BLOCK_ENTITIES;
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BasicEnergyPipeBlockEntity>> BASIC_WIRE_BLOCK_ENTITIES =
            BLOCK_ENTITIES.register("wire",
                    () -> BlockEntityType.Builder.of(
                            BasicEnergyPipeBlockEntity::new,
                            CELPBlock.BASIC_WIRE.get()
                    ).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BasicGeneratorBlockEntity>> BASIC_GENERATOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("basic_generator",
                    () -> BlockEntityType.Builder.of(
                            BasicGeneratorBlockEntity::new,
                            CELPBlock.BASIC_GENERATOR.get()
                    ).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BLOCK_ENTITY=
            BLOCK_ENTITIES.register("electric_furnace",
                    () ->BlockEntityType.Builder.of(
                            ElectricFurnaceBlockEntity::new,
                            CELPBlock.ELECTRIC_FURNACE.get()
                    ).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<LatexCreativeExtranalbodyCraftTableBlockEntity>> LATEX_CREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("latexcreative_extranalbody_craft_table",
                    () -> BlockEntityType.Builder.of(
                            LatexCreativeExtranalbodyCraftTableBlockEntity::new,
                            CELPBlock.LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK.get()
                    ).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BasicLatexFluidGeneratorBlockEntity>> BASIC_LATEX_FLUID_GENERATOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("basic_latex_fluid_generator",
                    () -> BlockEntityType.Builder.of(BasicLatexFluidGeneratorBlockEntity::new, CELPBlock.BASIC_LATEX_FLUID_GENERATOR.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BasicItemPipeBlockEntity>> BASIC_ITEM_PIPE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("basic_item_pipe",
                    () -> BlockEntityType.Builder.of(BasicItemPipeBlockEntity::new, CELPBlock.BASIC_ITEM_PIPE.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BasicFluidPipeBlockEntity>> BASIC_FLUID_PIPE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("basic_fluid_pipe",
                    () -> BlockEntityType.Builder.of(BasicFluidPipeBlockEntity::new, CELPBlock.BASIC_FLUID_PIPE.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BasicPumpBlockEntity>> BASIC_PUMP_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("basic_pump",
                    () -> BlockEntityType.Builder.of(BasicPumpBlockEntity::new, CELPBlock.BASIC_PUMP.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BasicCrystalGeneratorBlockEntity>> BASIC_CRYSTAL_GENERATOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("basic_crystal_generator",
                    () -> BlockEntityType.Builder.of(
                            BasicCrystalGeneratorBlockEntity::new,
                            CELPBlock.BASIC_CRYSTAL_GENERATOR.get()
                    ).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BlockEntity>> SPACE_TOWER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("space_tower",
                    () -> BlockEntityType.Builder.of(
                            SpaceTowerCompat::createBlockEntity,
                            CELPBlock.SPACE_TOWER.get()
                    ).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<LatexEnergyConverterBlockEntity>> WHITE_LATEX_POWER_CONVERTER =
            BLOCK_ENTITIES.register("white_latex_power_converter", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new LatexEnergyConverterBlockEntity(pos, state, true), CELPBlock.WHITE_LATEX_POWER_CONVERTER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<LatexEnergyConverterBlockEntity>> DARK_LATEX_POWER_CONVERTER =
            BLOCK_ENTITIES.register("dark_latex_power_converter", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new LatexEnergyConverterBlockEntity(pos, state, false), CELPBlock.DARK_LATEX_POWER_CONVERTER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<InfuserPowerBlockEntity>> INFUSER_POWER =
            BLOCK_ENTITIES.register("infuser_power",
                    () -> BlockEntityType.Builder.of(
                            InfuserPowerBlockEntity::new,
                            ChangedBlocks.INFUSER.get()
                    ).build(null));
}
