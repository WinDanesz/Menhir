package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.ability.ChannelingAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

public class ParticleEffectAbility extends ChannelingAbility {
	private final String particleType;
	private final int particleCount;
	private final double spreadX;
	private final double spreadY;
	private final double spreadZ;

	public ParticleEffectAbility(int chargeup, String particleType, int particleCount, double spreadX, double spreadY, double spreadZ) {
		super(chargeup);
		this.particleType = particleType;
		this.particleCount = particleCount;
		this.spreadX = spreadX;
		this.spreadY = spreadY;
		this.spreadZ = spreadZ;
	}

	public static ParticleEffectAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, 0);
		String particleType = ParameterUtils.getStringParameter(params, "particle_type", "SMOKE_NORMAL");
		int particleCount = ParameterUtils.getIntParameter(params, "particle_count", 20);
		double spreadX = ParameterUtils.getDoubleParameter(params, "particle_spread_x", 2.0);
		double spreadY = ParameterUtils.getDoubleParameter(params, "particle_spread_y", 1.0);
		double spreadZ = ParameterUtils.getDoubleParameter(params, "particle_spread_z", 2.0);

		return new ParticleEffectAbility(chargeup, particleType, particleCount, spreadX, spreadY, spreadZ);
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		World world = player.world;

		// Get the particle type from the string
		EnumParticleTypes particle = getParticleType(particleType);
		if (particle == null) {
			// Fallback to smoke if invalid particle type
			particle = EnumParticleTypes.SMOKE_NORMAL;
		}

		// Spawn particles around the player
		for (int i = 0; i < particleCount; i++) {
			double offsetX = (world.rand.nextDouble() - 0.5) * spreadX;
			double offsetY = world.rand.nextDouble() * spreadY;
			double offsetZ = (world.rand.nextDouble() - 0.5) * spreadZ;

			// Spawn particles on client side
			world.spawnParticle(
					particle,
					player.posX + offsetX,
					player.posY + offsetY,
					player.posZ + offsetZ,
					0.0, 0.0, 0.0
			);
		}
		return true;
	}

	private EnumParticleTypes getParticleType(String particleName) {
		try {
			return EnumParticleTypes.valueOf(particleName.toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
} 