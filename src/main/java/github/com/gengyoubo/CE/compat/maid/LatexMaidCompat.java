package github.com.gengyoubo.CE.compat.maid;

import github.com.gengyoubo.CE.compat.synergy.ChangedSynergyFeedApi;
import github.com.gengyoubo.CE.compat.synergy.CreatureInventoryAccess;
import net.minecraft.world.Container;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.process.TransfurEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.EntityArmorInvWrapper;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.MaidInvWrapper;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.MiddleBackpack;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.parkabird.changedsynergy.world.inventory.BondedCreatureInventoryMenu;
import net.parkabird.changedsynergy.world.inventory.BondedInventoryService;
import net.parkabird.changedsynergy.world.inventory.BondedLatexMenu;
import net.parkabird.changedsynergy.event.LatexSocialEvents;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Optional TLM bridge. This class is only loaded when both companion mods are present. */
public final class LatexMaidCompat {
    private static final String TASK_TAG = "changede_maid_work_task";
    private static final String WORK_OWNER_TAG = "changede_maid_work_owner";
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "changede");
    static final RegistryObject<MenuType<MaidWorkMenu>> WORK_MENU = MENUS.register("latex_maid_work", () -> IForgeMenuType.create(MaidWorkMenu::new));
    private static final Map<UUID, SyntheticMaid> WORKERS = new ConcurrentHashMap<>();
    private static final Set<UUID> FAILED_WORKERS = ConcurrentHashMap.newKeySet();

    /** SC replaces the entity when a creature changes species; its continuity copy omits CE's tags. */
    public static void transferWorkState(ChangedEntity previous, ChangedEntity replacement) {
        if (previous == replacement || replacement.level().isClientSide()) return;
        var source = previous.getPersistentData();
        var destination = replacement.getPersistentData();
        if (source.contains(TASK_TAG, net.minecraft.nbt.Tag.TAG_STRING)) {
            destination.putString(TASK_TAG, source.getString(TASK_TAG));
        }
        if (source.hasUUID(WORK_OWNER_TAG)) {
            destination.putUUID(WORK_OWNER_TAG, source.getUUID(WORK_OWNER_TAG));
        }
        BodyWorkBuffer.transfer(previous, replacement);
        releaseWorker(previous);
        FAILED_WORKERS.remove(previous.getUUID());
    }

    private LatexMaidCompat() {}

    public static void initialize(IEventBus modBus) {
        MENUS.register(modBus);
        modBus.addListener(LatexMaidCompat::clientSetup);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingEvent.LivingTickEvent.class, LatexMaidCompat::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, event -> {
            if (event.getEntity() instanceof ChangedEntity creature && !creature.level().isClientSide()) {
                releaseWorker(creature);
                BodyWorkBuffer.release(creature);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((EntityLeaveLevelEvent event) -> {
            if (event.getEntity() instanceof ChangedEntity creature && !creature.level().isClientSide()) releaseWorker(creature);
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, EntityJoinLevelEvent.class, event -> {
            // A task adapter must never become a second physical/saved entity.
            if (event.getEntity() instanceof SyntheticMaid) event.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> {
            WORKERS.values().forEach(SyntheticMaid::retire);
            WORKERS.clear();
            FAILED_WORKERS.clear();
        });
        if (Boolean.getBoolean("changede.verifyMaidLifecycle")) {
            MinecraftForge.EVENT_BUS.addListener(MaidLifecycleRegressionChecks::verify);
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                TransfurEvents.ChangedEntityFusionWithMobEvent.class,
                event -> transferAfterFusion(event.getSourceEntity(), event.getFusionEntity().getEntity()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                TransfurEvents.ChangedEntityFusionWithChangedEntityEvent.class,
                event -> transferAfterFusion(event.getSourceEntity(), event.getFusionEntity().getEntity()));
    }

    private static void transferAfterFusion(LivingEntity source, LivingEntity result) {
        if (source instanceof ChangedEntity previous && result instanceof ChangedEntity replacement) {
            transferWorkState(previous, replacement);
        }
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.gui.screens.MenuScreens.register(WORK_MENU.get(), MaidWorkScreen::new);
            MaidWorkTabClient.install();
        });
    }

    private static void openWorkScreen(ServerPlayer player, ChangedEntity creature, boolean fromWheel) {
        MenuProvider provider = new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("screen.changede.maid_work.title"); }
            @Override public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player opener) {
                return new MaidWorkMenu(id, inventory, creature.getId(), fromWheel);
            }
        };
        NetworkHooks.openScreen(player, provider, buffer -> {
            buffer.writeInt(creature.getId());
            buffer.writeBoolean(fromWheel);
        });
    }

    /** Called only by the server after a tab switch packet. */
    public static void switchMenu(ServerPlayer player, int creatureId, boolean openWork) {
        if (!(player.level().getEntity(creatureId) instanceof ChangedEntity creature)
                || !creature.isAlive() || player.distanceToSqr(creature) > 64.0D) return;

        if (openWork) {
            boolean fromWheel = player.containerMenu instanceof BondedLatexMenu wheel && wheel.getPet() == creature;
            boolean fromInventory = player.containerMenu instanceof BondedCreatureInventoryMenu inventory
                    && inventory.getPet() == creature;
            if (!fromWheel && !fromInventory) return;
            if (!ChangedSynergyFeedApi.hasMaximumFamiliarity(creature, player)) {
                player.displayClientMessage(Component.translatable("message.changede.maid_work.max_familiarity"), true);
                return;
            }
            if (isWorkBoundToOtherPlayer(creature, player)) {
                player.displayClientMessage(Component.translatable("message.changede.maid_work.bound_other"), true);
                return;
            }
            // Tasks assigned before UUID binding was added are claimed by the next
            // eligible player who opens their existing Synergy creature menu.
            if (!creature.getPersistentData().hasUUID(WORK_OWNER_TAG)
                    && !creature.getPersistentData().getString(TASK_TAG).isEmpty()) {
                creature.getPersistentData().putUUID(WORK_OWNER_TAG, player.getUUID());
            }
            openWorkScreen(player, creature, fromWheel);
        } else {
            if (!(player.containerMenu instanceof MaidWorkMenu workMenu) || workMenu.creatureId() != creatureId) return;
            if (workMenu.fromWheel()) LatexSocialEvents.openBondedPetMenu(player, creature);
            else BondedInventoryService.open(player, creature);
        }
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ChangedEntity creature) || creature.level().isClientSide()
                || !(creature.level() instanceof ServerLevel level)) return;
        if (!creature.isAlive() || creature.isRemoved()) {
            releaseWorker(creature);
            FAILED_WORKERS.remove(creature.getUUID());
            return;
        }
        String taskId = creature.getPersistentData().getString(TASK_TAG);
        if (taskId.isEmpty()) {
            releaseWorker(creature);
            FAILED_WORKERS.remove(creature.getUUID());
            restoreNativeAi(creature);
            return;
        }

        if (!creature.getPersistentData().hasUUID(WORK_OWNER_TAG)) {
            releaseWorker(creature);
            FAILED_WORKERS.remove(creature.getUUID());
            restoreNativeAi(creature);
            return;
        }
        UUID ownerId = creature.getPersistentData().getUUID(WORK_OWNER_TAG);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            releaseWorker(creature);
            restoreNativeAi(creature);
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(taskId);
        IMaidTask task = id == null ? null : TaskManager.getTaskMap().get(id);
        if (task == null) {
            creature.getPersistentData().remove(TASK_TAG);
            releaseWorker(creature);
            FAILED_WORKERS.remove(creature.getUUID());
            restoreNativeAi(creature);
            return;
        }

        // The latex body keeps running its own Monster goal AI (wander, transfur,
        // hunt players) while the TLM maid Brain drives it. Suppress that native AI
        // so the two "brains" cannot fight over the same body.
        suppressNativeAi(creature);

        SyntheticMaid maid = WORKERS.compute(creature.getUUID(), (uuid, current) -> {
            if (current == null || current.isRemoved() || !current.isAlive()
                    || current.body != creature || current.level() != level || current.getTask() != task) {
                if (current != null) current.retire();
                try {
                    current = new SyntheticMaid(level, creature, owner);
                    current.setTask(task);
                } catch (RuntimeException | LinkageError exception) {
                    if (FAILED_WORKERS.add(creature.getUUID())) {
                        github.com.gengyoubo.CE.changede.LOGGER.warn("Could not start latex maid task {} for {}", taskId, creature.getType(), exception);
                    }
                    return null;
                }
            }
            current.setWorkOwner(owner);
            return current;
        });
        if (maid == null) {
            restoreNativeAi(creature);
            return;
        }

        // The maid's full entity tick runs its Brain, navigation, movement control,
        // and task actions. Ticking only Brain and navigation leaves it stationary.
        try {
            Container inventory = CreatureInventoryAccess.resolve(creature);
            maid.syncFrom(creature, inventory);
            VecPosition before = VecPosition.of(maid);
            try {
                maid.tick();
            } finally {
                try {
                    maid.syncInventoryTo(creature, inventory);
                } finally {
                    maid.workInventory = null;
                }
            }
            if (!creature.isAlive() || creature.isRemoved() || maid.isRemoved()) {
                releaseWorker(creature);
                return;
            }
            VecPosition after = VecPosition.of(maid);
            if (before.distanceSquared(after) > 1.0E-5) {
                creature.moveTo(after.x, after.y, after.z, maid.getYRot(), maid.getXRot());
            }
            FAILED_WORKERS.remove(creature.getUUID());
        } catch (RuntimeException | LinkageError exception) {
            releaseWorker(creature);
            restoreNativeAi(creature);
            if (FAILED_WORKERS.add(creature.getUUID())) {
                github.com.gengyoubo.CE.changede.LOGGER.warn("Latex maid task {} failed for {}", taskId, creature.getType(), exception);
            }
        }
    }

    private static void releaseWorker(ChangedEntity creature) {
        SyntheticMaid worker = WORKERS.get(creature.getUUID());
        if (worker != null && worker.body == creature && WORKERS.remove(creature.getUUID(), worker)) worker.retire();
    }

    /** While a maid task is bound, the latex body must not run its own goal AI. */
    private static void suppressNativeAi(ChangedEntity creature) {
        if (!creature.isNoAi()) {
            creature.setNoAi(true);
        }
        if (creature.getTarget() != null) {
            creature.setTarget(null);
        }
    }

    /** Restores the latex body's native AI when it is no longer driven as a maid. */
    private static void restoreNativeAi(ChangedEntity creature) {
        if (creature.isNoAi()) {
            creature.setNoAi(false);
        }
    }

    static java.util.List<IMaidTask> tasks() { return TaskManager.getTaskIndex(); }
    static String taskTag() { return TASK_TAG; }
    static String workOwnerTag() { return WORK_OWNER_TAG; }

    static boolean isWorkBoundToOtherPlayer(ChangedEntity creature, Player player) {
        return creature.getPersistentData().hasUUID(WORK_OWNER_TAG)
                && !creature.getPersistentData().getUUID(WORK_OWNER_TAG).equals(player.getUUID());
    }

    private record VecPosition(double x, double y, double z) {
        static VecPosition of(EntityMaid maid) { return new VecPosition(maid.getX(), maid.getY(), maid.getZ()); }
        double distanceSquared(VecPosition other) {
            double dx=x-other.x, dy=y-other.y, dz=z-other.z;
            return dx*dx+dy*dy+dz*dz;
        }
    }

    /** Latex = the sole real entity and state owner; this adapter only borrows TLM work AI. */
    static final class SyntheticMaid extends EntityMaid {
        private ServerPlayer workOwner;
        private final ChangedEntity body;
        private Container workInventory;
        private final ItemStackHandler borrowedInventory = new BorrowedInventory();
        private final ItemStackHandler borrowedHide;
        private final ItemStackHandler borrowedTask;

        SyntheticMaid(ServerLevel level, ChangedEntity body, ServerPlayer owner) {
            super(level);
            this.workOwner = owner;
            this.body = body;
            BodyWorkBuffer.release(body);
            borrowedHide = new BodyWorkBuffer(body, "hand", 1, this::canWork);
            borrowedTask = new BodyWorkBuffer(body, "task", 9, this::canWork);
            syncFrom(body);
            setTame(true);
            setOwnerUUID(owner.getUUID());
            setFavorability(384);
            setSchedule(MaidSchedule.ALL);
            setMaidBackpackType(new MiddleBackpack());
            setPickup(false);
            setHomeModeEnable(false);
        }

        void setWorkOwner(ServerPlayer owner) { workOwner = owner; }
        void syncFrom(ChangedEntity source) {
            setPos(source.getX(), source.getY(), source.getZ());
            setYRot(source.getYRot());
            setXRot(source.getXRot());
            mirrorHealth();
        }
        void syncFrom(ChangedEntity source, Container inventory) {
            if (!canWork()) return;
            workInventory = inventory;
            syncFrom(source);
        }
        void syncInventoryTo(ChangedEntity target, Container inventory) {
            // Work mutates the real storage directly. Only flush in-place item/NBT changes.
            if (target != body || !canWork()) return;
            inventory.setChanged();
        }
        @Override public LivingEntity getOwner() { return workOwner; }

        @Override public ItemStackHandler getMaidInv() {
            return body == null ? super.getMaidInv() : borrowedInventory;
        }
        @Override public ItemStackHandler getHideInv() { return body == null ? super.getHideInv() : borrowedHide; }
        @Override public ItemStackHandler getTaskInv() { return body == null ? super.getTaskInv() : borrowedTask; }

        @Override public CombinedInvWrapper getAvailableInv(boolean handsFirst) {
            return handsFirst ? new MaidInvWrapper(this, getHandsInvWrapper(), getMaidInv())
                    : new MaidInvWrapper(this, getMaidInv(), getHandsInvWrapper());
        }

        @Override public CombinedInvWrapper getAvailableBackpackInv() {
            return new MaidInvWrapper(this, getMaidInv());
        }

        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
            if (body == null || capability != ForgeCapabilities.ITEM_HANDLER) return super.getCapability(capability, facing);
            if (!canWork()) return LazyOptional.empty();
            // TLM's default implementation uses its private inventory instead of getMaidInv().
            if (facing == null) return LazyOptional.of(() -> new CombinedInvWrapper(
                    new EntityArmorInvWrapper(this), getHandsInvWrapper(), getMaidInv())).cast();
            if (facing.getAxis().isVertical()) return LazyOptional.of(this::getHandsInvWrapper).cast();
            return LazyOptional.of(() -> new EntityArmorInvWrapper(this)).cast();
        }

        @Override public ItemStack getItemBySlot(EquipmentSlot slot) {
            if (body == null) return super.getItemBySlot(slot);
            return canWork() ? body.getItemBySlot(slot) : ItemStack.EMPTY;
        }

        @Override public ItemStack getMainHandItem() { return getItemBySlot(EquipmentSlot.MAINHAND); }
        @Override public ItemStack getOffhandItem() { return getItemBySlot(EquipmentSlot.OFFHAND); }
        @Override public Iterable<ItemStack> getArmorSlots() {
            return canWork() ? body.getArmorSlots() : super.getArmorSlots();
        }
        @Override public Iterable<ItemStack> getHandSlots() {
            return canWork() ? body.getHandSlots() : super.getHandSlots();
        }

        @Override public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
            if (body == null) super.setItemSlot(slot, stack);
            else if (canWork()) body.setItemSlot(slot, stack);
        }

        @Override public float getHealth() {
            if (body == null) return super.getHealth();
            return isRemoved() ? 0 : getMaxHealth() * body.getHealth() / Math.max(1.0F, body.getMaxHealth());
        }

        @Override public void setHealth(float health) {
            // Constructor initialization is allowed; TLM cannot independently change live health.
            if (body == null) super.setHealth(health);
        }

        private Container borrowedStorage() {
            return !canWork() ? null : workInventory != null ? workInventory : CreatureInventoryAccess.resolve(body);
        }

        /** A view, never a second bag. Retained task/capability handles stop working on retirement. */
        private final class BorrowedInventory extends ItemStackHandler {
            BorrowedInventory() { super(24); }

            @Override public ItemStack getStackInSlot(int slot) {
                validateSlotIndex(slot);
                Container inventory = borrowedStorage();
                return inventory == null ? ItemStack.EMPTY : inventory.getItem(slot);
            }

            @Override public void setStackInSlot(int slot, ItemStack stack) {
                validateSlotIndex(slot);
                Container inventory = borrowedStorage();
                if (inventory != null) inventory.setItem(slot, stack);
            }

            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                validateSlotIndex(slot);
                Container inventory = borrowedStorage();
                if (inventory == null || stack.isEmpty()) return stack;
                ItemStack present = inventory.getItem(slot);
                if (!present.isEmpty() && !ItemStack.isSameItemSameTags(present, stack)) return stack;
                int moved = Math.min(stack.getCount(), Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize()) - present.getCount());
                if (moved <= 0) return stack;
                if (!simulate) {
                    ItemStack inserted = stack.copy();
                    inserted.setCount(present.getCount() + moved);
                    inventory.setItem(slot, inserted);
                }
                return moved == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
            }

            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                validateSlotIndex(slot);
                Container inventory = borrowedStorage();
                if (inventory == null || amount <= 0) return ItemStack.EMPTY;
                ItemStack present = inventory.getItem(slot);
                int extracted = Math.min(amount, present.getCount());
                return simulate ? present.copyWithCount(extracted) : inventory.removeItem(slot, extracted);
            }
        }

        private boolean canWork() {
            return body != null && body.isAlive() && !body.isRemoved() && !isRemoved();
        }

        private void mirrorHealth() {
            if (body != null && !isRemoved()) {
                super.setHealth(getMaxHealth() * body.getHealth() / Math.max(1.0F, body.getMaxHealth()));
            }
        }

        @Override
        public void tick() {
            if (!canWork()) {
                retire();
                return;
            }
            super.tick();
            mirrorHealth();
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (!canWork()) return false;
            // Flush real storage before its only owner handles damage and death loot.
            if (workInventory != null) syncInventoryTo(body, workInventory);
            boolean hurt = body.hurt(source, amount);
            if (!body.isAlive() || body.isRemoved()) retire();
            else mirrorHealth();
            return hurt;
        }

        @Override
        public void die(DamageSource source) {
            // Never call EntityMaid.die: it creates a tombstone and a revivable maid film.
            if (canWork()) hurt(source, Float.MAX_VALUE);
        }

        @Override public void heal(float amount) { /* Recovery belongs to the latex body. */ }
        @Override protected void dropAllDeathLoot(DamageSource source) { }
        @Override protected void dropEquipment() { }
        @Override protected void dropExperience() { }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean save(CompoundTag tag) { return false; }
        @Override public boolean saveAsPassenger(CompoundTag tag) { return false; }

        void retire() {
            remove(RemovalReason.DISCARDED);
        }

        @Override
        public void remove(RemovalReason reason) {
            if (!isRemoved()) BodyWorkBuffer.release(body);
            workInventory = null;
            workOwner = null;
            // Clear only TLM's unused private buffers, never the borrowed body's storage.
            for (int slot = 0; slot < super.getMaidInv().getSlots(); slot++) super.getMaidInv().setStackInSlot(slot, ItemStack.EMPTY);
            for (int slot = 0; slot < getMaidBauble().getSlots(); slot++) getMaidBauble().setStackInSlot(slot, ItemStack.EMPTY);
            for (int slot = 0; slot < super.getHideInv().getSlots(); slot++) super.getHideInv().setStackInSlot(slot, ItemStack.EMPTY);
            for (int slot = 0; slot < super.getTaskInv().getSlots(); slot++) super.getTaskInv().setStackInSlot(slot, ItemStack.EMPTY);
            for (EquipmentSlot slot : EquipmentSlot.values()) super.setItemSlot(slot, ItemStack.EMPTY);
            super.remove(reason);
        }

        @Override
        public boolean canAttack(LivingEntity target) {
            if (isSelfOrSameKind(target)) {
                return false;
            }
            return super.canAttack(target);
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            if (isSelfOrSameKind(target)) {
                return false;
            }
            return super.doHurtTarget(target);
        }

        @Override
        public LivingEntity getTarget() {
            LivingEntity target = super.getTarget();
            return target != null && isSelfOrSameKind(target) ? null : target;
        }

        /** The maid is the latex body; it must never target its own body or its own latex kind. */
        private boolean isSelfOrSameKind(Entity other) {
            if (other == null) {
                return false;
            }
            if (other == body || other.getUUID().equals(body.getUUID())) {
                return true;
            }
            if (other instanceof ChangedEntity otherChanged) {
                LatexType selfType = body.getLatexType();
                return selfType != null && selfType == otherChanged.getLatexType();
            }
            return false;
        }
    }
}
