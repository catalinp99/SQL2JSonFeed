package com.sql2jsonfeed;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;

import org.springframework.jdbc.core.RowCountCallbackHandler;

import com.sql2jsonfeed.definition.TypeDefinition;

public class DomainRowHandler extends RowCountCallbackHandler {

	private DomainConfig domainConfig = null;
	private TypeDefinition typeDefinition = null;
	
	private LinkedHashMap<String, Object> domainRows = new LinkedHashMap<String, Object>();

	public DomainRowHandler(DomainConfig domainConfig,
			TypeDefinition typeDefinition) {
		super();
		this.domainConfig = domainConfig;
		this.typeDefinition = typeDefinition;
	}

	public DomainConfig getDomainConfig() {
		return domainConfig;
	}

	public void setDomainConfig(DomainConfig domainConfig) {
		this.domainConfig = domainConfig;
	}

	public TypeDefinition getTypeDefinition() {
		return typeDefinition;
	}

	public void setTypeDefinition(TypeDefinition typeDefinition) {
		this.typeDefinition = typeDefinition;
	}

	/* 
	 * @param rs ResultSet to extract data from. This method is
	 * invoked for each row
	 * @param rowNum number of the current row (starting from 0)
	 */
	@Override
	protected void processRow(ResultSet rs, int rowNum) throws SQLException {
		// Create here the ES object
		
		// 1. Get the main domain PK as string ID
		// domainConfig.createID(pkValueList)
		
		
		// 2. Lookup if
		
	}

	@Override
	public String toString() {
		return "DomainRowHandler [domainConfig=" + domainConfig
				+ ", typeDefinition=" + typeDefinition + "]";
	}
}
