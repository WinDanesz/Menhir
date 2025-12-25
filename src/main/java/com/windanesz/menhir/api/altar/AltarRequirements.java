package com.windanesz.menhir.api.altar;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

public class AltarRequirements {
	private List<ItemRequirement> items = new ArrayList<>();
	private int requiredXPLevels;
	private int requiredXPAmount;
	private boolean consumeXP;
	private List<String> requiredAdvancements = new ArrayList<>();
	private String requiredBirthsign;

	public static class ItemRequirement {
		private ItemStack itemStack;
		private int minCount;
		private int maxCount;
		private NBTTagCompound requiredNBT;
		private boolean consumed;

		public ItemRequirement(ItemStack itemStack, int minCount, int maxCount, NBTTagCompound requiredNBT, boolean consumed) {
			this.itemStack = itemStack;
			this.minCount = minCount;
			this.maxCount = maxCount;
			this.requiredNBT = requiredNBT;
			this.consumed = consumed;
		}

		public ItemStack getItemStack() {
			return itemStack;
		}

		public int getMinCount() {
			return minCount;
		}

		public int getMaxCount() {
			return maxCount;
		}

		public NBTTagCompound getRequiredNBT() {
			return requiredNBT;
		}

		public boolean isConsumed() {
			return consumed;
		}
	}

	public List<ItemRequirement> getItems() {
		return items;
	}

	public void addItem(ItemRequirement item) {
		this.items.add(item);
	}

	public int getRequiredXPLevels() {
		return requiredXPLevels;
	}

	public void setRequiredXPLevels(int requiredXPLevels) {
		this.requiredXPLevels = requiredXPLevels;
	}

	public int getRequiredXPAmount() {
		return requiredXPAmount;
	}

	public void setRequiredXPAmount(int requiredXPAmount) {
		this.requiredXPAmount = requiredXPAmount;
	}

	public boolean isConsumeXP() {
		return consumeXP;
	}

	public void setConsumeXP(boolean consumeXP) {
		this.consumeXP = consumeXP;
	}

	public List<String> getRequiredAdvancements() {
		return requiredAdvancements;
	}

	public void addAdvancement(String advancement) {
		this.requiredAdvancements.add(advancement);
	}

	public String getRequiredBirthsign() {
		return requiredBirthsign;
	}

	public void setRequiredBirthsign(String requiredBirthsign) {
		this.requiredBirthsign = requiredBirthsign;
	}
}
