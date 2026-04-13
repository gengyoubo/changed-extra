package github.com.gengyoubo.LP.world.Screen;

import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.LP.BlockEntity.RedstoneMode;
import github.com.gengyoubo.LP.network.CENetwork;
import github.com.gengyoubo.LP.network.packet.CycleGeneratorRedstoneModePacket;
import github.com.gengyoubo.LP.procedures.CloseTextures;
import github.com.gengyoubo.LP.world.Menu.BasicGeneratorBlockEntityMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class BasicGeneratorBlockEntityScreen extends AbstractContainerScreen<BasicGeneratorBlockEntityMenu> {

    private static final HashMap<String, Object> guistate = BasicGeneratorBlockEntityMenu.guistate;
    private final Player entity;
    private Button redstoneModeButton;

    public BasicGeneratorBlockEntityScreen(BasicGeneratorBlockEntityMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.entity = inventory.player;
        this.imageWidth = 230;
        this.imageHeight = 180;
    }

    private static final ResourceLocation texture = ResourceLocation.parse("changede:textures/screens/basic_generator_block_entity.png");
    private static final ResourceLocation TEXTURE_1 = ResourceLocation.parse("changede:textures/screens/778828.png");
    private static final ResourceLocation TEXTURE_2 = ResourceLocation.parse("changede:textures/screens/778863.png");

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

        if (CloseTextures.execute(entity)) {
            long time = System.currentTimeMillis() / 1000;
            ResourceLocation currentTexture = (time % 2 == 0) ? TEXTURE_1 : TEXTURE_2;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(this.leftPos+107, this.topPos+38, 0);
            guiGraphics.pose().scale(0.125f, 0.125f, 1.0f);

            guiGraphics.blit(currentTexture, 0, 0, 0, 0, 128, 128, 128, 128);

            guiGraphics.pose().popPose();
        }

        RenderSystem.disableBlend();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        this.renderTooltip(guiGraphics, mouseX, mouseY);

    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            if (this.minecraft != null && this.minecraft.player !=null) {
                this.minecraft.player.closeContainer();
            }
            return true;
        }

        return super.keyPressed(key, b, c);
    }

    private static String getRedstoneModeLabel(RedstoneMode mode) {
        return switch (mode) {
            case ALWAYS_ON -> "Redstone: Always On";
            case ALWAYS_OFF -> "Redstone: Always Off";
            case ON_WITH_REDSTONE -> "Redstone: On with Signal";
            case OFF_WITH_REDSTONE -> "Redstone: Off with Signal";
        };
    }

    private static String getShortRedstoneModeLabel(RedstoneMode mode) {
        return switch (mode) {
            case ALWAYS_ON -> "Always On";
            case ALWAYS_OFF -> "Always Off";
            case ON_WITH_REDSTONE -> "Signal On";
            case OFF_WITH_REDSTONE -> "Signal Off";
        };
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, "Generator", 10, 10, 0x404040, false);
        //Latex Power
        String energyText = menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " LP";
        guiGraphics.drawString(this.font, energyText, 10, 24, 0x404040, false);
        guiGraphics.drawString(this.font, getRedstoneModeLabel(menu.getRedstoneMode()), 10, 38, 0x404040, false);
    }

    @Override
    public void init() {
        super.init();
        redstoneModeButton = Button.builder(
                CommonComponents.EMPTY,
                button -> CENetwork.sendToServer(new CycleGeneratorRedstoneModePacket(menu.getBlockPos()))
        ).bounds(this.leftPos + 150, this.topPos + 20, 70, 20).build();
        updateRedstoneModeButton();
        this.addRenderableWidget(redstoneModeButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateRedstoneModeButton();
    }

    private void updateRedstoneModeButton() {
        if (redstoneModeButton != null) {
            redstoneModeButton.setMessage(Component.literal(getShortRedstoneModeLabel(menu.getRedstoneMode())));
        }
    }
}
