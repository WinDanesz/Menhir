package com.windanesz.menhir.client.gui;

import com.windanesz.menhir.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GuiEvents {

    @SubscribeEvent
    public void guiPostInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiInventory) {
            if (!Settings.clientSettings.show_inventory_button) {
                return;
            }
            GuiContainer gui = (GuiContainer) event.getGui();
            // GuiInventory's dimensions are typically 176x166
            final int GUI_INVENTORY_WIDTH = 176;
            final int GUI_INVENTORY_HEIGHT = 166;
            
            int guiLeft = (event.getGui().width - GUI_INVENTORY_WIDTH) / 2;
            int guiTop = (event.getGui().height - GUI_INVENTORY_HEIGHT) / 2;
            
            // Positioned based on config settings
            int xOffset = Settings.clientSettings.inventory_button_x_offset;
            int yOffset = Settings.clientSettings.inventory_button_y_offset;
            
            // Calculate absolute position
            int buttonX = guiLeft + xOffset;
            int buttonY = guiTop + yOffset;
            
            event.getButtonList().add(new GuiBirthsignButton(10000, buttonX, buttonY, ""));
        }
    }

    @SubscribeEvent
    public void drawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (GuiBirthsignButton.tooltipString != null) {
            event.getGui().drawHoveringText(java.util.Collections.singletonList(GuiBirthsignButton.tooltipString), event.getMouseX(), event.getMouseY());
            GuiBirthsignButton.tooltipString = null;
        }
    }

    @SubscribeEvent
    public void actionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getGui() instanceof GuiInventory) {
            if (event.getButton() instanceof GuiBirthsignButton) {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player != null) {
                    Minecraft.getMinecraft().displayGuiScreen(new GuiBirthsign(player));
                }
            }
        }
    }
}
