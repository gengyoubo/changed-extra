package github.com.gengyoubo.CE.LP.ponder;

import github.com.gengyoubo.CE.LP.Block.BasicEnergyPipeBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.SpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

public class CESpaceTowerPonderScenes {
    private static final ResourceLocation KINETIC_SOURCES = ResourceLocation.parse("create:kinetic_sources");

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(CELPBlock.SPACE_TOWER.getId())
                .addStoryBoard("space_tower/spacetower", CESpaceTowerPonderScenes::intro, KINETIC_SOURCES);
    }

    private static void intro(SceneBuilder scene, SceneBuildingUtil util) {
        CreateSceneBuilder createScene = new CreateSceneBuilder(scene);

        createScene.title("space_tower", "Space Tower");
        createScene.configureBasePlate(0, 0, 7);
        createScene.scaleSceneView(1f);
        createScene.showBasePlate();

        BlockPos tower = util.grid().at(3, 1, 3);
        createScene.world().setBlock(tower, CELPBlock.SPACE_TOWER.get().defaultBlockState(), false);
        createScene.world().showSection(util.select().position(tower), Direction.DOWN);
        createScene.idle(10);

        createScene.overlay().showText(100)
                .text(I18n.get("changede.ponder.space_tower.text_1"))
                .pointAt(util.vector().topOf(tower))
                .placeNearTarget();
        createScene.idle(110);

        createScene.overlay().showText(100)
                .text(I18n.get("changede.ponder.space_tower.text_2"))
                .pointAt(util.vector().topOf(tower))
                .placeNearTarget();
        createScene.idle(110);

        createScene.world().hideSection(util.select().position(tower), Direction.UP);
        createScene.idle(10);

        BlockPos wire = util.grid().at(0, 1, 3);
        createScene.world().setBlock(wire, createWireState(Direction.WEST, Direction.EAST), false);
        createScene.world().showSection(util.select().position(wire), Direction.DOWN);
        createScene.idle(5);

        tower = util.grid().at(1, 1, 3);
        createScene.world().setBlock(tower, CELPBlock.SPACE_TOWER.get().defaultBlockState(), false);
        createScene.world().showSection(util.select().position(tower), Direction.DOWN);

        for (int i = 2; i < 5; i++) {
            BlockPos shaft = util.grid().at(i, 1, 3);
            createScene.world().setBlock(shaft, createShaftState(Direction.Axis.X), false);
            createScene.world().showSection(util.select().position(shaft), Direction.DOWN);
            createScene.idle(5);
        }

        BlockPos motor = util.grid().at(5, 1, 3);
        createScene.world().setBlock(motor, createMotorState(Direction.WEST), false);
        createScene.world().showSection(util.select().position(motor), Direction.DOWN);
        createScene.world().setKineticSpeed(util.select().fromTo(
                util.grid().at(2, 1, 3),
                util.grid().at(5, 1, 3)
        ), 256.0f);

        createScene.overlay().showText(100)
                .text(I18n.get("changede.ponder.space_tower.text_3"))
                .pointAt(util.vector().topOf(tower))
                .placeNearTarget();
        createScene.idle(110);

        createScene.world().hideSection(util.select().fromTo(
                util.grid().at(0, 1, 3),
                util.grid().at(5, 1, 3)
        ), Direction.UP);
        createScene.idle(10);

        tower = util.grid().at(5, 1, 3);
        createScene.world().setBlock(tower, CELPBlock.SPACE_TOWER.get().defaultBlockState(), false);
        createScene.world().modifyBlockEntity(tower, SpaceTowerBlockEntity.class, spaceTower -> {
            spaceTower.setMode(SpaceTowerEnergyType.CE, IOType.OUTPUT);
            spaceTower.setCeSu(256);
            spaceTower.setCeRpm(256);
        });
        createScene.world().showSection(util.select().position(tower), Direction.DOWN);
        createScene.idle(5);

        wire = util.grid().at(6, 1, 3);
        createScene.world().setBlock(wire, createWireState(Direction.WEST, Direction.EAST), false);
        createScene.world().showSection(util.select().position(wire), Direction.DOWN);
        createScene.idle(5);

        for (int i = 4; i >= 3; i--) {
            BlockPos shaft = util.grid().at(i, 1, 3);
            createScene.world().setBlock(shaft, createShaftState(Direction.Axis.X), false);
            createScene.world().showSection(util.select().position(shaft), Direction.DOWN);
            createScene.idle(5);
        }

        BlockPos stressometer = util.grid().at(2, 1, 3);
        createScene.world().setBlock(stressometer, getCreateBlock("stressometer").defaultBlockState(), false);
        createScene.world().showSection(util.select().position(stressometer), Direction.DOWN);
        createScene.idle(5);

        BlockPos speedometer = util.grid().at(1, 1, 3);
        createScene.world().setBlock(speedometer, getCreateBlock("speedometer").defaultBlockState(), false);
        createScene.world().showSection(util.select().position(speedometer), Direction.DOWN);
        createScene.world().setKineticSpeed(util.select().fromTo(
                util.grid().at(1, 1, 3),
                util.grid().at(5, 1, 3)
        ), 256.0f);
        createScene.idle(10);

        createScene.overlay().showText(100)
                .text(I18n.get("changede.ponder.space_tower.text_4"))
                .pointAt(util.vector().topOf(tower))
                .placeNearTarget();
        createScene.idle(110);
    }

    private static Block getCreateBlock(String path) {
        BlockState air = Blocks.AIR.defaultBlockState();
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("create:" + path));
        return block == null ? air.getBlock() : block;
    }

    private static BlockState createShaftState(Direction.Axis axis) {
        return getCreateBlock("shaft").defaultBlockState().setValue(BlockStateProperties.AXIS, axis);
    }

    private static BlockState createMotorState(Direction facing) {
        return getCreateBlock("creative_motor").defaultBlockState().setValue(BlockStateProperties.FACING, facing);
    }

    private static BlockState createWireState(Direction... connections) {
        BlockState state = CELPBlock.BASIC_WIRE.get().defaultBlockState();
        for (Direction connection : connections) {
            state = switch (connection) {
                case NORTH -> state.setValue(BasicEnergyPipeBlock.NORTH, true);
                case SOUTH -> state.setValue(BasicEnergyPipeBlock.SOUTH, true);
                case EAST -> state.setValue(BasicEnergyPipeBlock.EAST, true);
                case WEST -> state.setValue(BasicEnergyPipeBlock.WEST, true);
                case UP -> state.setValue(BasicEnergyPipeBlock.UP, true);
                case DOWN -> state.setValue(BasicEnergyPipeBlock.DOWN, true);
            };
        }
        return state;
    }
}
