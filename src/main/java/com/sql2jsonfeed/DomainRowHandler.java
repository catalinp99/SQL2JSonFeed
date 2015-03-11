package com.sql2jsonfeed;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.RowCountCallbackHandler;

import com.sql2jsonfeed.definition.DomainDefinition;

public class DomainRowHandler extends RowCountCallbackHandler {

	private DomainConfig domainConfig = null;
	private DomainDefinition domainDefinition = null;
	private LinkedHashMap<Object, Map<String, Object>> valuesMapList = new LinkedHashMap<Object, Map<String, Object>>();
	
	public DomainRowHandler(DomainConfig domainConfig,
			DomainDefinition domainDefinition) {
		super();
		this.domainConfig = domainConfig;
		this.domainDefinition = domainDefinition;
	}

	public DomainConfig getDomainConfig() {
		return domainConfig;
	}

	public void setDomainConfig(DomainConfig domainConfig) {
		this.domainConfig = domainConfig;
	}

	public DomainDefinition getTypeDefinition() {
		return domainDefinition;
	}

	public void setTypeDefinition(DomainDefinition domainDefinition) {
		this.domainDefinition = domainDefinition;
	}
	
	public Map<Object, Map<String, Object>> getValuesMapList() {
		return valuesMapList;
	}

	/* 
	 * @param rs ResultSet to extract data from. This method is
	 * invoked for each row
	 * @param rowNum number of the current row (starting from 0)
	 */
	@Override
	protected void processRow(ResultSet rs, int rowNum) throws SQLException {

		// 1. Extract values -> each type definition has to extract each own values, as HashMap;
		// including the ID
		Map<String, Map<String, Object>> rowValues = domainDefinition.extractRow(rs, rowNum);
//		System.out.println(rowNum + ": " + rowValues);
		
		// 2. Merge values into the existing map
		Object rootId = domainDefinition.getRootId(rowValues);
		Map<String, Object> rootValues = valuesMapList.get(rootId);
//		System.out.println(rowNum + ": " + rootValues);
//		System.out.println(rowNum + ": " + valuesMapList);
		rootValues = domainDefinition.mergeValues(rootValues, rowValues);
//		System.out.println(rowNum + ": " + rootValues);
//		System.out.println(rowNum + ": " + valuesMapList);
		valuesMapList.put(rootId, rootValues);
//		System.out.println(rowNum + ": " + valuesMapList);
	}

	@Override
	public String toString() {
		return "DomainRowHandler [domainConfig=" + domainConfig
				+ ", domainDefinition=" + domainDefinition + "]";
	}
}
