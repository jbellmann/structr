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

package org.structr.rest.constraint;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.DirectedRelationship;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.PathException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;

/**
 *
 * @author Christian Morgner
 */
public class StaticRelationshipConstraint extends FilterableConstraint {

	TypedIdConstraint typedIdConstraint = null;
	TypeConstraint typeConstraint = null;

	public StaticRelationshipConstraint(SecurityContext securityContext, TypedIdConstraint typedIdConstraint, TypeConstraint typeConstraint) {
		this.securityContext = securityContext;
		this.typedIdConstraint = typedIdConstraint;
		this.typeConstraint = typeConstraint;
	}

	@Override
	public List<? extends GraphObject> doGet(final List<VetoableGraphObjectListener> listeners) throws PathException {

		List<GraphObject> results = typedIdConstraint.doGet(listeners);
		if(results != null) {

			// get source and target type from previous constraints
			String sourceType = typedIdConstraint.getTypeConstraint().getType();
			String targetType = typeConstraint.getType();

			// fetch static relationship definition
			DirectedRelationship staticRel = EntityContext.getRelation(sourceType, targetType);
			if(staticRel != null) {
				return staticRel.getRelatedNodes(securityContext, typedIdConstraint.getTypesafeNode(), targetType);
			}
		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet, final List<VetoableGraphObjectListener> listeners) throws Throwable {

		// create transaction closure
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				AbstractNode sourceNode = typedIdConstraint.getIdConstraint().getNode();
				AbstractNode newNode = typeConstraint.createNode(listeners, propertySet);
				DirectedRelationship rel = EntityContext.getRelation(sourceNode.getClass(), newNode.getClass());

				if(sourceNode != null && newNode != null && rel != null) {

					rel.createRelationship(securityContext, sourceNode, newNode, newNode.getType());

					ErrorBuffer errorBuffer = new ErrorBuffer();

					if(!validAfterCreation(listeners, newNode, errorBuffer)) {
						throw new IllegalArgumentException(errorBuffer.toString());
					}

					return newNode;
				}

				throw new IllegalPathException();
			}
		};

		// execute transaction: create new node
		AbstractNode newNode = (AbstractNode)Services.command(securityContext, TransactionCommand.class).execute(transaction);
		if(newNode == null) {

			// re-throw transaction exception cause
			if(transaction.getCause() != null) {
				throw transaction.getCause();
			}
		}

		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
		result.addHeader("Location", buildLocationHeader(newNode.getType(), newNode.getId()));
		return result;
	}

	@Override
	public RestMethodResult doHead() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {
		return super.tryCombineWith(next);
	}

	@Override
	public String getUriPart() {
		return typedIdConstraint.getUriPart().concat("/").concat(typeConstraint.getUriPart());
	}

	public TypedIdConstraint getTypedIdConstraint() {
		return typedIdConstraint;
	}

	public TypeConstraint getTypeConstraint() {
		return typeConstraint;
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}
}
