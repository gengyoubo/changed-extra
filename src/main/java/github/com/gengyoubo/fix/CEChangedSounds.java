package github.com.gengyoubo.fix;

import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.init.ChangedSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Function;

public class CEChangedSounds extends ChangedSounds {
    public static final RegistryObject<SoundEvent> POISON;
    static {
        POISON=register("poison");
    }
    private static RegistryObject<SoundEvent> register(String id) {
        return REGISTRY.register(id, () -> SoundEvent.createVariableRangeEvent(Changed.modResource(id)));
    }
    private static RegistryObject<SoundEvent> register(String id, Function<ResourceLocation, SoundEvent> finalizer) {
        return REGISTRY.register(id, () -> (SoundEvent)finalizer.apply(Changed.modResource(id)));
    }
}
