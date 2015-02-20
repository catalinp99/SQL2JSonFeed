package com.sql2jsonfeed;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sql2jsonfeed.definition.ObjectDefinition;

/**
 * Hello world!
 *
 */
public class App {
	
	private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
	
	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		final String yamlFile = "C:\\Users\\Catalin\\SkyDrive\\Documents\\ES\\test.yaml";
		
		@SuppressWarnings("unchecked")
//		Map<String, Map<String, ObjectDefinition>> yamlConfiguration = mapper.readValue(new FileReader(yamlFile), Map.class);
		
		Map<String, ObjectDefinition> yamlConfiguration = mapper.readValue(new FileReader(yamlFile), new TypeReference<Map<String, ObjectDefinition>>() {});
		
//		List<Object> yamlConfiguration = mapper.readValue(new FileReader(yamlFile), List.class);
		
		System.out.println(yamlConfiguration);
	}
}
