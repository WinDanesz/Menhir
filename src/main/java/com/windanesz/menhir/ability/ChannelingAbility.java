package com.windanesz.menhir.ability;

import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.eventhandler.ChannelingManager;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Abstract base class for abilities that support channeling.
 * Subclasses should:
 * 1. Call super(channelingTicks) in their constructor
 * 2. Override executeAbility() to implement the actual ability logic
 * 3. Optionally override canActivate() for custom activation checks
 */
public abstract class ChannelingAbility implements IBirthsignActiveAbility {
	
	protected final int channelingTicks;
	
	/**
	 * @param channelingTicks Number of ticks to channel. Use 0 for instant activation.
	 */
	protected ChannelingAbility(int channelingTicks) {
		this.channelingTicks = channelingTicks;
	}
	
	/**
	 * Helper method to read the "chargeup" parameter from ability config.
	 * @param params The parameter map from JSON
	 * @param defaultValue Default chargeup time in ticks (use 0 for instant)
	 * @return The chargeup time in ticks
	 */
	protected static int getChargeup(Map<String, Object> params, int defaultValue) {
		return ParameterUtils.getIntParameter(params, "chargeup", defaultValue);
	}
	
	@Override
	public final boolean activate(EntityPlayer player, @Nullable Entity target) {
		// Only execute on the server side
		if (player.world.isRemote) {
			return false;
		}
		
		// Check if player is already channeling
		if (ChannelingManager.isPlayerChanneling(player)) {
			return false;
		}
		
		// Custom activation checks
		if (!canActivate(player, target)) {
			return false;
		}
		
		// Start channeling or execute immediately
		if (channelingTicks > 0) {
			ChannelingManager.startChanneling(player, this, channelingTicks);
			// Return false so charge isn't consumed yet - it will be consumed when channeling completes
			return false;
		} else {
			// No channeling required, execute immediately and return success status
			return executeAbility(player, target);
		}
	}
	
	@Override
	public final void onChannelingComplete(EntityPlayer player) {
		if (!player.world.isRemote) {
			boolean success = executeAbility(player, null);
			// Store the success result so ChannelingManager can check it
			ChannelingManager.setLastExecutionResult(player, success);
		}
	}
	
	/**
	 * Override this to add custom checks before the ability can be activated.
	 * @return true if the ability can be activated, false otherwise
	 */
	protected boolean canActivate(EntityPlayer player, @Nullable Entity target) {
		return true;
	}
	
	/**
	 * Override this to implement the actual ability logic.
	 * This is called either immediately (if channelingTicks == 0) or after channeling completes.
	 * Only called on the server side.
	 * @return true if the ability was successful and a charge should be consumed, false otherwise
	 */
	protected abstract boolean executeAbility(EntityPlayer player, @Nullable Entity target);
}
