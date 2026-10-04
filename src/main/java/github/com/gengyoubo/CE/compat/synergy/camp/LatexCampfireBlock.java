package github.com.gengyoubo.CE.compat.synergy.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

@SuppressWarnings("deprecation")
public final class LatexCampfireBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public LatexCampfireBlock() {
        super(Properties.copy(Blocks.CAMPFIRE).lightLevel(state -> state.getValue(LIT) ? 13 : 0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, LIT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(0, 0, 0, 16, 7, 16); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LatexCampfireBlockEntity(pos, state); }
    @Override public java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        // No unconditional loot JSON referencing an item absent without the optional dependency.
        var params = builder.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
        Float radius = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.EXPLOSION_RADIUS);
        if (radius != null && params.getLevel().random.nextFloat() > 1 / radius) return java.util.List.of();
        return java.util.List.of(new ItemStack(LatexCampfireCompat.CAMPFIRE_ITEM.get()));
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level instanceof ServerLevel server && placer instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof LatexCampfireBlockEntity core) {
            // Placement always binds from server records, never trusts copied BlockEntityTag IDs.
            core.settlementId = null;
            LatexSettlementService.claim(server, core, player);
        }
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer owner
                && level.getBlockEntity(pos) instanceof LatexCampfireBlockEntity core) {
            if (LatexSettlementService.resolve(server, core) == null) LatexSettlementService.claim(server, core, owner);
            LatexSettlementService.open(owner, core);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof LatexCampfireBlockEntity core)
            LatexSettlementService.deactivate(server, core);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!state.getValue(LIT)) return;
        level.addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 0, 0.05, 0);
        if (random.nextInt(10) == 0) level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                net.minecraft.sounds.SoundEvents.CAMPFIRE_CRACKLE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1, false);
    }
}
