package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.ability.ChannelingAbility;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

public class TeleportAbility extends ChannelingAbility {

	private static final int DEFAULT_CHANNELING_TICKS = 60;
	private static final String DEFAULT_TELEPORT_DESTINATION = "spawn";

	public TeleportAbility() {
		this(DEFAULT_CHANNELING_TICKS, DEFAULT_TELEPORT_DESTINATION);
	}

	public TeleportAbility(int channelingTicks, String teleportDestination) {
		super(channelingTicks);
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, DEFAULT_CHANNELING_TICKS);
		String teleportDest = ParameterUtils.getStringParameter(params, "teleport_destination", DEFAULT_TELEPORT_DESTINATION);
		return new TeleportAbility(chargeup, teleportDest);
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		return performTeleport(player);
	}

	public boolean performTeleport(EntityPlayer player) {
		// Only perform teleport on the server side
		if (player.world.isRemote) {
			return false;
		}

		World world = player.world;
		BlockPos teleportPos = findTeleportDestination(player, world);

		if (teleportPos != null) {
			return executeTeleport(player, world, teleportPos);
		} else {
			handleTeleportFailure(player);
			return false;
		}
	}

	private BlockPos findTeleportDestination(EntityPlayer player, World world) {
		// Try player's bed location first
		BlockPos bedPos = findBedLocation(player);
		if (bedPos != null) {
			return bedPos;
		}

		// Try world spawn point
		BlockPos worldSpawn = findWorldSpawn(world);
		if (worldSpawn != null) {
			return worldSpawn;
		}

		// Fallback to default spawn
		//BlockPos fallbackSpawn = createFallbackSpawn(world);
		//logDebug("Using fallback spawn: " + fallbackSpawn);
		return null;
	}

	private BlockPos findBedLocation(EntityPlayer player) {
		int spawnDim = player.getSpawnDimension();
		BlockPos bedPos = player.getBedLocation(spawnDim);
		return bedPos;
	}

	private BlockPos findWorldSpawn(World world) {
		BlockPos worldSpawn = world.getSpawnPoint();
		return worldSpawn;
	}

	private boolean executeTeleport(EntityPlayer player, World world, BlockPos teleportPos) {
		int safeY = findSafeTeleportHeight(world, teleportPos);
		double x = teleportPos.getX() + 0.5;
		double y = safeY + 1.0;
		double z = teleportPos.getZ() + 0.5;

		if (performTeleportOperation(player, world, x, y, z)) {
			handleSuccessfulTeleport(player, world, teleportPos);
			return true;
		} else {
			handleTeleportFailure(player);
			return false;
		}
	}

	private boolean performTeleportOperation(EntityPlayer player, World world, double x, double y, double z) {
		try {
			// Ensure we're on the server side
			if (world.isRemote) {
				return false;
			}

			// For server-side teleport, use the proper server teleport method
			if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
				net.minecraft.entity.player.EntityPlayerMP playerMP = (net.minecraft.entity.player.EntityPlayerMP) player;

				// Use the server-side teleport method which will sync to client
				playerMP.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);

				// Clear motion and mark velocity as changed
				clearPlayerMotion(player);

				// Force a position update
				player.setPosition(x, y, z);

				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void clearPlayerMotion(EntityPlayer player) {
		player.motionX = 0;
		player.motionY = 0;
		player.motionZ = 0;
		player.velocityChanged = true;
	}

	private void handleSuccessfulTeleport(EntityPlayer player, World world, BlockPos teleportPos) {

		// Spawn portal particles
		world.playEvent(2003, teleportPos, 0);

		// Send success message
		player.sendMessage(new TextComponentString(
				TextFormatting.GREEN + "Teleported to spawn!"
		));
	}

	private void handleTeleportFailure(EntityPlayer player) {
		player.sendMessage(new TextComponentString(
				TextFormatting.RED + "Error: No valid spawn location found!"
		));
	}

	private int findSafeTeleportHeight(World world, BlockPos pos) {
		int y = world.getHeight();
		while (y > 0 && !world.getBlockState(new BlockPos(pos.getX(), y - 1, pos.getZ())).isFullBlock()) {
			y--;
		}
		return y;
	}
}