package github.com.gengyoubo.CE.skill;

import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = "changede")
public final class LatexSkills {
    private static final String TAG = "changede_latex_skills";
    private static final Map<ServerPlayer, SkillTreePacket.FlightState> FLIGHT_STATES = new WeakHashMap<>();
    private static ResourceLocation clientForm;
    private static double clientFlightControl;

    public static ResourceLocation form(Player player) {
        var variant = ProcessTransfur.getPlayerTransfurVariant(player);
        return variant == null ? null : variant.getFormId();
    }
    public static int experience(Player player) { return SkillExperience.points(player.experienceLevel, player.experienceProgress); }
    public static Set<ResourceLocation> unlocked(Player player) {
        Set<ResourceLocation> result = new HashSet<>();
        CompoundTag root = player.getPersistentData().getCompound(TAG);
        for (SkillNode node : LatexSkillTrees.all())
            if (root.getCompound(node.tree().toString()).getBoolean(node.id().toString())) result.add(node.id());
        return result;
    }
    public static List<SkillNode> active(Player player) {
        List<SkillNode> eligible = LatexSkillTrees.forPlayer(player);
        Set<ResourceLocation> enabled = SkillGraph.enabled(eligible, SkillNode::id, SkillNode::parents, unlocked(player));
        return eligible.stream().filter(n -> enabled.contains(n.id())).toList();
    }
    public static SkillAvailability availability(Player player, SkillNode node, Set<ResourceLocation> learned, Set<ResourceLocation> active) {
        List<SkillBlockReason> reasons = requirements(player, node, learned, active).stream().filter(r -> r.current() < r.required()).toList();
        return SkillAvailability.of(learned.contains(node.id()), active.contains(node.id()), reasons);
    }
    public static List<SkillBlockReason> requirements(Player player, SkillNode node, Set<ResourceLocation> learned, Set<ResourceLocation> active) {
        boolean unlocked = learned.contains(node.id());
        List<SkillBlockReason> reasons = new ArrayList<>(LatexSkillTrees.formRequirements(player, node));
        for (ResourceLocation parent : node.parents())
            reasons.add(new SkillBlockReason(learned.contains(parent) && !active.contains(parent) ? "changede:inactive_parent" : "changede:parent",
                    parent.toString(), active.contains(parent) ? 1 : 0, 1));
        if (!unlocked) {
            if (!player.isAlive() || player.isSpectator()) reasons.add(new SkillBlockReason("changede:player_state", "", 0, 1));
            int xp = experience(player);
            reasons.add(new SkillBlockReason("changede:experience", "", player.isCreative() ? Math.max(xp, node.cost()) : xp, node.cost()));
        }
        return List.copyOf(reasons);
    }
    public static void unlock(ServerPlayer player, ResourceLocation expectedForm, ResourceLocation id) {
        ResourceLocation form = form(player);
        if (form == null || !form.equals(expectedForm)) return;
        SkillNode node = LatexSkillTrees.all().stream().filter(n -> n.id().equals(id)).findFirst().orElse(null);
        Set<ResourceLocation> active = new HashSet<>();
        active(player).forEach(n -> active.add(n.id()));
        if (node == null || !availability(player, node, unlocked(player), active).purchasable()) return;
        if (!player.isCreative() && !spendExperience(player, node.cost())) return;
        CompoundTag root = player.getPersistentData().getCompound(TAG);
        CompoundTag progress = root.getCompound(node.tree().toString());
        progress.putBoolean(id.toString(), true);
        root.put(node.tree().toString(), progress);
        player.getPersistentData().put(TAG, root);
        refresh(player);
    }
    private static boolean spendExperience(ServerPlayer player, int cost) {
        if (cost == 0) return true;
        int before = experience(player), level = player.experienceLevel, total = player.totalExperience, score = player.getScore();
        float progress = player.experienceProgress;
        player.giveExperiencePoints(-cost);
        if (experience(player) == before - cost) return true;
        // A Forge XP event can cancel or alter the debit. Do not save a free/overcharged unlock.
        player.experienceLevel = level;
        player.experienceProgress = progress;
        player.totalExperience = total;
        player.setScore(score);
        return false;
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().contains(TAG))
            event.getEntity().getPersistentData().put(TAG, event.getOriginal().getPersistentData().getCompound(TAG).copy());
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount % 20 == 0) refresh(player);
    }
    public static void refresh(ServerPlayer player) {
        SkillRewards.reconcile(player, active(player));
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        SkillTreePacket.FlightState state = new SkillTreePacket.FlightState(form(player), flightControl(player));
        if (!state.equals(FLIGHT_STATES.put(player, state))) github.com.gengyoubo.CE.LP.network.CENetwork.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), state);
    }
    @SubscribeEvent public static void fall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            event.setDamageMultiplier(event.getDamageMultiplier() * (float) (1 - player.getAttributeValue(SkillAttributes.LANDING_RESISTANCE.get())));
    }
    @SubscribeEvent public static void damage(LivingHurtEvent event) {
        if (event.getAmount() <= 0 || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        var target = event.getEntity();
        var type = target instanceof ChangedEntity latex ? latex.getLatexType() : null;
        if (target instanceof Player targetPlayer) {
            var variant = ProcessTransfur.getPlayerTransfurVariant(targetPlayer);
            if (variant != null) type = variant.getLatexType();
        }
        double bonus = type == ChangedLatexTypes.WHITE_LATEX.get() ? player.getAttributeValue(SkillAttributes.DAMAGE_VS_WHITE.get())
                : type == ChangedLatexTypes.DARK_LATEX.get() ? player.getAttributeValue(SkillAttributes.DAMAGE_VS_DARK.get()) : 0;
        event.setAmount(event.getAmount() * (float) (1 + bonus));
    }
    public static double flightControl(Player player) {
        if (player.level().isClientSide) return Objects.equals(form(player), clientForm) ? clientFlightControl : 0;
        return player.getAttributeValue(SkillAttributes.FLIGHT_CONTROL.get());
    }
    public static void applyClientFlight(ResourceLocation form, double amount) {
        clientForm = form;
        clientFlightControl = amount;
    }
    /** Active grants are intentionally unavailable until the WLP/Power reward stage. */
    public static boolean hasActivePower(Player player, String power) { return false; }
}
