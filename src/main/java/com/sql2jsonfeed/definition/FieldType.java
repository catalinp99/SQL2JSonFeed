package com.sql2jsonfeed.definition;

public enum FieldType {

	STRING(1),
	DATETIME(2),
	NUMBER(3);

	private int value;

	private FieldType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
