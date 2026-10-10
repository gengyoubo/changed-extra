package github.com.gengyoubo.CE.compat.create.burner;

import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class LatexBurnerBlock extends BaseEntityBlock implements IWrenchable, SpecialBlockItemRequirement {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OCCUPIED = BooleanProperty.create("occupied");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);
    public LatexBurnerBlock() {
        super(Properties.copy(Blocks.IRON_BLOCK).strength(3.5f).noOcclusion().lightLevel(state -> switch (state.getValue(BlazeBurnerBlock.HEAT_LEVEL)) {
            case NONE -> 0; case SMOULDERING -> 8; default -> 15;
        }));
        registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(OCCUPIED, false).setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.NONE));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, OCCUPIED, BlazeBurnerBlock.HEAT_LEVEL); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LatexBurnerBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == LatexBurnerCompat.ENTITY.get() ? (world, pos, blockState, entity) -> ((LatexBurnerBlockEntity) entity).tick() : null;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean filled = context.getItemInHand().is(LatexBurnerCompat.FILLED.get());
        var data = context.getItemInHand().getTagElement("BlockEntityTag");
        if (filled && (data == null || LatexBurnerProfiles.find(data.getCompound("Creature").getString("id"), data.getString("RuntimeLatex")) == null)) return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(OCCUPIED, filled)
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, filled ? HeatLevel.SMOULDERING : HeatLevel.NONE);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.mayBuild() || !level.mayInteract(player, pos) || !(level.getBlockEntity(pos) instanceof LatexBurnerBlockEntity burner)
                || !burner.hasCreature()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(LatexBurnerBlockEntity.ITEM_FUELS)) {
            if (burner.items().insertItem(0, held.copyWithCount(1), true).isEmpty()) {
                if (!level.isClientSide) {
                    burner.items().insertItem(0, held.copyWithCount(1), false);
                    if (!player.isCreative()) held.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.FAIL;
        }
        FluidStack contained = FluidUtil.getFluidContained(held).orElse(FluidStack.EMPTY);
        if (!contained.isEmpty()) {
            // Buckets feed only the liquid lane, and only when the entire bucket can fit.
            if (!(held.getItem() instanceof BucketItem) || contained.getAmount() != 1000
                    || burner.fluids().fill(contained, IFluidHandler.FluidAction.SIMULATE) != 1000) return InteractionResult.FAIL;
            if (!level.isClientSide) {
                burner.fluids().fill(contained, IFluidHandler.FluidAction.EXECUTE);
                if (!player.isCreative()) {
                    held.shrink(1);
                    ItemStack bucket = new ItemStack(Items.BUCKET);
                    if (held.isEmpty()) player.setItemInHand(hand, bucket);
                    else player.getInventory().placeItemBackInInventory(bucket);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public ItemStack getCloneItemStack(BlockState state, HitResult hit, BlockGetter level, BlockPos pos, Player player) {
        return level.getBlockEntity(pos) instanceof LatexBurnerBlockEntity burner ? burner.carriedStack() : new ItemStack(LatexBurnerCompat.EMPTY.get());
    }
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity entity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        // A dynamic entry has no optional registry references when the loot table loads without Create.
        builder.withDynamicDrop(ResourceLocation.parse("changede:latex_burner"), output -> {
            if (entity instanceof LatexBurnerBlockEntity burner) output.accept(burner.carriedStack());
            else if (!state.getValue(OCCUPIED)) output.accept(new ItemStack(LatexBurnerCompat.EMPTY.get()));
        });
        return super.getDrops(state, builder);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!level.isClientSide && level.getBlockEntity(pos.above()) instanceof BasinBlockEntity basin) basin.notifyChangeOfContents();
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return switch (state.getValue(BlazeBurnerBlock.HEAT_LEVEL)) {
            case NONE -> 0; case SMOULDERING -> 1; case SEETHING -> 3; default -> 2;
        };
    }
    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            context.getLevel().setBlock(context.getClickedPos(), state.cycle(FACING), 3);
            IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
        }
        return InteractionResult.SUCCESS;
    }
    @Override public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity entity) {
        // Occupied schematics require an explicit future NBT-aware placement path; never clone a captive.
        if (state.getValue(OCCUPIED)) return ItemRequirement.INVALID;
        return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, LatexBurnerCompat.EMPTY.get());
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        HeatLevel heat = state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
        if (!heat.isAtLeast(HeatLevel.KINDLED) || random.nextInt(4) != 0) return;
        level.addParticle(heat == HeatLevel.SEETHING ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                pos.getX() + .3 + random.nextDouble() * .4, pos.getY() + .65, pos.getZ() + .3 + random.nextDouble() * .4,
                0, .015, 0);
    }
}
