package com.windanesz.menhir.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BirthsignDataLoader {

	/**
	 * Checks if all required mods for a birthsign are loaded.
	 *
	 * @param birthsign The birthsign to check
	 * @return true if all required mods are loaded, false otherwise
	 */
	private static boolean areRequiredModsLoaded(Birthsign birthsign) {
		if (birthsign.required_mods == null || birthsign.required_mods.isEmpty()) {
			Menhir.logger.debug("Birthsign '{}' has no mod requirements - loading", birthsign.name);
			return true; // No mod requirements
		}

		//Menhir.logger.info("Checking mod requirements for birthsign '{}': {}", birthsign.name, birthsign.required_mods);

		for (String modId : birthsign.required_mods) {
			if (!Loader.isModLoaded(modId)) {
				Menhir.logger.info("Skipping birthsign '{}' - required mod '{}' is not loaded", birthsign.name, modId);
				return false;
			}
		}

		Menhir.logger.info("Loading birthsign '{}' - all required mods are loaded: {}", birthsign.name, birthsign.required_mods);
		return true;
	}

	/**
	 * Checks if a birthsign is disabled in the config.
	 *
	 * @param birthsignName The name of the birthsign to check
	 * @return true if the birthsign is disabled, false otherwise
	 */
	private static boolean isBirthsignDisabled(String birthsignName) {
		if (birthsignName == null || birthsignName.isEmpty()) {
			return false;
		}

		// Get the disabled birthsigns from config
		String[] disabledBirthsigns = com.windanesz.menhir.Settings.generalSettings.disabled_birthsigns;
		if (disabledBirthsigns == null || disabledBirthsigns.length == 0) {
			return false;
		}

		// Check if the birthsign name matches any disabled entry
		// Support both "menhir:birthsign_name" and "birthsign_name" formats
		for (String disabledEntry : disabledBirthsigns) {
			if (disabledEntry == null || disabledEntry.isEmpty()) {
				continue;
			}

			// Check exact match
			if (disabledEntry.equals(birthsignName)) {
				return true;
			}

			// Check if it's in the format "menhir:birthsign_name" and matches the birthsign name
			if (disabledEntry.startsWith("menhir:") && disabledEntry.substring(7).equals(birthsignName)) {
				return true;
			}

			// Check if the birthsign name is in the format "menhir:birthsign_name" and matches the disabled entry
			if (birthsignName.startsWith("menhir:") && birthsignName.substring(7).equals(disabledEntry)) {
				return true;
			}
		}

		return false;
	}

	public static List<Birthsign> loadBirthsignData() {
		List<Birthsign> birthsigns = new ArrayList<>();
		final int[] counters = {0, 0, 0, 0}; // [totalFilesFound, totalFilesProcessed, totalFilesLoaded, totalFilesSkipped]

		try {
			// Create Gson with custom deserializers
			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(Birthsign.EffectType.class, (JsonDeserializer<Birthsign.EffectType>) (json, typeOfT, context) -> {
				String jsonName = json.getAsString();
				return Birthsign.EffectType.fromJsonName(jsonName);
			});
			gsonBuilder.registerTypeAdapter(Birthsign.EffectDetail.class, new EffectDetailDeserializer());
			Gson gson = gsonBuilder.create();

			// First, load birthsigns from config directory (user customizations)
			loadBirthsignsFromConfigDirectory(birthsigns, gson, counters);

			// Then, load birthsigns from assets (built-in birthsigns)
			loadBirthsignsFromAssets(birthsigns, gson, counters);

		} catch (Exception e) {
			Menhir.logger.error("BirthsignDataLoader: Error during birthsign loading", e);
			e.printStackTrace();
		}

		Menhir.logger.info("BirthsignDataLoader: Summary - Files found: {}, Processed: {}, Loaded: {}, Skipped: {}",
				counters[0], counters[1], counters[2], counters[3]);

		return birthsigns;
	}

	/**
	 * Loads birthsigns JSON files from the config/menhir directory
	 */
	private static void loadBirthsignsFromConfigDirectory(List<Birthsign> birthsigns, Gson gson, int[] counters) {
		Menhir.logger.info("Loading birthsigns from config folder");
		
		File configDir = new File(Loader.instance().getConfigDir(), Menhir.MODID);
		
		if (!configDir.exists()) {
			Menhir.logger.debug("Config directory does not exist, skipping custom birthsign loading");
			return; // If there's no config folder, do nothing (like Wizardry)
		}
		
		Menhir.logger.info("BirthsignDataLoader: Loading birthsigns from config directory: {}", configDir.getAbsolutePath());
		
		// Load from main config/menhir directory
		//loadBirthsignsFromDirectory(configDir, birthsigns, gson, counters, "config");
		
		// Also check config/menhir/custom subdirectory if it exists
		File customDir = new File(configDir, "");
		if (customDir.exists() && customDir.isDirectory()) {
			Menhir.logger.info("BirthsignDataLoader: Loading birthsigns from custom config directory: {}", customDir.getAbsolutePath());
			loadBirthsignsFromDirectory(customDir, birthsigns, gson, counters, "config/menhir");
		}
	}

	/**
	 * Loads birthsign JSON files from the assets/menhir/birthsigns resource path using CraftingHelper
	 */
	private static void loadBirthsignsFromAssets(List<Birthsign> birthsigns, Gson gson, int[] counters) {
		try {
			ModContainer mod = Loader.instance().getModList().stream()
					.filter(m -> m.getModId().equals(Menhir.MODID))
					.findFirst().orElse(null);
			
			if (mod == null) {
				Menhir.logger.error("BirthsignDataLoader: Could not find mod container for {}", Menhir.MODID);
				return;
			}

			Menhir.logger.info("BirthsignDataLoader: Loading birthsigns from assets using CraftingHelper");

			// Use CraftingHelper.findFiles - this method is reliable and used by Forge itself
			boolean success = CraftingHelper.findFiles(mod, "assets/" + Menhir.MODID + "/birthsigns", null,
					(root, file) -> {
						String relative = root.relativize(file).toString();
						if (!"json".equals(FilenameUtils.getExtension(file.toString())) || relative.startsWith("_")) {
							return true; // True or it'll look like it failed just because it found a non-JSON file
						}

						counters[0]++; // totalFilesFound
						String fileName = FilenameUtils.removeExtension(relative).replaceAll("\\\\", "/");
						Menhir.logger.debug("BirthsignDataLoader: Processing birthsign file: {}", fileName);

						BufferedReader reader = null;
						try {
							reader = Files.newBufferedReader(file);
							counters[1]++; // totalFilesProcessed
							
							Birthsign birthsign = gson.fromJson(reader, Birthsign.class);
							processBirthsign(birthsign, birthsigns, counters, "assets:" + fileName);
							
						} catch (JsonParseException jsonParseException) {
							Menhir.logger.error("BirthsignDataLoader: Parsing error loading birthsign file {}", file, jsonParseException);
							return false;
						} catch (IOException ioException) {
							Menhir.logger.error("BirthsignDataLoader: Couldn't read birthsign file {}", file, ioException);
							return false;
						} finally {
							IOUtils.closeQuietly(reader);
						}

						return true;
					},
					true, true);

			if (!success) {
				Menhir.logger.error("BirthsignDataLoader: Failed to load birthsigns from assets");
			} else {
				Menhir.logger.info("BirthsignDataLoader: Successfully processed assets");
			}
			
		} catch (Exception e) {
			Menhir.logger.error("BirthsignDataLoader: Error loading from assets: {}", e.getMessage(), e);
		}
	}

	/**
	 * Loads birthsigns JSON files from a specific directory
	 */
	private static void loadBirthsignsFromDirectory(File dir, List<Birthsign> birthsigns, Gson gson, int[] counters, String source) {
		File[] files = dir.listFiles((dir1, name) -> name.endsWith(".json"));
		if (files != null) {
			counters[0] += files.length; // totalFilesFound
			Menhir.logger.info("BirthsignDataLoader: Found {} JSON files in {} directory", files.length, source);

			for (File file : files) {
				counters[1]++; // totalFilesProcessed
				Menhir.logger.debug("Processing birthsigns file from {}: {}", source, file.getName());

				try (FileReader reader = new FileReader(file)) {
					Birthsign birthsign = gson.fromJson(reader, Birthsign.class);
					processBirthsign(birthsign, birthsigns, counters, source + ":" + file.getName());
				} catch (Exception e) {
					Menhir.logger.error("Error loading birthsigns from file {}: {}", file.getName(), e.getMessage());
				}
			}
		}
	}

	/**
	 * Processes a loaded birthsigns object and adds it to the list if valid
	 */
	private static void processBirthsign(Birthsign birthsign, List<Birthsign> birthsigns, int[] counters, String source) {
		birthsign.activeAbilities = new ArrayList<>();
		birthsign.spell_modifiers = new java.util.HashMap<>();

		// Check if this birthsign is disabled in config
		if (isBirthsignDisabled(birthsign.name)) {
			counters[3]++; // totalFilesSkipped
			//Menhir.logger.info("Skipped birthsign '{}' - disabled in config (loaded from {})", birthsign.name, source);
			return;
		}

		// Process passive effects to extract spell modifiers
		if (birthsign.passive != null) {
			for (Birthsign.BirthsignEffect effect : birthsign.passive) {
				if (effect.effect != null) {
					Birthsign.EffectDetail eff = effect.effect;
					if (eff.type == Birthsign.EffectType.WIZARDRY_SPELL_MODIFIER) {
						String modifierName = eff.getParameter("name", "");
						Float amount = eff.getParameter("spell_modifier_amount", 0.0f);
						if (modifierName != null && !modifierName.isEmpty() && amount != null) {
							birthsign.spell_modifiers.put(modifierName, amount);
						}
					}
				}
			}
		}

		if (birthsign.active != null) {
			for (Birthsign.BirthsignEffect effect : birthsign.active) {
				if (effect.effect != null) {
					Birthsign.EffectDetail eff = effect.effect;
					if (eff.type != null) {
						// Use the dynamic parameters map directly
						Map<String, Object> params = new java.util.HashMap<>(eff.parameters);

						try {
							birthsign.activeAbilities.add(com.windanesz.menhir.ability.AbilityFactory.create(eff.type.getJsonName(), params, birthsign.name));
						} catch (IllegalArgumentException e) {
							Menhir.logger.error("Failed to crea ability '{}' for birthsign '{}' from {}: {}",
								eff.type.getJsonName(), birthsign.name, source, e.getMessage());
						}
					} else {
						Menhir.logger.warn("Birthsign '{}' has an unrecognised active ability type!", birthsign.name, source);
					}
				}
			}
		}

		// Check mod dependencies before adding the birthsign
		if (areRequiredModsLoaded(birthsign)) {
			birthsigns.add(birthsign);
			counters[2]++; // totalFilesLoaded
			Menhir.logger.info("Added birthsign '{}' to registry from {}", birthsign.name, source);
		} else {
			counters[3]++; // totalFilesSkipped
			Menhir.logger.info("Skipped birthsign '{}' due to missing mod requirements", birthsign.name);
		}
	}

	private static int getOperation(String op) {
		switch (op) {
			case "add":
			case "add_number":
			case "0":
				return 0;
			case "multiply_base":
			case "1":
				return 1;
			case "multiply_total":
			case "2":
				return 2;
			default:
				return 0;
		}
	}

	// Factory interface for creating abilities
	public interface AbilityFactory {
		IBirthsignActiveAbility create();
	}

	// Factory interface for creating passive abilities
	public interface PassiveAbilityFactory {
		Runnable create(EntityPlayer player);
	}
} 