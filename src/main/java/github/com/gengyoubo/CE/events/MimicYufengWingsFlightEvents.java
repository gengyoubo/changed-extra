package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.LP.item.MimicYufengWingsItem;
import net.minecraftforge.event.TickEvent;

public class MimicYufengWingsFlightEvents {
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MimicYufengWingsItem.updateFlight(event.player);
    }
}
