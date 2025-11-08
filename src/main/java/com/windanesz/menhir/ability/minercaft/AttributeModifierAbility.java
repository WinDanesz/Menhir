package com.windanesz.menhir.ability.minercaft;

import com.windanesz.menhir.ability.ChannelingAbility;
import com.windanesz.menhir.api.BirthsignAttributeModifier;
import com.windanesz.menhir.api.IBirthsignActiveAbility;
import com.windanesz.menhir.util.ParameterUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nullable;
import java.util.Map;

public class AttributeModifierAbility extends ChannelingAbility {
	private final String attribute;
	private final double amount;
	private final int operation;
	private final String attributeClass;
	private final String attributeField;
	private final String birthsignName;

	public AttributeModifierAbility(int chargeup, String attribute, double amount, int operation, String attributeClass, String attributeField, String birthsignName) {
		super(chargeup);
		this.attribute = attribute;
		this.amount = amount;
		this.operation = operation;
		this.attributeClass = attributeClass;
		this.attributeField = attributeField;
		this.birthsignName = birthsignName;
	}

	public static IBirthsignActiveAbility create(Map<String, Object> params, String birthsignName) {
		int chargeup = getChargeup(params, 0);
		String attribute = ParameterUtils.getStringParameter(params, "attribute", "");
		double amount = ParameterUtils.getDoubleParameter(params, "amount", 0.0);
		int operation = ParameterUtils.getIntParameter(params, "operation", 0);
		String attributeClass = ParameterUtils.getStringParameter(params, "attributeClass", null);
		String attributeField = ParameterUtils.getStringParameter(params, "attributeField", null);
		return new AttributeModifierAbility(chargeup, attribute, amount, operation, attributeClass, attributeField, birthsignName);
	}

	@Override
	protected boolean executeAbility(EntityPlayer player, @Nullable Entity target) {
		BirthsignAttributeModifier mod = new BirthsignAttributeModifier(attribute, attributeClass, attributeField, amount, operation, birthsignName);
		mod.apply(player, birthsignName);
		return true;
	}
}