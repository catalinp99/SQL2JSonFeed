package com.sql2jsonfeed.util;

import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.elasticsearch.common.xcontent.XContentBuilder;

import com.sql2jsonfeed.definition.FieldType;

/**
 * Data conversion between various types
 * @author Take Moa
 */
public class Conversions {

	/**
	 * Convert from a value as received from ES API to a value having the expected type as per fieldType.
	 * 
	 * @param esValue
	 * @param expectedType
	 * @param esFormat
	 * @return
	 */
	public static Object toFieldValue(Object esValue, FieldType expectedType, String esFormat) { 
		if (esValue == null) {
			return null;
		}
		
		if (expectedType == FieldType.DATE || expectedType == FieldType.DATETIME) {
			// Must return a date
			Class<?> esClass = esValue.getClass();
			if (esClass == Date.class) {
				return esValue;
			} else if (esClass == String.class) {
				// convert from String using Joda
				if (esFormat == null) {
					return XContentBuilder.defaultDatePrinter.parseDateTime((String)esValue).toDate();
				} else {
					DateTimeFormatter.ofPattern(esFormat).parse((String)esValue);
				}
			} else if (esClass == Long.class) {
				// Create one automatically
				return new Date((Long)esValue);
			}
		}
		
		return esValue;
	}
}
