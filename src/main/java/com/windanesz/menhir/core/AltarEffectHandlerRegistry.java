package com.windanesz.menhir.core;

import com.windanesz.menhir.api.altar.IAltarEffectHandler;
import com.windanesz.menhir.api.altar.AltarDefinition;
import com.windanesz.menhir.tileentity.TileEntityAltar;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for altar effect handlers that provide custom interaction logic.
 */
public class AltarEffectHandlerRegistry {
	
	private static final Map<String, IAltarEffectHandler> HANDLERS = new HashMap<>();
	private static final List<IAltarEffectHandler> HANDLER_LIST = new ArrayList<>();
	
	/**
	 * Register an effect handler.
	 * @param handler The handler to register
	 */
	public static void registerHandler(IAltarEffectHandler handler) {
		String type = handler.getEffectType();
		if (HANDLERS.containsKey(type)) {
			throw new IllegalStateException("Handler already registered for effect type: " + type);
		}
		HANDLERS.put(type, handler);
		HANDLER_LIST.add(handler);
	}
	
	/**
	 * Get handler for a specific effect type.
	 */
	public static IAltarEffectHandler getHandler(String effectType) {
		return HANDLERS.get(effectType);
	}
	
	/**
	 * Find the first handler that can process the given altar definition.
	 * @return The handler, or null if no handler can process this altar
	 */
	public static IAltarEffectHandler findHandler(AltarDefinition definition) {
		for (IAltarEffectHandler handler : HANDLER_LIST) {
			if (handler.canHandle(definition)) {
				return handler;
			}
		}
		return null;
	}
	
	/**
	 * Process altar interaction through the appropriate handler.
	 * @return true if a handler processed the interaction, false otherwise
	 */
	public static boolean processInteraction(World world, BlockPos pos, TileEntityAltar altar,
	                                          AltarDefinition definition, EntityPlayer player) {
		IAltarEffectHandler handler = findHandler(definition);
		if (handler != null) {
			return handler.handleInteraction(world, pos, altar, definition, player);
		}
		return false;
	}
	
	/**
	 * Call post-interaction for the appropriate handler.
	 */
	public static void postProcessInteraction(World world, BlockPos pos, TileEntityAltar altar,
	                                          AltarDefinition definition, EntityPlayer player) {
		IAltarEffectHandler handler = findHandler(definition);
		if (handler != null) {
			handler.postInteraction(world, pos, altar, definition, player);
		}
	}
}
