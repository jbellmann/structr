/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.rest.constraint;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchAttributeGroup;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.node.search.TextualSearchAttribute;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.PathException;

/**
 * Represents a keyword match using the search term given in the constructor of
 * this class. A SearchConstraint will always result in a list of elements, and
 * will throw an IllegalPathException if it is NOT the last element in an URI.
 *
 * @author Christian Morgner
 */
public class SearchConstraint extends ResourceConstraint {

	private static final Logger logger = Logger.getLogger(SearchConstraint.class.getName());

	private String searchString = null;

	public SearchConstraint(SecurityContext securityContext, String searchString) {
		this.securityContext = securityContext;
		this.searchString = searchString;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;	// not directly selectable via URI
	}

	@Override
	public List<GraphObject> doGet(List<VetoableGraphObjectListener> listeners) throws PathException {

		if(searchString != null) {
			return getSearchResults(searchString);
		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet, List<VetoableGraphObjectListener> listeners) throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
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
	public ResourceConstraint tryCombineWith(ResourceConstraint next) {
		return null;
	}

	@Override
	public String getUriPart() {
		return "";
	}

	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}

	// ----- private methods -----
	private List<GraphObject> getSearchResults(String searchString) throws PathException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		AbstractNode topNode = null;
		boolean includeDeleted = false;
		boolean publicOnly = false;

		if(searchString != null) {
			
			// prepend "*" to the beginning of the search string
			if(!searchString.startsWith("*")) {
				searchString = "*".concat(searchString);
			}

			// append "*" to the end of the search string
			if(!searchString.endsWith("*")) {
				searchString = searchString.concat("*");
			}

			SearchAttributeGroup typeGroup = new SearchAttributeGroup(SearchOperator.AND);
//			typeGroup.add(new TextualSearchAttribute("type", Sport.class.getSimpleName(),		SearchOperator.OR));
//			typeGroup.add(new TextualSearchAttribute("type", SplinkUser.class.getSimpleName(),	SearchOperator.OR));
//			typeGroup.add(new TextualSearchAttribute("type", Organization.class.getSimpleName(),	SearchOperator.OR));
			searchAttributes.add(typeGroup);

			// TODO: configureContext searchable fields
			SearchAttributeGroup nameGroup = new SearchAttributeGroup(SearchOperator.AND);
			nameGroup.add(new TextualSearchAttribute("name",	searchString, SearchOperator.OR));
			nameGroup.add(new TextualSearchAttribute("shortName",	searchString, SearchOperator.OR));
			searchAttributes.add(nameGroup);

			return (List<GraphObject>)Services.command(securityContext, SearchNodeCommand.class).execute(
				topNode,
				includeDeleted,
				publicOnly,
				searchAttributes
			);
		}

		throw new NoResultsException();
	}
}
