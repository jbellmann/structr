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



package org.structr.core.node;

import org.neo4j.graphdb.GraphDatabaseService;

import org.structr.common.SecurityContext;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * <p>Searches for a user node by her/his name in the database and returns the result.</p>
 *
 * <p>This command takes one or two parameters:</p>
 *
 * <ol>
 *  <li>first parameter: User name
 *  <li>second parameter (optional): Top node, return users beneath this node
 * </ol>
 *
 *
 * @author amorgner
 */
public class FindUserCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(FindUserCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		Command searchNode           = Services.command(SecurityContext.getSuperUserInstance(),
			SearchNodeCommand.class);

		if (graphDb != null) {

			List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

			searchAttrs.add(Search.andExactType(User.class.getSimpleName()));

			switch (parameters.length) {

				case 0 :
					List<User> users          = new LinkedList<User>();
					List<AbstractNode> result = (List<AbstractNode>) searchNode.execute(null,
						false,
						false,
						searchAttrs);

					for (AbstractNode n : result) {

						if (n instanceof User) {
							users.add((User) n);
						}
					}

					return users;

				case 1 :

					// we have only a simple user name
					if (parameters[0] instanceof String) {

						String userName = (String) parameters[0];

						searchAttrs.add(Search.andExactName(userName));

						List<AbstractNode> usersFound =
							(List<AbstractNode>) searchNode.execute(null,
							false,
							false,
							searchAttrs);

						if ((usersFound != null) && (usersFound.size() > 0)
							&& (usersFound.get(0) instanceof User)) {
							return (User) usersFound.get(0);
						} else {

							logger.log(Level.FINE,
								   "No user with name {0} found.",
								   userName);

							return null;
						}
					}

//                              break;
				case 2 :

					// Limit search to a top node, means: Return users which are in the CHILD tree beneath a given node
					if ((parameters[0] instanceof String)
						&& (parameters[1] instanceof AbstractNode)) {

						String userName      = (String) parameters[0];
						AbstractNode topNode = (AbstractNode) parameters[1];

						searchAttrs.add(Search.andExactName(userName));

						List<AbstractNode> usersFound =
							(List<AbstractNode>) searchNode.execute(topNode,
							false,
							false,
							searchAttrs);

						if ((usersFound != null) && (usersFound.size() > 0)
							&& (usersFound.get(0) instanceof User)) {
							return (User) usersFound.get(0);
						} else {

							logger.log(Level.FINE,
								   "No user with name {0} found.",
								   userName);

							return null;
						}
					}

					break;

				default :
					break;
			}
		}

		return null;
	}
}
