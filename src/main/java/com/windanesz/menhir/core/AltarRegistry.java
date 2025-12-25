package com.windanesz.menhir.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.altar.AltarDefinition;
import com.windanesz.menhir.api.altar.AltarEffect;
import com.windanesz.menhir.api.altar.AltarRarity;
import com.windanesz.menhir.api.altar.AltarRequirements;
import com.windanesz.menhir.api.altar.GuardianSpawn;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AltarRegistry {
	private static final Map<String, AltarDefinition> ALTARS = new HashMap<>();
	private static final Map<AltarRarity, List<AltarDefinition>> ALTARS_BY_RARITY = new HashMap<>();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Random RANDOM = new Random();

	static {
		for (AltarRarity rarity : AltarRarity.values()) {
			ALTARS_BY_RARITY.put(rarity, new ArrayList<>());
		}
	}

	/**
	 * Load altar definitions from JSON files in the config directory
	 */
	public static void loadAltarDefinitions(File configDir) {
		Menhir.logger.info("Loading altar definitions from config directory: " + configDir.getAbsolutePath());
		
		File altarsDir = new File(configDir, "menhir/altars");
		if (!altarsDir.exists()) {
			altarsDir.mkdirs();
			Menhir.logger.warn("Created empty altars directory: " + altarsDir.getAbsolutePath());
			Menhir.logger.warn("No default altar definitions will be loaded. Add JSON files to this directory.");
			return;
		}

		File[] files = altarsDir.listFiles((dir, name) -> name.endsWith(".json"));
		if (files == null || files.length == 0) {
			Menhir.logger.warn("No altar definition files found in " + altarsDir.getAbsolutePath());
			return;
		}

		Menhir.logger.info("Found " + files.length + " altar JSON files to load");
		
		int loadedCount = 0;
		int failedCount = 0;
		for (File file : files) {
			try {
				Menhir.logger.info("Loading altar from: " + file.getName());
				loadAltarFromFile(file);
				loadedCount++;
			} catch (Exception e) {
				failedCount++;
				Menhir.logger.error("Failed to load altar definition from " + file.getName(), e);
			}
		}

		Menhir.logger.info("Successfully loaded " + loadedCount + " altar definitions (" + failedCount + " failed)");
		Menhir.logger.info("Total altars registered: " + ALTARS.size());
	}

	private static void loadAltarFromFile(File file) throws IOException {
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(new FileReader(file)).getAsJsonObject();

		String id = json.get("id").getAsString();
		String name = json.get("name").getAsString();
		AltarRarity rarity = AltarRarity.fromString(json.get("rarity").getAsString());

		AltarDefinition altar = new AltarDefinition(id, name, rarity);

		// Optional fields
		if (json.has("in_tier_weight")) {
			altar.setInTierWeight(json.get("in_tier_weight").getAsDouble());
		}

		if (json.has("unique_per_world")) {
			altar.setUniquePerWorld(json.get("unique_per_world").getAsBoolean());
		}

		if (json.has("obfuscated")) {
			altar.setObfuscated(json.get("obfuscated").getAsBoolean());
		}

		if (json.has("required_birthsign")) {
			altar.setRequiredBirthsign(json.get("required_birthsign").getAsString());
		}

		if (json.has("channel_time")) {
			altar.setChannelTime(json.get("channel_time").getAsInt());
		}

		if (json.has("exclude_from_chaotic")) {
			altar.setExcludeFromChaotic(json.get("exclude_from_chaotic").getAsBoolean());
		}

		if (json.has("global_guardian_chance")) {
			altar.setGlobalGuardianChance(json.get("global_guardian_chance").getAsDouble());
		}

		// Load usage configuration
		if (json.has("usage")) {
			JsonObject usageObj = json.getAsJsonObject("usage");
			if (usageObj.has("type")) {
				String usageType = usageObj.get("type").getAsString();
				altar.setUsageType(AltarDefinition.UsageType.fromString(usageType));
				
				if (usageObj.has("limit")) {
					altar.setUsageLimit(usageObj.get("limit").getAsInt());
				}
			}
		}

		// Load required mods
		if (json.has("required_mods")) {
			JsonArray modsArray = json.getAsJsonArray("required_mods");
			for (JsonElement modElement : modsArray) {
				String modId = modElement.getAsString();
				altar.addRequiredMod(modId);
				
				// Check if mod is loaded
				if (!Loader.isModLoaded(modId)) {
					Menhir.logger.warn("Altar " + id + " requires mod " + modId + " which is not loaded. Skipping.");
					return;
				}
			}
		}

		// Load allowed times of day
		if (json.has("allowed_times_of_day")) {
			JsonArray timesArray = json.getAsJsonArray("allowed_times_of_day");
			for (JsonElement timeElement : timesArray) {
				AltarDefinition.TimeOfDay time = AltarDefinition.TimeOfDay.fromString(timeElement.getAsString());
				if (time != null) {
					altar.addAllowedTimeOfDay(time);
				}
			}
		}

		// Load guardians
		if (json.has("guardians")) {
			JsonArray guardiansArray = json.getAsJsonArray("guardians");
			for (JsonElement guardianElement : guardiansArray) {
				GuardianSpawn guardian = parseGuardian(guardianElement.getAsJsonObject());
				if (guardian != null) {
					altar.addGuardian(guardian);
				}
			}
		}

		// Load guardian messages
		if (json.has("guardian_challenge_message")) {
			altar.setGuardianChallengeMessage(json.get("guardian_challenge_message").getAsString());
		}
		if (json.has("guardian_success_message")) {
			altar.setGuardianSuccessMessage(json.get("guardian_success_message").getAsString());
		}

		// Load requirements
		if (json.has("requirements")) {
			AltarRequirements requirements = parseRequirements(json.getAsJsonObject("requirements"));
			altar.setRequirements(requirements);
		}

		// Load effects
		if (json.has("effects")) {
			JsonArray effectsArray = json.getAsJsonArray("effects");
			for (JsonElement effectElement : effectsArray) {
				AltarEffect effect = parseEffect(effectElement.getAsJsonObject());
				if (effect != null) {
					altar.addEffect(effect);
				}
			}
		}

		// Register the altar
		registerAltar(altar);
		Menhir.logger.info("Loaded altar: " + id + " (" + rarity.getName() + ")");
	}

	private static GuardianSpawn parseGuardian(JsonObject json) {
		String entityId = json.get("entity").getAsString();
		ResourceLocation entityRL = new ResourceLocation(entityId);

		int minCount = json.has("min_count") ? json.get("min_count").getAsInt() : 1;
		int maxCount = json.has("max_count") ? json.get("max_count").getAsInt() : 1;
		
		GuardianSpawn.SpawnType spawnType = json.has("spawn_type") 
				? GuardianSpawn.SpawnType.fromString(json.get("spawn_type").getAsString())
				: GuardianSpawn.SpawnType.FIRST_INTERACTION;

		double spawnChance = json.has("spawn_chance") ? json.get("spawn_chance").getAsDouble() : 1.0;

		NBTTagCompound entityNBT = null;
		if (json.has("nbt")) {
			try {
				entityNBT = JsonToNBT.getTagFromJson(json.get("nbt").toString());
			} catch (NBTException e) {
				Menhir.logger.error("Failed to parse guardian NBT", e);
			}
		}

		return new GuardianSpawn(entityRL, minCount, maxCount, entityNBT, spawnType, spawnChance);
	}

	private static AltarRequirements parseRequirements(JsonObject json) {
		AltarRequirements requirements = new AltarRequirements();

		if (json.has("xp_levels")) {
			requirements.setRequiredXPLevels(json.get("xp_levels").getAsInt());
		}

		if (json.has("xp_amount")) {
			requirements.setRequiredXPAmount(json.get("xp_amount").getAsInt());
		}

		if (json.has("consume_xp")) {
			requirements.setConsumeXP(json.get("consume_xp").getAsBoolean());
		}

		if (json.has("birthsign")) {
			requirements.setRequiredBirthsign(json.get("birthsign").getAsString());
		}

		if (json.has("advancements")) {
			JsonArray advArray = json.getAsJsonArray("advancements");
			for (JsonElement advElement : advArray) {
				requirements.addAdvancement(advElement.getAsString());
			}
		}

		if (json.has("items")) {
			JsonArray itemsArray = json.getAsJsonArray("items");
			for (JsonElement itemElement : itemsArray) {
				JsonObject itemObj = itemElement.getAsJsonObject();
				
				String itemId = itemObj.get("item").getAsString();
				Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
				if (item == null) {
					Menhir.logger.warn("Unknown item: " + itemId);
					continue;
				}

				int meta = itemObj.has("data") ? itemObj.get("data").getAsInt() : 0;
				int minCount = itemObj.has("min_count") ? itemObj.get("min_count").getAsInt() : 1;
				int maxCount = itemObj.has("max_count") ? itemObj.get("max_count").getAsInt() : minCount;
				boolean consumed = itemObj.has("consumed") ? itemObj.get("consumed").getAsBoolean() : true;

				NBTTagCompound nbt = null;
				if (itemObj.has("nbt")) {
					try {
						nbt = JsonToNBT.getTagFromJson(itemObj.get("nbt").toString());
					} catch (NBTException e) {
						Menhir.logger.error("Failed to parse item NBT", e);
					}
				}

				ItemStack stack = new ItemStack(item, 1, meta);
				AltarRequirements.ItemRequirement itemReq = 
						new AltarRequirements.ItemRequirement(stack, minCount, maxCount, nbt, consumed);
				requirements.addItem(itemReq);
			}
		}

		return requirements;
	}

	private static AltarEffect parseEffect(JsonObject json) {
		String type = json.get("type").getAsString();
		String effectId = json.has("id") ? json.get("id").getAsString() : "effect_" + RANDOM.nextInt(10000);

		AltarEffect effect = null;

		switch (type.toLowerCase()) {
			case "potion":
				String potionId = json.get("potion").getAsString();
				Potion potion = ForgeRegistries.POTIONS.getValue(new ResourceLocation(potionId));
				if (potion == null) {
					Menhir.logger.warn("Unknown potion: " + potionId);
					return null;
				}
				int duration = json.has("duration") ? json.get("duration").getAsInt() : 600;
				int amplifier = json.has("amplifier") ? json.get("amplifier").getAsInt() : 0;
				effect = new AltarEffect.PotionEffect(effectId, potion, duration, amplifier);
				break;

			case "command":
				String command = json.get("command").getAsString();
				effect = new AltarEffect.CommandEffect(effectId, command);
				break;

			case "teleport_recall":
				int maxUses = json.has("max_uses") ? json.get("max_uses").getAsInt() : 1;
				effect = new AltarEffect.TeleportRecallEffect(effectId, maxUses);
				break;

			case "teleport_twin":
				String twinId = json.get("twin_id").getAsString();
				effect = new AltarEffect.TeleportTwinEffect(effectId, twinId);
				break;

			case "prayer_displayed":
			case "prayer_hidden":
				String prayerText = json.get("prayer_text").getAsString();
				boolean hidden = type.equalsIgnoreCase("prayer_hidden");
				List<AltarEffect> successEffects = new ArrayList<>();
				if (json.has("success_effects")) {
					JsonArray successArray = json.getAsJsonArray("success_effects");
					for (JsonElement successElement : successArray) {
						AltarEffect successEffect = parseEffect(successElement.getAsJsonObject());
						if (successEffect != null) {
							successEffects.add(successEffect);
						}
					}
				}
				effect = new AltarEffect.PrayerEffect(effectId, prayerText, hidden, successEffects);
				break;

			default:
				Menhir.logger.warn("Unknown effect type: " + type);
				return null;
		}

		// Parse common properties
		if (effect != null) {
			if (json.has("unique_per_world")) {
				effect.setUniquePerWorld(json.get("unique_per_world").getAsBoolean());
			}
			if (json.has("single_use")) {
				effect.setSingleUse(json.get("single_use").getAsBoolean());
			}
			if (json.has("obfuscated")) {
				effect.setObfuscated(json.get("obfuscated").getAsBoolean());
			}
			if (json.has("once_per_player")) {
				effect.setOncePerPlayer(json.get("once_per_player").getAsBoolean());
			}
		}

		return effect;
	}

	public static void registerAltar(AltarDefinition altar) {
		ALTARS.put(altar.getId(), altar);
		ALTARS_BY_RARITY.get(altar.getRarity()).add(altar);
	}

	public static AltarDefinition getAltarDefinition(String id) {
		return ALTARS.get(id);
	}

	public static List<AltarDefinition> getAllAltars() {
		return new ArrayList<>(ALTARS.values());
	}

	public static List<AltarDefinition> getAltarsByRarity(AltarRarity rarity) {
		return new ArrayList<>(ALTARS_BY_RARITY.get(rarity));
	}

	/**
	 * Get a random altar definition based on rarity weights
	 */
	public static AltarDefinition getRandomAltar(Random random) {
		// First, pick a rarity tier
		double totalWeight = 0.0;
		for (AltarRarity rarity : AltarRarity.values()) {
			totalWeight += rarity.getSpawnWeight();
		}

		double roll = random.nextDouble() * totalWeight;
		double current = 0.0;
		AltarRarity selectedRarity = AltarRarity.COMMON;

		for (AltarRarity rarity : AltarRarity.values()) {
			current += rarity.getSpawnWeight();
			if (roll <= current) {
				selectedRarity = rarity;
				break;
			}
		}

		// Then pick an altar from that rarity tier
		List<AltarDefinition> altarsInTier = ALTARS_BY_RARITY.get(selectedRarity);
		if (altarsInTier.isEmpty()) {
			return null;
		}

        // Apply in-tier weights
		double tierWeight = 0.0;
		for (AltarDefinition altar : altarsInTier) {
			tierWeight += altar.getInTierWeight();
		}

		roll = random.nextDouble() * tierWeight;
		current = 0.0;

		for (AltarDefinition altar : altarsInTier) {
			current += altar.getInTierWeight();
			if (roll <= current) {
				return altar;
			}
		}

		// Fallback to first altar in tier
		return altarsInTier.get(0);
	}

	public static void clear() {
		ALTARS.clear();
		for (List<AltarDefinition> list : ALTARS_BY_RARITY.values()) {
			list.clear();
		}
	}
}
