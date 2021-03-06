/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.entity.app;

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SessionValue;
import org.structr.common.StructrOutputStream;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.NodeSource;
import org.structr.core.NodeSource;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.app.slots.TypedDataSlot;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.LinkedList;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 * An ActionNode that collects values from input slots and stores them in a
 * new (or existing) node of a specific type when executed.
 *
 * The type of node this ActionNode creates must be determined by the property
 * <b>targetType</b>. The values for the newly created node will be collected
 * from InteractiveNodes connected to this node by DATA relationships.
 *
 * @author Christian Morgner
 */
public class AppNodeCreator extends ActionNode implements NodeSource {

	private static final Logger logger = Logger.getLogger(AppNodeCreator.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(AppLogout.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- fields ---------------------------------------------------------

	private SessionValue<AbstractNode> currentNode = new SessionValue<AbstractNode>("sdfjawerhq38rhqerfkeföq3");

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ targetType; }

	//~--- methods --------------------------------------------------------

	@Override
	public boolean doAction(final StructrOutputStream out, final AbstractNode startNode, final String editUrl,
				final Long editNodeId) {

		final List<NodeAttribute> attributes = new LinkedList<NodeAttribute>();
		final AbstractNode parentNode        = getCreateDestination();
		final String targetType              = getTargetType();
		boolean success                      = false;

		// display warning messages for common mistakes during test phase
		if (targetType == null) {

			logger.log(Level.WARNING,
				   "AppNodeCreator needs {0} property!",
				   Key.targetType.name());
			success = false;
		}

		if (targetType != null)    // && parentNode != null)
		{

			List<InteractiveNode> dataSource = getInteractiveSourceNodes();

			attributes.add(new NodeAttribute("type",
							 targetType));

			AbstractNode storeNode = getNodeFromLoader(out.getRequest());
			boolean error          = false;

			// add attributes from data sources
			for (InteractiveNode src : dataSource) {

				Object value = src.getValue(out.getRequest());

				if ((value != null) && (value.toString().length() > 0)) {

					attributes.add(new NodeAttribute(src.getMappedName(),
									 value));

				} else {

					setErrorValue(out.getRequest(),
						      src.getName(),
						      "Please enter a value for ".concat(src.getName()));
					error = true;
				}
			}

			if (error) {
				return (false);
			}

			// if no node provided by data source,
			if (storeNode == null) {

				// create node
				storeNode = createNewNode(parentNode, targetType);
			}

			// node exists / successfully created
			if (storeNode != null) {
				success = storeNodeAttributes(storeNode, attributes);
			} else {
				success = false;
			}

			logger.log(Level.INFO,
				   "Saving newly created node {0}",
				   storeNode);
			currentNode.set(out.getRequest(),
					storeNode);
		}

		return (success);
	}

	// ----- interface NodeSource -----
	// ----- interface NodeSource -----
	@Override
	public AbstractNode loadNode(HttpServletRequest request) {

		logger.log(Level.INFO,
			   "Returning newly created node {0}",
			   currentNode.get(request));

		return (currentNode.get(request));
	}

	private boolean storeNodeAttributes(final AbstractNode node, final List<NodeAttribute> attributes) {

		for (NodeAttribute attr : attributes) {

			node.setProperty(attr.getKey(),
					 attr.getValue());
		}

		return (true);
	}

	private AbstractNode createNewNode(final AbstractNode parentNode, final String type) {

		AbstractNode ret = (AbstractNode) Services.command(securityContext,
			TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				List<NodeAttribute> attributes = new LinkedList<NodeAttribute>();

				attributes.add(new NodeAttribute(AbstractNode.Key.type.name(),
								 type));

				// attributes.add(new NodeAttribute(PUBLIC_KEY, "true"));
				AbstractNode newNode = (AbstractNode) Services.command(securityContext,
					CreateNodeCommand.class).execute(attributes);

				if (newNode != null) {

					if (parentNode != null) {

						Command createRelationship = Services.command(securityContext,
							CreateRelationshipCommand.class);

						createRelationship.execute(parentNode,
									   newNode,
									   RelType.HAS_CHILD);
					}

					return (newNode);
				}

				return (null);
			}

		});

		return (ret);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Map<String, Slot> getSlots() {

		Map<String, Slot> ret            = new HashMap<String, Slot>();
		List<InteractiveNode> dataSource = getInteractiveSourceNodes();

		// add attributes from data sources
		for (InteractiveNode src : dataSource) {

			ret.put(src.getMappedName(),
				new TypedDataSlot(src.getParameterType()));
		}

		return (ret);
	}

	@Override
	public String getIconSrc() {
		return "/images/brick_add.png";
	}

	public String getTargetType() {
		return ((String) getProperty(Key.targetType.name()));
	}

	// ----- private methods -----
	private AbstractNode getCreateDestination() {

		List<StructrRelationship> rels = getRelationships(RelType.CREATE_DESTINATION,
			Direction.OUTGOING);
		AbstractNode ret               = null;

		for (StructrRelationship rel : rels) {

			// first one wins
			ret = rel.getEndNode();

			break;
		}

		return (ret);
	}

	//~--- set methods ----------------------------------------------------

	public void setTargetType(String targetType) {

		setProperty(Key.targetType.name(),
			    targetType);
	}
}
