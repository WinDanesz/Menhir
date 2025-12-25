package com.windanesz.menhir.api.altar;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public abstract class AltarEffect {
	protected String id;
	protected List<String> requiredMods;
	protected boolean uniquePerWorld;
	protected boolean singleUse;
	protected boolean obfuscated;
	protected boolean oncePerPlayer;

	public enum EffectType {
		POTION("potion"),
		COMMAND("command"),
		TEMPORARY_ITEM("temporary_item"),
		TELEPORT_TWIN("teleport_twin"),
		TELEPORT_RECALL("teleport_recall"),
		PRAYER_DISPLAYED("prayer_displayed"),
		PRAYER_HIDDEN("prayer_hidden");

		private final String name;

		EffectType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public static EffectType fromString(String name) {
			for (EffectType type : values()) {
				if (type.name.equalsIgnoreCase(name)) {
					return type;
				}
			}
			return null;
		}
	}

	public AltarEffect(String id) {
		this.id = id;
	}

	public abstract EffectType getType();

	public abstract boolean apply(World world, BlockPos pos, EntityPlayer player, NBTTagCompound altarData);

	public String getId() {
		return id;
	}

	public List<String> getRequiredMods() {
		return requiredMods;
	}

	public void setRequiredMods(List<String> requiredMods) {
		this.requiredMods = requiredMods;
	}

	public boolean isUniquePerWorld() {
		return uniquePerWorld;
	}

	public void setUniquePerWorld(boolean uniquePerWorld) {
		this.uniquePerWorld = uniquePerWorld;
	}

	public boolean isSingleUse() {
		return singleUse;
	}

	public void setSingleUse(boolean singleUse) {
		this.singleUse = singleUse;
	}

	public boolean isObfuscated() {
		return obfuscated;
	}

	public void setObfuscated(boolean obfuscated) {
		this.obfuscated = obfuscated;
	}

	public boolean isOncePerPlayer() {
		return oncePerPlayer;
	}

	public void setOncePerPlayer(boolean oncePerPlayer) {
		this.oncePerPlayer = oncePerPlayer;
	}

	// Potion effect implementation
	public static class PotionEffect extends AltarEffect {
		private Potion potion;
		private int duration;
		private int amplifier;

		public PotionEffect(String id, Potion potion, int duration, int amplifier) {
			super(id);
			this.potion = potion;
			this.duration = duration;
			this.amplifier = amplifier;
		}

		@Override
		public EffectType getType() {
			return EffectType.POTION;
		}

		@Override
		public boolean apply(World world, BlockPos pos, EntityPlayer player, NBTTagCompound altarData) {
			player.addPotionEffect(new net.minecraft.potion.PotionEffect(potion, duration, amplifier));
			return true;
		}

		public Potion getPotion() {
			return potion;
		}

		public int getDuration() {
			return duration;
		}

		public int getAmplifier() {
			return amplifier;
		}
	}

	// Command effect implementation
	public static class CommandEffect extends AltarEffect {
		private String command;

		public CommandEffect(String id, String command) {
			super(id);
			this.command = command;
		}

		@Override
		public EffectType getType() {
			return EffectType.COMMAND;
		}

		@Override
		public boolean apply(World world, BlockPos pos, EntityPlayer player, NBTTagCompound altarData) {
			if (!world.isRemote) {
				// Replace placeholders
				String processedCommand = command
						.replace("{player}", player.getName())
						.replace("{x}", String.valueOf(pos.getX()))
						.replace("{y}", String.valueOf(pos.getY()))
						.replace("{z}", String.valueOf(pos.getZ()));
				
				world.getMinecraftServer().getCommandManager().executeCommand(
						world.getMinecraftServer(), processedCommand);
				return true;
			}
			return false;
		}

		public String getCommand() {
			return command;
		}
	}

	// Prayer effect implementation
	public static class PrayerEffect extends AltarEffect {
		private String prayerText;
		private boolean hidden;
		private List<AltarEffect> successEffects;

		public PrayerEffect(String id, String prayerText, boolean hidden, List<AltarEffect> successEffects) {
			super(id);
			this.prayerText = prayerText;
			this.hidden = hidden;
			this.successEffects = successEffects;
		}

		@Override
		public EffectType getType() {
			return hidden ? EffectType.PRAYER_HIDDEN : EffectType.PRAYER_DISPLAYED;
		}

		@Override
		public boolean apply(World world, BlockPos pos, EntityPlayer player, NBTTagCompound altarData) {
			// This is handled separately in the altar interaction logic
			return false;
		}

		public String getPrayerText() {
			return prayerText;
		}

		public boolean isHidden() {
			return hidden;
		}

		public List<AltarEffect> getSuccessEffects() {
			return successEffects;
		}
	}

	// Teleport recall effect
	public static class TeleportRecallEffect extends AltarEffect {
		private int maxUses;

		public TeleportRecallEffect(String id, int maxUses) {
			super(id);
			this.maxUses = maxUses;
		}

		@Override
		public EffectType getType() {
			return EffectType.TELEPORT_RECALL;
		}

		@Override
		public boolean apply(World world, BlockPos pos, EntityPlayer player, NBTTagCompound altarData) {
			// Store the altar position in player NBT for later recall
			NBTTagCompound playerData = player.getEntityData();
			if (!playerData.hasKey("MenhirRecallAltars")) {
				playerData.setTag("MenhirRecallAltars", new NBTTagCompound());
			}
			NBTTagCompound recallData = playerData.getCompoundTag("MenhirRecallAltars");
			
			String key = pos.getX() + "," + pos.getY() + "," + pos.getZ();
			NBTTagCompound altarRecall = new NBTTagCompound();
			altarRecall.setInteger("x", pos.getX());
			altarRecall.setInteger("y", pos.getY());
			altarRecall.setInteger("z", pos.getZ());
			altarRecall.setInteger("dimension", world.provider.getDimension());
			altarRecall.setInteger("uses", maxUses);
			
			recallData.setTag(key, altarRecall);
			return true;
		}

		public int getMaxUses() {
			return maxUses;
		}
	}

	// Teleport twin effect (passage between two altars)
	public static class TeleportTwinEffect extends AltarEffect {
		private String twinId;

		public TeleportTwinEffect(String id, String twinId) {
			super(id);
			this.twinId = twinId;
		}

		@Override
		public EffectType getType() {
			return EffectType.TELEPORT_TWIN;
		}

		@Override
		public boolean apply(World world, BlockPos pos, EntityPlayer player, NBTTagCompound altarData) {
			// Teleport logic handled in altar interaction
			return false;
		}

		public String getTwinId() {
			return twinId;
		}
	}

	// Temporary item effect - gives player an item that disappears after time/logout
	public static class TemporaryItemEffect extends AltarEffect {
		private net.minecraft.item.ItemStack itemStack;
		private int durationTicks;
		private boolean removeOnLogout;

		public TemporaryItemEffect(String id, net.minecraft.item.ItemStack itemStack, int durationTicks, boolean removeOnLogout) {
			super(id);
			this.itemStack = itemStack;
			this.durationTicks = durationTicks;
			this.removeOnLogout = removeOnLogout;
		}

		@Override
		public EffectType getType() {
			return EffectType.TEMPORARY_ITEM;
		}

		@Override
		public boolean apply(World world, BlockPos pos, EntityPlayer player, NBTTagCompound altarData) {
			// Logic handled by TemporaryItemHandler
			return false;
		}

		public net.minecraft.item.ItemStack getItemStack() {
			return itemStack;
		}

		public int getDurationTicks() {
			return durationTicks;
		}

		public boolean isRemoveOnLogout() {
			return removeOnLogout;
		}
	}
}
