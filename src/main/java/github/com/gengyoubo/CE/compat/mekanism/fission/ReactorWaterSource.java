package github.com.gengyoubo.CE.compat.mekanism.fission;

import mekanism.generators.common.content.fission.FissionReactorMultiblockData;

/** Keeps optional AE2 types out of the reactor's common upgrade state. */
public interface ReactorWaterSource {
    void supplyWater(FissionReactorMultiblockData reactor);
    void assignOwner(net.minecraft.world.entity.player.Player player);
}
