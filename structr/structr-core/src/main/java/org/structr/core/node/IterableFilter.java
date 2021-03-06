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

package org.structr.core.node;

import java.util.Iterator;
import java.util.Set;
import org.structr.core.Predicate;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class IterableFilter<T extends AbstractNode> implements Iterable<T> {

	private Iterator<T> sourceIterator = null;
	private Set<Predicate<T>> filters = null;

	public IterableFilter(Iterable<T> source, Set<Predicate<T>> filters) {

		this.sourceIterator = source.iterator();
		this.filters = filters;
	}

	@Override
	public Iterator<T> iterator() {

		return(new Iterator<T>() {

			private boolean hasNextCalled = false;
			private T currentElement = null;

			@Override
			public boolean hasNext()
			{
				do {
					if(sourceIterator.hasNext()) {

						currentElement = sourceIterator.next();

					} else {

						currentElement = null;
					}

				} while(currentElement != null && !accept(currentElement));

				hasNextCalled = true;

				return(currentElement != null);
			}

			@Override
			public T next()
			{
				// prevent returning the same object over and over again
				// when user doesn't call hasNext()
				if(!hasNextCalled) {

					hasNext();

				} else {

					hasNextCalled = false;
				}

				return(currentElement);
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException("IterableFilterIterator does not support removal of elements");
			}

		});
	}

	// ----- private methods -----
	private boolean accept(T element) {

		boolean ret = true;

		for(Predicate<T> predicate : filters) {

			ret &= predicate.evaluate(element);
		}

		return(ret);
	}
}
