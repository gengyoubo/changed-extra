package github.com.gengyoubo.LP.world.Screen;

import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.LP.procedures.CloseTextures;
import github.com.gengyoubo.LP.world.Menu.BasicGeneratorBlockEntityMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class BasicGeneratorBlockEntityScreen extends AbstractContainerScreen<BasicGeneratorBlockEntityMenu> {

    private final static HashMap<String, Object> guistate = BasicGeneratorBlockEntityMenu.guistate;

    private final Player entity;

    public BasicGeneratorBlockEntityScreen(BasicGeneratorBlockEntityMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        Level world = container.world;
        int x = container.x;
        int y = container.y;
        int z = container.z;
        this.entity = container.entity;
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

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public void init() {
        super.init();

    }

}
