package com.sql2jsonfeed.definition;

import java.util.Map;

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

	@Override
	public String toString() {
		return "TypeDefinition [tableMap=" + tableMap + "]";
	}
}
