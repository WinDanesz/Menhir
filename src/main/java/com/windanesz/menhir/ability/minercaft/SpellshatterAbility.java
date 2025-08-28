package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpellshatterAbility implements IBirthsignActiveAbility {
	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		return new SpellshatterAbility();
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		if (player == null) return false;
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