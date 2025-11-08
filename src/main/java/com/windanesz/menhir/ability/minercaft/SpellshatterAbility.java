package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.ability.ChannelingAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpellshatterAbility extends ChannelingAbility {
	
	public SpellshatterAbility(int chargeup) {
		super(chargeup);
	}

	public static SpellshatterAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, 0);
		return new SpellshatterAbility(chargeup);
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		// Remove all active potion effects safely
		List<Potion> toRemove = new ArrayList<>();
		for (PotionEffect effect : player.getActivePotionEffects()) {
			toRemove.add(effect.getPotion());
		}

		boolean removed = false;
		for (Potion potion : toRemove) {
			player.removePotionEffect(potion);
			removed = true;
		}
		return removed;
	}
} 