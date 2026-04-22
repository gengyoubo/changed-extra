package github.com.gengyoubo.fix.SpecialLatex;

import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.init.ChangedSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

public class CEChangedSounds extends ChangedSounds {
    public static final RegistryObject<SoundEvent> POISON;
    static {
        POISON=register("poison");
    }
    private static RegistryObject<SoundEvent> register(String id) {
        return REGISTRY.register(id, () -> SoundEvent.createVariableRangeEvent(Changed.modResource(id)));
    }
}
