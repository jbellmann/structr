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
package org.structr.ui.page.admin;

import java.util.LinkedList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.tree.Tree;
import org.apache.click.extras.tree.TreeNode;
import org.apache.click.util.Bindable;
import org.apache.click.control.*;
import org.apache.click.control.Column;
import org.apache.click.extras.control.FieldColumn;
import org.apache.click.extras.control.FormTable;
import org.apache.click.extras.control.IntegerField;
import org.apache.click.extras.control.LinkDecorator;
import org.apache.click.extras.control.LongField;
import org.apache.click.extras.control.SubmitLink;
import org.apache.click.service.*;
import org.apache.click.util.ClickUtils;
import org.apache.click.util.HtmlStringBuffer;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.structr.common.Permission;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.File;
import org.structr.core.entity.Link;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.CopyNodeCommand;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.LinkNodeFactoryCommand;
import org.structr.core.node.MoveNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.NodePropertiesCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.common.RelType;
import org.structr.core.entity.DummyNode;
import org.structr.core.entity.Property;
import org.structr.core.entity.User;
import org.structr.core.module.GetEntitiesCommand;
import org.structr.core.node.ExtractFileCommand;
import org.structr.core.notification.AddNotificationCommand;
import org.structr.core.notification.ErrorNotification;
import org.structr.core.notification.SuccessNotification;

/**
 * Display the node tree.
 * 
 * By default, root is the reference node of the underlying graph database.
 * 
 * @author amorgner
 */
public class Nodes extends Admin {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(Nodes.class.getName());
	/** some id to identify the tree control */
	private final static String TREE_ID = "nodeTree";
	protected final static String REPORT_RESULTS_KEY = "reportResults";
	/** root node of tree */
	private TreeNode root;
	@Bindable
	protected Panel nodeTreePanel;
	/** form to create a new node */
	@Bindable
	protected Form newNodeForm = new Form();
	@Bindable
	protected String newNodeFormIsInvalid;
	/** panel containing the above form */
	@Bindable
	protected Panel newNodePanel;
	/** form to create a new relationship */
	@Bindable
	protected Form newRelationshipForm = new Form();
	@Bindable
	protected String newRelationshipFormIsInvalid;
	/** panel containing the above form */
	@Bindable
	protected Panel newRelationshipPanel;
	/** form to move a node */
	@Bindable
	protected Form moveNodeForm = new Form();
	@Bindable
	protected String moveNodeFormIsInvalid;
	/** panel containing the above form */
	@Bindable
	protected Panel moveNodePanel;
	@Bindable
	protected Form copyNodeForm = new Form();
	@Bindable
	protected String copyNodeFormIsInvalid;
	/** panel containing the above form */
	@Bindable
	protected Panel copyNodePanel;
	/** upload service, needed for file upload */
	protected FileUploadService upload = new CommonsFileUploadService();
	/** form to upload files */
	@Bindable
	protected Form uploadForm = new Form();
	@Bindable
	protected String uploadFormIsInvalid;
	/** panel containing the above form */
	@Bindable
	protected Panel uploadPanel;
	/** link to suitable view page (enters view mode) */
	@Bindable
	protected Form extractNodeForm = new Form();
	@Bindable
	protected String extractNodeFormIsInvalid;
	/** panel containing the above form */
	@Bindable
	protected Panel extractNodePanel;
	@Bindable
	protected PageLink viewLink;
	/** link to suitable edit page (enters edit mode) */
	@Bindable
	protected PageLink editLink;
	@Bindable
	protected PageLink editPropertiesLink = new PageLink("editPropertiesLink", "Properties", Properties.class);
	@Bindable
	protected Form deleteNodeForm = new Form();
	@Bindable
	protected String deleteNodeFormIsInvalid;
	/** panel containing the above form */
	@Bindable
	protected Panel deleteNodePanel;
	//@Bindable protected ActionLink createRelationshipLink = new ActionLink("createRelationshipLink", "Create Link", this, "onCreateRelationshipClick");
	/** link on node in tree */
	protected PageLink link;
	/** CSS class to mark view mode */
	@Bindable
	protected String viewClass = "";
	/** CSS class to mark edit mode */
	@Bindable
	protected String editClass = "";
	@Bindable
	protected FormTable propertiesTable;
	@Bindable
	protected Form pagerForm = new Form();
	@Bindable
	protected Form navigationForm = new Form();
	@Bindable
	protected ActionLink deleteLink = new ActionLink("Delete Property", this, "onDeleteProperty");
	@Bindable
	protected Button backButton;
	@Bindable
	protected String editPropertiesFormIsInvalid;
	@Bindable
	protected Form setPropertyForm = new Form();
	@Bindable
	protected Form form = new Form("form");
	@Bindable
	protected Panel renditionPanel;
	@Bindable
	protected String externalViewUrl;
	@Bindable
	protected String localViewUrl;
	@Bindable
	protected String rendition;
	@Bindable
	protected String source;
	@Bindable
	protected Panel notificationPanel;

	public Nodes() {

		super();

		contextPath = getContext().getRequest().getContextPath();

		initTree();
		getExpandedTreeNodesFromSession();
		buildTree();

	}

	@Override
	public void onInit() {

		super.onInit();

		if(securityContext.isAllowed(node, Permission.ShowTree)) {

			nodeTreePanel = new Panel("nodeTreePanel", "/panel/node-tree-panel.htm");
		}

		if(securityContext.isAllowed(node, Permission.CreateNode)) {

			Select nodeTypeField = new Select(AbstractNode.Key.type.name(), "Select Node Type", false);
			nodeTypeField.add(new Option("", "--- Select Node Type ---"));

			List<String> nodeTypes = new LinkedList<String>(((Map<String, Class>)Services.command(securityContext, GetEntitiesCommand.class).execute()).keySet());
			Collections.sort(nodeTypes);

//            Set<String> nodeTypes = Services.getCachedEntityTypes();
			for(String className : nodeTypes) {
				Option o = new Option(className);
				nodeTypeField.add(o);
			}

			newNodeForm.add(nodeTypeField);
			newNodeForm.add(new TextField("name", true));

			newNodeForm.add(new LongField(AbstractNode.Key.position.name(), false));
			newNodeForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
			newNodeForm.add(new TextField(TARGET_NODE_ID_KEY, true));
			newNodeForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
			newNodeForm.setListener(this, "onCreateNode");
			newNodeForm.add(new Submit("createNewNode", " Create new node ", this, "onCreateNode"));
			newNodePanel = new Panel("newNodePanel", "/panel/new-node-panel.htm");

			// assemble form to copy a node
			copyNodeForm.add(new TextField(SOURCE_NODE_ID_KEY, true));
			copyNodeForm.add(new TextField(TARGET_NODE_ID_KEY, true));
			copyNodeForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
			copyNodeForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
			copyNodeForm.setListener(this, "onCopyNode");
			copyNodeForm.add(new Submit("copyNode", " Copy node ", this, "onCopyNode"));
			copyNodePanel = new Panel("copyNodePanel", "/panel/copy-node-panel.htm");

			// assemble form to upload files
			FileField uploadField1 = new FileField("file1", false);
			FileField uploadField2 = new FileField("file2", false);
			FileField uploadField3 = new FileField("file3", false);
			FileField uploadField4 = new FileField("file4", false);
			FileField uploadField5 = new FileField("file5", false);
			FileField uploadField6 = new FileField("file6", false);
			FileField uploadField7 = new FileField("file7", false);
			FileField uploadField8 = new FileField("file8", false);
			FileField uploadField9 = new FileField("file9", false);
			FileField uploadField10 = new FileField("file10", false);
			uploadForm.add(uploadField1);
			uploadForm.add(uploadField2);
			uploadForm.add(uploadField3);
			uploadForm.add(uploadField4);
			uploadForm.add(uploadField5);
			uploadForm.add(uploadField6);
			uploadForm.add(uploadField7);
			uploadForm.add(uploadField8);
			uploadForm.add(uploadField9);
			uploadForm.add(uploadField10);
			uploadForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
			uploadForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
			uploadForm.add(new Submit("createFileNode", " Upload file(s)", this, "onUpload"));
			uploadPanel = new Panel("uploadPanel", "/panel/upload-panel.htm");

			// assemble form to extract a node
			extractNodeForm.add(new TextField(TARGET_NODE_ID_KEY, false));
			extractNodeForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
			extractNodeForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
			extractNodeForm.add(new Submit("extractNode", " Extract node ", this, "onExtractNode"));
			extractNodePanel = new Panel("extractNodePanel", "/panel/extract-node-panel.htm");

		}

		if(securityContext.isAllowed(node, Permission.DeleteNode)) {

			// assemble form to delete a node
			deleteNodeForm.add(new Checkbox(RECURSIVE_KEY, false));
			//deleteNodeForm.add(new HiddenField(PARENT_NODE_ID_KEY, String.class));
			deleteNodeForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
			deleteNodeForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
			deleteNodeForm.add(new Submit("deleteNode", " Delete node ", this, "onDeleteNode"));
			deleteNodePanel = new Panel("deleteNodePanel", "/panel/delete-node-panel.htm");

			// assemble form to move a node
			moveNodeForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
			moveNodeForm.add(new TextField(TARGET_NODE_ID_KEY, true));
			moveNodeForm.add(new TextField(PARENT_NODE_ID_KEY, ""));
			moveNodeForm.add(new TextField(SOURCE_NODE_ID_KEY, ""));
			moveNodeForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
			moveNodeForm.setListener(this, "onMoveNode");
			moveNodeForm.add(new Submit("moveNode", " Move node ", this, "onMoveNode"));
			moveNodePanel = new Panel("moveNodePanel", "/panel/move-node-panel.htm");
		}



		if(securityContext.isAllowed(node, Permission.CreateRelationship)) {

			// assemble form to create a new relationship
			newRelationshipForm.add(new TextField(SOURCE_NODE_ID_KEY, true));
			newRelationshipForm.add(new TextField(TARGET_NODE_ID_KEY, true));
			newRelationshipForm.add(new TextField(REL_TYPE_KEY, true));
//            Select relTypeField = new Select(REL_TYPE_KEY, "Select Relationship Type", true);
//            relTypeField.add(new Option("", "Please select"));
//            //Option linkOption = new Option("LINK", "LINK");
//            //relTypeField.add(linkOption);
//            // TODO: get possible relationship types dynamically
//            relTypeField.add(new Option("HAS_CHILD", "Has Child"));
//            relTypeField.add(new Option("LINK", "Link"));
//            relTypeField.add(new Option("PAGE_LINK", "Page Link"));
//            relTypeField.add(new Option("SECURITY", "Security"));
//            relTypeField.add(new Option("ROOT_NODE", "Root Node"));
//            relTypeField.add(new Option("USE_TEMPLATE", "Use Template"));
//            relTypeField.add(new Option("DATA", "Data"));
//            relTypeField.add(new Option("SUBMIT", "Submit"));
//            relTypeField.add(new Option("SUCCESS_DESTINATION", "Success Destination"));
//            relTypeField.add(new Option("ERROR_DESTINATION", "Error Destination"));
//            relTypeField.add(new Option("CREATE_DESTINATION", "Create Destination"));
//            relTypeField.add(new Option("TYPE", "Type"));
//            relTypeField.add(new Option("SUBTYPE", "Sub Type"));
//            //relTypeField.setDefaultOption(linkOption);
//            newRelationshipForm.add(relTypeField);
			newRelationshipForm.add(new TextField(REL_POSITION_KEY, false));
			newRelationshipForm.add(new TextField(TARGET_SLOT_NAME_KEY, false));
			newRelationshipForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
			newRelationshipForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
			newRelationshipForm.setListener(this, "onCreateRelationship");
			newRelationshipForm.add(new Submit("createNewRelationship", " Create new relationship ", this, "onCreateRelationship"));
			newRelationshipPanel = new Panel("newRelationshipPanel", "/panel/new-relationship-panel.htm");
		}

		FieldSet fieldSet = new FieldSet("property");
		fieldSet.add(new TextField(KEY_KEY)).setRequired(true);
		fieldSet.add(new TextField(VALUE_KEY)).setRequired(true);
		setPropertyForm.add(fieldSet);
		setPropertyForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
		setPropertyForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
		setPropertyForm.add(new Submit("setProperty", " Set property ",
		    this, "onSetProperty"));



		// create and assemble table
		propertiesTable = new FormTable("propertiesTable", form);
		propertiesTable.setClass(TABLE_CLASS);

		FieldColumn column = new FieldColumn(KEY_KEY, new TextField());
		column.getField().setRequired(true);
		propertiesTable.addColumn(column);

		column = new FieldColumn(VALUE_KEY, new TextField());
		column.getField().setRequired(true);
		propertiesTable.addColumn(column);

		deleteLink.setImageSrc("/images/table-delete.png");
		deleteLink.setTitle("Delete property");
		deleteLink.setParameter(NODE_ID_KEY, nodeId);
		deleteLink.setAttribute("onclick", "return window.confirm('Do you really want to delete this property?');");

		Column actionColumn = new Column("Action");
		actionColumn.setTextAlign("center");
		AbstractLink[] links = new AbstractLink[]{deleteLink};
		actionColumn.setDecorator(new LinkDecorator(propertiesTable, links, KEY_KEY));
		actionColumn.setSortable(false);
		propertiesTable.addColumn(actionColumn);


		propertiesTable.getForm().add(new Submit("updateProperty", "Update properties",
		    this, "onUpdateProperties"));

		fieldSet = new FieldSet("properties");
		form.add(fieldSet);
		fieldSet.add(propertiesTable);

		propertiesTable.getForm().add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
		propertiesTable.getForm().add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));

		//propertiesTable.getForm().add(new Submit("cancel", this, "onCancelClick"));


		// Pager form
		IntegerField pageSizeField = new IntegerField("pageSize", true);
		//pageSizeField.setLabel("Page size");
		pageSizeField.setInteger(DEFAULT_PAGESIZE);
		pageSizeField.setMinValue(DEFAULT_PAGER_MIN);
		pageSizeField.setMaxValue(DEFAULT_PAGER_MAX);
		pagerForm.add(pageSizeField);

		navigationForm.add(new HiddenField(NODE_ID_KEY, nodeId != null ? nodeId : ""));
		navigationForm.add(new HiddenField(RENDER_MODE_KEY, renderMode != null ? renderMode : ""));
		navigationForm.add(new Submit("back", " Back ", this, "onBack"));



		if(parentNodeId != null) {
			// set parent node id  on node deletion form
			deleteNodeForm.add(new HiddenField(PARENT_NODE_ID_KEY, parentNodeId));
//            moveNodeForm.add(new HiddenField(PARENT_NODE_ID_KEY, parentNodeId));
			//deleteNodeForm.getField(PARENT_NODE_ID_KEY).setValue(parentNodeId);
		}

		// console panel
		notificationPanel = new Panel("notificationPanel", "/panel/notification-panel.htm");
	}

	@Override
	public void onRender() {

		super.onRender();

		if(node != null) {
			title = node.getName() + " (" + node.getId() + ") [" + Services.getApplicationTitle() + "]";
		}

		TreeNode treeNode = null;
		if(nodeId != null) {
			treeNode = nodeTree.find(nodeId);
		}

		Long intId = 0L;
		AbstractNode n = null;

		if(treeNode != null) {

			if(!(treeNode.isRoot())) {

				n = (AbstractNode)treeNode.getValue();
				intId = n.getId();

				editPropertiesLink.setParameter(NODE_ID_KEY, intId);
				editPropertiesLink.setImageSrc("/images/table-edit.png");
				//editPropertiesLink.setAttribute("class", "nodeActionLink");
				editPropertiesLink.setAttribute("id", "editProps_" + intId);
				editPropertiesLink.setRenderLabelAndImage(true);

			}

		} else if("0".equals(getNodeId())) {

			logger.log(Level.FINE, "Root node accessed");

		} else {
			okMsg = "";
			logger.log(Level.FINE, "Node {0} not contained in node tree", getNodeId());
		}

		if(securityContext.isAllowed(node, Permission.Write)) {

			// Table rowList is populated through a DataProvider which loads
			// data on demand.
			propertiesTable.setDataProvider(new DataProvider() {

				@Override
				public List<Property> getData() {
					return getProperties();
				}
			});
		}

		storeExpandedTreeNodesInSession();


	}

	/**
	 * Add subnode of node which was expanded on current request
	 */
	@Override
	public void onGet() {

		String expandId = getContext().getRequestParameter(Tree.EXPAND_TREE_NODE_PARAM);
		if(expandId == null) {
			return;
		}

		TreeNode nodeToExpandOrCollapse = null;
		nodeToExpandOrCollapse = nodeTree.find(expandId);

		if(nodeToExpandOrCollapse != null && nodeToExpandOrCollapse.isExpanded()) {

			// remove dummy node
			if(nodeToExpandOrCollapse.hasChildren()) {
				TreeNode dummyNode = nodeToExpandOrCollapse.getChildren().get(0);
				nodeToExpandOrCollapse.remove(dummyNode);
			}

			// load subnodes
			addItems(nodeToExpandOrCollapse, (AbstractNode)nodeToExpandOrCollapse.getValue());

		} else {
			// unload subnodes
//            //  FIXME: throws an java.util.ConcurrentModificationException
//            List<TreeNode> children = nodeToExpandOrCollapse.getDirectChildren();
//            for (TreeNode t : children) {
//                String id = t.getId();
//                TreeNode toRemove = nodeTree.find(id);
//                nodeToExpandOrCollapse.remove(toRemove);
//            }
//            //  FIXME: throws an java.util.ConcurrentModificationException, too
//            nodeToExpandOrCollapse.setChildrenSupported(false);
		}
	}

	/**
	 * Build tree
	 */
	private void buildTree() {

		long t0 = System.currentTimeMillis();

		if(rootNode == null) {
			// get reference (root) node
			rootNode = getRootNode();
		}

		root = new TreeNode(rootNode, String.valueOf(rootNode.getId()));

		// activate tree by setting the root node
		// root node has to be set in constructor or onInit !!
		nodeTree.setRootNode(root);

		// add items recursively
		addItems(root, rootNode);

		long t1 = System.currentTimeMillis();

		logger.log(Level.FINE, "Built tree in {0} ms.", (t1 - t0));

	}

	/**
	 * Recursively add sub nodes to the node, starting with given node
	 *
	 * @param parentNode
	 *            display node
	 * @param nodeToAdd
	 *            node bean
	 */
	private void addItems(TreeNode parentNode, AbstractNode nodeToAdd) {

		AbstractNode p = (AbstractNode)parentNode.getValue();

		// don't add children of link nodes to the tree
		// to avoid circular relationships (cannot be mapped to a tree)
		if(!(p instanceof Link)) {
//        if (!(parentNode.getValue() instanceof Link) && (nodeToAdd.equals(node)) || openNodes.contains(parentNode)) {

			// set of nodes to be ordered by a certain key
			List<AbstractNode> nodes = new LinkedList<AbstractNode>();

			Command nodeFactory = Services.command(securityContext, NodeFactoryCommand.class);
//            Command relCommand = Services.command(securityContext, NodeRelationshipsCommand.class);

			List<StructrRelationship> rels = nodeToAdd.getOutgoingChildRelationships();
//            List<StructrRelationship> rels = (List<StructrRelationship>) relCommand.execute(nodeToAdd, RelType.HAS_CHILD, Direction.OUTGOING);

			if(rels.size() > 1000) {
				logger.log(Level.WARNING, "Node has many relationships: {0}", rels.size());
			}


			int counter = 0;

			// follow the HAS_CHILD relationships
			for(StructrRelationship r : rels) {

				AbstractNode subNode = r.getEndNode();

				// if the node is readable for current user
				if(securityContext.isAllowed(subNode, Permission.Read)) {
//				if(isSuperUser || (subNode.readAllowed()) {

					// instantiate new tree node with given repository path,
					// object's repository id as unique id (in tree context)
					AbstractNode s = (AbstractNode)nodeFactory.execute(subNode);
					nodes.add(s);
				}

				counter++;

				if(counter > 1000) {
					logger.log(Level.SEVERE, "This node has too many relationships, displaying only the first 1000");
					break;
				}


			}

			nodeFactory = Services.command(securityContext, LinkNodeFactoryCommand.class);

			rels = nodeToAdd.getOutgoingLinkRelationships();
//            rels = (List<StructrRelationship>) relCommand.execute(nodeToAdd, RelType.LINK, Direction.OUTGOING);

			// now LINK relationships
			for(StructrRelationship r : rels) {

				AbstractNode subNode = r.getEndNode();

				// if the node is readable for current user
				if(securityContext.isAllowed(subNode, Permission.Read)) {
//				if(isSuperUser || (subNode.readAllowed())) {

					// instantiate new tree node with given repository path,
					// object's repository id as unique id (in tree context)
					AbstractNode s = (AbstractNode)nodeFactory.execute(subNode);
					nodes.add(s);
				}

			}

			if(!(nodes.isEmpty())) {

//                parentNode.setChildrenSupported(true);

				// sort by position
				Collections.sort(nodes, new Comparator<AbstractNode>() {

					@Override
					public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {
						return nodeOne.getPosition().compareTo(nodeTwo.getPosition());
					}
				});

				// render nodes in correct order
				for(AbstractNode s : nodes) {

					// add AbstractNode object to tree
					TreeNode newNode = addTreeNode(s, parentNode);

					List<TreeNode> expandedNodes = nodeTree.getExpandedNodes(true);

					if(openNodes.contains(newNode) || expandedNodes.contains(newNode)) {

						nodeTree.expand(newNode);

						// add further subnodes
						addItems(newNode, s);

					} else {

						if(s.hasChildren()) {
							DummyNode dummyNode = new DummyNode();
							// add dummy node to have th [+] sign displayed
							addTreeNode(dummyNode, newNode);
						}
					}

					// if folder has sub folders, show [+] icon in tree
					newNode.setChildrenSupported(true);
				}
//                } else {
//                    // add dummy node
//                    new TreeNode(new AbstractNode(), "dummyId", parentNode);
			}
		}
	}

	/**
	 * Create a new sub node
	 *
	 * @return
	 */
	public boolean onCreateNode() {

		final User user = securityContext.getUser();
		AbstractNode s = null;

		final String targetNodeId = StringUtils.isNotEmpty(newNodeForm.getFieldValue(TARGET_NODE_ID_KEY)) ? newNodeForm.getFieldValue(TARGET_NODE_ID_KEY) : getNodeId();

		if(newNodeForm.isValid()) {
			Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

			s = (AbstractNode)transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {
					Command findNode = Services.command(securityContext, FindNodeCommand.class);
					Command createNode = Services.command(securityContext, CreateNodeCommand.class);
					Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

					AbstractNode parentNode = (AbstractNode)findNode.execute(Long.parseLong(targetNodeId));
					AbstractNode newNode = (AbstractNode)createNode.execute(user);

					newNodeForm.copyTo(newNode);

					createRel.execute(parentNode, newNode, RelType.HAS_CHILD);

					newNodeForm.clearValues();

					addTreeNode(newNode, getCurrentTreeNode());
					openNodes.add(getCurrentTreeNode());

					return (newNode);
				}
			});

			// avoid NullPointerException when no node was created..
			if(s != null) {
				okMsg = "New " + s.getType() + " node " + s.getName() + " has been created.";
				Services.command(securityContext, AddNotificationCommand.class).execute(new SuccessNotification(securityContext, okMsg));

				Command findNode = Services.command(securityContext, FindNodeCommand.class);
				AbstractNode n = (AbstractNode)findNode.execute(s.getId());

				Map<String, String> parameters = new HashMap<String, String>();
				parameters.put(NODE_ID_KEY, String.valueOf(s.getId()));
				parameters.put(RENDER_MODE_KEY, renderMode);
				parameters.put(OK_MSG_KEY, okMsg);
				setRedirect(getRedirectPage(s), parameters);
				setRedirect(getRedirect().concat("#properties-tab"));

			} else {
				errorMsg = "New node could not be created!";
				Services.command(securityContext, AddNotificationCommand.class).execute(new ErrorNotification(securityContext, errorMsg));
			}

		}

		if(!newNodeForm.isValid()) {
			newNodeFormIsInvalid = "true";
			return true;
		}

		// return false to stop continuing current controls
		return false;
	}

	/**
	 * Create a relationship
	 *
	 * @return true
	 */
	public boolean onCreateRelationship() {
		if(newRelationshipForm.isValid()) {
			final String sourceNodeId = StringUtils.isNotEmpty(newRelationshipForm.getFieldValue(SOURCE_NODE_ID_KEY)) ? newRelationshipForm.getFieldValue(SOURCE_NODE_ID_KEY) : getNodeId();
			final String targetNodeId = newRelationshipForm.getFieldValue(TARGET_NODE_ID_KEY);
			final String relType = newRelationshipForm.getFieldValue(REL_TYPE_KEY);
			final String targetSlotName = newRelationshipForm.getFieldValue(TARGET_SLOT_NAME_KEY);
			final User user = securityContext.getUser();

			Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {
					Command findNode = Services.command(securityContext, FindNodeCommand.class);
					Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

					AbstractNode startNode = (AbstractNode)findNode.execute(Long.parseLong(sourceNodeId));
					AbstractNode endNode = (AbstractNode)findNode.execute(Long.parseLong(targetNodeId));

					StructrRelationship newRel = (StructrRelationship)createRel.execute(startNode, endNode, relType);
					newRel.setProperty(TARGET_SLOT_NAME_KEY, targetSlotName);

					return (null);
				}
			});

			okMsg = "New relationship to node " + targetNodeId + " with type " + relType + " has been created.";
			Services.command(securityContext, AddNotificationCommand.class).execute(new SuccessNotification(securityContext, okMsg));

			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(NODE_ID_KEY, String.valueOf(getNodeId()));
			parameters.put(RENDER_MODE_KEY, renderMode);
			setRedirect(getRedirectPage(node), parameters);

		}

		if(!newRelationshipForm.isValid()) {
			newRelationshipFormIsInvalid = "true";
			return true;
		}

		// return false to stop continuing current controls
		return false;
	}

	public boolean onMoveNode() {
		if(moveNodeForm.isValid()) {
			final String targetNodeId = moveNodeForm.getFieldValue(TARGET_NODE_ID_KEY);
			final boolean isLink = StringUtils.isNotEmpty(moveNodeForm.getFieldValue(PARENT_NODE_ID_KEY));
			final String sourceNodeId = StringUtils.isNotEmpty(moveNodeForm.getFieldValue(SOURCE_NODE_ID_KEY)) ? moveNodeForm.getFieldValue(SOURCE_NODE_ID_KEY) : getNodeId();

			Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {
					Command moveNode = Services.command(securityContext, MoveNodeCommand.class);
					moveNode.execute(sourceNodeId, targetNodeId, isLink);
					return (null);
				}
			});

			okMsg = "Node moved to " + targetNodeId + ".";
			Services.command(securityContext, AddNotificationCommand.class).execute(new SuccessNotification(securityContext, okMsg));

			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(NODE_ID_KEY, String.valueOf(getNodeId()));
			parameters.put(RENDER_MODE_KEY, renderMode);
			parameters.put(OK_MSG_KEY, okMsg);
			setRedirect(getRedirectPage(node), parameters);

		}

		if(!moveNodeForm.isValid()) {
			moveNodeFormIsInvalid = "true";
			return true;
		}

		return false;
	}

	public boolean onCopyNode() {
		if(copyNodeForm.isValid()) {
			final String targetNodeId = copyNodeForm.getFieldValue(TARGET_NODE_ID_KEY);
			final String sourceNodeId = StringUtils.isNotEmpty(copyNodeForm.getFieldValue(SOURCE_NODE_ID_KEY)) ? copyNodeForm.getFieldValue(SOURCE_NODE_ID_KEY) : getNodeId();
			final User user = securityContext.getUser();

			Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {
					Command copyNode = Services.command(securityContext, CopyNodeCommand.class);
					copyNode.execute(sourceNodeId, targetNodeId, user);
					return (null);
				}
			});

			okMsg = "Node copied to " + targetNodeId + ".";
			Services.command(securityContext, AddNotificationCommand.class).execute(new SuccessNotification(securityContext, okMsg));

			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(NODE_ID_KEY, String.valueOf(getNodeId()));
			parameters.put(RENDER_MODE_KEY, renderMode);
			parameters.put(OK_MSG_KEY, okMsg);
			setRedirect(getRedirectPage(node), parameters);

		}

		if(!copyNodeForm.isValid()) {
			copyNodeFormIsInvalid = "true";
			return true;
		}

		return false;
	}

	public boolean onExtractNode() {

		if(extractNodeForm.isValid()) {

			final User user = securityContext.getUser();
			Object targetNodeId;
			String fieldValue = extractNodeForm.getFieldValue(TARGET_NODE_ID_KEY);
			if(fieldValue != null && !(fieldValue.isEmpty())) {
				targetNodeId = fieldValue;
			} else {
				targetNodeId = getNodeId();
			}

			Command findNode = Services.command(securityContext, FindNodeCommand.class);
			final AbstractNode targetNode = (AbstractNode)findNode.execute(targetNodeId);

			final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {
					Command extractFile = Services.command(securityContext, ExtractFileCommand.class);
					extractFile.execute(getNodeId(), targetNode, user);
					transactionCommand.setExitCode(extractFile.getExitCode());
					transactionCommand.setErrorMessage(extractFile.getErrorMessage());
					return null;
				}
			});

			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(NODE_ID_KEY, String.valueOf(getNodeId()));
			parameters.put(RENDER_MODE_KEY, renderMode);

			if(transactionCommand.getExitCode().equals(Command.exitCode.FAILURE)) {
				errorMsg = transactionCommand.getErrorMessage();
				Services.command(securityContext, AddNotificationCommand.class).execute(new ErrorNotification(securityContext, errorMsg));
				parameters.put(ERROR_MSG_KEY, errorMsg);
			} else {
				okMsg = "Node extracted to " + targetNodeId;
				Services.command(securityContext, AddNotificationCommand.class).execute(new SuccessNotification(securityContext, okMsg));
				parameters.put(OK_MSG_KEY, okMsg);
			}

			setRedirect(getRedirectPage(node), parameters);

		}

		if(!extractNodeForm.isValid()) {
			extractNodeFormIsInvalid = "true";
			return true;
		}

		return false;
	}

	/**
	 * Create a new file as sub node
	 *
	 * @return true
	 */
	public boolean onUpload() {
		AbstractNode s = null;

		if(uploadForm.isValid()) {

			List<Field> fieldList = (List<Field>)uploadForm.getFieldList();

			for(Field f : fieldList) {

				if(!(f instanceof FileField)) {
					break;
				}

				final FileField fileField = (FileField)f;
				final FileItem fileFromUpload = fileField.getFileItem();
				final String name = fileFromUpload.getName();
				final String mimeType = fileFromUpload.getContentType();
				final User user = securityContext.getUser();

				if(name != null && !("".equals(name))) {
					// process uploaded file(s):
					// read file via input stream (do not load into memory!)
					// and write to the configured location
					// TODO: move to helper class, support multiple files

					Command transaction = Services.command(securityContext, TransactionCommand.class);

					s = (AbstractNode)transaction.execute(new StructrTransaction() {

						@Override
						public Object execute() throws Throwable {
							Command createNode = Services.command(securityContext, CreateNodeCommand.class);
							Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

							String mimeProperty = null;

							if(mimeType != null && mimeType.startsWith("image")) {
								mimeProperty = "Image";

							} else {
								mimeProperty = "File";
							}

							// create node with appropriate type
							AbstractNode newNode = (AbstractNode)createNode.execute(new NodeAttribute(AbstractNode.Key.type.name(), mimeProperty), user);

							// determine properties
							String relativeFilePath = newNode.getId() + "_" + System.currentTimeMillis();
							String path = FILES_PATH + "/" + relativeFilePath;
							long size = fileFromUpload.getSize();
							java.io.File fileOnDisk = new java.io.File(path);
							String fileUrl = "file:///" + fileOnDisk.getPath();

							try {
								fileFromUpload.write(fileOnDisk);

							} catch(Exception e) {
								okMsg = "";
								errorMsg = "Error while write uploaded file(s) to disk: " + e.getStackTrace();
							}

							Date now = new Date();
							newNode.setProperty(AbstractNode.Key.name.name(), name);
							newNode.setProperty(AbstractNode.Key.createdDate.name(), now);
							newNode.setProperty(AbstractNode.Key.lastModifiedDate.name(), now);

							newNode.setProperty(File.Key.contentType.name(), mimeType);
							newNode.setProperty(File.Key.size.name(), String.valueOf(size));
							newNode.setProperty(File.Key.url.name(), fileUrl);
							newNode.setProperty(File.Key.relativeFilePath.name(), relativeFilePath);

							AbstractNode parentNode = node;
							createRel.execute(parentNode, newNode, RelType.HAS_CHILD);

							// clear form
							newNodeForm.clearValues();

							addTreeNode(newNode, getCurrentTreeNode());

							return (newNode);
						}
					});

				} else {
					warnMsg = "No file was selected.";
				}

			}

			// assemble feedback message
			okMsg = "New " + s.getType() + " node " + s.getName() + " has been created.";
			Services.command(securityContext, AddNotificationCommand.class).execute(new SuccessNotification(securityContext, okMsg));

			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(NODE_ID_KEY, String.valueOf(getNodeId()));
			parameters.put(RENDER_MODE_KEY, renderMode);
			parameters.put(OK_MSG_KEY, okMsg);
			setRedirect(getRedirectPage(node), parameters);

		}

		if(!uploadForm.isValid()) {
			uploadFormIsInvalid = "true";
			return true;
		}

		// return false to stop continuing current controls
		return false;
	}

	/**
	 * Delete a node
	 *
	 * @return
	 */
	public boolean onDeleteNode() {

		if(deleteNodeForm.isValid()) {

			final String parent = deleteNodeForm.getFieldValue(PARENT_NODE_ID_KEY);
			final String recursive = deleteNodeForm.getFieldValue(RECURSIVE_KEY);
			final User user = securityContext.getUser();

			AbstractNode parentNode = null;

			try {

				final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
				parentNode = (AbstractNode)transactionCommand.execute(new StructrTransaction() {

					@Override
					public Object execute() throws Throwable {
						Command deleteNode = Services.command(securityContext, DeleteNodeCommand.class);
						Object result = deleteNode.execute(getNodeId(), parent, recursive, user);
						transactionCommand.setExitCode(deleteNode.getExitCode());
						transactionCommand.setErrorMessage(deleteNode.getErrorMessage());
						return result;
					}
				});


				Map<String, String> parameters = new HashMap<String, String>();
				parameters.put(RENDER_MODE_KEY, renderMode);

				if(Command.exitCode.SUCCESS.equals(transactionCommand.getExitCode())) {
					removeTreeNode(getNodeId().toString());
					errorMsg = null;
					okMsg = "Node " + getNodeId() + " successfully deleted";
					parameters.put(OK_MSG_KEY, okMsg);
				} else {
					okMsg = null;
					errorMsg = transactionCommand.getErrorMessage();
					parameters.put(ERROR_MSG_KEY, errorMsg);
				}

				if(parentNode != null) {
					parameters.put(NODE_ID_KEY, String.valueOf(parentNode.getId()));
					setRedirect(getRedirectPage(parentNode), parameters);
				}


			} catch(Exception e) {

				logger.log(Level.WARNING, "Node {0} could not be deleted.", getNodeId());

				okMsg = null;
				errorMsg = "Node " + getNodeId() + " could not be deleted. " + e.getMessage();
				Services.command(securityContext, AddNotificationCommand.class).execute(new ErrorNotification(securityContext, errorMsg));
				return true;

			}

		}

		if(!deleteNodeForm.isValid()) {
			deleteNodeFormIsInvalid = "true";
			return true;
		}

		return false;
	}

	/**
	 * Add node to the tree. Use current node as parent.
	 *
	 * @param s
	 * @param parentNode
	 * @return newly created tree node
	 */
	private TreeNode addTreeNode(AbstractNode s, TreeNode parentNode) {

		TreeNode n = new TreeNode(s, String.valueOf(s.getId()));
		n.setIcon(contextPath + getIconSrc(s));

		parentNode.add(n);

		return n;
	}

	/**
	 * Find the tree node with the given id
	 *
	 * @param nodeId
	 * @return
	 */
	protected TreeNode getTreeNode(String nodeId) {
		return nodeTree.find(nodeId);
	}

	/**
	 * Find the currently selected tree node.
	 *
	 * @return
	 */
	private TreeNode getCurrentTreeNode() {
		return getTreeNode(getNodeId().toString());
	}

	/**
	 * Remove node with the given from tree, e.g. after a delete action
	 *
	 * @param nodeId
	 */
	private void removeTreeNode(String nodeId) {
		TreeNode nodeToRemove = nodeTree.find(nodeId);
		if(nodeToRemove != null) {
			nodeToRemove.getParent().remove(nodeToRemove);
		}
	}

	/**
	 * Default getter method for actual node
	 */
	public AbstractNode getNode() {
		return node;
	}

	/**
	 * Return node list
	 *
	 * @return List<Property>
	 */
	public List<Property> getProperties() {
		return ((List<Property>)Services.command(securityContext, NodePropertiesCommand.class).execute(node));
	}

	/**
	 * Set a property
	 */
	public boolean onSetProperty() {
		Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

		transactionCommand.execute(new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {
				node.setProperty(setPropertyForm.getFieldValue(KEY_KEY), setPropertyForm.getFieldValue(VALUE_KEY));
				return (null);
			}
		});

		okMsg = "Property successfully set!"; // TODO: localize
		Services.command(securityContext, AddNotificationCommand.class).execute(new SuccessNotification(securityContext, okMsg));

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(NODE_ID_KEY, String.valueOf(getNodeId()));
		parameters.put(RENDER_MODE_KEY, renderMode);
		setRedirect(getRedirectPage(node), parameters);

		return false;
	}

	/**
	 * Update properties in backend
	 */
	public boolean onUpdateProperties() {

		okMsg = "Property successfully set!"; // TODO: localize
		Services.command(securityContext, AddNotificationCommand.class).execute(new SuccessNotification(securityContext, okMsg));

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(NODE_ID_KEY, String.valueOf(getNodeId()));
		parameters.put(RENDER_MODE_KEY, renderMode);
		setRedirect(getRedirectPage(node), parameters);

		return false;
	}

	/**
	 * Back button
	 */
	public boolean onBack() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(NODE_ID_KEY, nodeId.toString());
		parameters.put(RENDER_MODE_KEY, renderMode);
		setRedirect(getRedirectPage(node), parameters);
		return false;
	}

	/**
	 * Logout: invalidate session and clear user name
	 *
	 * @return
	 */
	@Override
	public boolean onLogout() {

		// before logging out, store expanded nodes on user node
		storeExpandedNodesInUserProfile();

		// store last visited node
		storeLastVisitedNodeInUserProfile();

		// invalidate (destroy) session
		//getContext().getRequest().getSession().invalidate();

		// cleare user name variable
		//userName = null;

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(NODE_ID_KEY, getNodeId());
		parameters.put(RENDER_MODE_KEY, renderMode);
		setRedirect(getRedirectPage(node), parameters);

		return super.onLogout();
	}

	/**
	 * Initialize tree
	 */
	private void initTree() {

		nodeTree = new Tree(TREE_ID) {

			@Override
			public ActionLink getExpandLink() {
				if(expandLink == null) {
					expandLink = new SubmitLink();
				}
				// set node id parameter to avoid that current node id gets lost
				expandLink.setParameter(NODE_ID_KEY, getNodeId());
				return expandLink;
			}

			// override render method to add current path as a custom parameter
			@Override
			protected void renderValue(HtmlStringBuffer buf, TreeNode node) {

				if(node != null) {

					AbstractNode n = (AbstractNode)node.getValue();

					if(n != null) {

						String displayName = n.getName();

						// if no name was set, use (internal) id to be fault tolerant
						Long intId = n.getId();
						if(displayName == null) {
							displayName = intId.toString();
						}

						// limit length tree entries label to 30 characters
						String label = ClickUtils.limitLength(displayName, 30);

						//Class<? extends Page> c = getRedirectPage(n, getPage());

						//PageLink link = new PageLink(label, getRedirectPage(n, getPage()));
						PageLink link = new PageLink(label, Edit.class);
						link.setParameter(NODE_ID_KEY, intId);

						// mouseover hint
						link.setAttribute("title", displayName + " (" + intId.toString() + ")");

						if(n instanceof Link) {
							// mark as link and set parent id,
							// so that the relationship can be deleted in onDeleteNodeClick
							link.setParameter("isLink", "1");
							parentNodeId = node.getParent().getId();
							link.setParameter(PARENT_NODE_ID_KEY, parentNodeId);
						}

						//link.setAttribute("class", "treeNodeTrigger");

						if(String.valueOf(n.getId()).equals(getNodeId())) {
							label = "<b class=\"active\">" + label + "</b>";
						}

						// mouseover title
						link.setLabel(label);
						if(n.isHidden()) {
							link.addStyleClass("hidden");
						}
						if(n.isVisibleToPublicUsers()) {
							link.addStyleClass("public");
						}
						buf.append(link);


					}
				}

			}
		};

	}

	/**
	 * Get list with expanded nodes from session
	 */
	private void getExpandedTreeNodesFromSession() {

		if(openNodes == null) {
			openNodes = (List<TreeNode>)getContext().getSession().getAttribute(EXPANDED_NODES_KEY);

			// return empty list if no open nodes exist in session
			if(openNodes == null) {
				openNodes = new LinkedList<TreeNode>();
			}
		}
	}

	/**
	 * Store all expanded nodes in session
	 */
	private void storeExpandedTreeNodesInSession() {

		getContext().getSession().setAttribute(EXPANDED_NODES_KEY, nodeTree.getExpandedNodes(true));
	}
}
