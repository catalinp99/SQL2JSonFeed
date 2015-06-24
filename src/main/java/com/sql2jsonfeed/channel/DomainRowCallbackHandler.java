package com.sql2jsonfeed.channel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

import com.sql2jsonfeed.definition.ChannelDefinition;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.RowCountCallbackHandler;

import com.sql2jsonfeed.definition.DomainDefinition;

public class DomainRowCallbackHandler extends RowCountCallbackHandler {

	private ChannelManager channelManager = null;

	// Batch record count
	private int batchRecordCount = 0;
	
	// Internal value map: ID to name-value pairs (JSon object)
	private LinkedHashMap<String, Map<String, Object>> domainObjectMap = new LinkedHashMap<String, Map<String, Object>>();

	public DomainRowCallbackHandler(ChannelManager channelManager) {
		super();
		this.channelManager = channelManager;
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
}
