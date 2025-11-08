package com.windanesz.menhir;

import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class CommonProxy {

	/**
	 * Called from init() in the main mod class to initialise the particle factories.
	 */
	public void registerParticles() {
	}

	/**
	 * Called from preInit() in the main mod class to initialise the renderers.
	 */
	public void registerRenderers() {
	}


	public void init() {
	}

	public void initialiseLayers() {
	}


	public void registerAtlasMarkers() {
	}

	public void initialiseAnimations() {
	}

	/**
	 * Translates the given key and formats it with the given style. Client-side only; on the server this simply returns
	 * the given translation key. Useful whenever translation from common classes is required, e.g. item tooltips.
	 *
	 * @param key   The unlocalised name to be translated.
	 * @param style The {@link Style} to use for the displayed text.
	 * @param args  The format arguments to pass into the translation, if any.
	 * @return The resulting translated text.
	 */
	public String translate(String key, Style style, Object... args) {
		return key;
	}

	/**
	 * Translates the given key with no specified style. Client-side only; on the server this simply returns
	 * the given translation key. Useful whenever translation from common classes is required, e.g. item tooltips.
	 *
	 * @param key  The unlocalised name to be translated.
	 * @param args The format arguments to pass into the translation, if any.
	 * @return The resulting translated text.
	 */
	public String translate(String key, Object... args) {
		return translate(key, new Style(), args);
	}

	/**
	 * Like {@link electroblob.wizardry.CommonProxy#addMultiLineDescription(List, String, Style, Object...)}, but style defaults to light grey.
	 */
	public void addMultiLineDescription(List<String> tooltip, String key, Object... args) {
		this.addMultiLineDescription(tooltip, key, new Style().setColor(TextFormatting.GRAY), args);
	}

	/**
	 * Adds a multi-line description to the given tooltip list. The description is first translated using the given
	 * translation key, then the formatting code for the given style is appended, and finally the string is word-wrapped
	 * to the standard width (100).
	 *
	 * @param tooltip The tooltip list to add to
	 * @param key     The translation key for the description
	 * @param style   A style to apply
	 * @author Electroblob
	 */
	public void addMultiLineDescription(List<String> tooltip, String key, Style style, Object... args) {
	}


	public void checkTranslationKeys() {
	}

	/**
	 * Opens the birthsign selection GUI on client side.
	 * Server-side does nothing.
	 * 
	 * @param player The player to open the GUI for
	 */
	public void openBirthsignSelectionGUI(net.minecraft.entity.player.EntityPlayer player) {
		// Server-side does nothing
	}
}