package com.windanesz.menhir.util;

import java.util.Map;

/**
 * Utility class for extracting parameters from Maps with proper type conversion and default values.
 * Eliminates code duplication across ability factories.
 */
public class ParameterUtils {

	/**
	 * Extracts a double parameter from the map with a default value.
	 *
	 * @param parameters   The parameter map
	 * @param key          The parameter key
	 * @param defaultValue The default value if the parameter is missing or invalid
	 * @return The double value
	 */
	public static double getDoubleParameter(Map<String, Object> parameters, String key, double defaultValue) {
		Object value = parameters.get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		if (value instanceof String) {
			try {
				return Double.parseDouble((String) value);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Extracts an int parameter from the map with a default value.
	 *
	 * @param parameters   The parameter map
	 * @param key          The parameter key
	 * @param defaultValue The default value if the parameter is missing or invalid
	 * @return The int value
	 */
	public static int getIntParameter(Map<String, Object> parameters, String key, int defaultValue) {
		Object value = parameters.get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value instanceof String) {
			try {
				return Integer.parseInt((String) value);
			} catch (NumberFormatException e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Extracts a String parameter from the map with a default value.
	 *
	 * @param parameters   The parameter map
	 * @param key          The parameter key
	 * @param defaultValue The default value if the parameter is missing
	 * @return The String value
	 */
	public static String getStringParameter(Map<String, Object> parameters, String key, String defaultValue) {
		Object value = parameters.get(key);
		if (value == null) {
			return defaultValue;
		}
		return value.toString();
	}

	/**
	 * Extracts a boolean parameter from the map with a default value.
	 *
	 * @param parameters   The parameter map
	 * @param key          The parameter key
	 * @param defaultValue The default value if the parameter is missing or invalid
	 * @return The boolean value
	 */
	public static boolean getBooleanParameter(Map<String, Object> parameters, String key, boolean defaultValue) {
		Object value = parameters.get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof String) {
			return Boolean.parseBoolean((String) value);
		}
		if (value instanceof Number) {
			return ((Number) value).intValue() != 0;
		}
		return defaultValue;
	}
} 