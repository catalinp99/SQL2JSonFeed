package com.sql2jsonfeed;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sql2jsonfeed.definition.TableDefinition;
import com.sql2jsonfeed.definition.TypeDefinition;

/**
 * Hello world!
 *
 */
public class App {

	private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

	public static void main(String[] args) throws JsonParseException,
			JsonMappingException, IOException {
		final String yamlFilePath = "src\\test\\order.yaml";

		// 1. Read configuration
		TypeDefinition typeDefinition = parseTypeDefinition(yamlFilePath);

		System.out.println(typeDefinition);
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
}
