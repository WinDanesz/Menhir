package com.windanesz.menhir.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

import java.util.UUID;

public class BirthsignAttributeModifier {
	private final String attribute;
	private final double amount;
	private final int operation;
	private final UUID uuid;
	private final String name;

	public BirthsignAttributeModifier(String attribute, double amount, int operation, String birthsignName) {
		this.attribute = attribute;
		this.amount = amount;
		this.operation = operation;
		this.name = "birthsign_" + birthsignName + "_" + attribute;
		this.uuid = UUID.nameUUIDFromBytes((birthsignName + ":" + attribute).getBytes());
	}

	public void apply(EntityLivingBase entity, String birthsignName) {
		IAttributeInstance attr = getAttributeInstance(entity, attribute);
		if (attr != null && attr.getModifier(uuid) == null) {
			AttributeModifier mod = new AttributeModifier(uuid, name, amount, operation);
			attr.applyModifier(mod);
		}
	}

	public void remove(EntityLivingBase entity, String birthsignName) {
		IAttributeInstance attr = getAttributeInstance(entity, attribute);
		if (attr != null && attr.getModifier(uuid) != null) {
			attr.removeModifier(uuid);
		}
	}

	private IAttributeInstance getAttributeInstance(EntityLivingBase entity, String attribute) {
		// Try to resolve by name using the entity's attribute map (supports all registered attributes)
		IAttributeInstance instance = entity.getAttributeMap().getAttributeInstanceByName(attribute);
		if (instance != null) return instance;
		return null;
	}

} 