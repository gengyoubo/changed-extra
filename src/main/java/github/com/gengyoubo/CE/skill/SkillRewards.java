package github.com.gengyoubo.CE.skill;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/** Only Attribute and None are registered in the natural-growth stage. */
public final class SkillRewards {
    private static final String PREFIX = "changede.skill.";
    private static final ResourceLocation ATTRIBUTE = id("changede:attribute"), NONE = id("changede:none");
    private static final Map<ResourceLocation, Function<JsonObject, SkillReward>> TYPES = new LinkedHashMap<>();
    private record Source(ResourceLocation node, int index) { }
    private static final Map<Player, Map<Source, SkillReward>> APPLIED = new WeakHashMap<>();
    static {
        register(ATTRIBUTE, json -> {
            ResourceLocation attribute = id(GsonHelper.getAsString(json, "attribute"));
            double amount = GsonHelper.getAsDouble(json, "amount");
            if (!ForgeRegistries.ATTRIBUTES.containsKey(attribute) || !Double.isFinite(amount) || amount < 0 || amount > 100)
                throw new IllegalArgumentException("Invalid attribute reward " + attribute);
            String operation = GsonHelper.getAsString(json, "operation", "addition");
            AttributeModifier.Operation op = switch (operation) {
                case "addition" -> AttributeModifier.Operation.ADDITION;
                case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
                case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
                default -> throw new IllegalArgumentException("Unknown attribute operation " + operation);
            };
            if (op != AttributeModifier.Operation.ADDITION && amount > 1)
                throw new IllegalArgumentException("Multipliers must be in [0, 1]");
            return new AttributeReward(attribute, amount, op);
        });
        register(NONE, json -> new NoneReward());
    }
    private SkillRewards() { }
    public static void register(ResourceLocation type, Function<JsonObject, SkillReward> factory) {
        if (TYPES.putIfAbsent(type, factory) != null) throw new IllegalArgumentException("Duplicate reward type " + type);
    }
    public static ResourceLocation id(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) throw new IllegalArgumentException("Invalid resource id " + value);
        return id;
    }
    public static SkillReward parse(JsonObject json) {
        ResourceLocation type = id(GsonHelper.getAsString(json, "type"));
        var factory = TYPES.get(type);
        if (factory == null) throw new IllegalArgumentException("Unknown reward type " + type);
        return factory.apply(json);
    }
    private static String source(ResourceLocation node, int index) { return PREFIX + node + "." + index; }
    private static UUID modifierId(ResourceLocation node, int index) {
        return UUID.nameUUIDFromBytes(source(node, index).getBytes(StandardCharsets.UTF_8));
    }
    public record AttributeReward(ResourceLocation attribute, double amount, AttributeModifier.Operation operation) implements SkillReward {
        @Override public ResourceLocation type() { return ATTRIBUTE; }
        private AttributeInstance instance(Player player) { return player.getAttribute(ForgeRegistries.ATTRIBUTES.getValue(attribute)); }
        @Override public void apply(Player player, ResourceLocation node, int index) {
            AttributeInstance target = instance(player);
            if (target == null) return;
            UUID id = modifierId(node, index);
            var previous = target.getModifier(id);
            if (previous != null && (previous.getAmount() != amount || previous.getOperation() != operation)) {
                remove(player, node, index);
                previous = null;
            }
            if (previous == null) target.addTransientModifier(new AttributeModifier(id, source(node, index), amount, operation));
        }
        @Override public void remove(Player player, ResourceLocation node, int index) {
            AttributeInstance target = instance(player);
            if (target != null) target.removeModifier(modifierId(node, index));
        }
        @Override public CompoundTag describe() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", type().toString());
            tag.putString("attribute", attribute.toString());
            tag.putDouble("amount", amount);
            tag.putInt("operation", operation.toValue());
            return tag;
        }
    }
    public record NoneReward() implements SkillReward {
        @Override public ResourceLocation type() { return NONE; }
        @Override public void apply(Player player, ResourceLocation node, int index) { }
        @Override public void remove(Player player, ResourceLocation node, int index) { }
        @Override public CompoundTag describe() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", type().toString());
            return tag;
        }
    }
    public static void reconcile(Player player, List<SkillNode> active) {
        Map<Attribute, Set<UUID>> desired = new HashMap<>();
        Map<Source, SkillReward> next = new LinkedHashMap<>();
        for (SkillNode node : active) for (int i = 0; i < node.rewards().size(); i++) {
            next.put(new Source(node.id(), i), node.rewards().get(i));
            if (node.rewards().get(i) instanceof AttributeReward reward)
                desired.computeIfAbsent(ForgeRegistries.ATTRIBUTES.getValue(reward.attribute()), ignored -> new HashSet<>())
                        .add(modifierId(node.id(), i));
        }
        Map<Source, SkillReward> previous = APPLIED.getOrDefault(player, Map.of());
        previous.forEach((source, reward) -> {
            if (!reward.equals(next.get(source))) reward.remove(player, source.node(), source.index());
        });
        // Includes attributes removed from a datapack and modifiers from the previous schema.
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES.getValues()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            for (AttributeModifier modifier : List.copyOf(instance.getModifiers()))
                if (modifier.getName().startsWith(PREFIX) && !desired.getOrDefault(attribute, Set.of()).contains(modifier.getId()))
                    instance.removeModifier(modifier.getId());
        }
        next.forEach((source, reward) -> {
            // Attributes also repair missing transient modifiers. Other future reward types
            // receive apply once per source/change and remove on deactivation or reload.
            if (reward instanceof AttributeReward || !reward.equals(previous.get(source)))
                reward.apply(player, source.node(), source.index());
        });
        APPLIED.put(player, Map.copyOf(next));
    }
}
