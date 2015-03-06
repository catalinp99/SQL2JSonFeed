package com.sql2jsonfeed.definition;

public enum JoinTypeEnum {

	INNER(1),
	LEFT_OUTER(2);

	private int value;

	private JoinTypeEnum(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
