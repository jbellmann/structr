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

import org.apache.commons.lang.StringUtils;
import org.structr.common.RequestHelper;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.common.RequestCycleListener;
import org.structr.common.SessionValue;
import org.structr.common.renderer.ExternalTemplateRenderer;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;
import org.structr.core.NodeSource;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class TextField extends FormField implements InteractiveNode, RequestCycleListener {

	private static final Logger logger = Logger.getLogger(TextField.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(SubmitButton.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- fields ---------------------------------------------------------

	protected SessionValue<Object> errorSessionValue = null;
	private String mappedName                        = null;
	protected SessionValue<Object> sessionValue      = null;


	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ sourceSlotName; }

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new ExternalTemplateRenderer(false));
	}

	// ----- interface RequestCycleListener -----
	@Override
	public void onRequestStart(HttpServletRequest request) {}

	@Override
	public void onRequestEnd(HttpServletRequest request) {
		getErrorMessageValue().set(request, null);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/textfield.png";
	}

	// ----- interface InteractiveNode -----
	@Override
	public Object getValue(HttpServletRequest request)
	{
		Object value = getValueFromSource(request);
		String name = getName();
		String ret = null;

		// only return value from last request if we were redirected before
		if(RequestHelper.isRedirected(request))
		{
			value = getLastValue().get(request);

			// otherwise, clear value in session
			getLastValue().set(request, null);
		}

		if (request == null) {
			return value;
		}

		if (request != null) {

			ret = request.getParameter(name);

			if (ret != null) {

				// Parameter is there
				if (ret.length() == 0) {

					// Empty value
					return null;
				} else {

					// store value in session, in case we get a redirect afterwards
					getLastValue().set(request, ret);
					return ret;
				}
			} else {

				// Parameter is not in request
				return value;
			}
		}

		return null;
	}

	@Override
	public String getStringValue(HttpServletRequest request)
	{
		Object value = getValue(request);
		return (value != null ? value.toString() : null);
	}

	@Override
	public Class getParameterType() {
		return (String.class);
	}

	@Override
	public String getMappedName() {

		if (StringUtils.isNotBlank(mappedName)) {
			return (mappedName);
		}

		return (getName());
	}

	@Override
	public void setErrorValue(HttpServletRequest request, Object errorValue)
	{
		getErrorMessageValue().set(request, errorValue);
	}

	@Override
	public Object getErrorValue(HttpServletRequest request)
	{
		return(getErrorMessageValue().get(request));
	}

	@Override
	public String getErrorMessage(HttpServletRequest request)
	{
		Object errorValue = getErrorValue(request);
		if(errorValue != null)
		{
			return(errorValue.toString());
		}

		return(null);
	}



	// ----- private methods -----
	private SessionValue<Object> getErrorMessageValue() {

		if (errorSessionValue == null) {
			errorSessionValue = new SessionValue<Object>(createUniqueIdentifier("errorMessage"));
		}

		return (errorSessionValue);
	}

	private SessionValue<Object> getLastValue() {

		if (sessionValue == null) {
			sessionValue = new SessionValue<Object>(createUniqueIdentifier("lastValue"));
		}

		return (sessionValue);
	}

	/**
	 * Follows any incoming DATA relationship and tries to obtain a data value with
	 * the mapped name from the relationship.
	 *
	 * @return the value or null
	 */
	private Object getValueFromSource(HttpServletRequest request)
	{
		List<StructrRelationship> rels = getIncomingDataRelationships();
		String sourceName              = this.getName();
		Object ret                     = null;

		// follow INCOMING DATA relationships to found data source for this input field
		for (StructrRelationship rel : rels) {

			// first one wins
			AbstractNode startNode = rel.getStartNode();

			if (startNode instanceof NodeSource) {

				// source name mapping present? use input field name otherwise
				if (rel.getRelationship().hasProperty(Key.sourceSlotName.name())) {

					sourceName =
						(String) rel.getRelationship().getProperty(Key.sourceSlotName.name());
				}

				NodeSource source = (NodeSource) startNode;

				if (source != null) {

					AbstractNode loadedNode = source.loadNode(request);

					if (loadedNode != null) {
						ret = loadedNode.getProperty(sourceName);
					}
				}
			}

			// if a value is found, return it, otherwise try the next data source
			if (ret != null) {
				break;
			}
		}

		return (ret);
	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setMappedName(String mappedName) {
		this.mappedName = mappedName;
	}

}
