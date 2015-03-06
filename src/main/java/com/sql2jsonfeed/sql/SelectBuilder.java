package com.sql2jsonfeed.sql;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.text.StrSubstitutor;


/**
 * Helper for creating a select statement compatible with JDBC
 * 
 * @author Catalin
 */
public class SelectBuilder {
	
	// Default templates
	private String templateSelectClause = "SELECT TOP (:limit) ${select_list}";
	private String templateFromClause = "   FROM ${from_list}";
	private String templateWhereClause = "  WHERE ${where_list}";
	private String templateOrderByClause = "  ORDER BY ${order_by_list}";
	
	// Builders
	private StringBuffer selectListBuilder = new StringBuffer();
	private StringBuffer fromListBuilder = new StringBuffer();
	private StringBuffer whereListBuilder = new StringBuffer();
	private StringBuffer orderByBuilder = new StringBuffer();

	private static final String COMMA = ", ";
	private static final String NL = "\n";
	private static final String INDENT = "     ";

	public SelectBuilder() {
		// TODO get templates here
	}
	
	public SelectBuilder select(String columnExpression, String asName) {
		if (selectListBuilder.length() > 0) {
			selectListBuilder.append(NL);
			selectListBuilder.append(INDENT + COMMA);
		}
		selectListBuilder.append(columnExpression);
		if (asName != null) {
			selectListBuilder.append(" AS " + asName);
		}
		return this;
	}
	
	public SelectBuilder from(String tableName, String asName) {
		if (fromListBuilder.length() > 0) {
			fromListBuilder.append(NL);
			fromListBuilder.append(COMMA);
		}
		fromListBuilder.append(tableName + " AS " + asName);
		return this;
	}
	
	public SelectBuilder join(String tableName, String asName, String joinString, List<String> parentKeys, List<String> childKeys) {
		// TODO Use template
		// TODO joinString == constant (or enum)
		// INNER JOIN dbo.cpOrderConsignee AS cpoc WITH (NOLOCK) ON cpod.OrderConsigneeId = cpoc.OrderConsigneeId
		assert(fromListBuilder.length() > 0);
		assert(parentKeys.size() == childKeys.size());
		assert(parentKeys.size() > 0);
		fromListBuilder.append(NL);
		fromListBuilder.append(INDENT + joinString + " " + tableName + " AS " + asName);
		fromListBuilder.append(" ON " + parentKeys.get(0) + " = " + childKeys.get(0));
		for (int i = 1; i < parentKeys.size(); ++i) {
			fromListBuilder.append(" AND " + parentKeys.get(i) + " = " + childKeys.get(i));
		}
		
		return this;
	}

	public SelectBuilder where(String whereCondition) {
		if (whereListBuilder.length() > 0) {
			whereListBuilder.append(NL);
			whereListBuilder.append(INDENT + "AND ");
		}
		whereListBuilder.append(whereCondition);
		return this;
	}

	public SelectBuilder orderBy(String orderByField, boolean asc) {
		if (orderByBuilder.length() > 0) {
			orderByBuilder.append(NL);
			orderByBuilder.append(INDENT + COMMA);
		}
		orderByBuilder.append(orderByField);
		if (asc) {
			orderByBuilder.append(" ASC");
		} else {
			orderByBuilder.append(" DESC");
		}
		return this;
	}
	
	public String buildSelectQuery() {
		// Use template commons substr
//		private String templateSelectClause = "SELECT TOP :limit ${select_list}";
//		private String templateFromClause = "   FROM ${from_list}\n";
//		private String templateWhereClause = "  WHERE ${where_list}\n";
//		private String templateOrderByClause = "  ORDER BY ${order_by_list}\n";
		StringBuffer templateBuffer = new StringBuffer(templateSelectClause + NL + templateFromClause);
		HashMap<String, String> valuesMap = new HashMap<String, String>(4);
		valuesMap.put("select_list", selectListBuilder.toString());
		valuesMap.put("from_list", fromListBuilder.toString());
		if (whereListBuilder.length() > 0) {
			templateBuffer.append(NL + templateWhereClause);
			valuesMap.put("where_list", whereListBuilder.toString());
		}
		if (orderByBuilder.length() > 0) {
			templateBuffer.append(NL + templateOrderByClause);
			valuesMap.put("order_by_list", orderByBuilder.toString());
		}
		
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		String selectQuery = sub.replace(templateBuffer);
		
		return selectQuery;
	}
}
