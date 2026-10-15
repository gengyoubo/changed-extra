package github.com.gengyoubo.CE.compat.mekanism.fission;

import mekanism.api.providers.IBlockProvider;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Factory signature contains no AE2 type, keeping verification of the common registry safe without AE2. */
final class FissionAe2Factory {
    static TileEntityFissionReactorCasing create(IBlockProvider provider, BlockPos pos, BlockState state) {
        return new FissionMeInterface(provider, pos, state);
    }
    private FissionAe2Factory() {}
}
