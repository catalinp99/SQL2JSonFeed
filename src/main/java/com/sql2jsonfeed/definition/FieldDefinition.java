package com.sql2jsonfeed.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FieldDefinition {

/*
 * 
    consigneeNumber:
      dbField: "ConsigneeNumber"
      type: "string"
 * 
 */
	@JsonIgnore
	private String fieldName = null;

	// Either one must be not null
	private String sqlExpression = null;
	@JsonProperty("type")
	private FieldType fieldType = null;

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

	@Override
	public String toString() {
		return "FieldDefinition [fieldName=" + fieldName + ", sqlExpression="
				+ sqlExpression + ", fieldType=" + fieldType + "]";
	}
}
