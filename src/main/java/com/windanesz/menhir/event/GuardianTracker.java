package com.windanesz.menhir.event;

import com.windanesz.menhir.block.BlockAltar;
import com.windanesz.menhir.tileentity.TileEntityAltar;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GuardianTracker {

	@SubscribeEvent
	public void onEntityDeath(LivingDeathEvent event) {
		Entity entity = event.getEntity();
		NBTTagCompound entityData = entity.getEntityData();

		// Check if this entity is a guardian
		if (!entityData.hasKey("MenhirAltarX")) {
			return;
		}

		// Get altar position from entity NBT
		int altarX = entityData.getInteger("MenhirAltarX");
		int altarY = entityData.getInteger("MenhirAltarY");
		int altarZ = entityData.getInteger("MenhirAltarZ");
		int altarDim = entityData.getInteger("MenhirAltarDim");

		World world = entity.getEntityWorld();
		
		// Check if we're in the correct dimension
		if (world.provider.getDimension() != altarDim) {
			return;
		}

		BlockPos altarPos = new BlockPos(altarX, altarY, altarZ);
		
		// Get the tile entity
		TileEntity te = world.getTileEntity(altarPos);
		if (!(te instanceof TileEntityAltar)) {
			return;
		}

		TileEntityAltar altar = (TileEntityAltar) te;
		
		// Check if altar has pending reward
		if (!altar.hasPendingReward()) {
			return;
		}

		// Decrement guardian count
		boolean allDefeated = altar.decrementPendingGuardians();
		
		// If all guardians defeated, apply rewards
		if (allDefeated && world.getBlockState(altarPos).getBlock() instanceof BlockAltar) {
			BlockAltar altarBlock = (BlockAltar) world.getBlockState(altarPos).getBlock();
			altarBlock.applyPendingRewards(world, altarPos, altar);
		}
	}
}
