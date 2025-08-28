package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nullable;
import java.util.Map;

public class BurningAttackAbility implements IBirthsignActiveAbility {

	private final double igniteChance;
	private final int igniteDuration;

	public BurningAttackAbility(double igniteChance, int igniteDuration) {
		this.igniteChance = igniteChance;
		this.igniteDuration = igniteDuration;
	}

	public static BurningAttackAbility create(Map<String, Object> params, String birthsignName) {
		double igniteChance = 0.20; // Default 20%
		int igniteDuration = 3; // Default 3 seconds

		if (params.containsKey("ignite_chance")) {
			Object chanceObj = params.get("ignite_chance");
			if (chanceObj instanceof Number) {
				igniteChance = ((Number) chanceObj).doubleValue();
			}
		}

		if (params.containsKey("ignite_duration")) {
			Object durationObj = params.get("ignite_duration");
			if (durationObj instanceof Number) {
				igniteDuration = ((Number) durationObj).intValue();
			}
		}

		return new BurningAttackAbility(igniteChance, igniteDuration);
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		// This is a passive ability, not actively used
		// The actual effect is handled by the event handler
		return false;
	}

	/**
	 * Called when a player attacks an entity with a melee weapon
	 */
	public void onMeleeAttack(EntityPlayer attacker, Entity target) {
		if (target instanceof EntityLivingBase && attacker.world.rand.nextDouble() < igniteChance) {
			EntityLivingBase livingTarget = (EntityLivingBase) target;
			// Apply fire effect (equivalent to being set on fire)
			livingTarget.setFire(igniteDuration);
		}
	}

	public double getIgniteChance() {
		return igniteChance;
	}

	public int getIgniteDuration() {
		return igniteDuration;
	}
}
