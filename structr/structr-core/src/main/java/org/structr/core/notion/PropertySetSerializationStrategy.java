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

package org.structr.core.notion;

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class PropertySetSerializationStrategy implements SerializationStrategy {

	private PropertyKey[] propertyKeys = null;

	public PropertySetSerializationStrategy(PropertyKey... propertyKeys) {
		this.propertyKeys = propertyKeys;
	}

	@Override
	public Object serialize(SecurityContext securityContext, Class type, GraphObject source) {

		if(source != null) {
			Map<String, Object> propertySet = new LinkedHashMap<String, Object>();
			for(PropertyKey key : propertyKeys) {
				propertySet.put(key.name(), source.getProperty(key.name()));
			}
			return propertySet;
		}

		return null;
	}
}
