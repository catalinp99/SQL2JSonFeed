package com.sql2jsonfeed.channel;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sql2jsonfeed.definition.*;
import com.sql2jsonfeed.util.Conversions;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sql2jsonfeed.ESClientManager;
import com.sql2jsonfeed.config.ChannelConfigData;
import com.sql2jsonfeed.config.ConfigManager;
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

    // Automatic field to be added to any domain object
    public static final String FIELD_CHANNEL = "channel_";

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
        assert (StringUtils.isNotEmpty(esClusterName));
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

    /**
     * Does the actual processing, load from database and adding to
     * ElasticSearch.
     */
    public void execute() {

        // Init variables
        // JDBC DATA SOURCE
        DatasourceDefinition dataSourceDef = configManager
                .getDatasourceDef(channelDefinition.getDatasourceName());
        // SQL TEMPLATES (per jdbc driver)
        SqlTemplates sqlTemplates = configManager.getSqlTemplates(dataSourceDef
                .getJdbcDriverClassName());
        // Previously persisted config data
        ChannelConfigData configData = lookupConfigData();
        //
        FieldDefinition refFieldDef = domainDefinition.getRefFieldDef();
        // Retrieve lastReferenceValue from ES index directly
        Object lastReferenceValue = null;
        boolean veryFirstTime = false;
        if (configData == null) {
            configData = new ChannelConfigData(this.channelName);
            veryFirstTime = true;
        } else {
            lastReferenceValue = getChannelMaxReferenceValue();
        }
        // TODO Compare domain and channel def???
        configData.setChannelDef(channelDefinition);

        // TODO remove - this is for test only
        // setConfigDataMapping();

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
                    + batchCount + " from " + lastReferenceValue);
            // a. add conditional placeholder values
            if (lastReferenceValue != null) {
                Object lastRefValueSql = Conversions.localToSqlValue(lastReferenceValue, refFieldDef, channelDefinition.getDbTimeZone());
                System.out.println("Channel " + channelName + " batch " + batchCount + " - SQL ref value:" + lastRefValueSql);
                jdbcParamsMap
                        .put(SelectBuilder.P_REF_VALUE, lastRefValueSql);
            }
            // b. Execute the actual query
            domainRowHandler.reset();
            String query = selectBuilder.buildSelectQuery();
            System.out.println(query);
            System.out.println(jdbcParamsMap);
            jdbcTemplate.query(query, jdbcParamsMap, domainRowHandler);
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
                lastReferenceValue = storeValues(domainObjectMap, batchCount,
                        done);
                if (refFieldDef != null) {
                    configData.setLastRefValue(refFieldDef.getFieldName(),
                            lastReferenceValue);
                }
                configData.setLastExecutionDate(new Date());
                persistConfigData(configData, veryFirstTime);
                veryFirstTime = false;
            }
        }
    }

    /**
     * Lookup in the existing index the maximum value of the reference field
     *
     * @return max value or null if there is no reference field
     */
    private Object getChannelMaxReferenceValue() {
        FieldDefinition refFieldDef = domainDefinition.getRefFieldDef();
        if (refFieldDef == null) {
            // No reference field, return null
            return null;
        }
        Client esClient = ESClientManager.get(esClusterName);
        // Create search request
        final String maxRef = "maxRef";

        SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch(/*index*/ channelDefinition.getEsIndex());
        searchRequestBuilder.setTypes(channelDefinition.getEsType());
        // REPLACE WITH TERM
        searchRequestBuilder.setQuery(QueryBuilders.matchQuery(ChannelManager.FIELD_CHANNEL, channelDefinition.getName()));
        searchRequestBuilder.addAggregation(AggregationBuilders.max(maxRef).field(
                refFieldDef.getFieldName()));
        searchRequestBuilder.setSize(0);

        SearchResponse searchResponse = null;

        try {
            searchResponse = searchRequestBuilder.execute().actionGet();
        } catch (IndexMissingException ime) {
            return null; // not created yet
        } catch (RuntimeException re) {
            // TODO remove
            System.err.println(re);
            throw re;
        }

        System.out.println("Total records before this batch:" + searchResponse.getHits().totalHits());

        if (searchResponse.getHits().totalHits() == 0) {
            return null; // first time
        }
        Max maxAgg = searchResponse.getAggregations().get(maxRef);
        Object maxValue = Conversions.fromEsValue(maxAgg.getValue(), refFieldDef.getFieldType(), null);

        // How to cast to a date
        System.out.println("Max ref value before this batch:" + maxAgg.getValue() + " ---> " + maxValue);

        return maxValue;
    }

    /**
     * Persist values into the ES cluster
     *
     * @param domainObjectMap the values to be stored
     * @param lastBatch       whether this is the last batch
     * @return
     */
    private Object storeValues(
            LinkedHashMap<String, Map<String, Object>> domainObjectMap,
            int batchCount, boolean lastBatch) {
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
            // Set the unconditional 'channel' field
            lastEntry.getValue().put(FIELD_CHANNEL, channelName);
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
        System.out.println("Channel " + channelName + ": batch " + batchCount
                + " took " + (System.currentTimeMillis() - startTime)
                + " ms to insert " + domainObjectMap.size()
                + " domain objects; last ref=" + lastRefValue + " last ID="
                + lastEntry.getKey());
        if (bulkResponse.hasFailures()) {
            // process failures by iterating through each bulk response item
            System.out.println(bulkResponse.buildFailureMessage());
        }

        // Update the ES Channel values (in ES)

        return lastRefValue;
    }

    private static ObjectMapper mapper = new ObjectMapper();

    /**
     * @return previously persisted channel config data.
     */
    private ChannelConfigData lookupConfigData() {
        Client esClient = ESClientManager.get(esClusterName);
        GetRequestBuilder getRequestBuilder = esClient.prepareGet(
                ConfigManager.ES_CONFIG_INDEX, ConfigManager.ES_CONFIG_TYPE,
                channelName);

        GetResponse getResponse = null;

        try {
            getResponse = getRequestBuilder.get();
        } catch (IndexMissingException ime) {
            return null; // not created yet
        }
        if (!getResponse.isExists()) {
            return null;
        }

        // Convert ...
        ChannelConfigData configData = ChannelConfigData.fromMap(getResponse
                .getSource(), domainDefinition);

        return configData;
    }

    private void setConfigDataMapping() {
        // TODO Why is it not working??
        // String refEsType = "null";
        // FieldType refFieldType = domainDefinition.getRefFieldType();
        // if (refFieldType != null) {
        // refEsType = refFieldType.toEsType();
        // }
        //
        // Client esClient = ESClientManager.get(esClusterName);
        // try {
        // // TODO is it necessary?
        // esClient.admin()
        // .indices()
        // .create(new CreateIndexRequest(
        // ConfigManager.ES_CONFIG_INDEX)).actionGet();
        // } catch (IndexAlreadyExistsException iaee) {
        // // just ignore
        // }
        //
        // try {
        // XContentBuilder xcontentBuilder = XContentFactory.jsonBuilder()
        // .startObject().startObject(channelName)
        // .startObject("properties").startObject("lastExecutionTime")
        // .field("type", "date").endObject()
        // .startObject("lastRefValue").field("type", refEsType)
        // .endObject().endObject().endObject().endObject();
        //
        // PutMappingResponse putMappingResponse = esClient
        // .admin().indices()
        // .preparePutMapping(ConfigManager.ES_CONFIG_INDEX)
        // .setType(channelName)
        // .setSource(xcontentBuilder)
        // .execute().actionGet();
        // } catch (IOException e) {
        // // TODO handle exception
        // System.err.println(e);
        // }
    }

    /**
     * Persist the current config data for future use.
     */
    private void persistConfigData(ChannelConfigData configData,
                                   boolean updateMapping) {
        // First set the mapping
        if (updateMapping) {
            setConfigDataMapping();
        }

        // Then update data
        Client esClient = ESClientManager.get(esClusterName);
        IndexRequestBuilder indexRequestBuilder = esClient.prepareIndex(
                ConfigManager.ES_CONFIG_INDEX, ConfigManager.ES_CONFIG_TYPE,
                channelName);

        indexRequestBuilder.setSource(configData.toMap());

        IndexResponse indexResponse = indexRequestBuilder.execute().actionGet();
        System.out.println("Config data created (updated=false): "
                + indexResponse.isCreated());
    }

    @Override
    public String toString() {
        return "ChannelManager [channelName=" + channelName
                + ", channelDefinition=" + channelDefinition
                + ", domainDefinition=" + domainDefinition + "]";
    }
}
