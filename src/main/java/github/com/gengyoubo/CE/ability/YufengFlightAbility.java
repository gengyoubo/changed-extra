package github.com.gengyoubo.CE.ability;

import github.com.gengyoubo.CE.skill.LatexSkills;
import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.ability.SimpleAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.Collection;
import java.util.List;

/** Two independently learned actions sharing the existing launch/glide physics. */
public final class YufengFlightAbility extends SimpleAbility {
    private final boolean takeoff;
    public YufengFlightAbility(boolean takeoff) { this.takeoff = takeoff; }
    public String power() { return takeoff ? "yufeng_takeoff" : "yufeng_boost"; }

    public boolean unlocked(Player player) { return LatexSkills.hasActivePower(player, power()); }

    @Override public Component getAbilityName(IAbstractChangedEntity entity) {
        return Component.translatable("skill.changede." + power());
    }
    @Override public Collection<Component> getAbilityDescription(IAbstractChangedEntity entity) {
        return List.of(Component.translatable("skill.changede." + power() + ".description"));
    }
    @Override public int getCoolDown(IAbstractChangedEntity entity) { return takeoff ? 40 : 20; }
    @Override public boolean canUse(IAbstractChangedEntity entity) {
        if (!(entity.getEntity() instanceof Player player) || !player.isAlive() || player.isSpectator()
                || player.isInWaterOrBubble() || !unlocked(player)) return false;
        return takeoff ? player.onGround() : !player.onGround()
                && (player.isFallFlying() || player.getAbilities().flying);
    }
    @Override public void startUsing(IAbstractChangedEntity entity) {
        if (!canUse(entity)) return;
        Player player = (Player) entity.getEntity();
        double technique = 1 + LatexSkills.flightControl(player);
        if (takeoff) LaunchGlideAbility.takeoff(player, technique);
        else LaunchGlideAbility.boost(player, technique);
    }
}
