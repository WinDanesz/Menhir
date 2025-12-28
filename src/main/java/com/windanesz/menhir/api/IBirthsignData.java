package com.windanesz.menhir.api;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Necessary to have an interface for capabilities
 */
public interface IBirthsignData {
	String getBirthsign();

	void setBirthsign(String birthsign);

	void setTrait(String category, String traitId);

	String getTrait(String category);

	java.util.Map<String, String> getAllTraits();

	int getInt(String key);

	void setInt(String key, int value);

	String getString(String key);

	void setString(String key, String value);

	void readFromNBT(NBTTagCompound nbt);

	void writeToNBT(NBTTagCompound nbt);
} 