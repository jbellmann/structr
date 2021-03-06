package org.structr.core.renderer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.structr.common.AbstractNodeComparator;
import org.structr.common.RelType;
import org.structr.common.RenderMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Template;

/**
 *
 * @author Christian Morgner
 */
public class NodeListRenderer extends NodeViewRenderer
{
	private static final Logger logger = Logger.getLogger(NodeListRenderer.class.getName());
	private static final String PAGE_NO_PARAMETER_NAME_KEY = "pageNoParameterName";
	private static final String PAGE_SIZE_PARAMETER_NAME_KEY = "pageSizeParameterName";
	private static final String SORT_KEY_PARAMETER_NAME_KEY = "sortKeyParameterName";
	private static final String SORT_ORDER_PARAMETER_NAME_KEY = "sortOrderParameterName";
	// defaults
	private String sortKeyParameterName = "sortKey";
	private String sortOrderParameterName = "sortOrder";
	private String pageNoParameterName = "pageNo";
	private String pageSizeParameterName = "pageSize";
	private String sortKey = "name";
	private String sortOrder = AbstractNodeComparator.ASCENDING;
	private int pageNo = 1;
	private int pageSize = 10;
	private int lastPage = -1;

	@Override
	public void renderNode(StructrOutputStream out, AbstractNode currentNode, AbstractNode startNode, String editUrl, Long editNodeId, RenderMode renderMode)
	{
		SecurityContext securityContext = out.getSecurityContext();
		if(securityContext.isVisible(currentNode)) {

			if(currentNode.hasTemplate())
			{
				Template template = currentNode.getTemplate();
				String html = template.getContent();

				if(StringUtils.isNotBlank(html))
				{
					init(out.getRequest(), currentNode);

					List<AbstractNode> nodesToRender = new LinkedList<AbstractNode>();

					// iterate over children following the DATA relationship and collect all nodes
					for(AbstractNode container : currentNode.getDirectChildren(RelType.DATA))
					{
						collectDataNodes(out.getRequest(), container, nodesToRender, 0, 255);
					}

					//Collections.sort(nodesToRender, new AbstractNodeComparator(AbstractNode.toGetter(sortKey), sortOrder));

					int toIndex = Math.min(pageNo * pageSize, nodesToRender.size());
					int fromIndex = Math.min(Math.max(pageNo - 1, 0) * pageSize, toIndex);

					logger.log(Level.INFO, "Showing list elements from {0} to {1}", new Object[]
						{
							fromIndex, toIndex
						});

					// iterate over direct children of the given node
					for(AbstractNode n : nodesToRender.subList(fromIndex, toIndex))
					{

						doRendering(out, currentNode, n, editUrl, editNodeId);
					}

				} else
				{
					logger.log(Level.WARNING, "No template!");
				}
			}

		} else
		{
			logger.log(Level.WARNING, "Node not visible");
		}
	}

	@Override
	public String getContentType(AbstractNode node)
	{
		return ("text/html");
	}

	// ----- private methods -----
	private void collectDataNodes(HttpServletRequest request, AbstractNode rootNode, List<AbstractNode> nodesToRender, int depth, int maxDepth) {
		
		if(rootNode != null && depth < maxDepth) {
			
			Iterable<AbstractNode> iterable = rootNode.getDataNodes(request);
			if(iterable != null) {
				
				Iterator<AbstractNode> iter = iterable.iterator();
				if(iter.hasNext()) {

					for(AbstractNode dataNode : iterable) {

						// recurse deeper
						collectDataNodes(request, dataNode, nodesToRender, depth+1, maxDepth);
					}
					
				} else {
					
					// empty collection => this is a leaf
					nodesToRender.add(rootNode);
					
					// recursion ends here
				}
				
			} else {
				
				// empty collection => this is a leaf
				nodesToRender.add(rootNode);
					
				// recursion ends here				
			}
		}
	}
	
	private void init(HttpServletRequest request, AbstractNode node)
	{

		sortKey = getStringParameterValue(request, node, SORT_KEY_PARAMETER_NAME_KEY, sortKeyParameterName, sortKey);
		sortOrder = getStringParameterValue(request, node, SORT_ORDER_PARAMETER_NAME_KEY, sortOrderParameterName, sortOrder);
		pageNo = getIntParameterValue(request, node, PAGE_NO_PARAMETER_NAME_KEY, pageNoParameterName, pageNo);
		if(pageNo < 1)
		{
			pageNo = 1;
		}
		if(pageSize < 1)
		{
			pageSize = 1;
		}

		pageSize = getIntParameterValue(request, node, PAGE_SIZE_PARAMETER_NAME_KEY, pageSizeParameterName, pageSize);

		lastPage = Math.abs(getSize(node) / pageSize);
		if(getSize(node) % pageSize > 0)
		{
			lastPage++;
		}
	}

	private String getStringParameterValue(HttpServletRequest request, final AbstractNode node, final String namePropertyKey, final String defaultParameterName, final String defaultValue)
	{
		String nameValue = defaultParameterName;
		String propertyValue = node.getStringProperty(namePropertyKey);
		if(StringUtils.isNotEmpty(propertyValue))
		{
			nameValue = propertyValue;
		}
		String value = defaultValue;
		if(StringUtils.isNotEmpty(nameValue))
		{
			String parameterValue = request.getParameter(nameValue);
			if(StringUtils.isNotEmpty(parameterValue))
			{
				value = parameterValue;
			}
		}
		return value;
	}

	private int getIntParameterValue(HttpServletRequest request, final AbstractNode node, final String namePropertyKey, final String defaultParameterName, final int defaultValue)
	{
		String nameValue = defaultParameterName;
		String propertyValue = node.getStringProperty(namePropertyKey);
		if(StringUtils.isNotEmpty(propertyValue))
		{
			nameValue = propertyValue;
		}
		int value = defaultValue;
		if(StringUtils.isNotEmpty(nameValue))
		{
			String parameterValue = request.getParameter(nameValue);
			if(StringUtils.isNotEmpty(parameterValue))
			{
				value = Integer.parseInt(parameterValue);
			}
		}
		return value;
	}

	public int getLastPageNo(HttpServletRequest request, AbstractNode node)
	{
		if(lastPage == -1)
		{
			init(request, node);
		}
		return lastPage;
	}

	private int getSize(final AbstractNode node)
	{
		int size = 0;

		// iterate over children following the DATA relationship
		for(AbstractNode container : node.getSortedDirectChildren(RelType.DATA))
		{
			List<AbstractNode> nodes = container.getDirectChildNodes();
			size += nodes.size();
		}
		return size;
	}

	private String getPager(HttpServletRequest request, AbstractNode node)
	{
		StringBuilder out = new StringBuilder();
		init(request, node);

		out.append("<ul>");

		if(pageNo > 1)
		{
			out.append("<li><a href=\"?pageSize=").append(pageSize).append("&pageNo=").append(pageNo - 1).append("&sortKey=").append(sortKey).append("&sortOrder=").append(sortOrder).append("\">").append(" &lt; previous (").append(pageNo - 1).append(")").append("</a></li>");
		}

		for(int i = 1; i <= lastPage; i++)
		{

			// if we have more than 10 pages, skip some pages
			if(lastPage > 10
				&& (i < pageNo - 5 || i > pageNo + 5)
				&& (i < lastPage - 5 && i > 5))
			{
				continue;
			}

			out.append("<li");
			if(i == pageNo)
			{
				out.append(" class=\"current\"");
			}
			out.append("><a href=\"?pageSize=").append(pageSize).append("&pageNo=").append(i).append("&sortKey=").append(sortKey).append("&sortOrder=").append(sortOrder).append("\">").append(i).append("</a></li>");

		}


		if(pageNo < lastPage)
		{
			out.append("<li><a href=\"?pageSize=").append(pageSize).append("&pageNo=").append(pageNo + 1).append("&sortKey=").append(sortKey).append("&sortOrder=").append(sortOrder).append("\">").append("next (").append(pageNo + 1).append(") &gt;").append("</a></li>");
		}

		out.append("</ul>");

		return out.toString();

	}
}
