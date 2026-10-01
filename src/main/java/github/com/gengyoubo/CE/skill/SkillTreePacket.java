package github.com.gengyoubo.CE.skill;

import github.com.gengyoubo.CE.LP.network.CENetwork;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import java.util.function.Supplier;

public final class SkillTreePacket {
    private SkillTreePacket() { }

    public record FlightState(ResourceLocation form, double amount) {
        public static void encode(FlightState packet, FriendlyByteBuf buf) {
            buf.writeBoolean(packet.form != null);
            if (packet.form != null) buf.writeResourceLocation(packet.form);
            buf.writeDouble(packet.amount);
        }
        public static FlightState decode(FriendlyByteBuf buf) {
            return new FlightState(buf.readBoolean() ? buf.readResourceLocation() : null, buf.readDouble());
        }
        public static void handle(FlightState packet, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> LatexSkills.applyClientFlight(packet.form, packet.amount));
            context.setPacketHandled(true);
        }
    }

    /** Null node requests a snapshot. Every unlock is validated against the sender's current form. */
    public record Request(ResourceLocation form, ResourceLocation node) {
        public static void encode(Request packet, FriendlyByteBuf buf) {
            buf.writeBoolean(packet.form != null);
            if (packet.form != null) buf.writeResourceLocation(packet.form);
            buf.writeBoolean(packet.node != null);
            if (packet.node != null) buf.writeResourceLocation(packet.node);
        }
        public static Request decode(FriendlyByteBuf buf) {
            return new Request(buf.readBoolean() ? buf.readResourceLocation() : null,
                    buf.readBoolean() ? buf.readResourceLocation() : null);
        }
        public static void handle(Request packet, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> {
                var player = context.getSender();
                if (player == null) return;
                if (packet.node != null) LatexSkills.unlock(player, packet.form, packet.node);
                CompoundTag data = new CompoundTag();
                var form = LatexSkills.form(player);
                data.putString("form", form == null ? "" : form.toString());
                var variant = net.ltxprogrammer.changed.process.ProcessTransfur.getPlayerTransfurVariant(player);
                var latexType = variant == null ? null : variant.getLatexType();
                data.putString("latex_background", latexType == net.ltxprogrammer.changed.init.ChangedLatexTypes.DARK_LATEX.get()
                        ? "dark" : latexType == net.ltxprogrammer.changed.init.ChangedLatexTypes.WHITE_LATEX.get() ? "white" : "common");
                data.putInt("levels", player.experienceLevel);
                data.putBoolean("creative", player.isCreative());
                ListTag nodes = new ListTag();
                var unlocked = LatexSkills.unlocked(player);
                java.util.Set<ResourceLocation> active = new java.util.HashSet<>();
                LatexSkills.active(player).forEach(n -> active.add(n.id()));
                for (SkillNode node : LatexSkillTrees.forPlayer(player)) {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("tree", node.tree().toString());
                    tag.putString("scope", node.scope());
                    tag.putString("id", node.id().toString());
                    tag.putString("title", node.title());
                    tag.putString("description", node.description());
                    tag.putInt("cost", node.cost());
                    tag.putInt("x", node.x());
                    tag.putInt("y", node.y());
                    tag.putBoolean("key", node.key());
                    tag.putBoolean("unlocked", unlocked.contains(node.id()));
                    tag.putBoolean("active", active.contains(node.id()));
                    ListTag parents = new ListTag();
                    node.parents().forEach(parent -> parents.add(StringTag.valueOf(parent.toString())));
                    tag.put("parents", parents);
                    nodes.add(tag);
                }
                data.put("nodes", nodes);
                CENetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(data));
            });
            context.setPacketHandled(true);
        }
    }

    public record Snapshot(CompoundTag data) {
        public static void encode(Snapshot packet, FriendlyByteBuf buf) { buf.writeNbt(packet.data); }
        public static Snapshot decode(FriendlyByteBuf buf) {
            CompoundTag data = buf.readNbt();
            return new Snapshot(data == null ? new CompoundTag() : data);
        }
        public static void handle(Snapshot packet, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    github.com.gengyoubo.CE.client.LatexSkillScreen.receive(packet.data)));
            context.setPacketHandled(true);
        }
    }
}
