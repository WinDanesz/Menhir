package com.windanesz.menhir.client.gui;

import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.client.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiBirthsign extends GuiScreen {

	private static final int GUI_WIDTH = 256;
	private static final int MIN_GUI_HEIGHT = 200;
	private static final int MAX_GUI_HEIGHT = 300;
	private static final int PADDING = 10;
	private static final int LINE_HEIGHT = 10;
	private static final String ACTIVE_CHARGES_KEY = "birthsign_remaining_charges";
	private static final String PASSIVE_CHARGES_KEY = "birthsign_remaining_passive_charges";
	private static final int BUTTON_WIDTH = 20;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ICON_SIZE = 32;  // Size of birthsign constellation icons
	private static final int SELECT_BUTTON_WIDTH = 120;
	private static final int SELECT_BUTTON_HEIGHT = 20;

	private final EntityPlayer player;
	private Birthsign birthsign;
	private IBirthsignData birthsignData;
	private int guiLeft;
	private int guiTop;
	private int currentBirthsignIndex = 0;
	private List<Birthsign> allBirthsigns;
	private boolean browsingMode = false;
	private boolean selectionMode = false;  // New field for birthsign selection on first spawn
	private int dynamicGuiHeight = MIN_GUI_HEIGHT;
	
	// Cache for custom textures loaded from config folder
	private static final Map<String, ResourceLocation> customTextureCache = new HashMap<>();
	private static final String CONFIG_TEXTURE_PATH = "config/menhir/textures/";

	public GuiBirthsign(EntityPlayer player) {
		this(player, false);
	}

	public GuiBirthsign(EntityPlayer player, boolean selectionMode) {
		this.player = player;
		this.selectionMode = selectionMode;
		this.birthsignData = BirthsignDataProvider.get(player);
		// Get all available birthsigns for browsing
		this.allBirthsigns = new ArrayList<>();
		for (Birthsign birthsign : Birthsign.registry.getValues()) {
			this.allBirthsigns.add(birthsign);
		}
		
		// In selection mode, always start browsing
		if (selectionMode) {
			this.browsingMode = true;
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		
		// Only initialize the birthsign index if this is the first time opening the GUI
		// (not a resize event). Check if currentBirthsignIndex is still at default value.
		if (currentBirthsignIndex == 0 && !browsingMode) {
			// Find current birthsign index if player has one
			if (birthsignData != null) {
				String birthsignName = birthsignData.getBirthsign();
				if (birthsignName != null && !birthsignName.isEmpty()) {
					Birthsign playerBirthsign = Birthsign.getBirthsignFromString(birthsignName);
					if (playerBirthsign != null) {
						int playerIndex = allBirthsigns.indexOf(playerBirthsign);
						if (playerIndex != -1) {
							currentBirthsignIndex = playerIndex;
						}
					}
				}
			}
		}
		// If browsingMode is true or currentBirthsignIndex has been changed,
		// preserve the current state (don't reset to player's birthsign)
		
		// Update birthsign data BEFORE calculating height to ensure correct sizing
		updateBirthsignData();
		
		// Calculate dynamic height based on current birthsign content
		calculateDynamicHeight();
		this.guiLeft = (this.width - GUI_WIDTH) / 2;
		this.guiTop = (this.height - dynamicGuiHeight) / 2;
	}

	private void updateBirthsignData() {
		// In selection mode or browsing mode, show the currently selected birthsign from the list
		if ((selectionMode || browsingMode) && !allBirthsigns.isEmpty()) {
			this.birthsign = allBirthsigns.get(currentBirthsignIndex);
			// Recalculate height when birthsign changes
			calculateDynamicHeight();
			this.guiTop = (this.height - dynamicGuiHeight) / 2;
			return;
		}
		
		// Get the player's birthsign from capability data
		if (birthsignData != null) {
			String birthsignName = birthsignData.getBirthsign();
			if (birthsignName != null && !birthsignName.isEmpty()) {
				Birthsign newBirthsign = Birthsign.getBirthsignFromString(birthsignName);
				// Only update if it changed (to avoid spam in logs)
				if (this.birthsign != newBirthsign) {
					this.birthsign = newBirthsign;
				}
			} else if (this.birthsign != null) {
				// Birthsign was cleared
				this.birthsign = null;
			}
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		// Update birthsign data from capability (in case packet arrived)
		updateBirthsignData();

		// Draw background
		this.drawDefaultBackground();

		// Draw GUI background with rounded corners
		drawRoundedRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + dynamicGuiHeight, 2, 0xC0101010);
		drawRoundedRect(guiLeft + 2, guiTop + 2, guiLeft + GUI_WIDTH - 2, guiTop + dynamicGuiHeight - 2, 2, 0xC0202020);

		int yOffset = guiTop + PADDING;
		int xOffset = guiLeft + PADDING;

		if (birthsign == null) {
			// No birthsign assigned
			String noBirthsign = I18n.format("gui.menhir.birthsign.no_birthsign");
			this.drawCenteredString(Minecraft.getMinecraft().fontRenderer, noBirthsign, guiLeft + GUI_WIDTH / 2, yOffset, 0xFFFFFF);
		} else {
			// Extract birthsign name for localization
			String birthsignKey = birthsign.getRegistryName().getPath();

			// Draw constellation icon centered above the title
			// This will check config folder first, then fall back to resource pack
			ResourceLocation iconTexture = getBirthsignTexture(birthsignKey);
			int iconX = guiLeft + (GUI_WIDTH - ICON_SIZE) / 2;
			int iconY = yOffset;
			drawBirthsignIcon(iconTexture, iconX, iconY);
			yOffset += ICON_SIZE + 5;

			// Draw birthsign name (title)
			String birthsignName = I18n.format("birthsign." + birthsignKey + ".name");
			this.drawCenteredString(Minecraft.getMinecraft().fontRenderer, birthsignName, guiLeft + GUI_WIDTH / 2, yOffset, 0xFFD700);
			yOffset += LINE_HEIGHT + 5;
			
			// If browsing and this is not the player's birthsign, show a note
			if (browsingMode && !isPlayerBirthsign(birthsign)) {
				String notYourBirthsign = I18n.format("gui.menhir.birthsign.not_yours");
				this.drawCenteredString(Minecraft.getMinecraft().fontRenderer, notYourBirthsign, guiLeft + GUI_WIDTH / 2, yOffset, 0xFF8888);
				yOffset += LINE_HEIGHT + 5;
			}

			// Draw description
			String description = I18n.format("birthsign." + birthsignKey + ".desc");
			List<String> descLines = wrapText(description, GUI_WIDTH - 2 * PADDING);
			for (String line : descLines) {
				this.drawString(Minecraft.getMinecraft().fontRenderer, line, xOffset, yOffset, 0xCCCCCC);
				yOffset += LINE_HEIGHT;
			}
			yOffset += 5;

			// Draw lore
			String lore = I18n.format("birthsign." + birthsignKey + ".lore");
			List<String> loreLines = wrapText(lore, GUI_WIDTH - 2 * PADDING);
			for (String line : loreLines) {
				this.drawString(Minecraft.getMinecraft().fontRenderer, line, xOffset, yOffset, 0xAAAAFF);
				yOffset += LINE_HEIGHT;
			}
			yOffset += 10;

			// Draw passive abilities
			if (birthsign.passive != null && !birthsign.passive.isEmpty()) {
				String passiveTitle = I18n.format("gui.menhir.birthsign.passive_abilities");
				this.drawString(Minecraft.getMinecraft().fontRenderer, passiveTitle, xOffset, yOffset, 0x55FF55);
				yOffset += LINE_HEIGHT;

				String langKey = "birthsign." + birthsignKey + ".passive";
				String effectDesc = I18n.format(langKey);
				List<String> effectLines = wrapText(effectDesc, GUI_WIDTH - 2 * PADDING - 10);
				for (String line : effectLines) {
					this.drawString(Minecraft.getMinecraft().fontRenderer, "  " + line, xOffset, yOffset, 0xAAAAAA);
					yOffset += LINE_HEIGHT;
				}
				
				// Draw passive charges if applicable
				if (birthsign.passive_daily_uses > 0) {
					String chargesText;
					if (browsingMode && !isPlayerBirthsign(birthsign)) {
						// Browsing another birthsign - show max daily charges
						chargesText = I18n.format("gui.menhir.birthsign.daily_charges", birthsign.passive_daily_uses);
					} else {
						// Player's own birthsign - show remaining/max
						int remainingCharges = birthsignData != null ? birthsignData.getInt(PASSIVE_CHARGES_KEY) : 0;
						chargesText = I18n.format("gui.menhir.birthsign.charges", remainingCharges, birthsign.passive_daily_uses);
					}
					this.drawString(Minecraft.getMinecraft().fontRenderer, "  " + chargesText, xOffset, yOffset, 0xFFFF55);
					yOffset += LINE_HEIGHT;
				}
				yOffset += 5;
			}

			// Draw active abilities
			if (birthsign.active != null && !birthsign.active.isEmpty()) {
				String activeTitle = I18n.format("gui.menhir.birthsign.active_abilities");
				this.drawString(Minecraft.getMinecraft().fontRenderer, activeTitle, xOffset, yOffset, 0xFFAA00);
				yOffset += LINE_HEIGHT;

				String langKey = "birthsign." + birthsignKey + ".active";
				String effectDesc = I18n.format(langKey);
				List<String> effectLines = wrapText(effectDesc, GUI_WIDTH - 2 * PADDING - 10);
				for (String line : effectLines) {
					this.drawString(Minecraft.getMinecraft().fontRenderer, "  " + line, xOffset, yOffset, 0xAAAAAA);
					yOffset += LINE_HEIGHT;
				}
				
				// Draw active charges if applicable
				if (birthsign.active_daily_uses > 0) {
					String chargesText;
					if (browsingMode && !isPlayerBirthsign(birthsign)) {
						// Browsing another birthsign - show max daily charges
						chargesText = I18n.format("gui.menhir.birthsign.daily_charges", birthsign.active_daily_uses);
					} else {
						// Player's own birthsign - show remaining/max
						int remainingCharges = birthsignData != null ? birthsignData.getInt(ACTIVE_CHARGES_KEY) : 0;
						chargesText = I18n.format("gui.menhir.birthsign.charges", remainingCharges, birthsign.active_daily_uses);
					}
					this.drawString(Minecraft.getMinecraft().fontRenderer, "  " + chargesText, xOffset, yOffset, 0xFFFF55);
					yOffset += LINE_HEIGHT;
				}
				yOffset += 5;
			}
		}

		// Draw navigation buttons if there are multiple birthsigns
		if (!allBirthsigns.isEmpty() && allBirthsigns.size() > 1) {
			drawNavigationButtons(mouseX, mouseY);
		}

		// Draw selection button if player doesn't have a birthsign yet
		if (birthsign != null && !hasPlayerBirthsign()) {
			drawSelectionButton(mouseX, mouseY);
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private void drawNavigationButtons(int mouseX, int mouseY) {
		// Left button
		int leftButtonX = guiLeft - BUTTON_WIDTH - 5;
		int leftButtonY = guiTop + (dynamicGuiHeight / 2) - (BUTTON_HEIGHT / 2);
		boolean leftHovered = mouseX >= leftButtonX && mouseX <= leftButtonX + BUTTON_WIDTH &&
				mouseY >= leftButtonY && mouseY <= leftButtonY + BUTTON_HEIGHT;
		
		// Right button
		int rightButtonX = guiLeft + GUI_WIDTH + 5;
		int rightButtonY = guiTop + (dynamicGuiHeight / 2) - (BUTTON_HEIGHT / 2);
		boolean rightHovered = mouseX >= rightButtonX && mouseX <= rightButtonX + BUTTON_WIDTH &&
				mouseY >= rightButtonY && mouseY <= rightButtonY + BUTTON_HEIGHT;
		
		// Draw left button
		drawRect(leftButtonX, leftButtonY, leftButtonX + BUTTON_WIDTH, leftButtonY + BUTTON_HEIGHT, 
				leftHovered ? 0xFF555555 : 0xFF333333);
		drawRect(leftButtonX + 1, leftButtonY + 1, leftButtonX + BUTTON_WIDTH - 1, leftButtonY + BUTTON_HEIGHT - 1, 
				leftHovered ? 0xFF444444 : 0xFF222222);
		// Draw left arrow
		this.drawCenteredString(Minecraft.getMinecraft().fontRenderer, "<", leftButtonX + BUTTON_WIDTH / 2, leftButtonY + 6, 0xFFFFFF);
		
		// Draw right button
		drawRect(rightButtonX, rightButtonY, rightButtonX + BUTTON_WIDTH, rightButtonY + BUTTON_HEIGHT, 
				rightHovered ? 0xFF555555 : 0xFF333333);
		drawRect(rightButtonX + 1, rightButtonY + 1, rightButtonX + BUTTON_WIDTH - 1, rightButtonY + BUTTON_HEIGHT - 1, 
				rightHovered ? 0xFF444444 : 0xFF222222);
		// Draw right arrow
		this.drawCenteredString(Minecraft.getMinecraft().fontRenderer, ">", rightButtonX + BUTTON_WIDTH / 2, rightButtonY + 6, 0xFFFFFF);
	}

	private void drawSelectionButton(int mouseX, int mouseY) {
		int buttonX = guiLeft + (GUI_WIDTH - SELECT_BUTTON_WIDTH) / 2;
		int buttonY = guiTop + dynamicGuiHeight - SELECT_BUTTON_HEIGHT - 10; // 10 pixels from bottom
		boolean hovered = mouseX >= buttonX && mouseX <= buttonX + SELECT_BUTTON_WIDTH &&
				mouseY >= buttonY && mouseY <= buttonY + SELECT_BUTTON_HEIGHT;

		// Draw button border (darker)
		drawRect(buttonX - 1, buttonY - 1, buttonX + SELECT_BUTTON_WIDTH + 1, buttonY + SELECT_BUTTON_HEIGHT + 1,
				hovered ? 0xFF2A7A2A : 0xFF1A5A1A);
		
		// Draw button background
		drawRect(buttonX, buttonY, buttonX + SELECT_BUTTON_WIDTH, buttonY + SELECT_BUTTON_HEIGHT,
				hovered ? 0xFF55AA55 : 0xFF33AA33);
		drawRect(buttonX + 1, buttonY + 1, buttonX + SELECT_BUTTON_WIDTH - 1, buttonY + SELECT_BUTTON_HEIGHT - 1,
				hovered ? 0xFF44AA44 : 0xFF22AA22);

		// Draw button text
		String buttonText = I18n.format("gui.menhir.birthsign.select");
		this.drawCenteredString(Minecraft.getMinecraft().fontRenderer, buttonText,
				buttonX + SELECT_BUTTON_WIDTH / 2, buttonY + 6, 0xFFFFFF);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		
		if (mouseButton == 0) {
			// Check selection button click (only if player doesn't have a birthsign yet)
			if (birthsign != null && !hasPlayerBirthsign()) {
				int buttonX = guiLeft + (GUI_WIDTH - SELECT_BUTTON_WIDTH) / 2;
				int buttonY = guiTop + dynamicGuiHeight - SELECT_BUTTON_HEIGHT - 10; // 10 pixels from bottom

				if (mouseX >= buttonX && mouseX <= buttonX + SELECT_BUTTON_WIDTH &&
						mouseY >= buttonY && mouseY <= buttonY + SELECT_BUTTON_HEIGHT) {
					// Player selected this birthsign
					selectBirthsign();
					return;
				}
			}

			// Navigation buttons
			if (!allBirthsigns.isEmpty() && allBirthsigns.size() > 1) {
				// Left button
				int leftButtonX = guiLeft - BUTTON_WIDTH - 5;
				int leftButtonY = guiTop + (dynamicGuiHeight / 2) - (BUTTON_HEIGHT / 2);
				
				if (mouseX >= leftButtonX && mouseX <= leftButtonX + BUTTON_WIDTH &&
						mouseY >= leftButtonY && mouseY <= leftButtonY + BUTTON_HEIGHT) {
					// Previous birthsign
					currentBirthsignIndex--;
					if (currentBirthsignIndex < 0) {
						currentBirthsignIndex = allBirthsigns.size() - 1;
					}
					browsingMode = true;
					return;
				}
				
				// Right button
				int rightButtonX = guiLeft + GUI_WIDTH + 5;
				int rightButtonY = guiTop + (dynamicGuiHeight / 2) - (BUTTON_HEIGHT / 2);
				
				if (mouseX >= rightButtonX && mouseX <= rightButtonX + BUTTON_WIDTH &&
						mouseY >= rightButtonY && mouseY <= rightButtonY + BUTTON_HEIGHT) {
					// Next birthsign
					currentBirthsignIndex++;
					if (currentBirthsignIndex >= allBirthsigns.size()) {
						currentBirthsignIndex = 0;
					}
					browsingMode = true;
					return;
				}
			}
		}
	}

	private List<String> wrapText(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		String[] words = text.split(" ");
		StringBuilder currentLine = new StringBuilder();

		for (String word : words) {
			String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
			if (Minecraft.getMinecraft().fontRenderer.getStringWidth(testLine) <= maxWidth) {
				if (currentLine.length() > 0) {
					currentLine.append(" ");
				}
				currentLine.append(word);
			} else {
				if (currentLine.length() > 0) {
					lines.add(currentLine.toString());
					currentLine = new StringBuilder(word);
				} else {
					lines.add(word);
				}
			}
		}

		if (currentLine.length() > 0) {
			lines.add(currentLine.toString());
		}

		return lines;
	}

	private void calculateDynamicHeight() {
		int requiredHeight = PADDING; // Start with top padding
		
		if (birthsign == null) {
			// Just need space for "No Birthsign" message
			requiredHeight += LINE_HEIGHT + PADDING;
		} else {
			String birthsignKey = birthsign.getRegistryName().getPath();
			
			// Constellation icon
			requiredHeight += ICON_SIZE + 5;
			
			// Birthsign name
			requiredHeight += LINE_HEIGHT + 5;
			
			// "Not your birthsign" message if applicable
			if (browsingMode && !isPlayerBirthsign(birthsign)) {
				requiredHeight += LINE_HEIGHT + 5;
			}
			
			// Description
			String description = I18n.format("birthsign." + birthsignKey + ".desc");
			List<String> descLines = wrapText(description, GUI_WIDTH - 2 * PADDING);
			requiredHeight += descLines.size() * LINE_HEIGHT + 5;
			
			// Lore
			String lore = I18n.format("birthsign." + birthsignKey + ".lore");
			List<String> loreLines = wrapText(lore, GUI_WIDTH - 2 * PADDING);
			requiredHeight += loreLines.size() * LINE_HEIGHT + 10;
			
			// Passive abilities
			if (birthsign.passive != null && !birthsign.passive.isEmpty()) {
				requiredHeight += LINE_HEIGHT; // Title
				String langKey = "birthsign." + birthsignKey + ".passive";
				String effectDesc = I18n.format(langKey);
				List<String> effectLines = wrapText(effectDesc, GUI_WIDTH - 2 * PADDING - 10);
				requiredHeight += effectLines.size() * LINE_HEIGHT;
				if (birthsign.passive_daily_uses > 0) {
					requiredHeight += LINE_HEIGHT; // Charges
				}
				requiredHeight += 5;
			}
			
			// Active abilities
			if (birthsign.active != null && !birthsign.active.isEmpty()) {
				requiredHeight += LINE_HEIGHT; // Title
				String langKey = "birthsign." + birthsignKey + ".active";
				String effectDesc = I18n.format(langKey);
				List<String> effectLines = wrapText(effectDesc, GUI_WIDTH - 2 * PADDING - 10);
				requiredHeight += effectLines.size() * LINE_HEIGHT;
				if (birthsign.active_daily_uses > 0) {
					requiredHeight += LINE_HEIGHT; // Charges
				}
				requiredHeight += 5;
			}
		}
		
		requiredHeight += PADDING; // Bottom padding
		
		// Add extra space for the selection button if player doesn't have a birthsign
		if (!hasPlayerBirthsign()) {
			requiredHeight += SELECT_BUTTON_HEIGHT + 10; // Button height + 10px above + 10px below
		}
		
		// Clamp to min/max bounds
		dynamicGuiHeight = Math.max(MIN_GUI_HEIGHT, Math.min(MAX_GUI_HEIGHT, requiredHeight));
	}

	private boolean isPlayerBirthsign(Birthsign birthsign) {
		if (birthsignData == null || birthsign == null) {
			return false;
		}
		String playerBirthsignName = birthsignData.getBirthsign();
		if (playerBirthsignName == null || playerBirthsignName.isEmpty()) {
			return false;
		}
		Birthsign playerBirthsign = Birthsign.getBirthsignFromString(playerBirthsignName);
		return birthsign == playerBirthsign;
	}

	/**
	 * Checks if the player has any birthsign assigned
	 */
	private boolean hasPlayerBirthsign() {
		if (birthsignData == null) {
			return false;
		}
		String playerBirthsignName = birthsignData.getBirthsign();
		return playerBirthsignName != null && !playerBirthsignName.isEmpty();
	}

	/**
	 * Draws a birthsign constellation icon
	 */
	private void drawBirthsignIcon(ResourceLocation texture, int x, int y) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		
		// Draw the 32x32 texture
		drawModalRectWithCustomSizedTexture(x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        
		// Add a subtle glowing, twinkling halo effect to emphasize yellow (star) pixels.
		// This uses several additive, slightly-scaled passes of the same texture with a yellow tint
		// so the bright (yellow) parts bloom while darker/gray parts stay mostly unchanged.
		try {
			// Use DST_COLOR blend to multiply - this makes dark pixels stay dark
			// Only bright pixels will contribute to the glow effect
			GlStateManager.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ONE);
			// Slight pulsation for life
			long ms = System.currentTimeMillis();
			float pulse = 0.75f + 0.25f * (float)Math.sin(ms / 1200.0 * Math.PI * 2.0);

			// Scales and alpha for multiple blur-like passes (larger scale => softer, lower alpha)
			float[] scales = new float[] { 1.08f, 1.22f, 1.45f };
			float[] baseAlphas = new float[] { 0.18f, 0.12f, 0.06f };

			// Center the draws on the icon
			int cx = x + ICON_SIZE / 2;
			int cy = y + ICON_SIZE / 2;

			for (int i = 0; i < scales.length; i++) {
				float s = scales[i];
				float a = baseAlphas[i] * pulse;

				GlStateManager.pushMatrix();
				// Translate to center, scale, then draw centered quad
				GlStateManager.translate((float)cx, (float)cy, 0.0F);
				GlStateManager.scale(s, s, 1.0F);

				// Yellow-ish tint with higher intensity to only affect bright pixels
				// Dim pixels multiplied by this will stay dim
				GlStateManager.color(1.5F, 1.4F, 0.8F, a);

				// draw centered
				drawModalRectWithCustomSizedTexture(-ICON_SIZE / 2, -ICON_SIZE / 2, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
				GlStateManager.popMatrix();
			}

			// Restore blend to standard alpha blending for subsequent GUI elements
			GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		} catch (Exception e) {
			// If anything goes wrong here we silently fall back to the plain icon
			// (prevent GUI from crashing due to unexpected GL state)
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		}

		GlStateManager.disableBlend();
	}
	
	/**
	 * Gets the texture for a birthsign, checking config folder first, then falling back to resource pack.
	 * Config folder path: config/menhir/textures/birthsigns/{birthsignKey}.png
	 * 
	 * @param birthsignKey The registry key of the birthsign (e.g., "the_warrior")
	 * @return ResourceLocation for the texture
	 */
	private ResourceLocation getBirthsignTexture(String birthsignKey) {
		// Check if we already loaded this custom texture
		if (customTextureCache.containsKey(birthsignKey)) {
			return customTextureCache.get(birthsignKey);
		}
		
		// Try to load from config folder
		File configTexture = new File(CONFIG_TEXTURE_PATH + birthsignKey + ".png");
		if (configTexture.exists() && configTexture.isFile()) {
			try {
				BufferedImage image = ImageIO.read(configTexture);
				if (image != null) {
					// Create a dynamic texture from the image
					DynamicTexture dynamicTexture = new DynamicTexture(image);
					ResourceLocation customLocation = Minecraft.getMinecraft().getTextureManager()
							.getDynamicTextureLocation("menhir_custom_birthsign_" + birthsignKey, dynamicTexture);
					
					// Cache it for future use
					customTextureCache.put(birthsignKey, customLocation);
					return customLocation;
				}
			} catch (IOException e) {
				// Log error but continue to fallback
				System.err.println("Failed to load custom birthsign texture from " + configTexture.getAbsolutePath() + ": " + e.getMessage());
			}
		}
		
		// Fallback to built-in resource pack texture
		return new ResourceLocation("menhir", "textures/birthsigns/" + birthsignKey + ".png");
	}

	/**
	 * Draws a rectangle with rounded corners
	 * @param left Left x coordinate
	 * @param top Top y coordinate
	 * @param right Right x coordinate
	 * @param bottom Bottom y coordinate
	 * @param radius Corner radius in pixels
	 * @param color Color in ARGB format
	 */
	private void drawRoundedRect(int left, int top, int right, int bottom, int radius, int color) {
		// Draw main body (center rectangle)
		drawRect(left + radius, top, right - radius, bottom, color);
		
		// Draw left and right edges
		drawRect(left, top + radius, left + radius, bottom - radius, color);
		drawRect(right - radius, top + radius, right, bottom - radius, color);
		
		// Draw top and bottom edges (excluding corners)
		drawRect(left + radius, top, right - radius, top + radius, color);
		drawRect(left + radius, bottom - radius, right - radius, bottom, color);
		
		// Draw corner pixels to create rounded effect
		// Top-left corner
		for (int i = 0; i < radius; i++) {
			for (int j = 0; j < radius; j++) {
				if (i * i + j * j <= radius * radius) {
					drawRect(left + i, top + j, left + i + 1, top + j + 1, color);
				}
			}
		}
		
		// Top-right corner
		for (int i = 0; i < radius; i++) {
			for (int j = 0; j < radius; j++) {
				if (i * i + j * j <= radius * radius) {
					drawRect(right - i - 1, top + j, right - i, top + j + 1, color);
				}
			}
		}
		
		// Bottom-left corner
		for (int i = 0; i < radius; i++) {
			for (int j = 0; j < radius; j++) {
				if (i * i + j * j <= radius * radius) {
					drawRect(left + i, bottom - j - 1, left + i + 1, bottom - j, color);
				}
			}
		}
		
		// Bottom-right corner
		for (int i = 0; i < radius; i++) {
			for (int j = 0; j < radius; j++) {
				if (i * i + j * j <= radius * radius) {
					drawRect(right - i - 1, bottom - j - 1, right - i, bottom - j, color);
				}
			}
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		// In selection mode, only allow closing with ESC
		if (selectionMode) {
			if (keyCode == Keyboard.KEY_ESCAPE) {
				// Show confirmation or just close
				this.mc.displayGuiScreen(null);
				if (this.mc.currentScreen == null) {
					this.mc.setIngameFocus();
				}
			}
			return;
		}
		
		// Allow closing with ESC, inventory key, or the same key that opens the GUI
		if (keyCode == Keyboard.KEY_ESCAPE 
			|| keyCode == Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode()
			|| (ClientProxy.KEY_SHOW_BIRTHSIGN != null && keyCode == ClientProxy.KEY_SHOW_BIRTHSIGN.getKeyCode())) {
			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null) {
				this.mc.setIngameFocus();
			}
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return selectionMode; // Pause game during birthsign selection
	}

	private void selectBirthsign() {
		if (birthsign != null && player != null) {
			// Send packet to server to set birthsign
			String birthsignName = birthsign.getRegistryName().toString();
			com.windanesz.menhir.network.NetworkHandler.INSTANCE.sendToServer(
				new com.windanesz.menhir.network.PacketSetBirthsign(birthsignName)
			);
			
			// Show confirmation message
			String birthsignDisplayName = I18n.format("birthsign." + birthsign.getRegistryName().getPath() + ".name");
			String message = I18n.format("gui.menhir.birthsign.selected", birthsignDisplayName);
			player.sendMessage(new net.minecraft.util.text.TextComponentString(message));
			
			// Close GUI
			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null) {
				this.mc.setIngameFocus();
			}
		}
	}
}
