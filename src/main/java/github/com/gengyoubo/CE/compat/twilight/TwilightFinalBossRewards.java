package github.com.gengyoubo.CE.compat.twilight;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import twilightforest.TFConfig;
import twilightforest.init.TFBlocks;
import twilightforest.init.TFSounds;
import twilightforest.loot.TFLootTables;

/** Extra rewards are rolled once at death and saved through the native death animation. */
public final class TwilightFinalBossRewards {
    private static final String REWARDS = "changede_final_rewards";
    private static final String CLAIMED = "changede_final_rewards_claimed";
    public static final ResourceLocation TABLE = ResourceLocation.parse("changede:chests/twilight_final_boss");
    private TwilightFinalBossRewards() {}
    public static void prepare(LivingDeathEvent event) {
        var boss = event.getEntity();
        if (event.isCanceled() || !(boss.level() instanceof ServerLevel level)
                || !boss.getPersistentData().getBoolean(TwilightFinalBossCompat.MARKER)
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        var data = boss.getPersistentData();
        if (data.contains(REWARDS) || data.getBoolean(CLAIMED)) return;
        var params = TFLootTables.createLootParams(boss, true, event.getSource()).create(LootContextParamSets.ENTITY);
        var table = level.getServer().getLootData().getLootTable(TABLE);
        ListTag stacks = new ListTag();
        for (ItemStack stack : table.getRandomItems(params)) stacks.add(stack.save(new CompoundTag()));
        data.put(REWARDS, stacks);
    }
    public static void removed(EntityLeaveLevelEvent event) {
        Entity boss = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || boss.getRemovalReason() != Entity.RemovalReason.KILLED
                || !boss.getPersistentData().getBoolean(TwilightFinalBossCompat.MARKER)) return;
        deposit(level, boss);
    }
    public static void deposit(ServerLevel level, Entity boss) {
        var data = boss.getPersistentData();
        if (!data.contains(REWARDS, Tag.TAG_LIST) || data.getBoolean(CLAIMED)) return;
        var rewards = data.getList(REWARDS, Tag.TAG_COMPOUND);
        data.putBoolean(CLAIMED, true); data.remove(REWARDS);
        BlockPos home = BlockPos.of(data.getLong(TwilightFinalBossCompat.HOME));
        level.getChunkAt(home);
        Container container = null;
        if (TFConfig.COMMON_CONFIG.bossDropChests.get()) {
            BlockPos chestPos = chestPosition(level, home);
            if (chestPos != null && level.setBlockAndUpdate(chestPos, TFBlocks.DARK_CHEST.get().defaultBlockState())
                    && level.getBlockEntity(chestPos) instanceof Container chest) {
                container = chest;
                level.playSound(null, chestPos, TFSounds.BOSS_CHEST_APPEAR.get(), boss.getSoundSource(), 128, .5f);
            }
        }
        for (int i = 0; i < rewards.size(); i++) {
            ItemStack stack = ItemStack.of(rewards.getCompound(i));
            if (container != null) stack = ItemHandlerHelper.insertItemStacked(new InvWrapper(container), stack, false);
            if (!stack.isEmpty()) {
                var item = new ItemEntity(level, home.getX() + .5, home.getY(), home.getZ() + .5, stack);
                item.setExtendedLifetime(); item.setNoPickUpDelay(); level.addFreshEntity(item);
            }
        }
        if (container != null) container.setChanged();
    }
    private static BlockPos chestPosition(ServerLevel level, BlockPos home) {
        // Prefer Twilight's native home.below() position. Never replace an existing inventory.
        for (int y : new int[]{-1, 0}) {
            for (int radius = 0; radius <= 3; radius++) {
                for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                    BlockPos pos = home.offset(x, y, z);
                    var state = level.getBlockState(pos);
                    if (level.getBlockEntity(pos) == null && (state.isAir() || state.canBeReplaced() || state.is(TFBlocks.VIOLET_CASTLE_RUNE_BRICK.get()))) return pos;
                }
            }
        }
        return null;
    }
}
