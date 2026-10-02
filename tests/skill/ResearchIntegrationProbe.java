package github.com.gengyoubo.CE.skill;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.CE.BlockEntity.LatexSkillResearchBlockEntity;
import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.init.CEBlock;
import github.com.gengyoubo.CE.changede;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

/** Inject only into an isolated test server with tests/skill/research-smoke.init.gradle. */
@Mod.EventBusSubscriber(modid="changede")
public final class ResearchIntegrationProbe {
    private static void check(boolean result,String message) { if(!result)throw new AssertionError(message); }
    @SubscribeEvent public static void started(ServerStartedEvent event) {
        var server=event.getServer();var level=server.overworld();
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.fromString("6bc0c52e-7b26-4f8c-ae5b-d71ea19861db"),"ResearchProbe"));
        try {
            ResourceLocation form=ResourceLocation.parse("changed:form_latex_blue_dragon"),id=ResourceLocation.parse("changede:dragon_core");
            var instance=ProcessTransfur.setPlayerTransfurVariantNamed(player,form);
            check(instance!=null && form.equals(LatexSkills.form(player)),"set test Form");
            // Seed the full common path as learned, preserving the production active-parent checks.
            CompoundTag tree=new CompoundTag();for(var n:LatexSkillTrees.all())if(n.scope().equals("global"))tree.putBoolean(n.id().toString(),true);
            CompoundTag learned=new CompoundTag();learned.put("changede:common",tree);player.getPersistentData().put("changede_latex_skills",learned);
            BlockPos pos=new BlockPos(0,100,0),otherPos=new BlockPos(2,100,0);
            level.setBlockAndUpdate(pos,CEBlock.LATEX_SKILL_RESEARCH_TABLE.get().defaultBlockState());
            level.setBlockAndUpdate(otherPos,CEBlock.LATEX_SKILL_RESEARCH_TABLE.get().defaultBlockState());
            var station=(LatexSkillResearchBlockEntity)level.getBlockEntity(pos);
            var other=(LatexSkillResearchBlockEntity)level.getBlockEntity(otherPos);
            player.setPos(0.5,100,0.5);player.containerMenu=new LatexSkillResearchMenu(21,player.getInventory(),pos);
            var definition=SkillResearchDefinitions.get(id);check(definition!=null && definition.wlpPerSecond()==500,"loaded research costs");
            player.getInventory().clearContent();
            SkillResearchAccounts.start(player,form,id,false);check(SkillResearchAccounts.job(player,id).isEmpty(),"missing materials cannot start");
            for(var m:definition.materials())player.getInventory().add(new ItemStack(m.item(),m.count()));
            int before=player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum();
            SkillResearchAccounts.start(player,ResourceLocation.parse("changed:wrong_form"),id,false);
            check(player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum()==before,"stale Form cannot charge materials");
            SkillResearchAccounts.start(player,form,id,false);
            check(!SkillResearchAccounts.job(player,id).isEmpty() && player.getInventory().isEmpty(),"start charges materials exactly once");
            SkillResearchAccounts.start(player,form,id,false);check(SkillResearchAccounts.job(player,id).getInt("progress")==0,"duplicate start no progress");
            check(station.receiveTypedEnergy(LatexEnergyType.DLP,500)==0 && station.receiveTypedEnergy(LatexEnergyType.LP,500)==0,"only WLP accepted");
            station.receiveTypedEnergy(LatexEnergyType.WLP,499);SkillResearchAccounts.paidSecond(player,id,station);
            check(station.getTypedEnergyStored()==499 && SkillResearchAccounts.job(player,id).getInt("progress")==0,"insufficient WLP costs nothing");
            station.receiveTypedEnergy(LatexEnergyType.WLP,1);SkillResearchAccounts.paidSecond(player,id,station);
            check(station.getTypedEnergyStored()==0 && SkillResearchAccounts.job(player,id).getInt("progress")==20
                    && SkillResearchAccounts.job(player,id).getLong("consumed")==500,"one paid second advances 20 ticks");
            SkillResearchAccounts.pause(player,id);station.receiveTypedEnergy(LatexEnergyType.WLP,1000);
            SkillResearchAccounts.paidSecond(player,id,station);check(station.getTypedEnergyStored()==1000,"manual pause does not charge");
            // Save/reload account and block, then bind a different station without materials.
            CompoundTag saved=player.getPersistentData().copy();player.getPersistentData().put("changede_skill_research",saved.getCompound("changede_skill_research").copy());
            var savedStation=station.saveWithFullMetadata();station.load(savedStation);check(station.getTypedEnergyStored()==1000,"machine storage persistence");
            player.containerMenu=new LatexSkillResearchMenu(22,player.getInventory(),otherPos);
            SkillResearchAccounts.start(player,form,id,true);check(SkillResearchAccounts.bound(player,id,other),"resume at replacement station");
            SkillResearchAccounts.paidSecond(player,id,station);check(station.getTypedEnergyStored()==1000,"old station cannot advance rebound task");
            other.receiveTypedEnergy(LatexEnergyType.WLP,1000);
            for(int i=0;i<40;i++)other.tick();check(SkillResearchAccounts.job(player,id).getInt("progress")==20,"offline owner never advances");
            // Temporarily appear online to exercise the actual 20-tick machine loop.
            var onlineField=java.util.Arrays.stream(net.minecraft.server.players.PlayerList.class.getDeclaredFields())
                    .filter(f->f.getGenericType().getTypeName().contains("java.util.UUID") && java.util.Map.class.isAssignableFrom(f.getType()))
                    .findFirst().orElseThrow();onlineField.setAccessible(true);
            @SuppressWarnings("unchecked") var online=(java.util.Map<UUID,net.minecraft.server.level.ServerPlayer>)onlineField.get(server.getPlayerList());
            online.put(player.getUUID(),player);
            try {
                for(int i=0;i<19;i++)other.tick();check(SkillResearchAccounts.job(player,id).getInt("progress")==20,"no tick-fraction charge");
                other.tick();check(SkillResearchAccounts.job(player,id).getInt("progress")==40 && other.getTypedEnergyStored()==500,"machine charges once per second");
            } finally { online.remove(player.getUUID()); }
            var clone=FakePlayerFactory.get(level,new GameProfile(UUID.fromString("c7173e81-a661-4bba-b0a3-d8181dedf99b"),"ResearchClone"));
            SkillResearchAccounts.clone(new PlayerEvent.Clone(clone,player,true));
            check(SkillResearchAccounts.job(clone,id).getInt("progress")==40,"death clone keeps progress");
            // Force the saved job to one second before completion to test the final atomic payment.
            CompoundTag root=player.getPersistentData().getCompound("changede_skill_research");
            var job=root.getCompound("jobs").getCompound(id.toString());job.putInt("progress",job.getInt("duration")-20);
            SkillResearchAccounts.paidSecond(player,id,other);check(SkillResearchAccounts.researched(player,id),"completion grants qualification");
            check(SkillResearchAccounts.active(player).isEmpty(),"completion frees active slot");
            var node=LatexSkillTrees.all().stream().filter(n->n.id().equals(id)).findFirst().orElseThrow();
            check(!LatexSkills.unlocked(player).contains(id),"completion does not learn the skill or bypass XP");
            player.containerMenu=player.inventoryMenu;
            check(LatexSkills.requirements(player,node,LatexSkills.unlocked(player),java.util.Set.of())
                    .stream().filter(r->r.type().equals("changede:research_table")).allMatch(r->r.current()==1),"qualification works away from station");
            changede.LOGGER.info("RESEARCH_INTEGRATION_PASS: materials, Form guards, WLP typing, exact second payment, pause, persistence, replacement binding, offline, death clone and permanent qualification");
        } catch(Throwable e) { changede.LOGGER.error("RESEARCH_INTEGRATION_FAIL",e); }
        finally { server.halt(false); }
    }
}
