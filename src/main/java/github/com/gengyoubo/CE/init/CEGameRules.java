package github.com.gengyoubo.CE.init;

import net.minecraft.world.level.GameRules;

public final class CEGameRules {
    private CEGameRules() {
    }

    public static GameRules.Key<GameRules.BooleanValue> LATEX_START;

    public static void register() {
        LATEX_START = GameRules.register(
                "latex_start",
                GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(false)
        );
    }
}
