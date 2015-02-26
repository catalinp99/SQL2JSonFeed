package com.sql2jsonfeed.definition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Catalin
 */
public class TableDefinition {

/*

cpOrderConsignee:
  as: "cpOrderConsignee"
  domain: "order"
  primaryKeyColumns:
  - "OrderConsigneeId"
  join:
    type: "1to1"
    parentColumns:
    - "cpOrderDetails.OrderConsigneeId"
    childColumns:
    - "OrderConsigneeId"
  fields:
    consigneeNumber:
      dbField: "ConsigneeNumber"
      type: "string"

 */

	@JsonIgnore
	private String tableName;
	@JsonProperty("as")
	private String nickname;
	private String domain;
	
	// list of column names
	@JsonProperty("primaryKeyColumns")
	private List<String> pkColumns;
	@JsonProperty("join")
	private JoinDefinition joinDef;
	
	@JsonProperty("fields")
	private LinkedHashMap<String, FieldDefinition> fieldsMap;

	public TableDefinition() {
		super();
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public List<String> getPkColumns() {
		return pkColumns;
	}

	public void setPkColumns(List<String> pkColumns) {
		this.pkColumns = pkColumns;
	}

	public JoinDefinition getJoinDef() {
		return joinDef;
	}

	public void setJoinDef(JoinDefinition joinDef) {
		this.joinDef = joinDef;
	}

	public LinkedHashMap<String, FieldDefinition> getFieldsMap() {
		return fieldsMap;
	}

	public void setFieldsMap(LinkedHashMap<String, FieldDefinition> fieldsMap) {
		this.fieldsMap = fieldsMap;
		
		// Set the field name in the FieldDefinition
		if (fieldsMap != null) {
			for (Map.Entry<String, FieldDefinition> entry: fieldsMap.entrySet()) {
				entry.getValue().setFieldName(entry.getKey());
			}
		}
	}

	@Override
	public String toString() {
		return "TableDefinition [tableName=" + tableName + ", nickname="
				+ nickname + ", domain=" + domain + ", pkColumns=" + pkColumns
				+ ", joinDef=" + joinDef + ", fieldsMap=" + fieldsMap + "]";
	}
}
