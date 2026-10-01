package github.com.gengyoubo.CE.skill;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Serializable, data-driven node; amounts use vanilla attribute units. */
public record SkillNode(ResourceLocation tree, String scope, ResourceLocation id, String title, String description, int cost,
                        List<ResourceLocation> parents, int x, int y, boolean key, String power, double amount) {
}
