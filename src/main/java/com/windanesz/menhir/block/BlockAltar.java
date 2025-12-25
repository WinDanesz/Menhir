package com.windanesz.menhir.block;

import com.windanesz.menhir.Menhir;
import com.windanesz.menhir.api.IBirthsignData;
import com.windanesz.menhir.api.altar.AltarDefinition;
import com.windanesz.menhir.api.altar.AltarEffect;
import com.windanesz.menhir.api.altar.AltarRarity;
import com.windanesz.menhir.api.altar.AltarRequirements;
import com.windanesz.menhir.api.altar.GuardianSpawn;
import com.windanesz.menhir.capability.BirthsignDataProvider;
import com.windanesz.menhir.core.AltarEffectHandlerRegistry;
import com.windanesz.menhir.core.AltarRegistry;
import com.windanesz.menhir.core.BirthsignRegistrationHandler;
import com.windanesz.menhir.tileentity.TileEntityAltar;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class BlockAltar extends Block {

	public static final PropertyDirection FACING = BlockHorizontal.FACING;
	public static final PropertyEnum<BlockPosition> POSITION = PropertyEnum.create("position", BlockPosition.class);

	public BlockAltar() {
		super(Material.ROCK);
		this.setDefaultState(this.blockState.getBaseState()
				.withProperty(FACING, EnumFacing.NORTH)
				.withProperty(POSITION, BlockPosition.BOTTOM));
		this.setBlockUnbreakable();
		this.setResistance(6000000.0F);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING, POSITION);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		EnumFacing facing = EnumFacing.byHorizontalIndex(meta & 3);
		BlockPosition position = BlockPosition.values()[(meta >> 2) & 3];
		return this.getDefaultState().withProperty(FACING, facing).withProperty(POSITION, position);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		int facing = state.getValue(FACING).getHorizontalIndex();
		int position = state.getValue(POSITION).ordinal();
		return facing | (position << 2);
	}

	public enum BlockPosition implements net.minecraft.util.IStringSerializable {
		BOTTOM("bottom"),
		MIDDLE("middle"),
		TOP("top");

		private final String name;

		BlockPosition(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return state.getValue(POSITION) == BlockPosition.BOTTOM;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return state.getValue(POSITION) == BlockPosition.BOTTOM ? new TileEntityAltar() : null;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		BlockPosition position = state.getValue(POSITION);
		switch (position) {
			case BOTTOM:
				return new AxisAlignedBB(0.0625, 0.0, 0.1875, 0.9375, 1.0, 0.8125);
			case MIDDLE:
				return new AxisAlignedBB(0.1875, 0.0, 0.3125, 0.8125, 1.0, 0.6875);
			case TOP:
				return new AxisAlignedBB(0.1875, 0.0, 0.3125, 0.8125, 1.0, 0.6875);
			default:
				return FULL_BLOCK_AABB;
		}
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		EnumFacing facing = placer.getHorizontalFacing().getOpposite();
		placeMultiblock(worldIn, pos, facing);
	}

	private void placeMultiblock(World world, BlockPos pos, EnumFacing facing) {
		world.setBlockState(pos, this.getDefaultState().withProperty(FACING, facing).withProperty(POSITION, BlockPosition.BOTTOM), 2);
		world.setBlockState(pos.up(), this.getDefaultState().withProperty(FACING, facing).withProperty(POSITION, BlockPosition.MIDDLE), 2);
		world.setBlockState(pos.up(2), this.getDefaultState().withProperty(FACING, facing).withProperty(POSITION, BlockPosition.TOP), 2);
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		BlockPosition position = state.getValue(POSITION);
		BlockPos basePos = getBasePos(pos, position);
		
		// Remove all three blocks
		for (int i = 0; i < 3; i++) {
			BlockPos checkPos = basePos.up(i);
			if (worldIn.getBlockState(checkPos).getBlock() == this) {
				worldIn.setBlockToAir(checkPos);
			}
		}
		
		super.breakBlock(worldIn, pos, state);
	}

	private BlockPos getBasePos(BlockPos pos, BlockPosition position) {
		switch (position) {
			case BOTTOM:
				return pos;
			case MIDDLE:
				return pos.down();
			case TOP:
				return pos.down(2);
			default:
				return pos;
		}
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, 
	                                 EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (worldIn.isRemote) {
			return true;
		}

		// Get the base position to access the tile entity
		BlockPosition position = state.getValue(POSITION);
		BlockPos basePos = getBasePos(pos, position);
		
		TileEntityAltar altar = getTileEntity(worldIn, basePos);
		if (altar == null) {
			return false;
		}

		String altarId = altar.getAltarId();
		if (altarId == null || altarId.isEmpty()) {
			playerIn.sendMessage(new TextComponentString(TextFormatting.RED + "This altar has no definition."));
			return true;
		}

		AltarDefinition definition = AltarRegistry.getAltarDefinition(altarId);
		if (definition == null) {
			playerIn.sendMessage(new TextComponentString(TextFormatting.RED + "Unknown altar type: " + altarId));
			return true;
		}

		// Check if altar is obfuscated and player hasn't identified it yet
		boolean isObfuscated = definition.isObfuscated() && !altar.hasPlayerIdentified(playerIn.getUniqueID());

		// Display altar name
		String displayName = isObfuscated ? generateObfuscatedName(altarId) : definition.getName();
		TextFormatting nameColor = definition.getRarity().getColor();

		// Check usage limits
		if (!altar.canPlayerUse(playerIn.getUniqueID(), definition)) {
			playerIn.sendMessage(new TextComponentString(TextFormatting.RED + "This altar has no more power for you."));
			return true;
		}

		// Check time of day restrictions
		if (!definition.isValidTimeOfDay(worldIn.getWorldTime())) {
			playerIn.sendMessage(new TextComponentString(TextFormatting.RED + "This altar is not active at this time."));
			return true;
		}

		// Check birthsign requirement
		if (definition.getRequiredBirthsign() != null && !definition.getRequiredBirthsign().isEmpty()) {
			IBirthsignData birthsignData = BirthsignDataProvider.get(playerIn);
			if (birthsignData == null || !definition.getRequiredBirthsign().equals(birthsignData.getBirthsign())) {
				playerIn.sendMessage(new TextComponentString(TextFormatting.RED + "This altar requires a specific birthsign."));
				return true;
			}
		}

		// Check requirements
		if (!checkRequirements(playerIn, definition.getRequirements())) {
			playerIn.sendMessage(new TextComponentString(TextFormatting.RED + "You do not meet the requirements."));
			return true;
		}

		// Handle channeling
		if (definition.getChannelTime() > 0) {
			return handleChanneling(worldIn, pos, altar, definition, playerIn);
		}

		// Instant activation
		return activateAltar(worldIn, pos, altar, definition, playerIn, isObfuscated);
	}

	private boolean handleChanneling(World world, BlockPos pos, TileEntityAltar altar, 
	                                   AltarDefinition definition, EntityPlayer player) {
		UUID playerId = player.getUniqueID();

		if (altar.getChannelingPlayer() == null) {
			// Start channeling
			altar.setChannelingPlayer(playerId);
			altar.setChannelProgress(0);
			player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Channeling altar power..."));
			return true;
		} else if (altar.getChannelingPlayer().equals(playerId)) {
			// Continue channeling (10x faster in creative mode)
			int increment = player.isCreative() ? 10 : 1;
			altar.incrementChannelProgress(increment);
			
			int progress = altar.getChannelProgress();
			int required = definition.getChannelTime();
			
			if (progress >= required) {
				// Channeling complete
				boolean isObfuscated = definition.isObfuscated() && !altar.hasPlayerIdentified(playerId);
				boolean success = activateAltar(world, pos, altar, definition, player, isObfuscated);
				altar.resetChanneling();
				return success;
			} else {
				// Show progress
				int percentage = (progress * 100) / required;
				player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Channeling: " + percentage + "%"));
				return true;
			}
		} else {
			// Different player interrupted
			player.sendMessage(new TextComponentString(TextFormatting.RED + "Someone else is channeling this altar."));
			return true;
		}
	}

	private boolean activateAltar(World world, BlockPos pos, TileEntityAltar altar, 
	                               AltarDefinition definition, EntityPlayer player, boolean wasObfuscated) {
		UUID playerId = player.getUniqueID();

		// Spawn guardians if needed
		List<Entity> spawnedGuardians = new ArrayList<>();
		if (altar.isFirstInteraction() || shouldSpawnGuardians(altar, definition)) {
			spawnedGuardians = spawnGuardians(world, pos, definition, altar);
			altar.setFirstInteraction(false);
		}

		// If guardians were spawned, set up pending reward instead of applying effects immediately
		if (!spawnedGuardians.isEmpty()) {
			altar.setPendingReward(player.getUniqueID(), spawnedGuardians.size(), definition.getId());
			
			// Send challenge message
			String challengeMessage = definition.getGuardianChallengeMessage();
			if (challengeMessage != null && !challengeMessage.isEmpty()) {
				player.sendMessage(new TextComponentString(challengeMessage));
			}
			
			// Play sound effect
			world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
			
			// Mark as identified if first use
			if (wasObfuscated) {
				altar.addIdentifiedPlayer(playerId);
				player.sendMessage(new TextComponentString(TextFormatting.GREEN + "You have identified: " + 
						definition.getRarity().getColor() + definition.getName()));
			}
			
			return true; // Don't apply effects yet
		}

		// No guardians - proceed with normal activation
		
		// Consume requirements
		consumeRequirements(player, definition.getRequirements());

		// Try to find and use a custom effect handler
		boolean handlerProcessed = AltarEffectHandlerRegistry.processInteraction(world, pos, altar, definition, player);
		
		if (handlerProcessed) {
			// Handler fully processed the interaction
			return true;
		}

		// Apply standard effects
		NBTTagCompound altarData = new NBTTagCompound();
		altar.writeToNBT(altarData);

		for (AltarEffect effect : definition.getEffects()) {
			if (effect.isOncePerPlayer() && altar.getPlayerUses(playerId) > 0) {
				continue; // Skip if already used by this player
			}
			effect.apply(world, pos, player, altarData);
		}
		
		// Call post-processing hook
		AltarEffectHandlerRegistry.postProcessInteraction(world, pos, altar, definition, player);

		// Update usage counts
		altar.incrementPlayerUses(playerId);
		altar.incrementTotalUses();

		// Mark as identified if it was obfuscated
		if (wasObfuscated) {
			altar.addIdentifiedPlayer(playerId);
			player.sendMessage(new TextComponentString(TextFormatting.GREEN + "You have identified: " + 
					definition.getRarity().getColor() + definition.getName()));
		} else {
			player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Altar activated!"));
		}

		// Play sound
		world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 
				SoundCategory.BLOCKS, 1.0F, 1.0F);

		return true;
	}

	private boolean shouldSpawnGuardians(TileEntityAltar altar, AltarDefinition definition) {
		for (GuardianSpawn guardian : definition.getGuardians()) {
			if (guardian.getSpawnType() == GuardianSpawn.SpawnType.EVERY_INTERACTION) {
				return true;
			}
			if (guardian.getSpawnType() == GuardianSpawn.SpawnType.FIRST_INTERACTION && !altar.areGuardiansSpawned()) {
				return true;
			}
		}
		return false;
	}

	private List<Entity> spawnGuardians(World world, BlockPos pos, AltarDefinition definition, TileEntityAltar altar) {
		List<Entity> spawnedEntities = new ArrayList<>();
		Random random = world.rand;
		double globalChance = definition.getGlobalGuardianChance();
		
		// Use global chance if specified, otherwise use individual spawn chances
		if (globalChance > 0 && random.nextDouble() > globalChance) {
			return spawnedEntities; // Global roll failed
		}

		for (GuardianSpawn guardian : definition.getGuardians()) {
			// Check spawn type
			if (guardian.getSpawnType() == GuardianSpawn.SpawnType.FIRST_INTERACTION && altar.areGuardiansSpawned()) {
				continue;
			}

			// Check individual spawn chance
			if (random.nextDouble() > guardian.getSpawnChance()) {
				continue;
			}

			// Determine spawn count
			int count = guardian.getMinCount() + 
					random.nextInt(guardian.getMaxCount() - guardian.getMinCount() + 1);

			// Spawn entities
			for (int i = 0; i < count; i++) {
				Entity entity = EntityList.createEntityByIDFromName(guardian.getEntityId(), world);
				if (entity instanceof EntityLiving) {
					// Position around the altar
					double angle = (2 * Math.PI * i) / count;
					double distance = 3.0 + random.nextDouble() * 2.0;
					double x = pos.getX() + 0.5 + Math.cos(angle) * distance;
					double z = pos.getZ() + 0.5 + Math.sin(angle) * distance;
					double y = pos.getY();

					entity.setPosition(x, y, z);

					// Apply NBT data if present
					if (guardian.getEntityNBT() != null) {
						entity.readFromNBT(guardian.getEntityNBT());
					}

					// Tag entity with altar position and dimension for tracking
					NBTTagCompound entityData = entity.getEntityData();
					entityData.setInteger("MenhirAltarX", pos.getX());
					entityData.setInteger("MenhirAltarY", pos.getY());
					entityData.setInteger("MenhirAltarZ", pos.getZ());
					entityData.setInteger("MenhirAltarDim", world.provider.getDimension());

					world.spawnEntity(entity);
					spawnedEntities.add(entity);
				}
			}
		}

		altar.setGuardiansSpawned(true);
		return spawnedEntities;
	}

	public void applyPendingRewards(World world, BlockPos pos, TileEntityAltar altar) {
		if (!altar.hasPendingReward()) {
			return;
		}

		UUID playerUUID = altar.getPendingRewardPlayer();
		String altarDefId = altar.getPendingAltarDefinitionId();
		
		// Get player - may be null if they logged off
		EntityPlayer player = world.getPlayerEntityByUUID(playerUUID);
		if (player == null) {
			// Player not online, but still apply rewards (they'll see effects when they log back in)
			// For now, clear the pending reward - rewards require player presence
			altar.clearPendingReward();
			return;
		}

		// Reload the altar definition
		AltarDefinition definition = AltarRegistry.getAltarDefinition(altarDefId);
		if (definition == null) {
			altar.clearPendingReward();
			return;
		}

		// Consume requirements
		consumeRequirements(player, definition.getRequirements());

		// Try to find and use a custom effect handler
		boolean handlerProcessed = AltarEffectHandlerRegistry.processInteraction(world, pos, altar, definition, player);
		
		if (!handlerProcessed) {
			// Apply standard effects
			NBTTagCompound altarData = new NBTTagCompound();
			altar.writeToNBT(altarData);

			for (AltarEffect effect : definition.getEffects()) {
				if (effect.isOncePerPlayer() && altar.getPlayerUses(playerUUID) > 0) {
					continue; // Skip if already used by this player
				}
				effect.apply(world, pos, player, altarData);
			}
		}
		
		// Call post-processing hook
		AltarEffectHandlerRegistry.postProcessInteraction(world, pos, altar, definition, player);

		// Update usage counts
		altar.incrementPlayerUses(playerUUID);
		altar.incrementTotalUses();

		// Send success message
		String successMessage = definition.getGuardianSuccessMessage();
		if (successMessage != null && !successMessage.isEmpty()) {
			player.sendMessage(new TextComponentString(successMessage));
		}

		// Play sound
		world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, 
				SoundCategory.BLOCKS, 1.0F, 1.0F);

		// Clear pending reward
		altar.clearPendingReward();
	}

	private boolean checkRequirements(EntityPlayer player, AltarRequirements requirements) {
		if (true) return true;// Check XP levels
		if (requirements.getRequiredXPLevels() > 0 && player.experienceLevel < requirements.getRequiredXPLevels()) {
			return false;
		}

		// Check XP points
		if (requirements.getRequiredXPAmount() > 0 && player.experienceTotal < requirements.getRequiredXPAmount()) {
			return false;
		}

		// Check items
		for (AltarRequirements.ItemRequirement itemReq : requirements.getItems()) {
			if (!hasRequiredItem(player, itemReq)) {
				return false;
			}
		}

		// Check advancements
		if (player instanceof EntityPlayerMP) {
			EntityPlayerMP playerMP = (EntityPlayerMP) player;
			for (String advancementName : requirements.getRequiredAdvancements()) {
				ResourceLocation advId = new ResourceLocation(advancementName);
				net.minecraft.advancements.Advancement advancement = 
						playerMP.getServerWorld().getAdvancementManager().getAdvancement(advId);
				if (advancement == null || !playerMP.getAdvancements().getProgress(advancement).isDone()) {
					return false;
				}
			}
		}

		return true;
	}

	private boolean hasRequiredItem(EntityPlayer player, AltarRequirements.ItemRequirement itemReq) {
		int totalCount = 0;
		for (ItemStack stack : player.inventory.mainInventory) {
			if (itemsMatch(stack, itemReq)) {
				totalCount += stack.getCount();
				if (totalCount >= itemReq.getMinCount()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean itemsMatch(ItemStack stack, AltarRequirements.ItemRequirement itemReq) {
		if (stack.isEmpty()) return false;
		
		ItemStack required = itemReq.getItemStack();
		if (stack.getItem() != required.getItem()) return false;
		if (required.getMetadata() != 32767 && stack.getMetadata() != required.getMetadata()) return false;
		
		// Check NBT if required
		if (itemReq.getRequiredNBT() != null) {
			if (!stack.hasTagCompound()) return false;
			// TODO: More sophisticated NBT matching
		}
		
		return true;
	}

	private void consumeRequirements(EntityPlayer player, AltarRequirements requirements) {
		// Consume XP
		if (requirements.isConsumeXP()) {
			if (requirements.getRequiredXPLevels() > 0) {
				player.addExperienceLevel(-requirements.getRequiredXPLevels());
			}
			if (requirements.getRequiredXPAmount() > 0) {
				player.addExperience(-requirements.getRequiredXPAmount());
			}
		}

		// Consume items
		for (AltarRequirements.ItemRequirement itemReq : requirements.getItems()) {
			if (itemReq.isConsumed()) {
				consumeItem(player, itemReq);
			}
		}
	}

	private void consumeItem(EntityPlayer player, AltarRequirements.ItemRequirement itemReq) {
		int remaining = itemReq.getMinCount();
		for (int i = 0; i < player.inventory.mainInventory.size() && remaining > 0; i++) {
			ItemStack stack = player.inventory.mainInventory.get(i);
			if (itemsMatch(stack, itemReq)) {
				int toRemove = Math.min(remaining, stack.getCount());
				stack.shrink(toRemove);
				remaining -= toRemove;
			}
		}
	}

	private String generateObfuscatedName(String altarId) {
		Random random = new Random(altarId.hashCode());
		String[] prefixes = {"Ancient", "Forgotten", "Mysterious", "Dark", "Sacred", "Lost"};
		String[] suffixes = {"Shrine", "Monument", "Altar", "Pedestal", "Relic"};
		
		return prefixes[random.nextInt(prefixes.length)] + " " + 
		       suffixes[random.nextInt(suffixes.length)];
	}

	@Nullable
	private TileEntityAltar getTileEntity(World world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		return te instanceof TileEntityAltar ? (TileEntityAltar) te : null;
	}
}
