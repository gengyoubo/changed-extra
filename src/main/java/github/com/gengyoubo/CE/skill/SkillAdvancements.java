package github.com.gengyoubo.CE.skill;

import github.com.gengyoubo.CE.events.AdvancementChainEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Milestones derive from persistent, successfully learned nodes, never UI clicks or research. */
public final class SkillAdvancements {
    private static final ResourceLocation START=ResourceLocation.parse("changede:latex_mastery");
    private static final ResourceLocation PAGE=ResourceLocation.parse("changede:changed_skills");
    private static final ResourceLocation DISTINCT=ResourceLocation.parse("changede:skill_distinction");
    private static final ResourceLocation EVOLUTION=ResourceLocation.parse("changede:skill_evolution");
    private SkillAdvancements() { }
    public static void reconcile(ServerPlayer player) {
        var learned=LatexSkills.unlocked(player);
        if(!learned.contains(START))return;
        AdvancementChainEvents.awardAdvancement(player,PAGE);
        long common=0;boolean key=false;
        for(var node:LatexSkillTrees.all()) {
            if(!learned.contains(node.id()))continue;
            if(node.scope().equals("global"))common++;
            else if(node.key())key=true;
        }
        if(key)AdvancementChainEvents.awardAdvancement(player,DISTINCT);
        if(common>=30)AdvancementChainEvents.awardAdvancement(player,EVOLUTION);
    }
}
