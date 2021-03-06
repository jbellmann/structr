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
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.core.renderer.HtmlRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class AppButton extends HtmlNode {

	static {

		EntityContext.registerPropertySet(AppButton.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		HtmlRenderer renderer = new HtmlRenderer("input");

		renderer.addAttribute("type",
				      "button");
		renderers.put(RenderMode.Default,
			      renderer);
	}

	@Override
	public void doBeforeRendering(HtmlRenderer renderer, StructrOutputStream out, AbstractNode startNode,
				      String editUrl, Long editNodeId) {}

	@Override
	public void renderContent(HtmlRenderer renderer, StructrOutputStream out, AbstractNode startNode,
				  String editUrl, Long editNodeId) {}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/";
	}

	@Override
	public boolean hasContent(HtmlRenderer renderer, StructrOutputStream out, AbstractNode startNode,
				  String editUrl, Long editNodeId) {
		return false;
	}
}
