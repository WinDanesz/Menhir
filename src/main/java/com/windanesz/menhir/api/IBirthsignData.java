package com.windanesz.menhir.api;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Necessary to have an interface for capabilities
 */
public interface IBirthsignData {
	String getBirthsign();

	void setBirthsign(String birthsign);

	int getInt(String key);

	void setInt(String key, int value);

	String getString(String key);

	void setString(String key, String value);

	void readFromNBT(NBTTagCompound nbt);

	void writeToNBT(NBTTagCompound nbt);
} 