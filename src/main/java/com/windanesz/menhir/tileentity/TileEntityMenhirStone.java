package com.windanesz.menhir.tileentity;

import com.windanesz.menhir.api.Birthsign;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

public class TileEntityMenhirStone extends TileEntity {
	private String birthsign = "";

	public String getBirthsign() {
		return birthsign;
	}

	public void setBirthsign(Birthsign birthsign) {
		this.birthsign = birthsign != null ? birthsign.name : "";
		markDirty();
	}

	/**
	 * Sets the birthsign by string name (e.g., "the_warrior")
	 */
	public void setBirthsign(String birthsign) {
		this.birthsign = birthsign != null ? birthsign : "";
		markDirty();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setString("Birthsign", birthsign);
		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		this.birthsign = compound.getString("Birthsign");
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 0, this.getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}
}