package com.windanesz.menhir.client.renderer.tileentity;

import com.windanesz.menhir.api.altar.AltarDefinition;
import com.windanesz.menhir.api.altar.AltarEffect;
import com.windanesz.menhir.core.AltarRegistry;
import com.windanesz.menhir.tileentity.TileEntityAltar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;

public class RenderTileEntityAltar extends TileEntitySpecialRenderer<TileEntityAltar> {

	@Override
	public void render(TileEntityAltar te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (te == null || te.getAltarId() == null || te.getAltarId().isEmpty()) {
			return;
		}

		AltarDefinition definition = AltarRegistry.getAltarDefinition(te.getAltarId());
		if (definition == null) {
			return;
		}

		EntityPlayer player = Minecraft.getMinecraft().player;
		if (player == null) {
			return;
		}

		// Check if player is close enough to see the text
		double distance = player.getDistanceSq(te.getPos());
		if (distance > 64.0) { // 8 blocks
			return;
		}

		// Determine what name to show (obfuscated or real)
		String displayName;
		boolean isObfuscated = definition.isObfuscated() && !te.hasPlayerIdentified(player.getUniqueID());
		
		if (isObfuscated) {
			displayName = generateObfuscatedName(te.getAltarId());
		} else {
			displayName = definition.getName();
		}

		TextFormatting nameColor = definition.getRarity().getColor();
		
		GlStateManager.pushMatrix();
		
		// Position above the altar (3 blocks tall, so render above the top)
		GlStateManager.translate(x + 0.5, y + 2.5, z + 0.5);
		
		// Face the player
		GlStateManager.rotate(-this.rendererDispatcher.entityYaw, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(this.rendererDispatcher.entityPitch, 1.0F, 0.0F, 0.0F);
		
		// Scale
		float scale = 0.02F;
		GlStateManager.scale(-scale, -scale, scale);
		
		GlStateManager.disableLighting();
		GlStateManager.depthMask(false);
		GlStateManager.disableDepth();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		
		FontRenderer fontRenderer = this.rendererDispatcher.getFontRenderer();
		
		// Draw altar name
		String coloredName = nameColor + displayName;
		int nameWidth = fontRenderer.getStringWidth(coloredName);
		fontRenderer.drawString(coloredName, -nameWidth / 2, 0, 0xFFFFFFFF, false);
		
		// Draw effects summary (only if identified or not obfuscated)
		if (!isObfuscated) {
			int lineOffset = 10;
			
			// Show usage info
			String usageInfo = getUsageInfo(te, definition, player);
			if (usageInfo != null && !usageInfo.isEmpty()) {
				int usageWidth = fontRenderer.getStringWidth(usageInfo);
				fontRenderer.drawString(usageInfo, -usageWidth / 2, lineOffset, 0xFFAAAAAA, false);
				lineOffset += 10;
			}
			
			// Show a brief effect summary (first 2-3 effects)
			int effectCount = 0;
			for (AltarEffect effect : definition.getEffects()) {
				if (effectCount >= 2) {
					break;
				}
				
				String effectDesc = getEffectDescription(effect);
				if (effectDesc != null && !effectDesc.isEmpty()) {
					int effectWidth = fontRenderer.getStringWidth(effectDesc);
					fontRenderer.drawString(effectDesc, -effectWidth / 2, lineOffset, 0xFFCCCCCC, false);
					lineOffset += 10;
					effectCount++;
				}
			}
			
			// Show "..." if there are more effects
			if (definition.getEffects().size() > 2) {
				String more = TextFormatting.GRAY + "...";
				int moreWidth = fontRenderer.getStringWidth(more);
				fontRenderer.drawString(more, -moreWidth / 2, lineOffset, 0xFFAAAAAA, false);
			}
		} else {
			// Show obfuscated hint
			String hint = TextFormatting.GRAY + "???";
			int hintWidth = fontRenderer.getStringWidth(hint);
			fontRenderer.drawString(hint, -hintWidth / 2, 10, 0xFFAAAAAA, false);
		}
		
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		
		GlStateManager.popMatrix();
	}

	private String getUsageInfo(TileEntityAltar te, AltarDefinition definition, EntityPlayer player) {
		switch (definition.getUsageType()) {
			case UNLIMITED:
				return TextFormatting.GREEN + "Unlimited Uses";
			case TIMES_PER_PLAYER:
				int playerUses = te.getPlayerUses(player.getUniqueID());
				int playerLimit = definition.getUsageLimit();
				int remaining = playerLimit - playerUses;
				if (remaining <= 0) {
					return TextFormatting.RED + "No uses remaining";
				}
				return TextFormatting.YELLOW + "Uses: " + remaining + "/" + playerLimit;
			case TIMES_BY_ANYONE:
				int totalUses = te.getTotalUses();
				int totalLimit = definition.getUsageLimit();
				int totalRemaining = totalLimit - totalUses;
				if (totalRemaining <= 0) {
					return TextFormatting.RED + "Depleted";
				}
				return TextFormatting.YELLOW + "Uses: " + totalRemaining + "/" + totalLimit;
		}
		return null;
	}

	private String getEffectDescription(AltarEffect effect) {
		switch (effect.getType()) {
			case POTION:
				AltarEffect.PotionEffect potionEffect = (AltarEffect.PotionEffect) effect;
				String potionName = potionEffect.getPotion().getName();
				// Clean up the potion name
				potionName = potionName.replace("effect.", "").replace("minecraft.", "");
				potionName = potionName.substring(0, 1).toUpperCase() + potionName.substring(1);
				return TextFormatting.AQUA + potionName;
			case COMMAND:
				return TextFormatting.LIGHT_PURPLE + "Special Reward";
			case TELEPORT_RECALL:
				return TextFormatting.BLUE + "Recall Point";
			case TELEPORT_TWIN:
				return TextFormatting.BLUE + "Teleportation";
			case PRAYER_DISPLAYED:
			case PRAYER_HIDDEN:
				return TextFormatting.YELLOW + "Prayer Required";
			default:
				return null;
		}
	}

	private String generateObfuscatedName(String altarId) {
		// Same logic as in BlockAltar
		java.util.Random random = new java.util.Random(altarId.hashCode());
		String[] prefixes = {"Ancient", "Forgotten", "Mysterious", "Dark", "Sacred", "Lost"};
		String[] suffixes = {"Shrine", "Monument", "Altar", "Pedestal", "Relic"};
		
		return prefixes[random.nextInt(prefixes.length)] + " " + 
		       suffixes[random.nextInt(suffixes.length)];
	}
}
