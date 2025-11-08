package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.ability.ChannelingAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Map;

public class RepairItemAbility extends ChannelingAbility {

	private final double restorePercent;

	public RepairItemAbility(int chargeup, double restorePercent) {
		super(chargeup);
		this.restorePercent = restorePercent;
	}

	public static RepairItemAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, 0);
		double restorePercent = 0.25; // Default 25%

		if (params.containsKey("restore_percent")) {
			Object percentObj = params.get("restore_percent");
			if (percentObj instanceof Number) {
				restorePercent = ((Number) percentObj).doubleValue();
			}
		}

		return new RepairItemAbility(chargeup, restorePercent);
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		ItemStack mainHand = player.getHeldItem(EnumHand.MAIN_HAND);
		ItemStack offHand = player.getHeldItem(EnumHand.OFF_HAND);

		// Try to repair main hand item first, then off hand
		if (canRepairItem(mainHand)) {
			return repairItem(player, mainHand, EnumHand.MAIN_HAND);
		} else if (canRepairItem(offHand)) {
			return repairItem(player, offHand, EnumHand.OFF_HAND);
		} else {
			player.sendMessage(new TextComponentString(TextFormatting.RED + "No repairable item found in your hands!"));
			return false;
		}
	}

	private boolean canRepairItem(ItemStack stack) {
		return !stack.isEmpty() && stack.isItemDamaged() && stack.getItem().isRepairable();
	}

	private boolean repairItem(EntityPlayer player, ItemStack stack, EnumHand hand) {
		int maxDamage = stack.getMaxDamage();
		int currentDamage = stack.getItemDamage();

		// Calculate how much durability to restore
		int restoreAmount = (int) (maxDamage * restorePercent);

		if (restoreAmount > 0) {
			// Repair the item
			int newDamage = Math.max(0, currentDamage - restoreAmount);
			stack.setItemDamage(newDamage);

			// Update the player's inventory
			if (hand == EnumHand.MAIN_HAND) {
				player.setHeldItem(EnumHand.MAIN_HAND, stack);
			} else {
				player.setHeldItem(EnumHand.OFF_HAND, stack);
			}

			// Send success message
			String itemName = stack.getDisplayName();
			player.sendMessage(new TextComponentString(TextFormatting.GREEN +
					"Emberheart Blessing restored " + restoreAmount + " durability to " + itemName + "!"));
			
			return newDamage != currentDamage;
		}
		return false;
	}

	public double getRestorePercent() {
		return restorePercent;
	}
}
