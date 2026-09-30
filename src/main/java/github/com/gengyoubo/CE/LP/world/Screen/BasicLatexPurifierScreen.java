package github.com.gengyoubo.CE.LP.world.Screen;

import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.CE.LP.world.Menu.BasicLatexPurifierMenu;
import github.com.gengyoubo.CE.util.AmountFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BasicLatexPurifierScreen extends AbstractContainerScreen<BasicLatexPurifierMenu> {
    public BasicLatexPurifierScreen(BasicLatexPurifierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 184;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(75, 28, 16, 34, mouseX, mouseY)) {
            FluidStack fluid = menu.getStoredFluid();
            Component amount = Component.literal(AmountFormat.format(menu.getFluidAmount(), menu.getTankCapacity(), "mB"));
            graphics.renderComponentTooltip(font, fluid.isEmpty() ? List.of(amount)
                    : List.of(fluid.getDisplayName(), amount), mouseX, mouseY);
        }
        if (isHovering(8, 79, 160, 4, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal(AmountFormat.format(menu.getEnergy(), menu.getMaxEnergy(), "LP")), mouseX, mouseY);
        }
    }

    @Override protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF252932);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 20, 0xFF3B414D);
        graphics.fill(leftPos + 1, topPos + 83, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF343A45);
        // slot and tank wells
        graphics.fill(leftPos + 26, topPos + 26, leftPos + 62, topPos + 62, 0xFF16191F);
        graphics.fill(leftPos + 116, topPos + 26, leftPos + 152, topPos + 62, 0xFF16191F);
        graphics.fill(leftPos + 75, topPos + 28, leftPos + 91, topPos + 62, 0xFF11151A);
        renderFluid(graphics);
        graphics.fill(leftPos + 8, topPos + 79, leftPos + 168, topPos + 83, 0xFF15191E);
        int energyWidth = menu.getMaxEnergy() > 0 ? menu.getEnergy() * 160 / menu.getMaxEnergy() : 0;
        if (energyWidth > 0) graphics.fill(leftPos + 8, topPos + 79, leftPos + 8 + energyWidth, topPos + 83, 0xFF56A8FF);

        int max = Math.max(1, menu.getMaxProgress());
        int itemWidth = menu.getItemProgress() * 18 / max;
        int fluidWidth = menu.getFluidProgress() * 24 / max;
        graphics.fill(leftPos + 55, topPos + 38, leftPos + 55 + itemWidth, topPos + 43, 0xFFFFD16B);
        graphics.fill(leftPos + 93, topPos + 49, leftPos + 93 + fluidWidth, topPos + 54, 0xFF85D9FF);
    }

    private void renderFluid(GuiGraphics graphics) {
        FluidStack fluid = menu.getStoredFluid();
        if (fluid.isEmpty() || menu.getTankCapacity() <= 0) return;

        IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation texture = properties.getStillTexture(fluid);
        if (texture == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int color = properties.getTintColor(fluid);
        int height = Math.min(30, Math.max(1, fluid.getAmount() * 30 / menu.getTankCapacity()));
        int x = leftPos + 77;
        int bottom = topPos + 60;

        // Tile the actual fluid sprite and crop it to the tank's current fill level.
        graphics.enableScissor(x, bottom - height, x + 12, bottom);
        graphics.setColor(((color >>> 16) & 255) / 255.0F, ((color >>> 8) & 255) / 255.0F,
                (color & 255) / 255.0F, ((color >>> 24) & 255) / 255.0F);
        try {
            for (int offset = 0; offset < height; offset += 16) {
                graphics.blit(x, bottom - offset - 16, 0, 16, 16, sprite);
            }
        } finally {
            graphics.setColor(1, 1, 1, 1);
            graphics.disableScissor();
        }
    }

    @Override protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable("gui.changede.basic_latex_purifier.items"), 27, 15, 0xD8DDE5, false);
        graphics.drawString(font, Component.translatable("gui.changede.basic_latex_purifier.fluid"), 71, 15, 0xD8DDE5, false);
        graphics.drawString(font, Component.translatable("gui.changede.basic_latex_purifier.output"), 118, 15, 0xD8DDE5, false);
        graphics.drawString(font, Component.translatable("gui.changede.basic_latex_purifier.energy", AmountFormat.format(menu.getEnergy(), menu.getMaxEnergy())), 8, 65, 0x88BBFF, false);
        graphics.drawString(font, playerInventoryTitle, 8, 89, 0xFFFFFF, false);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256) {
            if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
