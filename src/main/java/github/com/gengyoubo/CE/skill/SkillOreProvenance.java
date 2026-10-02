package github.com.gengyoubo.CE.skill;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Only ores observed in a newly generated chunk qualify. Existing worlds remain conservative. */
@Mod.EventBusSubscriber(modid="changede")
public final class SkillOreProvenance extends SavedData {
    private final Map<Long,String> ores=new HashMap<>();
    public static SkillOreProvenance get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SkillOreProvenance::load,SkillOreProvenance::new,"changede_skill_natural_ores");
    }
    private static SkillOreProvenance load(CompoundTag tag) {
        var data=new SkillOreProvenance();
        for(Tag entry:tag.getList("ores",Tag.TAG_COMPOUND)) {
            var ore=(CompoundTag)entry;data.ores.put(ore.getLong("pos"),ore.getString("block"));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list=new ListTag();
        ores.forEach((pos,block)->{var ore=new CompoundTag();ore.putLong("pos",pos);ore.putString("block",block);list.add(ore);});
        tag.put("ores",list);return tag;
    }
    public boolean consume(BlockPos pos,String block) {
        String original=ores.remove(pos.asLong());if(original!=null)setDirty();return block.equals(original);
    }
    private void remove(BlockPos pos) {if(ores.remove(pos.asLong())!=null)setDirty();}
    @SubscribeEvent public static void generated(ChunkEvent.Load event) {
        if(!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level))return;
        // Snapshot before handing this chunk to players; SavedData is mutated only on the server thread.
        Map<Long,String> snapshot=new HashMap<>();var chunk=event.getChunk();var origin=chunk.getPos().getWorldPosition();
        for(int section=0;section<chunk.getSections().length;section++) {
            var blocks=chunk.getSections()[section];if(blocks.hasOnlyAir())continue;
            int yBase=chunk.getMinBuildHeight()+section*16;
            for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
                var state=blocks.getBlockState(x,y,z);
                if(SkillDragonLoot.oreProduct(state)!=null)snapshot.put(new BlockPos(origin.getX()+x,yBase+y,origin.getZ()+z).asLong(),net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            }
        }
        if(!snapshot.isEmpty())level.getServer().execute(()->{var data=get(level);data.ores.putAll(snapshot);data.setDirty();});
    }
    @SubscribeEvent public static void placed(BlockEvent.EntityPlaceEvent event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var data=get(level);data.remove(event.getPos());
        if(event instanceof BlockEvent.EntityMultiPlaceEvent multi)
            multi.getReplacedBlockSnapshots().forEach(s->data.remove(s.getPos()));
    }
    @SubscribeEvent public static void exploded(ExplosionEvent.Detonate event) {
        if(event.getLevel() instanceof ServerLevel level)event.getAffectedBlocks().forEach(get(level)::remove);
    }
}
