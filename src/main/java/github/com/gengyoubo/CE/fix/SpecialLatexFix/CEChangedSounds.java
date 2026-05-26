package github.com.gengyoubo.CE.fix.SpecialLatexFix;

import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.init.ChangedSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

public class CEChangedSounds extends ChangedSounds {
    public static final RegistryObject<SoundEvent> POISON;
    static {
        POISON=register();
    }
    private static RegistryObject<SoundEvent> register() {
        return REGISTRY.register("poison", () -> SoundEvent.createVariableRangeEvent(Changed.modResource("poison")));
    }
}
