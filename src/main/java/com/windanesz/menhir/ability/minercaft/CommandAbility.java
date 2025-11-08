package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.ability.ChannelingAbility;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.Map;

public class CommandAbility extends ChannelingAbility {

	private final String command;
	private final String executeAs;
	private final String playerPlaceholder;

	public CommandAbility(int chargeup, String command, String executeAs, String playerPlaceholder) {
		super(chargeup);
		this.command = command;
		this.executeAs = executeAs;
		this.playerPlaceholder = playerPlaceholder;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, 0);
		String command = ParameterUtils.getStringParameter(params, "command", "");
		String executeAs = ParameterUtils.getStringParameter(params, "execute_as", "console");
		String playerPlaceholder = ParameterUtils.getStringParameter(params, "player_placeholder", "@p");

		return new CommandAbility(chargeup, command, executeAs, playerPlaceholder);
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		MinecraftServer server = player.world.getMinecraftServer();
		if (server == null) return false;

		try {
			// Replace player placeholders in the command
			String processedCommand = processCommandPlaceholders(command, player);

			// Store original command feedback setting
			boolean originalFeedback = player.world.getGameRules().getBoolean("sendCommandFeedback");
			
			// Disable command feedback temporarily
			player.world.getGameRules().setOrCreateGameRule("sendCommandFeedback", "false");

			// Execute the command based on the specified method
			boolean success = false;
			switch (executeAs.toLowerCase()) {
				case "console":
				case "server":
					// Execute as server console - convert relative coords to absolute
					processedCommand = convertRelativeToAbsoluteCoords(processedCommand, player);
					success = server.commandManager.executeCommand(server, processedCommand) > 0;
					break;
				case "player":
				case "op_player":
					// Execute as the player - this supports relative coordinates but requires permissions
					success = server.commandManager.executeCommand(player, processedCommand) > 0;
					break;
				default:
					// Default to console execution (no permission issues)
					processedCommand = convertRelativeToAbsoluteCoords(processedCommand, player);
					success = server.commandManager.executeCommand(server, processedCommand) > 0;
					break;
			}
			
			// Restore original command feedback setting
			player.world.getGameRules().setOrCreateGameRule("sendCommandFeedback", String.valueOf(originalFeedback));

			return success;
		} catch (Exception e) {
			// Silently fail - don't spam the player with error messages
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

		// Replace absolute coordinate placeholders
		processed = processed.replace("{x}", String.valueOf((int) player.posX));
		processed = processed.replace("{y}", String.valueOf((int) player.posY));
		processed = processed.replace("{z}", String.valueOf((int) player.posZ));

		return processed;
	}

	/**
	 * Converts relative coordinates (~) to absolute coordinates for console execution
	 */
	private String convertRelativeToAbsoluteCoords(String command, EntityPlayer player) {
		String result = command;
		
		// Simple pattern matching for "~ ~ ~" or "~ ~N ~" etc.
		// Split by spaces to find coordinate triplets
		String[] tokens = result.split(" ");
		StringBuilder rebuilt = new StringBuilder();
		
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			
			// Check if this looks like a coordinate (starts with ~ or is a number or ~number)
			if (token.startsWith("~")) {
				// Check if we're in a position where coordinates are expected (followed by two more potential coords)
				if (i + 2 < tokens.length && (tokens[i + 1].startsWith("~") || tokens[i + 1].matches("-?\\d+"))
						&& (tokens[i + 2].startsWith("~") || tokens[i + 2].matches("-?\\d+"))) {
					
					// This looks like a coordinate triplet, convert all three
					int x = (int) player.posX;
					int y = (int) player.posY;
					int z = (int) player.posZ;
					
					// Parse X
					if (token.length() > 1) {
						x += Integer.parseInt(token.substring(1));
					}
					rebuilt.append(x).append(" ");
					
					// Parse Y
					i++;
					token = tokens[i];
					if (token.startsWith("~")) {
						if (token.length() > 1) {
							y += Integer.parseInt(token.substring(1));
						}
					} else {
						y = Integer.parseInt(token);
					}
					rebuilt.append(y).append(" ");
					
					// Parse Z
					i++;
					token = tokens[i];
					if (token.startsWith("~")) {
						if (token.length() > 1) {
							z += Integer.parseInt(token.substring(1));
						}
					} else {
						z = Integer.parseInt(token);
					}
					rebuilt.append(z);
					
					// Add space if not last token
					if (i < tokens.length - 1) {
						rebuilt.append(" ");
					}
					continue;
				}
			}
			
			// Not a coordinate, just append as-is
			rebuilt.append(token);
			if (i < tokens.length - 1) {
				rebuilt.append(" ");
			}
		}
		
		return rebuilt.toString();
	}
}
