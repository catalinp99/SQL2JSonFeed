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

import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
		TypeDefinition typeDefinition = parseTypeDefinition(yamlFilePath);
		System.out.println(typeDefinition);

		// 2. Create main SQL
		SelectBuilder selectBuilder = createSelectForType(typeDefinition);
		System.out.println(selectBuilder.buildSelectQuery());
		
		// 3. Execute SQL and add ES object
		loadFromDatabase(selectBuilder, typeDefinition, new DomainConfig(typeDefinition.getMainDomain()));
	}

	private static TypeDefinition parseTypeDefinition(String yamlFilePath)
			throws JsonParseException, JsonMappingException,
			FileNotFoundException, IOException {
		// 1. Read configuration
		Map<String, TableDefinition> tableMap = mapper.readValue(
				new FileReader(yamlFilePath),
				new TypeReference<LinkedHashMap<String, TableDefinition>>() {
				});

		TypeDefinition typeDefinition = new TypeDefinition(tableMap);
		return typeDefinition;
	}
	
	private static SelectBuilder createSelectForType(TypeDefinition typeDefinition) {
		SelectBuilder selectBuilder = typeDefinition.buildSelect(new SelectBuilder());
		return selectBuilder;
	}

	private static void loadFromDatabase(SelectBuilder selectBuilder,
			TypeDefinition typeDefinition, DomainConfig domainConfig) {
		DataSource dataSource = null;
		NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(domainConfig.getDataSource());
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		if (domainConfig.getLimit() > 0) {
			paramsMap.put("limit", domainConfig.getLimit());
		}
		// TODO add reference filter to SQL
		
		jdbcTemplate.query(selectBuilder.buildSelectQuery(), paramsMap, new DomainRowHandler(domainConfig, typeDefinition));
	}
}
