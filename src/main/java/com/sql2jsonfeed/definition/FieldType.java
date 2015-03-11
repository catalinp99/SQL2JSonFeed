package com.sql2jsonfeed.definition;

public enum FieldType {

	FLOAT (1),
	DOUBLE (2),
	INTEGER (3),
	LONG (4),
	SHORT (5),
	BOOLEAN (6),
	STRING (7),
	DATE (8),
	DATETIME (9);

	private int value;

	private FieldType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
