/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.rest.constraint;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.core.GraphObject;

/**
 * Encapsulates the result of a query operation.
 *
 * @author Christian Morgner
 */
public class Result {

	private List<? extends GraphObject> results = null;
	private boolean isCollectionResource = false;
	private PropertyView propertyView = null;

	private String searchString = null;
	private String queryTime = null;
	private String sortOrder = null;
	private String sortKey = null;

	//private Integer resultCount = null;
	private Integer pageCount = null;
	private Integer pageSize = null;
	private Integer page = null;

	public Result(List<? extends GraphObject> listResult, boolean isCollectionResource) {
		this.isCollectionResource = isCollectionResource;
		this.results = listResult;
	}

	public List<? extends GraphObject> getResults() {
		return results;
	}

	public void setQueryTime(String queryTime) {
		this.queryTime = queryTime;
	}

	public String getQueryTime() {
		return queryTime;
	}

	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	public String getSortKey() {
		return sortKey;
	}

	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	public Integer getResultCount() {
		return results.size();
	}

//	public void setResultCount(Integer resultCount) {
//		this.resultCount = resultCount;
//	}

	public Integer getPageCount() {
		return pageCount;
	}

	public void setPageCount(Integer pageCount) {
		this.pageCount = pageCount;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public PropertyView getPropertyView() {
		return propertyView;
	}

	public void setPropertyView(PropertyView propertyView) {
		this.propertyView = propertyView;
	}

	public boolean isCollectionResource() {
		return isCollectionResource;
	}
}
