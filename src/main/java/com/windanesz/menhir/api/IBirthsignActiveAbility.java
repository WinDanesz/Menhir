package com.windanesz.menhir.api;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nullable;

public interface IBirthsignActiveAbility {
	boolean activate(EntityPlayer player, @Nullable Entity target);
} 