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
package org.structr.core.cloud;

import org.structr.core.entity.User;

/**
 * Encapsulates a pull request for a node
 *
 *
 * @author Christian Morgner
 */
public class PullNodeRequestContainer extends DataContainer
{
	private boolean recursive = false;
	private String remoteHost = null;
	private User remoteUser = null;
	private long sourceNodeId = 0L;
	private long targetNodeId = 0L;
	private int remoteTcpPort = 0;
	private int remoteUdpPort = 0;

	public PullNodeRequestContainer()
	{
	}

	public PullNodeRequestContainer(User remoteUser, long remoteSourceNodeId, long localTargetNodeId, String remoteHost, int remoteTcpPort, int remoteUdpPort, boolean recursive)
	{
		this.sourceNodeId = remoteSourceNodeId;
		this.targetNodeId = localTargetNodeId;
		this.remoteHost = remoteHost;
		this.remoteTcpPort = remoteTcpPort;
		this.remoteUdpPort = remoteUdpPort;
		this.recursive = recursive;

		this.estimatedSize = 1024;
	}

	/**
	 * @return the sourceNodeId
	 */
	public long getSourceNodeId()
	{
		return sourceNodeId;
	}

	/**
	 * @param remoteSourceNodeId the sourceNodeId to set
	 */
	public void setSourceNodeId(long sourceNodeId)
	{
		this.sourceNodeId = sourceNodeId;
	}

	/**
	 * @return the recursive
	 */
	public boolean isRecursive()
	{
		return recursive;
	}

	/**
	 * @param recursive the recursive to set
	 */
	public void setRecursive(boolean recursive)
	{
		this.recursive = recursive;
	}

	/**
	 * @return the targetNodeId
	 */
	public long getTargetNodeId()
	{
		return targetNodeId;
	}

	/**
	 * @param localTargetNodeId the targetNodeId to set
	 */
	public void setTargetNodeId(long targetNodeId)
	{
		this.targetNodeId = targetNodeId;
	}

	/**
	 * @return the remoteHost
	 */
	public String getRemoteHost()
	{
		return remoteHost;
	}

	/**
	 * @param remoteHost the remoteHost to set
	 */
	public void setRemoteHost(String remoteHost)
	{
		this.remoteHost = remoteHost;
	}

	/**
	 * @return the remoteTcpPort
	 */
	public int getRemoteTcpPort()
	{
		return remoteTcpPort;
	}

	/**
	 * @param remoteTcpPort the remoteTcpPort to set
	 */
	public void setRemoteTcpPort(int remoteTcpPort)
	{
		this.remoteTcpPort = remoteTcpPort;
	}

	/**
	 * @return the remoteUdpPort
	 */
	public int getRemoteUdpPort()
	{
		return remoteUdpPort;
	}

	/**
	 * @param remoteUdpPort the remoteUdpPort to set
	 */
	public void setRemoteUdpPort(int remoteUdpPort)
	{
		this.remoteUdpPort = remoteUdpPort;
	}

	/**
	 * @return the remoteUser
	 */
	public User getRemoteUser()
	{
		return remoteUser;
	}

	/**
	 * @param remoteUser the remoteUser to set
	 */
	public void setRemoteUser(User remoteUser)
	{
		this.remoteUser = remoteUser;
	}
}
