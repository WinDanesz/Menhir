package com.windanesz.menhir.client;

import com.windanesz.menhir.Menhir;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.Loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles loading custom language files from the config/menhir/lang/ directory.
 * This allows users to add or override localization without needing a resource pack.
 * Only loads the file named "menhir.lang".
 */
public class CustomLangLoader {

	private static final Map<String, String> customTranslations = new HashMap<>();
	private static final String LANG_FILENAME = "menhir.lang";
	private static boolean loaded = false;

	/**
	 * Loads the menhir.lang file from the config/menhir/lang/ directory.
	 * Should be called during client initialization.
	 */
	public static void loadCustomLangFiles() {
		if (loaded) {
			Menhir.logger.debug("Custom lang file already loaded, skipping");
			return;
		}

		File configDir = new File(Loader.instance().getConfigDir(), Menhir.MODID);
		File langDir = new File(configDir, "lang");

		if (!langDir.exists() || !langDir.isDirectory()) {
			Menhir.logger.debug("No custom lang directory found at: {}", langDir.getAbsolutePath());
			return;
		}

		File langFile = new File(langDir, LANG_FILENAME);
		if (!langFile.exists() || !langFile.isFile()) {
			Menhir.logger.debug("No {} file found in custom lang directory", LANG_FILENAME);
			return;
		}

		Menhir.logger.info("Loading custom language file: {}", langFile.getAbsolutePath());

		int keys = loadLangFile(langFile);
		if (keys > 0) {
			Menhir.logger.info("Loaded {} translation keys from {}", keys, LANG_FILENAME);
			injectTranslations();
			loaded = true;
		}
	}

	/**
	 * Loads a single .lang file and adds its translations to the custom translations map.
	 *
	 * @param langFile The .lang file to load
	 * @return The number of translation keys loaded from this file
	 */
	private static int loadLangFile(File langFile) {
		int keyCount = 0;

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8))) {

			String line;
			int lineNumber = 0;

			while ((line = reader.readLine()) != null) {
				lineNumber++;
				line = line.trim();

				// Skip empty lines and comments
				if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
					continue;
				}

				// Parse key=value format
				int equalsIndex = line.indexOf('=');
				if (equalsIndex <= 0 || equalsIndex >= line.length() - 1) {
					Menhir.logger.warn("Invalid lang entry at {}:{} - {}", langFile.getName(), lineNumber, line);
					continue;
				}

				String key = line.substring(0, equalsIndex).trim();
				String value = line.substring(equalsIndex + 1).trim();

				if (key.isEmpty()) {
					Menhir.logger.warn("Empty key at {}:{}", langFile.getName(), lineNumber);
					continue;
				}

				// Store the translation
				if (customTranslations.containsKey(key)) {
					Menhir.logger.debug("Overriding existing translation for key: {}", key);
				}

				customTranslations.put(key, value);
				keyCount++;
			}

		} catch (Exception e) {
			Menhir.logger.error("Error loading lang file {}: {}", langFile.getName(), e.getMessage());
			e.printStackTrace();
		}

		return keyCount;
	}

	/**
	 * Injects the custom translations into Minecraft's I18n system.
	 * This uses reflection to access and modify the private translation map.
	 */
	private static void injectTranslations() {
		try {
			// Access the private localizations field in I18n class
			// In 1.12.2, the field is called "localizations" (SRG name: field_135054_a)
			java.lang.reflect.Field localizationsField = null;
			
			// Try different possible field names
			String[] possibleNames = {"localizations", "field_135054_a", "i18nLocale"};
			for (String fieldName : possibleNames) {
				try {
					localizationsField = I18n.class.getDeclaredField(fieldName);
					break;
				} catch (NoSuchFieldException e) {
					// Try next name
				}
			}
			
			if (localizationsField == null) {
				Menhir.logger.error("Could not find I18n localizations field. Tried: {}", String.join(", ", possibleNames));
				return;
			}
			
			localizationsField.setAccessible(true);
			Object localizationsObj = localizationsField.get(null);

			if (localizationsObj == null) {
				Menhir.logger.error("Could not access I18n localizations object");
				return;
			}

			// The field contains a Locale object, we need to access its 'properties' field
			// which is a Map<String, String>
			Map<String, String> localizations = null;
			
			// If it's already a Map, use it directly (for some MC versions)
			if (localizationsObj instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, String> directMap = (Map<String, String>) localizationsObj;
				localizations = directMap;
			} else {
				// It's a Locale object, get the properties field
				Class<?> localeClass = localizationsObj.getClass();
				Field propertiesField = localeClass.getDeclaredField("properties");
				propertiesField.setAccessible(true);
				@SuppressWarnings("unchecked")
				Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(localizationsObj);
				localizations = propertiesMap;
			}
			
			if (localizations == null) {
				Menhir.logger.error("Could not access translation properties map");
				return;
			}

			// Inject our custom translations
			int injected = 0;
			for (Map.Entry<String, String> entry : customTranslations.entrySet()) {
				localizations.put(entry.getKey(), entry.getValue());
				injected++;
			}

			Menhir.logger.info("Successfully injected {} custom translations into I18n system", injected);

		} catch (IllegalAccessException e) {
			Menhir.logger.error("Could not access required field for translation injection");
			e.printStackTrace();
		} catch (ClassCastException e) {
			Menhir.logger.error("Unexpected type for localizations field");
			e.printStackTrace();
		} catch (Exception e) {
			Menhir.logger.error("Unexpected error injecting custom translations: {}", e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Gets a custom translation if available, or null if not found.
	 *
	 * @param key The translation key
	 * @return The translated string, or null if not found in custom translations
	 */
	public static String getCustomTranslation(String key) {
		return customTranslations.get(key);
	}

	/**
	 * Checks if a custom translation exists for the given key.
	 *
	 * @param key The translation key
	 * @return true if a custom translation exists
	 */
	public static boolean hasCustomTranslation(String key) {
		return customTranslations.containsKey(key);
	}
}
