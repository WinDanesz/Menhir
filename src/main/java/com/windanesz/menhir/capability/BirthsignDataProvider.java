package com.windanesz.menhir.capability;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.core.BirthsignData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class BirthsignDataProvider {

	public static final ResourceLocation KEY = new ResourceLocation(Menhir.MODID + ":" + "birthsign_data");

	@CapabilityInject(IBirthsignData.class)
	public static final Capability<IBirthsignData> BIRTHSIGN_DATA_CAP = null;

	/**
	 * Called from preInit in the main mod class to register the BirthsignData capability.
	 */
	public static void register() {
		CapabilityManager.INSTANCE.register(IBirthsignData.class, new Storage(), BirthsignData::new);
	}

	/**
	 * Returns the BirthsignData instance for the specified player.
	 */
	@Nullable
	public static IBirthsignData get(EntityPlayer player) {
		return player.getCapability(BIRTHSIGN_DATA_CAP, null);
	}

	@SubscribeEvent
	public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayer) {
			event.addCapability(KEY, new Provider((EntityPlayer) event.getObject()));
		}
	}

	// ========================================== Capability Boilerplate ==========================================

	public static class Provider implements ICapabilitySerializable<NBTTagCompound> {

		private final IBirthsignData data;

		public Provider(EntityPlayer player) {
			data = new BirthsignData();
		}

		@Override
		public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
			return capability == BIRTHSIGN_DATA_CAP;
		}

		@Override
		@Nullable
		public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
			if (capability == BIRTHSIGN_DATA_CAP) {
				return BIRTHSIGN_DATA_CAP.cast(data);
			}
			return null;
		}

		@Override
		public NBTTagCompound serializeNBT() {
			NBTTagCompound tag = new NBTTagCompound();
			data.writeToNBT(tag);
			return tag;
		}

		@Override
		public void deserializeNBT(NBTTagCompound nbt) {
			data.readFromNBT(nbt);
		}
	}

	public static class Storage implements IStorage<IBirthsignData> {
		@Override
		public NBTBase writeNBT(Capability<IBirthsignData> capability, IBirthsignData instance, EnumFacing side) {
			NBTTagCompound tag = new NBTTagCompound();
			instance.writeToNBT(tag);
			return tag;
		}

		@Override
		public void readNBT(Capability<IBirthsignData> capability, IBirthsignData instance, EnumFacing side, NBTBase nbt) {
			if (nbt instanceof NBTTagCompound) {
				instance.readFromNBT((NBTTagCompound) nbt);
			}
		}
	}
} 