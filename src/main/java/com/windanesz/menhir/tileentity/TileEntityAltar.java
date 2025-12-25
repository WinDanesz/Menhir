package com.windanesz.menhir.tileentity;

import com.windanesz.menhir.api.altar.AltarDefinition;
import com.windanesz.menhir.block.BlockAltar;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TileEntityAltar extends TileEntity implements ITickable {
	private String altarId = "";
	private boolean guardiansSpawned = false;
	private int totalUses = 0;
	private Map<UUID, Integer> playerUsages = new HashMap<>();
	private Set<UUID> playersIdentified = new HashSet<>(); // For obfuscated altars
	private boolean firstInteraction = true;
	private long lastChaosRebind = -1; // For chaotic altars

	// For channeling
	private UUID channelingPlayer = null;
	private int channelProgress = 0;

	// For pending guardian rewards
	private UUID pendingRewardPlayer = null;
	private int pendingGuardianCount = 0;
	private long pendingRewardStartTime = 0;
	private String pendingAltarDefinitionId = null;
	private static final long GUARDIAN_TIMEOUT_TICKS = 12000; // 10 minutes

	// Generic data storage for altar-specific data (e.g., teleport twins, prayers, etc.)
	private NBTTagCompound altarData = new NBTTagCompound();

	@Override
	public void update() {
		if (world == null || world.isRemote) {
			return;
		}

		// Check for guardian reward timeout
		if (hasPendingReward() && isRewardTimedOut()) {
			// Time out expired - apply rewards anyway
			if (world.getBlockState(pos).getBlock() instanceof BlockAltar) {
				BlockAltar altarBlock = (BlockAltar) world.getBlockState(pos).getBlock();
				altarBlock.applyPendingRewards(world, pos, this);
			}
		}
	}

	public String getAltarId() {
		return altarId;
	}

	public void setAltarId(String altarId) {
		this.altarId = altarId != null ? altarId : "";
		markDirty();
	}

	public boolean areGuardiansSpawned() {
		return guardiansSpawned;
	}

	public void setGuardiansSpawned(boolean guardiansSpawned) {
		this.guardiansSpawned = guardiansSpawned;
		markDirty();
	}

	public int getTotalUses() {
		return totalUses;
	}

	public void incrementTotalUses() {
		this.totalUses++;
		markDirty();
		syncToClient();
	}

	public int getPlayerUses(UUID playerId) {
		return playerUsages.getOrDefault(playerId, 0);
	}

	public void incrementPlayerUses(UUID playerId) {
		playerUsages.put(playerId, playerUsages.getOrDefault(playerId, 0) + 1);
		markDirty();
		syncToClient();
	}

	public boolean hasPlayerIdentified(UUID playerId) {
		return playersIdentified.contains(playerId);
	}

	public void addIdentifiedPlayer(UUID playerId) {
		playersIdentified.add(playerId);
		markDirty();
		syncToClient();
	}

	private void syncToClient() {
		if (world != null && !world.isRemote) {
			IBlockState state = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, state, state, 3);
		}
	}

	public boolean isFirstInteraction() {
		return firstInteraction;
	}

	public void setFirstInteraction(boolean firstInteraction) {
		this.firstInteraction = firstInteraction;
		markDirty();
	}

	// Generic data access for altar-specific data
	public NBTTagCompound getAltarData() {
		return altarData;
	}

	public void setAltarData(NBTTagCompound data) {
		this.altarData = data != null ? data : new NBTTagCompound();
		markDirty();
		syncToClient();
	}

	// Convenience methods for common data operations
	public void setDataValue(String key, NBTTagCompound value) {
		altarData.setTag(key, value);
		markDirty();
		syncToClient();
	}

	public NBTTagCompound getDataValue(String key) {
		return altarData.getCompoundTag(key);
	}

	public boolean hasDataValue(String key) {
		return altarData.hasKey(key);
	}

	public long getLastChaosRebind() {
		return lastChaosRebind;
	}

	public void setLastChaosRebind(long lastChaosRebind) {
		this.lastChaosRebind = lastChaosRebind;
		markDirty();
	}

	// Pending reward methods
	public void setPendingReward(UUID player, int guardianCount, String altarDefId) {
		this.pendingRewardPlayer = player;
		this.pendingGuardianCount = guardianCount;
		this.pendingAltarDefinitionId = altarDefId;
		this.pendingRewardStartTime = world != null ? world.getTotalWorldTime() : 0;
		markDirty();
		syncToClient();
	}

	public boolean decrementPendingGuardians() {
		if (pendingGuardianCount > 0) {
			pendingGuardianCount--;
			markDirty();
			syncToClient();
			return pendingGuardianCount == 0; // Return true when all guardians are dead
		}
		return false;
	}

	public boolean hasPendingReward() {
		return pendingRewardPlayer != null && pendingGuardianCount > 0;
	}

	public boolean isRewardTimedOut() {
		if (world == null || !hasPendingReward()) {
			return false;
		}
		long currentTime = world.getTotalWorldTime();
		return (currentTime - pendingRewardStartTime) >= GUARDIAN_TIMEOUT_TICKS;
	}

	public UUID getPendingRewardPlayer() {
		return pendingRewardPlayer;
	}

	public String getPendingAltarDefinitionId() {
		return pendingAltarDefinitionId;
	}

	public void clearPendingReward() {
		this.pendingRewardPlayer = null;
		this.pendingGuardianCount = 0;
		this.pendingRewardStartTime = 0;
		this.pendingAltarDefinitionId = null;
		markDirty();
		syncToClient();
	}

	public UUID getChannelingPlayer() {
		return channelingPlayer;
	}

	public void setChannelingPlayer(UUID channelingPlayer) {
		this.channelingPlayer = channelingPlayer;
		markDirty();
	}

	public int getChannelProgress() {
		return channelProgress;
	}

	public void setChannelProgress(int channelProgress) {
		this.channelProgress = channelProgress;
		markDirty();
	}

	public void incrementChannelProgress() {
		incrementChannelProgress(1);
	}

	public void incrementChannelProgress(int amount) {
		this.channelProgress += amount;
		markDirty();
	}

	public void resetChanneling() {
		this.channelingPlayer = null;
		this.channelProgress = 0;
		markDirty();
	}

	/**
	 * Check if the player can use this altar based on usage restrictions
	 */
	public boolean canPlayerUse(UUID playerId, AltarDefinition definition) {
		if (definition == null) {
			return false;
		}

		switch (definition.getUsageType()) {
			case UNLIMITED:
				return true;
			case TIMES_PER_PLAYER:
				return getPlayerUses(playerId) < definition.getUsageLimit();
			case TIMES_BY_ANYONE:
				return totalUses < definition.getUsageLimit();
			default:
				return false;
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		
		compound.setString("AltarId", altarId);
		compound.setBoolean("GuardiansSpawned", guardiansSpawned);
		compound.setInteger("TotalUses", totalUses);
		compound.setBoolean("FirstInteraction", firstInteraction);
		compound.setLong("LastChaosRebind", lastChaosRebind);

		// Save player usages
		NBTTagList usagesList = new NBTTagList();
		for (Map.Entry<UUID, Integer> entry : playerUsages.entrySet()) {
			NBTTagCompound usageTag = new NBTTagCompound();
			usageTag.setUniqueId("Player", entry.getKey());
			usageTag.setInteger("Uses", entry.getValue());
			usagesList.appendTag(usageTag);
		}
		compound.setTag("PlayerUsages", usagesList);

		// Save identified players
		NBTTagList identifiedList = new NBTTagList();
		for (UUID uuid : playersIdentified) {
			NBTTagCompound identTag = new NBTTagCompound();
			identTag.setUniqueId("Player", uuid);
			identifiedList.appendTag(identTag);
		}
		compound.setTag("PlayersIdentified", identifiedList);

		// Save channeling state
		if (channelingPlayer != null) {
			compound.setUniqueId("ChannelingPlayer", channelingPlayer);
			compound.setInteger("ChannelProgress", channelProgress);
		}

		// Save pending reward state
		if (pendingRewardPlayer != null) {
			compound.setUniqueId("PendingRewardPlayer", pendingRewardPlayer);
			compound.setInteger("PendingGuardianCount", pendingGuardianCount);
			compound.setLong("PendingRewardStartTime", pendingRewardStartTime);
			if (pendingAltarDefinitionId != null) {
				compound.setString("PendingAltarDefinitionId", pendingAltarDefinitionId);
			}
		}

		// Save generic altar data
		if (altarData != null && !altarData.isEmpty()) {
			compound.setTag("AltarData", altarData);
		}

		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		
		this.altarId = compound.getString("AltarId");
		this.guardiansSpawned = compound.getBoolean("GuardiansSpawned");
		this.totalUses = compound.getInteger("TotalUses");
		this.firstInteraction = compound.getBoolean("FirstInteraction");
		this.lastChaosRebind = compound.getLong("LastChaosRebind");

		// Load player usages
		playerUsages.clear();
		NBTTagList usagesList = compound.getTagList("PlayerUsages", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < usagesList.tagCount(); i++) {
			NBTTagCompound usageTag = usagesList.getCompoundTagAt(i);
			UUID playerId = usageTag.getUniqueId("Player");
			int uses = usageTag.getInteger("Uses");
			playerUsages.put(playerId, uses);
		}

		// Load identified players
		playersIdentified.clear();
		NBTTagList identifiedList = compound.getTagList("PlayersIdentified", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < identifiedList.tagCount(); i++) {
			NBTTagCompound identTag = identifiedList.getCompoundTagAt(i);
			UUID playerId = identTag.getUniqueId("Player");
			playersIdentified.add(playerId);
		}

		// Load channeling state
		if (compound.hasUniqueId("ChannelingPlayer")) {
			this.channelingPlayer = compound.getUniqueId("ChannelingPlayer");
			this.channelProgress = compound.getInteger("ChannelProgress");
		} else {
			this.channelingPlayer = null;
			this.channelProgress = 0;
		}

		// Load pending reward state
		if (compound.hasUniqueId("PendingRewardPlayer")) {
			this.pendingRewardPlayer = compound.getUniqueId("PendingRewardPlayer");
			this.pendingGuardianCount = compound.getInteger("PendingGuardianCount");
			this.pendingRewardStartTime = compound.getLong("PendingRewardStartTime");
			this.pendingAltarDefinitionId = compound.getString("PendingAltarDefinitionId");
		} else {
			this.pendingRewardPlayer = null;
			this.pendingGuardianCount = 0;
			this.pendingRewardStartTime = 0;
			this.pendingAltarDefinitionId = null;
		}

		// Load generic altar data
		if (compound.hasKey("AltarData")) {
			this.altarData = compound.getCompoundTag("AltarData");
		} else {
			this.altarData = new NBTTagCompound();
		}
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
