package com.windanesz.menhir.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BirthsignAttributeModifier {
	private static final Map<String, IAttribute> MODDED_ATTRIBUTES = new ConcurrentHashMap<>();
	private final String attribute;
	private final double amount;
	private final int operation;
	private final UUID uuid;
	private final String name;
	private final String attributeClass;
	private final String attributeField;

	public BirthsignAttributeModifier(String attribute, double amount, int operation, String birthsignName) {
		this(attribute, null, null, amount, operation, birthsignName);
	}

	public BirthsignAttributeModifier(String attribute, String attributeClass, String attributeField, double amount, int operation, String birthsignName) {
		this.attribute = attribute;
		this.attributeClass = attributeClass;
		this.attributeField = attributeField;
		this.amount = amount;
		this.operation = operation;
		this.name = "birthsign_" + birthsignName + "_" + attribute;
		this.uuid = UUID.nameUUIDFromBytes((birthsignName + ":" + attribute).getBytes());
	}

	public static void registerModdedAttribute(String name, IAttribute attribute) {
		MODDED_ATTRIBUTES.put(name, attribute);
	}

	/**
	 * Resolves an IAttribute from a class and static field name using reflection.
	 *
	 * @param className The fully qualified class name.
	 * @param fieldName The static field name.
	 * @return The IAttribute, or null if not found.
	 */
	public static IAttribute resolveAttributeFromClassField(String className, String fieldName) {
		try {
			Class<?> clazz = Class.forName(className);
			java.lang.reflect.Field field = clazz.getField(fieldName);
			Object value = field.get(null);
			if (value instanceof IAttribute) {
				return (IAttribute) value;
			}
		} catch (Exception e) {
			// Optionally log or handle error
		}
		return null;
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
		IAttribute attr = getAttributeByName(attribute);
		return attr != null ? entity.getEntityAttribute(attr) : null;
	}

	private IAttribute getAttributeByName(String name) {
		// 1. If class/field is provided, use reflection
		if (attributeClass != null && attributeField != null) {
			IAttribute attr = resolveAttributeFromClassField(attributeClass, attributeField);
			if (attr != null) return attr;
		}
		// 2. Check registered modded attributes
		if (MODDED_ATTRIBUTES.containsKey(name)) {
			return MODDED_ATTRIBUTES.get(name);
		}
		// 3. Try to resolve wizardryutils attributes
		if (name.equals("wizardryutils.SpellPotency")) {
			return resolveAttributeFromClassField("com.windanesz.wizardryutils.attributes.WizardryUtilsAttributes", "SPELL_POTENCY");
		}
		// 4. Try vanilla attributes
		switch (name) {
			case "max_health":
				return SharedMonsterAttributes.MAX_HEALTH;
			case "movement_speed":
				return SharedMonsterAttributes.MOVEMENT_SPEED;
			case "attack_damage":
				return SharedMonsterAttributes.ATTACK_DAMAGE;
			case "armor":
				return SharedMonsterAttributes.ARMOR;
			case "armor_toughness":
				return SharedMonsterAttributes.ARMOR_TOUGHNESS;
			default:
				return null;
		}
	}
} 