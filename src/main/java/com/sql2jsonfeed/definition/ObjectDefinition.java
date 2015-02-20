package com.sql2jsonfeed.definition;

import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Catalin
 */
public class ObjectDefinition {

	@JsonProperty("index")
	private String indexName;
	@JsonProperty("table")
	private String tableName;
	private String[] primaryKey;
	private Map<String, ObjectDefinition> children = null;
	private boolean noModel = false;

	public ObjectDefinition() {
		super();
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String[] getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(String[] primaryKey) {
		this.primaryKey = primaryKey;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public boolean isNoModel() {
		return noModel;
	}

	public void setNoModel(boolean noModel) {
		this.noModel = noModel;
	}

	public Map<String, ObjectDefinition> getChildren() {
		return children;
	}

	public void setChildren(Map<String, ObjectDefinition> children) {
		this.children = children;
	}

	@Override
	public String toString() {
		return "ObjectDefinition [indexName=" + indexName + ", tableName="
				+ tableName + ", primaryKey=" + Arrays.toString(primaryKey)
				+ ", children=" + children + ", noModel=" + noModel + "]";
	}
}
