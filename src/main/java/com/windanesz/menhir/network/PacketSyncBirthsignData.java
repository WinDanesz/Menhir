package com.windanesz.menhir.network;

import com.windanesz.menhir.Menhir;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncBirthsignData implements IMessage {
	public String birthsign;
	public NBTTagCompound additionalData;

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
			Menhir.proxy.handleSyncBirthsignData(message, ctx);
			return null;
		}
	}
}
