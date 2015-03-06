package com.sql2jsonfeed.definition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sql2jsonfeed.sql.SelectBuilder;

public class TypeDefinition {

/*
<type>:
  # relation to parent object: one object or an array of object; not required for the main domain/object
  inParent: ONE | MANY
  # the field that uniquely identifies the domain
  idField: <field_name_from_field_list>
  # reference field - for sorting and detecting new values
  refField: <field_name_from_field_list>
  # Additional sort fields
  sortFields:
    <field_name_from_field_list>: ASC | DESC
  # List of tables, part of this domain
  tables:
    <table_name_1>:
  fields:
    <field_name_1>:
 */

	@JsonIgnore
	private String typeName = null;
	
	private MultiplicityEnum inParent = null;
	// Field keys
	@JsonProperty("idField")
	private String idFieldKey = null;
	@JsonProperty("refField")
	private String refFieldKey = null;
	@JsonProperty("sortFields")
	private List<String> sortFieldKeys = null;
	@JsonProperty("tables")
	private LinkedHashMap<String, TableDefinition> tablesMap = null;
	@JsonProperty("fields")
	private LinkedHashMap<String, FieldDefinition> fieldsMap;
	
	// CONSTANTS
//	private final static String REF_FIELD = "r_e_f_";
//	private final static String PK_FIELD_PREFIX = "p_k_";
	private final static String TYPE_SEP_SQL = "___";
//	private final static String TABLE_SEP_SQL = "_x_";

	public TypeDefinition() {
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public MultiplicityEnum getInParent() {
		return inParent;
	}

	public void setInParent(MultiplicityEnum inParent) {
		this.inParent = inParent;
	}

	public String getIdFieldKey() {
		return idFieldKey;
	}

	public void setIdFieldKey(String idFieldKey) {
		this.idFieldKey = idFieldKey;
	}

	public String getRefFieldKey() {
		return refFieldKey;
	}

	public void setRefFieldKey(String refFieldKey) {
		this.refFieldKey = refFieldKey;
	}

	public List<String> getSortFieldKeys() {
		return sortFieldKeys;
	}

	public void setSortFieldKeys(List<String> sortFieldKeys) {
		this.sortFieldKeys = sortFieldKeys;
	}

	public LinkedHashMap<String, TableDefinition> getTablesMap() {
		return tablesMap;
	}

	public void setTablesMap(LinkedHashMap<String, TableDefinition> tablesMap) {
		this.tablesMap = tablesMap;
	}

	public LinkedHashMap<String, FieldDefinition> getFieldsMap() {
		return fieldsMap;
	}

	public void setFieldsMap(LinkedHashMap<String, FieldDefinition> fieldsMap) {
		this.fieldsMap = fieldsMap;
	}
	
	/**
	 * Initialize and validate current type
	 * @param typeName
	 */
	public void init(String typeName) {
		this.typeName = typeName;
		// Set the table name
		if (tablesMap != null) {
			for (Map.Entry<String, TableDefinition> entry: tablesMap.entrySet()) {
				entry.getValue().setTableName(entry.getKey());
			}
		}
		// TODO also validate field keys and other fields
	}
	

	@Override
	public String toString() {
		return "TypeDefinition [typeName=" + typeName + ", inParent="
				+ inParent + ", idFieldKey=" + idFieldKey + ", refFieldKey="
				+ refFieldKey + ", sortFieldKeys=" + sortFieldKeys
				+ ", tablesMap=" + tablesMap + ", fieldsMap=" + fieldsMap + "]";
	}
}
