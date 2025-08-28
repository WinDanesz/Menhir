package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class AOEPotionEffectAbility implements IBirthsignActiveAbility {
	private final String potionName;
	private final int amplifier;
	private final int duration;
	private final double radius;
	private final Set<TargetType> targetTypes;

	public AOEPotionEffectAbility(String potionName, int amplifier, int duration, double radius, Set<TargetType> targetTypes) {
		this.potionName = potionName;
		this.amplifier = amplifier;
		this.duration = duration;
		this.radius = radius;
		this.targetTypes = targetTypes;
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		World world = player.world;
		Potion potion = Potion.getPotionFromResourceLocation(potionName);
		if (potion == null) return false;
		double centerX = target != null ? target.posX : player.posX;
		double centerY = target != null ? target.posY + target.getEyeHeight() : player.posY + player.getEyeHeight();
		double centerZ = target != null ? target.posZ : player.posZ;
		AxisAlignedBB aabb = new AxisAlignedBB(
				centerX - radius, centerY - radius, centerZ - radius,
				centerX + radius, centerY + radius, centerZ + radius
		);
		List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, aabb);
		for (EntityLivingBase entity : entities) {
			if (shouldAffect(player, entity)) {
				entity.addPotionEffect(new PotionEffect(potion, duration, amplifier));
			}
		}
		return true;
	}

	private boolean shouldAffect(EntityPlayer player, EntityLivingBase entity) {
		if (entity == player && targetTypes.contains(TargetType.SELF)) {
			return true;
		}
		if (entity instanceof EntityPlayer && entity != player && targetTypes.contains(TargetType.ALLIES)) {
			return true;
		}
		if (!(entity instanceof EntityPlayer) && targetTypes.contains(TargetType.ENEMIES)) {
			return true;
		}
		return false;
	}

	public enum TargetType {
		SELF, ALLIES, ENEMIES
	}
} 