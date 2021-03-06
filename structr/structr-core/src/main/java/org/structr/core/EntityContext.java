/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core;

import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.entity.DirectedRelationship;
import org.structr.core.entity.DirectedRelationship.Cardinality;

/**
 * A global context for functional mappings between nodes and
 * relationships, property views and property validators.
 *
 * @author Christian Morgner
 */
public class EntityContext {

	private static final Map<Class, Map<String, Class<? extends PropertyConverter>>> globalPropertyConverterMap = new LinkedHashMap<Class, Map<String, Class<? extends PropertyConverter>>>();
	private static final Map<Class, Map<String, Class<? extends PropertyValidator>>> globalValidatorMap = new LinkedHashMap<Class, Map<String, Class<? extends PropertyValidator>>>();
	private static final Map<String, Map<String, DirectedRelationship>> globalRelationshipMap = new LinkedHashMap<String, Map<String, DirectedRelationship>>();
	private static final Map<Class, Map<PropertyView, Set<String>>> globalStringMap = new LinkedHashMap<Class, Map<PropertyView, Set<String>>>();
	private static final Map<Class, Map<String, PropertyGroup>> globalPropertyGroupMap = new LinkedHashMap<Class, Map<String, PropertyGroup>>();
	private static final Map<Class, Map<String, Value>> globalValidationParameterMap = new LinkedHashMap<Class, Map<String, Value>>();
	private static final Map<Class, Map<String, Value>> globalConversionParameterMap = new LinkedHashMap<Class, Map<String, Value>>();
	private static final Map<Class, Set<String>> globalReadOnlyPropertyMap = new LinkedHashMap<Class, Set<String>>();

	private static final Logger logger = Logger.getLogger(EntityContext.class.getName());

	// ----- property notions -----
	public static PropertyGroup getPropertyGroup(Class type, PropertyKey key) {
		return getPropertyGroup(type, key.name());
	}

	public static PropertyGroup getPropertyGroup(Class type, String key) {
		return getPropertyGroupMapForType(type).get(key);
	}

	public static void registerPropertyGroup(Class type, PropertyKey key, PropertyGroup propertyGroup) {
		getPropertyGroupMapForType(type).put(key.name(), propertyGroup);
	}

	// ----- static relationship methods -----
	public static DirectedRelationship getRelation(String sourceType, String destType) {
		return getRelationshipMapForType(sourceType.toLowerCase()).get(destType.toLowerCase());
	}

	public static DirectedRelationship getRelation(Class sourceType, Class destType) {
		return getRelationshipMapForType(convertName(sourceType)).get(convertName(destType));
	}

	/**
	 * Defines a static relationship between <code>sourceType</code> and <code>destType</code>
	 * with the given relationship type, direction and the default cardinality of <code>ManyToMany<code>.
	 *
	 * @param sourceType
	 * @param destType
	 * @param relType
	 * @param direction
	 */
	public static void registerRelation(Class sourceType, Class destType, RelationshipType relType, Direction direction) {
		registerRelation(sourceType, destType, relType, direction, Cardinality.ManyToMany);
	}

	/**
	 * Defines a static relationship between <code>sourceType</code> and <code>destType</code>
	 * with the given relationship type, direction, cardinality and the default notion which
	 * returns the source object unmodified.
	 *
	 * @param sourceType
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param cardinality
	 */
	public static void registerRelation(Class sourceType, Class destType, RelationshipType relType, Direction direction, Cardinality cardinality) {
		
		// need to set type here
		Notion objectNotion = new ObjectNotion();
		objectNotion.setType(destType);

		registerRelation(convertName(sourceType), convertName(destType), relType, direction, cardinality, objectNotion);
	}

	/**
	 * Defines a static relationship between <code>sourceType</code> and <code>destType</code>
	 * with the given relationship type, direction and cardinality.
	 *
	 * @param sourceType
	 * @param destType
	 * @param relType
	 * @param direction
	 * @param cardinality
	 * @param notion
	 */
	public static void registerRelation(Class sourceType, Class destType, RelationshipType relType, Direction direction, Cardinality cardinality, Notion notion) {
		registerRelation(convertName(sourceType), convertName(destType), relType, direction, cardinality, notion);
		notion.setType(destType);
	}

	public static Map<String, DirectedRelationship> getRelations(Class sourceType) {
		return getRelationshipMapForType(convertName(sourceType));
	}

	public static Map<String, DirectedRelationship> getRelations(String sourceType) {
		return getRelationshipMapForType(sourceType);
	}

	// ----- property set methods -----
	public static void registerPropertySet(Class type, PropertyView propertyView, PropertyKey... propertySet) {

		Set<String> properties = getPropertySet(type, propertyView);
		for(PropertyKey property : propertySet) {
			properties.add(property.name());
		}

		// include property sets from superclass
		Class superClass = type.getSuperclass();
		while(superClass != null && !superClass.equals(Object.class)) {
			
			Set<String> superProperties = getPropertySet(superClass, propertyView);
			properties.addAll(superProperties);

			// one level up :)
			superClass = superClass.getSuperclass();
		}
	}

	public static Set<String> getPropertySet(Class type, PropertyView propertyView) {

		Map<PropertyView, Set<String>> propertyViewMap = getPropertyViewMapForType(type);
		Set<String> propertySet = propertyViewMap.get(propertyView);

		if(propertySet == null) {
			propertySet = new LinkedHashSet<String>();
			propertyViewMap.put(propertyView, propertySet);
		}

		return propertySet;
	}

	// ----- validator methods -----
	public static void registerPropertyValidator(Class type, PropertyKey propertyKey, Class<? extends PropertyValidator> validatorClass) {
		registerPropertyValidator(type, propertyKey.name(), validatorClass);
	}

	public static void registerPropertyValidator(Class type, String propertyKey, Class<? extends PropertyValidator> validatorClass) {
		registerPropertyValidator(type, propertyKey, validatorClass, null);
	}

	public static void registerPropertyValidator(Class type, PropertyKey propertyKey, Class<? extends PropertyValidator> validatorClass, Value parameter) {
		registerPropertyValidator(type, propertyKey.name(), validatorClass, parameter);
	}

	public static void registerPropertyValidator(Class type, String propertyKey, Class<? extends PropertyValidator> validatorClass, Value parameter) {

		Map<String, Class<? extends PropertyValidator>> validatorMap = getPropertyValidatorMapForType(type);
		validatorMap.put(propertyKey, validatorClass);

		if(parameter != null) {
			Map<String, Value> validatorParameterMap = getPropertyValidatonParameterMapForType(type);
			validatorParameterMap.put(propertyKey, parameter);
		}
	}

	public static PropertyValidator getPropertyValidator(final SecurityContext securityContext, Class type, String propertyKey) {

		Map<String, Class<? extends PropertyValidator>> validatorMap = null;
		PropertyValidator validator = null;
		Class localType = type;
		Class clazz = null;
		
		// try all superclasses
		while(clazz == null && !localType.equals(Object.class)) {
			validatorMap = getPropertyValidatorMapForType(localType);
			clazz = validatorMap.get(propertyKey);
			
//			logger.log(Level.INFO, "Validator class {0} found for type {1}", new Object[] { clazz != null ? clazz.getSimpleName() : "null", localType } );

			// one level up :)
			localType = localType.getSuperclass();
		}

		if(clazz != null) {

			try {
				validator = (PropertyValidator)clazz.newInstance();
				validator.setSecurityContext(securityContext);

			} catch(Throwable t) {
				logger.log(Level.WARNING, "Unable to instantiate validator {0}: {1}", new Object[] { clazz.getName(), t.getMessage() } );
			}
		}

		return validator;
	}

	public static Value getPropertyValidationParameter(Class type, String propertyKey) {

		Map<String, Value> validationParameterMap = null;
		Class localType = type;
		Value value = null;
		
		while(value == null && !localType.equals(Object.class)) {
			validationParameterMap = getPropertyValidatonParameterMapForType(localType);
			value = validationParameterMap.get(propertyKey);
			
//			logger.log(Level.INFO, "Validation parameter value {0} found for type {1}", new Object[] { value != null ? value.getClass().getSimpleName() : "null", localType } );
			
			localType = localType.getSuperclass();
		}

		return value;
	}

	// ----- PropertyConverter methods -----
	public static void registerPropertyConverter(Class type, PropertyKey propertyKey, Class<? extends PropertyConverter> propertyConverterClass) {
		registerPropertyConverter(type, propertyKey.name(), propertyConverterClass);
	}

	public static void registerPropertyConverter(Class type, String propertyKey, Class<? extends PropertyConverter> propertyConverterClass) {
		registerPropertyConverter(type, propertyKey, propertyConverterClass, null);
	}
	
	public static void registerPropertyConverter(Class type, String propertyKey, Class<? extends PropertyConverter> propertyConverterClass, Value value) {
		getPropertyConverterMapForType(type).put(propertyKey, propertyConverterClass);
		
		if(value != null) {
			getPropertyConversionParameterMapForType(type).put(propertyKey, value);
		}
	}

	public static PropertyConverter getPropertyConverter(final SecurityContext securityContext, Class type, String propertyKey) {

		Map<String, Class<? extends PropertyConverter>> converterMap = null;
		PropertyConverter propertyConverter = null;
		Class localType = type;
		Class clazz = null;
		
		while(clazz == null && !localType.equals(Object.class)) {
			converterMap = getPropertyConverterMapForType(localType);
			clazz = converterMap.get(propertyKey);
			
//			logger.log(Level.INFO, "Converter class {0} found for type {1}", new Object[] { clazz != null ? clazz.getSimpleName() : "null", localType } );
			
			localType = localType.getSuperclass();
		}
		
		if(clazz != null) {

			try {
				propertyConverter = (PropertyConverter)clazz.newInstance();
				propertyConverter.setSecurityContext(securityContext);

			} catch(Throwable t) {
				logger.log(Level.WARNING, "Unable to instantiate property PropertyConverter {0}: {1}", new Object[] { clazz.getName(), t.getMessage() } );
			}
		}

		return propertyConverter;
	}

	public static Value getPropertyConversionParameter(Class type, String propertyKey) {

		Map<String, Value> conversionParameterMap = null;
		Class localType = type;
		Value value = null;
		
		while(value == null && !localType.equals(Object.class)) {
			conversionParameterMap = getPropertyConversionParameterMapForType(localType);
			value = conversionParameterMap.get(propertyKey);
			
//			logger.log(Level.INFO, "Conversion parameter value {0} found for type {1}", new Object[] { value != null ? value.getClass().getSimpleName() : "null", localType } );
			
			localType = localType.getSuperclass();
		}

		return value;
	}

	// ----- read-only property map -----
	public static void registerReadOnlyProperty(Class type, String key) {

		getReadOnlyPropertySetForType(type).add(key);
	}

	public static boolean isReadOnlyProperty(Class type, String key) {
		
		boolean isReadOnly = getReadOnlyPropertySetForType(type).contains(key);
		Class localType = type;

		// try all superclasses
		while(!isReadOnly && !localType.equals(Object.class)) {

			isReadOnly = getReadOnlyPropertySetForType(localType).contains(key);

			// one level up :)
			localType = localType.getSuperclass();
		}

		return isReadOnly;
	}

	// ----- private methods -----
	private static String convertName(Class type) {

		return(type.getSimpleName().toLowerCase());
	}

	private static void registerRelation(String sourceType, String destType, RelationshipType relType, Direction direction, Cardinality cardinality, Notion notion) {

		Map<String, DirectedRelationship> typeMap = getRelationshipMapForType(sourceType);
		typeMap.put(destType, new DirectedRelationship(relType, direction, cardinality, notion));
	}

	private static Map<String, DirectedRelationship> getRelationshipMapForType(String sourceType) {

		Map<String, DirectedRelationship> typeMap = globalRelationshipMap.get(sourceType);
		if(typeMap == null) {
			typeMap = new LinkedHashMap<String, DirectedRelationship>();
			globalRelationshipMap.put(sourceType, typeMap);
		}

		return(typeMap);
	}

	private static Map<PropertyView, Set<String>> getPropertyViewMapForType(Class type) {

		Map<PropertyView, Set<String>> propertyViewMap = globalStringMap.get(type);
		if(propertyViewMap == null) {
			propertyViewMap = new EnumMap<PropertyView, Set<String>>(PropertyView.class);
			globalStringMap.put(type, propertyViewMap);
		}

		return propertyViewMap;
	}

	private static Map<String, Class<? extends PropertyValidator>> getPropertyValidatorMapForType(Class type) {

		Map<String, Class<? extends PropertyValidator>> validatorMap = globalValidatorMap.get(type);
		if(validatorMap == null) {
			validatorMap = new LinkedHashMap<String, Class<? extends PropertyValidator>>();
			globalValidatorMap.put(type, validatorMap);
		}

		return validatorMap;
	}

	private static Map<String, Value> getPropertyValidatonParameterMapForType(Class type) {

		Map<String, Value> validationParameterMap = globalValidationParameterMap.get(type);
		if(validationParameterMap == null) {
			validationParameterMap = new LinkedHashMap<String, Value>();
			globalValidationParameterMap.put(type, validationParameterMap);
		}

		return validationParameterMap;
	}

	private static Map<String, Class<? extends PropertyConverter>> getPropertyConverterMapForType(Class type) {

		Map<String, Class<? extends PropertyConverter>> PropertyConverterMap = globalPropertyConverterMap.get(type);
		if(PropertyConverterMap == null) {
			PropertyConverterMap = new LinkedHashMap<String, Class<? extends PropertyConverter>>();
			globalPropertyConverterMap.put(type, PropertyConverterMap);
		}

		return PropertyConverterMap;
	}

	private static Map<String, Value> getPropertyConversionParameterMapForType(Class type) {

		Map<String, Value> conversionParameterMap = globalConversionParameterMap.get(type);
		if(conversionParameterMap == null) {
			conversionParameterMap = new LinkedHashMap<String, Value>();
			globalConversionParameterMap.put(type, conversionParameterMap);
		}

		return conversionParameterMap;
	}

	private static Set<String> getReadOnlyPropertySetForType(Class type) {

		Set<String> readOnlyPropertySet = globalReadOnlyPropertyMap.get(type);
		if(readOnlyPropertySet == null) {
			readOnlyPropertySet = new LinkedHashSet<String>();
			globalReadOnlyPropertyMap.put(type, readOnlyPropertySet);
		}

		return readOnlyPropertySet;
	}

	private static Map<String, PropertyGroup> getPropertyGroupMapForType(Class type) {

		Map<String, PropertyGroup> groupMap = globalPropertyGroupMap.get(type);
		if(groupMap == null) {
			groupMap = new LinkedHashMap<String, PropertyGroup>();
			globalPropertyGroupMap.put(type, groupMap);
		}

		return groupMap;
	}
}
