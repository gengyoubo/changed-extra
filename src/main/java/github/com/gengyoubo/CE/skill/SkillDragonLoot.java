package github.com.gengyoubo.CE.skill;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="changede")
@SuppressWarnings("deprecation")
public final class SkillDragonLoot {
    private record Harvest(ServerLevel level,BlockPos pos,BlockState state,long time,boolean natural) { }
    private static final Map<ServerPlayer,Harvest> HARVESTS=new WeakHashMap<>();
    private static final Set<Entity> HUNTED=Collections.newSetFromMap(new WeakHashMap<>());
    private SkillDragonLoot() { }
    public static Item oreProduct(BlockState state) {
        String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if(!id.startsWith("minecraft:"))return null;
        return switch(id.substring(10).replace("deepslate_","")) {
            case "coal_ore"->Items.COAL;case "iron_ore"->Items.RAW_IRON;case "copper_ore"->Items.RAW_COPPER;
            case "gold_ore"->Items.RAW_GOLD;case "redstone_ore"->Items.REDSTONE;case "lapis_ore"->Items.LAPIS_LAZULI;
            case "diamond_ore"->Items.DIAMOND;case "emerald_ore"->Items.EMERALD;case "nether_quartz_ore"->Items.QUARTZ;
            default->null;
        };
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void breaking(BlockEvent.BreakEvent event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        boolean natural=SkillOreProvenance.get(level).consume(event.getPos(),BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).toString());
        if(event.getPlayer() instanceof ServerPlayer player && !(player instanceof FakePlayer) && !player.isCreative()
                && !player.isSpectator() && player.hasCorrectToolForDrops(event.getState()))
            HARVESTS.put(player,new Harvest(level,event.getPos().immutable(),event.getState(),level.getGameTime(),natural));
    }
    private static Harvest harvest(Entity entity,ServerLevel level,BlockPos pos,BlockState state) {
        if(!(entity instanceof ServerPlayer player))return null;
        Harvest h=HARVESTS.get(player);
        return h!=null && h.level==level && h.pos.equals(pos) && h.state.equals(state) && h.time==level.getGameTime()?h:null;
    }
    public static ItemStack tool(ItemStack original,Entity entity,ServerLevel level,BlockPos pos,BlockState state) {
        if(harvest(entity,level,pos,state)==null || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,original)>0)return original;
        int extra=(int)SkillMechanics.value((ServerPlayer)entity,"dragon_fortune");
        if(extra==0)return original;
        int current=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE,original);
        int next=SkillMechanicRules.virtualEnchantment(current,extra);if(next==current)return original;
        ItemStack copy=original.copy();Map<Enchantment,Integer> enchants=new HashMap<>(EnchantmentHelper.getEnchantments(copy));
        enchants.put(Enchantments.BLOCK_FORTUNE,next);EnchantmentHelper.setEnchantments(enchants,copy);return copy;
    }
    public static List<ItemStack> extra(List<ItemStack> original,ItemStack tool,Entity entity,ServerLevel level,BlockPos pos,BlockState state) {
        Harvest harvest=harvest(entity,level,pos,state);
        if(harvest==null)return original;
        HARVESTS.remove((ServerPlayer)entity);
        if(!harvest.natural || !level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
                || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,tool)>0)return original;
        Item product=oreProduct(state);
        if(product==null || level.random.nextDouble()>=SkillMechanics.value((ServerPlayer)entity,"dragon_ore"))return original;
        var result=new ArrayList<>(original);result.add(new ItemStack(product));return result;
    }
    @SubscribeEvent public static void looting(LootingLevelEvent event) {
        var source=event.getDamageSource();
        if(source!=null && source.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer))
            event.setLootingLevel(SkillMechanicRules.virtualEnchantment(event.getLootingLevel(),(int)SkillMechanics.value(player,"dragon_looting")));
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        if(!(event.getSource().getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer
                || !(event.getEntity() instanceof Enemy) || !event.isRecentlyHit() || event.getEntity().isAlliedTo(player)
                || !player.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT) || !HUNTED.add(event.getEntity()))return;
        if(player.getRandom().nextDouble()>=SkillMechanics.value(player,"dragon_hunt"))return;
        int roll=player.getRandom().nextInt(100);Item item=roll<50?Items.IRON_NUGGET:roll<80?Items.GOLD_NUGGET:Items.EMERALD;
        event.getDrops().add(new ItemEntity(player.level(),event.getEntity().getX(),event.getEntity().getY(),event.getEntity().getZ(),new ItemStack(item)));
    }
}
