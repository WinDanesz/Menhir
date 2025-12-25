package com.windanesz.menhir.api.altar;

import net.minecraft.util.text.TextFormatting;

public enum AltarRarity {
	COMMON("common", TextFormatting.WHITE, 1.0),
	UNCOMMON("uncommon", TextFormatting.GREEN, 0.5),
	RARE("rare", TextFormatting.BLUE, 0.25),
	EPIC("epic", TextFormatting.LIGHT_PURPLE, 0.1),
	LEGENDARY("legendary", TextFormatting.GOLD, 0.05);

	private final String name;
	private final TextFormatting color;
	private final double spawnWeight;

	AltarRarity(String name, TextFormatting color, double spawnWeight) {
		this.name = name;
		this.color = color;
		this.spawnWeight = spawnWeight;
	}

	public String getName() {
		return name;
	}

	public TextFormatting getColor() {
		return color;
	}

	public double getSpawnWeight() {
		return spawnWeight;
	}

	public static AltarRarity fromString(String name) {
		for (AltarRarity rarity : values()) {
			if (rarity.name.equalsIgnoreCase(name)) {
				return rarity;
			}
		}
		return COMMON;
	}
}
