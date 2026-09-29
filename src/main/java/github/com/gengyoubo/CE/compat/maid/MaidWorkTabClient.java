package github.com.gengyoubo.CE.compat.maid;

import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.MaidWorkSwitchPacket;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.parkabird.changedsynergy.client.BondedCreatureInventoryScreen;

/** Adds the work-page entry to Changed: Synergy's existing bonded creature screen. */
public final class MaidWorkTabClient {
    private MaidWorkTabClient() {}

    public static void install() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ScreenEvent.Init.Post.class, MaidWorkTabClient::onScreenInit);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof BondedCreatureInventoryScreen screen)) return;
        ChangedEntity creature = screen.getMenu().getPet();
        if (creature == null) return;

        Button workTab = Button.builder(Component.translatable("screen.changede.maid_work.tab"), ignored ->
                CENetwork.sendToServer(new MaidWorkSwitchPacket(creature.getId(), true)))
                .bounds(screen.getGuiLeft() + 158, screen.getGuiTop() + 7, 48, 16)
                .build();
        event.addListener(workTab);
    }
}
