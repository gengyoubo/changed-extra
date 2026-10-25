package github.com.gengyoubo.CE.client;

import github.com.gengyoubo.CE.mixins.ImageButtonAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = "changede", value = Dist.CLIENT)
public final class LatexSkillScreenEvents {
    private static final ResourceLocation ACCESSORY_ICON = ResourceLocation.fromNamespaceAndPath("changed", "textures/gui/basic_player_info.png");
    private static final boolean CURIOS = ModList.get().isLoaded("curios");

    @SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        github.com.gengyoubo.CE.skill.LatexSkills.applyClientFlight(null, 0);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void init(ScreenEvent.Init.Post event) {
        var screen = event.getScreen();
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
        event.addListener(new SkillButton(screen));
        if (CURIOS) {
            for (var child : java.util.List.copyOf(screen.children())) {
                if (child instanceof ImageButton button
                        && ACCESSORY_ICON.equals(((ImageButtonAccessor) button).changede$getResourceLocation())) {
                    event.removeListener(child);
                }
            }
        }
        updateSideButtons(screen);
    }

    @SubscribeEvent
    public static void beforeRender(ScreenEvent.Render.Pre event) {
        updateSideButtons(event.getScreen());
    }

    @SubscribeEvent
    public static void beforeClick(ScreenEvent.MouseButtonPressed.Pre event) {
        updateSideButtons(event.getScreen());
    }

    private static void updateSideButtons(Screen screen) {
        if (!(screen instanceof InventoryScreen) && !(screen instanceof CreativeModeInventoryScreen)) return;
        var layout = PlayerAttributePanelEvents.layout(screen);
        for (var child : screen.children()) {
            if (child instanceof SkillButton button) {
                button.setPosition(layout.buttonX(), layout.buttonY() + PlayerAttributePanelEvents.Layout.TAB_SPACING);
            } else if (child instanceof ImageButton button
                    && ACCESSORY_ICON.equals(((ImageButtonAccessor) button).changede$getResourceLocation())) {
                // Keep Changed's original action and creative-tab visibility, but share the side-tab layout.
                button.setPosition(layout.buttonX() + (PlayerAttributePanelEvents.Layout.BUTTON_WIDTH - button.getWidth()) / 2,
                        layout.buttonY() + PlayerAttributePanelEvents.Layout.TAB_SPACING * 2);
            }
        }
    }

    private static final class SkillButton extends Button {
        private SkillButton(Screen screen) {
            super(0, 0, PlayerAttributePanelEvents.Layout.BUTTON_WIDTH, PlayerAttributePanelEvents.Layout.BUTTON_HEIGHT,
                    Component.translatable("screen.changede.skills.button"),
                    button -> Minecraft.getInstance().setScreen(new LatexSkillScreen(screen)), narration -> narration.get());
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            PlayerAttributePanelEvents.renderTab(graphics, Minecraft.getInstance().font, getX(), getY(), getMessage(), isHoveredOrFocused(), false);
        }
    }
}
