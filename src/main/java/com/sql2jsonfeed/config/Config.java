package com.sql2jsonfeed.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sql2jsonfeed.definition.ChannelDefinition;
import com.sql2jsonfeed.definition.DatasourceDefinition;
import com.sql2jsonfeed.sql.SqlTemplates;

/**
 * Application config as loaded from configuration file
 * 
 * @author Catalin
 *
 */
public class Config {

	private int runIntervalMins = 30;
	private int batchSize = 5000;
	
	// ES cluster name
	private String clusterName = null;
	
	// Datasources map: name to Datasource
	@JsonProperty("datasources")
	private Map<String, DatasourceDefinition> datasourceMap;
	
	// SQL Templates map: driver name to 
	@JsonProperty("sqlTemplates")
	private Map<String, SqlTemplates> sqlTemplatesMap;
	
	// The channel list
	private LinkedHashMap<String, ChannelDefinition> channels;

	public Config() {
		super();
	}

	public int getRunIntervalMins() {
		return runIntervalMins;
	}

	public void setRunIntervalMins(int runIntervalMins) {
		this.runIntervalMins = runIntervalMins;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public Map<String, DatasourceDefinition> getDatasourceMap() {
		return datasourceMap;
	}

	public void setDatasourceMap(Map<String, DatasourceDefinition> datasourceMap) {
		this.datasourceMap = datasourceMap;
	}

	public Map<String, SqlTemplates> getSqlTemplatesMap() {
		return sqlTemplatesMap;
	}

	public void setSqlTemplatesMap(Map<String, SqlTemplates> sqlTemplatesMap) {
		this.sqlTemplatesMap = sqlTemplatesMap;
	}

	public Map<String, ChannelDefinition> getChannels() {
		return channels;
	}

	public void setChannels(LinkedHashMap<String, ChannelDefinition> channels) {
		this.channels = channels;
	}
	
	public void validate() throws ConfigException {
		if (channels == null || channels.isEmpty()) {
			throw new ConfigException("No channles defined in config");
		}
		
		// TODO more validation
		// Datasources for each channel
		
	}

	@Override
	public String toString() {
		return "Config [runIntervalMins=" + runIntervalMins + ", batchSize="
				+ batchSize + ", clusterName=" + clusterName
				+ ", datasourceMap=" + datasourceMap + ", sqlTemplatesMap="
				+ sqlTemplatesMap + ", channels=" + channels + "]";
	}
}
