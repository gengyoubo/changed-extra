package github.com.gengyoubo.CE.LP.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CEPonderPlugin implements PonderPlugin {
    @Override
    public @NotNull String getModId() {
        return "changede";
    }

    @Override
    public void registerScenes(@NotNull PonderSceneRegistrationHelper<ResourceLocation> helper) {
        CESpaceTowerPonderScenes.register(helper);
    }
}
