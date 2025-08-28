package com.windanesz.menhir.core;

import com.google.gson.*;
import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.ability.ebwizardry.ArcaneEchoAbility;
import com.windanesz.menhir.ability.minercaft.BlazeFireballAbility;
import com.windanesz.menhir.ability.minercaft.BlinkAbility;
import com.windanesz.menhir.ability.minercaft.RevelationAbility;
import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class BirthsignDataLoader {

	// Static mapping from birthsign name to ability factory
	private static final java.util.Map<String, AbilityFactory> BIRTHSIGN_ABILITY_FACTORIES = new java.util.HashMap<>();
	// Static mapping from birthsign name to passive ability factory
	private static final java.util.Map<String, PassiveAbilityFactory> birthsign_PASSIVE_ABILITY_FACTORIES = new java.util.HashMap<>();

	static {
		BIRTHSIGN_ABILITY_FACTORIES.put("the_blaze", BlazeFireballAbility::new);
		BIRTHSIGN_ABILITY_FACTORIES.put("the_ender", () -> new BlinkAbility(32.0));
		BIRTHSIGN_ABILITY_FACTORIES.put("the_conjuration", ArcaneEchoAbility::new);
		BIRTHSIGN_ABILITY_FACTORIES.put("the_seer", RevelationAbility::new);

		// Passive ability factories
		// birthsign_PASSIVE_ABILITY_FACTORIES.put("the_seer", player -> () -> ThreatSenseAbility.checkAndApplyThreatSense(player));
		// birthsign_ABILITY_FACTORIES.put("the_thief", com.windanesz.birthsigns.ability.ThiefSmokeAbility::new);
		// Add more mappings as needed
	}

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

		Menhir.logger.info("Checking mod requirements for birthsign '{}': {}", birthsign.name, birthsign.required_mods);

		for (String modId : birthsign.required_mods) {
			if (!Loader.isModLoaded(modId)) {
				Menhir.logger.info("Skipping birthsign '{}' - required mod '{}' is not loaded", birthsign.name, modId);
				return false;
			}
		}

		Menhir.logger.info("Loading birthsign '{}' - all required mods are loaded: {}", birthsign.name, birthsign.required_mods);
		return true;
	}

	public static List<Birthsign> loadBirthsignData() {
		List<Birthsign> birthsigns = new ArrayList<>();
		final int[] counters = {0, 0, 0, 0}; // [totalFilesFound, totalFilesProcessed, totalFilesLoaded, totalFilesSkipped]

		try {
			// Create Gson with custom deserializers
			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(Birthsign.EffectType.class, new JsonDeserializer<Birthsign.EffectType>() {
				@Override
				public Birthsign.EffectType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
					String jsonName = json.getAsString();
					return Birthsign.EffectType.fromJsonName(jsonName);
				}
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
		try {
			// Get the config directory path
			File configDir = new File("config/menhir");
			if (!configDir.exists() || !configDir.isDirectory()) {
				Menhir.logger.debug("Config directory config/menhir does not exist, skipping");
				return;
			}

			Menhir.logger.info("BirthsignDataLoader: Loading birthsigns from config directory: {}", configDir.getAbsolutePath());

			// Load from main config/menhir directory
			loadBirthsignsFromDirectory(configDir, birthsigns, gson, counters, "config");

			// Also check config/menhir/custom subdirectory if it exists
			File customDir = new File(configDir, "custom");
			if (customDir.exists() && customDir.isDirectory()) {
				Menhir.logger.info("BirthsignDataLoader: Loading birthsigns from custom config directory: {}", customDir.getAbsolutePath());
				loadBirthsignsFromDirectory(customDir, birthsigns, gson, counters, "config/custom");
			}

		} catch (Exception e) {
			Menhir.logger.error("BirthsignDataLoader: Error loading from config directory: {}", e.getMessage());
		}
	}

	/**
	 * Loads birthsign JSON files from the assets/menhir/birthsigns resource path
	 */
	private static void loadBirthsignsFromAssets(List<Birthsign> birthsigns, Gson gson, int[] counters) {
		try {
			ClassLoader classLoader = BirthsignDataLoader.class.getClassLoader();
			URL dirURL = classLoader.getResource("assets/menhir/birthsigns");

			Menhir.logger.info("BirthsignDataLoader: Loading birthsigns from assets: {}", dirURL);

			if (dirURL != null && "jar".equals(dirURL.getProtocol())) {
				// Handle JAR resources
				Menhir.logger.info("BirthsignDataLoader: Processing JAR resources");
				String jarPath = dirURL.getPath();
				String jarFile = jarPath.substring(0, jarPath.indexOf("!"));
				jarFile = jarFile.substring(jarFile.indexOf(":") + 1);
				if (System.getProperty("os.name").toLowerCase().contains("windows")) {
					jarFile = jarFile.substring(1);
				}

				try (JarInputStream jarStream = new JarInputStream(Files.newInputStream(Paths.get(jarFile)))) {
					JarEntry entry;
					while ((entry = jarStream.getNextJarEntry()) != null) {
						String name = entry.getName();
						if (name.startsWith("assets/menhir/birthsigns/") && name.endsWith(".json")) {
							counters[0]++; // totalFilesFound
							Menhir.logger.debug("Found birthsigns file in JAR: {}", name);

							try (InputStream stream = classLoader.getResourceAsStream(name)) {
								if (stream != null) {
									counters[1]++; // totalFilesProcessed
									Birthsign birthsign = gson.fromJson(new InputStreamReader(stream), Birthsign.class);
									processBirthsign(birthsign, birthsigns, counters, "jar:" + name);
								}
							}
						}
					}
				} catch (IOException e) {
					Menhir.logger.error("Error reading JAR file: {}", e.getMessage());
				}
			} else {
				// Handle file system resources
				Menhir.logger.info("BirthsignDataLoader: Processing file system resources");
				File dir = new File(dirURL.toURI());
				if (dir.exists() && dir.isDirectory()) {
					loadBirthsignsFromDirectory(dir, birthsigns, gson, counters, "assets");
				}
			}
		} catch (Exception e) {
			Menhir.logger.error("BirthsignDataLoader: Error loading from assets: {}", e.getMessage());
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

						birthsign.activeAbilities.add(com.windanesz.menhir.ability.AbilityFactory.create(eff.type.getJsonName(), params, birthsign.name));
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
			Menhir.logger.info("Skipped birthsign '{}' due to missing mod requirements (loaded from {})", birthsign.name, source);
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