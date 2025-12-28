package com.windanesz.menhir.network;

import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.eventhandler.BirthsignEffectManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketActivateBirthsignPower implements IMessage {
	// No payload needed
	public PacketActivateBirthsignPower() {
	}

	@Override
	public void fromBytes(ByteBuf buf) {
	}

	@Override
	public void toBytes(ByteBuf buf) {
	}

	public static class Handler implements IMessageHandler<PacketActivateBirthsignPower, IMessage> {
		@Override
		public IMessage onMessage(PacketActivateBirthsignPower message, MessageContext ctx) {
			MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
			EntityPlayerMP player = ctx.getServerHandler().player;
			server.addScheduledTask(() -> {
				IBirthsignData data = BirthsignDataProvider.get(player);
				if (data != null) {
					// We pass an empty string because the method ignores it anyway, 
					// and we don't want to restrict usage to only having a "birthsign" trait set.
					BirthsignEffectManager.applyBirthsignActiveEffects(player, "");
				}
			});
			return null;
		}
	}
} 