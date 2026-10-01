package github.com.gengyoubo.CE.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** A source is one reward slot, so revoking it cannot remove another node's contribution. */
public interface SkillReward {
    ResourceLocation type();
    void apply(Player player, ResourceLocation node, int index);
    void remove(Player player, ResourceLocation node, int index);
    CompoundTag describe();
}
