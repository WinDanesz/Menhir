package com.windanesz.menhir.ability.minercaft;

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

public class TeleportAbility implements IBirthsignActiveAbility {

	private static final int DEFAULT_CHANNELING_TICKS = 60;
	private static final String DEFAULT_TELEPORT_DESTINATION = "spawn";
	private static final String DEBUG_PREFIX = "[Menhir] Lodestone: ";

	private final int channelingTicks;
	private final String teleportDestination;

	public TeleportAbility() {
		this(DEFAULT_CHANNELING_TICKS, DEFAULT_TELEPORT_DESTINATION);
	}

	public TeleportAbility(int channelingTicks, String teleportDestination) {
		this.channelingTicks = channelingTicks;
		this.teleportDestination = teleportDestination;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		int channelingTicks = ParameterUtils.getIntParameter(params, "channeling_ticks", DEFAULT_CHANNELING_TICKS);
		String teleportDest = ParameterUtils.getStringParameter(params, "teleport_destination", DEFAULT_TELEPORT_DESTINATION);
		return new TeleportAbility(channelingTicks, teleportDest);
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		// Only start channeling on the server side
		if (!player.world.isRemote) {
			performTeleport(player);
			return true;
			//ChannelingManager.startChanneling(player, this, channelingTicks);
		}
		return false;
	}

	public void performTeleport(EntityPlayer player) {
		// Only perform teleport on the server side
		if (player.world.isRemote) {
			return;
		}

		World world = player.world;
		BlockPos teleportPos = findTeleportDestination(player, world);

		if (teleportPos != null) {
			executeTeleport(player, world, teleportPos);
		} else {
			handleTeleportFailure(player);
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

	private BlockPos createFallbackSpawn(World world) {
		int fallbackY = world.getHeight();
		return new BlockPos(0, fallbackY, 0);
	}

	private void executeTeleport(EntityPlayer player, World world, BlockPos teleportPos) {
		TeleportCoordinates coords = calculateSafeTeleportCoordinates(world, teleportPos);

		if (performTeleportOperation(player, world, coords)) {
			handleSuccessfulTeleport(player, world, teleportPos);
		} else {
			handleTeleportFailure(player);
		}
	}

	private TeleportCoordinates calculateSafeTeleportCoordinates(World world, BlockPos teleportPos) {
		int safeY = findSafeTeleportHeight(world, teleportPos);
		double x = teleportPos.getX() + 0.5;
		double y = safeY + 1.0;
		double z = teleportPos.getZ() + 0.5;
		return new TeleportCoordinates(x, y, z);
	}

	private boolean performTeleportOperation(EntityPlayer player, World world, TeleportCoordinates coords) {
		try {
			// Ensure we're on the server side
			if (world.isRemote) {
				return false;
			}

			// For server-side teleport, use the proper server teleport method
			if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
				net.minecraft.entity.player.EntityPlayerMP playerMP = (net.minecraft.entity.player.EntityPlayerMP) player;

				// Use the server-side teleport method which will sync to client
				playerMP.connection.setPlayerLocation(coords.x, coords.y, coords.z, player.rotationYaw, player.rotationPitch);

				// Clear motion and mark velocity as changed
				clearPlayerMotion(player);

				// Force a position update
				player.setPosition(coords.x, coords.y, coords.z);

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

	private static class TeleportCoordinates {
		final double x, y, z;

		TeleportCoordinates(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
} 