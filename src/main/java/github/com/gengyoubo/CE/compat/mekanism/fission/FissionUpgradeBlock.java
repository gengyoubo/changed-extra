package github.com.gengyoubo.CE.compat.mekanism.fission;

import java.util.List;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.generators.common.registries.GeneratorsBlockTypes;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;

/** Native casing block type: participates in formation, GUI, heat, cache and dismantling. */
public class FissionUpgradeBlock extends BlockTile<TileEntityFissionReactorCasing, BlockTypeTile<TileEntityFissionReactorCasing>> {
    public final FissionUpgrade upgrade;

    public FissionUpgradeBlock(FissionUpgrade upgrade) {
        super(GeneratorsBlockTypes.FISSION_REACTOR_CASING, properties -> properties);
        this.upgrade = upgrade;
    }

    @Override
    public TileEntityTypeRegistryObject<TileEntityFissionReactorCasing> getTileType() {
        return FissionCompat.TILES.get(upgrade);
    }

    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                            net.minecraft.world.level.block.state.BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof net.minecraft.world.entity.player.Player player
                && level.getBlockEntity(pos) instanceof ReactorWaterSource source) source.assignOwner(player);
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable("tooltip.changede.fission_casing_upgrade"));
        lines.add(Component.translatable("tooltip.changede." + upgrade.id));
    }
}
