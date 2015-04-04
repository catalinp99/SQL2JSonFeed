package com.sql2jsonfeed.channel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.sql2jsonfeed.DomainRowHandler;
import com.sql2jsonfeed.ESClientManager;
import com.sql2jsonfeed.config.Config;
import com.sql2jsonfeed.config.ConfigManager;
import com.sql2jsonfeed.definition.ChannelDefinition;
import com.sql2jsonfeed.definition.DatasourceDefinition;
import com.sql2jsonfeed.definition.DomainDefinition;
import com.sql2jsonfeed.sql.SelectBuilder;
import com.sql2jsonfeed.sql.SqlTemplates;

/**
 * Manages a single channel
 * 
 * @author Take Moa
 */
public class ChannelManager {

	private String channelName;
	private ConfigManager configManager;
	private ChannelDefinition channelDefinition;
	private DomainDefinition domainDefinition;
	// Calculated/initialized values
	private int batchSize;
	private int maxRecords;
	private String esClusterName = null;

	public ChannelManager(String channelName, ConfigManager configManager,
			ChannelDefinition channelDefinition,
			DomainDefinition domainDefinition) {
		super();
		this.channelName = channelName;
		this.configManager = configManager;
		this.channelDefinition = channelDefinition;
		this.domainDefinition = domainDefinition;

		init();
	}

	private void init() {
		batchSize = channelDefinition.getBatchSize();
		if (batchSize <= 0) {
			batchSize = configManager.getDefaultBatchSize();
		}
		maxRecords = channelDefinition.getMaxRecords();
		esClusterName = channelDefinition.getEsClusterName();
		if (StringUtils.isEmpty(esClusterName)) {
			esClusterName = configManager.getDefaultEsClusterName();
		}
	}

	public String getChannelName() {
		return channelName;
	}

	public ConfigManager getConfigManager() {
		return configManager;
	}

	public ChannelDefinition getChannelDefinition() {
		return channelDefinition;
	}

	public DomainDefinition getDomainDefinition() {
		return domainDefinition;
	}

	@Override
	public String toString() {
		return "ChannelManager [channelName=" + channelName
				+ ", channelDefinition=" + channelDefinition
				+ ", domainDefinition=" + domainDefinition + "]";
	}

	/**
	 * Does the actual processing, load from database and adding to
	 * ElasticSearch.
	 */
	public void execute() {

		// Init variables
		DatasourceDefinition dataSourceDef = configManager
				.getDatasourceDef(channelDefinition.getDatasourceName());
		SqlTemplates sqlTemplates = configManager.getSqlTemplates(dataSourceDef
				.getJdbcDriverClassName());

		// TODO retrieve value from ElasticSearch
		Object lastReferenceValue = null;

		// 1. Create and initialize the select builder and row handler - only
		// first time
		SelectBuilder selectBuilder = domainDefinition
				.buildSelect(new SelectBuilder(sqlTemplates));
		if (lastReferenceValue != null) {
			domainDefinition.addRefFilter(selectBuilder);
		}
		DomainRowCallbackHandler domainRowHandler = new DomainRowCallbackHandler(
				this);

		// JDBC template and parameters
		NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(
				dataSourceDef.getDataSource());
		Map<String, Object> jdbcParamsMap = new HashMap<String, Object>();
		jdbcParamsMap.put(SelectBuilder.P_LIMIT, batchSize);
		boolean done = false;
		int batchCount = 0;

		while (!done) {
			++batchCount;
			// TODO log
			long startTime = System.currentTimeMillis();
			System.out.println("Channel " + channelName + ": start batch "
					+ batchCount);
			// a. add conditional placeholder values
			if (lastReferenceValue != null) {
				jdbcParamsMap
						.put(SelectBuilder.P_REF_VALUE, lastReferenceValue);
			}
			// b. Execute the actual query
			domainRowHandler.reset();
			jdbcTemplate.query(selectBuilder.buildSelectQuery(), jdbcParamsMap,
					domainRowHandler);
			// Add reference filter to SQL in preparation for next run
			if (lastReferenceValue == null) {
				domainDefinition.addRefFilter(selectBuilder);
			}
			System.out.println("Channel " + channelName + ": batch "
					+ batchCount + " took "
					+ (System.currentTimeMillis() - startTime)
					+ " ms to bring " + domainRowHandler.getBatchRecordCount()
					+ " records");

			// c. Retrieve domain objects
			LinkedHashMap<String, Map<String, Object>> domainObjectMap = domainRowHandler
					.getDomainObjectMap();
			if (domainObjectMap.isEmpty()) {
				done = true;
			} else {
				done = (domainRowHandler.getBatchRecordCount() < batchSize || domainRowHandler
						.getRowCount() >= maxRecords);
				// Store domain object into ES
				lastReferenceValue = storeValues(domainObjectMap, batchCount, done);
			}
		}
	}

	/**
	 * Persist values into the ES cluster
	 * 
	 * @param domainObjectMap
	 *            the values to be stored
	 * @param lastBatch
	 *            whether this is the last batch
	 * @return
	 */
	private Object storeValues(
			LinkedHashMap<String, Map<String, Object>> domainObjectMap, int batchCount,
			boolean lastBatch) {
		// If not the last batch, do not insert the last domain object
		// as it could
		// be incomplete. Instead make sure it will be part of the next
		// batch.
		Object lastRefValue = null;
		Map.Entry<String, Map<String, Object>> lastEntry = null;
		Iterator<Map.Entry<String, Map<String, Object>>> it = domainObjectMap
				.entrySet().iterator();
		while (it.hasNext()) {
			lastEntry = it.next();
		}
		lastRefValue = domainDefinition.getRefValue(lastEntry.getValue());

		if (!lastBatch) {
			// Remove the last value from map as the next value will be picked
			// up anyways
			domainObjectMap.remove(lastEntry.getKey());
		}

		long startTime = System.currentTimeMillis();
		// Do persist into ES here
		Client esClient = ESClientManager.get(esClusterName);
		BulkRequestBuilder bulkRequest = esClient.prepareBulk();

		// either use client#prepare, or use Requests# to directly build
		// index/delete requests
		for (Map.Entry<String, Map<String, Object>> entry : domainObjectMap
				.entrySet()) {
			bulkRequest.add(esClient.prepareIndex(
					channelDefinition.getEsIndex(),
					channelDefinition.getEsType(), entry.getKey()).setSource(
					entry.getValue()));
		}

		BulkResponse bulkResponse = bulkRequest.execute().actionGet();
		
		// TODO log
		System.out.println("Channel " + channelName + ": batch "
				+ batchCount + " took "
				+ (System.currentTimeMillis() - startTime)
				+ " ms to insert " + domainObjectMap.size()
				+ " domain objects; last ref=" + lastRefValue + " last ID=" + lastEntry.getKey());
		if (bulkResponse.hasFailures()) {
			// process failures by iterating through each bulk response item
			System.out.println(bulkResponse.buildFailureMessage());
		}

		// Update the E Channel values (in ES)

		return lastRefValue;
	}

}
