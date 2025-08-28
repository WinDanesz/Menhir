package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Map;

public class GetItemAbility implements IBirthsignActiveAbility {

	private final String itemId;
	private final int count;

	public GetItemAbility(String itemId, int count) {
		this.itemId = itemId;
		this.count = count;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		String item = ParameterUtils.getStringParameter(params, "item", "");
		int count = ParameterUtils.getIntParameter(params, "count", 1);
		return new GetItemAbility(item, count);
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		if (itemId == null || itemId.isEmpty()) {
			sendNoItemConfiguredMessage(player);
			return false;
		}

		Item item = Item.getByNameOrId(itemId);
		if (item == null) {
			sendItemNotFoundMessage(player, itemId);
			return false;
		}

		ItemStack itemStack = new ItemStack(item, count);
		boolean success = player.inventory.addItemStackToInventory(itemStack);

		if (success) {
			sendItemGivenMessage(player, itemStack);
			return true;
		} else {
			// Drop the item on the ground if inventory is full
			player.dropItem(itemStack, false);
			return true;
		}
	}

	private void sendNoItemConfiguredMessage(EntityPlayer player) {
		player.sendMessage(new TextComponentString(
				TextFormatting.RED + "No item configured for this ability!"
		));
	}

	private void sendItemNotFoundMessage(EntityPlayer player, String itemId) {
		player.sendMessage(new TextComponentString(
				TextFormatting.RED + "Error: Could not find item: " + itemId
		));
	}

	private void sendItemGivenMessage(EntityPlayer player, ItemStack itemStack) {
		player.sendMessage(new TextComponentString(
				TextFormatting.GREEN + "Received: " +
						TextFormatting.GOLD + itemStack.getCount() + "x " + itemStack.getDisplayName()
		));
	}
}
