package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import javax.annotation.Nullable;
import java.util.Map;

public class PotionEffectAbility implements IBirthsignActiveAbility {
	private final String potionName;
	private final int amplifier;
	private final int duration;

	public PotionEffectAbility(String potionName, int amplifier, int duration) {
		this.potionName = potionName;
		this.amplifier = amplifier;
		this.duration = duration;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		String potion = ParameterUtils.getStringParameter(params, "potioneffect", "");
		int amplifier = ParameterUtils.getIntParameter(params, "amplifier", 0);
		int duration = ParameterUtils.getIntParameter(params, "duration", 200);
		return new PotionEffectAbility(potion, amplifier, duration);
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		Potion potion = Potion.getPotionFromResourceLocation(potionName);
		if (potion != null) {
			PotionEffect potionEffect = new PotionEffect(potion, duration, amplifier);
			player.addPotionEffect(potionEffect);
			return true;
		}
		return false;
	}
} 