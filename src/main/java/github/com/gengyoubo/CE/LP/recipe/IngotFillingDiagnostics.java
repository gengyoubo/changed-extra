package github.com.gengyoubo.CE.LP.recipe;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import github.com.gengyoubo.CE.changede;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Map;
import java.util.TreeMap;

/** RecipeManager skips malformed recipes; keep their reasons available to the machine GUI. */
@Mod.EventBusSubscriber(modid="changede")
public final class IngotFillingDiagnostics extends SimpleJsonResourceReloadListener {
    private static volatile String display="";
    public IngotFillingDiagnostics() { super(new Gson(),"recipes"); }
    @SubscribeEvent public static void reload(AddReloadListenerEvent event) { event.addListener(new IngotFillingDiagnostics()); }
    public static String getDisplay() { return display; }
    @Override protected void apply(Map<ResourceLocation,JsonElement> resources,ResourceManager manager,ProfilerFiller profiler) {
        var errors=new TreeMap<ResourceLocation,String>();
        for(var entry:resources.entrySet()) {
            if(!entry.getValue().isJsonObject())continue;
            var json=entry.getValue().getAsJsonObject();
            if(!json.has("type") || !json.get("type").isJsonPrimitive() || !"changede:ingot_filling".equals(json.get("type").getAsString()))continue;
            try { CELPRecipes.INGOT_FILLING_SERIALIZER.get().fromJson(entry.getKey(),json); }
            catch(RuntimeException error) {
                String reason=error.getMessage()==null ? error.getClass().getSimpleName() : error.getMessage();
                errors.put(entry.getKey(),reason);
                changede.LOGGER.error("Skipping ingot filling recipe {}: {}",entry.getKey(),reason);
            }
        }
        String text=errors.entrySet().stream().map(e->e.getKey()+": "+e.getValue()).collect(java.util.stream.Collectors.joining("\n"));
        // Bound the opening payload while retaining full diagnostics in the server log.
        display=text.length()>4096 ? text.substring(0,4096)+"\n... (see server log)" : text;
    }
}
