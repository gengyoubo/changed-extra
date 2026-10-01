package github.com.gengyoubo.CE.skill;

import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod.EventBusSubscriber(modid = "changede")
public final class LatexSkills {
    private static final String TAG = "changede_latex_skills";
    private static final String MODIFIER_PREFIX = "changede.skill.";
    private static final Map<ServerPlayer, SkillTreePacket.FlightState> FLIGHT_STATES = new WeakHashMap<>();
    private static ResourceLocation clientForm;
    private static double clientFlightControl;
    private static final Map<String, Attribute> ATTRIBUTES = Map.of("health", Attributes.MAX_HEALTH,
            "attack", Attributes.ATTACK_DAMAGE, "armor", Attributes.ARMOR, "speed", Attributes.MOVEMENT_SPEED);

    public static ResourceLocation form(Player player) {
        var variant = ProcessTransfur.getPlayerTransfurVariant(player);
        return variant == null ? null : variant.getFormId();
    }

    public static Set<ResourceLocation> unlocked(Player player) {
        Set<ResourceLocation> result = new HashSet<>();
        CompoundTag root = player.getPersistentData().getCompound(TAG);
        for (SkillNode node : LatexSkillTrees.forPlayer(player)) {
            if (root.getCompound(node.tree().toString()).getBoolean(node.id().toString())) result.add(node.id());
        }
        return result;
    }

    public static void unlock(ServerPlayer player, ResourceLocation expectedForm, ResourceLocation id) {
        ResourceLocation form = form(player);
        if (form == null || !form.equals(expectedForm) || !player.isAlive() || player.isSpectator()) return;
        Set<ResourceLocation> learned = unlocked(player);
        Set<ResourceLocation> enabled = new HashSet<>();
        active(player).forEach(n -> enabled.add(n.id()));
        SkillNode node = LatexSkillTrees.forPlayer(player).stream().filter(n -> n.id().equals(id)).findFirst().orElse(null);
        if (node == null || learned.contains(id) || !enabled.containsAll(node.parents())
                || (!player.isCreative() && player.experienceLevel < node.cost())) return;
        CompoundTag root = player.getPersistentData().getCompound(TAG);
        CompoundTag progress = root.getCompound(node.tree().toString());
        progress.putBoolean(id.toString(), true);
        root.put(node.tree().toString(), progress);
        player.getPersistentData().put(TAG, root);
        if (!player.isCreative()) player.giveExperienceLevels(-node.cost());
        refresh(player);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(TAG))
            event.getEntity().getPersistentData().put(TAG, event.getOriginal().getPersistentData().getCompound(TAG).copy());
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount % 20 == 0)
            refresh(player);
    }

    public static List<SkillNode> active(Player player) {
        ResourceLocation form = form(player);
        if (form == null) return List.of();
        Set<ResourceLocation> learned = unlocked(player);
        List<SkillNode> eligible = LatexSkillTrees.forPlayer(player);
        Set<ResourceLocation> enabled = SkillGraph.enabled(eligible, SkillNode::id, SkillNode::parents, learned);
        return eligible.stream().filter(n -> enabled.contains(n.id())).toList();
    }

    public static void refresh(ServerPlayer player) {
        List<SkillNode> active = active(player);
        for (var entry : ATTRIBUTES.entrySet()) {
            AttributeInstance attribute = player.getAttribute(entry.getValue());
            if (attribute == null) continue;
            Map<UUID, SkillNode> desired = new HashMap<>();
            for (SkillNode node : active) if (node.power().equals(entry.getKey()))
                desired.put(UUID.nameUUIDFromBytes((MODIFIER_PREFIX + node.id()).getBytes(StandardCharsets.UTF_8)), node);
            for (AttributeModifier modifier : List.copyOf(attribute.getModifiers())) {
                if (!modifier.getName().startsWith(MODIFIER_PREFIX)) continue;
                SkillNode node = desired.get(modifier.getId());
                if (node == null || node.amount() != modifier.getAmount()) attribute.removeModifier(modifier.getId());
            }
            desired.forEach((id, node) -> {
                if (attribute.getModifier(id) == null) attribute.addTransientModifier(new AttributeModifier(id,
                        MODIFIER_PREFIX + node.id(), node.amount(), AttributeModifier.Operation.ADDITION));
            });
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        SkillTreePacket.FlightState state = new SkillTreePacket.FlightState(form(player), flightControl(player));
        if (!state.equals(FLIGHT_STATES.put(player, state))) {
            github.com.gengyoubo.CE.LP.network.CENetwork.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), state);
        }
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            double reduction = active(player).stream().filter(n -> n.power().equals("fall_resistance")).mapToDouble(SkillNode::amount).sum();
            event.setDamageMultiplier(event.getDamageMultiplier() * (float) Math.max(0, 1 - reduction));
        }
    }

    @SubscribeEvent
    public static void damage(LivingHurtEvent event) {
        if (event.getAmount() <= 0 || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        var target = event.getEntity();
        var type = target instanceof ChangedEntity latex ? latex.getLatexType() : null;
        if (target instanceof Player targetPlayer) {
            var variant = ProcessTransfur.getPlayerTransfurVariant(targetPlayer);
            if (variant != null) type = variant.getLatexType();
        }
        String power = type == ChangedLatexTypes.WHITE_LATEX.get() ? "damage_vs_white"
                : type == ChangedLatexTypes.DARK_LATEX.get() ? "damage_vs_dark" : "";
        if (!power.isEmpty()) {
            double bonus = active(player).stream().filter(n -> n.power().equals(power)).mapToDouble(SkillNode::amount).sum();
            event.setAmount(event.getAmount() * (float) (1 + bonus));
        }
    }

    public static double flightControl(Player player) {
        if (player.level().isClientSide) return Objects.equals(form(player), clientForm) ? clientFlightControl : 0;
        return Math.min(1, active(player).stream().filter(n -> n.power().equals("flight_control")).mapToDouble(SkillNode::amount).sum());
    }

    public static void applyClientFlight(ResourceLocation form, double amount) {
        clientForm = form;
        clientFlightControl = amount;
    }
}
