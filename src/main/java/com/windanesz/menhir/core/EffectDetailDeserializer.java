package com.windanesz.menhir.core;

import com.google.gson.*;
import com.windanesz.menhir.api.Birthsign;

import java.lang.reflect.Type;

/**
 * Custom GSON deserializer for EffectDetail that automatically populates the parameters map
 * from any JSON fields that aren't the "type" field.
 */
public class EffectDetailDeserializer implements JsonDeserializer<Birthsign.EffectDetail> {

	@Override
	public Birthsign.EffectDetail deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {

		JsonObject jsonObject = json.getAsJsonObject();
		Birthsign.EffectDetail effectDetail = new Birthsign.EffectDetail();

		// Extract the type field first
		if (jsonObject.has("type")) {
			String typeName = jsonObject.get("type").getAsString();
			effectDetail.type = Birthsign.EffectType.fromJsonName(typeName);
		}

		// Add all other fields to the parameters map
		for (java.util.Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
			String key = entry.getKey();
			if (!"type".equals(key)) {
				JsonElement value = entry.getValue();
				Object paramValue = parseJsonValue(value);
				effectDetail.setParameter(key, paramValue);
			}
		}

		return effectDetail;
	}

	private Object parseJsonValue(JsonElement element) {
		if (element.isJsonPrimitive()) {
			JsonPrimitive primitive = element.getAsJsonPrimitive();
			if (primitive.isString()) {
				return primitive.getAsString();
			} else if (primitive.isNumber()) {
				// Try to preserve the original number type
				String numStr = primitive.getAsString();
				if (numStr.contains(".")) {
					try {
						return primitive.getAsDouble();
					} catch (Exception e) {
						return primitive.getAsFloat();
					}
				} else {
					try {
						return primitive.getAsLong();
					} catch (Exception e) {
						return primitive.getAsInt();
					}
				}
			} else if (primitive.isBoolean()) {
				return primitive.getAsBoolean();
			}
		} else if (element.isJsonArray()) {
			return element.getAsJsonArray();
		} else if (element.isJsonObject()) {
			return element.getAsJsonObject();
		}

		return null;
	}
}
