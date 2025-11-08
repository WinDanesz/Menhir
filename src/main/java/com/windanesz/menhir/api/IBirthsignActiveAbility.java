package com.windanesz.menhir.api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nullable;

public interface IBirthsignActiveAbility {
	boolean activate(EntityPlayer player, @Nullable Entity target);
	
	/**
	 * Called when channeling completes successfully.
	 * Override this method for abilities that use channeling.
	 * Default implementation just calls activate().
	 */
	default void onChannelingComplete(EntityPlayer player) {
		activate(player, null);
	}
}