/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package com.ibm.jusb.os.linux;

/**
 * Copyright (c) 1999 - 2001, International Business Machines Corporation.
 * All Rights Reserved.
 *
 * This software is provided and licensed under the terms and conditions
 * of the Common Public License:
 * http://oss.software.ibm.com/developerworks/opensource/license-cpl.html
 */

/**
 * Request to claim or release an interface.
 * @author Dan Streetman
 */
abstract class LinuxInterfaceRequest extends LinuxRequest
{
	/** Constructor */
	public LinuxInterfaceRequest(int ifnum) { setInterfaceNumber(ifnum); }

	/** @return The interface number */
	public int getInterfaceNumber() { return interfaceNumber; }

	/** @param number The interface number */
	public void setInterfaceNumber( int number ) { interfaceNumber = number; }

	/** @return If the interface is claimed */
	public boolean isClaimed() { return claimed; }

	/** @param c If the interface is claimed */
	public void setClaimed(boolean c) { claimed = c; }

	/** @return If claiming an interface, if the claim should be forced. */
	public boolean getForceClaim() { return forceClaim; }

	private int interfaceNumber = 0;

	private boolean claimed = false;
	protected boolean forceClaim = false;

	public static class LinuxClaimInterfaceRequest extends LinuxInterfaceRequest
	{
		public LinuxClaimInterfaceRequest(int ifnum) { super(ifnum); }
		public LinuxClaimInterfaceRequest(int ifnum, boolean force)
		{
			this(ifnum);
			forceClaim = force;
		}
		public int getType() { return LinuxRequest.LINUX_CLAIM_INTERFACE_REQUEST; }
	}

	public static class LinuxIsClaimedInterfaceRequest extends LinuxInterfaceRequest
	{
		public LinuxIsClaimedInterfaceRequest(int ifnum) { super(ifnum); }
		public int getType() { return LinuxRequest.LINUX_IS_CLAIMED_INTERFACE_REQUEST; }
	}

	public static class LinuxReleaseInterfaceRequest extends LinuxInterfaceRequest
	{
		public LinuxReleaseInterfaceRequest(int ifnum) { super(ifnum); }
		public int getType() { return LinuxRequest.LINUX_RELEASE_INTERFACE_REQUEST; }
	}

}
