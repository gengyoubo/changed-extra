package github.com.gengyoubo.CE.LP.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class CEPonderPlugin implements PonderPlugin {
    @Override
    public String getModId() {
        return "changede";
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        CESpaceTowerPonderScenes.register(helper);
    }
}
