package com.windanesz.menhir.network;

import com.windanesz.menhir.api.Birthsign;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.eventhandler.BirthsignEffectManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Packet sent from client to server when a player selects their birthsign
 */
public class PacketSetBirthsign implements IMessage {
	
	private String birthsignName;
	
	public PacketSetBirthsign() {
		// Required default constructor
	}
	
	public PacketSetBirthsign(String birthsignName) {
		this.birthsignName = birthsignName;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		this.birthsignName = ByteBufUtils.readUTF8String(buf);
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, this.birthsignName);
	}
	
	public static class Handler implements IMessageHandler<PacketSetBirthsign, IMessage> {
		@Override
		public IMessage onMessage(PacketSetBirthsign message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			
			// Execute on the main server thread
			player.getServerWorld().addScheduledTask(() -> {
				// Validate the birthsign exists
				Birthsign birthsign = Birthsign.registry.getValue(new ResourceLocation(message.birthsignName));
				if (birthsign == null) {
					return; // Invalid birthsign
				}
				
				// Get the player's birthsign data
				IBirthsignData data = BirthsignDataProvider.get(player);
				if (data == null) {
					return;
				}
				
				// Get the old birthsign before changing it
				String oldBirthsign = data.getBirthsign();
				
				// Set the new birthsign
				data.setBirthsign(message.birthsignName);
				
				// Sync to client with full capability data
				net.minecraft.nbt.NBTTagCompound nbt = new net.minecraft.nbt.NBTTagCompound();
				data.writeToNBT(nbt);
				NetworkHandler.INSTANCE.sendTo(new PacketSyncBirthsignData(message.birthsignName, nbt), player);
				
				// Reapply birthsign effects (this will remove old effects and apply new ones)
				BirthsignEffectManager.reapplyBirthsignEffects(player, oldBirthsign, message.birthsignName);
			});
			
			return null; // No response packet
		}
	}
}
