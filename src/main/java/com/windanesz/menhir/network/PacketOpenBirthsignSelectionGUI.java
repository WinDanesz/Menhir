package com.windanesz.menhir.network;


import com.windanesz.menhir.Menhir;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

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
		public IMessage onMessage(PacketOpenBirthsignSelectionGUI message, MessageContext ctx) {
			Menhir.proxy.handleOpenBirthsignSelectionGUI(message, ctx);
			return null; // No response packet
		}
	}
}
