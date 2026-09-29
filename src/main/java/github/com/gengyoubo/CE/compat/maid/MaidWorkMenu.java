package github.com.gengyoubo.CE.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MaidWorkMenu extends AbstractContainerMenu {
    private final Inventory inventory;
    private final int creatureId;
    private final boolean fromWheel;

    public MaidWorkMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, extraData.readInt(), extraData.readBoolean());
    }

    MaidWorkMenu(int id, Inventory inventory, int creatureId, boolean fromWheel) {
        super(LatexMaidCompat.WORK_MENU.get(), id);
        this.inventory = inventory;
        this.creatureId = creatureId;
        this.fromWheel = fromWheel;
    }

    public List<IMaidTask> tasks() { return LatexMaidCompat.tasks(); }
    public int creatureId() { return creatureId; }
    public boolean fromWheel() { return fromWheel; }

    @Override
    public boolean stillValid(@NotNull Player player) {
        ChangedEntity creature = getCreature(player);
        if (creature == null || !creature.isAlive() || player.distanceToSqr(creature) > 64.0D) return false;
        return player.level().isClientSide() || player instanceof ServerPlayer serverPlayer
                && github.com.gengyoubo.CE.compat.synergy.ChangedSynergyFeedApi.hasMaximumFamiliarity(creature, serverPlayer)
                && net.parkabird.changedsynergy.ai.LatexSocialMemory.isPetOwner(creature, serverPlayer);
    }

    private ChangedEntity getCreature(Player player) {
        return player.level().getEntity(creatureId) instanceof ChangedEntity changed ? changed : null;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        List<IMaidTask> available = tasks();
        if (buttonId < 0 || buttonId > available.size() || !(player instanceof ServerPlayer serverPlayer)) return false;
        ChangedEntity creature = getCreature(player);
        if (creature == null || !creature.isAlive() || player.distanceToSqr(creature) > 64.0D
                || !github.com.gengyoubo.CE.compat.synergy.ChangedSynergyFeedApi.hasMaximumFamiliarity(creature, serverPlayer)
                || !net.parkabird.changedsynergy.ai.LatexSocialMemory.isPetOwner(creature, serverPlayer)) return false;

        if (buttonId == available.size()) {
            creature.getPersistentData().remove(LatexMaidCompat.taskTag());
            return true;
        }
        ResourceLocation taskId = available.get(buttonId).getUid();
        creature.getPersistentData().putString(LatexMaidCompat.taskTag(), taskId.toString());
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) { return ItemStack.EMPTY; }
}
