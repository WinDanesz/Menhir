package com.windanesz.menhir.api.altar;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GuardianSpawn {
	private ResourceLocation entityId;
	private int minCount;
	private int maxCount;
	private NBTTagCompound entityNBT;
	private SpawnType spawnType;
	private double spawnChance; // 0.0 to 1.0

	public enum SpawnType {
		MOB_SPAWNER("mob_spawner"),
		FIRST_INTERACTION("first_interaction"),
		EVERY_INTERACTION("every_interaction");

		private final String name;

		SpawnType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public static SpawnType fromString(String name) {
			for (SpawnType type : values()) {
				if (type.name.equalsIgnoreCase(name)) {
					return type;
				}
			}
			return FIRST_INTERACTION;
		}
	}

	public GuardianSpawn(ResourceLocation entityId, int minCount, int maxCount, NBTTagCompound entityNBT, SpawnType spawnType, double spawnChance) {
		this.entityId = entityId;
		this.minCount = minCount;
		this.maxCount = maxCount;
		this.entityNBT = entityNBT;
		this.spawnType = spawnType;
		this.spawnChance = spawnChance;
	}

	public ResourceLocation getEntityId() {
		return entityId;
	}

	public int getMinCount() {
		return minCount;
	}

	public int getMaxCount() {
		return maxCount;
	}

	public NBTTagCompound getEntityNBT() {
		return entityNBT;
	}

	public SpawnType getSpawnType() {
		return spawnType;
	}

	public double getSpawnChance() {
		return spawnChance;
	}
}
