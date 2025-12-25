package com.windanesz.menhir.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer; // Still needed for parentGui in drawHoveringText
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import java.util.Collections;

public class GuiBirthsignButton extends GuiButton {
    private static final ResourceLocation TEXTURE = new ResourceLocation("menhir", "textures/gui/birthsign_button.png");
    public static String tooltipString = null;

    // Constructor now takes absolute x, y coordinates
    public GuiBirthsignButton(int buttonId, int x, int y, String buttonText) {
        super(buttonId, x, y, 10, 10, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (this.visible) {
            // No updatePosition() call needed as x,y are absolute
            mc.getTextureManager().bindTexture(TEXTURE);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            
            int textureX = 0;
            if (this.hovered) {
                textureX = 10;
                tooltipString = I18n.format("menhir.tooltip.birthsigns");
            }

            drawModalRectWithCustomSizedTexture(this.x, this.y, textureX, 0, this.width, this.height, 20, 10);
        }
    }
}
