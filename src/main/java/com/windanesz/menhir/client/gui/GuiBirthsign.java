package com.windanesz.menhir.client.gui;

import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiBirthsign extends GuiScreen {

	private static final int GUI_WIDTH = 256;
	private static final int GUI_HEIGHT = 200;
	private static final int PADDING = 10;
	private static final int LINE_HEIGHT = 10;

	private final EntityPlayer player;
	private Birthsign birthsign;
	private IBirthsignData birthsignData;
	private int guiLeft;
	private int guiTop;

	public GuiBirthsign(EntityPlayer player) {
		this.player = player;
		this.birthsignData = BirthsignDataProvider.get(player);
	}

	@Override
	public void initGui() {
		super.initGui();
		this.guiLeft = (this.width - GUI_WIDTH) / 2;
		this.guiTop = (this.height - GUI_HEIGHT) / 2;

		// Get the player's birthsign
		if (birthsignData != null) {
			String birthsignName = birthsignData.getBirthsign();
			if (birthsignName != null && !birthsignName.isEmpty()) {
				this.birthsign = Birthsign.getBirthsignFromString(birthsignName);
			}
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		// Draw background
		this.drawDefaultBackground();

		// Draw GUI background
		drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xC0101010);
		drawRect(guiLeft + 2, guiTop + 2, guiLeft + GUI_WIDTH - 2, guiTop + GUI_HEIGHT - 2, 0xC0202020);

		int yOffset = guiTop + PADDING;
		int xOffset = guiLeft + PADDING;

		if (birthsign == null) {
			// No birthsign assigned
			String noBirthsign = I18n.format("gui.menhir.birthsign.no_birthsign");
			this.drawCenteredString(fontRenderer, noBirthsign, guiLeft + GUI_WIDTH / 2, yOffset, 0xFFFFFF);
		} else {
			// Extract birthsign name for localization
			String birthsignKey = birthsign.getRegistryName().getPath();

			// Draw birthsign name (title)
			String birthsignName = I18n.format("birthsign." + birthsignKey + ".name");
			this.drawCenteredString(fontRenderer, birthsignName, guiLeft + GUI_WIDTH / 2, yOffset, 0xFFD700);
			yOffset += LINE_HEIGHT + 5;

			// Draw description
			String description = I18n.format("birthsign." + birthsignKey + ".desc");
			List<String> descLines = wrapText(description, GUI_WIDTH - 2 * PADDING);
			for (String line : descLines) {
				this.drawString(fontRenderer, line, xOffset, yOffset, 0xCCCCCC);
				yOffset += LINE_HEIGHT;
			}
			yOffset += 5;

			// Draw lore
			String lore = I18n.format("birthsign." + birthsignKey + ".lore");
			List<String> loreLines = wrapText(lore, GUI_WIDTH - 2 * PADDING);
			for (String line : loreLines) {
				this.drawString(fontRenderer, line, xOffset, yOffset, 0xAAAAFF);
				yOffset += LINE_HEIGHT;
			}
			yOffset += 10;

			// Draw passive abilities
			if (birthsign.passive != null && !birthsign.passive.isEmpty()) {
				String passiveTitle = I18n.format("gui.menhir.birthsign.passive_abilities");
				this.drawString(fontRenderer, passiveTitle, xOffset, yOffset, 0x55FF55);
				yOffset += LINE_HEIGHT;

				for (Birthsign.BirthsignEffect effect : birthsign.passive) {
					String effectDesc = getEffectDescription(effect);
					this.drawString(fontRenderer, "  • " + effectDesc, xOffset, yOffset, 0xAAAAAA);
					yOffset += LINE_HEIGHT;
				}
				yOffset += 5;
			}

			// Draw active abilities
			if (birthsign.active != null && !birthsign.active.isEmpty()) {
				String activeTitle = I18n.format("gui.menhir.birthsign.active_abilities");
				this.drawString(fontRenderer, activeTitle, xOffset, yOffset, 0xFFAA00);
				yOffset += LINE_HEIGHT;

				for (Birthsign.BirthsignEffect effect : birthsign.active) {
					String effectDesc = getEffectDescription(effect);
					this.drawString(fontRenderer, "  • " + effectDesc, xOffset, yOffset, 0xAAAAAA);
					yOffset += LINE_HEIGHT;
				}
				yOffset += 5;
			}

			// Draw available charges
			if (birthsign.active_daily_uses > 0) {
				int usedCharges = birthsignData != null ? birthsignData.getInt("active_uses") : 0;
				int availableCharges = birthsign.active_daily_uses - usedCharges;
				String chargesText = I18n.format("gui.menhir.birthsign.charges", availableCharges, birthsign.active_daily_uses);
				this.drawString(fontRenderer, chargesText, xOffset, yOffset, 0xFFFF55);
			}
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private List<String> wrapText(String text, int maxWidth) {
		List<String> lines = new ArrayList<>();
		String[] words = text.split(" ");
		StringBuilder currentLine = new StringBuilder();

		for (String word : words) {
			String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
			if (fontRenderer.getStringWidth(testLine) <= maxWidth) {
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

	private String getEffectDescription(Birthsign.BirthsignEffect effect) {
		if (effect == null || effect.effect == null) {
			return "Unknown Effect";
		}

		Birthsign.EffectType type = effect.effect.type;
		if (type == null) {
			return "Unknown Effect";
		}

		switch (type) {
			case ATTRIBUTE_MODIFIER:
				String attribute = effect.effect.getParameter("attribute", "unknown");
				Number amount = effect.effect.getParameter("amount", 0.0);
				Integer operation = effect.effect.getParameter("operation", 0);
				
				String attrName = attribute.replace("_", " ");
				String sign = amount.doubleValue() >= 0 ? "+" : "";
				
				if (operation == 0) {
					// Additive
					return sign + amount + " " + attrName;
				} else if (operation == 1) {
					// Multiplicative
					return sign + (amount.doubleValue() * 100) + "% " + attrName;
				} else {
					return sign + amount + " " + attrName;
				}

			case POTION_EFFECT:
				String potionName = effect.effect.getParameter("potioneffect", "unknown");
				Integer amplifier = effect.effect.getParameter("amplifier", 0);
				Integer duration = effect.effect.getParameter("duration", 0);
				
				// Clean up potion name
				if (potionName.contains(":")) {
					potionName = potionName.split(":")[1];
				}
				potionName = potionName.replace("_", " ");
				
				int level = amplifier + 1;
				int seconds = duration / 20;
				return potionName + " " + level + " (" + seconds + "s)";

			case WIZARDRY_SPELL_MODIFIER:
				String modifierName = effect.effect.getParameter("name", "unknown");
				Number modAmount = effect.effect.getParameter("amount", 0.0);
				return "+" + (modAmount.doubleValue() * 100) + "% " + modifierName;

			default:
				return type.getJsonName().replace("_", " ");
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode()) {
			this.mc.displayGuiScreen(null);
			if (this.mc.currentScreen == null) {
				this.mc.setIngameFocus();
			}
		}
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
