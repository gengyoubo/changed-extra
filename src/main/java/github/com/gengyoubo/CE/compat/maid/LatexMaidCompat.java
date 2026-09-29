package github.com.gengyoubo.CE.compat.maid;

import github.com.gengyoubo.CE.compat.synergy.ChangedSynergyFeedApi;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.MenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import net.parkabird.changedsynergy.world.inventory.BondedCreatureInventoryMenu;
import net.parkabird.changedsynergy.world.inventory.BondedInventoryService;
import net.parkabird.changedsynergy.world.inventory.BondedLatexMenu;
import net.parkabird.changedsynergy.event.LatexSocialEvents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Optional TLM bridge. This class is only loaded when both companion mods are present. */
public final class LatexMaidCompat {
    private static final String TASK_TAG = "changede_maid_work_task";
    private static final String WORK_OWNER_TAG = "changede_maid_work_owner";
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "changede");
    static final RegistryObject<MenuType<MaidWorkMenu>> WORK_MENU = MENUS.register("latex_maid_work", () -> IForgeMenuType.create(MaidWorkMenu::new));
    private static final Map<UUID, SyntheticMaid> WORKERS = new ConcurrentHashMap<>();

    private LatexMaidCompat() {}

    public static void initialize(IEventBus modBus) {
        MENUS.register(modBus);
        modBus.addListener(LatexMaidCompat::clientSetup);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingEvent.LivingTickEvent.class, LatexMaidCompat::onLivingTick);
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
        String taskId = creature.getPersistentData().getString(TASK_TAG);
        if (taskId.isEmpty()) {
            WORKERS.remove(creature.getUUID());
            return;
        }

        if (!creature.getPersistentData().hasUUID(WORK_OWNER_TAG)) {
            WORKERS.remove(creature.getUUID());
            return;
        }
        UUID ownerId = creature.getPersistentData().getUUID(WORK_OWNER_TAG);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.level() != level || owner.distanceToSqr(creature) > 64 * 64
                || !ChangedSynergyFeedApi.hasMaximumFamiliarity(creature, owner)) return;

        ResourceLocation id = ResourceLocation.tryParse(taskId);
        IMaidTask task = id == null ? null : TaskManager.getTaskMap().get(id);
        if (task == null) {
            creature.getPersistentData().remove(TASK_TAG);
            WORKERS.remove(creature.getUUID());
            return;
        }

        SyntheticMaid maid = WORKERS.compute(creature.getUUID(), (uuid, current) -> {
            if (current == null || current.level() != level || current.getTask() != task) {
                try {
                    current = new SyntheticMaid(level, creature, owner);
                    current.setTask(task);
                } catch (RuntimeException | LinkageError exception) {
                    return null;
                }
            }
            current.setWorkOwner(owner);
            return current;
        });
        if (maid == null) return;

        // Keep the maid-shaped worker colocated with its Changed body; movement issued by
        // maid task behaviors is mirrored back to the real creature below.
        maid.syncFrom(creature);
        VecPosition before = VecPosition.of(maid);
        try {
            maid.getBrain().tick(level, maid);
            maid.getNavigation().tick();
        } catch (RuntimeException | LinkageError exception) {
            WORKERS.remove(creature.getUUID());
            github.com.gengyoubo.CE.changede.LOGGER.warn("Latex maid task {} failed for {}", taskId, creature.getType(), exception);
            return;
        }
        VecPosition after = VecPosition.of(maid);
        if (before.distanceSquared(after) > 1.0E-5) {
            creature.teleportTo(after.x, after.y, after.z);
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
        private ChangedEntity body;
        private ServerPlayer workOwner;

        SyntheticMaid(ServerLevel level, ChangedEntity body, ServerPlayer owner) {
            super(level);
            this.body = body;
            this.workOwner = owner;
            syncFrom(body);
            setFavorability(384);
            setPickup(false);
            setHomeModeEnable(false);
        }

        void setWorkOwner(ServerPlayer owner) { workOwner = owner; }
        void syncFrom(ChangedEntity source) {
            body = source;
            setPos(source.getX(), source.getY(), source.getZ());
            setYRot(source.getYRot());
            setXRot(source.getXRot());
        }
        @Override public LivingEntity getOwner() { return workOwner; }
    }
}
