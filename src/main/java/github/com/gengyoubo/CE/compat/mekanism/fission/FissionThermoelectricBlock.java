package github.com.gengyoubo.CE.compat.mekanism.fission;

import java.util.List;
import mekanism.common.util.ItemDataUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public final class FissionThermoelectricBlock extends FissionUpgradeBlock {
    public static final BooleanProperty FUSED = BooleanProperty.create("fused");
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public FissionThermoelectricBlock() {
        super(FissionUpgrade.THERMOELECTRIC);
        registerDefaultState(stateDefinition.any().setValue(FUSED, false).setValue(LIT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(FUSED, LIT);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean repairing = player.getItemInHand(hand).is(FissionCompat.HEAT_EXCHANGE_CORE.get());
        boolean inspect = player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty();
        if (!repairing && !inspect) return super.use(state, level, pos, player, hand, hit);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FissionThermoelectricBlockEntity tile) {
            if (repairing) { if (tile.repair(player) && !player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1); }
            else tile.describe(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        if (ItemDataUtils.getCompound(stack, FissionThermoelectricBlockEntity.DATA).getBoolean("Fused"))
            lines.add(Component.translatable("state.changede.thermo.fused").withStyle(net.minecraft.ChatFormatting.RED));
        lines.add(Component.translatable("tooltip.changede.thermo.inspect"));
    }
}
