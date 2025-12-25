package com.windanesz.menhir.api.altar;

import com.windanesz.menhir.tileentity.TileEntityAltar;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Handler interface for altar effects that require special interaction logic.
 * Effects that need to handle altar activation differently (like teleport twins)
 * should register a handler implementation.
 */
public interface IAltarEffectHandler {
	
	/**
	 * Get the effect type this handler manages.
	 * Should match the effect type string from AltarEffect subclasses.
	 */
	String getEffectType();
	
	/**
	 * Check if this handler can process the given altar definition.
	 * @param definition The altar definition to check
	 * @return true if this handler should process this altar
	 */
	boolean canHandle(AltarDefinition definition);
	
	/**
	 * Handle the altar interaction before standard effects are applied.
	 * @param world The world
	 * @param pos The altar position (base block)
	 * @param altar The altar tile entity
	 * @param definition The altar definition
	 * @param player The player activating the altar
	 * @return true if the handler fully processed the interaction (skip standard effects),
	 *         false to continue with standard effect processing
	 */
	boolean handleInteraction(World world, BlockPos pos, TileEntityAltar altar, 
	                          AltarDefinition definition, EntityPlayer player);
	
	/**
	 * Called after standard effects are applied, for any cleanup or additional logic.
	 * Only called if handleInteraction returned false.
	 */
	default void postInteraction(World world, BlockPos pos, TileEntityAltar altar, 
	                             AltarDefinition definition, EntityPlayer player) {
		// Optional post-processing
	}
}
