package com.sql2jsonfeed.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class FieldDefinition {

/*
 * 
    hsCode:
      sqlExpression: "cat.hscode"
      type: STRING
      mappings:
        index: "not_analyzed"
        store: "yes"
 * 
 */
	@JsonIgnore
	private String fieldName = null;

	private String sqlExpression = null;
	@JsonProperty("type")
	private FieldType fieldType = null;
	// ES Style mappings
	private Map<String, String> mappings = null;

	public FieldDefinition() {
		super();
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getSqlExpression() {
		return sqlExpression;
	}

	public void setSqlExpression(String dbField) {
		this.sqlExpression = dbField;
	}

	public FieldType getFieldType() {
		return fieldType;
	}

	public void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	public Map<String, String> getMappings() {
		return mappings;
	}

	public void setMappings(Map<String, String> mappings) {
		this.mappings = mappings;
	}

	@Override
	public String toString() {
		return "FieldDefinition{" +
				"fieldName='" + fieldName + '\'' +
				", sqlExpression='" + sqlExpression + '\'' +
				", fieldType=" + fieldType +
				", mappings=" + mappings +
				'}';
	}
}
