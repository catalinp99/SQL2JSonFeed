package com.sql2jsonfeed.channel;

import com.sql2jsonfeed.definition.ChannelDefinition;
import com.sql2jsonfeed.definition.DomainDefinition;
import com.sql2jsonfeed.definition.FieldDefinition;
import com.sql2jsonfeed.definition.TypeUpdateDefinition;
import com.sql2jsonfeed.sql.SelectBuilder;
import com.sql2jsonfeed.util.Conversions;
import org.springframework.jdbc.core.RowCountCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChannelSqlFetcher extends RowCountCallbackHandler {

    private ChannelManager channelManager = null;
    private SelectBuilder selectBuilder = null;
    private NamedParameterJdbcTemplate jdbcTemplate = null;
    private final int batchSize;
    private final int maxSize;
    private final TypeUpdateDefinition typeUpdateDefinition;
    Map<String, Object> jdbcParamsMap = new HashMap<String, Object>();

	// Batch record count
	private int batchRecordCount = 0;

	// Internal value map: ID to name-value pairs (JSon object)
	private LinkedHashMap<String, Map<String, Object>> domainObjectMap = new LinkedHashMap<String, Map<String, Object>>();

    private int batchCount = 0;

	public ChannelSqlFetcher(ChannelManager channelManager, SelectBuilder selectBuilder, NamedParameterJdbcTemplate
            jdbcTemplate, int batchSize, int maxSize, TypeUpdateDefinition typeUpdateDefinition) {
		super();
		this.channelManager = channelManager;
        this.selectBuilder = selectBuilder;
        this.jdbcTemplate = jdbcTemplate;
        this.batchSize = batchSize;
        this.maxSize = maxSize;
        this.typeUpdateDefinition = typeUpdateDefinition;
        jdbcParamsMap.put(SelectBuilder.P_LIMIT, batchSize);
	}

    public TypeUpdateDefinition getTypeUpdateDefinition() {
        return typeUpdateDefinition;
    }

    /**
     * Bring a new batch of data from database
     */
    public boolean processNewBatch(Object lastReferenceValue, Object lastUpdateReferenceValue) {
        ++batchCount;
        boolean done = false;
        ChannelDefinition channelDefinition = channelManager.getChannelDefinition();
        String channelName = channelManager.getChannelName();
        DomainDefinition domainDefinition = channelManager.getDomainDefinition();

        if (lastReferenceValue != null) {
            jdbcParamsMap.put(SelectBuilder.P_REF_VALUE, lastReferenceValue);
        }

        // TODO log
        long startTime = System.currentTimeMillis();
//        System.out.println("Channel " + channelName + ": start batch "
//                + batchCount + " from " + lastReferenceValue);
        // a. add conditional placeholder values
        if (lastReferenceValue != null) {
            Object lastRefValueSql = Conversions.localToSqlValue(lastReferenceValue, domainDefinition.getRefFieldDef(), channelDefinition.getDbTimeZone());
            System.out.println("Channel " + channelName + " batch " + batchCount + " - SQL ref value:" + lastRefValueSql);
            jdbcParamsMap
                    .put(SelectBuilder.P_REF_VALUE, lastRefValueSql);
        }
        if (lastUpdateReferenceValue != null) {
            // This is for update
            assert(typeUpdateDefinition != null);
            Object lastRefUpdateValueSql = Conversions.localToSqlValue(lastUpdateReferenceValue, domainDefinition
                    .getRefFieldDef(), channelDefinition.getDbTimeZone());
            System.out.println("Channel " + channelName + " update " + typeUpdateDefinition.getName() + " batch " +
                    batchCount + " - SQL update ref " + "value:" + lastRefUpdateValueSql);
            jdbcParamsMap
                    .put(SelectBuilder.P_REF_UPDATE_VALUE, lastRefUpdateValueSql);
        }
        // b. Execute the actual query
        this.reset();
        String query = selectBuilder.buildSelectQuery();
        System.out.println(query);
        System.out.println(jdbcParamsMap);
        jdbcTemplate.query(query, jdbcParamsMap, this);
        // Add reference filter to SQL in preparation for next run
        if (lastReferenceValue == null) {
            domainDefinition.addRefFilter(selectBuilder);
        }
        System.out.println("Channel " + channelName + ": batch "
                + batchCount + " took "
                + (System.currentTimeMillis() - startTime)
                + " ms to bring " + getBatchRecordCount()
                + " records");

        // c. Figure out whether it is done or not
        done = (domainObjectMap.isEmpty() || batchRecordCount < batchSize || (maxSize > 0 && getRowCount() >= maxSize));
        return !done;
    }
	
	public void reset() {
		batchRecordCount = 0;
	}

	public LinkedHashMap<String, Map<String, Object>> getDomainObjectMap() {
		return domainObjectMap;
	}

	/* 
	 * 
	 * @param rs ResultSet to extract data from. This method is
	 * invoked for each row
	 * @param rowNum number of the current row (starting from 0)
	 */
	@Override
	protected void processRow(ResultSet rs, int rowNum) throws SQLException {
		
		if (batchRecordCount == 0) {
			// prepare for next batch
			domainObjectMap.clear();
		}

		DomainDefinition domainDefinition = channelManager.getDomainDefinition();
		
		// 1. Extract values -> each type definition has to extract each own values, as HashMap;
		// including the ID
		Map<String, Map<String, Object>> rowValues = domainDefinition.extractRow(rs, rowNum, channelManager.getChannelDefinition().getDbTimeZone());
//		System.out.println(rowNum + ": " + rowValues);
		
		// 2. Merge values into the existing map
		String rootId = domainDefinition.getRootId(rowValues);
		Map<String, Object> rootValues = domainObjectMap.get(rootId);
//		System.out.println(rowNum + ": " + rootValues);
//		System.out.println(rowNum + ": " + valuesMapList);
		rootValues = domainDefinition.mergeValues(rootValues, rowValues);
//		System.out.println(rowNum + ": " + rootValues);
//		System.out.println(rowNum + ": " + valuesMapList);
		domainObjectMap.put(rootId, rootValues);
//		System.out.println(rowNum + ": " + valuesMapList);
		
		++batchRecordCount;
	}

	public int getBatchRecordCount() {
		return batchRecordCount;
	}

    public int getBatchCount() {
        return batchCount;
    }
}
