package github.com.gengyoubo.CE.compat.curios;

import net.ltxprogrammer.changed.data.AccessorySlots;
import net.ltxprogrammer.changed.entity.LivingEntityDataExtension;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

/** Server-side migration; this class is loaded only with Curios present. */
public final class CuriosAccessoryCompat {
    public static void initialize(net.minecraftforge.eventbus.api.IEventBus modBus) {
        ChangedCuriosSlots.initialize(modBus);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerEvent.PlayerLoggedInEvent.class,
                event -> { if (event.getEntity() instanceof ServerPlayer player) migrate(player); });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, TickEvent.PlayerTickEvent.class,
                event -> {
                    if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer player && player.isAlive()) {
                        migrate(player);
                    }
                });
    }

    public static void migrate(ServerPlayer player) {
        AccessorySlots legacy = ((LivingEntityDataExtension) player).getAccessorySlots().orElse(null);
        if (legacy == null || legacy == AccessorySlots.DUMMY || legacy.isEmpty()) return;
        // A missing capability may be temporary during login/respawn. Wait for it instead
        // of treating it as a full inventory and unnecessarily moving equipped items.
        CuriosApi.getCuriosInventory(player).ifPresent(curios -> migrate(player, legacy, curios));
    }

    private static void migrate(ServerPlayer player, AccessorySlots legacy, ICuriosItemHandler curios) {
        for (int index = 0; index < legacy.getContainerSize(); index++) {
            if (legacy.getItem(index).isEmpty()) continue;
            ItemStack remaining = legacy.removeItemNoUpdate(index);
            remaining = insertIntoEmptySlots(curios, remaining);
            if (!remaining.isEmpty()) {
                // Avoid Inventory.add's creative-mode shortcut, which discards overflow.
                remaining = net.minecraftforge.items.ItemHandlerHelper.insertItemStacked(
                        new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(player.getInventory()), remaining, false);
            }
            if (!remaining.isEmpty()) {
                ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), remaining);
                drop.setDefaultPickUpDelay();
                if (!player.level().addFreshEntity(drop)) {
                    // Preserve the unplaced remainder if another mod rejects the drop; retry next tick.
                    legacy.setItem(index, remaining);
                }
            }
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    static ItemStack insertIntoEmptySlots(ICuriosItemHandler curios, ItemStack stack) {
        ItemStack remaining = stack;
        for (var handler : curios.getCurios().values()) {
            var slots = handler.getStacks();
            for (int index = 0; index < slots.getSlots() && !remaining.isEmpty(); index++) {
                if (slots.getStackInSlot(index).isEmpty()) {
                    // insertItem enforces Curios slot validators and equip events, without
                    // replacing existing equipment or placing functional items in cosmetic slots.
                    remaining = slots.insertItem(index, remaining, false);
                }
            }
            if (remaining.isEmpty()) break;
        }
        return remaining;
    }

    private CuriosAccessoryCompat() {}
}
