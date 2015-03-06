package com.sql2jsonfeed.definition;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sql2jsonfeed.sql.SelectBuilder;

public class DomainDefinition {

	private LinkedHashMap<String, TypeDefinition> typesMap = null;
	private TypeDefinition mainType = null;

	public DomainDefinition(LinkedHashMap<String, TypeDefinition> typesMap) {
		this.typesMap = typesMap;
		
		init();
	}

	private void init() {
		assert(typesMap != null && !typesMap.isEmpty());
		
		// TODO validate
		
		// Init types
		for (Map.Entry<String, TypeDefinition> entry: typesMap.entrySet()) {
			entry.getValue().init(entry.getKey());
		}
		
		mainType = typesMap.values().iterator().next();
		
		// TODO check main type required fields
	}
	
	
	/**
	 * Build a select statement from the table definitions
	 * @param selectBuilder
	 */
	public SelectBuilder buildSelect(SelectBuilder selectBuilder, boolean withRefValue) {
		assert(selectBuilder != null);
		
		// 1. Each type to add its own data to select
		
		// 2. If withRefValue == true; add the where condition. Here or higher???
		
		
		selectBuilder.orderBy(referenceField.getSqlExpression(), true);
		
		boolean isFirst = true;
		for (Map.Entry<String, TableDefinition> entry: tablesMap.entrySet()) {
			TableDefinition tableDefinition = entry.getValue();
			// FROM/JOIN
			if (tableDefinition.isJoined()) {
				selectBuilder.join(tableDefinition.getTableName(), tableDefinition.getNickname(), tableDefinition.getJoinDef().getJoinString(),
						tableDefinition.getJoinDef().getParentColumns(), tableDefinition.getJoinDef().getChildColumns());
			} else {
				selectBuilder.from(tableDefinition.getTableName(), tableDefinition.getNickname());
			}
			String fieldAsPrefix = StringUtils.replace(tableDefinition.getDomain(), ".", DOMAIN_SEP_SQL);
			// SELECT fields
			if (tableDefinition.getFieldsMap() != null) {
				for (FieldDefinition fieldDef: tableDefinition.getFieldsMap().values()) {
					selectBuilder.select(fieldDef.getSqlExpression(), fieldAsPrefix + DOMAIN_SEP_SQL + fieldDef.getFieldName());
				}
			}
			// Add PK columns for the first table only
			if (isFirst) {
				List<String> pkFieldNames = tableDefinition.getPkFieldNames();
				if (pkFieldNames != null) {
					for (String pkFieldName: pkFieldNames) {
						FieldDefinition pkFieldDefinition = tableDefinition.lookupFieldDefinition(pkFieldName);
						assert(pkFieldDefinition != null);
						selectBuilder.orderBy(pkFieldDefinition.getSqlExpression(), true);
					}
				}
				isFirst = false;
			}
		}
		return selectBuilder;
	}
	
}
