package com.sql2jsonfeed;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sql2jsonfeed.definition.DomainDefinition;
import com.sql2jsonfeed.definition.TableDefinition;
import com.sql2jsonfeed.definition.TypeDefinition;
import com.sql2jsonfeed.sql.SelectBuilder;

/**
 * Hello world!
 *
 */
public class App {

	private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

	public static void main(String[] args) throws JsonParseException,
			JsonMappingException, IOException {
		final String yamlFilePath = "src\\main\\resources\\order.yaml";

		// 1. Read configuration
		DomainDefinition domainDefinition = parseDomainDefinition(yamlFilePath);
		System.out.println(domainDefinition);

		// 2. Create main SQL
		SelectBuilder selectBuilder = createSelectForDomain(domainDefinition);
		System.out.println(selectBuilder.buildSelectQuery());

		// 3. Execute SQL and add ES object
		DomainConfig domainConfig = new DomainConfig("order");
		Map<Object, Map<String, Object>> values = loadFromDatabase(
				selectBuilder, domainDefinition, domainConfig);

		// 4. Persist into Elastic Search
		storeToES(values, domainConfig);
	}

	private static DomainDefinition parseDomainDefinition(String yamlFilePath)
			throws JsonParseException, JsonMappingException,
			FileNotFoundException, IOException {
		// 1. Read configuration
		LinkedHashMap<String, TypeDefinition> typesMap = mapper.readValue(
				new FileReader(yamlFilePath),
				new TypeReference<LinkedHashMap<String, TypeDefinition>>() {
				});

		DomainDefinition domainDefinition = new DomainDefinition(typesMap);
		return domainDefinition;
	}

	private static SelectBuilder createSelectForDomain(
			DomainDefinition domainDefinition) {
		SelectBuilder selectBuilder = domainDefinition.buildSelect(
				new SelectBuilder(), false);
		return selectBuilder;
	}

	private static Map<Object, Map<String, Object>> loadFromDatabase(
			SelectBuilder selectBuilder, DomainDefinition domainDefinition,
			DomainConfig domainConfig) {
		NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(
				domainConfig.getDataSource());
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		if (domainConfig.getLimit() > 0) {
			paramsMap.put(SelectBuilder.P_LIMIT, domainConfig.getLimit());
		}
		// TODO add reference filter to SQL

		DomainRowHandler domainRowHandler = new DomainRowHandler(domainConfig,
				domainDefinition);
		jdbcTemplate.query(selectBuilder.buildSelectQuery(), paramsMap,
				domainRowHandler);

		System.out.println(domainRowHandler.getValuesMapList());

		return domainRowHandler.getValuesMapList();
	}

	private static void storeToES(Map<Object, Map<String, Object>> values,
			DomainConfig domainConfig) {
		// TODO Auto-generated method stub
		Node node = NodeBuilder.nodeBuilder().client(true)
				.clusterName(domainConfig.getESClusterName()).node();

		try {
			Client client = node.client();

			BulkRequestBuilder bulkRequest = client.prepareBulk();

			// either use client#prepare, or use Requests# to directly build
			// index/delete requests
			// TODO id must be a String
			for (Map.Entry<Object, Map<String, Object>> entry : values
					.entrySet()) {
				//
				bulkRequest.add(client.prepareIndex(domainConfig.getESIndex(),
						domainConfig.getESType(), entry.getKey().toString())
						.setSource(entry.getValue()));
			}

			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			System.out.println(bulkResponse);
			if (bulkResponse.hasFailures()) {
				// process failures by iterating through each bulk response item
				System.out.println(bulkResponse.buildFailureMessage());
			}
		} finally {
			node.close();
		}
	}
}
