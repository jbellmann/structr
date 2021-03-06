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

import org.apache.click.Context;
import org.apache.click.Page;
import org.apache.click.control.AbstractLink;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Checkbox;
import org.apache.click.control.Column;
import org.apache.click.control.Field;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.PageLink;
import org.apache.click.control.Panel;
import org.apache.click.control.PasswordField;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.Table;
import org.apache.click.control.TextField;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.DateField;
import org.apache.click.extras.control.FormTable;
import org.apache.click.extras.control.IntegerField;
import org.apache.click.extras.control.LinkDecorator;
import org.apache.click.extras.control.LongField;
import org.apache.click.extras.control.PickList;
import org.apache.click.util.Bindable;
import org.apache.click.util.HtmlStringBuffer;

import org.neo4j.graphdb.RelationshipType;

import org.structr.common.AbstractComponent;
import org.structr.common.Permission;
import org.structr.common.RelType;
import org.structr.common.RenderMode;
import org.structr.common.StructrOutputStream;
import org.structr.common.renderer.HtmlComponentRenderer;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.cloud.CloudService;
import org.structr.core.cloud.CloudTransmission;
import org.structr.core.cloud.GetCloudServiceCommand;
import org.structr.core.cloud.PullNode;
import org.structr.core.cloud.PushNodes;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractNode.Title;
import org.structr.core.entity.CustomTypeNode;
import org.structr.core.entity.Folder;
import org.structr.core.entity.Group;
import org.structr.core.entity.Image;
import org.structr.core.entity.NodeType;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.Template;
import org.structr.core.entity.User;
import org.structr.core.module.GetEntitiesCommand;
import org.structr.core.module.GetEntityClassCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.FindGroupCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.FindUserCommand;
import org.structr.core.node.NodeConsoleCommand;
import org.structr.core.node.SetOwnerCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.operation.Callback;
import org.structr.core.node.operation.CallbackValue;
import org.structr.core.node.operation.Operation;
import org.structr.core.node.operation.PrimaryOperation;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.node.search.TextualSearchAttribute;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.click.extras.control.SubmitLink;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 */
public class DefaultEdit extends Nodes {

	private static final Logger logger = Logger.getLogger(DefaultEdit.class.getName());

	//~--- fields ---------------------------------------------------------

	/**
	 * The main form for editing node parameter.
	 * Child pages should just append fields to this form.
	 */
	protected Form editPropertiesForm           = new Form("editPropertiesForm");
	protected Form editVisibilityForm           = new Form("editVisibilityForm");
	protected Table incomingRelationshipsTable  = new Table("incomingRelationshipsTable");
	protected ActionLink incomingRelsControl    = incomingRelationshipsTable.getControlLink();
	protected Table outgoingRelationshipsTable  = new Table("outgoingRelationshipsTable");
	protected ActionLink outgoingRelsControl    = outgoingRelationshipsTable.getControlLink();
	protected ActionLink deleteRelationshipLink = new ActionLink("Delete Relationship", this,
							      "onDeleteRelationship");
	protected SubmitLink setCurrentOwnerRecursivelyLink = new SubmitLink("Set current owner recursively", this,
								      "onSetCurrentOwnerRecursively");
	protected FormTable childNodesTable = new FormTable("childNodesTable");

//      protected ActionLink deleteNodeLink = new ActionLink("Delete", this, "onDeleteNode");
	protected Table titlesTable       = new Table(AbstractNode.Key.titles.name());
	protected FormTable securityTable = new FormTable("Security");
	protected Form securityForm       = new Form("securityForm");
	protected Form consoleForm        = new Form("consoleForm");
	protected Form cloudForm          = new Form("cloudForm");
	protected Select userSelect       = new Select("selectUser", "User");
	protected Select typeSelect       = new Select(AbstractNode.Key.type.name(), "Type");
	protected Select templateSelect   = new Select(AbstractNode.Key.templateId.name(), "Template");
	protected Checkbox recursive      = new Checkbox("recursive");
	protected Select ownerSelect      = new Select(AbstractNode.Key.ownerId.name(), "Owner");
	protected Select groupSelect      = new Select("selectGroup", "Group");
	protected Select customTypeSelect = new Select(CustomTypeNode.Key.typeNodeId.name(), "Custom Type");
	protected Checkbox cloudRecursive = new Checkbox("cloudRecursive", "Recursive");
	protected Select cloudPushPull    = new Select("cloudPushPull", "Push / Pull");
	protected PickList allowed        = new PickList(StructrRelationship.Permission.allowed.name(), "Allowed");
	protected Panel cloudPanel;
	protected TextField consoleCommand;
	@Bindable
	protected String consoleOutput;
	protected Panel consolePanel;
	protected Panel editChildNodesPanel;
	protected Panel editPropertiesPanel;
	protected Panel editRelationshipsPanel;
	protected Panel editSecurityPanel;
	protected Panel editVisibilityPanel;
	@Bindable
	protected String helpOutput;
	protected Panel helpPanel;
	protected PasswordField passwordInput;
	protected TextField remoteHost;
	protected LongField remoteSourceNode;
	protected IntegerField remoteTcpPort;
	protected IntegerField remoteUdpPort;
	protected TextField userNameInput;

	//~--- constructors ---------------------------------------------------

	public DefaultEdit() {
		super();
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void onInit() {

		super.onInit();

		FieldSet nodePropertiesFields   = new FieldSet("Node Properties");

		nodePropertiesFields.setColumns(2);

		// add common fields
		nodePropertiesFields.add(new TextField(AbstractNode.Key.name.name(), true));
		nodePropertiesFields.add(new TextField(AbstractNode.Key.title.name(), false));
		nodePropertiesFields.add(typeSelect);
		nodePropertiesFields.add(new IntegerField(AbstractNode.Key.position.name()));

		TextField createdBy = new TextField(AbstractNode.Key.createdBy.name());

		createdBy.setReadonly(true);
		nodePropertiesFields.add(createdBy);

		DateField createdDate = new DateField(AbstractNode.Key.createdDate.name());

		createdDate.setFormatPattern(dateFormat.toPattern());
		createdDate.setShowTime(true);
		createdDate.setReadonly(true);
		nodePropertiesFields.add(createdDate);
		nodePropertiesFields.add(ownerSelect);

		if (securityContext.isAllowed(node, Permission.AccessControl)) {
			addControl(setCurrentOwnerRecursivelyLink);
			nodePropertiesFields.add(setCurrentOwnerRecursivelyLink);
		}

		DateField lastModifiedDate = new DateField(AbstractNode.Key.lastModifiedDate.name());

		lastModifiedDate.setFormatPattern(dateFormat.toPattern());
		lastModifiedDate.setShowTime(true);
		lastModifiedDate.setReadonly(true);
		nodePropertiesFields.add(lastModifiedDate);
		titlesTable.addColumn(new Column(AbstractNode.Key.title.name(), "Title"));
		titlesTable.addColumn(new Column(Title.LOCALE_KEY, "Locale"));
		titlesTable.setClass(TABLE_CLASS);
		nodePropertiesFields.add(titlesTable);
		editPropertiesForm.add(nodePropertiesFields);
		addControl(editPropertiesForm);

		FieldSet templateFields = new FieldSet("Template");

		templateFields.add(templateSelect);
		editPropertiesForm.add(templateFields);

		FieldSet customTypeFields = new FieldSet("Custom Type");

		customTypeFields.add(customTypeSelect);
		editPropertiesForm.add(customTypeFields);

		FieldSet visibilityFields = new FieldSet("Visibility");

		visibilityFields.setColumns(1);

		DateField visibilityStartDate = new DateField(AbstractNode.Key.visibilityStartDate.name());

		visibilityStartDate.setFormatPattern(dateFormat.toPattern());
		visibilityStartDate.setShowTime(true);
		visibilityFields.add(visibilityStartDate);

		DateField visibilityEndDate = new DateField(AbstractNode.Key.visibilityEndDate.name());

		visibilityEndDate.setFormatPattern(dateFormat.toPattern());
		visibilityEndDate.setShowTime(true);
		visibilityFields.add(visibilityEndDate);

		Checkbox hidden = new Checkbox(AbstractNode.Key.hidden.name(), "Completely hidden in frontend for anyone");

		visibilityFields.add(hidden);

		Checkbox publicCheckbox = new Checkbox(AbstractNode.Key.visibleToPublicUsers.name(), "Visible to public users");

		visibilityFields.add(publicCheckbox);

		Checkbox forAuthenticatedUsersCheckbox = new Checkbox(AbstractNode.Key.visibleToAuthenticatedUsers.name(),
								 "Visible to authenticated users");

		visibilityFields.add(forAuthenticatedUsersCheckbox);
		editVisibilityForm.add(visibilityFields);
		editVisibilityForm.add(new HiddenField(NODE_ID_KEY, (nodeId != null)
			? nodeId
			: ""));
		editVisibilityForm.add(new HiddenField(RENDER_MODE_KEY, (renderMode != null)
			? renderMode
			: ""));
		editVisibilityForm.setActionURL(editVisibilityForm.getActionURL().concat("#visibility-tab"));

		if (securityContext.isAllowed(node, Permission.EditProperty)) {

			visibilityFields.add(new Submit("saveVisibility", " Save Visibility ", this,
							"onSaveVisibility"));
			visibilityFields.add(new Submit("saveVisibilityDirectChildren",
							" Save Visibility (including direct children) ", this,
							"onSaveVisibilityIncludingDirectChildren"));
			visibilityFields.add(new Submit("saveVisibilityAllChildren",
							" Save Visibility (including all children) ", this,
							"onSaveVisibilityIncludingAllChildren"));
			visibilityFields.add(new Submit("cancel", " Cancel ", this, "onCancel"));
		}

		addControl(editVisibilityForm);
		editVisibilityPanel = new Panel("editVisibilityPanel", "/panel/edit-visibility-panel.htm");
		addControl(editVisibilityPanel);
		editPropertiesForm.add(new HiddenField(NODE_ID_KEY, (nodeId != null)
			? nodeId
			: ""));
		editPropertiesForm.add(new HiddenField(RENDER_MODE_KEY, (renderMode != null)
			? renderMode
			: ""));
		editPropertiesForm.setActionURL(editPropertiesForm.getActionURL().concat("#properties-tab"));

		if (securityContext.isAllowed(node, Permission.EditProperty)) {

			editPropertiesForm.add(new Submit("saveProperties", " Save Properties ", this,
				"onSaveProperties"));

//                      editPropertiesForm.add(new Submit("savePropertiesAndReturn", " Save and Return ", this, "onSaveAndReturn"));
			editPropertiesForm.add(new Submit("cancel", " Cancel ", this, "onCancel"));
		}

		// editPropertiesForm.add(new Submit("saveAndView", " Save And View ", this, "onSaveAndView"));
		editPropertiesPanel = new Panel("editPropertiesPanel", "/panel/edit-properties-panel.htm");
		addControl(editPropertiesPanel);

		Column nameColumn, typeColumn;
		PageLink viewRelLink = new PageLink("viewRel", DefaultEdit.class);
		LinkDecorator nameDec, iconDec;

		// ------------------ child nodes start --------------------------------
		if ((node != null) && (node.hasChildren() || (node instanceof Folder))) {

//                      if (node != null) {
			Column actionColumnNodes = new Column("Actions");

			actionColumnNodes.setTextAlign("center");
			actionColumnNodes.setDecorator(new LinkDecorator(childNodesTable, new PageLink(),
				AbstractNode.Key.nodeId.name()) {

				@Override
				protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link,
								Context context, Object row, Object value) {

					AbstractNode n = (AbstractNode) row;

					link = new PageLink(AbstractNode.Key.nodeId.name(), getEditPageClass(n)) {

						@Override
						public String getHref() {
							return super.getHref().concat("#properties-tab");
						}
					};
					link.setParameter(NODE_ID_KEY, n.getId());
					link.setImageSrc("/images/table-edit.png");
					super.renderActionLink(buffer, link, context, row, value);
				}

			});
			typeColumn = new Column(AbstractNode.Key.type.name());
			iconDec    = new LinkDecorator(childNodesTable, new PageLink(), AbstractNode.Key.nodeId.name()) {

				@Override
				protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link,
								Context context, Object row, Object value) {

					AbstractNode n = (AbstractNode) row;

					link = new PageLink(AbstractNode.Key.nodeId.name(), getEditPageClass(n));
					link.setParameter(NODE_ID_KEY, n.getId());

					boolean hasThumbnail = false;

					if (n instanceof Image) {

						Image image     = (Image) n;
						Image thumbnail = image.getScaledImage(100, 100);

						if (thumbnail != null) {

							String thumbnailSrc = "/view/" + thumbnail.getId();

							link.setImageSrc(thumbnailSrc);

							// link.setRenderLabelAndImage(true);
							hasThumbnail = true;
						}
					}

					if (!hasThumbnail) {
						link.setImageSrc(getIconSrc(n));
					}

					link.setLabel(n.getName());
					super.renderActionLink(buffer, link, context, row, value);
				}
			};
			typeColumn.setDecorator(iconDec);
			childNodesTable.addColumn(typeColumn);
			nameColumn = new Column(AbstractNode.Key.name.name());
			nameDec    = new LinkDecorator(childNodesTable, viewRelLink, "id") {

				@Override
				protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link,
								Context context, Object row, Object value) {

					AbstractNode n    = (AbstractNode) row;
					PageLink pageLink = new PageLink("id", getEditPageClass(n)) {

						@Override
						public String getHref() {
							return super.getHref().concat("#childnodes-tab");
						}
					};

					pageLink.setParameter(NODE_ID_KEY, n.getId());
					pageLink.setLabel(n.getName());
					super.renderActionLink(buffer, pageLink, context, row, value);
				}
			};
			nameColumn.setDecorator(nameDec);
			childNodesTable.addColumn(nameColumn);
			childNodesTable.addColumn(new Column(AbstractNode.Key.lastModifiedDate.name()));
			childNodesTable.addColumn(new Column(AbstractNode.Key.owner.name()));
			childNodesTable.addColumn(new Column(AbstractNode.Key.createdBy.name()));
			childNodesTable.addColumn(new Column(AbstractNode.Key.createdDate.name()));
			childNodesTable.addColumn(new Column(AbstractNode.Key.position.name()));
			childNodesTable.addColumn(new Column(AbstractNode.Key.visibleToPublicUsers.name()));
			childNodesTable.addColumn(new Column(AbstractNode.Key.deleted.name()));
			childNodesTable.addColumn(new Column(AbstractNode.Key.hidden.name()));
			childNodesTable.addColumn(actionColumnNodes);
			childNodesTable.setSortable(true);
			childNodesTable.setShowBanner(true);
			childNodesTable.setPageSize(DEFAULT_PAGESIZE);
			childNodesTable.getControlLink().setParameter(AbstractNode.Key.nodeId.name(), getNodeId());
			childNodesTable.setClass(TABLE_CLASS);
			addControl(childNodesTable);
			editChildNodesPanel = new Panel("editChildNodesPanel", "/panel/edit-child-nodes-panel.htm");
			addControl(editChildNodesPanel);
		}

		// ------------------ child nodes end --------------------------------
		// ------------------ incoming relationships start ---------------------
		if (securityContext.isAllowed(node, Permission.DeleteRelationship)) {

			deleteRelationshipLink.setImageSrc("/images/delete.png");
			deleteRelationshipLink.setTitle("Delete relationship");
			deleteRelationshipLink.setAttribute("onclick",
				"return window.confirm('Do you really want to delete this relationship?');");
			addControl(deleteRelationshipLink);

			Column actionColumnIn = new Column("Action");

			actionColumnIn.setTextAlign("center");

			AbstractLink[] linksIn = new AbstractLink[] { deleteRelationshipLink };

			actionColumnIn.setDecorator(new LinkDecorator(incomingRelationshipsTable, linksIn,
				RELATIONSHIP_ID_KEY));
			actionColumnIn.setSortable(false);
			typeColumn  = new Column(AbstractNode.Key.type.name());
			viewRelLink = new PageLink("view", DefaultEdit.class);

//                      viewRelLink = new PageLink("view", DefaultView.class);
			iconDec = new LinkDecorator(incomingRelationshipsTable, viewRelLink, "id") {

				@Override
				protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link,
								Context context, Object row, Object value) {

					StructrRelationship r  = (StructrRelationship) row;
					AbstractNode startNode = r.getStartNode();

//                                      link = new PageLink("id", getViewPageClass(startNode));
					link = new PageLink("id", getEditPageClass(startNode));
					link.setParameter(NODE_ID_KEY, startNode.getId());
					link.setImageSrc(startNode.getIconSrc());
					super.renderActionLink(buffer, link, context, row, value);
				}
			};
			typeColumn.setDecorator(iconDec);
			incomingRelationshipsTable.addColumn(typeColumn);
			nameColumn = new Column(AbstractNode.Key.name.name());
			nameDec    = new LinkDecorator(incomingRelationshipsTable, viewRelLink, "id") {

				@Override
				protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link,
								Context context, Object row, Object value) {

					StructrRelationship r  = (StructrRelationship) row;
					AbstractNode startNode = r.getStartNode();
					PageLink pageLink      = new PageLink("id", getEditPageClass(startNode)) {

						@Override
						public String getHref() {
							return super.getHref().concat("#relationships-tab");
						}
					};

					pageLink.setParameter(NODE_ID_KEY, startNode.getId());
					pageLink.setLabel(startNode.getName());
					super.renderActionLink(buffer, pageLink, context, row, value);
				}
			};
			nameColumn.setDecorator(nameDec);
			incomingRelationshipsTable.addColumn(nameColumn);
			incomingRelationshipsTable.addColumn(new Column(RELATIONSHIP_ID_KEY));
			incomingRelationshipsTable.addColumn(new Column(START_NODE_KEY));
			incomingRelationshipsTable.addColumn(new Column(END_NODE_KEY));
			incomingRelationshipsTable.addColumn(new Column(REL_TYPE_KEY));
			incomingRelationshipsTable.addColumn(new Column(REL_ATTRS_KEY));
			incomingRelationshipsTable.addColumn(actionColumnIn);
			incomingRelationshipsTable.setPageSize(DEFAULT_PAGESIZE);
			incomingRelationshipsTable.getControlLink().setParameter(AbstractNode.Key.nodeId.name(), getNodeId());
			incomingRelationshipsTable.setClass(TABLE_CLASS);
			addControl(incomingRelationshipsTable);

			// ------------------ incoming relationships end ---------------------
			// ------------------ outgoing relationships start ---------------------
			Column actionColumnOut = new Column("Action");

			actionColumnOut.setTextAlign("center");

			AbstractLink[] linksOut = new AbstractLink[] { deleteRelationshipLink };

			actionColumnOut.setDecorator(new LinkDecorator(outgoingRelationshipsTable, linksOut,
				RELATIONSHIP_ID_KEY));
			actionColumnOut.setSortable(false);
			typeColumn = new Column(AbstractNode.Key.type.name());
			iconDec    = new LinkDecorator(outgoingRelationshipsTable, viewRelLink, "id") {

				@Override
				protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link,
								Context context, Object row, Object value) {

					StructrRelationship r = (StructrRelationship) row;
					AbstractNode endNode  = r.getEndNode();

					link = new PageLink("id", getEditPageClass(endNode));
					link.setParameter(NODE_ID_KEY, endNode.getId());
					link.setImageSrc(endNode.getIconSrc());
					super.renderActionLink(buffer, link, context, row, value);
				}
			};
			typeColumn.setDecorator(iconDec);
			outgoingRelationshipsTable.addColumn(typeColumn);
			nameColumn = new Column(AbstractNode.Key.name.name());
			nameDec    = new LinkDecorator(outgoingRelationshipsTable, viewRelLink, "id") {

				@Override
				protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link,
								Context context, Object row, Object value) {

					StructrRelationship r = (StructrRelationship) row;
					AbstractNode endNode  = r.getEndNode();
					PageLink pageLink     = new PageLink("id", getEditPageClass(endNode)) {

						@Override
						public String getHref() {
							return super.getHref().concat("#relationships-tab");
						}
					};

					pageLink.setParameter(NODE_ID_KEY, endNode.getId());
					pageLink.setLabel(endNode.getName());
					super.renderActionLink(buffer, pageLink, context, row, value);
				}
			};
			nameColumn.setDecorator(nameDec);
			outgoingRelationshipsTable.addColumn(nameColumn);
			outgoingRelationshipsTable.addColumn(new Column(RELATIONSHIP_ID_KEY));
			outgoingRelationshipsTable.addColumn(new Column(START_NODE_KEY));
			outgoingRelationshipsTable.addColumn(new Column(END_NODE_KEY));
			outgoingRelationshipsTable.addColumn(new Column(REL_TYPE_KEY));
			outgoingRelationshipsTable.addColumn(new Column(REL_ATTRS_KEY));
			outgoingRelationshipsTable.addColumn(actionColumnOut);
			outgoingRelationshipsTable.setPageSize(DEFAULT_PAGESIZE);
			outgoingRelationshipsTable.getControlLink().setParameter(AbstractNode.Key.nodeId.name(), getNodeId());
			outgoingRelationshipsTable.setClass(TABLE_CLASS);
			addControl(outgoingRelationshipsTable);
			editRelationshipsPanel = new Panel("editRelationshipsPanel",
							   "/panel/edit-relationships-panel.htm");
			addControl(editRelationshipsPanel);
		}

		// ------------------ outgoing relationships end ---------------------
		// ------------------ security begin ---------------------
		if (securityContext.isAllowed(node, Permission.AccessControl)) {

			typeColumn = new Column(AbstractNode.Key.type.name());
			iconDec    = new LinkDecorator(securityTable, viewRelLink, "id") {

				@Override
				protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link,
								Context context, Object row, Object value) {

					StructrRelationship r  = (StructrRelationship) row;
					AbstractNode startNode = r.getStartNode();

					link = new PageLink("id", getEditPageClass(startNode)) {

						@Override
						public String getHref() {
							return super.getHref().concat("#security-tab");
						}
					};
					link.setParameter(NODE_ID_KEY, startNode.getId());
					link.setLabel(startNode.getName());
					link.setImageSrc(getIconSrc(startNode));
					super.renderActionLink(buffer, link, context, row, value);
				}
			};
			typeColumn.setDecorator(iconDec);
			securityTable.addColumn(typeColumn);
			nameColumn = new Column(AbstractNode.Key.name.name());
			nameDec    = new LinkDecorator(securityTable, viewRelLink, "id") {

				@Override
				protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link,
								Context context, Object row, Object value) {

					StructrRelationship r  = (StructrRelationship) row;
					AbstractNode startNode = r.getStartNode();

					link = new PageLink("id", getEditPageClass(startNode)) {

						@Override
						public String getHref() {
							return super.getHref().concat("#security-tab");
						}
					};
					link.setParameter(NODE_ID_KEY, startNode.getId());
					link.setLabel(startNode.getName());
					super.renderActionLink(buffer, link, context, row, value);
				}
			};
			nameColumn.setDecorator(nameDec);
			securityTable.addColumn(nameColumn);
			securityTable.addColumn(new Column(StructrRelationship.Permission.allowed.name()));
			securityTable.setClass(TABLE_CLASS);
			securityForm.add(securityTable);

			FieldSet setPermissionFields = new FieldSet("Set Permissions");

			setPermissionFields.add(userSelect);
			setPermissionFields.add(groupSelect);
			setPermissionFields.add(recursive);

			List<Option> optionList = new LinkedList<Option>();
			Option readOption       = new Option(StructrRelationship.Permission.read.name(), "Read");

			optionList.add(readOption);

			Option showTreeOption = new Option(StructrRelationship.Permission.showTree.name(), "Show Tree");

			optionList.add(showTreeOption);

			Option editPropsOption = new Option(StructrRelationship.Permission.editProperties.name(), "Edit Properties");

			optionList.add(editPropsOption);

			Option addRelOption = new Option(StructrRelationship.Permission.addRelationship.name(), "Add Relationship");

			optionList.add(addRelOption);

			Option removeRelOption = new Option(StructrRelationship.Permission.removeRelationship.name(),
							 "Remove Relationship");

			optionList.add(removeRelOption);

			Option deleteNodeOption = new Option(StructrRelationship.Permission.deleteNode.name(), "Delete Node");

			optionList.add(deleteNodeOption);

			Option createSubnodeOption = new Option(StructrRelationship.Permission.createNode.name(),
							     "Create Subnode");

			optionList.add(createSubnodeOption);

			Option executeOption = new Option(StructrRelationship.Permission.execute.name(), "Execute");

			optionList.add(executeOption);

			Option accessControlOption = new Option(StructrRelationship.Permission.accessControl.name(),
							     "Access Control");

			optionList.add(accessControlOption);
			allowed.addAll(optionList);
			setPermissionFields.add(allowed);
			setPermissionFields.add(new Submit("setPermissions", " Set Permissions ", this,
							   "onSetPermissions"));
			securityForm.add(setPermissionFields);
			securityForm.add(new HiddenField(NODE_ID_KEY, (nodeId != null)
				? nodeId
				: ""));
			securityForm.add(new HiddenField(RENDER_MODE_KEY, (renderMode != null)
				? renderMode
				: ""));
			securityForm.setActionURL(securityForm.getActionURL().concat("#security-tab"));
			addControl(securityForm);
			editSecurityPanel = new Panel("editSecurityPanel", "/panel/edit-security-panel.htm");
			addControl(editSecurityPanel);
		}

		// ------------------ security end ---------------------
		// ------------------ cloud begin ---------------------
		FieldSet pushFields = new FieldSet("Transmit Nodes");

		remoteSourceNode = new LongField("remoteSourceNode", "Remote Node ID");
		remoteHost       = new TextField("remoteHost", "Remote Host");
		userNameInput    = new TextField("userName", "User Name");
		passwordInput    = new PasswordField("password", "Password");
		remoteTcpPort    = new IntegerField("remoteTcpPort", "Remote TCP Port");
		remoteUdpPort    = new IntegerField("remoteUdp", "Remote UDP Port");
		cloudPushPull.add(new Option("push", "Push nodes to remote destination"));
		cloudPushPull.add(new Option("pull", "Pull nodes from remote destination"));
		pushFields.add(remoteSourceNode);
		pushFields.add(remoteHost);
		pushFields.add(userNameInput);
		pushFields.add(passwordInput);
		pushFields.add(remoteTcpPort);
		pushFields.add(remoteUdpPort);
		pushFields.add(cloudPushPull);
		pushFields.add(cloudRecursive);
		pushFields.add(new Submit("transmitNodes", "Transmit", this, "onTransmitNodes"));

		Table transmissionsTable = new Table("Transmissions");

		transmissionsTable.addColumn(new Column("transmissionType", "Type"));
		transmissionsTable.addColumn(new Column("remoteHost", "Remote Host"));
		transmissionsTable.addColumn(new Column("remoteTcpPort", "TCP"));
		transmissionsTable.addColumn(new Column("remoteUdpPort", "UDP"));
		transmissionsTable.addColumn(new Column("estimatedSize", "Estimated Size"));
		transmissionsTable.addColumn(new Column("transmittedObjectCount", "Objects Transmitted"));
		transmissionsTable.setDataProvider(new DataProvider() {

			@Override
			public List<CloudTransmission> getData() {

				CloudService cloudService =
					(CloudService) Services.command(securityContext, GetCloudServiceCommand.class).execute();

				if (cloudService != null) {
					return (cloudService.getActiveTransmissions());
				}

				return (new LinkedList());
			}

		});
		cloudForm.add(pushFields);
		cloudForm.add(transmissionsTable);
		cloudForm.add(new HiddenField(NODE_ID_KEY, (nodeId != null)
			? nodeId
			: ""));
		cloudForm.add(new HiddenField(RENDER_MODE_KEY, (renderMode != null)
			? renderMode
			: ""));
		cloudForm.setActionURL(cloudForm.getActionURL().concat("#cloud-tab"));
		addControl(cloudForm);

		// cloud
		cloudPanel = new Panel("cloudPanel", "/panel/cloud-panel.htm");
		addControl(cloudPanel);

		// ------------------ cloud end ---------------------
		// console
		String prompt = ((user != null)
				 ? user.getName()
				 : "anonymous") + "@structr" + (securityContext.isSuperUser()
			? "# "
			: "$ ");

		consoleCommand = new TextField("command", prompt);
		consoleCommand.addStyleClass("commandInput");
		consoleForm.add(consoleCommand);
		consoleForm.addStyleClass("commandInput");
		consoleForm.add(new Submit("executeCommand", "Execute", this, "onConsoleCommand"));

		StringBuilder actionURL = new StringBuilder(100);

		actionURL.append(consoleForm.getActionURL());

		if (nodeId != null) {
			actionURL.append("?nodeId=").append(nodeId);
		}

		actionURL.append("#console-tab");
		consoleForm.setActionURL(actionURL.toString());
		addControl(consoleForm);
		consolePanel = new Panel("consolePanel", "/panel/console-panel.htm");
		addControl(consolePanel);
		readConsoleOutput();

		// ------------------ console end ---------------------
		helpPanel = new Panel("helpPanel", "/panel/help-panel.htm");
		addControl(helpPanel);
		createHelpOutput();

		// ------------------ console end ---------------------
		if (!(securityContext.isAllowed(node, Permission.EditProperty))) {

			// make all property fields read-only
			List<Field> propertyFields = editPropertiesForm.getFieldList();

			for (Field f : propertyFields) {
				f.setReadonly(true);
			}

			// remove delete relationship link from relationship tables
			incomingRelationshipsTable.removeColumn("Action");
			outgoingRelationshipsTable.removeColumn("Action");
		}
	}

	/**
	 * @see Page#onRender()
	 */
	@Override
	public void onRender() {

		super.onRender();

		if (node != null) {

			final Template templateNode = node.getTemplate();

			templateSelect.setDataProvider(new DataProvider() {

				@Override
				public List<Option> getData() {

					List<Option> options     = new LinkedList<Option>();
					List<AbstractNode> nodes = null;

					if (templateNode != null) {
						nodes = templateNode.getSiblingNodes();
					} else {

						List<TextualSearchAttribute> searchAttrs =
							new LinkedList<TextualSearchAttribute>();

						searchAttrs.add(new TextualSearchAttribute(AbstractNode.Key.type.name(),
							Template.class.getSimpleName(), SearchOperator.OR));
						nodes = (List<AbstractNode>) Services.command(securityContext, 
							SearchNodeCommand.class).execute(
							null, false, false, searchAttrs);
					}

					if (nodes != null) {

						Collections.sort(nodes);
						options.add(Option.EMPTY_OPTION);

						for (AbstractNode n : nodes) {

							if (n instanceof Template) {

								Option opt = new Option(n.getId(), n.getName());

								options.add(opt);
							}
						}
					}

					return options;
				}

			});

			if (node instanceof CustomTypeNode) {

				final NodeType typeNode = ((CustomTypeNode) node).getTypeNode();

				customTypeSelect.setDataProvider(new DataProvider() {

					@Override
					public List<Option> getData() {

						List<Option> options     = new LinkedList<Option>();
						List<AbstractNode> nodes = null;

						if (typeNode != null) {
							nodes = typeNode.getSiblingNodes();
						} else {

							List<TextualSearchAttribute> searchAttrs =
								new LinkedList<TextualSearchAttribute>();

							searchAttrs.add(
							    new TextualSearchAttribute(
								AbstractNode.Key.type.name(), NodeType.class.getSimpleName(),
								SearchOperator.OR));
							nodes = (List<AbstractNode>) Services.command(securityContext, 
								SearchNodeCommand.class).execute(
								null, false, false, searchAttrs);
						}

						if (nodes != null) {

							Collections.sort(nodes);
							options.add(Option.EMPTY_OPTION);

							for (AbstractNode n : nodes) {

								if (n instanceof NodeType) {

									Option opt = new Option(n.getId(), n.getName());

									options.add(opt);
								}
							}
						}

						return options;
					}

				});
			}

			typeSelect.setDataProvider(new DataProvider() {

				@Override
				public List<Option> getData() {

					List<Option> nodeList = new LinkedList<Option>();

					nodeList.add(new Option("", "--- Select Node Type ---"));

					List<String> nodeTypes =
						new LinkedList<String>(
						    ((Map<String,
							  Class>) Services.command(securityContext, 
								  GetEntitiesCommand.class).execute()).keySet());

					Collections.sort(nodeTypes);

					for (String className : nodeTypes) {

						Option o = new Option(className);

						nodeList.add(o);
					}

					return nodeList;
				}

			});

//                      User owner = node.getOwnerNode();
			ownerSelect.setDataProvider(new DataProvider() {

				@Override
				public List<Option> getData() {

					List<Option> options = new LinkedList<Option>();
					List<User> users     = getAllUsers();

					if (users != null) {

						Collections.sort(users);
						options.add(Option.EMPTY_OPTION);

						for (User user : users) {

							Option opt = new Option(user.getId(), user.getName());

							options.add(opt);
						}
					}

					return options;
				}

			});
			editVisibilityForm.copyFrom(node);
			editVisibilityForm.add(new HiddenField(NODE_ID_KEY, (nodeId != null)
				? nodeId
				: ""));
			editVisibilityForm.add(new HiddenField(RENDER_MODE_KEY, (renderMode != null)
				? renderMode
				: ""));
			editVisibilityForm.add(new HiddenField(RETURN_URL_KEY, (returnUrl != null)
				? returnUrl
				: ""));
			editPropertiesForm.copyFrom(node);
			editPropertiesForm.add(new HiddenField(NODE_ID_KEY, (nodeId != null)
				? nodeId
				: ""));
			editPropertiesForm.add(new HiddenField(RENDER_MODE_KEY, (renderMode != null)
				? renderMode
				: ""));
			editPropertiesForm.add(new HiddenField(RETURN_URL_KEY, (returnUrl != null)
				? returnUrl
				: ""));
			deleteRelationshipLink.setParameter(NODE_ID_KEY, nodeId);
			setCurrentOwnerRecursivelyLink.setParameter(NODE_ID_KEY, nodeId);
			securityForm.copyFrom(node);
			securityForm.add(new HiddenField(NODE_ID_KEY, (nodeId != null)
				? nodeId
				: ""));
			securityForm.add(new HiddenField(RENDER_MODE_KEY, (renderMode != null)
				? renderMode
				: ""));
			securityForm.add(new HiddenField(RETURN_URL_KEY, (returnUrl != null)
				? returnUrl
				: ""));
			childNodesTable.setDataProvider(new DataProvider() {

				@Override
				public List<AbstractNode> getData() {

					List<AbstractNode> result = new LinkedList<AbstractNode>();

					result.addAll(node.getSortedDirectChildAndLinkNodes());

					return result;
				}

			});

			// populate titlesTable table
			titlesTable.setDataProvider(new DataProvider() {

				@Override
				public List<Title> getData() {
					return node.getTitles();
				}

			});
			incomingRelationshipsTable.setDataProvider(new DataProvider() {

				@Override
				public List<StructrRelationship> getData() {
					return node.getIncomingRelationships();
				}

			});
			outgoingRelationshipsTable.setDataProvider(new DataProvider() {

				@Override
				public List<StructrRelationship> getData() {
					return node.getOutgoingRelationships();
				}

			});
			userSelect.setDataProvider(new DataProvider() {

				@Override
				public List<Option> getData() {

					List<Option> optionList = new LinkedList<Option>();

					optionList.add(Option.EMPTY_OPTION);

//                                      List<AbstractNode> principals =  node.getSecurityPrincipals();
					List<User> users = getAllUsers();

					if (users != null) {

						for (User u : users) {

							Option o = new Option(u.getName());

							optionList.add(o);
						}
					}

					return optionList;
				}

			});
			groupSelect.setDataProvider(new DataProvider() {

				@Override
				public List<Option> getData() {

					List<Option> optionList = new LinkedList<Option>();

					optionList.add(Option.EMPTY_OPTION);

//                                      List<AbstractNode> principals =  node.getSecurityPrincipals();
					List<Group> groups = getAllGroups();

					if (groups != null) {

						for (Group u : groups) {

							Option o = new Option(u.getName());

							optionList.add(o);
						}
					}

					return optionList;
				}

			});

			// populate security table
			securityTable.setDataProvider(new DataProvider() {

				@Override
				public List<StructrRelationship> getData() {

					List<StructrRelationship> rels   = node.getIncomingRelationships();
					List<StructrRelationship> result = new LinkedList<StructrRelationship>();

					for (StructrRelationship r : rels) {

						RelationshipType rt   = r.getRelType();
						boolean isSecurityRel = rt.equals(RelType.SECURITY);

						if (isSecurityRel) {
							result.add(r);
						}
					}

					return result;
				}

			});
		}
	}

	/**
	 * Save form data and stay in edit mode
	 *
	 * @return
	 */
	public boolean onSaveProperties() {

		if (editPropertiesForm.isValid()) {

			save();
			okMsg = "Node parameter successfully saved.";

			return redirect();

		} else {
			return true;
		}
	}

	/**
	 * Don't save form data. May redirect to previous action
	 * {@see redirect()}
	 *
	 * @return
	 */
	public boolean onCancel() {

		return redirect();

//              Map<String, String> parameters = new HashMap<String, String>();
//              parameters.put(OK_MSG_KEY, okMsg);
//
//              AbstractNode targetNode = node.getParentNode(user);
//              if (targetNode == null) {
//                  // if no parent available, keep node
//                  targetNode = node;
//              }
//
//              parameters.put(NODE_ID_KEY, Long.toString(targetNode.getId()));
//
//              Class<? extends Page> c = Edit.class;
//              try {
//                  c = (Class<? extends Page>) Class.forName(targetNode.getEditPageName());
//              } catch (ClassNotFoundException e) {
//                  System.out.println("No edit page found for " + targetNode.getEditPageName());
//              }
//
//              setRedirect(c, parameters);
//
//              return false;
	}

	/**
	 * Save form data
	 */
	protected void save() {

		final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				AbstractNode s = getNodeByIdOrPath(getNodeId());

				if (editPropertiesForm.isValid()) {

					editPropertiesForm.copyTo(s);
					transactionCommand.setExitCode(Command.exitCode.SUCCESS);
					okMsg = "Form data saved successfully";

				} else {

					transactionCommand.setExitCode(Command.exitCode.FAILURE);
					errorMsg = "Properties form is invalid";
					transactionCommand.setErrorMessage(errorMsg);
				}

				return (null);
			}

		};
		
		transactionCommand.execute(transaction);
		
		if(transaction.getCause() != null) {
			okMsg = transaction.getCause().getMessage();
			logger.log(Level.WARNING, "Exception while storing properties", transaction.getCause());
		}
	}

	/**
	 * Delete a property
	 */
	public boolean onDeleteRelationship() {

		final Command transaction            = Services.command(securityContext, TransactionCommand.class);
		String localNodeId                   = deleteRelationshipLink.getParameter(NODE_ID_KEY);
		final String relationshipId          = deleteRelationshipLink.getValue();
		final Map<String, String> parameters = new HashMap<String, String>();

		if (relationshipId != null) {

			final Long id = Long.parseLong(relationshipId);

			transaction.execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					Command deleteRelationship = Services.command(securityContext, DeleteRelationshipCommand.class);

					deleteRelationship.execute(id);
					transaction.setErrorMessage(deleteRelationship.getErrorMessage());
					transaction.setExitCode(deleteRelationship.getExitCode());

					return (null);
				}

			});

			if (Command.exitCode.FAILURE.equals(transaction.getExitCode())) {

				errorMsg = transaction.getErrorMessage();
				parameters.put(ERROR_MSG_KEY, errorMsg);

			} else {

				okMsg = "Relationship successfully removed!";    // TODO: localize
				parameters.put(OK_MSG_KEY, okMsg);
			}

		} else {

			errorMsg = "No Relationship ID!";
			parameters.put(ERROR_MSG_KEY, errorMsg);
		}

		parameters.put(NODE_ID_KEY, localNodeId);
		parameters.put(RENDER_MODE_KEY, renderMode);
		setRedirect(getRedirectPage(getNodeByIdOrPath(getNodeId())), parameters);

		return false;
	}

	/**
	 * Make owner of current node owner of all sub nodes
	 *
	 * @return
	 */
	public boolean onSetCurrentOwnerRecursively() {

		Command setOwner         = Services.command(securityContext, SetOwnerCommand.class);
		List<AbstractNode> nodes = node.getAllChildren();

		setOwner.execute(nodes, node.getOwnerNode());

		return redirect();
	}

	/**
	 * Save form data
	 */
	public boolean onSetPermissions() {

		final Map<String, String> parameters  = new HashMap<String, String>();

		if (securityForm.isValid()) {

			final String selectedUserName     = securityForm.getFieldValue(userSelect.getName());
			final String selectedGroupName    = securityForm.getFieldValue(groupSelect.getName());
			final List<String> selectedValues = allowed.getSelectedValues();
			final boolean rec                 = recursive.isChecked();
			Command transaction               = Services.command(securityContext, TransactionCommand.class);

			transaction.execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					Command findUser    = Services.command(securityContext, FindUserCommand.class);
					Command findGroup   = Services.command(securityContext, FindGroupCommand.class);
					User selectedUser   = (User) findUser.execute(selectedUserName);
					Group selectedGroup = (Group) findGroup.execute(selectedGroupName);
					User user           = securityContext.getUser();

					if ((selectedUser != null) || (selectedGroup != null)) {

						List<AbstractNode> nodes = new LinkedList<AbstractNode>();

						if (rec) {

							Command findNode          =
								Services.command(securityContext, FindNodeCommand.class);
							List<AbstractNode> result =
								(List<AbstractNode>) findNode.execute(node);

							for (AbstractNode s : result) {

								// superuser can always change access control
								if (securityContext.isAllowed(
									node, Permission.AccessControl)) {
									nodes.add(s);
								}
							}

						} else {

							// not recursive, change only this node
							nodes.add(node);
						}

						Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);
						Command deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);

						for (AbstractNode n : nodes) {

//                                                      if (n.equals(selectedUser) || n.equals(selectedGroup)) {
//                                                          // don't try to set a relationship with node itself
//                                                          continue;
//                                                      }
							// User
							if (selectedUser != null) {

								StructrRelationship r =
									n.getSecurityRelationship(selectedUser);

								if (r == null) {

									r = (StructrRelationship) createRel.execute(
										selectedUser, n, RelType.SECURITY);
								}

								if ((selectedValues != null)
									&& (selectedValues.size() > 0)) {
									r.setAllowed(selectedValues);
								} else {
									deleteRel.execute(r);
								}
							}

							// Group
							if (selectedGroup != null) {

								StructrRelationship r =
									n.getSecurityRelationship(selectedGroup);

								if (r == null) {

									r = (StructrRelationship) createRel.execute(
										selectedGroup, n, RelType.SECURITY);
								}

								if ((selectedValues != null)
									&& (selectedValues.size() > 0)) {
									r.setAllowed(selectedValues);
								} else {
									deleteRel.execute(r);
								}
							}
						}

						okMsg = "Permissions successfully set";
						parameters.put(OK_MSG_KEY, okMsg);
					}

					return (null);
				}

			});
		}

		parameters.put(NODE_ID_KEY, nodeId.toString());
		setRedirect(getRedirectPage(getNodeByIdOrPath(getNodeId())), parameters);

		return false;
	}

	/**
	 * Save visibility data
	 */
	public boolean onSaveVisibility() {

		if (editVisibilityForm.isValid()) {

			final TransactionCommand transactionCommand = (TransactionCommand)Services.command(securityContext, TransactionCommand.class);
			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					AbstractNode s = getNodeByIdOrPath(getNodeId());

					if (editVisibilityForm.isValid()) {

						editVisibilityForm.copyTo(s, true);
						transactionCommand.setExitCode(Command.exitCode.SUCCESS);

					} else {

						transactionCommand.setExitCode(Command.exitCode.FAILURE);
						transactionCommand.setErrorMessage("Visibility form is invalid");
					}

					return (null);
				}

			};
			
			transactionCommand.execute(transaction);
			
			if(transaction.getCause() != null) {
				okMsg = transaction.getCause().getMessage();
			} else {
				okMsg = "Node visibility parameter successfully saved.";
			}

			Map<String, String> parameters = new HashMap<String, String>();

			parameters.put(NODE_ID_KEY, nodeId.toString());
			setRedirect(getPath(), parameters);

			return false;

		} else {
			return true;
		}
	}

	/**
	 * Save visibility data on given node and subnodes (all direct children)
	 */
	public boolean onSaveVisibilityIncludingDirectChildren() {

		if (editVisibilityForm.isValid()) {

			final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					AbstractNode root             = getNodeByIdOrPath(getNodeId());
					List<AbstractNode> childNodes = root.getDirectChildNodes();

					// include node itself
					childNodes.add(root);

					for (AbstractNode s : childNodes) {
						editVisibilityForm.copyTo(s, true);
					}

					transactionCommand.setExitCode(Command.exitCode.SUCCESS);
					okMsg = "Node visibility parameter successfully saved on " + childNodes.size()
						+ " nodes.";

					return (null);
				}

			});

			Map<String, String> parameters = new HashMap<String, String>();

			parameters.put(NODE_ID_KEY, nodeId.toString());
			setRedirect(getPath(), parameters);

			return false;

		} else {
			return true;
		}
	}

	/**
	 * Save visibility data on given node and all subnodes (recursively)
	 */
	public boolean onSaveVisibilityIncludingAllChildren() {

		if (editVisibilityForm.isValid()) {

			final Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					AbstractNode root             = getNodeByIdOrPath(getNodeId());
					List<AbstractNode> childNodes = root.getAllChildren();

					// include node itself
					childNodes.add(root);

					for (AbstractNode s : childNodes) {
						editVisibilityForm.copyTo(s, true);
					}

					transactionCommand.setExitCode(Command.exitCode.SUCCESS);
					okMsg = "Node visibility parameter successfully saved on " + childNodes.size()
						+ " nodes.";

					return (null);
				}

			});

			Map<String, String> parameters = new HashMap<String, String>();

			parameters.put(NODE_ID_KEY, nodeId.toString());
			setRedirect(getPath(), parameters);

			return false;

		} else {
			return true;
		}
	}

	public boolean onTransmitNodes() {

		final String remoteHostValue = remoteHost.getValue();
		final String userNameValue   = userNameInput.getValue();
		final String passwordValue   = passwordInput.getValue();
		final Long targetNodeValue   = remoteSourceNode.getLong();
		final Integer tcpPort        = remoteTcpPort.getInteger();
		final Integer udpPort        = remoteUdpPort.getInteger();
		final boolean rec            = cloudRecursive.isChecked();
		final String pushPull        = cloudPushPull.getValue();

		// start new thread with name of current session Id
		new Thread(new Runnable() {

			@Override
			public void run() {

				if ("push".equals(pushPull)) {

					Command transmitCommand = Services.command(securityContext, PushNodes.class);

					transmitCommand.execute(userNameValue, passwordValue, node, targetNodeValue,
								remoteHostValue, tcpPort, udpPort, rec);

				} else if ("pull".equals(pushPull)) {

					Command transmitCommand = Services.command(securityContext, PullNode.class);
					User user               = securityContext.getUser();

					transmitCommand.execute(user, targetNodeValue, node, remoteHostValue, tcpPort,
								udpPort, rec);
				}
			}

		}, getContext().getSession().getId()).start();

		return false;
	}

	public boolean onConsoleCommand() {

		List<String> consoleLines = getConsoleLines();

		// create callback to pass new parent node on deleted nodes
		final CallbackValue<String> newNodeId = new CallbackValue<String>(nodeId);
		Callback callback                     = new Callback() {

			@Override
			public void callback(Object... params) {

				if (params.length > 0) {
					newNodeId.setValue(params[0].toString());
				}
			}
		};

		consoleLines.add(Services.command(securityContext, NodeConsoleCommand.class).execute(securityContext, node, consoleCommand.getValue(),
						  callback).toString());

		if (consoleLines.size() > 20) {
			consoleLines.remove(0);
		}

		readConsoleOutput();

		StringBuilder redirectPath = new StringBuilder(100);

		redirectPath.append(getContext().getRequest().getContextPath());
		redirectPath.append(getPath());
		redirectPath.append("?nodeId=").append(newNodeId.getValue());
		redirectPath.append("#console-tab");

		// test
		nodeTree.expand(getTreeNode(newNodeId.getValue()));
		redirect = redirectPath.toString();

		return (true);
	}

	private void readConsoleOutput() {

		List<String> consoleLines = getConsoleLines();

		// initialize buffer with estimated size to prevent resizing
		StringBuilder buffer = new StringBuilder(consoleLines.size() * 80);

		for (String line : consoleLines) {
			buffer.append(line);
		}

		// set consoleOutput
		consoleOutput = buffer.toString();
	}

	private void createHelpOutput() {

		String helpTarget     = getContext().getRequest().getParameter("helpTarget");
		AbstractNode helpNode = null;

		if ((helpTarget != null) && (helpTarget.length() > 0)) {

			Class nodeClass = (Class) Services.command(securityContext, GetEntityClassCommand.class).execute(helpTarget);

			if (nodeClass != null) {

				try {
					helpNode = (AbstractNode) nodeClass.newInstance();
				} catch (Throwable t) {}
			}

		} else {
			helpNode = this.node;
		}

		if (helpNode != null) {

			Context context                             = getContext();
			HtmlComponentRenderer htmlComponentRenderer = new HtmlComponentRenderer();
			StructrOutputStream out                     = new StructrOutputStream(getContext().getRequest(), securityContext);
			AbstractComponent content                   = helpNode.getHelpContent();

			if (content != null) {

				htmlComponentRenderer.renderNode(out, content, helpNode, "", editNodeId,
								 RenderMode.Direct);

				// create help output
				helpOutput = out.toString();

			} else {
				helpOutput = "No help available for this node type.";
			}
		}
	}

	//~--- get methods ----------------------------------------------------

	// use template for backend pages
	@Override
	public String getTemplate() {
		return "/admin-edit-template.htm";
	}

	private List<String> getConsoleLines() {

		List<String> ret =
			(List<String>) getContext().getSession().getAttribute(NodeConsoleCommand.CONSOLE_BUFFER_KEY);

		if (ret == null) {

			StringBuilder welcome = new StringBuilder();

			welcome.append("<p>Welcome to the structr console!</p>");
			welcome.append("<p>Supported commands: ");

			for (Entry<String, Class<? extends Operation>> entry :
				NodeConsoleCommand.operationsMap.entrySet()) {

				String cmd = entry.getKey();
				Class op   = entry.getValue();

				if (PrimaryOperation.class.isAssignableFrom(op)) {
					welcome.append(" <b>").append(cmd).append("</b>");
				}
			}

			welcome.append("</p>");
			welcome.append("<p>Use 'help <command>' to show a short help text for each command.</p>");
			welcome.append("<p>&nbsp;</p>");
			ret = new LinkedList<String>();
			ret.add(welcome.toString());
			getContext().getSession().setAttribute(NodeConsoleCommand.CONSOLE_BUFFER_KEY, ret);
		}

		return (ret);
	}
}
