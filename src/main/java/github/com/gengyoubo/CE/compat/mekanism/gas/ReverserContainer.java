package github.com.gengyoubo.CE.compat.mekanism.gas;

import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** Mekanism's tile container, with the inventory lowered for the taller screen and the drain toggle as a menu button. */
public final class ReverserContainer extends MekanismTileContainer<ReverserBlockEntity> {
    public static final int DRAIN_BUTTON = 0;
    public ReverserContainer(int id, Inventory inventory, ReverserBlockEntity tile) { super(GasCompat.REVERSER_CONTAINER, id, inventory, tile); }
    @Override protected int getInventoryYOffset() { return 96; }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != DRAIN_BUTTON || player.level().isClientSide) return false;
        tile.toggleDrain();
        return true;
    }
}
