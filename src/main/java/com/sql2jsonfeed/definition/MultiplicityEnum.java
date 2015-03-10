package com.sql2jsonfeed.definition;

public enum MultiplicityEnum {
	ONE(1),
	MANY(2);

	private int value;

	private MultiplicityEnum(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
