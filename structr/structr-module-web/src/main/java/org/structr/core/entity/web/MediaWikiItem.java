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



package org.structr.core.entity.web;

import org.apache.commons.lang.StringUtils;

import org.structr.common.PropertyView;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * Wraps an entry of a MediaWiki wiki into a node
 *
 * @author Axel Morgner
 */
public class MediaWikiItem extends AbstractNode {

	static {

		EntityContext.registerPropertySet(MediaWikiItem.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- fields ---------------------------------------------------------

	private Node source                = null;
	private Map<String, Object> values = new LinkedHashMap<String, Object>();

	//~--- constructors ---------------------------------------------------

	public MediaWikiItem() {}

	public MediaWikiItem(int index, Node node) {

		this.source = node;

		// synthesize type
		values.put(AbstractNode.Key.type.name(),
			   "MediaWikiItem");
		values.put(AbstractNode.Key.name.name(),
			   "item" + index);
		initialize();
	}

	//~--- methods --------------------------------------------------------

	// ----- private methods -----
	private void initialize() {

		if (this.source != null) {

			NodeList children = this.source.getChildNodes();
			int len           = children.getLength();

			for (int i = 0; i < len; i++) {

				Node child   = children.item(i);
				String name  = child.getNodeName();
				String value = getValue(child);

				if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(value)) {

					if (name.contains(":")) {

						String[] namespaceParts = name.split("[:]+");

						if (namespaceParts.length == 2) {

							String namespace                 = namespaceParts[0];
							String relativeName              = namespaceParts[1];
							Map<String, Object> namespaceMap =
								(Map<String, Object>) values.get(namespace);

							if (namespaceMap == null) {

								namespaceMap = new LinkedHashMap<String, Object>();
								values.put(namespace,
									   namespaceMap);
							}

							namespaceMap.put(relativeName,
									 value);
						}

					} else {

						values.put(name,
							   value);
					}
				}
			}
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Iterable<String> getPropertyKeys() {
		return (values.keySet());
	}

	@Override
	public Object getProperty(String key) {
		return (values.get(key));
	}

	@Override
	public String getIconSrc() {
		return "/images/page_white_text_width.png";
	}

	private String getValue(Node child) {

		if (child.hasChildNodes()) {
			return (child.getFirstChild().getNodeValue());
		} else {
			return (child.getNodeValue());
		}
	}
}
