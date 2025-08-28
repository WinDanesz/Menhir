package com.windanesz.menhir;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = Menhir.MODID, name = "Menhir") // No fancy configs here so we can use the annotation, hurrah!
public class Settings {


	@Config.Name("General Settings")
	@Config.LangKey("settings.menhir:general_settings")
	public static GeneralSettings generalSettings = new GeneralSettings();

	@SuppressWarnings("unused")
	@Mod.EventBusSubscriber(modid = Menhir.MODID)
	private static class EventHandler {
		/**
		 * Inject the new values and save to the config file when the config has been changed from the GUI.
		 *
		 * @param event The event
		 */
		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if (event.getModID().equals(Menhir.MODID)) {
				ConfigManager.sync(Menhir.MODID, Config.Type.INSTANCE);
			}
		}
	}

	public static class GeneralSettings {

		@Config.Name("Random Birthsign Assignment")
		@Config.Comment("If enabled, assigns a random birthsign to players when they join a world for the first time.")
		public boolean random_birthsign_assignment = true;

		@Config.Name("Random Birthsign Assignment Message")
		@Config.Comment("If true, displays a message to players when they are assigned a random birthsign.")
		public boolean show_random_birthsign_message = true;

		@Config.Name("Birthsign Stones Can Override Existing Birthsigns")
		@Config.Comment("If true, players can use birthsign stones to change their existing birthsign. If false, players cannot change their birthsign once assigned.")
		public boolean menhir_stones_can_override_existing_birthsigns = true;


		@Config.Name("Max Birthsigns Per World")
		@Config.Comment("The maximum number of birthsigns that can be generated in a world. Chosen birthsigns are always determined by the world seed.")
		@Config.RangeInt(min = 2, max = 16)
		public int max_birthsigns_per_world = 12;

		@Config.Name("Birthsign Stone Spawn Distance In Chunks From Spawn")
		@Config.Comment("The distance in chunks from spawn that birthsign stones will be generated. Each chunk is 16x16 blocks, so 4 chunks = 64 blocks from spawn. Birthsign stones always spawn in circles around the world spawn.")
		@Config.RangeInt(min = 100)
		public int menhir_stone_spawn_distance_chunks = 120;

		@Config.Name("Antique Atlas Integration")
		@Config.Comment("If true, enables integration with Antique Atlas mod for automatic birthsign stone markers.")
		public boolean antique_atlas_integration = true;

		@Config.Name("Auto Birthsign Stone Markers")
		@Config.Comment("If true, automatically places menhir stone markers in Antique Atlas when menhir stones are generated in the world.")
		public boolean auto_menhir_stone_markers = true;

	}
}