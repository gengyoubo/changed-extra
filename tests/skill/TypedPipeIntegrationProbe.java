package github.com.gengyoubo.CE.skill;

import github.com.gengyoubo.CE.BlockEntity.LatexSkillResearchBlockEntity;
import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.Block.BasicEnergyPipeBlock;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.BasePipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.PipeConnectionMode;
import github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity.E.TypedEnergyPipeBlockEntity;
import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.LatexEnergyConverterBlockEntity;
import github.com.gengyoubo.CE.LP.compat.jade.LPEnergyProvider;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.init.CEBlock;
import github.com.gengyoubo.CE.changede;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import snownee.jade.api.BlockAccessor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

/** Opt-in isolated server checks; never compiled into a normal release. */
@Mod.EventBusSubscriber(modid="changede")
public final class TypedPipeIntegrationProbe {
    private static void check(boolean result,String message) { if(!result)throw new AssertionError(message); }
    private static void mode(BasePipeBlockEntity pipe,Direction side,PipeConnectionMode mode) {
        while(pipe.getConnectionMode(side)!=mode)pipe.cycleConnectionMode(side);
    }
    private static <T extends BlockEntity> T put(ServerLevel level,BlockPos pos,Block block,Class<T> type) {
        level.setBlock(pos,block.defaultBlockState(),3);return type.cast(level.getBlockEntity(pos));
    }
    private static void fill(LatexEnergyConverterBlockEntity converter,int amount) {
        var tag=converter.saveWithFullMetadata();tag.putInt("TypedEnergy",amount);converter.load(tag);
    }
    private static CompoundTag jade(BlockEntity entity) {
        var accessor=(BlockAccessor)Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(),new Class[]{BlockAccessor.class},
                (proxy,method,args)->method.getName().equals("getBlockEntity") ? entity : null);
        var tag=new CompoundTag();LPEnergyProvider.INSTANCE.appendServerData(tag,accessor);return tag;
    }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(!Boolean.getBoolean("changede.typedPipeProbe") || !event.getServer().isDedicatedServer())return;
        var server=event.getServer();var level=server.overworld();var clock=server.getWorldData().overworldData();
        try {
            BlockPos origin=new BlockPos(0,100,0);
            var source=put(level,origin,CELPBlock.WHITE_LATEX_POWER_CONVERTER.get(),LatexEnergyConverterBlockEntity.class);
            var pipes=new ArrayList<TypedEnergyPipeBlockEntity>();
            for(int x=1;x<=4;x++)pipes.add(put(level,origin.offset(x,0,0),CELPBlock.WLP_PIPE.get(),TypedEnergyPipeBlockEntity.class));
            var station=put(level,origin.offset(5,0,0),CEBlock.LATEX_SKILL_RESEARCH_TABLE.get(),LatexSkillResearchBlockEntity.class);
            fill(source,10000);
            for(int tick=0;tick<120;tick++) {
                clock.setGameTime(clock.getGameTime()+1);
                for(var pipe:pipes)pipe.tick();station.tick();
                check(source.getOutputStored()+station.getTypedEnergyStored()+pipes.stream().mapToInt(TypedEnergyPipeBlockEntity::getTypedEnergyStored).sum()==10000,"WLP conservation across four segments");
            }
            check(station.getTypedEnergyStored()==10000 && source.getOutputStored()==0,"converter feeds distant research station");
            var snapshot=jade(station);
            check(snapshot.getInt("StoredTypedEnergy")==10000 && snapshot.getInt("CapacityTypedEnergy")==100000
                    && snapshot.getString("TypedEnergyType").equals("WLP") && !snapshot.contains("StoredLP"),"Jade research WLP storage and actual capacity");
            snapshot=jade(pipes.get(0));check(snapshot.getInt("CapacityTypedEnergy")==1000,"Jade pipe capacity");
            snapshot=jade(source);check(snapshot.contains("StoredLP") && snapshot.getString("TypedEnergyType").equals("WLP"),"Jade retains both converter LP and WLP");

            // Disable the final output: both pipe push and station pull must stop.
            fill(source,5000);mode(pipes.get(3),Direction.EAST,PipeConnectionMode.DISABLED);
            for(int tick=0;tick<40;tick++) {
                clock.setGameTime(clock.getGameTime()+1);for(var pipe:pipes)pipe.tick();station.tick();
            }
            check(station.getTypedEnergyStored()==10000,"disabled output also blocks station pull");
            check(source.getOutputStored()+pipes.stream().mapToInt(TypedEnergyPipeBlockEntity::getTypedEnergyStored).sum()==5000,"blocked network retains energy");
            mode(pipes.get(3),Direction.EAST,PipeConnectionMode.INPUT);
            clock.setGameTime(clock.getGameTime()+1);pipes.get(3).tick();station.tick();
            check(station.getTypedEnergyStored()==10000,"input-only face cannot output");
            mode(pipes.get(3),Direction.EAST,PipeConnectionMode.OUTPUT);
            for(int tick=0;tick<80;tick++) {
                clock.setGameTime(clock.getGameTime()+1);for(var pipe:pipes)pipe.tick();station.tick();
            }
            check(station.getTypedEnergyStored()==15000,"resume output after mode change");

            var wlp=put(level,new BlockPos(10,100,0),CELPBlock.WLP_PIPE.get(),TypedEnergyPipeBlockEntity.class);
            var dlp=put(level,new BlockPos(11,100,0),CELPBlock.DLP_PIPE.get(),TypedEnergyPipeBlockEntity.class);
            var lp=put(level,new BlockPos(9,100,0),CELPBlock.BASIC_WIRE.get(),BasePipeBlockEntity.class);
            check(!wlp.canConnect(Direction.EAST) && !wlp.canConnect(Direction.WEST)
                    && !dlp.canConnect(Direction.WEST) && !lp.canConnect(Direction.EAST),"LP WLP and DLP networks do not connect");
            var refreshed=Block.updateFromNeighbourShapes(wlp.getBlockState(),level,wlp.getBlockPos());
            check(!refreshed.getValue(BasicEnergyPipeBlock.EAST) && !refreshed.getValue(BasicEnergyPipeBlock.WEST),"multipart arms respect type isolation");
            check(wlp.receiveTypedEnergy(LatexEnergyType.DLP,500)==0 && wlp.receiveTypedEnergy(LatexEnergyType.LP,500)==0,"wrong energies rejected");
            check(wlp.receiveTypedEnergy(LatexEnergyType.WLP,5000)==1000,"bounded pipe capacity");
            clock.setGameTime(clock.getGameTime()+1);
            check(wlp.extractTypedEnergy(LatexEnergyType.WLP,999,Direction.UP)==100
                    && wlp.extractTypedEnergy(LatexEnergyType.WLP,999,Direction.DOWN)==0,"all outputs share 100 per tick budget");
            clock.setGameTime(clock.getGameTime()+1);
            check(wlp.extractTypedEnergy(LatexEnergyType.WLP,50,Direction.UP)==50,"budget resets next tick");
            mode(wlp,Direction.SOUTH,PipeConnectionMode.DISABLED);
            var saved=wlp.saveWithFullMetadata();wlp.load(saved);
            check(wlp.getTypedEnergyStored()==850 && wlp.getConnectionMode(Direction.SOUTH)==PipeConnectionMode.DISABLED,"cache and port mode NBT round trip");
            check(wlp.receiveTypedEnergy(LatexEnergyType.WLP,100,Direction.SOUTH)==0
                    && wlp.extractTypedEnergy(LatexEnergyType.WLP,100,Direction.SOUTH)==0,"disabled face blocks both directions");
            saved.putInt("TypedEnergy",Integer.MAX_VALUE);wlp.load(saved);check(wlp.getTypedEnergyStored()==1000,"malformed saved capacity bounded");

            var dark=put(level,new BlockPos(20,100,0),CELPBlock.DARK_LATEX_POWER_CONVERTER.get(),LatexEnergyConverterBlockEntity.class);
            var darkPipe=put(level,new BlockPos(21,100,0),CELPBlock.DLP_PIPE.get(),TypedEnergyPipeBlockEntity.class);
            var secondDark=put(level,new BlockPos(22,100,0),CELPBlock.DLP_PIPE.get(),TypedEnergyPipeBlockEntity.class);
            var wrongSink=put(level,new BlockPos(23,100,0),CEBlock.LATEX_SKILL_RESEARCH_TABLE.get(),LatexSkillResearchBlockEntity.class);
            fill(dark,500);
            for(int tick=0;tick<20;tick++) {clock.setGameTime(clock.getGameTime()+1);darkPipe.tick();secondDark.tick();wrongSink.tick();}
            check(dark.getOutputStored()==0 && darkPipe.getTypedEnergyStored()+secondDark.getTypedEnergyStored()==500
                    && secondDark.getTypedEnergyStored()>0 && wrongSink.getTypedEnergyStored()==0,"DLP travels through DLP pipes and cannot enter research table");
            check(jade(secondDark).getString("TypedEnergyType").equals("DLP"),"Jade DLP label");
            changede.LOGGER.info("TYPED_PIPE_INTEGRATION_PASS: multi-segment supply, conservation, port modes, type isolation, throughput, NBT, Jade data");
        } catch(Throwable error) {
            changede.LOGGER.error("TYPED_PIPE_INTEGRATION_FAIL",error);
        } finally {server.halt(false);}
    }
}
