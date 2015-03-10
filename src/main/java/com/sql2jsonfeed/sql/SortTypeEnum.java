package com.sql2jsonfeed.sql;

public enum SortTypeEnum {
	ASC(1),
	DESC(2);

	private int value;

	private SortTypeEnum(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
