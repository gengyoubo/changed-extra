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

    public record FlightState(ResourceLocation form, double amount, CompoundTag mechanics) {
        public static void encode(FlightState packet, FriendlyByteBuf buf) {
            buf.writeBoolean(packet.form != null);
            if (packet.form != null) buf.writeResourceLocation(packet.form);
            buf.writeDouble(packet.amount);
            buf.writeNbt(packet.mechanics);
        }
        public static FlightState decode(FriendlyByteBuf buf) {
            ResourceLocation form=buf.readBoolean() ? buf.readResourceLocation() : null;
            double amount=buf.readDouble(); CompoundTag mechanics=buf.readNbt();
            return new FlightState(form,amount,mechanics==null?new CompoundTag():mechanics);
        }
        public static void handle(FlightState packet, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> { LatexSkills.applyClientFlight(packet.form, packet.amount); SkillMechanics.applyClient(packet.form,packet.mechanics); });
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
                var type = SkillCombat.latexType(player);
                data.putString("latex_type", type == net.ltxprogrammer.changed.init.ChangedLatexTypes.DARK_LATEX.get() ? "dark"
                        : type == net.ltxprogrammer.changed.init.ChangedLatexTypes.WHITE_LATEX.get() ? "white" : "any");
                data.putInt("experience", LatexSkills.experience(player));
                data.putInt("lives", SkillMechanics.lives(player));
                data.putBoolean("creative", player.isCreative());
                ListTag nodes = new ListTag();
                var unlocked = LatexSkills.unlocked(player);
                java.util.Set<ResourceLocation> active = new java.util.HashSet<>();
                LatexSkills.active(player).forEach(n -> active.add(n.id()));
                for (SkillNode node : LatexSkillTrees.all()) {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("tree", node.tree().toString());
                    tag.putString("scope", node.scope());
                    tag.putBoolean("applicable", LatexSkillTrees.applicable(player, node));
                    tag.putString("latex_type", LatexSkillTrees.latexType(node));
                    tag.putString("id", node.id().toString());
                    tag.putString("title", node.title());
                    tag.putString("description", node.description());
                    tag.putInt("cost", node.cost());
                    tag.putInt("x", node.x());
                    tag.putInt("y", node.y());
                    tag.putBoolean("key", node.key());
                    var availability = LatexSkills.availability(player, node, unlocked, active);
                    tag.putBoolean("unlocked", availability.unlocked());
                    tag.putBoolean("active", availability.active());
                    tag.putBoolean("purchasable", availability.purchasable());
                    ListTag reasons = new ListTag(), requirements = new ListTag(), rewards = new ListTag();
                    availability.reasons().forEach(reason -> reasons.add(describe(reason)));
                    LatexSkills.requirements(player, node, unlocked, active).forEach(reason -> requirements.add(describe(reason)));
                    node.rewards().forEach(reward -> rewards.add(reward.describe()));
                    tag.put("reasons", reasons);
                    tag.put("requirements", requirements);
                    tag.put("rewards", rewards);
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

    private static CompoundTag describe(SkillBlockReason reason) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", reason.type());
        tag.putString("subject", reason.subject());
        tag.putInt("current", reason.current());
        tag.putInt("required", reason.required());
        tag.putBoolean("met", reason.current() >= reason.required());
        return tag;
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
