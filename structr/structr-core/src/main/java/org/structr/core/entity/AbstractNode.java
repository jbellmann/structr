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



package org.structr.core.entity;

import freemarker.ext.servlet.HttpRequestParametersHashModel;

import freemarker.template.Configuration;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.kernel.Traversal;

import org.structr.common.AbstractComponent;
import org.structr.common.AbstractNodeComparator;
import org.structr.common.AccessControllable;
import org.structr.common.ErrorBuffer;
import org.structr.common.PathHelper;
import org.structr.common.Permission;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.RenderMode;
import org.structr.common.RequestHelper;
import org.structr.common.SecurityContext;
import org.structr.common.StructrOutputStream;
import org.structr.common.TemplateHelper;
import org.structr.common.renderer.DefaultEditRenderer;
import org.structr.common.renderer.RenderContext;
import org.structr.common.renderer.RenderController;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.NodeRenderer;
import org.structr.core.NodeSource;
import org.structr.core.PropertyConverter;
import org.structr.core.PropertyValidator;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.cloud.NodeDataContainer;
import org.structr.core.converter.LongDateConverter;
import org.structr.core.converter.NodeIdNodeConverter;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.IndexNodeCommand;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.NodeRelationshipStatisticsCommand;
import org.structr.core.node.NodeRelationshipsCommand;
import org.structr.core.node.SetOwnerCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.XPath;
import org.structr.core.node.search.Search;

//~--- JDK imports ------------------------------------------------------------

import java.io.StringReader;
import java.io.Writer;

import java.lang.reflect.Method;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.structr.core.IterableAdapter;
import org.structr.core.PropertyGroup;
import org.structr.core.notion.Notion;

//~--- classes ----------------------------------------------------------------

/**
 * The base class for all node types in structr.
 *
 * @author amorgner
 *
 */
public abstract class AbstractNode
	implements Comparable<AbstractNode>, RenderController, AccessControllable, GraphObject {

//      public final static String CATEGORIES_KEY         = "categories";
//      public final static String CREATED_BY_KEY         = "createdBy";
//      public final static String CREATED_DATE_KEY       = "createdDate";
//      public final static String DELETED_KEY            = "deleted";
//      public final static String HIDDEN_KEY             = "hidden";
//      public final static String LAST_MODIFIED_DATE_KEY = "lastModifiedDate";
//      public final static String NAME_KEY               = "name";
//      public final static String NODE_ID_KEY            = "nodeId";
//      public final static String ACL_KEY = "acl";
	// private final static String keyPrefix = "${";
	// private final static String keySuffix = "}";
	private final static String NODE_KEY_PREFIX = "%{";
	private final static String NODE_KEY_SUFFIX = "}";

//      public final static String OWNER_ID_KEY                   = "ownerId";
//      public final static String OWNER_KEY                      = "owner";
//      public final static String POSITION_KEY                   = "position";
//      public final static String PUBLIC_KEY                     = "public";
	private final static String SUBNODES_AND_LINKED_NODES_KEY = "#";
	private final static String SUBNODES_KEY                  = "*";

//      public final static String TEMPLATE_ID_KEY                = "templateId";
//      public final static String LOCALE_KEY = "locale";
//      public final static String TITLES_KEY = "titles";
//      public final static String TITLE_KEY  = "title";
	// keys for basic properties (any node should have at least all of the following properties)
//      public final static String TYPE_KEY                           = "type";
//      public final static String VISIBILITY_END_DATE_KEY            = "visibilityEndDate";
//      public final static String VISIBILITY_START_DATE_KEY          = "visibilityStartDate";
//      public final static String VISIBLE_TO_AUTHENTICATED_USERS_KEY = "visibleToAuthenticatedUsers";
	private static final Logger logger              = Logger.getLogger(AbstractNode.class.getName());
	private static final boolean updateIndexDefault = true;

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(AbstractNode.class,
						  PropertyView.All,
						  Key.values());
		EntityContext.registerPropertyConverter(AbstractNode.class,
			Key.visibilityStartDate,
			LongDateConverter.class);
		EntityContext.registerPropertyConverter(AbstractNode.class,
			Key.visibilityEndDate,
			LongDateConverter.class);
		EntityContext.registerPropertyConverter(AbstractNode.class,
			Key.lastModifiedDate,
			LongDateConverter.class);
		EntityContext.registerPropertyConverter(AbstractNode.class,
			Key.createdDate,
			LongDateConverter.class);
		EntityContext.registerPropertyConverter(AbstractNode.class,
			Key.ownerId,
			NodeIdNodeConverter.class);
	}

	//~--- fields ---------------------------------------------------------

	private List<StructrRelationship> allRelationships           = null;
	private List<StructrRelationship> incomingChildRelationships = null;
	private List<StructrRelationship> incomingDataRelationships  = null;
	private List<StructrRelationship> incomingLinkRelationships  = null;
	private List<StructrRelationship> incomingRelationships      = null;
	private List<StructrRelationship> outgoingChildRelationships = null;
	private List<StructrRelationship> outgoingDataRelationships  = null;
	private List<StructrRelationship> outgoingLinkRelationships  = null;
	private List<StructrRelationship> outgoingRelationships      = null;

	// request parameters
	// private HttpServletRequest request = null;
	// private HttpSession session = null;
	private final Map<RenderMode, NodeRenderer> rendererMap = new EnumMap<RenderMode,
									  NodeRenderer>(RenderMode.class);
	protected SecurityContext securityContext                    = null;
	private Map<Long, StructrRelationship> securityRelationships = null;
	private boolean renderersInitialized                         = false;
	private boolean readOnlyPropertiesUnlocked                   = false;

	// reference to database node
	protected Node dbNode;

	// dirty flag, true means that some changes are not yet written to the database
	protected boolean isDirty;
	protected Map<String, Object> properties;

	// public final static String TEMPLATES_KEY = "templates";
	protected Template template;
	protected User user;

	//~--- constant enums -------------------------------------------------

	public static enum Key implements PropertyKey {

		uuid, name, type, nodeId, createdBy, createdDate, deleted, hidden, lastModifiedDate, position,
		visibleToPublicUsers, title, titles, visibilityEndDate, visibilityStartDate,
		visibleToAuthenticatedUsers, templateId, categories, ownerId, owner;
	}

	//~--- constructors ---------------------------------------------------

	public AbstractNode() {

		this.properties = new HashMap<String, Object>();
		isDirty         = true;
	}

	public AbstractNode(final Map<String, Object> properties) {

		this.properties = properties;
		isDirty         = true;
	}

	public AbstractNode(SecurityContext securityContext, final Node dbNode) {

		init(securityContext,
		     dbNode);
	}

	public AbstractNode(final SecurityContext securityContext, final NodeDataContainer data) {

		if (data != null) {

			this.securityContext = securityContext;
			this.properties      = data.getProperties();
			isDirty              = true;
		}
	}

	//~--- methods --------------------------------------------------------

	/**
	 * Implement this method to specify renderers for the different rendering modes.
	 *
	 * @param rendererMap the map that hosts renderers for the different rendering modes
	 */
	public void initializeRenderers(final Map<RenderMode, NodeRenderer> rendererMap) {

		// override me
	}

	/**
	 * Called when a node of this type is created in the UI.
	 */
	public void onNodeCreation() {

		// override me
	}

	/**
	 * Called when a node of this type is instatiated. Please note that a
	 * node can (and will) be instantiated several times during a normal
	 * rendering turn.
	 */
	public void onNodeInstantiation() {

		// override me
	}

	/**
	 * Called when a node of this type is deleted.
	 */
	public void onNodeDeletion() {

		// override me
	}

	@Override
	public boolean renderingAllowed(final RenderContext context) {
		return true;
	}

	public void init(final SecurityContext securityContext, final Node dbNode) {

		this.dbNode          = dbNode;
		this.isDirty         = false;
		this.securityContext = securityContext;
		logger.log(Level.FINE,
			   "User set to {0}",
			   user);
	}

	private void init(final SecurityContext securityContext, final AbstractNode node) {

		this.dbNode          = node.dbNode;
		this.isDirty         = false;
		this.securityContext = securityContext;
	}

	public void init(final SecurityContext securityContext, final NodeDataContainer data) {

		if (data != null) {

			this.properties      = data.getProperties();
			this.isDirty         = true;
			this.securityContext = securityContext;
		}
	}

	@Override
	public boolean equals(final Object o) {

		if (o == null) {
			return false;
		}

		if (!(o instanceof AbstractNode)) {
			return false;
		}

		return (new Integer(this.hashCode()).equals(new Integer(o.hashCode())));
	}

	@Override
	public int hashCode() {

		if (this.dbNode == null) {
			return (super.hashCode());
		}

		return (new Long(dbNode.getId()).hashCode());
	}

	@Override
	public int compareTo(final AbstractNode node) {

		// TODO: implement finer compare methods, e.g. taking title and position into account
		if ((node == null) || (node.getName() == null) || (this.getName() == null)) {
			return -1;
		}

		return (this.getName().compareTo(node.getName()));
	}

	public final void renderNode(final StructrOutputStream out, final AbstractNode startNode, final String editUrl,
				     final Long editNodeId) {

		if (this.equals(startNode) &&!(this.renderingAllowed(RenderContext.AsTopNode))) {
			return;
		}

		if (!(this.equals(startNode)) &&!(this.renderingAllowed(RenderContext.AsSubnode))) {
			return;
		}

		// initialize renderers
		if (!renderersInitialized) {

			initializeRenderers(rendererMap);
			renderersInitialized = true;
		}

		// determine RenderMode
		RenderMode renderMode = RenderMode.Default;

		if (this.equals(startNode) && rendererMap.containsKey(RenderMode.Direct)) {
			renderMode = RenderMode.Direct;
		}

		if ((editNodeId != null) && (getId() == editNodeId.longValue())) {
			renderMode = RenderMode.Edit;
		}

		// fetch Renderer
		NodeRenderer nodeRenderer = rendererMap.get(renderMode);

		if (nodeRenderer == null) {

			logger.log(Level.FINE,
				   "No renderer found for mode {0}, using default renderers",
				   renderMode);

			switch (renderMode) {

				case Default :
					nodeRenderer = new DefaultRenderer();

					break;

				case Direct :

					// no default renderer for Direct
					break;

				case Edit :
					nodeRenderer = new DefaultEditRenderer();

					break;
			}
		}

		logger.log(Level.FINE,
			   "Got renderer {0} for mode {1}, node type {2} ({3})",
			   new Object[] { (nodeRenderer != null)
					  ? nodeRenderer.getClass().getName()
					  : "Unknown", renderMode, this.getType(), this.getId() });

		if (nodeRenderer != null) {

			// set content type
			out.setContentType(nodeRenderer.getContentType(this));

			// render node
			nodeRenderer.renderNode(out,
						this,
						startNode,
						editUrl,
						editNodeId,
						renderMode);
		} else {

			logger.log(Level.WARNING,
				   "No renderer for mode {0}, node {1}",
				   new Object[] { renderMode, this.getId() });
		}
	}

	public void createTemplateRelationship(final Template template) {

		// create a relationship to the given template node
		Command createRel = Services.command(securityContext,
			CreateRelationshipCommand.class);

		createRel.execute(this,
				  template,
				  RelType.USE_TEMPLATE);
	}

	/**
	 * Render a node-specific inline edit view as html
	 *
	 * @param out
	 * @param node
	 * @param editUrl
	 * @param editNodeId
	 */
	public void renderEditView(StructrOutputStream out, final AbstractNode startNode, final String editUrl,
				   final Long editNodeId) {

		if (getId() == editNodeId.longValue()) {

			renderEditFrame(out,
					editUrl);
		}
	}

	/**
	 * Render an IFRAME element with the given editor URL inside
	 *
	 * @param out
	 * @param editUrl
	 */
	public void renderEditFrame(StructrOutputStream out, final String editUrl) {

		// create IFRAME with given URL
		out.append("<iframe style=\"border: 1px solid #ccc; background-color: #fff\" src=\"").append(
		    editUrl).append("\" width=\"100%\" height=\"100%\"").append("></iframe>");
	}

	/**
	 * Implement standard toString() method
	 */
	@Override
	public String toString() {

		/*
		 * StringBuilder out = new StringBuilder();
		 *
		 * out.append(getName()).append(" [").append(getId()).append("]: ");
		 *
		 * List<String> props = new LinkedList<String>();
		 *
		 * for (String key : getPropertyKeys()) {
		 *
		 *       Object value = getProperty(key);
		 *
		 *       if (value != null) {
		 *
		 *               String displayValue = "";
		 *
		 *               if (value.getClass().isPrimitive()) {
		 *                       displayValue = value.toString();
		 *               } else if (value.getClass().isArray()) {
		 *
		 *                       if (value instanceof byte[]) {
		 *                               displayValue = new String((byte[]) value);
		 *                       } else if (value instanceof char[]) {
		 *                               displayValue = new String((char[]) value);
		 *                       } else if (value instanceof double[]) {
		 *
		 *                               Double[] values = ArrayUtils.toObject((double[]) value);
		 *
		 *                               displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
		 *
		 *                       } else if (value instanceof float[]) {
		 *
		 *                               Float[] values = ArrayUtils.toObject((float[]) value);
		 *
		 *                               displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
		 *
		 *                       } else if (value instanceof short[]) {
		 *
		 *                               Short[] values = ArrayUtils.toObject((short[]) value);
		 *
		 *                               displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
		 *
		 *                       } else if (value instanceof long[]) {
		 *
		 *                               Long[] values = ArrayUtils.toObject((long[]) value);
		 *
		 *                               displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
		 *
		 *                       } else if (value instanceof int[]) {
		 *
		 *                               Integer[] values = ArrayUtils.toObject((int[]) value);
		 *
		 *                               displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
		 *
		 *                       } else if (value instanceof boolean[]) {
		 *
		 *                               Boolean[] values = (Boolean[]) value;
		 *
		 *                               displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
		 *
		 *                       } else if (value instanceof byte[]) {
		 *                               displayValue = new String((byte[]) value);
		 *                       } else {
		 *
		 *                               Object[] values = (Object[]) value;
		 *
		 *                               displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
		 *                       }
		 *
		 *               } else {
		 *
		 *                       if(!(value instanceof AbstractNode)) {
		 *                               displayValue = value.toString();
		 *                       } else {
		 *                               displayValue = "AbstractNode";
		 *                       }
		 *               }
		 *
		 *               props.add("\"" + key + "\"" + " : " + "\"" + displayValue + "\"");
		 *       }
		 * }
		 *
		 * out.append("{ ").append(StringUtils.join(props.toArray(), " , ")).append(" }");
		 *
		 * return out.toString();
		 */
		return "AbstractNode";
	}

	/**
	 * Write this node as an array of strings
	 */
	public String[] toStringArray() {

		List<String> props = new LinkedList<String>();

		for (String key : getPropertyKeys()) {

			Object value        = getProperty(key);
			String displayValue = "";

			if (value.getClass().isPrimitive()) {
				displayValue = value.toString();
			} else if (value.getClass().isArray()) {

				if (value instanceof byte[]) {
					displayValue = new String((byte[]) value);
				} else if (value instanceof char[]) {
					displayValue = new String((char[]) value);
				} else if (value instanceof double[]) {

					Double[] values = ArrayUtils.toObject((double[]) value);

					displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

				} else if (value instanceof float[]) {

					Float[] values = ArrayUtils.toObject((float[]) value);

					displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

				} else if (value instanceof short[]) {

					Short[] values = ArrayUtils.toObject((short[]) value);

					displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

				} else if (value instanceof long[]) {

					Long[] values = ArrayUtils.toObject((long[]) value);

					displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

				} else if (value instanceof int[]) {

					Integer[] values = ArrayUtils.toObject((int[]) value);

					displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

				} else if (value instanceof boolean[]) {

					Boolean[] values = (Boolean[]) value;

					displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

				} else if (value instanceof byte[]) {
					displayValue = new String((byte[]) value);
				} else {

					Object[] values = (Object[]) value;

					displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
				}

			} else {
				displayValue = value.toString();
			}

			props.add(displayValue);
		}

		return (String[]) props.toArray(new String[props.size()]);
	}

	/**
	 * Discard changes and overwrite the properties map with the values
	 * from database
	 */
	public void discard() {

		// TODO: Implement the full pattern with commit(), discard(), init() etc.
	}

	/**
	 * Commit unsaved property values to the database node.
	 */
	public void commit(final User user) {

		isDirty = false;

		// Create an outer transaction to combine any inner neo4j transactions
		// to one single transaction
		Command transactionCommand = Services.command(securityContext,
			TransactionCommand.class);

		transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				Command createNode = Services.command(securityContext,
					CreateNodeCommand.class);
				AbstractNode s     = (AbstractNode) createNode.execute(user);

				init(securityContext,
				     s);

				Set<String> keys = properties.keySet();

				for (String key : keys) {

					Object value = properties.get(key);

					if ((key != null) && (value != null)) {

						setProperty(key,
							    value,
							    false);    // Don't update index now!
					}
				}

				return null;
			}

		});
	}

	/**
	 * Test: Evaluate BeanShell script in this text node.
	 *
	 * @return the output
	 */
	public String evaluate(HttpServletRequest request) {
		return ("");
	}

	/**
	 * Populate the security relationship cache map
	 */
	private void populateSecurityRelationshipCacheMap() {

		if (securityRelationships == null) {
			securityRelationships = new HashMap<Long, StructrRelationship>();
		}

		// Fill cache map
		for (StructrRelationship r : getRelationships(RelType.SECURITY, Direction.INCOMING)) {

			securityRelationships.put(r.getStartNode().getId(),
						  r);
		}
	}

	/**
	 * Check if given node may be read by current user.
	 *
	 * @return
	 */
	public boolean readAllowed() {

		// Check global settings first
		if (isVisible()) {
			return true;
		}

		// Then check per-user permissions
		return hasPermission(StructrRelationship.Permission.read.name(),
				     user);
	}

	/**
	 * Check if given node may see the navigation tree
	 *
	 * @return
	 */
	public boolean showTreeAllowed() {

		return hasPermission(StructrRelationship.Permission.showTree.name(),
				     user);
	}

	/**
	 * Check if given node may be written by current user.
	 *
	 * @return
	 */
	public boolean writeAllowed() {

		return hasPermission(StructrRelationship.Permission.showTree.name(),
				     user);
	}

	/**
	 * Check if given user may create new sub nodes.
	 *
	 * @return
	 */
	public boolean createSubnodeAllowed() {

		return hasPermission(StructrRelationship.Permission.createNode.name(),
				     user);
	}

	/**
	 * Check if given user may delete this node
	 *
	 * @return
	 */
	public boolean deleteNodeAllowed() {

		return hasPermission(StructrRelationship.Permission.deleteNode.name(),
				     user);
	}

	/**
	 * Check if given user may add new relationships to this node
	 *
	 * @return
	 */
	public boolean addRelationshipAllowed() {

		return hasPermission(StructrRelationship.Permission.addRelationship.name(),
				     user);
	}

	/**
	 * Check if given user may edit (set) properties of this node
	 *
	 * @return
	 */
	public boolean editPropertiesAllowed() {

		return hasPermission(StructrRelationship.Permission.editProperties.name(),
				     user);
	}

	/**
	 * Check if given user may remove relationships to this node
	 *
	 * @return
	 */
	public boolean removeRelationshipAllowed() {

		return hasPermission(StructrRelationship.Permission.removeRelationship.name(),
				     user);
	}

	/**
	 * Check if access of given node may be controlled by current user.
	 *
	 * @return
	 */
	public boolean accessControlAllowed() {

		// just in case ...
		if (user == null) {
			return false;
		}

		// superuser
		if (user instanceof SuperUser) {
			return true;
		}

		// node itself
		if (this.equals(user)) {
			return true;
		}

		StructrRelationship r = null;

		// owner has always access control
		if (user.equals(getOwnerNode())) {
			return true;
		}

		r = getSecurityRelationship(user);

		if ((r != null) && r.isAllowed(StructrRelationship.Permission.accessControl.name())) {
			return true;
		}

		return false;
	}

	/**
	 * Replace $(key) by the content rendered by the subnode with name "key"
	 *
	 * @param content
	 * @param node
	 * @param editUrl
	 * @param editNodeId
	 */
	public void replaceBySubnodes(HttpServletRequest request, StringBuilder content, final AbstractNode startNode,
				      final String editUrl, final Long editNodeId) {

		List<AbstractNode> subnodes               = null;
		List<AbstractNode> subnodesAndLinkedNodes = null;

		template = startNode.getTemplate();

		AbstractNode callingNode = null;

		if ((template != null) && (this instanceof Template)) {

			callingNode = template.getCallingNode();

			if (callingNode != null) {

				subnodesAndLinkedNodes = callingNode.getSortedDirectChildAndLinkNodes();
				subnodes               = callingNode.getSortedDirectChildNodes();
			}

		} else {

			subnodesAndLinkedNodes = getSortedDirectChildAndLinkNodes();
			subnodes               = getSortedDirectChildNodes();
		}

		Command findNode = Services.command(securityContext,
			FindNodeCommand.class);
		int start        = content.indexOf(NODE_KEY_PREFIX);

		while (start > -1) {

			int end = content.indexOf(NODE_KEY_SUFFIX,
						  start + NODE_KEY_PREFIX.length());

			if (end < 0) {

				logger.log(Level.WARNING,
					   "Node key suffix {0} not found in template {1}",
					   new Object[] { NODE_KEY_SUFFIX, template.getName() });

				break;
			}

			String key              = content.substring(start + NODE_KEY_PREFIX.length(),
				end);
			int indexOfComma        = key.indexOf(",");
			int indexOfDot          = key.indexOf(".");
			String templateKey      = null;
			String methodKey        = null;
			Template customTemplate = null;

			if (indexOfComma > 0) {

				String[] splitted = StringUtils.split(key,
					",");

				key         = splitted[0];
				templateKey = splitted[1];

				if (StringUtils.isNotEmpty(templateKey)) {
					customTemplate = (Template) findNode.execute(this, new XPath(templateKey));
				}

			} else if (indexOfDot > 0) {

				String[] splitted = StringUtils.split(key,
					".");

				key       = splitted[0];
				methodKey = splitted[1];
			}

			// StringBuilder replacement = new StringBuilder();
			StructrOutputStream replacement = new StructrOutputStream(request,
				securityContext);

			if ((callingNode != null) && key.equals(SUBNODES_KEY)) {

				// render subnodes in correct order
				for (AbstractNode s : subnodes) {

					// propagate request and template
					// s.setRequest(request);
					s.renderNode(replacement,
						     startNode,
						     editUrl,
						     editNodeId);
				}
			} else if ((callingNode != null) && key.equals(SUBNODES_AND_LINKED_NODES_KEY)) {

				// render subnodes in correct order
				for (AbstractNode s : subnodesAndLinkedNodes) {

					// propagate request and template
					// s.setRequest(request);
					s.renderNode(replacement,
						     startNode,
						     editUrl,
						     editNodeId);
				}
			} else {

				// if (key.startsWith("/") || key.startsWith("count(")) {
				// use XPath notation
				// search relative to calling node
				// List<AbstractNode> nodes = (List<AbstractNode>) findNode.execute(callingNode, new XPath(key));
				// Object result = findNode.execute(this, new XPath(key));
				Object result = findNode.execute(this,
								 key);

				if (result instanceof List) {

					// get referenced nodes relative to the template
					List<AbstractNode> nodes = (List<AbstractNode>) result;

					if (nodes != null) {

						for (AbstractNode s : nodes) {

							if (customTemplate != null) {
								s.setTemplate(customTemplate);
							}

							// propagate request
							// s.setRequest(getRequest());
							s.renderNode(replacement,
								     startNode,
								     editUrl,
								     editNodeId);
						}
					}
				} else if (result instanceof AbstractNode) {

					AbstractNode s = (AbstractNode) result;

					if (customTemplate != null) {
						s.setTemplate(customTemplate);
					}

					// propagate request
					// s.setRequest(getRequest());
					if (StringUtils.isNotEmpty(methodKey)) {

						methodKey = toGetter(methodKey);

						Method getter = null;

						try {

							getter = s.getClass().getMethod(methodKey);

							Object value = null;

							try {

								value = getter.invoke(s);
								replacement.append(value);

							} catch (Exception ex) {

								logger.log(Level.FINE,
									   "Cannot invoke method {0} on {1}",
									   new Object[] { getter, s });
							}

						} catch (Exception ex) {

							logger.log(Level.FINE,
								   "Cannot invoke method {0}",
								   methodKey);
						}

					} else {

						s.renderNode(replacement,
							     startNode,
							     editUrl,
							     editNodeId);
					}

				} else {
					replacement.append(result);
				}
			}

			String replaceBy = replacement.toString();

			content.replace(start,
					end + NODE_KEY_SUFFIX.length(),
					replaceBy);

			// avoid replacing in the replacement again
			start = content.indexOf(NODE_KEY_PREFIX, start + replaceBy.length() + 1);
		}
	}

	private RelationshipType[] splitRelationshipTypes(String relTypes) {

		if (relTypes != null) {

			List<RelationshipType> relTypeList = new ArrayList<RelationshipType>(10);

			for (String type : relTypes.split("[, ]+")) {
				relTypeList.add(DynamicRelationshipType.withName(type));
			}

			return (relTypeList.toArray(new RelationshipType[0]));
		}

		return (null);
	}

	/**
	 * Recursively add Html / JavaScript nodes to the springy graph
	 *
	 * @param buffer
	 * @param currentNode
	 * @param depth
	 * @param maxDepth
	 */
	private void collectRelatedNodes(Set<AbstractNode> nodes, Set<StructrRelationship> rels,
					 Set<AbstractNode> visitedNodes, AbstractNode currentNode, int depth,
					 int maxDepth, int maxNum, RelationshipType... relTypes) {

		if (depth >= maxDepth) {
			return;
		}

		if (nodes.size() < maxNum) {

			nodes.add(currentNode);

			// collect incoming relationships
			List<StructrRelationship> inRels = new LinkedList<StructrRelationship>();

			if ((relTypes != null) && (relTypes.length > 0)) {

				for (RelationshipType type : relTypes) {
					inRels.addAll(currentNode.getRelationships(type, Direction.INCOMING));
				}

			} else {
				inRels = currentNode.getIncomingRelationships();
			}

			for (StructrRelationship rel : inRels) {

				AbstractNode startNode = rel.getStartNode();

				if ((startNode != null) && (nodes.size() < maxNum)) {

					nodes.add(startNode);
					rels.add(rel);
					collectRelatedNodes(nodes,
							    rels,
							    visitedNodes,
							    startNode,
							    depth + 1,
							    maxDepth,
							    maxNum,
							    relTypes);
				}
			}

			// collect outgoing relationships
			List<StructrRelationship> outRels = new LinkedList<StructrRelationship>();

			if ((relTypes != null) && (relTypes.length > 0)) {

				for (RelationshipType type : relTypes) {
					outRels.addAll(currentNode.getRelationships(type, Direction.OUTGOING));
				}

			} else {
				outRels = currentNode.getOutgoingRelationships();
			}

			for (StructrRelationship rel : outRels) {

				AbstractNode endNode = rel.getEndNode();

				if ((endNode != null) && (nodes.size() < maxNum)) {

					nodes.add(endNode);
					rels.add(rel);
					collectRelatedNodes(nodes,
							    rels,
							    visitedNodes,
							    endNode,
							    depth + 1,
							    maxDepth,
							    maxNum,
							    relTypes);
				}
			}

			// visitedNodes.add(currentNode);
		}
	}

	// ----- protected methods -----
	public static String toGetter(String name) {
		return "get".concat(name.substring(0, 1).toUpperCase()).concat(name.substring(1));
	}

	public void replaceByFreeMarker(HttpServletRequest request, final String templateString, Writer out,
					final AbstractNode startNode, final String editUrl, final Long editNodeId) {

		Configuration cfg = new Configuration();

		// TODO: enable access to content tree, see below (Content variable)
		// cfg.setSharedVariable("Tree", new StructrTemplateNodeModel(this));
		try {

			AbstractNode callingNode = null;

			if (getTemplate() != null) {
				callingNode = template.getCallingNode();
			} else {
				callingNode = startNode;
			}

			Map root = new HashMap();

			root.put("this",
				 this);
			root.put("StartNode",
				 startNode);

			// just for convenience
			root.put("node",
				 startNode);

			if (callingNode != null) {

				root.put(callingNode.getType(),
					 callingNode);
				root.put("CallingNode",
					 callingNode);
			}

			if (request != null) {

				// root.put("Request", new freemarker.template.SimpleHash(request.getParameterMap().));
				HttpRequestParametersHashModel requestModel =
					new HttpRequestParametersHashModel(request);

				root.put("Request",
					 requestModel);
				root.put("ContextPath",
					 request.getContextPath());

				String searchString = Search.clean(request.getParameter("search"));

				if ((searchString != null) &&!(searchString.isEmpty())) {

					root.put("SearchString",
						 searchString);

					List<AbstractNode> result = RequestHelper.retrieveSearchResult(securityContext,
						request);

					root.put("SearchResults",
						 result);
				}
			}

			if (user != null) {

				root.put("User",
					 user);
			}

			// Add a generic helper
			root.put("Helper",
				 new TemplateHelper());

			// Add path helper / finder
			root.put("path",
				 new PathHelper(securityContext));

			// Add error and ok message if present
			HttpSession session = securityContext.getSession();

			if (session != null) {

				if (session.getAttribute("errorMessage") != null) {

					root.put("ErrorMessage",
						 session.getAttribute("errorMessage"));
				}

				if (session.getAttribute("okMessage") != null) {

					root.put("OkMessage",
						 session.getAttribute("okMessage"));
				}
			}

			// root.put("Content", new StructrTemplateNodeModel(this, startNode, editUrl, editNodeId, user));
			// root.put("ContextPath", callingNode.getNodePath(startNode));
			String name                    = (template != null)
							 ? template.getName()
							 : getName();
			freemarker.template.Template t = new freemarker.template.Template(name,
				new StringReader(templateString),
				cfg);

			t.process(root,
				  out);
			out.flush();

		} catch (Throwable t) {

			logger.log(Level.WARNING,
				   "Error: {0}",
				   t.getMessage());
		}
	}

	// ----- protected methods -----
	protected String createUniqueIdentifier(String prefix) {

		StringBuilder identifier = new StringBuilder(100);

		identifier.append(prefix);
		identifier.append(getIdString());

		return identifier.toString();
	}

	/**
	 * Returns an Iterable that lazily traverses the node tree depth first with the given parameters.
	 *
	 * @param relType the relationship type to follow
	 * @param direction the direction of the relationship to follow
	 * @param evaluator the evaluator that decides how the traversal will be done
	 *
	 * @return an Iterable of the nodes found in the traversal
	 */
	protected Iterable<Node> traverseDepthFirst(final RelationshipType relType, final Direction direction,
		Evaluator evaluator) {

		return (Traversal.description().depthFirst().relationships(relType,
			direction).evaluator(evaluator).traverse(dbNode).nodes());
	}

	/**
	 * Returns an Iterable that lazily traverses the node tree breadth first with the given parameters.
	 *
	 * @param relType the relationship type to follow
	 * @param direction the direction of the relationship to follow
	 * @param evaluator the evaluator that decides how the traversal will be done
	 *
	 * @return an Iterable of the nodes found in the traversal
	 */
	protected Iterable<Node> traverseBreadthFirst(final RelationshipType relType, final Direction direction,
		Evaluator evaluator) {

		return (Traversal.description().breadthFirst().relationships(relType,
			direction).evaluator(evaluator).traverse(dbNode).nodes());
	}

	/**
	 * Returns the number of elements in the given Iterable
	 *
	 * @param iterable
	 * @return the number of elements in the given iterable
	 */
	protected int countIterableElements(Iterable iterable) {

		int count = 0;

		for (Object o : iterable) {
			count++;
		}

		return (count);
	}

	@Override
	public boolean delete() {

		dbNode.delete();

		return true;
	}

	/**
	 * Can be used to permit the setting of a read-only
	 * property once. The lock will be restored automatically
	 * after the next setProperty operation. This method exists
	 * to prevent automatic set methods from setting a read-only
	 * property while allowing a manual set method to override this
	 * default behaviour.
	 */
	public void unlockReadOnlyPropertiesOnce() {
		this.readOnlyPropertiesUnlocked = true;
	}

	//~--- get methods ----------------------------------------------------

	/**
	 * Returns the icon src attribute for this node type.
	 *
	 * @return the icon src attribute, for use in the HTML img element
	 */
	public abstract String getIconSrc();

	public Long getTemplateId() {

		Template n = getTemplate();

		return ((n != null)
			? n.getId()
			: null);
	}

	/**
	 * Wrapper for toString method
	 * @return
	 */
	public String getAllProperties() {
		return toString();
	}

	/**
	 * Render a node-specific view (binary)
	 *
	 * Should be overwritten by any node which holds binary content
	 * public void renderNode(StructrOutputStream out, final AbstractNode startNode,
	 * final String editUrl, final Long editNodeId) {
	 *
	 * try {
	 * if (isVisible()) {
	 * out.write(getName().getBytes());
	 * }
	 * } catch (IOException e) {
	 * logger.log(Level.SEVERE, "Could not write node name to output stream: {0}", e.getStackTrace());
	 * }
	 * }
	 */

	/**
	 * Get this node's template
	 *
	 * @return
	 */
	public Template getTemplate() {

		long t0 = System.currentTimeMillis();

		if (this instanceof Template) {

			template = (Template) this;

			return template;
		}

		if (template != null) {

//                      long t1 = System.currentTimeMillis();
			logger.log(Level.FINE,
				   "Cached template found");

			return template;
		}

		// TODO: move to command and avoid to use the graph db interface directly
//              Iterable<Node> nodes = Traversal.description().relationships(RelType.HAS_CHILD, Direction.INCOMING).traverse(dbNode).nodes();
		AbstractNode startNode = this;

		while ((startNode != null) &&!(startNode.isRootNode())) {

			List<StructrRelationship> templateRelationships =
				startNode.getRelationships(RelType.USE_TEMPLATE,
							   Direction.OUTGOING);

			if ((templateRelationships != null) &&!(templateRelationships.isEmpty())) {

				template = (Template) templateRelationships.get(0).getEndNode();

				return template;
			}

			if (template == null) {

				startNode = startNode.getParentNode();

				continue;
			}
		}

		long t1 = System.currentTimeMillis();

		logger.log(Level.FINE,
			   "No template found in {0} ms",
			   (t1 - t0));

		return null;
	}

	/**
	 * Get type from underlying db node If no type property was found, return
	 * info
	 */
	@Override
	public String getType() {
		return (String) getProperty(Key.type.name());
	}

	/**
	 * Return node's title, or if title is null, name
	 */
	public String getTitleOrName() {

		String title = getTitle();

		return (title != null)
		       ? title
		       : getName();
	}

	/**
	 * Get name from underlying db node
	 *
	 * If name is null, return node id as fallback
	 */
	public String getName() {

		Object nameProperty = getProperty(Key.name.name());

		if (nameProperty != null) {
			return (String) nameProperty;
		} else {
			return getNodeId().toString();
		}
	}

	/**
	 * Get categories
	 */
	public String[] getCategories() {
		return (String[]) getProperty(Key.categories.name());
	}

	/**
	 * Return title dependend of locale
	 *
	 * @param locale
	 * @return
	 */
	public String getTitle(final Locale locale) {
		return (String) getProperty(getTitleKey(locale));
	}

	/**
	 * Return title
	 */
	public String getTitle() {

//              logger.log(Level.FINE, "Title without locale requested.");
//              return getTitle(new Locale("en"));
		return getStringProperty(Key.title.name());
	}

	public static String getTitleKey(final Locale locale) {
		return Key.title.name() + "_" + locale;
	}

	/**
	 * Get titles from underlying db node
	 */
	public List<Title> getTitles() {

		List<Title> titleList = new LinkedList<Title>();

		for (Locale locale : Locale.getAvailableLocales()) {

			String title = (String) getProperty(getTitleKey(locale));

			if (title != null) {

				titleList.add(new Title(locale,
							title));
			}
		}

		return titleList;
	}

	/**
	 * Get id from underlying db
	 */
	@Override
	public long getId() {

		if (isDirty) {
			return -1;
		}

		return dbNode.getId();
	}

	public Long getNodeId() {
		return getId();
	}

	public String getIdString() {
		return Long.toString(getId());
	}

//      public Long getId() {
//          return getId();
//      }
	public Date getDateProperty(final String key) {

		Object propertyValue = getProperty(key);

		if (propertyValue != null) {

			if (propertyValue instanceof Date) {
				return (Date) propertyValue;
			} else if (propertyValue instanceof Long) {
				return new Date((Long) propertyValue);
			} else if (propertyValue instanceof String) {

				try {

					// try to parse as a number
					return new Date(Long.parseLong((String) propertyValue));
				} catch (NumberFormatException nfe) {

					try {

						Date date = DateUtils.parseDate(((String) propertyValue),
										new String[] {
											"yyyy-MM-dd'T'HH:mm:ssZ",
											"yyyy-MM-dd'T'HH:mm:ss",
											"yyyymmdd", "yyyymm", "yyyy" });

						return date;

					} catch (ParseException ex2) {

						logger.log(Level.WARNING,
							   "Could not parse " + propertyValue + " to date",
							   ex2);
					}

					logger.log(Level.WARNING,
						   "Can''t parse String {0} to a Date.",
						   propertyValue);

					return null;
				}

			} else {

				logger.log(Level.WARNING,
					   "Date property is not null, but type is neither Long nor String, returning null");

				return null;
			}
		}

		return null;
	}

	public String getCreatedBy() {
		return (String) getProperty(Key.createdBy.name());
	}

	public Long getPosition() {

		Object p = getProperty(Key.position.name());
		Long pos;

		if (p != null) {

			if (p instanceof Long) {
				return (Long) p;
			} else if (p instanceof Integer) {

				try {

					pos = Long.parseLong(p.toString());

					// convert old String-based position property
					setPosition(pos);

				} catch (NumberFormatException e) {

					pos = getId();

					return pos;
				}

			} else if (p instanceof String) {

				try {

					pos = Long.parseLong(((String) p));

					// convert old String-based position property
					setPosition(pos);

				} catch (NumberFormatException e) {

					pos = getId();

					return pos;
				}

			} else {

				logger.log(Level.SEVERE,
					   "Position property not stored as Integer or String: {0}",
					   p.getClass().getName());
			}
		}

		// If no value is in the database, write the actual id once
		long id = getId();

		logger.log(Level.FINE,
			   "No position property in database, writing id {0} to database",
			   id);
		setPosition(id);

		return id;
	}

	public boolean getVisibleToPublicUsers() {
		return getBooleanProperty(Key.visibleToPublicUsers.name());
	}

	public boolean getVisibleToAuthenticatedUsers() {
		return getBooleanProperty(Key.visibleToAuthenticatedUsers.name());
	}

	public boolean getHidden() {
		return getBooleanProperty(Key.hidden.name());
	}

	public boolean getDeleted() {
		return getBooleanProperty(Key.deleted.name());
	}

	/**
	 * Return a map with all properties of this node. Caution, this method can
	 * not be used to retrieve the full map of persistent properties. Use
	 * {@see #getPropertyKeys} instead.
	 *
	 * @return
	 */
	public Map<String, Object> getPropertyMap() {
		return properties;
	}

	/**
	 * Return the property signature of a node
	 *
	 * @return
	 */
	public Map<String, Class> getSignature() {

		Map<String, Class> signature = new HashMap<String, Class>();

		for (String key : getPropertyKeys()) {

			Object prop = getProperty(key);

			if (prop != null) {

				signature.put(key,
					      prop.getClass());
			}
		}

		return signature;
	}

	/**
	 * Return all property keys of the underlying database node
	 *
	 * @return
	 */
	public final Iterable<String> getDatabasePropertyKeys() {

		if (dbNode == null) {
			return null;
		}

		return dbNode.getPropertyKeys();
	}

	/**
	 * Return all property keys.
	 *
	 * @return
	 */
	public Iterable<String> getPropertyKeys() {
		return getPropertyKeys(PropertyView.All);
	}

	/**
	 * Depending on the given value, return different set of properties for
	 * a node.
	 *
	 * F.e. in Public mode, only the properties suitable for public users should
	 * be returned.
	 *
	 * This method should be overwritten in any subclass where you want to
	 * hide certain properties in different view modes.
	 *
	 * @param propertyView
	 * @return
	 */
	@Override
	public Iterable<String> getPropertyKeys(final PropertyView propertyView) {

		return EntityContext.getPropertySet(this.getClass(),
			propertyView);
	}

	public Object getProperty(final PropertyKey propertyKey) {
		return (getProperty(propertyKey.name()));
	}

	/**
	 * Return property value which is used for indexing.
	 *
	 * This is useful f.e. to filter markup from HTML to index only text
	 *
	 * @param key
	 * @return
	 */
	public Object getPropertyForIndexing(final String key) {
		return getProperty(key);
	}

	@Override
	public Object getProperty(final String key) {

		boolean idRequested = false;
		Object value        = null;
		Class type          = this.getClass();

		// ----- BEGIN property group resolution -----
		PropertyGroup propertyGroup = EntityContext.getPropertyGroup(type, key);
		if(propertyGroup != null) {
			return propertyGroup.getGroupedProperties(this);
		}
		// ----- END property group resolution -----

		// ----- BEGIN automatic property resolution (check for static relationships and return related nodes) -----
		String singularType = getSingularTypeName(key);
		if (key.endsWith("Id")) {

			// remove "Id" from the end of the value and set "id requested"-flag
			singularType = singularType.substring(0, singularType.length() - 2);
			idRequested  = true;
		}

		if ((singularType != null) && EntityContext.getRelations(type).containsKey(singularType.toLowerCase())) {

			// static relationship detected, return related nodes
			// (we omit null check here because containsKey ensures that rel is not null)
			DirectedRelationship rel = EntityContext.getRelations(type).get(singularType.toLowerCase());

			if (idRequested) {

				GraphObject node = rel.getRelatedNode(securityContext,
					this,
					singularType);

				if (node != null) {
					return node.getId();
				} else {

					logger.log(Level.FINE,
						   "Related node not found for key {0}",
						   key);
				}

			} else {

				// apply notion (default is "as-is")
				Notion notion = rel.getNotion();

				// return collection or single element depending on cardinality of relationship
				switch (rel.getCardinality()) {

					case ManyToMany :
					case OneToMany :
						return new IterableAdapter(rel.getRelatedNodes(securityContext, this, singularType), notion.getAdapterForGetter(securityContext));

					case OneToOne :
					case ManyToOne :
						return notion.getAdapterForGetter(securityContext).adapt(rel.getRelatedNode(securityContext, this, singularType));
				}
			}
		}

		// ----- END automatic property resolution -----

		if (isDirty) {
			value = properties.get(key);
		}

		// Temporary hook for format conversion introduced with 0.4.3-SNAPSHOT:
		// public -> visibleToPublicUsers (due to usage of enum Permission public instead of public static final String PUBLIC = "public"
		// TODO: remove this hook if you can be absolutely sure that no old repository is in use anymore!
		if (key.equals(Key.visibleToPublicUsers.name()) && dbNode.hasProperty("public")) {

			final Object oldValue          = dbNode.getProperty("public");
			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					dbNode.setProperty(Key.visibleToPublicUsers.name(),
							   oldValue);
					dbNode.removeProperty("public");

					return null;
				}
			};

			// execute transaction
			Services.command(securityContext,
					 TransactionCommand.class).execute(transaction);

			// debug
			if (transaction.getCause() != null) {

				logger.log(Level.WARNING,
					   "Error while setting property",
					   transaction.getCause());
			}
		}

		if ((key != null) && (dbNode != null) && dbNode.hasProperty(key)) {
			value = dbNode.getProperty(key);
		}

		PropertyConverter converter = EntityContext.getPropertyConverter(securityContext,
			type,
			key);

		if (converter != null) {

			Value conversionValue = EntityContext.getPropertyConversionParameter(type,
				key);

			value = converter.convertForGetter(value, conversionValue);
		}

		return value;
	}

	public String getStringProperty(final PropertyKey propertyKey) {
		return (getStringProperty(propertyKey.name()));
	}

	public String getPropertyMD5(final String key) {

		Object value = getProperty(key);

		if (value instanceof String) {
			return DigestUtils.md5Hex((String) value);
		} else if (value instanceof byte[]) {
			return DigestUtils.md5Hex((byte[]) value);
		}

		logger.log(Level.WARNING,
			   "Could not create MD5 hex out of value {0}",
			   value);

		return null;
	}

	public String getStringProperty(final String key) {

		Object propertyValue = getProperty(key);
		String result        = null;

		if (propertyValue == null) {
			return null;
		}

		if (propertyValue instanceof String) {
			result = ((String) propertyValue);
		}

		return result;
	}

	public List<String> getStringListProperty(final PropertyKey propertyKey) {
		return (getStringListProperty(propertyKey.name()));
	}

	public List<String> getStringListProperty(final String key) {

		Object propertyValue = getProperty(key);
		List<String> result  = new LinkedList<String>();

		if (propertyValue == null) {
			return null;
		}

		if (propertyValue instanceof String) {

			// Split by carriage return / line feed
			String[] values = StringUtils.split(((String) propertyValue),
				"\r\n");

			result = Arrays.asList(values);
		} else if (propertyValue instanceof String[]) {

			String[] values = (String[]) propertyValue;

			result = Arrays.asList(values);
		}

		return result;
	}

	public String getStringArrayPropertyAsString(final PropertyKey propertyKey) {
		return (getStringArrayPropertyAsString(propertyKey.name()));
	}

	public String getStringArrayPropertyAsString(final String key) {

		Object propertyValue = getProperty(key);
		StringBuilder result = new StringBuilder();

		if (propertyValue instanceof String[]) {

			int i           = 0;
			String[] values = (String[]) propertyValue;

			for (String value : values) {

				result.append(value);

				if (i < values.length - 1) {
					result.append("\r\n");
				}
			}
		}

		return result.toString();
	}

	public Integer getIntProperty(final PropertyKey propertyKey) {
		return (getIntProperty(propertyKey.name()));
	}

	public Integer getIntProperty(final String key) {

		Object propertyValue = getProperty(key);
		Integer result       = null;

		if (propertyValue == null) {
			return null;
		}

		if (propertyValue instanceof Integer) {
			result = ((Integer) propertyValue);
		} else if (propertyValue instanceof String) {

			if ("".equals((String) propertyValue)) {
				return null;
			}

			result = Integer.parseInt(((String) propertyValue));
		}

		return result;
	}

	public Long getLongProperty(final PropertyKey propertyKey) {
		return (getLongProperty(propertyKey.name()));
	}

	public Long getLongProperty(final String key) {

		Object propertyValue = getProperty(key);
		Long result          = null;

		if (propertyValue == null) {
			return null;
		}

		if (propertyValue instanceof Long) {
			result = ((Long) propertyValue);
		} else if (propertyValue instanceof String) {

			if ("".equals((String) propertyValue)) {
				return null;
			}

			result = Long.parseLong(((String) propertyValue));
		}

		return result;
	}

	public Double getDoubleProperty(final PropertyKey propertyKey) {
		return (getDoubleProperty(propertyKey.name()));
	}

	public Double getDoubleProperty(final String key) {

		Object propertyValue = getProperty(key);
		Double result        = null;

		if (propertyValue == null) {
			return null;
		}

		if (propertyValue instanceof Double) {

			Double doubleValue = (Double) propertyValue;

			if (doubleValue.equals(Double.NaN)) {

				// clean NaN values from database
				setProperty(key,
					    null);

				return null;
			}

			result = doubleValue.doubleValue();

		} else if (propertyValue instanceof String) {

			if ("".equals((String) propertyValue)) {
				return null;
			}

			result = Double.parseDouble(((String) propertyValue));
		}

		return result;
	}

	public boolean getBooleanProperty(final PropertyKey propertyKey) {
		return (getBooleanProperty(propertyKey.name()));
	}

	public boolean getBooleanProperty(final String key) {

		Object propertyValue = getProperty(key);
		Boolean result       = false;

		if (propertyValue == null) {
			return Boolean.FALSE;
		}

		if (propertyValue instanceof Boolean) {
			result = ((Boolean) propertyValue).booleanValue();
		} else if (propertyValue instanceof String) {
			result = Boolean.parseBoolean(((String) propertyValue));
		}

		return result;
	}

	/**
	 * Return database node
	 *
	 * @return
	 */
	public Node getNode() {
		return dbNode;
	}

//
//      /**
//       * Render a minimal html header
//       *
//       * @param out
//       */
//      protected void renderHeader(StringBuilder out) {
//              out.append("<html><head><title>").append(getName()).append(" (Domain)</title></head><body>");
//      }
//
//      /**
//       * Render a minimal html footer
//       *
//       * @param out
//       */
//      protected void renderFooter(StringBuilder out) {
//              out.append("</body></html>");
//      }

	/**
	 * Return a relative URL according to RFC 1808
	 *
	 * @param node
	 * @return
	 */
	public String getNodeURL(final AbstractNode node) {

		String nodePath = getNodePath(node);

		if (nodePath.equals(".")) {
			return "";
		}

		if (nodePath.startsWith("../")) {
			return nodePath.substring(3);
		}

		return nodePath;
	}

	/*
	 * @Override
	 * public int compareTo(AbstractNode otherNode) {
	 * return this.getPosition().compareTo(otherNode.getPosition());
	 * }
	 */

	/**
	 * Get path relative to given node
	 *
	 * @param node
	 * @return
	 */
	public String getNodePath(final AbstractNode node) {

		// clicked node as reference
		String refPath = node.getNodePath();

		// currently rendered node, the link target
		String thisPath = this.getNodePath();

		// Both not working :-(
		// String combinedPath = FilenameUtils.concat(thisPath, refPath);
		// String combinedPath = new java.io.File(refPath).toURI().relativize(new java.io.File(thisPath).toURI()).getPath();
		String combinedPath = PathHelper.getRelativeNodePath(refPath,
			thisPath);

		logger.log(Level.FINE,
			   "{0} + {1} = {2}",
			   new Object[] { thisPath, refPath, combinedPath });

		return combinedPath;

//              String[] refParts  = refPath.split("/");
//              String[] thisParts = thisPath.split("/");
//              int level          = refParts.length - thisParts.length;
//
//              if (level == 0) {
//
//                      // paths are identical, return last part
//                      return thisParts[thisParts.length - 1];
//              } else if (level < 0) {
//
//                      // link down
////                      return thisPath.substring(refPath.length());
//                      // Bug fix: Don't include the leading "/", this is a relative path!
//                      return thisPath.substring(refPath.length() + 1);
//              } else {
//
//                      // link up
//                      int i      = 0;
//                      String ret = "";
//
//                      do {
//                              ret = ret.concat("../");
//                      } while (++i < level);
//
//                      return ret.concat(thisParts[thisParts.length - 1]);
//              }
	}

	/**
	 * Get path relative to this node
	 *
	 * @param node
	 * @return
	 */
	public String getRelativeNodePath() {
		return getNodePath(this);
	}

	/**
	 * Assemble path for given node.
	 *
	 * This is an inverse method of @getNodeByIdOrPath.
	 *
	 * @param node
	 * @param renderMode
	 * @return
	 */

	/*
	 * public String getNodePath(final AbstractNode node, final Enum renderMode) {
	 *
	 * Command nodeFactory = Services.command(securityContext, NodeFactoryCommand.class);
	 * AbstractNode n = (AbstractNode) nodeFactory.execute(node);
	 * return n.getNodePath();
	 * }
	 */

	/**
	 * Assemble absolute path for given node.
	 *
	 * @return
	 */
	public String getNodePath() {

		String path = "";

		// get actual database node
		AbstractNode node = this;

		// create bean node
//              Command nodeFactory = Services.command(securityContext, NodeFactoryCommand.class);
//              AbstractNode n = (AbstractNode) nodeFactory.execute(node);
		// stop at root node
		while ((node != null) && (node.getId() > 0)) {

			path = node.getName() + (!("".equals(path))
						 ? "/" + path
						 : "");
			node = node.getParentNode();

			// check parent nodes
//                      Relationship r = node.getSingleRelationship(RelType.HAS_CHILD, Direction.INCOMING);
//                      if (r != null) {
//                          node = r.getStartNode();
//                          n = (AbstractNode) nodeFactory.execute(node);
//                      }
		}

		return "/".concat(path);    // add leading slash, because we always include the root node
	}

	/**
	 * Assemble absolute path for given node.
	 *
	 * @return
	 */
	public String getNodeXPath() {

		String xpath = "";

		// get actual database node
		AbstractNode node = this;

		// create bean node
//              Command nodeFactory = Services.command(securityContext, NodeFactoryCommand.class);
//              AbstractNode n = (AbstractNode) nodeFactory.execute(node);
		// stop at root node
		while ((node != null) && (node.getId() > 0)) {

			xpath = node.getType() + "[@name='" + node.getName() + "']" + (!("".equals(xpath))
				? "/" + xpath
				: "");

			// check parent nodes
			node = node.getParentNode();
		}

		return "/".concat(xpath);    // add leading slash, because we always include the root node
	}

//
//      /**
//       * Default: Return this node's name
//       *
//       * @param user
//       * @param renderMode
//       * @param contextPath
//       * @return
//       */
//      public String getUrlPart() {
//          return getName();
//      }

	/**
	 * Return null mime type. Method has to be overwritten,
	 * returning real mime type
	 */
	public String getContentType() {
		return null;
	}

	/**
	 * Return the (cached) incoming relationship between this node and the
	 * given principal which holds the security information.
	 *
	 * @param principal
	 * @return incoming security relationship
	 */
	@Override
	public StructrRelationship getSecurityRelationship(final Principal principal) {

		if (principal == null) {
			return null;
		}

		long userId = principal.getId();

		if (securityRelationships == null) {
			securityRelationships = new HashMap<Long, StructrRelationship>();
		}

		if (!(securityRelationships.containsKey(userId))) {
			populateSecurityRelationshipCacheMap();
		}

		return securityRelationships.get(userId);
	}

	/**
	 * Return all relationships of given type and direction
	 *
	 * @return list with relationships
	 */
	@Override
	public List<StructrRelationship> getRelationships(RelationshipType type, Direction dir) {

		return (List<StructrRelationship>) Services.command(securityContext,
			NodeRelationshipsCommand.class).execute(this,
			type,
			dir);
	}

	/**
	 * Return statistical information on all relationships of this node
	 *
	 * @return number of relationships
	 */
	@Override
	public Map<RelationshipType, Long> getRelationshipInfo(Direction dir) {

		return (Map<RelationshipType, Long>) Services.command(securityContext,
			NodeRelationshipStatisticsCommand.class).execute(this,
			dir);
	}

//
//      /**
//       * Return true if this node has child nodes visible for current user
//       *
//       * @return
//       */
//      public boolean hasChildren() {
//          List<StructrRelationship> childRels = getOutgoingChildRelationships();
//          List<StructrRelationship> linkRels = getOutgoingLinkRelationships();
//          return (linkRels != null && !(linkRels.isEmpty())
//                  && childRels != null && !(childRels.isEmpty()));
////          return (hasRelationship(RelType.HAS_CHILD, Direction.OUTGOING)
////                  || hasRelationship(RelType.LINK, Direction.OUTGOING));
//      }

	/**
	 * Return unordered list of all direct child nodes (no recursion)
	 *
	 * @return list with structr nodes
	 */
	public List<AbstractNode> getDirectChildNodes() {
		return getDirectChildren(RelType.HAS_CHILD);
	}

	/**
	 * Return unordered list of all direct child nodes (no recursion)
	 * Ignores permissions
	 *
	 * @return list with structr nodes
	 */
	public List<AbstractNode> getDirectChildNodesIgnorePermissions() {
		return getDirectChildrenIgnorePermissions(RelType.HAS_CHILD);
	}

	/**
	 * Return the first parent node found.
	 *
	 * @return
	 */
	public AbstractNode getParentNode() {

		List<AbstractNode> parentNodes = getParentNodes();

		if ((parentNodes != null) &&!(parentNodes.isEmpty())) {
			return parentNodes.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Return the first parent node found.
	 *
	 * Ignores permissions.
	 *
	 * @return
	 */
	public AbstractNode getParentNodeIgnorePermissions() {

		List<AbstractNode> parentNodes = getParentNodesIgnorePermissions();

		if ((parentNodes != null) &&!(parentNodes.isEmpty())) {
			return parentNodes.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Return next ancestor which defines a local context for this node.
	 *
	 * @return
	 */
	public AbstractNode getContextNode() {

		List<AbstractNode> ancestors = getAncestorNodes();

		// If node has no ancestors, itself is its context node
		if (ancestors.isEmpty()) {
			return this;
		}

		// Return root node
		return ancestors.get(0);
	}

	/**
	 * Return a path of this node relative to the next ancestor
	 * which defines a local context.
	 *
	 * @return
	 */
	public String getContextPath() {

		// This is the default
		return getNodePath();
	}

	/**
	 * Return sibling nodes. Follows the HAS_CHILD relationship
	 *
	 * @return
	 */
	public List<AbstractNode> getSiblingNodes() {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();
		AbstractNode parentNode  = getParentNode();

		if (parentNode != null) {

			Command nodeFactory            = Services.command(securityContext,
				NodeFactoryCommand.class);
			Command relsCommand            = Services.command(securityContext,
				NodeRelationshipsCommand.class);
			List<StructrRelationship> rels = (List<StructrRelationship>) relsCommand.execute(parentNode,
				RelType.HAS_CHILD,
				Direction.OUTGOING);

			for (StructrRelationship r : rels) {

				AbstractNode s = (AbstractNode) nodeFactory.execute(r.getEndNode());

				if (securityContext.isAllowed(s, Permission.Read)) {
					nodes.add(s);
				}
			}
		}

		return nodes;
	}

	/**
	 * Return all ancestor nodes. Follows the INCOMING HAS_CHILD relationship
	 * and stops at the root node.
	 *
	 * Ignores permissions.
	 *
	 * @return
	 */
	public List<AbstractNode> getAncestorNodes() {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();
		AbstractNode node        = getParentNodeIgnorePermissions();

		do {

			nodes.add(node);
			node = node.getParentNodeIgnorePermissions();

		} while ((node != null) &&!node.isRootNode());

		if (node != null) {

			// Finally add root node
			nodes.add(node);
		}

		return nodes;
	}

	/**
	 * Return parent nodes. Follows the INCOMING HAS_CHILD relationship and
	 * needs read permissions.
	 *
	 * @return
	 */
	public List<AbstractNode> getParentNodes() {

		List<AbstractNode> nodes       = new LinkedList<AbstractNode>();
		Command nodeFactory            = Services.command(securityContext,
			NodeFactoryCommand.class);
		List<StructrRelationship> rels = getIncomingChildRelationships();

		for (StructrRelationship r : rels) {

			AbstractNode s = (AbstractNode) nodeFactory.execute(r.getStartNode());

			if (securityContext.isAllowed(s, Permission.Read)) {
				nodes.add(s);
			}
		}

		return nodes;
	}

	/**
	 * Return parent nodes. Follows the INCOMING HAS_CHILD relationship.
	 *
	 * Ignores permissions.
	 *
	 * @return
	 */
	public List<AbstractNode> getParentNodesIgnorePermissions() {

		List<AbstractNode> nodes       = new LinkedList<AbstractNode>();
		Command nodeFactory            = Services.command(securityContext,
			NodeFactoryCommand.class);
		List<StructrRelationship> rels = getIncomingChildRelationships();

		for (StructrRelationship r : rels) {

			AbstractNode s = (AbstractNode) nodeFactory.execute(r.getStartNode());

			nodes.add(s);
		}

		return nodes;
	}

	/**
	 * Cached list of all relationships
	 *
	 * @return
	 */
	public List<StructrRelationship> getRelationships() {

		if (allRelationships == null) {
			allRelationships = getRelationships(null, Direction.BOTH);
		}

		return allRelationships;
	}

	/**
	 * Get all relationships of given direction
	 *
	 * @return
	 */
	public List<StructrRelationship> getRelationships(Direction dir) {

		return getRelationships(null,
					dir);
	}

	/**
	 * Cached list of incoming relationships
	 *
	 * @return
	 */
	public List<StructrRelationship> getIncomingRelationships() {

		if (incomingRelationships == null) {
			incomingRelationships = getRelationships(null, Direction.INCOMING);
		}

		return incomingRelationships;
	}

	/**
	 * Cached list of outgoing relationships
	 *
	 * @return
	 */
	public List<StructrRelationship> getOutgoingRelationships() {

		if (outgoingRelationships == null) {
			outgoingRelationships = getRelationships(null, Direction.OUTGOING);
		}

		return outgoingRelationships;
	}

	public Iterable<AbstractNode> getDataNodes(HttpServletRequest request) {

		// this is the default implementation
		return getDirectChildNodes();
	}

	/**
	 * Non-cached list of outgoing relationships
	 *
	 * @return
	 */
	public List<StructrRelationship> getOutgoingRelationships(final RelationshipType type) {

		return getRelationships(type,
					Direction.OUTGOING);
	}

	/**
	 * Cached list of incoming link relationships
	 *
	 * @return
	 */
	public List<StructrRelationship> getIncomingLinkRelationships() {

		if (incomingLinkRelationships == null) {
			incomingLinkRelationships = getRelationships(RelType.LINK, Direction.INCOMING);
		}

		return incomingLinkRelationships;
	}

	/**
	 * Cached list of outgoing data relationships
	 *
	 * @return
	 */
	public List<StructrRelationship> getOutgoingDataRelationships() {

		if (outgoingDataRelationships == null) {
			outgoingDataRelationships = getRelationships(RelType.DATA, Direction.OUTGOING);
		}

		return outgoingDataRelationships;
	}

	/**
	 * Cached list of incoming data relationships
	 *
	 * @return
	 */
	public List<StructrRelationship> getIncomingDataRelationships() {

		if (incomingDataRelationships == null) {
			incomingDataRelationships = getRelationships(RelType.DATA, Direction.INCOMING);
		}

		return incomingDataRelationships;
	}

	/**
	 * Cached list of outgoing link relationships
	 *
	 * @return
	 */
	public List<StructrRelationship> getOutgoingLinkRelationships() {

		if (outgoingLinkRelationships == null) {
			outgoingLinkRelationships = getRelationships(RelType.LINK, Direction.OUTGOING);
		}

		return outgoingLinkRelationships;
	}

	/**
	 * Cached list of incoming child relationships
	 *
	 * @return
	 */
	public List<StructrRelationship> getIncomingChildRelationships() {

		if (incomingChildRelationships == null) {
			incomingChildRelationships = getRelationships(RelType.HAS_CHILD, Direction.INCOMING);
		}

		return incomingChildRelationships;
	}

	/**
	 * Cached list of outgoing child relationships
	 *
	 * @return
	 */
	public List<StructrRelationship> getOutgoingChildRelationships() {

		if (outgoingChildRelationships == null) {
			outgoingChildRelationships = getRelationships(RelType.HAS_CHILD, Direction.OUTGOING);
		}

		return outgoingChildRelationships;
	}

	/**
	 * Return unordered list of all directly linked nodes (no recursion)
	 *
	 * @return list with structr nodes
	 */
	public List<AbstractNode> getLinkedNodes() {
		return getDirectChildren(RelType.LINK);
	}

	/**
	 * Return ordered list of all directly linked nodes (no recursion)
	 *
	 * @return list with structr nodes
	 */
	public List<AbstractNode> getSortedLinkedNodes() {
		return getSortedDirectChildren(RelType.LINK);
	}

	/**
	 * Return unordered list of all child nodes (recursively)
	 *
	 * @return list with structr nodes
	 */
	public List<AbstractNode> getAllChildren() {
		return getAllChildren(null);
	}

	public List<AbstractNode> getAllChildrenForRemotePush() {

		// FIXME: add handling for remote user here
		Command findNode = Services.command(securityContext,
			FindNodeCommand.class);

		return ((List<AbstractNode>) findNode.execute(this));
	}

	public int getRemotePushSize(final int chunkSize) {

		Command findNode        = Services.command(securityContext,
			FindNodeCommand.class);
		List<AbstractNode> list = ((List<AbstractNode>) findNode.execute(this));
		int size                = 0;

		for (AbstractNode node : list) {

			if (node instanceof File) {

				File file = (File) node;

				size += (file.getSize() / chunkSize);
				size += 3;

			} else {
				size++;
			}

			List<StructrRelationship> rels = node.getOutgoingRelationships();

			for (StructrRelationship r : rels) {

				if (list.contains(r.getStartNode()) && list.contains(r.getEndNode())) {
					size++;
				}
			}
		}

		return (size);
	}

	/**
	 * Return unordered list of all direct child nodes (no recursion)
	 * with given relationship type
	 *
	 * @return list with structr nodes
	 */
	public List<AbstractNode> getDirectChildren(final RelationshipType relType) {

		return getDirectChildren(relType,
					 null);
	}

	/**
	 * Return unordered list of all direct child nodes (no recursion)
	 * with given relationship type
	 * Ignores permissions
	 *
	 * @return list with structr nodes
	 */
	public List<AbstractNode> getDirectChildrenIgnorePermissions(final RelationshipType relType) {

		return getDirectChildrenIgnorePermissions(relType,
			null);
	}

	/**
	 * Return ordered list of all direct child nodes (no recursion)
	 * with given relationship type
	 *
	 * @return list with structr nodes
	 */
	public List<AbstractNode> getSortedDirectChildren(final RelationshipType relType) {

		List<AbstractNode> nodes = getDirectChildren(relType,
			null);

		Collections.sort(nodes);

		return nodes;
	}

	/**
	 * Return unordered list of all direct child nodes (no recursion)
	 * with given relationship type and given node type.
	 *
	 * Given user must have read access.
	 *
	 * @return list with structr nodes
	 */
	public List<AbstractNode> getDirectChildren(final RelationshipType relType, final String nodeType) {

		List<StructrRelationship> rels = this.getOutgoingRelationships(relType);
		List<AbstractNode> nodes       = new LinkedList<AbstractNode>();

		for (StructrRelationship r : rels) {

			AbstractNode s = r.getEndNode();

			if (securityContext.isAllowed(s, Permission.Read)
				&& ((nodeType == null) || nodeType.equals(s.getType()))) {
				nodes.add(s);
			}
		}

		return nodes;
	}

	/**
	 * Return unordered list of all direct child nodes (no recursion)
	 * with given relationship type and given node type.
	 *
	 * Ignores permissions
	 *
	 * @return list with structr nodes
	 */
	public List<AbstractNode> getDirectChildrenIgnorePermissions(final RelationshipType relType,
		final String nodeType) {

		List<StructrRelationship> rels = this.getOutgoingRelationships(relType);

//              SecurityContext securityContext = CurrentRequest.getSecurityContext();
		List<AbstractNode> nodes = new LinkedList<AbstractNode>();

		for (StructrRelationship r : rels) {

			AbstractNode s = r.getEndNode();

			if ((nodeType == null) || nodeType.equals(s.getType())) {
				nodes.add(s);
			}
		}

		return nodes;
	}

	/**
	 * Get child nodes and sort them before returning
	 *
	 * @return
	 */
	public List<AbstractNode> getSortedDirectChildNodes() {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();

		nodes.addAll(getDirectChildNodes());

		// sort by position
		Collections.sort(nodes,
				 new Comparator<AbstractNode>() {

			@Override
			public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
				return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
			}

		});

		return nodes;
	}

	/**
	 * Get child nodes and sort them before returning
	 * Ignores permissions
	 *
	 * @return
	 */
	public List<AbstractNode> getSortedDirectChildNodesIgnorePermissions() {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();

		nodes.addAll(getDirectChildNodesIgnorePermissions());

		// sort by position
		Collections.sort(nodes,
				 new Comparator<AbstractNode>() {

			@Override
			public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
				return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
			}

		});

		return nodes;
	}

	/**
	 * Get child nodes and sort them before returning
	 *
	 * @return
	 */
	public List<AbstractNode> getSortedDirectChildNodes(final String sortKey, final String sortOrder) {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();

		nodes.addAll(getDirectChildNodes());

		// sort by key, order by order {@see AbstractNodeComparator.ASCENDING} or {@see AbstractNodeComparator.DESCENDING}
		Collections.sort(nodes,
				 new AbstractNodeComparator(sortKey,
			sortOrder));

		return nodes;
	}

	/**
	 * Get direct child nodes, link nodes, and sort them before returning
	 *
	 * @return
	 */
	public List<AbstractNode> getDirectChildAndLinkNodes() {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();

		nodes.addAll(getDirectChildNodes());

		// get linked child nodes
		nodes.addAll(getLinkedNodes());

		return nodes;
	}

	/**
	 * Get direct child nodes, link nodes, and sort them before returning
	 *
	 * @return
	 */
	public List<AbstractNode> getSortedDirectChildAndLinkNodes() {

		List<AbstractNode> nodes = getDirectChildAndLinkNodes();

		// sort by position
		Collections.sort(nodes,
				 new Comparator<AbstractNode>() {

			@Override
			public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
				return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
			}

		});

		return nodes;
	}

	/**
	 * Get menu items and sort them before returning.
	 *
	 * @return
	 */
	public List<AbstractNode> getSortedMenuItems() {

		List<AbstractNode> menuItems = new LinkedList<AbstractNode>();

		// add direct children of type MenuItem
		menuItems.addAll(getDirectChildren(RelType.HAS_CHILD, "MenuItem"));

		// add linked children, f.e. direct links to pages
		menuItems.addAll(getDirectChildren(RelType.LINK));

		// sort by position
		Collections.sort(menuItems,
				 new Comparator<AbstractNode>() {

			@Override
			public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
				return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
			}

		});

		return menuItems;
	}

	/**
	 * Return unordered list of all child nodes (recursively)
	 * with given relationship type and given node type.
	 *
	 * Given user must have read access.
	 *
	 * @param nodeType node type filter, can be null
	 * @param user
	 * @return list with structr nodes
	 */
	protected List<AbstractNode> getAllChildren(final String nodeType) {

		List<AbstractNode> nodes  = new LinkedList<AbstractNode>();
		Command findNode          = Services.command(securityContext,
			FindNodeCommand.class);
		List<AbstractNode> result = (List<AbstractNode>) findNode.execute(this);

		for (AbstractNode s : result) {

			if (securityContext.isAllowed(s, Permission.Read)
				&& ((nodeType == null) || nodeType.equals(s.getType()))) {
				nodes.add(s);
			}
		}

		return nodes;
	}

	/**
	 * Return owner node
	 *
	 * @return
	 */
	@Override
	public User getOwnerNode() {

		for (StructrRelationship s : getRelationships(RelType.OWNS, Direction.INCOMING)) {

			AbstractNode n = s.getStartNode();

			if (n instanceof User) {
				return (User) n;
			}

			logger.log(Level.SEVERE,
				   "Owner node is not a user: {0}[{1}]",
				   new Object[] { n.getName(), n.getId() });
		}

		return null;
	}

	public Long getOwnerId() {
		return getOwnerNode().getId();
	}

	/**
	 * Return owner
	 *
	 * @return
	 */
	public String getOwner() {

		User ownner = getOwnerNode();

		return ((ownner != null)
			? ownner.getRealName() + " (" + ownner.getName() + ")"
			: null);
	}

	/**
	 * Return a list with the connected principals (user, group, role)
	 * @return
	 */
	public List<AbstractNode> getSecurityPrincipals() {

		List<AbstractNode> principalList = new LinkedList<AbstractNode>();

		// check any security relationships
		for (StructrRelationship r : getRelationships(RelType.SECURITY, Direction.INCOMING)) {

			// check security properties
			AbstractNode principalNode = r.getEndNode();

			principalList.add(principalNode);
		}

		return principalList;
	}

	/**
	 * Generic getter for use with Freemarker template language
	 *
	 * @param key
	 * @return
	 */
	public Object get(final String key) {

		if (key == null) {
			return null;
		}

		Object propertyValue = this.getProperty(key);

		if (propertyValue != null) {
			return propertyValue;
		}

		List<AbstractNode> subnodes = this.getDirectChildAndLinkNodes();

		for (AbstractNode node : subnodes) {

			if (key.equals(node.getName())) {
				return node;
			}
		}

		// nothing found
		return null;
	}

	public Set<AbstractNode> getRelatedNodes(int maxDepth) {

		return (getRelatedNodes(maxDepth,
					20 /* Integer.MAX_VALUE */,
					null));
	}

	public Set<AbstractNode> getRelatedNodes(int maxDepth, String relTypes) {

		return (getRelatedNodes(maxDepth,
					20 /* Integer.MAX_VALUE */,
					relTypes));
	}

	public Set<AbstractNode> getRelatedNodes(int maxDepth, int maxNum) {

		return (getRelatedNodes(maxDepth,
					maxNum,
					null));
	}

	public Set<AbstractNode> getRelatedNodes(int maxDepth, int maxNum, String relTypes) {

		Set<AbstractNode> visitedNodes = new LinkedHashSet<AbstractNode>();
		Set<AbstractNode> nodes        = new LinkedHashSet<AbstractNode>();
		Set<StructrRelationship> rels  = new LinkedHashSet<StructrRelationship>();

		collectRelatedNodes(nodes,
				    rels,
				    visitedNodes,
				    this,
				    0,
				    maxDepth,
				    maxNum,
				    splitRelationshipTypes(relTypes));

		return (nodes);
	}

	public Set<StructrRelationship> getRelatedRels(int maxDepth) {

		return (getRelatedRels(maxDepth,
				       20 /* Integer.MAX_VALUE */,
				       null));
	}

	public Set<StructrRelationship> getRelatedRels(int maxDepth, String relTypes) {

		return (getRelatedRels(maxDepth,
				       20 /* Integer.MAX_VALUE */,
				       relTypes));
	}

	public Set<StructrRelationship> getRelatedRels(int maxDepth, int maxNum) {

		return (getRelatedRels(maxDepth,
				       maxNum,
				       null));
	}

	public Set<StructrRelationship> getRelatedRels(int maxDepth, int maxNum, String relTypes) {

		Set<AbstractNode> visitedNodes = new LinkedHashSet<AbstractNode>();
		Set<AbstractNode> nodes        = new LinkedHashSet<AbstractNode>();
		Set<StructrRelationship> rels  = new LinkedHashSet<StructrRelationship>();

		collectRelatedNodes(nodes,
				    rels,
				    visitedNodes,
				    this,
				    0,
				    maxDepth,
				    maxNum,
				    splitRelationshipTypes(relTypes));

		return (rels);
	}

	protected AbstractNode getNodeFromLoader(HttpServletRequest request) {

		List<StructrRelationship> rels = getIncomingDataRelationships();
		AbstractNode node              = null;

		for (StructrRelationship rel : rels) {

			// first one wins
			AbstractNode startNode = rel.getStartNode();

			if (startNode instanceof NodeSource) {

				NodeSource source = (NodeSource) startNode;

				node = source.loadNode(request);

				break;
			}
		}

		return node;
	}

	@Override
	public Date getVisibilityStartDate() {
		return getDateProperty(Key.visibilityStartDate.name());
	}

	@Override
	public Date getVisibilityEndDate() {
		return getDateProperty(Key.visibilityEndDate.name());
	}

	@Override
	public Date getCreatedDate() {
		return getDateProperty(Key.createdDate.name());
	}

	@Override
	public Date getLastModifiedDate() {
		return getDateProperty(Key.lastModifiedDate.name());
	}

	public AbstractComponent getHelpContent() {
		return (null);
	}

	@Override
	public Long getStartNodeId() {
		return null;
	}

	@Override
	public Long getEndNodeId() {
		return null;
	}

	private String getSingularTypeName(String key) {

		return (key.endsWith("ies")
			? key.substring(0,
					key.length() - 3).concat("y")
			: (key.endsWith("s")
			   ? key.substring(0,
					   key.length() - 1)
			   : key));
	}

	public boolean hasTemplate() {
		return (getTemplate() != null);
	}

	/**
	 * Return true if this node has a relationship of given type and direction.
	 *
	 * @param type
	 * @param dir
	 * @return
	 */
	public boolean hasRelationship(final RelType type, final Direction dir) {

		List<StructrRelationship> rels = this.getRelationships(type,
			dir);

		return ((rels != null) &&!(rels.isEmpty()));
	}

	/**
	 * Return true if this node has child nodes
	 *
	 * @return
	 */
	public boolean hasChildren() {

		return (hasRelationship(RelType.HAS_CHILD,
					Direction.OUTGOING) || hasRelationship(RelType.LINK,
			Direction.OUTGOING));
	}

	// ----- interface AccessControllable -----
	@Override
	public boolean hasPermission(final String permission, final Principal principal) {

		if (principal == null) {
			return false;
		}

		// just in case ...
		if (permission == null) {
			return false;
		}

		// superuser
		if (principal instanceof SuperUser) {
			return true;
		}

		// user has full control over his/her own user node
		if (this.equals(principal)) {
			return true;
		}

		StructrRelationship r = getSecurityRelationship(principal);

		if ((r != null) && r.isAllowed(permission)) {
			return true;
		}

		// Check group
		// We cannot use getParent() here because it uses hasPermission itself,
		// that would lead to an infinite loop
		List<StructrRelationship> rels = principal.getIncomingChildRelationships();

		for (StructrRelationship sr : rels) {

			AbstractNode node = sr.getStartNode();

			if (!(node instanceof Group)) {
				continue;
			}

			Group group = (Group) node;

			r = getSecurityRelationship(group);

			if ((r != null) && r.isAllowed(permission)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isVisibleToPublicUsers() {
		return getVisibleToPublicUsers();
	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {
		return getBooleanProperty(Key.visibleToAuthenticatedUsers.name());
	}

	@Override
	public boolean isNotHidden() {
		return !getHidden();
	}

	@Override
	public boolean isHidden() {
		return getHidden();
	}

	// ----- end interface AccessControllable -----
	public boolean isNotDeleted() {
		return !getDeleted();
	}

	public boolean isDeleted() {

		boolean hasDeletedFlag = getDeleted();
		boolean isInTrash      = isInTrash();

		return hasDeletedFlag || isInTrash;
	}

	/**
	 * Return true if node is the root node
	 *
	 * @return
	 */
	public boolean isRootNode() {
		return getId() == 0;
	}

	/**
	 *
	 * Returns true if an ancestor node is a Trash node
	 */
	public boolean isInTrash() {

		return (countIterableElements(traverseDepthFirst(RelType.HAS_CHILD, Direction.INCOMING,
			new Evaluator() {

			@Override
			public Evaluation evaluate(Path path) {

				Node node = path.endNode();

				// check for type property with value "Trash"
				if (node.hasProperty(Key.type.name())
					&& node.getProperty(Key.type.name()).equals("Trash")) {

					// only include Trash nodes in result set
					return (Evaluation.INCLUDE_AND_PRUNE);
				} else {

					// else just continue
					return (Evaluation.EXCLUDE_AND_CONTINUE);
				}
			}

		})) > 0);
	}

	public boolean isVisible() {
		return securityContext.isVisible(this);
	}

	//~--- set methods ----------------------------------------------------

	/**
	 * Check visibility of given node, used for rendering in view mode
	 *
	 * @return
	 * public boolean isVisible() {
	 *
	 *       if (user instanceof SuperUser) {
	 *
	 *               // Super user may always see it
	 *               return true;
	 *       }
	 *
	 *       // check hidden flag (see STRUCTR-12)
	 *       if (isHidden()) {
	 *               return false;
	 *       }
	 *
	 *       boolean visibleByTime = false;
	 *
	 *       // check visibility period of time (see STRUCTR-13)
	 *       Date visStartDate       = getVisibilityStartDate();
	 *       long effectiveStartDate = 0L;
	 *       Date createdDate        = getCreatedDate();
	 *
	 *       if (createdDate != null) {
	 *               effectiveStartDate = Math.max(createdDate.getTime(), 0L);
	 *       }
	 *
	 *       // if no start date for visibility is given,
	 *       // take the maximum of 0 and creation date.
	 *       visStartDate = ((visStartDate == null)
	 *                       ? new Date(effectiveStartDate)
	 *                       : visStartDate);
	 *
	 *       // if no end date for visibility is given,
	 *       // take the Long.MAX_VALUE
	 *       Date visEndDate = getVisibilityEndDate();
	 *
	 *       visEndDate = ((visEndDate == null)
	 *                     ? new Date(Long.MAX_VALUE)
	 *                     : visEndDate);
	 *
	 *       Date now = new Date();
	 *
	 *       visibleByTime = (now.after(visStartDate) && now.before(visEndDate));
	 *
	 *       if (user == null) {
	 *
	 *               // No logged-in user
	 *               if (visibleToPublicUsers()) {
	 *                       return visibleByTime;
	 *               } else {
	 *                       return false;
	 *               }
	 *       } else {
	 *
	 *               // Logged-in users
	 *               if (isVisibleToAuthenticatedUsers()) {
	 *                       return visibleByTime;
	 *               } else {
	 *                       return false;
	 *               }
	 *       }
	 * }
	 */
	public void setTemplate(final Template template) {
		this.template = template;
	}

	public void setTemplateId(final Long value) {

		// find template node
		Command findNode      = Services.command(securityContext,
			FindNodeCommand.class);
		Template templateNode = (Template) findNode.execute(value);

		// delete existing template relationships
		List<StructrRelationship> templateRels = this.getOutgoingRelationships(RelType.USE_TEMPLATE);
		Command delRel                         = Services.command(securityContext,
			DeleteRelationshipCommand.class);

		if (templateRels != null) {

			for (StructrRelationship r : templateRels) {
				delRel.execute(r);
			}
		}

		// create new link target relationship
		Command createRel = Services.command(securityContext,
			CreateRelationshipCommand.class);

		createRel.execute(this,
				  templateNode,
				  RelType.USE_TEMPLATE);
	}

	public void setCreatedBy(final String createdBy) {

		setProperty(Key.createdBy.name(),
			    createdBy);
	}

	public void setCreatedDate(final Date date) {

		setProperty(Key.createdDate.name(),
			    date);
	}

	public void setLastModifiedDate(final Date date) {

		setProperty(Key.lastModifiedDate.name(),
			    date);
	}

	public void setVisibilityStartDate(final Date date) {

		setProperty(Key.visibilityStartDate.name(),
			    date);
	}

	public void setVisibilityEndDate(final Date date) {

		setProperty(Key.visibilityEndDate.name(),
			    date);
	}

	public void setPosition(final Long position) {

		setProperty(Key.position.name(),
			    position);
	}

	public void setVisibleToPublicUsers(final boolean publicFlag) {

		setProperty(Key.visibleToPublicUsers.name(),
			    publicFlag);
	}

	public void setVisibleToAuthenticatedUsers(final boolean flag) {

		setProperty(Key.visibleToAuthenticatedUsers.name(),
			    flag);
	}

	public void setHidden(final boolean hidden) {

		setProperty(Key.hidden.name(),
			    hidden);
	}

	public void setDeleted(final boolean deleted) {

		setProperty(Key.deleted.name(),
			    deleted);
	}

	public void setType(final String type) {

		setProperty(Key.type.name(),
			    type);
	}

	public void setName(final String name) {

		setProperty(Key.name.name(),
			    name);
	}

	public void setCategories(final String[] categories) {

		setProperty(Key.categories.name(),
			    categories);
	}

	public void setTitle(final String title) {

		setProperty(Key.title.name(),
			    title);
	}

	/**
	 * Multiple titles (one for each language)
	 *
	 * @param title
	 */
	public void setTitles(final String[] titles) {

		setProperty(Key.titles.name(),
			    titles);
	}

	public void setId(final Long id) {

		// setProperty(NODE_ID_KEY, id);
		// not allowed
	}

	public void setNodeId(final Long id) {

		// setProperty(NODE_ID_KEY, id);
		// not allowed
	}

	public void setProperty(final PropertyKey propertyKey, final Object value) {

		setProperty(propertyKey.name(),
			    value);
	}

	/**
	 * Set a property in database backend without updating index
	 *
	 * Set property only if value has changed
	 *
	 * @param key
	 * @param convertedValue
	 */
	@Override
	public void setProperty(final String key, final Object value) {

		setProperty(key,
			    value,
			    updateIndexDefault);
	}

	public void setPropertyAsStringArray(final PropertyKey propertyKey, final String value) {

		setPropertyAsStringArray(propertyKey.name(),
					 value);
	}

	/**
	 * Split String value and set as String[] property in database backend
	 *
	 * @param key
	 * @param stringList
	 *
	 */
	public void setPropertyAsStringArray(final String key, final String value) {

		String[] values = StringUtils.split(((String) value),
			"\r\n");

		setProperty(key,
			    values,
			    updateIndexDefault);
	}

	public void setProperty(final PropertyKey propertyKey, final Object value, final boolean updateIndex) {

		setProperty(propertyKey.name(),
			    value,
			    updateIndex);
	}

	/**
	 * Set a property in database backend
	 *
	 * Set property only if value has changed
	 *
	 * Update index only if updateIndex is true
	 *
	 * @param key
	 * @param convertedValue
	 * @param updateIndex
	 */
	public void setProperty(final String key, final Object value, final boolean updateIndex) {

		Class type = this.getClass();

		if (key == null) {

			logger.log(Level.SEVERE,
				   "Tried to set property with null key (action was denied)");

			throw new IllegalArgumentException("Property key '".concat(key).concat("' is null."));

		}

		// check for read-only properties
		if (EntityContext.isReadOnlyProperty(type, key)) {

			if (readOnlyPropertiesUnlocked) {

				// permit write operation once and
				// lock read-only properties again
				readOnlyPropertiesUnlocked = false;
			} else {
				throw new IllegalArgumentException("Property '".concat(key).concat("' is read-only."));
			}
		}

		// ----- BEGIN property group resolution -----
		PropertyGroup propertyGroup = EntityContext.getPropertyGroup(type, key);
		if(propertyGroup != null) {
			propertyGroup.setGroupedProperties(value, this);
			return;
		}
		// ----- END property group resolution -----

		String singularType = getSingularTypeName(key);
		if (key.endsWith("Id")) {
			singularType = singularType.substring(0, singularType.length() - 2);
		}

		// check for static relationships and connect node
		if (EntityContext.getRelations(type).containsKey(singularType.toLowerCase())) {

			// static relationship detected, create relationship
			DirectedRelationship rel = EntityContext.getRelations(type).get(singularType.toLowerCase());
			if (rel != null) {

				try {

					GraphObject graphObject = rel.getNotion().getAdapterForSetter(securityContext).adapt(value);
					rel.createRelationship(securityContext, this, graphObject, singularType);

					return;

				} catch(IllegalArgumentException iaex) {

					// re-throw exception
					throw iaex;

				} catch (Throwable t) {

					// logger.log(Level.WARNING, "Exception in setProperty", t);

					// report exception upwards
					throw new IllegalArgumentException(t.getMessage());
				}
			}
		}

		PropertyConverter converter = EntityContext.getPropertyConverter(securityContext,
			type,
			key);
		final Object convertedValue;

		if (converter != null) {

			Value conversionValue = EntityContext.getPropertyConversionParameter(type,
				key);

			convertedValue = converter.convertForSetter(value, conversionValue);

		} else {
			convertedValue = value;
		}

		// look for validator
		PropertyValidator validator = EntityContext.getPropertyValidator(securityContext,
			type,
			key);

		if (validator != null) {

			logger.log(Level.FINE,
				   "Using validator of type {0} for property {1}",
				   new Object[] { validator.getClass().getSimpleName(), key });

			Value parameter         = EntityContext.getPropertyValidationParameter(type,
				key);
			ErrorBuffer errorBuffer = new ErrorBuffer();

			if (!validator.isValid(key, convertedValue, parameter, errorBuffer)) {
				throw new IllegalArgumentException(errorBuffer.toString());
			}
		}

		if (isDirty) {

			// Don't write directly to database, but store property values
			// in a map for later use
			properties.put(key,
				       convertedValue);
		} else {

			// Commit value directly to database
			Object oldValue = getProperty(key);

			// don't make any changes if
			// - old and new value both are null
			// - old and new value are not null but equal
			if (((convertedValue == null) && (oldValue == null))
				|| ((convertedValue != null) && (oldValue != null)
				    && convertedValue.equals(oldValue))) {
				return;
			}

			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					// save space
					if (convertedValue == null) {
						dbNode.removeProperty(key);
					} else {

						// Setting last modified date explicetely is not allowed
						if (!key.equals(Key.lastModifiedDate.name())) {

							if (convertedValue instanceof Date) {

								dbNode.setProperty(key,
										   ((Date) convertedValue).getTime());

							} else {

								dbNode.setProperty(key,
										   convertedValue);

								// set last modified date if not already happened
								dbNode.setProperty(Key.lastModifiedDate.name(),
										   (new Date()).getTime());
							}

						} else {

							logger.log(Level.FINE,
								   "Tried to set lastModifiedDate explicitely (action was denied)");
						}
					}

					// Don't automatically update index
					// TODO: Implement something really fast to keep the index automatically in sync
					if (updateIndex && dbNode.hasProperty(key)) {

						Services.command(securityContext,
								 IndexNodeCommand.class).execute(getId(),
							key);
					}

					return null;
				}
			};

			// execute transaction
			Services.command(securityContext,
					 TransactionCommand.class).execute(transaction);

			// debug
			if (transaction.getCause() != null) {

				logger.log(Level.WARNING,
					   "Error while setting property",
					   transaction.getCause());
			}
		}
	}

	@Override
	public void removeProperty(final String key) {

		if(this.dbNode != null) {

			if (key == null) {

				logger.log(Level.SEVERE,
					   "Tried to set property with null key (action was denied)");

				return;
			}

			// check for read-only properties
			if (EntityContext.isReadOnlyProperty(this.getClass(), key)) {

				if (readOnlyPropertiesUnlocked) {

					// permit write operation once and
					// lock read-only properties again
					readOnlyPropertiesUnlocked = false;
				} else {
					throw new IllegalArgumentException("Property '".concat(key).concat("' is read-only."));
				}
			}

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					dbNode.removeProperty(key);
					return null;
				}
			});
		}
	}

	/**
	 * Sets the currently accessing user, overriding user from request in case
	 * there is no request..
	 *
	 * @param user
	 */
	public void setAccessingUser(User user) {
		this.user = user;
	}

	public void setOwnerId(final Long value) {
		setOwnerNode(value);
	}

	private void setOwnerNode(final Long nodeId) {

		Command setOwner = Services.command(securityContext,
			SetOwnerCommand.class);

		setOwner.execute(this,
				 Services.command(securityContext, FindNodeCommand.class).execute(nodeId));
	}

	//~--- inner classes --------------------------------------------------

	private class DefaultRenderer implements NodeRenderer<AbstractNode> {

		@Override
		public void renderNode(StructrOutputStream out, AbstractNode currentNode, AbstractNode startNode,
				       String editUrl, Long editNodeId, RenderMode renderMode) {

			SecurityContext securityContext = out.getSecurityContext();

			if (securityContext.isVisible(currentNode)) {
				out.append(currentNode.getName());
			}
		}

		//~--- get methods --------------------------------------------

		@Override
		public String getContentType(AbstractNode node) {
			return ("blah");
		}
	}


	/*
	 * Helper class for multilanguage titles
	 */
	public class Title {

		public final static String LOCALE_KEY = "locale";

		//~--- fields -------------------------------------------------

		private Locale locale;
		private String title;

		//~--- constructors -------------------------------------------

		public Title(final Locale locale, final String title) {

			this.locale = locale;
			this.title  = title;
		}

		//~--- get methods --------------------------------------------

		public Locale getLocale() {
			return locale;
		}

		public String getTitle() {
			return title;
		}

		//~--- set methods --------------------------------------------

		public void setLocale(final Locale locale) {
			this.locale = locale;
		}

		public void setTitle(final String title) {
			this.title = title;
		}
	}
}
