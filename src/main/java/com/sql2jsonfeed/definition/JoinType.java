package com.sql2jsonfeed.definition;

public enum JoinType {

	ONE_TO_ONE(1),
	ZERO_TO_ONE(2),
	ONE_TO_MANY(3),
	ZERO_TO_MANY(4);

	private int value;

	private JoinType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
