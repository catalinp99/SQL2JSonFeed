package com.sql2jsonfeed.definition;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.sql2jsonfeed.sql.SelectBuilder;

public class TypeDefinition {

	private Map<String, TableDefinition> tableMap = null;

	public TypeDefinition(Map<String, TableDefinition> tableMap) {
		this.tableMap = tableMap;
		
		init();
	}

	private void init() {
		// Set the table name
		if (tableMap != null) {
			for (Map.Entry<String, TableDefinition> entry: tableMap.entrySet()) {
				entry.getValue().setTableName(entry.getKey());
			}
		}
	}
	
	private String DOMAIN_SEP_SQL = "___";
	
	/**
	 * Build a select statement from the table definitions
	 * @param selectBuilder
	 */
	public SelectBuilder buildSelect(SelectBuilder selectBuilder) {
		assert(selectBuilder != null);
		if (tableMap == null) {
			return selectBuilder; // nothing to do
		}
		for (Map.Entry<String, TableDefinition> entry: tableMap.entrySet()) {
			TableDefinition tableDefinition = entry.getValue();
			// FROM/JOIN
			if (tableDefinition.isJoined()) {
				selectBuilder.join(tableDefinition.getTableName(), tableDefinition.getNickname(), tableDefinition.getJoinDef().getJoinString(),
						tableDefinition.getJoinDef().getParentColumns(), tableDefinition.getJoinDef().getChildColumns());
			} else {
				selectBuilder.from(tableDefinition.getTableName(), tableDefinition.getNickname());
			}
			// SELECT fields
			if (tableDefinition.getFieldsMap() != null) {
				String fieldAsPrefix = StringUtils.replace(tableDefinition.getDomain(), ".", DOMAIN_SEP_SQL);
				for (FieldDefinition fieldDef: tableDefinition.getFieldsMap().values()) {
					selectBuilder.select(fieldDef.getSqlExpression(), fieldAsPrefix + DOMAIN_SEP_SQL + fieldDef.getFieldName());
				}
			}
			if (tableDefinition.getReferenceColumn() != null) {
				selectBuilder.orderBy(tableDefinition.getReferenceColumn(), true);
//				selectBuilder.where(tableDefinition.getReferenceColumn() + " > :" + tableDefinition.getTableName() + "___ref");
			}
		}
		return selectBuilder;
	}

	@Override
	public String toString() {
		return "TypeDefinition [tableMap=" + tableMap + "]";
	}
}
