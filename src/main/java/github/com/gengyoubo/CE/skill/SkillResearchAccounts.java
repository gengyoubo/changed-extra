package github.com.gengyoubo.CE.skill;

import github.com.gengyoubo.CE.BlockEntity.LatexSkillResearchBlockEntity;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Player-owned, persistent work. A station binding grants one machine permission to advance it. */
@Mod.EventBusSubscriber(modid="changede")
public final class SkillResearchAccounts {
    private static final String TAG="changede_skill_research";
    private SkillResearchAccounts() { }
    private static CompoundTag account(Player player) { return player.getPersistentData().getCompound(TAG); }
    public static CompoundTag job(Player player,ResourceLocation id) { return account(player).getCompound("jobs").getCompound(id.toString()); }
    public static String active(Player player) { return account(player).getString("active"); }
    public static boolean researched(Player player,ResourceLocation id) {
        // Already learned keys are grandfathered in, including saves made before research existed.
        return job(player,id).getBoolean("completed") || LatexSkills.unlocked(player).contains(id);
    }
    private static void save(Player player,ResourceLocation id,CompoundTag job,String active) {
        CompoundTag root=account(player),jobs=root.getCompound("jobs");jobs.put(id.toString(),job);
        root.put("jobs",jobs);root.putString("active",active);player.getPersistentData().put(TAG,root);
    }
    public static boolean bound(Player player,ResourceLocation id,LatexSkillResearchBlockEntity station) {
        CompoundTag job=job(player,id);
        return !job.isEmpty() && !job.getBoolean("completed") && !job.getBoolean("paused")
                && active(player).equals(id.toString()) && station.getLevel()!=null
                && job.getString("dimension").equals(station.getLevel().dimension().location().toString())
                && job.getLong("station")==station.getBlockPos().asLong();
    }
    private static boolean eligible(Player player,SkillNode node) {
        Set<ResourceLocation> enabled=new HashSet<>();LatexSkills.active(player).forEach(n->enabled.add(n.id()));
        return player.isAlive() && !player.isSpectator() && LatexSkillTrees.applicable(player,node)
                && enabled.containsAll(node.parents());
    }
    private static int count(Player player,Item item) {
        return player.getInventory().items.stream().filter(s->s.is(item)).mapToInt(s->s.getCount()).sum();
    }
    private static boolean materials(Player player,SkillResearchDefinitions.Definition definition) {
        return player.isCreative() || definition.materials().stream().allMatch(m->count(player,m.item())>=m.count());
    }
    private static void consumeMaterials(Player player,SkillResearchDefinitions.Definition definition) {
        if(player.isCreative())return;
        for(var material:definition.materials()) {
            int remaining=material.count();
            for(var stack:player.getInventory().items) if(stack.is(material.item())) {
                int consumed=Math.min(remaining,stack.getCount());stack.shrink(consumed);remaining-=consumed;
                if(remaining==0)break;
            }
        }
        player.getInventory().setChanged();
    }
    /** All checks and mutations run on the server thread, so material input is atomic. */
    public static void start(ServerPlayer player,ResourceLocation expectedForm,ResourceLocation id,boolean resume) {
        if(!Objects.equals(LatexSkills.form(player),expectedForm) || !LatexSkillResearchMenu.isResearching(player)
                || !player.isAlive() || player.isSpectator())return;
        var station=((LatexSkillResearchMenu)player.containerMenu).station();
        var node=LatexSkillTrees.all().stream().filter(n->n.id().equals(id)).findFirst().orElse(null);
        var definition=SkillResearchDefinitions.get(id);
        if(station==null || node==null || !node.research().requiresStation() || definition==null
                || researched(player,id) || (definition.durationTicks()>0 && !station.canBind(player)))return;
        CompoundTag job=job(player,id);String current=active(player);
        if(definition.durationTicks()>0 && !current.isEmpty() && !current.equals(id.toString()))return;
        if(job.isEmpty()) {
            if(resume || !eligible(player,node) || !materials(player,definition))return;
            consumeMaterials(player,definition);
            job.putInt("duration",definition.durationTicks());job.putInt("rate",definition.wlpPerSecond());
            job.putInt("progress",0);job.putLong("consumed",0);
            if(definition.durationTicks()==0) {
                job.putBoolean("completed",true);save(player,id,job,current);return;
            }
        } else if(!resume)return;
        job.putBoolean("paused",false);
        job.putString("dimension",station.getLevel().dimension().location().toString());
        job.putLong("station",station.getBlockPos().asLong());
        save(player,id,job,id.toString());station.bind(player.getUUID(),id);
    }
    public static void pause(ServerPlayer player,ResourceLocation id) {
        if(!LatexSkillResearchMenu.isResearching(player) || !active(player).equals(id.toString()))return;
        CompoundTag job=job(player,id);if(job.isEmpty() || job.getBoolean("completed"))return;
        job.putBoolean("paused",true);save(player,id,job,"");
    }
    public static void paidSecond(ServerPlayer player,ResourceLocation id,LatexSkillResearchBlockEntity station) {
        if(!bound(player,id,station) || !player.isAlive() || player.isSpectator() || SkillResearchDefinitions.get(id)==null)return;
        CompoundTag job=job(player,id);
        var step=ResearchClock.second(job.getInt("progress"),job.getInt("duration"),job.getInt("rate"),station.getTypedEnergyStored(),true);
        if(!step.advanced())return;
        station.consume(step.energySpent());job.putInt("progress",step.progressTicks());
        job.putLong("consumed",job.getLong("consumed")+step.energySpent());job.putBoolean("completed",step.completed());
        save(player,id,job,step.completed() ? "" : id.toString());
    }
    private static String status(Player player,ResourceLocation id,CompoundTag job) {
        if(researched(player,id))return "completed";
        if(job.isEmpty())return "unstarted";
        if(job.getBoolean("paused"))return "paused";
        if(SkillResearchDefinitions.get(id)==null)return "unavailable";
        if(!(player instanceof ServerPlayer serverPlayer))return "paused";
        var key=ResourceLocation.tryParse(job.getString("dimension"));
        var level=key==null ? null : serverPlayer.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,key));
        var pos=net.minecraft.core.BlockPos.of(job.getLong("station"));
        if(level==null || !level.hasChunkAt(pos))return "station_missing";
        if(!(level.getBlockEntity(pos) instanceof LatexSkillResearchBlockEntity station) || !station.owns(player,id))return "station_missing";
        if(!player.isAlive() || player.isSpectator())return "paused";
        return station.getTypedEnergyStored()<job.getInt("rate") ? "no_wlp" : "running";
    }
    public static CompoundTag snapshot(Player player,SkillNode node) {
        var id=node.id();var definition=SkillResearchDefinitions.get(id);var job=job(player,id);CompoundTag result=new CompoundTag();
        boolean complete=researched(player,id);result.putBoolean("completed",complete);
        result.putBoolean("started",!job.isEmpty());result.putString("status",status(player,id,job));
        if(definition==null) { result.putString("status",complete ? "completed" : "unavailable");return result; }
        int duration=job.isEmpty() ? definition.durationTicks() : job.getInt("duration");
        result.putInt("duration",duration);result.putInt("rate",job.isEmpty() ? definition.wlpPerSecond() : job.getInt("rate"));
        result.putInt("progress",complete ? duration : job.getInt("progress"));result.putLong("consumed",job.getLong("consumed"));
        result.putString("tier",definition.tier());
        var station=player.containerMenu instanceof LatexSkillResearchMenu menu && menu.stillValid(player) ? menu.station() : null;
        boolean slot=active(player).isEmpty() || active(player).equals(id.toString());
        boolean available=station!=null && (definition.durationTicks()==0 || station.canBind(player) && slot)
                && player.isAlive() && !player.isSpectator();
        result.putBoolean("can_start",!complete && job.isEmpty() && available && eligible(player,node) && materials(player,definition));
        result.putBoolean("can_resume",!complete && !job.isEmpty() && available && !"running".equals(result.getString("status")));
        ListTag inputs=new ListTag();
        for(var material:definition.materials()) {
            CompoundTag input=new CompoundTag();input.putString("title",material.item().getDescriptionId());
            input.putInt("required",material.count());input.putInt("current",count(player,material.item()));inputs.add(input);
        }
        result.put("materials",inputs);return result;
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if(event.getOriginal().getPersistentData().contains(TAG))
            event.getEntity().getPersistentData().put(TAG,event.getOriginal().getPersistentData().getCompound(TAG).copy());
    }
}
