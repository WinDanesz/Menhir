package com.windanesz.menhir.command;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.eventhandler.BirthsignEffectManager;
import com.windanesz.menhir.network.NetworkHandler;
import com.windanesz.menhir.network.PacketSyncBirthsignData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandSetBirthsign extends CommandBase {
	@Override
	public String getName() {
		return "setbirthsign";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/setbirthsign <player> <modid:birthsign_name>";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length != 2) {
			throw new CommandException(getUsage(sender));
		}
		EntityPlayerMP player;
		try {
			player = getPlayer(server, sender, args[0]);
		} catch (PlayerNotFoundException e) {
			throw new CommandException("Player not found: " + args[0]);
		}
		String birthsignName = args[1];
		Birthsign birthsign = Birthsign.registry.getValue(new net.minecraft.util.ResourceLocation(birthsignName));
		if (birthsign == null) {
			sender.sendMessage(new TextComponentString("Unknown trait/birthsign: " + birthsignName));
			return;
		}

		// Get the category from the birthsign/trait object
		String category = birthsign.category;
		if (category == null || category.isEmpty()) {
			category = "birthsign";
		}

		// Get the player's current data
		IBirthsignData data = BirthsignDataProvider.get(player);
		if (data == null) {
			sender.sendMessage(new TextComponentString("Failed to get data for player: " + player.getName()));
			return;
		}

		// Get the old trait for this category before changing it
		String oldTrait = data.getTrait(category);

		// Set the new trait
		data.setTrait(category, birthsignName);

		// Sync to client with full capability data
		net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
		data.writeToNBT(nbt);
		// We send the primary birthsign as the key for compatibility, the map is in the NBT
		String primary = data.getBirthsign();
		NetworkHandler.INSTANCE.sendTo(new PacketSyncBirthsignData(primary, nbt), player);

		// Reapply effects (this will remove old effects and apply new ones for this category)
		BirthsignEffectManager.reapplyTraitEffects(player, oldTrait, birthsignName);

		// Get the localized birthsign name for display
		String birthsignNameForTranslation = birthsignName;
		if (birthsignName.contains(":")) {
			birthsignNameForTranslation = birthsignName.split(":")[1];
		}
		String localizedBirthsignName = Menhir.proxy.translate("birthsign." + birthsignNameForTranslation + ".name");

		sender.sendMessage(new TextComponentString("Set " + category + " for " + player.getName() + " to " + localizedBirthsignName));
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, net.minecraft.util.math.BlockPos pos) {
		if (Birthsign.registry == null) {
			Menhir.logger.warn("BirthsignRegistry.registry is null during tab completion");
			return Collections.emptyList();
		}

		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
		} else if (args.length == 2) {
			List<String> birthsigns = new ArrayList<>();
			for (Birthsign birthsign : Birthsign.registry) {
				birthsigns.add(birthsign.getRegistryName().toString());
			}
			return getListOfStringsMatchingLastWord(args, birthsigns);
		}
		return Collections.emptyList();
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2; // OP only
	}
} 