package github.com.gengyoubo.CE.Block;

import github.com.gengyoubo.CE.skill.LatexSkillResearchMenu;
import github.com.gengyoubo.CE.BlockEntity.LatexSkillResearchBlockEntity;
import github.com.gengyoubo.CE.init.CEBlockEntity;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;

public final class LatexSkillResearchTableBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape SHAPE=Shapes.or(box(0,10,0,16,14,16),box(1,0,1,4,10,4),
            box(12,0,1,15,10,4),box(1,0,12,4,10,15),box(12,0,12,15,10,15),box(4,14,4,12,16,12));
    public LatexSkillResearchTableBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE).strength(3.5F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new LatexSkillResearchBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return !level.isClientSide && type==CEBlockEntity.LATEX_SKILL_RESEARCH.get()
                ? (world,pos,block,entity)->((LatexSkillResearchBlockEntity)entity).tick() : null;
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState state,Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state,Mirror mirror) { return rotate(state,mirror.getRotation(state.getValue(FACING))); }
    @Override public VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,CollisionContext context) { return SHAPE; }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,new SimpleMenuProvider((id,inventory,p)->
                    new LatexSkillResearchMenu(id,inventory,pos),Component.translatable("block.changede.latex_skill_research_table")),pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
