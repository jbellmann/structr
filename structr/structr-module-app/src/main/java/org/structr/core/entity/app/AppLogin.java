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

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.SessionValue;
import org.structr.common.StructrOutputStream;
import org.structr.context.SessionMonitor;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.app.slots.NullSlot;
import org.structr.core.entity.app.slots.StringSlot;
import org.structr.core.node.FindUserCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

import javax.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.structr.common.AbstractComponent;
import org.structr.core.auth.AuthenticationException;
import org.structr.core.entity.User;
import org.structr.help.Container;
import org.structr.help.Content;
import org.structr.help.HelpLink;
import org.structr.help.ListItem;
import org.structr.help.Paragraph;
import org.structr.help.Subtitle;
import org.structr.help.UnorderedList;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class AppLogin extends ActionNode {

	protected final static int DefaultDelayThreshold = 100;
	protected final static int DefaultDelayTime      = 3;
	protected final static int DefaultMaxErrors      = 1000;
	private final static String LOGIN_FAILURE_TEXT   =
		"Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
	private static final Logger logger = Logger.getLogger(AppLogin.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(AppLogin.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- fields ---------------------------------------------------------

	private SessionValue<String> errorMessageValue   = null;
	private SessionValue<Integer> loginAttemptsValue = null;
	private SessionValue<String> okMessageValue      = null;

	// non-final variables
	private SessionValue<Boolean> sessionBlockedValue = null;
	private SessionValue<String> userNameValue        = null;

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey {
		username, password, antiRobot, delayThreshold, delayTime, maxErrors;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public boolean doAction(final StructrOutputStream out, final AbstractNode startNode, final String editUrl,
				final Long editNodeId) {


		HttpServletRequest request = out.getRequest();

		if (out.getSecurityContext().getUser() != null) {
			return (true);
		}

		Boolean sessionBlocked = getSessionBlockedValue().get(out.getRequest());

		if (Boolean.TRUE.equals(sessionBlocked)) {

			getErrorMessageValue().set(out.getRequest(), "Too many login attempts, session is blocked for login");

			return (false);
		}

		String username  = (String) getValue(request, Key.username.name());
		String password  = (String) getValue(request, Key.password.name());
		String antiRobot = (String) getValue(request, Key.antiRobot.name());

		if (StringUtils.isNotEmpty(antiRobot)) {

			// Don't process form if someone has filled the anti-robot field
			return (false);
		}

		if (StringUtils.isEmpty(username)) {

			setErrorValue(request, Key.username.name(), "You must enter a username");
			countLoginFailure(request, request.getSession(), getMaxErrors(),
					  getDelayThreshold(),
					  getDelayTime());

			return (false);
		}

		if (StringUtils.isEmpty(password)) {

			setErrorValue(request, Key.password.name(), "You must enter a password");
			countLoginFailure(request, request.getSession(), getMaxErrors(),
					  getDelayThreshold(),
					  getDelayTime());

			return (false);
		}

		// Session is not blocked, and we have a username and a password
		// First, check if we have a user with this name
		User loginUser = (User) Services.command(out.getSecurityContext(), FindUserCommand.class).execute(username);

		// No matter what reason to deny login, always show the same error message to
		// avoid giving hints
		getErrorMessageValue().set(request, LOGIN_FAILURE_TEXT);

		if (loginUser == null) {

			logger.log(Level.INFO,
				   "No user found for name {0}",
				   loginUser);
			countLoginFailure(request, request.getSession(), getMaxErrors(),
					  getDelayThreshold(),
					  getDelayTime());

			return (false);
		}


		try {
			out.getSecurityContext().doLogin(username, password);

		} catch(AuthenticationException aex) {
			logger.log(Level.INFO,
				   "User {0} is blocked",
				   loginUser);
			countLoginFailure(request, request.getSession(), getMaxErrors(),
					  getDelayThreshold(),
					  getDelayTime());

			return (false);
		}

		// Check password
		String encryptedPasswordValue = DigestUtils.sha512Hex(password);

		if (!encryptedPasswordValue.equals(loginUser.getEncryptedPassword())) {

			logger.log(Level.INFO,
				   "Wrong password for user {0}",
				   loginUser);
			countLoginFailure(request, request.getSession(), getMaxErrors(),
					  getDelayThreshold(),
					  getDelayTime());

			getErrorMessageValue().set(request, LOGIN_FAILURE_TEXT);
			countLoginFailure(request, request.getSession(), getMaxErrors(), getDelayThreshold(), getDelayTime());
			return false;
		}

		// Register user with internal session management
		long sessionId = SessionMonitor.registerUserSession(securityContext, request.getSession());

		SessionMonitor.logActivity(out.getSecurityContext(), sessionId,
					   "Login");

		// Clear all blocking stuff
		getSessionBlockedValue().set(request, false);
		getLoginAttemptsValue().set(request, 0);
		getErrorMessageValue().set(request, null);

		// set success message
		getOkMessageValue().set(out.getRequest(), "Login successful.");

		// finally, return success
		return (true);
	}

	// ----- private methods -----
	private void countLoginFailure(HttpServletRequest request, HttpSession session, int maxErrors, int delayThreshold, int delayTime) {

		Integer retries     = getLoginAttemptsValue().get(request);

		if ((retries != null) && (retries > maxErrors)) {

			logger.log(Level.SEVERE, "More than {0} login failures, session {1} is blocked",
				   new Object[] { maxErrors,
						  session.getId() });
			getSessionBlockedValue().set(request, true);
			getErrorMessageValue().set(request,
			    "Too many unsuccessful login attempts, your session is blocked for login!");

			getSessionBlockedValue().set(request, true);

		} else if ((retries != null) && (retries > delayThreshold)) {

			logger.log(Level.INFO,
				   "More than {0} login failures, execution delayed by {1} seconds",
				   new Object[] { maxErrors, delayTime });

			try {
				Thread.sleep(delayTime * 1000);
			} catch (InterruptedException ex) {

				logger.log(Level.SEVERE,
					   null,
					   ex);
			}

			StringBuilder errorBuffer = new StringBuilder(200);

			errorBuffer.append("You had more than ");
			errorBuffer.append(maxErrors);
			errorBuffer.append(" unsuccessful login attempts, execution was delayed by ");
			errorBuffer.append(delayTime);
			errorBuffer.append(" seconds.");
			getErrorMessageValue().set(request, errorBuffer.toString());

		} else if (retries != null) {
			getLoginAttemptsValue().set(request, getLoginAttemptsValue().get(request) + 1);
		} else {
			getLoginAttemptsValue().set(request, 1);
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/door_in.png";
	}

	@Override
	public Map<String, Slot> getSlots() {

		Map<String, Slot> ret = new LinkedHashMap<String, Slot>();

		ret.put(Key.username.name(),
			new StringSlot());
		ret.put(Key.password.name(),
			new StringSlot());
		ret.put(Key.antiRobot.name(),
			new NullSlot(String.class));

		return (ret);
	}

	// ----- getter & setter

	/**
	 * Return number of unsuccessful login attempts for a session
	 *
	 * If number is exceeded, login is blocked for this session.
	 */
	public int getMaxErrors() {

		Integer maxErrors = getIntProperty(Key.maxErrors.name());

		return (maxErrors == null)
		       ? DefaultMaxErrors
		       : maxErrors;
	}

	/**
	 * Return number of unsuccessful login attempts before retry delay becomes active
	 */
	public int getDelayThreshold() {

		Integer delayThreshold = getIntProperty(Key.delayThreshold.name());

		return (delayThreshold == null)
		       ? DefaultDelayThreshold
		       : delayThreshold;
	}

	/**
	 * Return time (in seconds) to wait for retry after an unsuccessful login attempt
	 */
	public int getDelayTime() {

		Integer delayTime = getIntProperty(Key.delayTime.name());

		return (delayTime == null)
		       ? DefaultDelayTime
		       : delayTime;
	}

	private SessionValue<String> getUserNameValue() {

		if (userNameValue == null) {
			userNameValue = new SessionValue<String>(createUniqueIdentifier("userName"));
		}

		return (userNameValue);
	}

	private SessionValue<Integer> getLoginAttemptsValue() {

		if (loginAttemptsValue == null) {

			// create new session value with default 0
			loginAttemptsValue = new SessionValue<Integer>(createUniqueIdentifier("loginAttempts"),
				0);
		}

		return (loginAttemptsValue);
	}

	private SessionValue<Boolean> getSessionBlockedValue() {

		if (sessionBlockedValue == null) {

			// create new session value with default false
			sessionBlockedValue = new SessionValue<Boolean>(createUniqueIdentifier("sessionBlocked"),
				false);
		}

		return (sessionBlockedValue);
	}

	private SessionValue<String> getErrorMessageValue() {

		if (errorMessageValue == null) {

			// create new session value with default ""
			errorMessageValue = new SessionValue<String>("errorMessage",
				"");
		}

		return (errorMessageValue);
	}

	private SessionValue<String> getOkMessageValue() {

		if (okMessageValue == null) {

			// create new session value with default ""
			okMessageValue = new SessionValue<String>("okMessage",
				"");
		}

		return (okMessageValue);
	}

	@Override
	public AbstractComponent getHelpContent() {

		AbstractComponent root = new Container();

		root.add(
		    new Paragraph().add(
			    new Content("This is a Login node. It can only be activated when used as a child of ",
					new HelpLink("AppActionContainer"))));
		root.add(new Subtitle().add(new Content("Slots")));
		root.add(
		    new UnorderedList().add(new ListItem().add(new Content(Key.username.name()))).add(
			    new ListItem().add(new Content(Key.password.name()))).add(
			    new ListItem().add(new Content(Key.antiRobot.name()))));

		return (root);
	}
}
