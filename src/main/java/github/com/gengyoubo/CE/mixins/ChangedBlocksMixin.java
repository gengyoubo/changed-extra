package github.com.gengyoubo.CE.mixins;

import net.ltxprogrammer.changed.init.ChangedBlocks;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(value = ChangedBlocks.class, remap = false)
public class ChangedBlocksMixin {
    @Unique
    private static final Map<RegistryObject<?>, Object> CHANGEDE$REGISTRY_GET_CACHE = new IdentityHashMap<>();

    /**
     * ChangedBlocks.<clinit> may call RegistryObject#get many times while building the huge registry graph.
     * Cache lookups inside this one-time static init to reduce duplicate resolution cost.
     */
    @Redirect(
            method = {
                    "lambda$static$15",
                    "lambda$static$18",
                    "lambda$static$20",
                    "lambda$static$21",
                    "lambda$static$22",
                    "lambda$static$23",
                    "lambda$static$24",
                    "lambda$static$25",
                    "lambda$static$27",
                    "lambda$static$30",
                    "lambda$static$32",
                    "lambda$static$33",
                    "lambda$static$39",
                    "lambda$static$40",
                    "lambda$static$43",
                    "lambda$static$44",
                    "lambda$static$45",
                    "lambda$static$46",
                    "lambda$static$47",
                    "lambda$static$48",
                    "lambda$static$49",
                    "lambda$static$50",
                    "lambda$static$51",
                    "lambda$static$52",
                    "lambda$static$53",
                    "lambda$static$54",
                    "lambda$static$55",
                    "lambda$static$56",
                    "lambda$static$57",
                    "lambda$static$58",
                    "lambda$static$60",
                    "lambda$static$61",
                    "lambda$static$62",
                    "lambda$static$68",
                    "lambda$static$69",
                    "lambda$static$70",
                    "lambda$static$71",
                    "lambda$static$72",
                    "lambda$static$73",
                    "lambda$static$74",
                    "lambda$static$75",
                    "lambda$static$76",
                    "lambda$static$77",
                    "lambda$static$78",
                    "lambda$static$79",
                    "lambda$static$80",
                    "lambda$static$81",
                    "lambda$static$82",
                    "lambda$static$83",
                    "lambda$static$84",
                    "lambda$static$85",
                    "lambda$static$87",
                    "lambda$static$88",
                    "lambda$static$89",
                    "lambda$static$90",
                    "lambda$static$91",
                    "lambda$static$124"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/registries/RegistryObject;get()Ljava/lang/Object;", remap = false),
            remap = false,
            require = 0
    )
    private static Object changede$cacheRegistryObjectGet(RegistryObject<?> registryObject) {
        Object cached = CHANGEDE$REGISTRY_GET_CACHE.get(registryObject);
        if (cached != null) return cached;

        Object value = registryObject.get();
        CHANGEDE$REGISTRY_GET_CACHE.put(registryObject, value);
        return value;
    }
}
