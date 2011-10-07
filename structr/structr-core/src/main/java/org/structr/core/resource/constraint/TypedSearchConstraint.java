/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.resource.constraint;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchAttributeGroup;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.node.search.TextualSearchAttribute;
import org.structr.core.resource.NoResultsException;
import org.structr.core.resource.PathException;
import org.structr.core.servlet.JsonRestServlet;

/**
 * Represents a keyword match using the search term given in the constructor of
 * this class. A SearchConstraint will always result in a list of elements, and
 * will throw an IllegalPathException if it is NOT the last element in an URI.
 *
 * @author Christian Morgner
 */
public class TypedSearchConstraint implements ResourceConstraint {

	private static final Logger logger = Logger.getLogger(TypedSearchConstraint.class.getName());

	private String searchString = null;
	private String type = null;

	public TypedSearchConstraint(String type, String queryString) {
		this.searchString = queryString;
		this.type = type;
	}

	@Override
	public boolean acceptUriPart(String part) {
		return false;	// we will not accept URI parts directly
	}

	@Override
	public Result processParentResult(Result result, HttpServletRequest request) throws PathException {

		// fetch search string from request
		if(searchString == null) {
			searchString = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SEARCH_STRING);
		}

		if(type.endsWith("s")) {
			logger.log(Level.FINEST, "Removing trailing plural 's' from type {0}", type);
			type = type.substring(0, type.length() - 1);
		}

		// build search results
		List<GraphObject> searchResults = getSearchResults(searchString);

		// remove search results that are not in given list
		if(result != null) {
			logger.log(Level.WARNING, "Received results from predecessor, this query is probably not optimized!");
			searchResults.retainAll(result.getResults());
		}

		return new Result(searchResults);
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) {
		return null;
	}

	// ----- private methods -----
	private List<GraphObject> getSearchResults(String searchString) throws PathException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		User user = new SuperUser();
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
			typeGroup.add(new TextualSearchAttribute("type", StringUtils.capitalize(type), SearchOperator.OR));
			searchAttributes.add(typeGroup);

			// TODO: configure searchable fields
			SearchAttributeGroup nameGroup = new SearchAttributeGroup(SearchOperator.AND);
			nameGroup.add(new TextualSearchAttribute("name",	searchString, SearchOperator.OR));
			nameGroup.add(new TextualSearchAttribute("shortName",	searchString, SearchOperator.OR));
			searchAttributes.add(nameGroup);

			return (List<GraphObject>)Services.command(SearchNodeCommand.class).execute(
				user,
				topNode,
				includeDeleted,
				publicOnly,
				searchAttributes
			);
		}

		throw new NoResultsException();
	}
}