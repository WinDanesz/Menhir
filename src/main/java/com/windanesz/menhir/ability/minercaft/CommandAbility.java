package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nullable;
import java.util.Map;

public class CommandAbility implements IBirthsignActiveAbility {

	private final String command;
	private final String executeAs;
	private final String playerPlaceholder;

	public CommandAbility(String command, String executeAs, String playerPlaceholder) {
		this.command = command;
		this.executeAs = executeAs;
		this.playerPlaceholder = playerPlaceholder;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		String command = ParameterUtils.getStringParameter(params, "command", "");
		String executeAs = ParameterUtils.getStringParameter(params, "execute_as", "console");
		String playerPlaceholder = ParameterUtils.getStringParameter(params, "player_placeholder", "@p");

		return new CommandAbility(command, executeAs, playerPlaceholder);
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		if (player.world.isRemote) return false; // Only run on server

		MinecraftServer server = player.world.getMinecraftServer();
		if (server == null) return false;

		try {
			// Replace player placeholders in the command
			String processedCommand = processCommandPlaceholders(command, player);

			// Execute the command based on the specified method
			boolean success = false;
			switch (executeAs.toLowerCase()) {
				case "console":
				case "server":
					success = server.commandManager.executeCommand(server, processedCommand) > 0;
					break;
				case "op_player":
					// Execute as the player but with op permissions temporarily
					success = server.commandManager.executeCommand(player, processedCommand) > 0;
					break;
				default:
					// Default to console execution
					success = server.commandManager.executeCommand(server, processedCommand) > 0;
					break;
			}

			if (success) {
				player.sendMessage(new TextComponentString(
						TextFormatting.GREEN + "Command executed successfully: " +
								TextFormatting.GRAY + processedCommand
				));
			} else {
				player.sendMessage(new TextComponentString(
						TextFormatting.RED + "Failed to execute command: " +
								TextFormatting.GRAY + processedCommand
				));
			}

			return success;

		} catch (Exception e) {
			player.sendMessage(new TextComponentString(
					TextFormatting.RED + "Error executing command: " + e.getMessage()
			));
			return false;
		}
	}

	/**
	 * Processes command placeholders to replace them with actual player information
	 */
	private String processCommandPlaceholders(String command, EntityPlayer player) {
		String processed = command;

		// Replace common placeholders
		processed = processed.replace("@p", player.getName());
		processed = processed.replace("@s", player.getName());
		processed = processed.replace("@r", player.getName());
		processed = processed.replace("@a", player.getName());

		// Replace custom placeholders
		if (playerPlaceholder != null && !playerPlaceholder.isEmpty()) {
			processed = processed.replace(playerPlaceholder, player.getName());
		}

		// Replace coordinate placeholders
		processed = processed.replace("~", String.valueOf((int) player.posX));
		processed = processed.replace("~y", String.valueOf((int) player.posY));
		processed = processed.replace("~z", String.valueOf((int) player.posZ));

		return processed;
	}
}
