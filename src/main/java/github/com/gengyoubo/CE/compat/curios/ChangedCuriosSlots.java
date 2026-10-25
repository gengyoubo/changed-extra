package github.com.gengyoubo.CE.compat.curios;

import github.com.gengyoubo.CE.mixins.CuriosEntityManagerAccessor;
import github.com.gengyoubo.CE.mixins.CuriosSlotManagerAccessor;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.ltxprogrammer.changed.data.AccessorySlotContext;
import net.ltxprogrammer.changed.data.AccessorySlotType;
import net.ltxprogrammer.changed.entity.AccessoryEntities;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.item.AccessoryItem;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.common.data.CuriosEntityManager;
import top.theillusivec4.curios.common.data.CuriosSlotManager;
import top.theillusivec4.curios.common.slottype.SlotType;

/** Adds missing slot types after Curios has merged all datapacks and configuration. */
public final class ChangedCuriosSlots {
    private static final ResourceLocation VALIDATOR = ResourceLocation.fromNamespaceAndPath("changede", "changed_accessory");

    static void initialize(IEventBus modBus) {
        CuriosApi.registerCurioPredicate(VALIDATOR,
                result -> typeFor(result.slotContext().identifier(), result.stack()).isPresent());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, AddReloadListenerEvent.class,
                event -> event.addListener((ResourceManagerReloadListener) resources -> installSlots()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, CurioEquipEvent.class, event -> {
            if (event.getResult() == Event.Result.DENY) return;
            boolean changedItem = typeFor(event.getSlotContext().identifier(), event.getStack()).isPresent();
            if (changedItem ? !canEquip(event.getSlotContext(), event.getStack()) : blockedByChangedEquipment(event.getSlotContext())) {
                event.setResult(Event.Result.DENY);
            }
        });
        modBus.addListener((FMLLoadCompleteEvent event) -> event.enqueueWork(() -> {
            for (var item : ForgeRegistries.ITEMS) {
                if (item instanceof AccessoryItem && !CuriosApi.getCurio(new ItemStack(item)).isPresent()) {
                    CuriosApi.registerCurio(item, new ChangedCurioItem());
                }
            }
        }));
    }

    public static List<String> aliases(AccessorySlotType type) {
        var id = ChangedRegistry.ACCESSORY_SLOTS.getKey(type);
        return id.getNamespace().equals("changed") && id.getPath().equals("neck")
                ? List.of("necklace", "neck") : List.of(id.getPath());
    }

    static String identifier(AccessorySlotType type, Collection<String> existing) {
        return aliases(type).stream().filter(existing::contains).findFirst().orElse(aliases(type).get(0));
    }

    public static void installSlots() {
        var slots = new LinkedHashMap<>(CuriosSlotManager.SERVER.getSlots());
        var accessor = (CuriosEntityManagerAccessor) CuriosEntityManager.SERVER;
        var entities = new LinkedHashMap<>(accessor.changede$getEntitySlots());
        var playerSlots = new LinkedHashMap<>(entities.getOrDefault(EntityType.PLAYER, Map.of()));
        for (var type : ChangedRegistry.ACCESSORY_SLOTS.get().getValues()) {
            // Prefer an alias already assigned to players, then an existing global type.
            String id = aliases(type).stream().filter(playerSlots::containsKey).findFirst()
                    .orElseGet(() -> identifier(type, slots.keySet()));
            ISlotType existing = slots.get(id);
            if (existing == null) {
                existing = new SlotType.Builder(id).size(1).icon(type.getNoItemIcon()).build();
            }
            // Preserve size (including zero), order, icon, cosmetics and existing validators.
            var data = existing.writeNbt();
            var validators = data.getList("Validators", Tag.TAG_STRING);
            if (!existing.getValidators().contains(VALIDATOR)) validators.add(StringTag.valueOf(VALIDATOR.toString()));
            data.put("Validators", validators);
            ISlotType resolved = SlotType.from(data);
            slots.put(id, resolved);
            playerSlots.put(id, resolved);
        }
        ((CuriosSlotManagerAccessor) CuriosSlotManager.SERVER).changede$setSlots(Map.copyOf(slots));
        entities.put(EntityType.PLAYER, Map.copyOf(playerSlots));
        accessor.changede$setEntitySlots(Map.copyOf(entities));
    }

    public static Optional<AccessorySlotType> typeFor(String identifier, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        return ChangedRegistry.ACCESSORY_SLOTS.get().getValues().stream()
                .filter(type -> (aliases(type).contains(identifier) || identifier.equals("curio")) && stack.is(type.getItemTag()))
                .findFirst();
    }

    static boolean canEquip(SlotContext context, ItemStack stack) {
        var type = typeFor(context.identifier(), stack).orElse(null);
        if (type == null) return false;
        var wearer = context.entity();
        if (wearer == null) return true;
        if (!type.canHoldItem(stack, wearer)
                || !AccessoryEntities.INSTANCE.canEntityTypeUseSlot(AccessoryEntities.getApparentEntityType(wearer)).test(type)) return false;
        if (context.cosmetic()) return true;
        return CuriosApi.getCuriosInventory(wearer).map(curios -> {
            for (var entry : curios.getCurios().entrySet()) {
                var stacks = entry.getValue().getStacks();
                for (int index = 0; index < stacks.getSlots(); index++) {
                    if (entry.getKey().equals(context.identifier()) && index == context.index()) continue;
                    ItemStack other = stacks.getStackInSlot(index);
                    if (other.isEmpty()) continue;
                    var changedType = typeFor(entry.getKey(), other);
                    var otherType = changedType.orElseGet(() -> typeForIdentifier(entry.getKey()).orElse(null));
                    if (otherType == null) continue;
                    // Changed has one equipment position per body part even if a shared Curios type has several slots.
                    if (otherType == type && changedType.isPresent()) return false;
                    if (stack.getItem() instanceof AccessoryItem item
                            && (!item.allowedWith(stack, other, wearer, type, otherType)
                            || item.shouldDisableSlot(new AccessorySlotContext<>(wearer, type, stack), otherType))) return false;
                    if (other.getItem() instanceof AccessoryItem item
                            && (!item.allowedWith(other, stack, wearer, otherType, type)
                            || item.shouldDisableSlot(new AccessorySlotContext<>(wearer, otherType, other), type))) return false;
                }
            }
            return true;
        }).orElse(false);
    }

    private static Optional<AccessorySlotType> typeForIdentifier(String identifier) {
        return ChangedRegistry.ACCESSORY_SLOTS.get().getValues().stream()
                .filter(type -> aliases(type).contains(identifier)).findFirst();
    }

    private static boolean blockedByChangedEquipment(SlotContext context) {
        if (context.cosmetic() || context.entity() == null) return false;
        var targetType = typeForIdentifier(context.identifier()).orElse(null);
        if (targetType == null) return false;
        return CuriosApi.getCuriosInventory(context.entity()).map(curios -> {
            for (var entry : curios.getCurios().entrySet()) {
                var stacks = entry.getValue().getStacks();
                for (int index = 0; index < stacks.getSlots(); index++) {
                    ItemStack other = stacks.getStackInSlot(index);
                    var type = typeFor(entry.getKey(), other).orElse(null);
                    if (type != null && other.getItem() instanceof AccessoryItem item
                            && item.shouldDisableSlot(new AccessorySlotContext<>(context.entity(), type, other), targetType)) return true;
                }
            }
            return false;
        }).orElse(false);
    }

    private ChangedCuriosSlots() {}
}
