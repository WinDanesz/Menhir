package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

public class BlinkAbility implements IBirthsignActiveAbility {
	private final double maxDistance;

	public BlinkAbility(double maxDistance) {
		this.maxDistance = maxDistance;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		double range = ParameterUtils.getDoubleParameter(params, "range", 32.0);
		return new BlinkAbility(range);
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		World world = player.world;
		Vec3d look = player.getLookVec();
		Vec3d start = player.getPositionEyes(1.0F);
		Vec3d end = start.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);
		RayTraceResult result = world.rayTraceBlocks(start, end, false, true, false);
		Vec3d dest;
		if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) {
			// Teleport just before the block
			dest = result.hitVec.subtract(look.scale(0.5));
		} else {
			dest = end;
		}
		// Check for safe spot (not inside a block)
		BlockPos pos = new BlockPos(dest.x, dest.y, dest.z);
		IBlockState state = world.getBlockState(pos);
		if (state.getMaterial() == Material.AIR || state.getMaterial().isReplaceable()) {
			player.setPositionAndUpdate(dest.x, dest.y, dest.z);
			world.playEvent(2003, pos, 0); // Portal particles
		} else {
			// Try one block above
			BlockPos above = pos.up();
			IBlockState stateAbove = world.getBlockState(above);
			if (stateAbove.getMaterial() == Material.AIR || stateAbove.getMaterial().isReplaceable()) {
				player.setPositionAndUpdate(dest.x, dest.y + 1, dest.z);
				world.playEvent(2003, above, 0);
			}
		}
		return true;
	}
} 