package com.windanesz.menhir.network;

import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncBirthsignData implements IMessage {
	private String birthsign;
	private NBTTagCompound additionalData;

	public PacketSyncBirthsignData() {
	}

	public PacketSyncBirthsignData(String birthsign) {
		this.birthsign = birthsign;
		this.additionalData = new NBTTagCompound();
	}
	
	public PacketSyncBirthsignData(String birthsign, NBTTagCompound data) {
		this.birthsign = birthsign;
		this.additionalData = data;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.birthsign = ByteBufUtils.readUTF8String(buf);
		this.additionalData = ByteBufUtils.readTag(buf);
		if (this.additionalData == null) {
			this.additionalData = new NBTTagCompound();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		ByteBufUtils.writeUTF8String(buf, this.birthsign != null ? this.birthsign : "");
		ByteBufUtils.writeTag(buf, this.additionalData != null ? this.additionalData : new NBTTagCompound());
	}

	public static class Handler implements IMessageHandler<PacketSyncBirthsignData, IMessage> {
		@Override
		public IMessage onMessage(PacketSyncBirthsignData message, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(() -> {
				EntityPlayer player = Minecraft.getMinecraft().player;
				if (player != null) {
					IBirthsignData data = BirthsignDataProvider.get(player);
					if (data != null) {
						// Read all data (including birthsign name) from NBT
						if (message.additionalData != null && !message.additionalData.isEmpty()) {
							data.readFromNBT(message.additionalData);
						} else {
							// Fallback if NBT is empty - just set the birthsign name
							data.setBirthsign(message.birthsign);
						}
					}
				}
			});
			return null;
		}
	}
}
