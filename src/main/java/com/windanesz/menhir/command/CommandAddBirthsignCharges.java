package com.windanesz.menhir.command;

import com.windanesz.menhir.eventhandler.BirthsignEffectManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.List;

public class CommandAddBirthsignCharges extends CommandBase {
	@Override
	public String getName() {
		return "addbirthsigncharges";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/addbirthsigncharges <player> [amount] [type]";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1) {
			throw new CommandException("Usage: /addbirthsigncharges <player> [amount] [type]");
		}
		EntityPlayerMP player = getPlayer(server, sender, args[0]);
		int amount = 1;
		String type = "active"; // Default to active charges

		if (args.length >= 2) {
			try {
				amount = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				throw new CommandException("Amount must be an integer.");
			}
		}

		if (args.length >= 3) {
			type = args[2].toLowerCase();
			if (!type.equals("active") && !type.equals("passive")) {
				throw new CommandException("Type must be 'active' or 'passive'.");
			}
		}

		if (type.equals("active")) {
			int current = BirthsignEffectManager.getBirthsignRemainingCharges(player);
			BirthsignEffectManager.setBirthsignRemainingCharges(player, current + amount);
			sender.sendMessage(new TextComponentString("Added " + amount + " active birthsign charge(s) to " + player.getName() + ". Now has " + (current + amount) + "."));
		} else {
			int current = BirthsignEffectManager.getBirthsignRemainingPassiveCharges(player);
			BirthsignEffectManager.setBirthsignRemainingPassiveCharges(player, current + amount);
			sender.sendMessage(new TextComponentString("Added " + amount + " passive birthsign charge(s) to " + player.getName() + ". Now has " + (current + amount) + "."));
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable net.minecraft.util.math.BlockPos targetPos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
		} else if (args.length == 2) {
			return getListOfStringsMatchingLastWord(args, new String[]{"1"});
		}
		return super.getTabCompletions(server, sender, args, targetPos);
	}
} 