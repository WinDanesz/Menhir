package com.windanesz.menhir.network;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * <b>[Client -> Server]</b> This packet is used to select the active ability index for the player.
 */
public class PacketSelectActiveAbility implements IMessageHandler<PacketSelectActiveAbility.Message, IMessage> {

	@Override
	public IMessage onMessage(Message message, MessageContext ctx) {
		if (ctx.side.isServer()) {
			final EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(() -> {
				IBirthsignData data = BirthsignDataProvider.get(player);
				if (data != null) {
					data.setInt("selected_ability_index", message.abilityIndex);
					
					// Sync back to client so they know it's selected (though client likely knows already)
					// It's good practice to ensure server state is authoritative and synced
					// But for this simple int, maybe not strictly necessary to spam packets back immediately
					// unless we want to confirm validation.
					// For now, assume client prediction is fine, or sync on next update.
				}
			});
		}
		return null;
	}

	public static class Message implements IMessage {
		private int abilityIndex;

		public Message() {}

		public Message(int abilityIndex) {
			this.abilityIndex = abilityIndex;
		}

		@Override
		public void fromBytes(ByteBuf buf) {
			this.abilityIndex = buf.readInt();
		}

		@Override
		public void toBytes(ByteBuf buf) {
			buf.writeInt(abilityIndex);
		}
	}
}
