package github.com.gengyoubo.CE.compat.twilight;

import net.foxyas.changedaddon.entity.bosses.Experiment009BossEntity;

public interface Twilight009StateAccess {
    int changed_extra$stage();
    void changed_extra$setStage(int stage);
    boolean changed_extra$locked();
    void changed_extra$setLocked(boolean locked);
    void changed_extra$phaseChanged(Experiment009BossEntity.Exp9Phase phase);
}
