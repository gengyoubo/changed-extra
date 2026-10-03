package github.com.gengyoubo.CE.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidDeathEvent;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidTombstoneEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.CE.changede;
import net.ltxprogrammer.changed.entity.beast.AbstractDarkLatexEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.parkabird.changedsynergy.world.inventory.BondedCreatureInventory;
import net.parkabird.changedsynergy.world.inventory.BondedCreatureInventoryMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Checks real TLM death/drop paths in an explicitly enabled disposable server. */
public final class MaidLifecycleRegressionChecks {
    private MaidLifecycleRegressionChecks() {}

    @SuppressWarnings("unchecked")
    public static void verify(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "MaidDeathTest"));
        var bodies = new ArrayList<AbstractDarkLatexEntity>();
        var proxies = new ArrayList<LatexMaidCompat.SyntheticMaid>();
        AtomicInteger proxyDeaths = new AtomicInteger();
        AtomicInteger proxyTombstones = new AtomicInteger();
        AtomicInteger realMaidTombstones = new AtomicInteger();
        AtomicInteger diamondDrops = new AtomicInteger();
        AtomicInteger emeraldDrops = new AtomicInteger();
        AtomicBoolean cancelDeath = new AtomicBoolean();
        Consumer<MaidDeathEvent> maidDeath = death -> {
            if (death.getMaid() instanceof LatexMaidCompat.SyntheticMaid) proxyDeaths.incrementAndGet();
        };
        Consumer<MaidTombstoneEvent> tombstone = death -> {
            if (death.getMaid() instanceof LatexMaidCompat.SyntheticMaid) proxyTombstones.incrementAndGet();
            else realMaidTombstones.incrementAndGet();
        };
        // Changed's inventory uses a player wrapper's drop(), which bypasses LivingDropsEvent.
        Consumer<EntityJoinLevelEvent> drops = join -> {
            if (join.getEntity() instanceof ItemEntity item
                    && bodies.stream().anyMatch(body -> body.distanceToSqr(item) < 16)) {
                if (item.getItem().is(Items.DIAMOND)) diamondDrops.addAndGet(item.getItem().getCount());
                if (item.getItem().is(Items.EMERALD)) emeraldDrops.addAndGet(item.getItem().getCount());
            }
        };
        Consumer<LivingDeathEvent> revival = death -> {
            if (cancelDeath.get() && bodies.contains(death.getEntity())) {
                death.getEntity().setHealth(1);
                death.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(maidDeath);
        MinecraftForge.EVENT_BUS.addListener(tombstone);
        MinecraftForge.EVENT_BUS.addListener(drops);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, LivingDeathEvent.class, revival);
        Map<UUID, LatexMaidCompat.SyntheticMaid> workers = null;
        EntityMaid regular = null;
        try {
            // Exercise the production cache and death/unload/task listeners without an online test account.
            var field = LatexMaidCompat.class.getDeclaredField("WORKERS");
            field.setAccessible(true);
            workers = (Map<UUID, LatexMaidCompat.SyntheticMaid>) field.get(null);
            AbstractDarkLatexEntity body = create(level, player.getUUID(), bodies);
            var inventory = body.getInventory();
            inventory.setItem(2, new ItemStack(Items.DIAMOND, 7));
            var maid = new LatexMaidCompat.SyntheticMaid(level, body, player);
            proxies.add(maid);
            workers.put(body.getUUID(), maid);
            maid.syncFrom(body, inventory);
            var retainedStorage = maid.getMaidInv();
            var retainedCapability = maid.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
            check(retainedStorage.getStackInSlot(2) == inventory.getItem(2), "AI reads the real stack, not an inventory copy");
            check(retainedStorage.insertItem(2, new ItemStack(Items.DIAMOND, 3), true).isEmpty()
                            && retainedStorage.extractItem(2, 2, true).getCount() == 2 && inventory.getItem(2).getCount() == 7,
                    "Simulated work insertion/extraction leaves the real inventory unchanged");
            retainedStorage.insertItem(2, new ItemStack(Items.DIAMOND, 3), false);
            check(inventory.getItem(2).getCount() == 10, "Work insertion immediately updates the real inventory");
            check(retainedStorage.extractItem(2, 3, false).getCount() == 3 && inventory.getItem(2).getCount() == 7,
                    "Work extraction immediately consumes the real inventory");
            maid.getAvailableBackpackInv().insertItem(2, new ItemStack(Items.DIAMOND, 1), false);
            check(inventory.getItem(2).getCount() == 8, "TLM task wrappers share the real inventory");
            retainedStorage.extractItem(2, 1, false);
            // Armor (4), hands (2), then storage: no hidden private maid bag via Forge capabilities.
            retainedCapability.insertItem(8, new ItemStack(Items.DIAMOND, 1), false);
            check(inventory.getItem(2).getCount() == 8, "Forge item capability shares the real inventory");
            retainedStorage.extractItem(2, 1, false);
            body.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            check(maid.getOffhandItem() == body.getOffhandItem(), "Equipment reads borrow the actual body stack");
            maid.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            check(body.getOffhandItem().isEmpty(), "Equipment writes immediately reach the body");
            maid.tick();
            check(body.isAlive() && inventory.getItem(2).getCount() == 7, "TLM AI tick works with borrowed storage");
            check(!level.addFreshEntity(maid), "Work proxy cannot be registered as a physical world entity");
            check(!maid.shouldBeSaved() && !maid.save(new CompoundTag()) && !maid.saveAsPassenger(new CompoundTag()),
                    "Work proxy cannot be saved or restored from a maid backup");
            float health = body.getHealth();
            maid.setHealth(1);
            maid.heal(100);
            check(body.getHealth() == health && maid.getHealth() == maid.getMaxHealth(),
                    "Proxy setters and TLM healing cannot create independent health");
            maid.hurt(body.damageSources().generic(), 3);
            check(body.getHealth() < health, "Proxy damage reaches the latex body");
            check(Math.abs(maid.getHealth() / maid.getMaxHealth() - body.getHealth() / body.getMaxHealth()) < 0.0001F,
                    "Proxy health follows the real body");
            check(inventory.getItem(2).getCount() == 7 && diamondDrops.get() == 0, "Nonfatal proxy damage cannot duplicate loot");
            maid.getHideInv().insertItem(0, new ItemStack(Items.EMERALD, 3), false);
            maid.getTaskInv().insertItem(1, new ItemStack(Items.EMERALD, 4), false);
            check(body.getPersistentData().getCompound("changede_maid_work_buffers").getCompound("hand")
                            .getCompound("0").getByte("Count") == 3,
                    "Temporary work items are saved on the body, never on the maid proxy");

            cancelDeath.set(true);
            maid.die(body.damageSources().genericKill());
            check(body.isAlive() && !maid.isRemoved() && workers.get(body.getUUID()) == maid,
                    "Canceled body death keeps one live worker and one real body");
            check(inventory.getItem(2).getCount() == 7 && proxyDeaths.get() == 0 && proxyTombstones.get() == 0,
                    "Revival never creates a maid tombstone or a second inventory");
            check(maid.getHideInv().getStackInSlot(0).getCount() == 3 && emeraldDrops.get() == 0,
                    "Canceled death keeps temporary work items with the living body");
            cancelDeath.set(false);
            body.setHealth(body.getMaxHealth());
            // Treat the next fatal hit as a later hit, outside the canceled hit's vanilla cooldown.
            body.invulnerableTime = 0;
            // Newly gathered items are already owned by the real body before fatal damage.
            maid.getMaidInv().setStackInSlot(2, new ItemStack(Items.DIAMOND, 10));
            check(inventory.getItem(2).getCount() == 10, "Gathered items need no copied-inventory replay");
            maid.die(body.damageSources().genericKill());
            check(!body.isAlive() && maid.isRemoved() && !workers.containsKey(body.getUUID()),
                    "Fatal proxy damage kills the real body and removes its cached worker (health="
                            + body.getHealth() + ", removed=" + maid.isRemoved() + ", cached=" + workers.containsKey(body.getUUID()) + ")");
            check(diamondDrops.get() == 10 && inventory.getItem(2).isEmpty(),
                    "Body death drops exactly the original plus gathered items once (drops="
                            + diamondDrops.get() + ", remaining=" + inventory.getItem(2).getCount() + ")");
            check(maid.getMaidInv().getStackInSlot(2).isEmpty(), "Dead workers expose no usable inventory");
            check(emeraldDrops.get() == 7 && !body.getPersistentData().contains("changede_maid_work_buffers"),
                    "Real death releases temporary work items once and consumes their saved state");
            check(retainedStorage.extractItem(2, 64, false).isEmpty()
                            && retainedCapability.insertItem(8, new ItemStack(Items.DIAMOND, 64), false).getCount() == 64,
                    "Retained task/capability handles become inert after death");
            maid.getMaidInv().setStackInSlot(2, new ItemStack(Items.DIAMOND, 64));
            maid.syncInventoryTo(body, inventory);
            check(inventory.getItem(2).isEmpty(), "Dead-body final synchronization cannot restore dropped items");
            maid.die(body.damageSources().genericKill());
            maid.remove(Entity.RemovalReason.KILLED);
            maid.dropEquipment();
            check(diamondDrops.get() == 10 && proxyDeaths.get() == 0 && proxyTombstones.get() == 0,
                    "Repeated death/removal/drop calls cannot create extra drops or a tombstone");

            body = create(level, player.getUUID(), bodies);
            inventory = body.getInventory();
            inventory.setItem(2, new ItemStack(Items.DIAMOND, 5));
            maid = new LatexMaidCompat.SyntheticMaid(level, body, player);
            proxies.add(maid);
            workers.put(body.getUUID(), maid);
            maid.syncFrom(body, inventory);
            body.hurt(body.damageSources().genericKill(), Float.MAX_VALUE);
            check(maid.isRemoved() && !workers.containsKey(body.getUUID()) && maid.getMaidInv().getStackInSlot(2).isEmpty(),
                    "Direct latex death also retires its worker before drops");
            check(diamondDrops.get() == 15, "Direct body death drops its inventory only once");

            body = create(level, player.getUUID(), bodies);
            inventory = body.getInventory();
            inventory.setItem(2, new ItemStack(Items.DIAMOND, 9));
            maid = new LatexMaidCompat.SyntheticMaid(level, body, player);
            proxies.add(maid);
            workers.put(body.getUUID(), maid);
            maid.syncFrom(body, inventory);
            maid.getHideInv().insertItem(0, new ItemStack(Items.EMERALD, 2), false);
            body.setNoAi(true);
            MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(body));
            check(maid.isRemoved() && !workers.containsKey(body.getUUID()) && !body.isNoAi(),
                    "Stopping work retires the proxy and restores native AI");
            check(body.isAlive() && inventory.getItem(2).getCount() == 9 && diamondDrops.get() == 15,
                    "Retirement keeps the real inventory and drops no copies");
            check(count(inventory, Items.EMERALD) == 2 && emeraldDrops.get() == 7,
                    "Stopping work returns temporary items to the real backpack without extra drops");
            maid.syncInventoryTo(body, inventory);
            check(inventory.getItem(2).getCount() == 9, "Retired workers cannot overwrite a living body's inventory");

            maid = new LatexMaidCompat.SyntheticMaid(level, body, player);
            proxies.add(maid);
            workers.put(body.getUUID(), maid);
            maid.syncFrom(body, inventory);
            maid.getHideInv().insertItem(0, new ItemStack(Items.EMERALD, 3), false);
            body.discard();
            check(maid.isRemoved() && !workers.containsKey(body.getUUID()), "World removal immediately retires the worker");
            CompoundTag unloaded = body.getPersistentData().getCompound("changede_maid_work_buffers").copy();
            check(!unloaded.isEmpty() && emeraldDrops.get() == 7, "Unloading preserves temporary items in body NBT");
            body = create(level, player.getUUID(), bodies);
            body.getPersistentData().put("changede_maid_work_buffers", unloaded);
            maid = new LatexMaidCompat.SyntheticMaid(level, body, player);
            proxies.add(maid);
            check(count(body.getInventory(), Items.EMERALD) == 3
                            && !body.getPersistentData().contains("changede_maid_work_buffers"),
                    "A reloaded body's interrupted work items return once to its own storage");
            var storage = new BondedCreatureInventory(body);
            storage.setItem(2, new ItemStack(Items.DIAMOND, 4));
            level.players().add(player);
            player.containerMenu = new BondedCreatureInventoryMenu(1, player.getInventory(), body, storage);
            maid.syncFrom(body, github.com.gengyoubo.CE.compat.synergy.CreatureInventoryAccess.resolve(body));
            maid.getMaidInv().insertItem(2, new ItemStack(Items.DIAMOND), false);
            check(player.containerMenu.getSlot(43).getItem().getCount() == 5,
                    "Work insertion immediately appears in the open CS screen");
            storage.setItem(4, new ItemStack(Items.EMERALD));
            maid.syncInventoryTo(body, storage);
            check(new BondedCreatureInventory(body).getItem(2).getCount() == 5,
                    "Later GUI edits cannot overwrite work with a stale inventory copy");
            player.containerMenu = player.inventoryMenu;
            level.players().remove(player);
            maid.syncFrom(body, github.com.gengyoubo.CE.compat.synergy.CreatureInventoryAccess.resolve(body));
            maid.getMaidInv().extractItem(2, 2, false);
            check(new BondedCreatureInventory(body).getItem(2).getCount() == 3,
                    "Work extraction stays persistent after closing the CS screen");
            maid.getHideInv().insertItem(0, new ItemStack(Items.EMERALD, 5), false);
            workers.put(body.getUUID(), maid);
            var replacement = create(level, player.getUUID(), bodies);
            LatexMaidCompat.transferWorkState(body, replacement);
            check(maid.isRemoved() && !body.getPersistentData().contains("changede_maid_work_buffers")
                            && replacement.getPersistentData().contains("changede_maid_work_buffers"),
                    "Species replacement moves temporary item ownership and retires the previous worker");
            maid = new LatexMaidCompat.SyntheticMaid(level, replacement, player);
            proxies.add(maid);
            check(count(replacement.getInventory(), Items.EMERALD) == 5,
                    "The replacement body receives interrupted work items exactly once");
            check(proxyDeaths.get() == 0 && proxyTombstones.get() == 0, "Work lifecycle never enters TLM's death/tombstone pipeline");

            regular = new EntityMaid(level);
            regular.setPos(0, 96, 0);
            regular.setTame(true);
            regular.setOwnerUUID(player.getUUID());
            regular.getMaidInv().setStackInSlot(0, new ItemStack(Items.EMERALD, 2));
            regular.hurt(regular.damageSources().genericKill(), Float.MAX_VALUE);
            check(realMaidTombstones.get() == 1, "Ordinary TLM maids retain their normal tombstone behavior");
            MaidEnvironmentRegressionChecks.verify(level, player);
            changede.LOGGER.info("MAID LIFECYCLE REGRESSION CHECKS PASSED");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not inspect the work proxy cache", exception);
        } finally {
            player.containerMenu = player.inventoryMenu;
            level.players().remove(player);
            cancelDeath.set(false);
            proxies.forEach(LatexMaidCompat.SyntheticMaid::retire);
            if (workers != null) for (var body : bodies) workers.remove(body.getUUID());
            bodies.forEach(Entity::discard);
            if (regular != null) regular.discard();
            MinecraftForge.EVENT_BUS.unregister(maidDeath);
            MinecraftForge.EVENT_BUS.unregister(tombstone);
            MinecraftForge.EVENT_BUS.unregister(drops);
            MinecraftForge.EVENT_BUS.unregister(revival);
            event.getServer().halt(false);
        }
    }

    private static AbstractDarkLatexEntity create(ServerLevel level, UUID owner, ArrayList<AbstractDarkLatexEntity> bodies) {
        AbstractDarkLatexEntity body = ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
        if (body == null) throw new AssertionError("Could not create dark latex wolf");
        body.setPos(0, 96, 0);
        body.setOwnerUUID(owner);
        body.setTame(true);
        level.addFreshEntity(body);
        bodies.add(body);
        return body;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        changede.LOGGER.info("Maid lifecycle PASS: {}", message);
    }

    private static int count(net.minecraft.world.Container inventory, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < 24; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }
}
