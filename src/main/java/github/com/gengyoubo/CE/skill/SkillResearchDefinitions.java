package github.com.gengyoubo.CE.skill;

import com.google.gson.*;
import github.com.gengyoubo.CE.changede;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Research costs are separate from the XP needed to learn a qualified skill. */
@Mod.EventBusSubscriber(modid="changede")
public final class SkillResearchDefinitions extends SimpleJsonResourceReloadListener {
    public record Material(Item item, int count) { }
    public record Definition(ResourceLocation id, int durationTicks, int wlpPerSecond, String tier, List<Material> materials) { }
    private static Map<ResourceLocation,Definition> definitions=Map.of();
    private SkillResearchDefinitions() { super(new Gson(),"skill_research"); }
    @SubscribeEvent public static void reload(AddReloadListenerEvent event) { event.addListener(new SkillResearchDefinitions()); }
    public static Definition get(ResourceLocation id) { return definitions.get(id); }
    @Override protected void apply(Map<ResourceLocation,JsonElement> resources,ResourceManager manager,ProfilerFiller profiler) {
        Map<ResourceLocation,Definition> next=new HashMap<>();
        try {
            for(var entry:resources.entrySet()) {
                JsonObject json=entry.getValue().getAsJsonObject();
                int ticks=GsonHelper.getAsInt(json,"duration_ticks"), rate=GsonHelper.getAsInt(json,"wlp_per_second");
                String tier=GsonHelper.getAsString(json,"tier");
                if(ticks<0 || ticks>20*60*60*24 || ticks%20!=0 || rate<0 || rate>100_000
                        || (ticks>0 && rate==0) || (ticks==0 && rate!=0)
                        || !Set.of("free","basic","intermediate","advanced","rare","core").contains(tier))
                    throw new IllegalArgumentException("Invalid research duration/rate/tier: "+entry.getKey());
                List<Material> materials=new ArrayList<>();Set<Item> seen=new HashSet<>();
                for(JsonElement element:GsonHelper.getAsJsonArray(json,"startup_materials",new JsonArray())) {
                    JsonObject m=element.getAsJsonObject();ResourceLocation id=ResourceLocation.parse(GsonHelper.getAsString(m,"item"));
                    Item item=ForgeRegistries.ITEMS.getValue(id);int count=GsonHelper.getAsInt(m,"count");
                    if(item==null || item==net.minecraft.world.item.Items.AIR || count<1 || count>2304 || !seen.add(item))
                        throw new IllegalArgumentException("Invalid research material: "+id);
                    materials.add(new Material(item,count));
                }
                if(ticks==0 && !materials.isEmpty())throw new IllegalArgumentException("Free research cannot consume materials");
                next.put(entry.getKey(),new Definition(entry.getKey(),ticks,rate,tier,List.copyOf(materials)));
            }
            definitions=Map.copyOf(next);
            changede.LOGGER.info("Loaded {} skill research projects",definitions.size());
        } catch(RuntimeException e) { changede.LOGGER.error("Rejected skill research reload; keeping previous definitions",e); }
    }
}
