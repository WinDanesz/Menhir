package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.ability.ChannelingAbility;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

public class BlinkAbility extends ChannelingAbility {
	private final double maxDistance;

	public BlinkAbility(int chargeup, double maxDistance) {
		super(chargeup);
		this.maxDistance = maxDistance;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, 0); // Default: instant
		double range = ParameterUtils.getDoubleParameter(params, "range", 32.0);
		return new BlinkAbility(chargeup, range);
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		World world = player.world;
		Vec3d look = player.getLookVec();
		Vec3d start = player.getPositionEyes(1.0F);
		Vec3d end = start.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);
		RayTraceResult result = world.rayTraceBlocks(start, end, false, true, false);
		Vec3d dest;
		
		if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) {
			// Aim just before the block
			dest = result.hitVec.subtract(look.scale(0.5));
		} else {
			dest = end;
		}
		
		// Create an ender pearl at the destination that will teleport the player
		EntityEnderPearl pearl = new EntityEnderPearl(world, player);
		pearl.setPosition(dest.x, dest.y, dest.z);
		pearl.motionX = 0;
		pearl.motionY = 0;
		pearl.motionZ = 0;
		world.spawnEntity(pearl);
		return true;
	}
}