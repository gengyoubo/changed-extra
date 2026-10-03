package github.com.gengyoubo.CE.compat.synergy.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class LatexCampfireMenu extends AbstractContainerMenu {
    final BlockPos pos;
    final UUID settlementId;
    CompoundTag view = new CompoundTag();
    private int syncCounter;
    private long lastActionTick = -1;
    private final Player viewer;
    public LatexCampfireMenu(int id, Inventory inventory, FriendlyByteBuf data) { this(id, inventory, data.readBlockPos(), data.readUUID()); }
    LatexCampfireMenu(int id, Inventory inventory, BlockPos pos, UUID settlementId) {
        super(LatexCampfireCompat.MENU.get(), id);
        this.pos = pos; this.settlementId = settlementId; this.viewer = inventory.player;
    }
    LatexSettlementData.Settlement camp(ServerPlayer player) {
        if (!stillValid(player) || !(player.serverLevel().getBlockEntity(pos) instanceof LatexCampfireBlockEntity core)) return null;
        return LatexSettlementService.resolve(player.serverLevel(), core);
    }
    @Override public boolean stillValid(Player player) {
        double distance = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (distance > 32 * 32) return false;
        if (player.level().isClientSide) return true;
        if (!(player instanceof ServerPlayer server) || !(server.serverLevel().getBlockEntity(pos) instanceof LatexCampfireBlockEntity core)
                || !settlementId.equals(core.settlementId)) return false;
        var camp = LatexSettlementService.resolve(server.serverLevel(), core);
        if (camp == null) return false;
        if (distance <= 64) return true;
        var visitor = camp.visitor == null ? null : LatexSettlementService.entity(server.serverLevel(), camp.visitor.id);
        return visitor != null && visitor.distanceToSqr(player) <= 64;
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (viewer instanceof ServerPlayer player && syncCounter++ % 20 == 0) sync(player);
    }
    void sync(ServerPlayer player) {
        var camp = camp(player);
        if (camp != null) CampNetwork.sync(player, containerId, LatexSettlementService.snapshot(player, camp));
    }
    void action(ServerPlayer player, String action, UUID target, int index) {
        var camp = camp(player);
        if (camp == null) return;
        long tick = player.serverLevel().getGameTime();
        if (lastActionTick == tick) return;
        lastActionTick = tick;
        LatexSettlementService.action(player, camp, action, target, index);
        sync(player);
    }
}
