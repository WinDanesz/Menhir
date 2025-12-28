package com.windanesz.menhir.command;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.eventhandler.BirthsignEffectManager;
import com.windanesz.menhir.worldgen.WorldGenMenhirStone;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Collections;
import java.util.List;

public class CommandGetBirthsign extends CommandBase {
	@Override
	public String getName() {
		return "getbirthsign";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/getbirthsign <player> [birthsign] OR /getbirthsign positions";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0) {
			sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: " + getUsage(sender)));
			return;
		}

		if ("positions".equals(args[0])) {
			// Show all stored birthsign stone positions
			World world = sender.getEntityWorld();
			if (world.provider.getDimension() != 0) {
				sender.sendMessage(new TextComponentString(TextFormatting.RED + "This command only works in the overworld."));
				return;
			}

			java.util.List<WorldGenMenhirStone.MenhirSpawnLocation> locations =
					WorldGenMenhirStone.getAllStoredSpawnLocations(world);

			if (locations.isEmpty()) {
				sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "No menhir spawn locations stored in this world."));
			} else {
				sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Stored menhir spawn locations (" + locations.size() + "):"));
				for (WorldGenMenhirStone.MenhirSpawnLocation location : locations) {
					String status = WorldGenMenhirStone.getAllPlacedMenhirs(world).contains(location.birthsignName) ?
							TextFormatting.GREEN + " [PLACED]" : TextFormatting.YELLOW + " [PENDING]";

					// Get the localized birthsign name for display
					String birthsignNameForTranslation = location.birthsignName;
					if (location.birthsignName.contains(":")) {
						birthsignNameForTranslation = location.birthsignName.split(":")[1];
					}
					String localizedBirthsignName = Menhir.proxy.translate(Menhir.MODID + "." + birthsignNameForTranslation + ".name");

					sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "  " + location.position.getX() + ", " +
							location.position.getY() + ", " + location.position.getZ() + " - " + localizedBirthsignName + status));
				}
			}

			// Also show placed birthsigns count
			java.util.Set<String> placedMenhirs = WorldGenMenhirStone.getAllPlacedMenhirs(world);
			sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Placed: " + placedMenhirs.size() + "/" + locations.size()));

			return;
		}

		EntityPlayerMP player;
		if (args.length >= 1 && !"positions".equals(args[0])) {
			try {
				player = getPlayer(server, sender, args[0]);
			} catch (PlayerNotFoundException e) {
				// If argument is not a player, maybe they meant themselves if args.length == 0,
				// but here we are in the "args.length >= 1" block.
				// Actually, logic below handles args.length == 0 check at start.
				// If args[0] is not a player, throw exception.
				throw new CommandException("Player not found: " + args[0]);
			}
		} else if (sender instanceof EntityPlayerMP) {
			player = (EntityPlayerMP) sender;
		} else {
			throw new CommandException("You must specify a player from the console.");
		}

		IBirthsignData data = BirthsignDataProvider.get(player);
		java.util.Map<String, String> traits = data != null ? data.getAllTraits() : Collections.emptyMap();
		
		if (traits.isEmpty()) {
			sender.sendMessage(new TextComponentString(player.getName() + " has no traits assigned."));
		} else {
			sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Traits for " + player.getName() + ":"));
			
			for (java.util.Map.Entry<String, String> entry : traits.entrySet()) {
				String category = entry.getKey();
				String traitId = entry.getValue();
				
				if (traitId == null || traitId.isEmpty()) continue;
				
				Birthsign trait = Birthsign.registry.getValue(new net.minecraft.util.ResourceLocation(traitId));
				String display = traitId;
				
				if (trait != null) {
					String traitNameForTranslation = traitId;
					if (traitId.contains(":")) {
						traitNameForTranslation = traitId.split(":")[1];
					}
					// Attempt to translate, fallback to raw name if needed
					// Assuming generic key format: "birthsign.name.name" - might need "race.name.name" in future?
					// For now keeping consistent with existing lang keys which seem to be "birthsign.<id>.name"
					display = Menhir.proxy.translate("birthsign." + traitNameForTranslation + ".name");
				}
				
				// Capitalize category for display
				String categoryDisplay = category.substring(0, 1).toUpperCase() + category.substring(1);
				
				// Show charges for this specific trait
				String chargesInfo = "";
				if (trait != null) {
					chargesInfo = String.format(" (Active: %d, Passive: %d)", trait.active_daily_uses, trait.passive_daily_uses);
				}
				
				sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "  " + categoryDisplay + ": " + TextFormatting.WHITE + display + TextFormatting.GRAY + chargesInfo));
			}

			// Show charges status (global pool)
			String chargesStatus = BirthsignEffectManager.getBirthsignChargesStatus(player);
			sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Active Charges: " + TextFormatting.WHITE + chargesStatus));

			// Show passive charges status (global pool)
			String passiveChargesStatus = BirthsignEffectManager.getBirthsignPassiveChargesStatus(player);
			sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Passive Charges: " + TextFormatting.WHITE + passiveChargesStatus));
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, net.minecraft.util.math.BlockPos pos) {
		if (args.length == 1) {
			List<String> options = new java.util.ArrayList<>();
			options.add("positions");
			options.addAll(java.util.Arrays.asList(server.getOnlinePlayerNames()));
			return getListOfStringsMatchingLastWord(args, options);
		}
		return Collections.emptyList();
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0; // Anyone can use
	}
} 