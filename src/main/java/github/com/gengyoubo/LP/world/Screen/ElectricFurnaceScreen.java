package github.com.gengyoubo.LP.world.Screen;

import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.LP.world.Menu.ElectricFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ElectricFurnaceScreen extends AbstractContainerScreen<ElectricFurnaceMenu> {
    private static final HashMap<String, Object> guistate = ElectricFurnaceMenu.guistate;
    private static final ResourceLocation texture = ResourceLocation.parse("changede:textures/screens/electric_furnace_block_entity.png");
    private final Level world;
    private final int x;
    private final int y;
    private final int z;
    private final Player entity;

    public ElectricFurnaceScreen(ElectricFurnaceMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    private int getEnergyScaled(int pixels) {
        int energy = this.menu.getEnergyStored();
        int maxEnergy = this.menu.getMaxEnergyStored();
        return maxEnergy > 0 && energy > 0
                ? energy * pixels / maxEnergy
                : 0;
    }

    private boolean hasEnergy() {
        return this.menu.getEnergyStored() > 0;
    }

    private int getCookProgressScaled(int pixels) {
        int progress = this.menu.getProgress();
        int maxProgress = this.menu.getMaxProgress();
        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }
        return progress * pixels / maxProgress;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }


    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // ===== 背景 =====
        guiGraphics.blit(
                texture,
                this.leftPos,
                this.topPos,
                0, 0,
                this.imageWidth,
                this.imageHeight,
                this.imageWidth,
                this.imageHeight
        );

// ===== 熔炼进度条（左 → 右，纯橙色，无贴图）=====
        int progressWidth = getCookProgressScaled(24);
        if (progressWidth > 0) {
            guiGraphics.fill(
                    this.leftPos + 76,                 // 起点 X
                    this.topPos + 35,                  // 起点 Y
                    this.leftPos + 76 + progressWidth, // 终点 X
                    this.topPos + 35 + 14,              // 终点 Y
                    0xFFFFA500                         // 橙色（ARGB）
            );
        }


        RenderSystem.disableBlend();
    }


    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            if (this.minecraft != null && this.minecraft.player != null) {
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
