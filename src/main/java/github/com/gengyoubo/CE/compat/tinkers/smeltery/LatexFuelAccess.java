package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import slimeknights.mantle.block.entity.MantleBlockEntity;

/** State shared by fuel rules, menu synchronization and the controller tooltip. */
public interface LatexFuelAccess {
    MantleBlockEntity changed_extra$parent();
    boolean changed_extra$coreReady();
    void changed_extra$setCoreReady(boolean ready);
    boolean changed_extra$latexBatch();
    void changed_extra$setLatexBatch(boolean latex);
}
