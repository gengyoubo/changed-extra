package github.com.gengyoubo.CE.compat.synergy;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import github.com.gengyoubo.CE.changede;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.lang.reflect.Method;
import java.util.Optional;

/** Persistent recovery checkpoints; ordinary item use never triggers automatic recovery. */
public final class CreatureInventoryBackup {
    private static final String BACKUP = "changede_inventory_backup";
    private static final String PROMOTION = "changede_inventory_before_promotion";
    private static final String SYNERGY = "ChangedSynergyBondedInventory";
    private static final int HISTORY_SIZE = 5;
    private static final ClassValue<Optional<Method>> NATIVE_INVENTORY = new ClassValue<>() {
        @Override protected Optional<Method> computeValue(Class<?> type) {
            try {
                return Optional.of(type.getMethod("getInventory"));
            } catch (NoSuchMethodException ignored) {
                return Optional.empty();
            }
        }
    };

    private CreatureInventoryBackup() {}

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                LivingEvent.LivingTickEvent.class, CreatureInventoryBackup::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(CreatureInventoryBackup::registerCommands);
        if (Boolean.getBoolean("changede.verifyInventoryBackup")) {
            MinecraftForge.EVENT_BUS.addListener(InventoryBackupRegressionChecks::verify);
        }
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof ChangedEntity creature && !creature.level().isClientSide()
                && creature.isAlive() && !creature.isRemoved()
                && Math.floorMod(creature.tickCount + creature.getId(), 100) == 0) {
            checkpoint(creature, "periodic");
        }
    }

    /** CS promotes friends to native pets when forming a bond or opening their inventory. */
    public static void beforePromotion(ChangedEntity creature) {
        if (creature.level().isClientSide()) return;
        CompoundTag state = capture(creature);
        checkpoint(creature, state, "before_taming");
        // Only a newly created native inventory changes the source of hand equipment.
        if (nativeInventory(creature) == null) {
            creature.getPersistentData().put(PROMOTION, state.copy());
        } else {
            creature.getPersistentData().remove(PROMOTION);
        }
    }

    public static void afterPromotion(ChangedEntity creature) {
        if (creature.level().isClientSide()) return;
        var data = creature.getPersistentData();
        if (data.contains(PROMOTION, Tag.TAG_COMPOUND)) {
            CompoundTag before = data.getCompound(PROMOTION);
            data.remove(PROMOTION);
            if (nativeInventory(creature) != null) {
                restoreEquipment(creature, before.getCompound("Equipment"));
            }
        }
        checkpoint(creature, "after_taming");
    }

    /** Carries recovery records along when CS replaces the body with a different species. */
    public static void transfer(ChangedEntity previous, ChangedEntity replacement) {
        if (previous == replacement || replacement.level().isClientSide()) return;
        checkpoint(previous, "before_morph");
        if (previous.getPersistentData().contains(BACKUP, Tag.TAG_COMPOUND)) {
            replacement.getPersistentData().put(BACKUP, previous.getPersistentData().getCompound(BACKUP).copy());
        }
        checkpoint(replacement, "after_morph");
    }

    public static void checkpoint(ChangedEntity creature, String reason) {
        if (!creature.level().isClientSide()) checkpoint(creature, capture(creature), reason);
    }

    private static void checkpoint(ChangedEntity creature, CompoundTag state, String reason) {
        CompoundTag data = creature.getPersistentData();
        CompoundTag backup = data.getCompound(BACKUP);
        if (isEmpty(state) && !data.contains(BACKUP, Tag.TAG_COMPOUND)) return;
        if (backup.getCompound("Current").equals(state)) return;
        backup.put("Current", state.copy());
        // Empty states are legitimate, but must not displace the last recoverable records.
        if (!isEmpty(state)) {
            ListTag history = backup.getList("History", Tag.TAG_COMPOUND);
            if (history.isEmpty() || !history.getCompound(0).getCompound("State").equals(state)) {
                CompoundTag record = new CompoundTag();
                record.put("State", state.copy());
                record.putLong("Time", creature.level().getGameTime());
                record.putString("Reason", reason);
                history.add(0, record);
                while (history.size() > HISTORY_SIZE) history.remove(history.size() - 1);
                backup.put("History", history);
            }
        }
        data.put(BACKUP, backup);
    }

    static CompoundTag capture(ChangedEntity creature) {
        CompoundTag state = new CompoundTag();
        Container inventory = nativeInventory(creature);
        if (inventory != null) {
            ListTag items = new ListTag();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty()) {
                    CompoundTag entry = stack.save(new CompoundTag());
                    entry.putInt("Slot", slot);
                    items.add(entry);
                }
            }
            state.put("NativeItems", items);
            state.putInt("NativeSize", inventory.getContainerSize());
            try {
                state.putInt("Selected", inventory.getClass().getField("selected").getInt(inventory));
            } catch (ReflectiveOperationException ignored) { /* Not all containers select a main hand. */ }
        }
        if (creature.getPersistentData().contains(SYNERGY, Tag.TAG_COMPOUND)) {
            state.put("Synergy", creature.getPersistentData().getCompound(SYNERGY).copy());
        }
        CompoundTag equipment = new CompoundTag();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = creature.getItemBySlot(slot);
            if (!stack.isEmpty()) equipment.put(slot.getName(), stack.save(new CompoundTag()));
        }
        state.put("Equipment", equipment);
        return state;
    }

    private static Container nativeInventory(ChangedEntity creature) {
        Optional<Method> getter = NATIVE_INVENTORY.get(creature.getClass());
        if (getter.isEmpty()) return null;
        try {
            return getter.get().invoke(creature) instanceof Container inventory ? inventory : null;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not back up native inventory of " + creature.getType(), exception);
        }
    }

    private static boolean isEmpty(CompoundTag state) {
        return state.getList("NativeItems", Tag.TAG_COMPOUND).isEmpty()
                && state.getCompound("Synergy").getList("Items", Tag.TAG_COMPOUND).isEmpty()
                && state.getCompound("Equipment").isEmpty();
    }

    private static int restoreEquipment(ChangedEntity creature, CompoundTag equipment) {
        int restored = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = ItemStack.of(equipment.getCompound(slot.getName()));
            if (!stack.isEmpty() && creature.getItemBySlot(slot).isEmpty()) {
                creature.setItemSlot(slot, stack.copy());
                restored++;
            }
        }
        return restored;
    }

    /** An explicit operator recovery fills missing slots, preserving all occupied slots. */
    static int restore(ChangedEntity creature, CompoundTag state) {
        int restored = 0;
        Container nativeInventory = nativeInventory(creature);
        if (state.contains("NativeSize") && nativeInventory == null) {
            // Do not silently move selected native-hand items into a separate CS backpack.
            return -1;
        }
        if (nativeInventory != null && state.contains("NativeSize")) {
            boolean mainHandWasEmpty = creature.getMainHandItem().isEmpty();
            for (Tag tag : state.getList("NativeItems", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) tag;
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < nativeInventory.getContainerSize() && nativeInventory.getItem(slot).isEmpty()) {
                    nativeInventory.setItem(slot, ItemStack.of(entry).copy());
                    restored++;
                }
            }
            // Keep the recovered main hand in its original native slot, not another copy.
            if (mainHandWasEmpty && state.contains("Selected")) {
                int selected = state.getInt("Selected");
                if (selected >= 0 && selected < Math.min(24, nativeInventory.getContainerSize())) {
                    try {
                        nativeInventory.getClass().getField("selected").setInt(nativeInventory, selected);
                    } catch (ReflectiveOperationException ignored) { /* No selected-slot support. */ }
                }
            }
            nativeInventory.setChanged();
        }
        if (state.contains("Synergy", Tag.TAG_COMPOUND)) {
            BondedStorageRecovery recovery = new BondedStorageRecovery(creature);
            restored += recovery.restore(state.getCompound("Synergy"));
        }
        CompoundTag equipment = state.getCompound("Equipment").copy();
        // Native snapshots already include both hands. Re-applying a hand can duplicate
        // the selected stack when another item currently occupies its original slot.
        if (state.contains("NativeSize")) {
            equipment.remove(EquipmentSlot.MAINHAND.getName());
            equipment.remove(EquipmentSlot.OFFHAND.getName());
        }
        return restored + restoreEquipment(creature, equipment);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ce_inventory_backup")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").then(Commands.argument("target", EntityArgument.entity())
                        .executes(context -> {
                            if (!(EntityArgument.getEntity(context, "target") instanceof ChangedEntity creature)) {
                                context.getSource().sendFailure(Component.translatable("command.changede.inventory_backup.creature_required"));
                                return 0;
                            }
                            checkpoint(creature, "inspection");
                            ListTag history = creature.getPersistentData().getCompound(BACKUP).getList("History", Tag.TAG_COMPOUND);
                            context.getSource().sendSuccess(() -> Component.translatable("command.changede.inventory_backup.count", history.size()), false);
                            for (int index = 0; index < history.size(); index++) {
                                CompoundTag record = history.getCompound(index);
                                Component line = Component.translatable("command.changede.inventory_backup.record", index,
                                        record.getLong("Time"), record.getString("Reason"));
                                context.getSource().sendSuccess(() -> line, false);
                            }
                            return history.size();
                        })))
                .then(Commands.literal("restore").then(Commands.argument("target", EntityArgument.entity())
                        .then(Commands.argument("index", IntegerArgumentType.integer(0, HISTORY_SIZE - 1))
                                .executes(context -> {
                                    if (!(EntityArgument.getEntity(context, "target") instanceof ChangedEntity creature)) {
                                        context.getSource().sendFailure(Component.translatable("command.changede.inventory_backup.creature_required"));
                                        return 0;
                                    }
                                    CompoundTag backup = creature.getPersistentData().getCompound(BACKUP);
                                    ListTag history = backup.getList("History", Tag.TAG_COMPOUND);
                                    int index = IntegerArgumentType.getInteger(context, "index");
                                    if (index >= history.size()) {
                                        context.getSource().sendFailure(Component.translatable("command.changede.inventory_backup.missing"));
                                        return 0;
                                    }
                                    int restored = restore(creature, history.getCompound(index).getCompound("State"));
                                    if (restored < 0) {
                                        context.getSource().sendFailure(Component.translatable("command.changede.inventory_backup.native_unavailable"));
                                        return 0;
                                    }
                                    changede.LOGGER.info("{} recovered {} inventory slots for {} from checkpoint {}",
                                            context.getSource().getTextName(), restored, creature.getUUID(), index);
                                    if (restored > 0) {
                                        // Keep other recovery versions; the same record cannot be replayed.
                                        history.remove(index);
                                        backup.put("History", history);
                                        backup.put("Current", capture(creature));
                                        creature.getPersistentData().put(BACKUP, backup);
                                    }
                                    context.getSource().sendSuccess(() -> Component.translatable("command.changede.inventory_backup.restored", restored), true);
                                    return restored;
                                })))));
    }

    /** Isolated so merely reading a native-only pet never instantiates CS's storage. */
    private record BondedStorageRecovery(ChangedEntity creature) {
        int restore(CompoundTag saved) {
            var items = net.minecraft.core.NonNullList.withSize(24, ItemStack.EMPTY);
            net.minecraft.world.ContainerHelper.loadAllItems(saved, items);
            var inventory = new net.parkabird.changedsynergy.world.inventory.BondedCreatureInventory(creature);
            int restored = 0;
            for (int slot = 0; slot < items.size(); slot++) {
                if (!items.get(slot).isEmpty() && inventory.getItem(slot).isEmpty()) {
                    inventory.setItem(slot, items.get(slot).copy());
                    restored++;
                }
            }
            return restored;
        }
    }
}
