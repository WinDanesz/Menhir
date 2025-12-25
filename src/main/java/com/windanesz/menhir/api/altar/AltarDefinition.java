package com.windanesz.menhir.api.altar;

import java.util.ArrayList;
import java.util.List;

public class AltarDefinition {
	private String id;
	private String name;
	private AltarRarity rarity;
	private double inTierWeight = 1.0;
	private List<GuardianSpawn> guardians = new ArrayList<>();
	private double globalGuardianChance = -1.0; // -1 means use default
	private List<AltarEffect> effects = new ArrayList<>();
	private boolean uniquePerWorld;
	private UsageType usageType;
	private int usageLimit; // Used for both TIMES_PER_PLAYER and TIMES_BY_ANYONE
	private boolean obfuscated;
	private String requiredBirthsign;
	private List<String> requiredMods = new ArrayList<>();
	private int channelTime; // in ticks
	private List<TimeOfDay> allowedTimesOfDay = new ArrayList<>();
	private boolean excludeFromChaotic;
	private AltarRequirements requirements;
	
	// Custom messages
	private String guardianChallengeMessage = "Defeat the guardians to claim your reward!";
	private String guardianSuccessMessage = "Guardians defeated! Your reward has been granted.";

	public enum UsageType {
		UNLIMITED("unlimited"),
		TIMES_PER_PLAYER("times_per_player"),
		TIMES_BY_ANYONE("times_by_anyone");

		private final String name;

		UsageType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public static UsageType fromString(String name) {
			for (UsageType type : values()) {
				if (type.name.equalsIgnoreCase(name)) {
					return type;
				}
			}
			return UNLIMITED;
		}
	}

	public enum TimeOfDay {
		DAY("day", 0, 12000),
		NOON("noon", 5000, 7000),
		DUSK("dusk", 11000, 13000),
		NIGHT("night", 12000, 24000),
		MIDNIGHT("midnight", 17000, 19000),
		DAWN("dawn", 23000, 1000);

		private final String name;
		private final int startTime;
		private final int endTime;

		TimeOfDay(String name, int startTime, int endTime) {
			this.name = name;
			this.startTime = startTime;
			this.endTime = endTime;
		}

		public String getName() {
			return name;
		}

		public boolean isValidTime(long worldTime) {
			long time = worldTime % 24000;
			if (startTime < endTime) {
				return time >= startTime && time <= endTime;
			} else {
				// Wraps around midnight
				return time >= startTime || time <= endTime;
			}
		}

		public static TimeOfDay fromString(String name) {
			for (TimeOfDay time : values()) {
				if (time.name.equalsIgnoreCase(name)) {
					return time;
				}
			}
			return null;
		}
	}

	public AltarDefinition(String id, String name, AltarRarity rarity) {
		this.id = id;
		this.name = name;
		this.rarity = rarity;
		this.usageType = UsageType.UNLIMITED;
		this.requirements = new AltarRequirements();
	}

	// Getters and setters
	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AltarRarity getRarity() {
		return rarity;
	}

	public void setRarity(AltarRarity rarity) {
		this.rarity = rarity;
	}

	public double getInTierWeight() {
		return inTierWeight;
	}

	public void setInTierWeight(double inTierWeight) {
		this.inTierWeight = inTierWeight;
	}

	public List<GuardianSpawn> getGuardians() {
		return guardians;
	}

	public void addGuardian(GuardianSpawn guardian) {
		this.guardians.add(guardian);
	}

	public double getGlobalGuardianChance() {
		return globalGuardianChance;
	}

	public void setGlobalGuardianChance(double globalGuardianChance) {
		this.globalGuardianChance = globalGuardianChance;
	}

	public List<AltarEffect> getEffects() {
		return effects;
	}

	public void addEffect(AltarEffect effect) {
		this.effects.add(effect);
	}

	public boolean isUniquePerWorld() {
		return uniquePerWorld;
	}

	public void setUniquePerWorld(boolean uniquePerWorld) {
		this.uniquePerWorld = uniquePerWorld;
	}

	public UsageType getUsageType() {
		return usageType;
	}

	public void setUsageType(UsageType usageType) {
		this.usageType = usageType;
	}

	public int getUsageLimit() {
		return usageLimit;
	}

	public void setUsageLimit(int usageLimit) {
		this.usageLimit = usageLimit;
	}

	public boolean isObfuscated() {
		return obfuscated;
	}

	public void setObfuscated(boolean obfuscated) {
		this.obfuscated = obfuscated;
	}

	public String getRequiredBirthsign() {
		return requiredBirthsign;
	}

	public void setRequiredBirthsign(String requiredBirthsign) {
		this.requiredBirthsign = requiredBirthsign;
	}

	public List<String> getRequiredMods() {
		return requiredMods;
	}

	public void addRequiredMod(String modId) {
		this.requiredMods.add(modId);
	}

	public int getChannelTime() {
		return channelTime;
	}

	public void setChannelTime(int channelTime) {
		this.channelTime = channelTime;
	}

	public List<TimeOfDay> getAllowedTimesOfDay() {
		return allowedTimesOfDay;
	}

	public void addAllowedTimeOfDay(TimeOfDay timeOfDay) {
		this.allowedTimesOfDay.add(timeOfDay);
	}

	public boolean isExcludeFromChaotic() {
		return excludeFromChaotic;
	}

	public void setExcludeFromChaotic(boolean excludeFromChaotic) {
		this.excludeFromChaotic = excludeFromChaotic;
	}

	public AltarRequirements getRequirements() {
		return requirements;
	}

	public void setRequirements(AltarRequirements requirements) {
		this.requirements = requirements;
	}

	public boolean isValidTimeOfDay(long worldTime) {
		if (allowedTimesOfDay.isEmpty()) {
			return true; // No time restrictions
		}
		for (TimeOfDay time : allowedTimesOfDay) {
			if (time.isValidTime(worldTime)) {
				return true;
			}
		}
		return false;
	}

	public String getGuardianChallengeMessage() {
		return guardianChallengeMessage;
	}

	public void setGuardianChallengeMessage(String guardianChallengeMessage) {
		this.guardianChallengeMessage = guardianChallengeMessage != null ? guardianChallengeMessage : "Defeat the guardians to claim your reward!";
	}

	public String getGuardianSuccessMessage() {
		return guardianSuccessMessage;
	}

	public void setGuardianSuccessMessage(String guardianSuccessMessage) {
		this.guardianSuccessMessage = guardianSuccessMessage != null ? guardianSuccessMessage : "Guardians defeated! Your reward has been granted.";
	}
}
