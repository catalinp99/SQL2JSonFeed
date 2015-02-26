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
	
	private String dbField = null;
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

	public String getDbField() {
		return dbField;
	}

	public void setDbField(String dbField) {
		this.dbField = dbField;
	}

	public FieldType getFieldType() {
		return fieldType;
	}

	public void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	@Override
	public String toString() {
		return "FieldDefinition [fieldName=" + fieldName + ", dbField="
				+ dbField + ", fieldType=" + fieldType + "]";
	}
}
