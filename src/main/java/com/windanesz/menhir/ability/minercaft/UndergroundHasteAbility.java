package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.ability.ChannelingAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Conditional passive ability that gives Haste I when underground (Y ≤ 40) and holding a pickaxe.
 * This is used by The Mountain birthsign sign.
 */
public class UndergroundHasteAbility extends ChannelingAbility {

	private static final int UNDERGROUND_Y_LEVEL = 40;
	private static final int HASTE_AMPLIFIER = 0; // Haste I
	private static final int HASTE_DURATION = 200; // 10 seconds

	public UndergroundHasteAbility(int chargeup) {
		super(chargeup);
	}

	public static UndergroundHasteAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, 0);
		return new UndergroundHasteAbility(chargeup);
	}

	/**
	 * Checks if the player should have the Haste effect based on conditions.
	 *
	 * @param player The player to check
	 * @return true if the player should have Haste, false otherwise
	 */
	public static boolean shouldApplyHaste(EntityPlayer player) {
		if (player == null || player.world == null) {
			return false;
		}

		// Check if player is underground (Y ≤ 40)
		if (player.posY > UNDERGROUND_Y_LEVEL) {
			return false;
		}

		// Check if player is holding a pickaxe in main hand
		ItemStack mainHand = player.getHeldItemMainhand();
		if (mainHand.isEmpty() || mainHand.getItem() != Items.DIAMOND_PICKAXE &&
				mainHand.getItem() != Items.IRON_PICKAXE &&
				mainHand.getItem() != Items.GOLDEN_PICKAXE &&
				mainHand.getItem() != Items.STONE_PICKAXE &&
				mainHand.getItem() != Items.WOODEN_PICKAXE) {
			return false;
		}

		return true;
	}

	/**
	 * Applies the Haste effect to the player if conditions are met.
	 *
	 * @param player The player to apply the effect to
	 */
	public static void applyHasteIfConditionsMet(EntityPlayer player) {
		if (shouldApplyHaste(player)) {
			Potion haste = Potion.getPotionFromResourceLocation("minecraft:haste");
			if (haste != null) {
				PotionEffect hasteEffect = new PotionEffect(haste, HASTE_DURATION, HASTE_AMPLIFIER, true, true);
				player.addPotionEffect(hasteEffect);
			}
		}
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		// This is a passive ability, so executeAbility is empty
		// The effect is applied continuously in the event handler
		return false;
	}
}
