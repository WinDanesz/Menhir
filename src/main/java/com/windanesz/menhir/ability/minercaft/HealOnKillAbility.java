package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.ability.ChannelingAbility;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nullable;
import java.util.Map;

public class HealOnKillAbility extends ChannelingAbility {

	private final double healAmount;

	public HealOnKillAbility(int chargeup, double healAmount) {
		super(chargeup);
		this.healAmount = healAmount;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, 0);
		double amount = ParameterUtils.getDoubleParameter(params, "amount", 0.5);
		return new HealOnKillAbility(chargeup, amount);
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		// This ability is passive and doesn't need activation
		// It will be handled by the event system
		return false;
	}

	/**
	 * Called when the player kills an entity
	 *
	 * @param player       The player who got the kill
	 * @param killedEntity The entity that was killed
	 */
	public void onKill(EntityPlayer player, EntityLivingBase killedEntity) {
		if (player.world.isRemote) return; // Only run on server

		float currentHealth = player.getHealth();
		float maxHealth = player.getMaxHealth();

		// Don't heal if already at full health
		if (currentHealth >= maxHealth) return;

		// Calculate heal amount (can be fractional)
		float healValue = (float) healAmount;
		float newHealth = Math.min(currentHealth + healValue, maxHealth);

		// Apply healing
		player.setHealth(newHealth);
	}
}
