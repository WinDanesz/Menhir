package com.windanesz.menhir.network;

import com.windanesz.menhir.eventhandler.ChannelingManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketStopChanneling implements IMessage {
	public PacketStopChanneling() {
	}

	@Override
	public void fromBytes(ByteBuf buf) {
	}

	@Override
	public void toBytes(ByteBuf buf) {
	}

	public static class Handler implements IMessageHandler<PacketStopChanneling, IMessage> {
		@Override
		public IMessage onMessage(PacketStopChanneling message, MessageContext ctx) {
			MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
			EntityPlayerMP player = ctx.getServerHandler().player;
			server.addScheduledTask(() -> {
				ChannelingManager.stopChanneling(player);
			});
			return null;
		}
	}
}
