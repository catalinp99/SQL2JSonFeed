package com.sql2jsonfeed.definition;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JoinDefinition {

	/*
	 * join: type: "ONE_TO_ONE" parentColumns: -
	 * "cpOrderDetails.OrderConsigneeId" childColumns: - "OrderConsigneeId"
	 */

	@JsonProperty("type")
	private JoinType joinType; // todo enum

	@JsonProperty("parentColumns")
	private List<String> parentColumns;
	@JsonProperty("childColumns")
	private List<String> childColumns;

	public JoinDefinition() {
		super();
	}

	public JoinType getJoinType() {
		return joinType;
	}

	public void setJoinType(JoinType joinType) {
		this.joinType = joinType;
	}

	public List<String> getParentColumns() {
		return parentColumns;
	}

	public void setParentColumns(List<String> parentColumns) {
		this.parentColumns = parentColumns;
	}

	public List<String> getChildColumns() {
		return childColumns;
	}

	public void setChildColumns(List<String> childColumns) {
		this.childColumns = childColumns;
	}

	public String getJoinString() {
		// TODO use constants or enums
		switch (joinType) {
		case ONE_TO_ONE:
		case ONE_TO_MANY:
			return "INNER JOIN";
		case ZERO_TO_ONE:
		case ZERO_TO_MANY:
			return "LEFT OUTER JOIN";
		}
		return null;
	}

	@Override
	public String toString() {
		return "JoinDefinition [joinType=" + joinType + ", parentColumns="
				+ parentColumns + ", childColumns=" + childColumns + "]";
	}
}
