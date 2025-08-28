package com.windanesz.menhir.integration.antiqueatlas;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.Settings;
import hunternif.mc.atlas.api.AtlasAPI;
import hunternif.mc.atlas.registry.MarkerType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

/**
 * This class handles all of Menhir's integration with the <i>Antique Atlas</i> mod.
 * This class contains only the code that requires Antique Atlas to be loaded in order to run.
 *
 * @author WinDanesz
 * @since Menhir 1.0.0
 */
public class MenhirAntiqueAtlasIntegration {

	public static final String ANTIQUE_ATLAS_MOD_ID = "antiqueatlas";

	private static final ResourceLocation MENHIR_STONE_MARKER = new ResourceLocation(Menhir.MODID, "textures/integration/antiqueatlas/menhir_stone.png");

	private static boolean antiqueAtlasLoaded;

	public static void init() {
		antiqueAtlasLoaded = Loader.isModLoaded(ANTIQUE_ATLAS_MOD_ID);
		Menhir.proxy.registerAtlasMarkers(); // Needs routing through the proxies to make sure it's only client-side
	}

	public static boolean enabled() {
		return Settings.generalSettings.antique_atlas_integration && antiqueAtlasLoaded;
	}

	/**
	 * Places a global birthsign stone marker in all antique atlases at the given coordinates in the given world if
	 * {@link Settings#antique_atlas_integration} is enabled. Server side only!
	 */
	public static void markMenhirStone(World world, int x, int z, String birthsignName) {
		if (enabled() && Settings.generalSettings.auto_menhir_stone_markers) {
			// Extract the birthsign name without modid prefix for translation
			String birthsignNameForTranslation = birthsignName;
			if (birthsignName.contains(":")) {
				birthsignNameForTranslation = birthsignName.split(":")[1];
			}

			// Use the birthsign's display name for the marker
			String markerLabel = "birthsign." + birthsignNameForTranslation + ".name";
			AtlasAPI.getMarkerAPI().putGlobalMarker(world, false, MENHIR_STONE_MARKER.toString(),
					markerLabel, x, z);
		}
	}

	/**
	 * Registers the marker icons with Antique Atlas. Client side only!
	 */
	public static void registerMarkers() {

		if (!enabled()) return;

		AtlasAPI.getMarkerAPI().registerMarker(new MarkerType(MENHIR_STONE_MARKER,
				new ResourceLocation(Menhir.MODID, "textures/integration/antiqueatlas/menhir_stone.png")));

	}
}
