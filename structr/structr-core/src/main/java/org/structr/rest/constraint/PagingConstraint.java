/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.rest.constraint;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.PathException;

/**
 * Implements paging.
 *
 * @author Christian Morgner
 */
public class PagingConstraint extends WrappingConstraint {

	private static final Logger logger = Logger.getLogger(PagingConstraint.class.getName());

	private int resultCount = 0;
	private int pageSize = 0;
	private int page = 0;

	public PagingConstraint(SecurityContext securityContext, int page, int pageSize) {
		this.securityContext = securityContext;
		this.page = page;
		this.pageSize = pageSize;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;
	}

	@Override
	public List<? extends GraphObject> doGet(List<VetoableGraphObjectListener> listeners) throws PathException {

		/*
		 * page 1: 0 -> pageSize-1
		 * page 2: pageSize -> (2*pageSize)-1
		 * page 3: (2*pageSize) -> (3*pageSize)-1
		 * page n: ((n-1) * pageSize) -> (n * pageSize) - 1
		 */

		List<? extends GraphObject> results = wrappedConstraint.doGet(listeners);
		resultCount = results.size();

		int fromIndex = Math.min(resultCount, Math.max(0, (getPage()-1) * getPageSize()));
		int toIndex = Math.min(resultCount, getPage()*getPageSize());

		logger.log(Level.FINEST, "returning results from {0} to {1}, page {2}, pageSize {3}", new Object[] { fromIndex, toIndex-1, getPage(), getPageSize()} );

		return results.subList(fromIndex, toIndex);
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) {
		return null;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getResultCount() {
		return resultCount;
	}

	public int getPageCount() {
		return (int)Math.rint(Math.ceil((double)getResultCount() / (double)getPageSize()));
	}
}
