package com.windanesz.menhir.client;

import com.windanesz.menhir.CommonProxy;
import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.block.BlockMenhirStone;
import com.windanesz.menhir.integration.antiqueatlas.MenhirAntiqueAtlasIntegration;
import com.windanesz.menhir.tileentity.TileEntityMenhirStone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.Style;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;

import java.util.List;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	private static final int TOOLTIP_WRAP_WIDTH = 140;
	public static KeyBinding KEY_ACTIVATE_POWER;

	public void init() {
		registerKeybindings();
		MinecraftForge.EVENT_BUS.register(this);
	}

	private void registerKeybindings() {
		KEY_ACTIVATE_POWER = new KeyBinding("key.menhir.charm_bauble_activate", Keyboard.KEY_K, "key.menhir.category");
		ClientRegistry.registerKeyBinding(KEY_ACTIVATE_POWER);
	}

	public void registerRenderers() {
		// Register custom renderer for birthsign stone blocks
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityMenhirStone.class, new MenhirStoneRenderer());
	}

	@Override
	public void registerParticles() {
	}

	@Override
	public String translate(String key, Style style, Object... args) {
		return style.getFormattingCode() + I18n.format(key, args);
	}

	@Override
	public void addMultiLineDescription(List<String> tooltip, String key, Style style, Object... args) {
		tooltip.addAll(Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(translate(key, style, args), TOOLTIP_WRAP_WIDTH));
	}

	/**
	 * Utility method to check if language keys are missing. Only runs in dev environment and the client side.
	 */
	private void missingKeyWarning(String type, String registryName, String expectedKey) {
		Menhir.logger.warn("{} {} is missing a translation key: \"{}\"", type, registryName, expectedKey);
	}

	@Override
	public void registerAtlasMarkers() {
		MenhirAntiqueAtlasIntegration.registerMarkers();
	}

	//	TODO: Antique atlas support?
	//	public void registerAtlasMarkers() {
	//	AntiqueAtlasIntegration.registerMarkers();
	//	}

	/**
	 * Custom renderer for birthsign stone blocks that displays birthsign information above the block
	 */
	private static class MenhirStoneRenderer extends TileEntitySpecialRenderer<TileEntityMenhirStone> {

		@Override
		public void render(TileEntityMenhirStone te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
			if (te == null || te.getWorld() == null) {
				return;
			}

			String birthsignName = te.getBirthsign();

			// Check the block position and type
			net.minecraft.block.state.IBlockState state = te.getWorld().getBlockState(te.getPos());
			BlockMenhirStone.BlockPosition position = state.getValue(BlockMenhirStone.POSITION);

			if (birthsignName == null || birthsignName.isEmpty()) {
				return;
			}

			// Only render for the top part of the multiblock (so the label appears above everything)
			if (position != BlockMenhirStone.BlockPosition.TOP) {
				return;
			}

			// Get the player's position for distance checking
			net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
			if (player == null) {
				return;
			}

			// Calculate distance to player
			double distance = player.getDistance(te.getPos().getX() + 0.5, te.getPos().getY() + 0.5, te.getPos().getZ() + 0.5);

			if (distance > 5) {
				return; // Only render within 32 blocks
			}
			// Start rendering
			net.minecraft.client.renderer.GlStateManager.pushMatrix();
			net.minecraft.client.renderer.GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);

			// Position text above the block
			net.minecraft.client.renderer.GlStateManager.translate(0, 1.2, 0);

			// Make text face the player
			net.minecraft.client.renderer.GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
			net.minecraft.client.renderer.GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);

			// Scale text based on distance
			float scale = 0.025F;
			net.minecraft.client.renderer.GlStateManager.scale(-scale, -scale, scale);

			// Disable lighting for text
			net.minecraft.client.renderer.GlStateManager.disableLighting();
			net.minecraft.client.renderer.GlStateManager.enableBlend();
			net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(net.minecraft.client.renderer.GlStateManager.SourceFactor.SRC_ALPHA, net.minecraft.client.renderer.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, net.minecraft.client.renderer.GlStateManager.SourceFactor.ONE, net.minecraft.client.renderer.GlStateManager.DestFactor.ZERO);

			// Render the birthsign name with localization
			// Extract the birthsign name without modid prefix for translation
			String birthsignForTranslation = birthsignName;
			if (birthsignName.contains(":")) {
				birthsignForTranslation = birthsignName.split(":")[1];
			}
			String localizedBirthsignName = I18n.format("birthsign." + birthsignForTranslation + ".name");
			String displayText = "§a" + localizedBirthsignName;
			int textWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(displayText);
			Minecraft.getMinecraft().fontRenderer.drawString(displayText, -textWidth / 2, 0, 0xFFFFFFFF);

			// Restore rendering state
			net.minecraft.client.renderer.GlStateManager.enableLighting();
			net.minecraft.client.renderer.GlStateManager.disableBlend();
			net.minecraft.client.renderer.GlStateManager.popMatrix();
		}
	}
}
