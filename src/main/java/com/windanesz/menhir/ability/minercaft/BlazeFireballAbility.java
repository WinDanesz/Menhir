package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

public class BlazeFireballAbility implements IBirthsignActiveAbility {
	private final double speedMultiplier;
	private final double damageMultiplier;
	private final double spawnDistance;

	/**
	 * Default constructor
	 */
	public BlazeFireballAbility() {
		this(1.0, 1.0, 1.5);
	}

	public BlazeFireballAbility(double speedMultiplier, double damageMultiplier, double spawnDistance) {
		this.speedMultiplier = speedMultiplier;
		this.damageMultiplier = damageMultiplier;
		this.spawnDistance = spawnDistance;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		double speed = ParameterUtils.getDoubleParameter(params, "speed_multiplier", 1.0);
		double damage = ParameterUtils.getDoubleParameter(params, "damage_multiplier", 1.0);
		double distance = ParameterUtils.getDoubleParameter(params, "spawn_distance", 1.5);
		return new BlazeFireballAbility(speed, damage, distance);
	}

	public static boolean validateParams(Map<String, Object> params) {
		return true; // all parameters are optional
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		World world = player.world;
		if (!world.isRemote) {
			Vec3d look;
			if (target != null) {
				double dx = target.posX - player.posX;
				double dy = (target.posY + target.getEyeHeight()) - (player.posY + player.getEyeHeight());
				double dz = target.posZ - player.posZ;
				look = new Vec3d(dx, dy, dz).normalize();
			} else {
				look = player.getLookVec();
			}
			double x = player.posX + look.x * spawnDistance;
			double y = player.posY + player.getEyeHeight() + look.y * spawnDistance;
			double z = player.posZ + look.z * spawnDistance;
			EntitySmallFireball fireball = new EntitySmallFireball(world, x, y, z, look.x * speedMultiplier, look.y * speedMultiplier, look.z * speedMultiplier);
			fireball.shootingEntity = player;
			world.spawnEntity(fireball);
		}
		return true;
	}
} 