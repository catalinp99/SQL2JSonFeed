package com.sql2jsonfeed.definition;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sql2jsonfeed.sql.SelectBuilder;

/**
 * @author Catalin
 *
 */
public class DomainDefinition {

	private LinkedHashMap<String, TypeDefinition> typesMap = null;
	private TypeDefinition rootTypeDef = null;

	public DomainDefinition(LinkedHashMap<String, TypeDefinition> typesMap) {
		this.typesMap = typesMap;
		
		init();
	}

	private void init() {
		assert(typesMap != null && !typesMap.isEmpty());
		
		// TODO validate
		
		// Init types
		for (Map.Entry<String, TypeDefinition> entry: typesMap.entrySet()) {
			TypeDefinition typeDefinition = entry.getValue(); 
			typeDefinition.init(entry.getKey());
			String parentKey = typeDefinition.getParentTypeName();
			// Add definition to the parent
			if (parentKey != null) {
				TypeDefinition parentTypeDefinition = typesMap.get(parentKey);
				if (parentTypeDefinition == null) {
					// TODO dedicated exception
					throw new RuntimeException("Missing parent: " + parentKey + " for type:" + entry.getKey());
				}
				parentTypeDefinition.addChildTypeDef(typeDefinition);
			} else {
				if (rootTypeDef != null) {
					// TODO dedicated exception
					throw new RuntimeException("Two top level definitions: " + rootTypeDef.getTypeName() + " and " + typeDefinition.getTypeName());
				}
				rootTypeDef = typeDefinition;
			}
		}
		
		if (rootTypeDef == null) {
			throw new RuntimeException("Missing root type");
		}
		
		// TODO check main type required fields
	}
	
	
	/**
	 * Merge the values from a single row into the main object (if any) or create a new object
	 * @param rootValues
	 * @param rowValues
	 */
	public Map<String, Object> mergeValues(Map<String, Object> rootValues, Map<String, Map<String, Object>> rowValues) {
		assert(rootTypeDef != null);
		Map<String, Object> rootRowValues = rowValues.get(rootTypeDef.getTypeName());
		if (rootValues == null) {
			rootValues = rootRowValues;
		} else {
			// TODO verify all values are the same
			assert(rootValues.get(rootTypeDef.getIdFieldKey()).equals(rootRowValues.get(rootTypeDef.getIdFieldKey())));
		}
		List<TypeDefinition> childTypeList = rootTypeDef.getChildTypes();
		if (childTypeList == null) {
			return rootValues;
		}
		for (TypeDefinition childType: childTypeList) {
			mergeValuesToParent(rootValues, childType, rowValues);
		}
		
		return rootValues;
	}
	
	/**
	 * Recursive function to merge the values to the parent
	 * 
	 * @param parentValues
	 * @param childType
	 * @param rowValues
	 */
	private void mergeValuesToParent(Map<String, Object> parentValues,
			TypeDefinition childType, Map<String, Map<String, Object>> rowValues) {
		
		Map<String, Object> thisValues = rowValues.get(childType.getTypeName());
		if(thisValues == null) {
			thisValues = new LinkedHashMap<String, Object>();
		}
		
		Map<String, Object> childValues = null;
		
		if (childType.getParentRelation() == MultiplicityEnum.ONE) {
			childValues = (Map<String, Object>)parentValues.get(childType.getParentFieldName());
			if (childValues == null) {
				childValues = thisValues;
				parentValues.put(childType.getParentFieldName(), childValues);
			} else {
				// TODO verify they have the same ID and/or values
				// nothing to do
			}
		} else { // MultiplicityEnum.MANY
			List<Map<String, Object>> childValuesList = (List<Map<String, Object>>)parentValues.get(childType.getParentFieldName());
			if (childValuesList == null) { // First time
				childValuesList = new ArrayList<Map<String, Object>>();
				childValues = thisValues;
				childValuesList.add(childValues);
				parentValues.put(childType.getParentFieldName(), childValuesList);
			} else {
				// Add it only if not already there
				String idFieldName = childType.getIdFieldKey();
				if (idFieldName != null) {
					Object idValue = thisValues.get(idFieldName);
					assert(idValue != null);
					for (Map<String, Object> childValuesObject: childValuesList) {
						if (idValue.equals(childValuesObject.get(idFieldName))) {
							childValues = childValuesObject; // already there
							break;
						}
					}
				}
				if (childValues == null) {
					childValues = thisValues;
					childValuesList.add(childValues);
				}
			}
		}
		
		// Process children
		List<TypeDefinition> grandChildTypeList = childType.getChildTypes();
		if (grandChildTypeList != null) {
			for (TypeDefinition grandChildType: grandChildTypeList) {
				mergeValuesToParent(childValues, grandChildType, rowValues);
			}
		}
	}

	/**
	 * @param rowValues
	 * @return The ID of the actual object
	 */
	public Object getRootId(Map<String, Map<String, Object>> rowValues) {
		assert(rootTypeDef != null);
		Map<String, Object> rootValues = rowValues.get(rootTypeDef.getTypeName());
		return rootValues.get(rootTypeDef.getIdFieldKey());
	}
	
	public TypeDefinition getRootTypeDef() {
		return rootTypeDef;
	}

	/**
	 * Build a select statement from the table definitions
	 * @param selectBuilder
	 */
	public SelectBuilder buildSelect(SelectBuilder selectBuilder, boolean withRefValue) {
		assert(selectBuilder != null);
		
		for (TypeDefinition typeDef: typesMap.values()) {
			typeDef.addToSelectBuilder(selectBuilder, withRefValue);
		}
		return selectBuilder;
	}

	public Map<String, Map<String, Object>> extractRow(ResultSet rs, int rowNum) throws SQLException {
		HashMap<String, Map<String, Object>> rowValues = new HashMap<String, Map<String, Object>>();
		
		for (Map.Entry<String, TypeDefinition> entry: typesMap.entrySet()) {
			Map<String, Object> typeRowValues = entry.getValue().extractRow(rs, rowNum);
			rowValues.put(entry.getKey(), typeRowValues);
		}
		return rowValues;
	}
	
}
