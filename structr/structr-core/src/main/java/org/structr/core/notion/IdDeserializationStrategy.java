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

import org.apache.commons.lang.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class IdDeserializationStrategy implements DeserializationStrategy {
	@Override
	public GraphObject deserialize(SecurityContext securityContext, Class type, Object source) {

		try {
			return (GraphObject)Services.command(securityContext, FindNodeCommand.class).execute(source);
			
		} catch(Throwable t) {
			
			StringBuilder errorMessage = new StringBuilder(100);

			if(type != null) {
				errorMessage.append(StringUtils.capitalize(type.getSimpleName()));
			}
			errorMessage.append(" with id ");
			errorMessage.append(source);
			errorMessage.append(" not found.");

			throw new IllegalArgumentException(errorMessage.toString());
		}
	}
}
