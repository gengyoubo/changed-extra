package github.com.gengyoubo.CE.compat.maid;

import github.com.gengyoubo.CE.compat.synergy.ChangedSynergyFeedApi;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.latex.LatexType;
import net.ltxprogrammer.changed.process.TransfurEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.entity.living.LivingEvent;
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
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.MaidSchedule;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.MiddleBackpack;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.parkabird.changedsynergy.world.inventory.BondedCreatureInventoryMenu;
import net.parkabird.changedsynergy.world.inventory.BondedCreatureInventory;
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
        WORKERS.remove(previous.getUUID());
        FAILED_WORKERS.remove(previous.getUUID());
    }

    private LatexMaidCompat() {}

    public static void initialize(IEventBus modBus) {
        MENUS.register(modBus);
        modBus.addListener(LatexMaidCompat::clientSetup);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingEvent.LivingTickEvent.class, LatexMaidCompat::onLivingTick);
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
            WORKERS.remove(creature.getUUID());
            FAILED_WORKERS.remove(creature.getUUID());
            return;
        }
        String taskId = creature.getPersistentData().getString(TASK_TAG);
        if (taskId.isEmpty()) {
            WORKERS.remove(creature.getUUID());
            FAILED_WORKERS.remove(creature.getUUID());
            restoreNativeAi(creature);
            return;
        }

        if (!creature.getPersistentData().hasUUID(WORK_OWNER_TAG)) {
            WORKERS.remove(creature.getUUID());
            FAILED_WORKERS.remove(creature.getUUID());
            restoreNativeAi(creature);
            return;
        }
        UUID ownerId = creature.getPersistentData().getUUID(WORK_OWNER_TAG);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            restoreNativeAi(creature);
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(taskId);
        IMaidTask task = id == null ? null : TaskManager.getTaskMap().get(id);
        if (task == null) {
            creature.getPersistentData().remove(TASK_TAG);
            WORKERS.remove(creature.getUUID());
            FAILED_WORKERS.remove(creature.getUUID());
            restoreNativeAi(creature);
            return;
        }

        // The latex body keeps running its own Monster goal AI (wander, transfur,
        // hunt players) while the TLM maid Brain drives it. Suppress that native AI
        // so the two "brains" cannot fight over the same body.
        suppressNativeAi(creature);

        SyntheticMaid maid = WORKERS.compute(creature.getUUID(), (uuid, current) -> {
            if (current == null || current.level() != level || current.getTask() != task) {
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
            BondedCreatureInventory inventory = new BondedCreatureInventory(creature);
            maid.syncFrom(creature, inventory);
            VecPosition before = VecPosition.of(maid);
            try {
                maid.tick();
            } finally {
                maid.syncInventoryTo(creature, inventory);
            }
            VecPosition after = VecPosition.of(maid);
            if (before.distanceSquared(after) > 1.0E-5) {
                creature.moveTo(after.x, after.y, after.z, maid.getYRot(), maid.getXRot());
            }
            FAILED_WORKERS.remove(creature.getUUID());
        } catch (RuntimeException | LinkageError exception) {
            WORKERS.remove(creature.getUUID());
            restoreNativeAi(creature);
            if (FAILED_WORKERS.add(creature.getUUID())) {
                github.com.gengyoubo.CE.changede.LOGGER.warn("Latex maid task {} failed for {}", taskId, creature.getType(), exception);
            }
        }
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

    private static final class SyntheticMaid extends EntityMaid {
        private ServerPlayer workOwner;
        private final ChangedEntity body;

        SyntheticMaid(ServerLevel level, ChangedEntity body, ServerPlayer owner) {
            super(level);
            this.workOwner = owner;
            this.body = body;
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
        }
        void syncFrom(ChangedEntity source, BondedCreatureInventory inventory) {
            syncFrom(source);
            for (int slot = 0; slot < 24; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!ItemStack.matches(getMaidInv().getStackInSlot(slot), stack)) {
                    getMaidInv().setStackInSlot(slot, stack.copy());
                }
            }
            if (!ItemStack.matches(getMainHandItem(), source.getMainHandItem())) {
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, source.getMainHandItem().copy());
            }
            if (!ItemStack.matches(getOffhandItem(), source.getOffhandItem())) {
                setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, source.getOffhandItem().copy());
            }
        }
        void syncInventoryTo(ChangedEntity target, BondedCreatureInventory inventory) {
            for (int slot = 0; slot < 24; slot++) {
                ItemStack stack = getMaidInv().getStackInSlot(slot);
                if (!ItemStack.matches(inventory.getItem(slot), stack)) {
                    inventory.setItem(slot, stack.copy());
                }
            }
            for (int slot = 24; slot < getMaidInv().getSlots(); slot++) {
                ItemStack stack = getMaidInv().getStackInSlot(slot);
                if (stack.isEmpty()) continue;
                ItemStack remainder = inventory.insert(stack.copy());
                getMaidInv().setStackInSlot(slot, ItemStack.EMPTY);
                if (!remainder.isEmpty()) target.spawnAtLocation(remainder);
            }
            if (!ItemStack.matches(target.getMainHandItem(), getMainHandItem())) {
                target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, getMainHandItem().copy());
            }
            if (!ItemStack.matches(target.getOffhandItem(), getOffhandItem())) {
                target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, getOffhandItem().copy());
            }
        }
        @Override public LivingEntity getOwner() { return workOwner; }

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
