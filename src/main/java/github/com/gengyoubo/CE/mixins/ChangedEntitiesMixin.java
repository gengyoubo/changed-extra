package github.com.gengyoubo.CE.mixins;

import com.mojang.datafixers.util.Pair;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(value = ChangedEntities.class, remap = false)
public class ChangedEntitiesMixin {
    @Unique
    private static final int CHANGEDE$ENTITY_COLLECTION_CAPACITY = 192;

    @Shadow @Final @Mutable
    private static Map<ResourceLocation, Pair<Integer, Integer>> ENTITY_COLOR_MAP;

    @Shadow @Final @Mutable
    private static List<Pair<Supplier<EntityType<? extends ChangedEntity>>, Supplier<AttributeSupplier.Builder>>> ATTR_FUNC_REGISTRY;

    @Shadow @Final @Mutable
    private static List<ChangedEntities.VoidConsumer> INIT_FUNC_REGISTRY;

    @Shadow @Final @Mutable
    public static Map<RegistryObject<? extends EntityType<?>>, RegistryObject<ForgeSpawnEggItem>> SPAWN_EGGS;

    @Shadow @Final @Mutable
    public static Map<Supplier<? extends EntityType<?>>, Predicate<Level>> DIMENSION_RESTRICTIONS;

    /**
     * ChangedEntities registers a large amount of entities during class init.
     * Pre-sizing these collections avoids repeated internal resize/rehash cost.
     */
    @Inject(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/ltxprogrammer/changed/init/ChangedEntities;registerSpawning(Ljava/lang/String;IILnet/minecraft/world/entity/EntityType$Builder;Ljava/util/function/Predicate;Lnet/minecraft/world/entity/SpawnPlacements$Type;Lnet/minecraft/world/entity/SpawnPlacements$SpawnPredicate;)Lnet/minecraftforge/registries/RegistryObject;",
                    ordinal = 0,
                    remap = false
            ),
            remap = false
    )
    private static void changede$preSizeEntityCollections(CallbackInfo ci) {
        Map<ResourceLocation, Pair<Integer, Integer>> entityColorMap = new HashMap<>(CHANGEDE$ENTITY_COLLECTION_CAPACITY);
        entityColorMap.putAll(ENTITY_COLOR_MAP);
        ENTITY_COLOR_MAP = entityColorMap;

        List<Pair<Supplier<EntityType<? extends ChangedEntity>>, Supplier<AttributeSupplier.Builder>>> attrFuncRegistry =
                new ArrayList<>(CHANGEDE$ENTITY_COLLECTION_CAPACITY);
        attrFuncRegistry.addAll(ATTR_FUNC_REGISTRY);
        ATTR_FUNC_REGISTRY = attrFuncRegistry;

        List<ChangedEntities.VoidConsumer> initFuncRegistry = new ArrayList<>(CHANGEDE$ENTITY_COLLECTION_CAPACITY);
        initFuncRegistry.addAll(INIT_FUNC_REGISTRY);
        INIT_FUNC_REGISTRY = initFuncRegistry;

        Map<RegistryObject<? extends EntityType<?>>, RegistryObject<ForgeSpawnEggItem>> spawnEggs =
                new HashMap<>(CHANGEDE$ENTITY_COLLECTION_CAPACITY);
        spawnEggs.putAll(SPAWN_EGGS);
        SPAWN_EGGS = spawnEggs;

        Map<Supplier<? extends EntityType<?>>, Predicate<Level>> dimensionRestrictions =
                new HashMap<>(CHANGEDE$ENTITY_COLLECTION_CAPACITY);
        dimensionRestrictions.putAll(DIMENSION_RESTRICTIONS);
        DIMENSION_RESTRICTIONS = dimensionRestrictions;
    }
}
