package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import slimeknights.mantle.block.entity.MantleBlockEntity;

/** State shared by fuel rules, menu synchronization and the controller tooltip. */
public interface LatexFuelAccess {
    MantleBlockEntity changede$parent();
    boolean changede$coreReady();
    void changede$setCoreReady(boolean ready);
    boolean changede$latexBatch();
    void changede$setLatexBatch(boolean latex);
}
