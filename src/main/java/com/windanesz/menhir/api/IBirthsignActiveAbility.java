package com.windanesz.menhir.api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;

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

	@Nullable
	default ResourceLocation getIcon() {
		return null;
	}

	default String getUnlocalizedName() {
		return "";
	}

	default void configure(Map<String, Object> params) {}

	default void setParentName(String name) {}

	default String getParentName() {
		return "";
	}

	default void setGroupName(String name) {}

	default String getGroupName() {
		return "";
	}
}