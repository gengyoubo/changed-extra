package github.com.gengyoubo.CE.client;

import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.skill.LatexSkillResearchMenu;
import github.com.gengyoubo.CE.skill.SkillTreePacket;
import github.com.gengyoubo.CE.skill.SkillResearchPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import java.util.*;

/** Server-owned research tasks and a separate learning action share the skill catalog. */
public final class LatexSkillResearchScreen extends AbstractContainerScreen<LatexSkillResearchMenu> {
    private CompoundTag data=new CompoundTag();
    private boolean core,previewAll,pending;
    private int page,detailScroll,ticks,listWidth,rows;
    private String selected="";
    private Button raceButton,coreButton,previewButton,previous,next,research,pause;
    public LatexSkillResearchScreen(LatexSkillResearchMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);
    }
    public static void receive(CompoundTag data) {
        if(Minecraft.getInstance().screen instanceof LatexSkillResearchScreen screen) {
            boolean changedForm=!screen.data.getString("form").equals(data.getString("form"));
            screen.data=data;screen.pending=false;
            if(changedForm) { screen.page=0;screen.detailScroll=0;screen.selected=""; }
            screen.updateControls();
        }
    }
    private List<CompoundTag> allNodes() {
        List<CompoundTag> result=new ArrayList<>();
        for(Tag tag:data.getList("nodes",Tag.TAG_COMPOUND))result.add((CompoundTag)tag);
        return result;
    }
    private List<CompoundTag> catalog() {
        return allNodes().stream().filter(n->n.getBoolean("key") && !n.getString("scope").equals("global")
                && (previewAll || n.getBoolean("applicable")) && n.getString("research").equals(core ? "core" : "race"))
                .sorted(Comparator.comparing((CompoundTag n)->n.getString("tree")).thenComparing(n->n.getInt("y"))) .toList();
    }
    private CompoundTag selectedNode() {
        return catalog().stream().filter(n->n.getString("id").equals(selected)).findFirst().orElse(null);
    }
    @Override protected void init() {
        imageWidth=Math.min(420,width-16);imageHeight=Math.min(300,height-16);
        listWidth=(imageWidth-32)*42/100;rows=Math.max(1,(imageHeight-110)/24);
        super.init();
        raceButton=addRenderableWidget(Button.builder(Component.translatable("screen.changede.research.race"),b->changeTab(false)).bounds(leftPos+10,topPos+28,92,20).build());
        coreButton=addRenderableWidget(Button.builder(Component.translatable("screen.changede.research.core"),b->changeTab(true)).bounds(leftPos+106,topPos+28,80,20).build());
        previewButton=addRenderableWidget(Button.builder(Component.empty(),b->{previewAll=!previewAll;page=0;detailScroll=0;updateControls();}).bounds(leftPos+190,topPos+28,imageWidth-200,20).build());
        previous=addRenderableWidget(Button.builder(Component.literal("<"),b->{page--;selected="";detailScroll=0;updateControls();}).bounds(leftPos+10,topPos+imageHeight-35,24,20).build());
        next=addRenderableWidget(Button.builder(Component.literal(">"),b->{page++;selected="";detailScroll=0;updateControls();}).bounds(leftPos+listWidth-14,topPos+imageHeight-35,24,20).build());
        research=addRenderableWidget(Button.builder(Component.empty(),b->{
            var n=selectedNode();
            if(n==null || pending || !research.active)return;
            pending=true;updateControls();
            var form=ResourceLocation.tryParse(data.getString("form"));var id=ResourceLocation.tryParse(n.getString("id"));
            var job=n.getCompound("research_data");
            if(job.getBoolean("completed"))CENetwork.sendToServer(new SkillTreePacket.Request(form,id));
            else CENetwork.sendToServer(new SkillResearchPacket(menu.containerId,form,id,
                    job.getBoolean("started") ? SkillResearchPacket.Action.RESUME : SkillResearchPacket.Action.START));
        }).bounds(leftPos+listWidth+24,topPos+imageHeight-35,imageWidth-listWidth-96,20).build());
        pause=addRenderableWidget(Button.builder(Component.translatable("screen.changede.research.pause"),b->{
            var id=ResourceLocation.tryParse(data.getString("active_research"));if(id==null || pending)return;
            pending=true;updateControls();
            CENetwork.sendToServer(new SkillResearchPacket(menu.containerId,ResourceLocation.tryParse(data.getString("form")),id,SkillResearchPacket.Action.PAUSE));
        }).bounds(leftPos+imageWidth-68,topPos+imageHeight-35,58,20).build());
        updateControls();request();
    }
    private void changeTab(boolean value) { core=value;page=0;detailScroll=0;selected="";updateControls(); }
    private void request() { CENetwork.sendToServer(new SkillTreePacket.Request(null,null)); }
    private void updateControls() {
        if(research==null)return;
        var nodes=catalog();
        int pages=Math.max(1,(nodes.size()+rows-1)/rows);
        page=Math.max(0,Math.min(page,pages-1));
        if(selectedNode()==null)selected=nodes.isEmpty() ? "" : nodes.get(Math.min(page*rows,nodes.size()-1)).getString("id");
        var n=selectedNode();
        raceButton.active=core;coreButton.active=!core;
        previewButton.setMessage(Component.translatable(previewAll ? "screen.changede.research.all" : "screen.changede.research.matching"));
        previous.active=page>0;next.active=page+1<pages;
        var job=n==null ? new CompoundTag() : n.getCompound("research_data");
        research.active=n!=null && !pending && !n.getBoolean("unlocked") && (job.getBoolean("completed")
                ? n.getBoolean("purchasable") : job.getBoolean("started") ? job.getBoolean("can_resume") : job.getBoolean("can_start"));
        research.setMessage(Component.translatable(pending ? "screen.changede.research.pending" : n!=null && n.getBoolean("unlocked")
                ? "screen.changede.skills.learned" : job.getBoolean("completed") ? "screen.changede.research.learn"
                : job.getBoolean("started") ? "screen.changede.research.resume" : "screen.changede.research.start"));
        if(pause!=null)pause.active=!pending && !data.getString("active_research").isEmpty();
    }
    @Override protected void containerTick() { super.containerTick();if(ticks++%20==0)request(); }
    private Component requirement(CompoundTag r,Map<String,CompoundTag> nodes) {
        String type=r.getString("type");Component text;
        if(type.equals("changede:parent") || type.equals("changede:inactive_parent")) {
            var parent=nodes.get(r.getString("subject"));
            text=Component.translatable("screen.changede.skills.reason."+(type.endsWith("inactive_parent") ? "inactive_parent" : "parent"),
                    parent==null ? Component.translatable("screen.changede.research.parent") : Component.translatable(parent.getString("title")));
        } else if(type.equals("changede:experience"))text=Component.translatable("screen.changede.skills.reason.experience",r.getInt("current"),r.getInt("required"));
        else if(type.equals("changede:research_table") || type.equals("changede:player_state") || type.equals("changede:latex_form"))
            text=Component.translatable("screen.changede.skills.reason."+type.substring(type.indexOf(':')+1));
        else text=Component.translatable("screen.changede.research.form_match");
        return Component.literal(r.getBoolean("met") ? "✓ " : "✗ ").append(text).withStyle(r.getBoolean("met") ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
    private List<FormattedCharSequence> details(CompoundTag n,int width) {
        List<FormattedCharSequence> lines=new ArrayList<>();
        Map<String,CompoundTag> nodes=new HashMap<>();allNodes().forEach(v->nodes.put(v.getString("id"),v));
        lines.addAll(font.split(Component.translatable(n.getString("title")).withStyle(ChatFormatting.AQUA),width));
        lines.addAll(font.split(Component.translatable(n.getString("description")),width));
        lines.add(FormattedCharSequence.EMPTY);
        var job=n.getCompound("research_data");
        lines.addAll(font.split(Component.translatable("screen.changede.research.tier."+job.getString("tier")).withStyle(ChatFormatting.AQUA),width));
        lines.addAll(font.split(Component.translatable("screen.changede.research.status."+job.getString("status")).withStyle(
                job.getString("status").equals("running") || job.getBoolean("completed") ? ChatFormatting.GREEN : ChatFormatting.GOLD),width));
        lines.addAll(font.split(Component.translatable("screen.changede.research.duration",time(job.getInt("duration"))),width));
        lines.addAll(font.split(Component.translatable("screen.changede.research.rate",job.getInt("rate")),width));
        lines.addAll(font.split(Component.translatable("screen.changede.research.total",(long)job.getInt("duration")/20*job.getInt("rate")),width));
        lines.addAll(font.split(Component.translatable("screen.changede.research.consumed",job.getLong("consumed")),width));
        lines.addAll(font.split(Component.translatable(job.getBoolean("started") ? "screen.changede.research.materials_paid" : "screen.changede.research.materials"),width));
        if(!job.getBoolean("started"))for(Tag input:job.getList("materials",Tag.TAG_COMPOUND)) {
            var material=(CompoundTag)input;
            lines.addAll(font.split(Component.translatable("screen.changede.research.material",Component.translatable(material.getString("title")),
                    material.getInt("current"),material.getInt("required")).withStyle(data.getBoolean("creative") || material.getInt("current")>=material.getInt("required")
                    ? ChatFormatting.GREEN : ChatFormatting.RED),width));
        }
        if(job.getInt("duration")>0 && !data.getBoolean("station_available"))lines.addAll(font.split(Component.translatable("screen.changede.research.busy"),width));
        if(job.getInt("duration")>0 && !data.getString("active_research").isEmpty() && !data.getString("active_research").equals(n.getString("id")))
            lines.addAll(font.split(Component.translatable("screen.changede.research.one_project"),width));
        lines.add(FormattedCharSequence.EMPTY);
        lines.addAll(font.split(Component.translatable("screen.changede.skills.cost",n.getInt("cost")).withStyle(ChatFormatting.GOLD),width));
        lines.addAll(font.split(Component.translatable("screen.changede.skills.requirements"),width));
        for(Tag r:n.getList("requirements",Tag.TAG_COMPOUND))lines.addAll(font.split(requirement((CompoundTag)r,nodes),width));
        if(n.getBoolean("unlocked"))lines.addAll(font.split(Component.translatable(n.getBoolean("active") ? "screen.changede.skills.learned" : "screen.changede.skills.inactive"),width));
        return lines;
    }
    private static String time(int ticks) { int seconds=Math.max(0,ticks/20);return String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60); }
    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY) {
        g.fill(leftPos-1,topPos-1,leftPos+imageWidth+1,topPos+imageHeight+1,0xFF62878C);
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF142831);
        g.fill(leftPos+8,topPos+54,leftPos+listWidth+12,topPos+imageHeight-40,0xFF203A42);
        g.fill(leftPos+listWidth+20,topPos+54,leftPos+imageWidth-8,topPos+imageHeight-40,0xFF102029);
        var nodes=catalog();
        for(int row=0;row<rows && page*rows+row<nodes.size();row++) {
            var n=nodes.get(page*rows+row);int y=topPos+58+row*24;
            if(n.getString("id").equals(selected))g.fill(leftPos+10,y-2,leftPos+listWidth+10,y+20,0xFF385D63);
            var job=n.getCompound("research_data");
            int color=n.getBoolean("unlocked") || job.getBoolean("completed") ? 0x78D8A3
                    : job.getBoolean("can_start") || job.getBoolean("can_resume") || job.getString("status").equals("running") ? 0xF3D980 : 0x8DADB4;
            var title=font.split(Component.translatable(n.getString("title")),listWidth-10);
            for(int i=0;i<Math.min(title.size(),2);i++)g.drawString(font,title.get(i),leftPos+14,y+i*9,color,false);
        }
        if(nodes.isEmpty()) {
            var lines=font.split(Component.translatable("screen.changede.research.empty"),listWidth-10);
            for(int i=0;i<lines.size();i++)g.drawString(font,lines.get(i),leftPos+14,topPos+64+i*10,0x8DADB4,false);
        }
        var n=selectedNode();
        if(n!=null) {
            int x=leftPos+listWidth+26,w=imageWidth-listWidth-42,bottom=topPos+imageHeight-44;
            var job=n.getCompound("research_data");int duration=job.getInt("duration"),progress=job.getInt("progress");
            float ratio=duration==0 ? job.getBoolean("completed") ? 1 : 0 : Math.min(1,(float)progress/duration);
            g.fill(x,topPos+58,x+w,topPos+70,0xFF263F48);
            g.fill(x,topPos+58,x+(int)(w*ratio),topPos+70,0xFF2A977F);
            g.drawCenteredString(font,Component.literal((int)(ratio*100)+"%"),x+w/2,topPos+60,0xFFFFFF);
            g.drawString(font,Component.translatable("screen.changede.research.remaining",time(duration-progress)),x,topPos+77,0xB1D6D5,false);
            var lines=details(n,w);int visibleLines=Math.max(1,(bottom-topPos-94)/10);
            detailScroll=Math.max(0,Math.min(detailScroll,Math.max(0,lines.size()-visibleLines)));
            g.enableScissor(x,topPos+94,x+w,bottom);
            for(int i=detailScroll;i<Math.min(lines.size(),detailScroll+visibleLines);i++)g.drawString(font,lines.get(i),x,topPos+94+(i-detailScroll)*10,0xE7F2F2,false);
            g.disableScissor();
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        g.drawCenteredString(font,title,imageWidth/2,9,0xECF9FA);
        allNodes().stream().filter(n->n.getString("id").equals(data.getString("active_research"))).findFirst().ifPresent(n->{
            var job=n.getCompound("research_data");int duration=job.getInt("duration");
            int percent=duration<=0 ? 0 : (int)(100L*job.getInt("progress")/duration);
            var text=Component.translatable("screen.changede.research.current",Component.translatable(n.getString("title")),percent);
            g.drawCenteredString(font,font.plainSubstrByWidth(text.getString(),imageWidth-20),imageWidth/2,19,0xA8CFC9);
        });
        g.drawCenteredString(font,Component.literal((page+1)+" / "+Math.max(1,(catalog().size()+rows-1)/rows)),listWidth/2+10,imageHeight-29,0xABC7CD);
        g.drawCenteredString(font,Component.translatable("screen.changede.research.energy",data.getInt("station_wlp"),data.getInt("station_capacity")),imageWidth/2,imageHeight-11,0x91D4C8);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) { renderBackground(g);super.render(g,mouseX,mouseY,partialTick); }
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0 && x>=leftPos+10 && x<leftPos+listWidth+10 && y>=topPos+58 && y<topPos+58+rows*24) {
            int index=page*rows+(int)((y-topPos-58)/24);var nodes=catalog();
            if(index<nodes.size()) { selected=nodes.get(index).getString("id");detailScroll=0;updateControls();return true; }
        }
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseScrolled(double x,double y,double delta) {
        if(x>=leftPos+listWidth+20 && x<leftPos+imageWidth && y>=topPos+54 && y<topPos+imageHeight-40) {
            detailScroll=Math.max(0,detailScroll-(int)Math.signum(delta)*3);return true;
        }
        if(x>=leftPos && x<leftPos+listWidth+16 && y>=topPos+54 && y<topPos+imageHeight-40) {
            page-=(int)Math.signum(delta);selected="";detailScroll=0;updateControls();return true;
        }
        return super.mouseScrolled(x,y,delta);
    }
}
