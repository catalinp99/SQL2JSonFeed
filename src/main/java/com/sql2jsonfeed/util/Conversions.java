package com.sql2jsonfeed.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.sql2jsonfeed.definition.FieldDefinition;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.sql2jsonfeed.definition.FieldType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Data conversion between various types
 * @author Take Moa
 */
public class Conversions {

	/**
	 * Convert from a value as received from ES API to a value having the expected type as per fieldType.
	 * 
	 * @param esValue
	 * @param fieldType
	 * @param esFormat
	 * @return
	 */
	public static Object fromEsValue(Object esValue, FieldType fieldType, String esFormat) {
		if (esValue == null) {
			return null;
		}
		
		if (fieldType == FieldType.DATE || fieldType == FieldType.DATETIME) {
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
				// Create one automatically - Use UTC (as dates are coming from ES as UTC)
                DateTime dateTimeUtc = new DateTime((Long)esValue, DateTimeZone.UTC );
                Date date = dateTimeUtc.toDate();
                return date;
            } else if (esClass == Double.class) {
                // Create one automatically - Use UTC (as dates are coming from ES as UTC)
                DateTime dateTimeUtc = new DateTime(((Double)esValue).longValue(), DateTimeZone.UTC );
                Date date = dateTimeUtc.toDate();
                return date;
			}
		// TODO implement the other types also
//        if (refFieldDef.getFieldType() == FieldType.DATE) {
//            maxValue = new java.sql.Date((long) esValue);
//        } else if (refFieldDef.getFieldType() == FieldType.DATETIME) {
//            maxValue = new java.sql.Timestamp((long) esValue);
//        } else if (fieldType == FieldType.FLOAT) {
//            maxValue = new Float(esValue);
//        } else if (fieldType == FieldType.DOUBLE) {
//            maxValue = new Double(esValue);
//        } else if (fieldType == FieldType.INTEGER) {
//            maxValue = new Integer((int) esValue);
//        } else if (fieldType == FieldType.LONG) {
//            maxValue = new Long((long) esValue);
//        } else if (fieldType == FieldType.SHORT) {
//            maxValue = new Short((short) esValue);
        }
		
		return esValue;
	}

    /**
     * Convert a value received from DB to the corresponding ES field value.
     *
     * @param rs
     * @param fieldDef
     * @param columnLabel
     * @param dbTimeZone
     * @return The ES field value
     * @throws SQLException
     */
    public static Object sqlToESValue(ResultSet rs, FieldDefinition fieldDef, String columnLabel, TimeZone dbTimeZone) throws SQLException {

        switch (fieldDef.getFieldType()) {
            case STRING:
                return rs.getString(columnLabel);
            case DATE:
                return rs.getDate(columnLabel);
            case DATETIME:
                // TODO - get the timezone from configuration
                if (dbTimeZone == null) {
                    return rs.getTimestamp(columnLabel);
                }
                return rs.getTimestamp(columnLabel, Calendar.getInstance(dbTimeZone));
            case FLOAT:
                return rs.getFloat(columnLabel);
            case DOUBLE:
                return rs.getDouble(columnLabel);
            case INTEGER:
                return rs.getInt(columnLabel);
            case LONG:
                return rs.getLong(columnLabel);
            case SHORT:
                return rs.getShort(columnLabel);
            case BOOLEAN:
                return rs.getBoolean(columnLabel);
        }
        return null;

    };
}
