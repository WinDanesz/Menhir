package com.windanesz.menhir.client.gui;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.Settings;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.client.ClientProxy;
import com.windanesz.menhir.eventhandler.BirthsignEffectManager;
import com.windanesz.menhir.network.NetworkHandler;
import com.windanesz.menhir.network.PacketSelectActiveAbility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Radial menu for selecting active abilities.
 * Adapted from Ancient Spellcraft's GuiRadialMenu.
 */
@Mod.EventBusSubscriber(Side.CLIENT)
public class GuiRadialMenu extends GuiScreen {

    public static final ResourceLocation UNKNOWN_ICON = new ResourceLocation(Menhir.MODID, "textures/gui/unknown_ability.png");

    private boolean closing;
    private boolean doneClosing;
    private double startAnimation;

    private int selectedItem = -1;

    private final List<IBirthsignActiveAbility> abilities;
    private int abilityCount;
    private int currentIndex = 0;

    public GuiRadialMenu() {
        Minecraft mc = Minecraft.getMinecraft();
        this.abilities = BirthsignEffectManager.getFlattenedAbilities(mc.player);
        this.abilityCount = abilities.size();
        
        // Get currently selected index
        IBirthsignData data = BirthsignDataProvider.get(mc.player);
        if (data != null) {
            this.currentIndex = data.getInt("selected_ability_index");
            if (this.currentIndex < 0 || this.currentIndex >= abilityCount) {
                this.currentIndex = 0;
            }
        }

        this.startAnimation = mc.world.getTotalWorldTime() + (double) mc.getRenderPartialTicks();
    }

    @SubscribeEvent
    public static void overlayEvent(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.CROSSHAIRS) { return; }

        if (Minecraft.getMinecraft().currentScreen instanceof GuiRadialMenu) {
            event.setCanceled(true);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if (closing) {
            if (doneClosing) {
                Minecraft.getMinecraft().displayGuiScreen(null);
                ClientProxy.wipeOpen();
            }
            return;
        }

        if (!GameSettings.isKeyDown(ClientProxy.KEY_ACTIVATE_RADIAL_MENU)) {
            if (Settings.clientSettings.release_to_swap) {
                processClick();
            } else {
                animateClose();
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        processClick();
    }

    private void processClick() {
        if (closing) { return; }

        if (selectedItem != -1) {
            currentIndex = selectedItem;
            IMessage msg = new PacketSelectActiveAbility.Message(currentIndex);
            NetworkHandler.INSTANCE.sendToServer(msg);
        }

        animateClose();
    }

    private void animateClose() {
        closing = true;
        doneClosing = false;
        startAnimation = Minecraft.getMinecraft().world.getTotalWorldTime() + (double) Minecraft.getMinecraft().getRenderPartialTicks();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        int numItems = abilityCount;
        if (numItems <= 0) {
            if (closing) { doneClosing = true; }
            return;
        }

        final float OPEN_ANIMATION_LENGTH = 2.5f;

        float openAnimation = closing
                ? (float) (1 - ((Minecraft.getMinecraft().world.getTotalWorldTime() + partialTicks - startAnimation) / OPEN_ANIMATION_LENGTH))
                : (float) ((Minecraft.getMinecraft().world.getTotalWorldTime() + partialTicks - startAnimation) / OPEN_ANIMATION_LENGTH);

        if (closing && openAnimation <= 0) { doneClosing = true; }

        float animProgress = MathHelper.clamp(openAnimation, 0, 1);
        float radiusIn = Math.max(0.1f, 30 * animProgress);
        float radiusOut = radiusIn * 2;
        float itemRadius = (radiusIn + radiusOut) * 0.5f;
        float animTop = (1 - animProgress) * height / 2.0f;

        int x = width / 2;
        int y = height / 2;

        double a = Math.toDegrees(Math.atan2(mouseY - y, mouseX - x));
        double d = Math.sqrt(Math.pow(mouseX - x, 2) + Math.pow(mouseY - y, 2));
        float s0 = (((0 - 0.5f) / (float) numItems) + 0.25f) * 360;
        if (a < s0) { a += 360; }

        GlStateManager.pushMatrix();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GlStateManager.translate(0, animTop, 0);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        boolean hasMouseOver = false;
        IBirthsignActiveAbility abilityOver = null;

        if (!closing) {
            selectedItem = -1;
            for (int i = 0; i < numItems; i++) {
                float s = (((i - 0.5f) / (float) numItems) + 0.25f) * 360;
                float e = (((i + 0.5f) / (float) numItems) + 0.25f) * 360;
                if (a >= s && a < e && d >= radiusIn && d < radiusOut) {
                    selectedItem = i;
                    break;
                }
            }
        }

        for (int i = 0; i < numItems; i++) {
            float s = (((i - 0.5f) / (float) numItems) + 0.25f) * 360;
            float e = (((i + 0.5f) / (float) numItems) + 0.25f) * 360;
            if (selectedItem == i && i >= 0) {
                abilityOver = abilities.get(i);
                
                // Draw highlighted segment
                drawPieArc(buffer, x, y, zLevel, radiusIn, radiusOut, s, e, 100, 255, 100, 128); // Greenish highlight
                hasMouseOver = true;

            } else {
                // Draw normal segment
                // If it's the currently selected ability (but not mouse over), maybe highlight slightly?
                if (i == currentIndex) {
                    drawPieArc(buffer, x, y, zLevel, radiusIn, radiusOut, s, e, 100, 100, 255, 128); // Blueish for current active
                } else {
                    drawPieArc(buffer, x, y, zLevel, radiusIn, radiusOut, s, e, 0, 0, 0, 64); // Black transparent
                }
            }
        }

        tessellator.draw();
        GlStateManager.enableTexture2D();

        RenderHelper.enableGUIStandardItemLighting();
        for (int index = 0; index < abilityCount; index++) {
            float angle1 = ((index / (float) abilityCount) + 0.25f) * 2 * (float) Math.PI;
            float centerX = x + itemRadius * (float) Math.cos(angle1);
            float centerY = y + itemRadius * (float) Math.sin(angle1);

            GlStateManager.pushMatrix();
            //GlStateManager.scale(0.5, 0.5, 0.5); // Maybe no scale? The ref used scale.
            
            IBirthsignActiveAbility ability = abilities.get(index);
            ResourceLocation icon = ability.getIcon();
            boolean isFallback = false;
            
            if (icon == null) {
                String parentName = ability.getParentName();
                if (parentName != null && !parentName.isEmpty()) {
                     String birthsignKey = parentName;
                    if (parentName.contains(":")) {
                        birthsignKey = parentName.split(":")[1];
                    }
                    icon = new ResourceLocation(Menhir.MODID, "textures/birthsigns/" + birthsignKey + ".png");
                    isFallback = true;
                }
            }
            
            if (icon == null) icon = UNKNOWN_ICON;

            int size = isFallback ? 48 : 16;
            float halfSize = size / 2.0f;
            float posX = centerX - halfSize;
            float posY = centerY - halfSize;

            try {
                Minecraft.getMinecraft().renderEngine.bindTexture(icon);
                drawModalRectWithCustomSizedTexture((int)posX, (int)posY, 0, 0, size, size, size, size);
            } catch (Exception e) {
                // Fallback
                 Minecraft.getMinecraft().renderEngine.bindTexture(UNKNOWN_ICON);
                 drawModalRectWithCustomSizedTexture((int)posX, (int)posY, 0, 0, size, size, size, size);
            }
            
            GlStateManager.popMatrix();
        }

        if (hasMouseOver && abilityOver != null) {
            GlStateManager.pushMatrix();
            String name = abilityOver.getUnlocalizedName();
            if (!name.isEmpty()) {
                name = I18n.format(name);
            } else {
                name = "Unknown Ability";
            }
            
            // Add charge info
            String category = BirthsignEffectManager.getAbilityCategory(abilityOver);
            int current = BirthsignEffectManager.getBirthsignRemainingCharges(mc.player, category);
            int max = BirthsignEffectManager.getBirthsignMaxCharges(mc.player, category);
            name += " (" + current + "/" + max + ")";
            
            drawCenteredString(fontRenderer, name, width / 2, (height - fontRenderer.FONT_HEIGHT) / 2, 0xFFFFFFFF);
            GlStateManager.color(1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }

        GlStateManager.popMatrix();

        // Mouse clipping
        if (Settings.clientSettings.clip_mouse_to_circle) {
            double scaledX = Mouse.getX() - (mc.displayWidth / 2.0f);
            double scaledY = Mouse.getY() - (mc.displayHeight / 2.0f);
            double distance = Math.sqrt(scaledX * scaledX + scaledY * scaledY);
            double radius = 60.0 * (mc.displayWidth / width); // Approximation

            if (distance > radius) {
                double fixedX = scaledX * radius / distance;
                double fixedY = scaledY * radius / distance;
                Mouse.setCursorPosition((int) (mc.displayWidth / 2 + fixedX), (int) (mc.displayHeight / 2 + fixedY));
            }
        }
    }

    private static final float PRECISION = 5;

    private void drawPieArc(BufferBuilder buffer, float x, float y, float z, float radiusIn, float radiusOut, float startAngle, float endAngle, int r, int g, int b, int a) {
        float angle = endAngle - startAngle;
        int sections = Math.max(1, MathHelper.ceil(angle / PRECISION));

        startAngle = (float) Math.toRadians(startAngle);
        endAngle = (float) Math.toRadians(endAngle);
        angle = endAngle - startAngle;

        for (int i = 0; i < sections; i++) {
            float angle1 = startAngle + (i / (float) sections) * angle;
            float angle2 = startAngle + ((i + 1) / (float) sections) * angle;

            float pos1InX = x + radiusIn * (float) Math.cos(angle1);
            float pos1InY = y + radiusIn * (float) Math.sin(angle1);
            float pos1OutX = x + radiusOut * (float) Math.cos(angle1);
            float pos1OutY = y + radiusOut * (float) Math.sin(angle1);
            float pos2OutX = x + radiusOut * (float) Math.cos(angle2);
            float pos2OutY = y + radiusOut * (float) Math.sin(angle2);
            float pos2InX = x + radiusIn * (float) Math.cos(angle2);
            float pos2InY = y + radiusIn * (float) Math.sin(angle2);

            buffer.pos(pos1OutX, pos1OutY, z).color(r, g, b, a).endVertex();
            buffer.pos(pos1InX, pos1InY, z).color(r, g, b, a).endVertex();
            buffer.pos(pos2InX, pos2InY, z).color(r, g, b, a).endVertex();
            buffer.pos(pos2OutX, pos2OutY, z).color(r, g, b, a).endVertex();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

/*
The code above is a modified version of David Quintana <gigaherz@gmail.com>'s solution.

Copyright (c) 2015, David Quintana <gigaherz@gmail.com>
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the author nor the
      names of the contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
* */