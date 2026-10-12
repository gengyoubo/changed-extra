package github.com.gengyoubo.CE.compat.twilight;

import net.foxyas.changedaddon.entity.bosses.Experiment009BossEntity;

public interface Twilight009StateAccess {
    int changede$stage();
    void changede$setStage(int stage);
    boolean changede$locked();
    void changede$setLocked(boolean locked);
    void changede$phaseChanged(Experiment009BossEntity.Exp9Phase phase);
}
