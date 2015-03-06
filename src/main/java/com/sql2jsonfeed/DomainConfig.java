package com.sql2jsonfeed;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * Domain configuration: datasource, 
 * 
 * @author Catalin
 */
public class DomainConfig {
	
	private String domain = null;
	private DataSource dataSource = null;

	public DomainConfig(String domain) {
		super();
		this.domain = domain;
		
		init();
	}
	
	private void init() {
		SimpleDriverDataSource simpleDataSource = new SimpleDriverDataSource();
		simpleDataSource
				.setDriverClass(com.microsoft.sqlserver.jdbc.SQLServerDriver.class);
		// dataSource.setUrl("jdbc:sqlserver://localhost:1433;"
		// + "databaseName=Rating;user=test;password=test;");
		simpleDataSource.setUrl("jdbc:sqlserver://192.168.241.45:1433;"
				+ "databaseName=xbec01p;user=xbecuser;password=xbecuser;");
		// dataSource.setUsername("test");
		// dataSource.setPassword("test");
		dataSource = simpleDataSource;
	}

	public DataSource getDataSource() {
		return dataSource;
	}
	
	public int getLimit() {
		// TODO from config
		return 200;
	}
	
	public Object getLastReferenceValue() {
		return null;
	}
}
