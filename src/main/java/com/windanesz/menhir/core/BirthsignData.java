package com.windanesz.menhir.core;

import com.windanesz.menhir.api.IBirthsignData;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.Map;

public class BirthsignData implements IBirthsignData {
	private final Map<String, Integer> data = new HashMap<>();
	private final Map<String, String> stringData = new HashMap<>();
	private final Map<String, String> selectedTraits = new HashMap<>();

	@Override
	public String getBirthsign() {
		return selectedTraits.getOrDefault("birthsign", "");
	}

	@Override
	public void setBirthsign(String birthsign) {
		setTrait("birthsign", birthsign);
	}

	@Override
	public void setTrait(String category, String traitId) {
		if (traitId == null || traitId.isEmpty()) {
			selectedTraits.remove(category);
		} else {
			selectedTraits.put(category, traitId);
		}
	}

	@Override
	public String getTrait(String category) {
		return selectedTraits.getOrDefault(category, "");
	}

	@Override
	public Map<String, String> getAllTraits() {
		return new HashMap<>(selectedTraits);
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
		selectedTraits.clear();
		// Legacy support
		if (nbt.hasKey("Birthsign")) {
			selectedTraits.put("birthsign", nbt.getString("Birthsign"));
		}
		
		// New map support
		if (nbt.hasKey("SelectedTraits")) {
			NBTTagCompound traitsTag = nbt.getCompoundTag("SelectedTraits");
			for (String key : traitsTag.getKeySet()) {
				selectedTraits.put(key, traitsTag.getString(key));
			}
		}

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
		// Legacy support
		nbt.setString("Birthsign", getBirthsign());
		
		// New map support
		NBTTagCompound traitsTag = new NBTTagCompound();
		for (Map.Entry<String, String> entry : selectedTraits.entrySet()) {
			traitsTag.setString(entry.getKey(), entry.getValue());
		}
		nbt.setTag("SelectedTraits", traitsTag);

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