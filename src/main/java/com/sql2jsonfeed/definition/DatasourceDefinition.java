package com.sql2jsonfeed.definition;

import java.sql.Driver;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sql2jsonfeed.config.ConfigException;

/**
 * Define a JDBC data source
 *
 * @author Catalin
 *
 */
public class DatasourceDefinition {
	
	@JsonProperty("driverClassName")
	private String jdbcDriverClassName = null;

	@JsonProperty("url")
	private String dbUrl = null;

	private String username = null;
	private String password = null;
	
	private DataSource dataSource = null;
	
	public DatasourceDefinition() {
		super();
	}

	public synchronized DataSource getDataSource() {

		if (dataSource == null) {
			SimpleDriverDataSource simpleDataSource = new SimpleDriverDataSource();
			try {
				simpleDataSource
						.setDriverClass((Class<? extends Driver>) Class.forName(getJdbcDriverClassName()));
			} catch (ClassNotFoundException e) {
				// TODO throw a different type of exception, or validate first
				throw new ConfigException("Invalid driver class: " + getJdbcDriverClassName(), e);
			}
			simpleDataSource.setUrl(getDbUrl());
			if (!StringUtils.isEmpty(getUsername())) {
				simpleDataSource.setUsername(getUsername());
			}
			if (!StringUtils.isEmpty(getPassword())) {
				simpleDataSource.setPassword(getPassword());
			}
			dataSource = simpleDataSource;
		}
		
		return dataSource;
	}
	
	public String getJdbcDriverClassName() {
		return jdbcDriverClassName;
	}
	public void setJdbcDriverClassName(String jdbcDriverClassName) {
		this.jdbcDriverClassName = jdbcDriverClassName;
	}
	public String getDbUrl() {
		return dbUrl;
	}
	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	@Override
	public String toString() {
		return "DatasourceDefinition [jdbcDriverClassName="
				+ jdbcDriverClassName + ", dbUrl=" + dbUrl + ", username="
				+ username + ", password=" + password + "]";
	}
}
