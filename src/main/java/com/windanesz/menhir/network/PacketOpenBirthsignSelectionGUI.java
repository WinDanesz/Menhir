package com.windanesz.menhir.network;

import com.windanesz.menhir.Menhir;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Packet sent from server to client to open the birthsign selection GUI
 */
public class PacketOpenBirthsignSelectionGUI implements IMessage {
	
	public PacketOpenBirthsignSelectionGUI() {
		// Required default constructor
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		// No data to read
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		// No data to write
	}
	
	public static class Handler implements IMessageHandler<PacketOpenBirthsignSelectionGUI, IMessage> {
		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(PacketOpenBirthsignSelectionGUI message, MessageContext ctx) {
			// Execute on the main client thread
			Minecraft.getMinecraft().addScheduledTask(() -> {
				EntityPlayer player = Minecraft.getMinecraft().player;
				if (player != null) {
					Menhir.proxy.openBirthsignSelectionGUI(player);
				}
			});
			
			return null; // No response packet
		}
	}
}
