package com.windanesz.menhir.core;

import com.windanesz.menhir.api.IBirthsignData;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.Map;

public class BirthsignData implements IBirthsignData {
	private final Map<String, Integer> data = new HashMap<>();
	private final Map<String, String> stringData = new HashMap<>();
	private String birthsign = "";

	@Override
	public String getBirthsign() {
		return birthsign;
	}

	@Override
	public void setBirthsign(String birthsign) {
		this.birthsign = birthsign;
	}

	@Override
	public int getInt(String key) {
		return data.getOrDefault(key, 0);
	}

	@Override
	public void setInt(String key, int value) {
		data.put(key, value);
	}

	@Override
	public String getString(String key) {
		return stringData.getOrDefault(key, "");
	}

	@Override
	public void setString(String key, String value) {
		stringData.put(key, value);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		birthsign = nbt.getString("Birthsign");
		NBTTagCompound dataTag = nbt.getCompoundTag("BirthsignData");
		data.clear();
		for (String key : dataTag.getKeySet()) {
			data.put(key, dataTag.getInteger(key));
		}

		NBTTagCompound stringDataTag = nbt.getCompoundTag("BirthsignStringData");
		stringData.clear();
		for (String key : stringDataTag.getKeySet()) {
			stringData.put(key, stringDataTag.getString(key));
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setString("Birthsign", birthsign);
		NBTTagCompound dataTag = new NBTTagCompound();
		for (Map.Entry<String, Integer> entry : data.entrySet()) {
			dataTag.setInteger(entry.getKey(), entry.getValue());
		}
		nbt.setTag("BirthsignData", dataTag);

		NBTTagCompound stringDataTag = new NBTTagCompound();
		for (Map.Entry<String, String> entry : stringData.entrySet()) {
			stringDataTag.setString(entry.getKey(), entry.getValue());
		}
		nbt.setTag("BirthsignStringData", stringDataTag);
	}
} 