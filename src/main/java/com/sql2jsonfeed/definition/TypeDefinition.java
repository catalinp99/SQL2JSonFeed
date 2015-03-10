package com.sql2jsonfeed.definition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sql2jsonfeed.sql.SelectBuilder;
import com.sql2jsonfeed.sql.SortTypeEnum;

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

	// Setup during init
	@JsonIgnore
	private String typeName = null;
	private String parentFieldName = null;
	private String parentTypeName = null;
	
	private MultiplicityEnum parentRelation = null;
	
	// Field keys
	@JsonProperty("idField")
	private String idFieldKey = null;
	@JsonProperty("refField")
	private String refFieldKey = null;
	@JsonProperty("sortFields")
	private LinkedHashMap<String, SortTypeEnum> sortFields = null;
	@JsonProperty("tables")
	private LinkedHashMap<String, TableDefinition> tablesMap = null;
	@JsonProperty("fields")
	private LinkedHashMap<String, FieldDefinition> fieldsMap;

	// A list of children type definitions
	private List<TypeDefinition> childTypes = null;
	
	// CONSTANTS
	public final static String SQL_TYPE_SEP = "___";
	public static final String TYPE_NAME_SEP = ".";

	public TypeDefinition() {
	}

	public String getTypeName() {
		return typeName;
	}
	
	public String getParentFieldName() {
		return parentFieldName;
	}

	public String getParentTypeName() {
		return parentTypeName;
	}
	
	public void addChildTypeDef(TypeDefinition childTypeDefinition) {
		if (childTypes == null) {
			childTypes = new ArrayList<TypeDefinition>();
		}
		childTypes.add(childTypeDefinition);
	}

	public List<TypeDefinition> getChildTypes() {
		return childTypes;
	}

	public MultiplicityEnum getParentRelation() {
		return parentRelation;
	}

	public void setParentRelation(MultiplicityEnum parentRelation) {
		this.parentRelation = parentRelation;
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

	public LinkedHashMap<String, SortTypeEnum> getSortFields() {
		return sortFields;
	}

	public void setSortFields(LinkedHashMap<String, SortTypeEnum> sortFields) {
		this.sortFields = sortFields;
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
		
		if (fieldsMap != null) {
			for (Map.Entry<String, FieldDefinition> fieldEntry: fieldsMap.entrySet()) {
				fieldEntry.getValue().setFieldName(fieldEntry.getKey());
			}
		}
		
		// TODO also validate field keys and other fields
		
		// Init parent fields and prefix
		int nameIndex = this.typeName.lastIndexOf(TYPE_NAME_SEP);
		if (nameIndex <= 0) {
			// This is the top level parent
			selectItemPrefix = this.typeName + SQL_TYPE_SEP;
		} else {
			selectItemPrefix = StringUtils.replace(this.typeName, TYPE_NAME_SEP, SQL_TYPE_SEP) + SQL_TYPE_SEP;
			parentTypeName = StringUtils.substring(this.typeName, 0, nameIndex);
			parentFieldName = StringUtils.substring(this.typeName, nameIndex + 1);
		}
	}
	
	private String selectItemPrefix = null;
	
	public String fieldAsSelectItem(String fieldName) {
		return selectItemPrefix + fieldName;
	}
	
	public void addToSelectBuilder(SelectBuilder selectBuilder, boolean withRefValue) {
		// 1. Add tables
		if (tablesMap != null) {
			for (Map.Entry<String, TableDefinition> entry: tablesMap.entrySet()) {
				TableDefinition tableDefinition = entry.getValue();
				// FROM/JOIN
				if (tableDefinition.isJoined()) {
					selectBuilder.join(tableDefinition.getTableName(), tableDefinition.getNickname(), tableDefinition.getJoinDef().getJoinString(),
							tableDefinition.getJoinDef().getParentColumns(), tableDefinition.getJoinDef().getChildColumns());
				} else {
					selectBuilder.from(tableDefinition.getTableName(), tableDefinition.getNickname());
				}
			}
		}
		
		// 2. Add sort by reference
		if (refFieldKey != null) {
			selectBuilder.orderBy(fieldAsSelectItem(refFieldKey), SortTypeEnum.ASC);
		}
		
		// 2. add select fields, and sort by
		if (getFieldsMap() != null) {
			for (FieldDefinition fieldDef: getFieldsMap().values()) {
				String selectItemName = fieldAsSelectItem(fieldDef.getFieldName());
				selectBuilder.select(fieldDef.getSqlExpression(), selectItemName);
				if (sortFields != null) {
					SortTypeEnum sortType = sortFields.get(fieldDef.getFieldName());
					if (sortType != null) {
						selectBuilder.orderBy(selectItemName, sortType);
					}
				}
			}
		}
	
		// 3. Add where
		if (withRefValue && StringUtils.isNotEmpty(refFieldKey)) {
			FieldDefinition refFieldDef = fieldsMap.get(refFieldKey);
			assert(refFieldDef != null);
			selectBuilder.where(refFieldDef.getSqlExpression() + " > :" + SelectBuilder.P_REF_VALUE);
		}
	}

	public Map<String, Object> extractRow(ResultSet rs, int rowNum) throws SQLException {
		if (getFieldsMap() == null) {
			return null;
		}
		
		Map<String, Object> typeRowValues = new LinkedHashMap<String, Object>();
		for (FieldDefinition fieldDef: getFieldsMap().values()) {
			typeRowValues.put(fieldDef.getFieldName(), getFieldValue(rs, fieldDef));
		}
		return typeRowValues;
	}
	
	// TODO Separate helper class
	private Object getFieldValue(ResultSet rs, FieldDefinition fieldDef) throws SQLException {
		// TODO More efficient - the field aliases were already computed previously
		String selectItemName = fieldAsSelectItem(fieldDef.getFieldName());
		switch (fieldDef.getFieldType()) {
		case STRING:
			return rs.getString(selectItemName);
		case DATETIME:
			return (java.util.Date) rs.getDate(selectItemName);
		case NUMBER:
			return rs.getBigDecimal(selectItemName);
		}
		return null;
	}

	@Override
	public String toString() {
		return "TypeDefinition [typeName=" + typeName + ", parentFieldName="
				+ parentFieldName + ", parentTypeName=" + parentTypeName
				+ ", parentRelation=" + parentRelation + ", idFieldKey="
				+ idFieldKey + ", refFieldKey=" + refFieldKey + ", sortFields="
				+ sortFields + ", tablesMap=" + tablesMap + ", fieldsMap="
				+ fieldsMap + ", childTypes=" + childTypes
				+ ", selectItemPrefix=" + selectItemPrefix + "]";
	}
}
