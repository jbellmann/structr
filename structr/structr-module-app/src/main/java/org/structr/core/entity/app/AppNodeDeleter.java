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

import org.structr.common.PropertyView;
import org.structr.common.StructrOutputStream;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.DeleteNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class AppNodeDeleter extends ActionNode {

	static {

		EntityContext.registerPropertySet(AppNodeDeleter.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- methods --------------------------------------------------------

	@Override
	public boolean doAction(final StructrOutputStream out, final AbstractNode startNode, final String editUrl,
				final Long editNodeId) {

		AbstractNode toDelete = getNodeFromLoader(out.getRequest());

		if (toDelete != null) {

			// FIXME: is this the right way to delete a node?
			Services.command(securityContext,
					 DeleteNodeCommand.class).execute(toDelete,
				null);
		}

		return (true);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Map<String, Slot> getSlots() {
		return null;
	}

	@Override
	public String getIconSrc() {
		return "/images/brick_delete.png";
	}
}
