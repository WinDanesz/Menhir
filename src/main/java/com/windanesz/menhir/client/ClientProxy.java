package com.windanesz.menhir.client;

import com.windanesz.menhir.CommonProxy;
import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.block.BlockMenhirStone;
import com.windanesz.menhir.integration.antiqueatlas.MenhirAntiqueAtlasIntegration;
import com.windanesz.menhir.tileentity.TileEntityMenhirStone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
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
	public static KeyBinding KEY_SHOW_BIRTHSIGN;

	public void init() {
		registerKeybindings();
		MinecraftForge.EVENT_BUS.register(this);
		
		// Load custom language files from config/menhir/lang/
		CustomLangLoader.loadCustomLangFiles();
	}

	private void registerKeybindings() {
		KEY_ACTIVATE_POWER = new KeyBinding("key.menhir.charm_bauble_activate", Keyboard.KEY_K, "key.menhir.category");
		ClientRegistry.registerKeyBinding(KEY_ACTIVATE_POWER);
		
		KEY_SHOW_BIRTHSIGN = new KeyBinding("key.menhir.show_birthsign", Keyboard.KEY_B, "key.menhir.category");
		ClientRegistry.registerKeyBinding(KEY_SHOW_BIRTHSIGN);
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

			// Get the player's position for distance checking
			net.minecraft.entity.player.EntityPlayer player = Minecraft.getMinecraft().player;
			if (player == null) {
				return;
			}

			// Calculate distance to player
			double distance = player.getDistance(te.getPos().getX() + 0.5, te.getPos().getY() + 0.5, te.getPos().getZ() + 0.5);

			// Render constellation texture on the middle block (32 blocks range)
			if (position == BlockMenhirStone.BlockPosition.MIDDLE && distance <= 32) {
				renderConstellationTexture(te, x, y, z, birthsignName, state);
			}

			// Only render text label for the top part of the multiblock (5 blocks range)
			if (position != BlockMenhirStone.BlockPosition.TOP || distance > 5) {
				return;
			}

			// Start rendering text
			GlStateManager.pushMatrix();
			GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);

			// Position text above the block
			GlStateManager.translate(0, 1.2, 0);
 
			// Make text face the player
			GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);

			// Scale text based on distance
			float scale = 0.025F;
			GlStateManager.scale(-scale, -scale, scale);

			// Disable lighting for text
			GlStateManager.disableLighting();
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

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
			GlStateManager.enableLighting();
			GlStateManager.disableBlend();
			GlStateManager.popMatrix();
		}

		/**
		 * Renders the birthsign constellation texture on the stone face
		 */
		private void renderConstellationTexture(TileEntityMenhirStone te, double x, double y, double z, String birthsignName, net.minecraft.block.state.IBlockState state) {
			// Get the facing direction of the block
			net.minecraft.util.EnumFacing facing = state.getValue(BlockMenhirStone.FACING);
			
			// Extract birthsign key for texture lookup
			String birthsignKey = birthsignName;
			if (birthsignName.contains(":")) {
				birthsignKey = birthsignName.split(":")[1];
			}
			
			// Bind the constellation texture (32x32)
			net.minecraft.util.ResourceLocation texture = new net.minecraft.util.ResourceLocation("menhir", "textures/birthsigns/" + birthsignKey + ".png");
			bindTexture(texture);
			
			// Enable blending for transparent textures
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(
				GlStateManager.SourceFactor.SRC_ALPHA, 
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, 
				GlStateManager.SourceFactor.ONE, 
				GlStateManager.DestFactor.ZERO
			);
			GlStateManager.disableLighting();
			GlStateManager.disableCull(); // Disable culling so both sides render
			
			// Size of the texture on the block face
			// Middle block width: 0.625 (from 0.1875 to 0.8125), height: 0.375 (from 0.3125 to 0.6875)
			// Use 80% of the available face size to leave margins
			float size = 1.0F; // Larger size for better visibility
			float halfSize = size / 2.0F;
			
			// Render on both front and back faces
			for (int side = 0; side < 2; side++) {
				GlStateManager.pushMatrix();
				GlStateManager.translate(x + 0.5, y + 0.9, z + 0.5); // Moved up by 0.5 blocks
				
				// Rotate based on facing direction to render on the front face
				switch (facing) {
					case NORTH:
						GlStateManager.rotate(180, 0, 1, 0);
						break;
					case SOUTH:
						// No rotation needed
						break;
					case WEST:
						GlStateManager.rotate(90, 0, 1, 0);
						break;
					case EAST:
						GlStateManager.rotate(-90, 0, 1, 0);
						break;
					default:
						break;
				}
				
				// For the second iteration, rotate 180 degrees to render on the back
				if (side == 1) {
					GlStateManager.rotate(180, 0, 1, 0);
				}
				
				// Move slightly forward from the block face to prevent z-fighting
				// Middle block AABB: 0.3125 to 0.6875 in Z (depth of 0.375), so front face is at 0.6875 offset from center
				// Use different offsets for front and back to avoid z-fighting
				GlStateManager.translate(0, 0, -0.1875 - 0.01 - (side * 0.01));
				
				// Add a subtle glow effect
				GlStateManager.color(1.0F, 1.0F, 0.9F, 1.0F);
				
				// Render the quad with the constellation texture
				net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
				net.minecraft.client.renderer.BufferBuilder buffer = tessellator.getBuffer();
				buffer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);
				
				// Render a quad facing the camera (texture coordinates: 0,0 to 1,1)
				buffer.pos(-halfSize, -halfSize, 0).tex(0, 1).endVertex();
				buffer.pos(-halfSize, halfSize, 0).tex(0, 0).endVertex();
				buffer.pos(halfSize, halfSize, 0).tex(1, 0).endVertex();
				buffer.pos(halfSize, -halfSize, 0).tex(1, 1).endVertex();
				
				tessellator.draw();
				
				// Add glowing effect with additive blending
				GlStateManager.blendFunc(
					GlStateManager.SourceFactor.SRC_ALPHA, 
					GlStateManager.DestFactor.ONE
				);
				
				// Pulsating glow effect
				long time = System.currentTimeMillis();
				float pulse = 0.5F + 0.3F * (float)Math.sin(time / 1000.0 * Math.PI);
				GlStateManager.color(1.0F, 1.0F, 0.8F, pulse);
				
				// Render slightly larger for glow (reduced from 1.2 to 1.0 = 20% reduction)
				float glowSize = size * 1.075F;
				float halfGlowSize = glowSize / 2.0F;
				
				buffer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);
				buffer.pos(-halfGlowSize, -halfGlowSize, 0).tex(0, 1).endVertex();
				buffer.pos(-halfGlowSize, halfGlowSize, 0).tex(0, 0).endVertex();
				buffer.pos(halfGlowSize, halfGlowSize, 0).tex(1, 0).endVertex();
				buffer.pos(halfGlowSize, -halfGlowSize, 0).tex(1, 1).endVertex();
				tessellator.draw();
				
				// Reset blend function for next iteration
				GlStateManager.tryBlendFuncSeparate(
					GlStateManager.SourceFactor.SRC_ALPHA, 
					GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, 
					GlStateManager.SourceFactor.ONE, 
					GlStateManager.DestFactor.ZERO
				);
				
				GlStateManager.popMatrix();
			}
			
			// Restore render state
			GlStateManager.enableCull();
			GlStateManager.enableLighting();
			GlStateManager.disableBlend();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	@Override
	public void openBirthsignSelectionGUI(net.minecraft.entity.player.EntityPlayer player) {
		// Open the birthsign GUI in selection mode
		Minecraft.getMinecraft().addScheduledTask(() -> {
			Minecraft.getMinecraft().displayGuiScreen(new com.windanesz.menhir.client.gui.GuiBirthsign(player, true));
		});
	}
}
