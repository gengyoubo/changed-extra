package github.com.gengyoubo.CE.skill;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Explicit station actions, separate from ordinary skill learning. */
public record SkillResearchPacket(int menuId,ResourceLocation form,ResourceLocation project,Action action) {
    public enum Action { START, RESUME, PAUSE }
    public static void encode(SkillResearchPacket packet,FriendlyByteBuf buf) {
        buf.writeVarInt(packet.menuId);buf.writeBoolean(packet.form!=null);
        if(packet.form!=null)buf.writeResourceLocation(packet.form);
        buf.writeResourceLocation(packet.project);buf.writeEnum(packet.action);
    }
    public static SkillResearchPacket decode(FriendlyByteBuf buf) {
        return new SkillResearchPacket(buf.readVarInt(),buf.readBoolean() ? buf.readResourceLocation() : null,
                buf.readResourceLocation(),buf.readEnum(Action.class));
    }
    public static void handle(SkillResearchPacket packet,Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();context.enqueueWork(()->{
            var player=context.getSender();
            if(player==null || player.containerMenu.containerId!=packet.menuId || !LatexSkillResearchMenu.isResearching(player))return;
            switch(packet.action) {
                case START -> SkillResearchAccounts.start(player,packet.form,packet.project,false);
                case RESUME -> SkillResearchAccounts.start(player,packet.form,packet.project,true);
                case PAUSE -> SkillResearchAccounts.pause(player,packet.project);
            }
            SkillTreePacket.sendSnapshot(player);
        });context.setPacketHandled(true);
    }
}
