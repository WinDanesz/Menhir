package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.api.BirthsignAttributeModifier;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nullable;
import java.util.Map;

public class AttributeModifierAbility implements IBirthsignActiveAbility {
	private final String attribute;
	private final double amount;
	private final int operation;
	private final String attributeClass;
	private final String attributeField;
	private final String birthsignName;

	public AttributeModifierAbility(String attribute, double amount, int operation, String attributeClass, String attributeField, String birthsignName) {
		this.attribute = attribute;
		this.amount = amount;
		this.operation = operation;
		this.attributeClass = attributeClass;
		this.attributeField = attributeField;
		this.birthsignName = birthsignName;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		String attribute = ParameterUtils.getStringParameter(params, "attribute", "");
		double amount = ParameterUtils.getDoubleParameter(params, "amount", 0.0);
		int operation = ParameterUtils.getIntParameter(params, "operation", 0);
		String attributeClass = ParameterUtils.getStringParameter(params, "attributeClass", null);
		String attributeField = ParameterUtils.getStringParameter(params, "attributeField", null);
		return new AttributeModifierAbility(attribute, amount, operation, attributeClass, attributeField, birthsignName);
	}

	@Override
	public boolean activate(EntityPlayer player, @Nullable Entity target) {
		BirthsignAttributeModifier mod = new BirthsignAttributeModifier(attribute, attributeClass, attributeField, amount, operation, birthsignName);
		mod.apply(player, birthsignName);
		return false;
	}
} 