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

/**
 * A PropertyGroup can be used to encapsulate multiple node properties
 * into a single property. Register classes of this type with
 * {@see EntityContext#registerPropertyGroup}.
 *
 * @author Christian Morgner
 */
public interface PropertyGroup<T> {
	public T getGroupedProperties(GraphObject source);
	public void setGroupedProperties(T source, GraphObject destination);
}
